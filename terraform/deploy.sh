#!/bin/bash
# ============================================================
# Terraform apply 후 실행하는 배포 스크립트
# 사용법: ./deploy.sh
# ============================================================

set -e

echo "========================================="
echo "SeRViS EKS 배포 스크립트"
echo "========================================="

# 1. Terraform output에서 정보 가져오기
echo "[1/9] Terraform 출력값 가져오는 중..."
RDS_HOST=$(terraform -chdir=terraform output -raw rds_address)
ECR_REGISTRY=$(terraform -chdir=terraform output -json deploy_info | jq -r '.ecr_registry')
PUBLIC_SUBNETS=$(terraform -chdir=terraform output -json deploy_info | jq -r '.public_subnets')
CLUSTER_NAME=$(terraform -chdir=terraform output -raw eks_cluster_name)
ACM_ARN=$(terraform -chdir=terraform output -raw acm_certificate_arn)
HOSTED_ZONE_ID=$(terraform -chdir=terraform output -raw hosted_zone_id)

# DB 계정 정보 (terraform.tfvars에서 읽거나 환경변수로 오버라이드 가능)
RDS_MASTER_USERNAME="${RDS_MASTER_USERNAME:-admin}"
RDS_MASTER_PASSWORD="${RDS_MASTER_PASSWORD:-$(grep db_master_password terraform/terraform.tfvars | awk -F'"' '{print $2}')}"
DB_APP_PASSWORD="${DB_APP_PASSWORD:-$(grep db_app_password terraform/terraform.tfvars | awk -F'"' '{print $2}')}"

echo "  RDS: ${RDS_HOST}"
echo "  ECR: ${ECR_REGISTRY}"
echo "  Subnets: ${PUBLIC_SUBNETS}"
echo "  ACM ARN: ${ACM_ARN}"
echo "  Hosted Zone: ${HOSTED_ZONE_ID}"

# 2. kubeconfig 설정
echo "[2/9] kubeconfig 설정 중..."
aws eks update-kubeconfig --name ${CLUSTER_NAME} --region ap-northeast-2

# 3. DB 초기화 (serve_user + 3개 스키마 생성 — 이미 존재하면 스킵)
echo "[3/9] DB 초기화 중..."
kubectl run db-init --image=mariadb:10.11 --restart=Never -n serve --rm -i \
  --env="MYSQL_PWD=${RDS_MASTER_PASSWORD}" \
  -- mysql -h "${RDS_HOST}" -u "${RDS_MASTER_USERNAME}" \
  -e "
CREATE DATABASE IF NOT EXISTS serve_auth_db CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS serve_team_db CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS serve_core_db CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE USER IF NOT EXISTS 'serve_user'@'%' IDENTIFIED BY '${DB_APP_PASSWORD}';
GRANT ALL PRIVILEGES ON serve_auth_db.* TO 'serve_user'@'%';
GRANT ALL PRIVILEGES ON serve_team_db.* TO 'serve_user'@'%';
GRANT ALL PRIVILEGES ON serve_core_db.* TO 'serve_user'@'%';
FLUSH PRIVILEGES;
"
echo "  DB 초기화 완료"

# 4. ECR 로그인
echo "[4/9] ECR 로그인 중..."
aws ecr get-login-password --region ap-northeast-2 | \
  docker login --username AWS --password-stdin ${ECR_REGISTRY}

# 5. Docker 빌드 + Push (M칩 맥북용 amd64 빌드)
echo "[5/9] Docker 이미지 빌드 + Push 중..."
docker buildx build --platform linux/amd64 -f Dockerfile.auth -t ${ECR_REGISTRY}/serve-auth:latest --push .
docker buildx build --platform linux/amd64 -f Dockerfile.team -t ${ECR_REGISTRY}/serve-team:latest --push .
docker buildx build --platform linux/amd64 -f Dockerfile.core -t ${ECR_REGISTRY}/serve-core:latest --push .

# 5. K8s 매니페스트의 동적 값 교체
echo "[6/9] K8s 매니페스트 업데이트 중..."

# secrets.yaml의 DB_HOST 업데이트
sed -i '' "s|DB_HOST:.*|DB_HOST: \"${RDS_HOST}\"|" k8s-manifests/secrets.yaml

# deployment.yaml의 ECR 이미지 URL 업데이트
for svc in auth team core; do
  sed -i '' "s|image:.*serve-${svc}:.*|image: ${ECR_REGISTRY}/serve-${svc}:latest|" k8s-manifests/${svc}/deployment.yaml
done

# ingress.yaml의 서브넷 업데이트
sed -i '' "s|alb.ingress.kubernetes.io/subnets:.*|alb.ingress.kubernetes.io/subnets: \"${PUBLIC_SUBNETS}\"|" k8s-manifests/ingress.yaml

# ingress.yaml의 ACM ARN 업데이트
sed -i '' "s|alb.ingress.kubernetes.io/certificate-arn:.*|alb.ingress.kubernetes.io/certificate-arn: \"${ACM_ARN}\"|" k8s-manifests/ingress.yaml

# 6. K8s 배포
echo "[7/9] K8s 리소스 배포 중..."
kubectl apply -f k8s-manifests/namespace.yaml
kubectl apply -f k8s-manifests/secrets.yaml
kubectl apply -f k8s-manifests/auth/
kubectl apply -f k8s-manifests/team/
kubectl apply -f k8s-manifests/core/
kubectl apply -f k8s-manifests/ingress.yaml

# 7. 상태 확인
echo "[8/9] 배포 상태 확인 중..."
echo ""
echo "--- Pods ---"
kubectl get pods -n serve
echo ""
echo "--- Services ---"
kubectl get svc -n serve
echo ""
echo "--- Ingress ---"
kubectl get ingress -n serve

# 8. ALB DNS 대기 → Route 53 CNAME 업데이트
echo ""
echo "[9/9] ALB DNS 대기 중 (최대 5분)..."
ALB_DNS=""
ELAPSED=0
while [ -z "$ALB_DNS" ] && [ $ELAPSED -lt 300 ]; do
  ALB_DNS=$(kubectl get ingress serve-ingress -n serve \
    -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || true)
  if [ -z "$ALB_DNS" ]; then
    sleep 10
    ELAPSED=$((ELAPSED + 10))
    echo "  대기 중... (${ELAPSED}s)"
  fi
done

if [ -z "$ALB_DNS" ]; then
  echo "⚠️  ALB DNS 조회 실패. 수동으로 Route 53 CNAME을 업데이트하세요."
  echo "  kubectl get ingress serve-ingress -n serve"
  exit 1
fi

echo "  ALB DNS: ${ALB_DNS}"
echo "  Route 53 CNAME 업데이트 중..."

aws route53 change-resource-record-sets \
  --hosted-zone-id "${HOSTED_ZONE_ID}" \
  --change-batch "{
    \"Changes\": [{
      \"Action\": \"UPSERT\",
      \"ResourceRecordSet\": {
        \"Name\": \"api.ssucheckmate.com\",
        \"Type\": \"CNAME\",
        \"TTL\": 60,
        \"ResourceRecords\": [{\"Value\": \"${ALB_DNS}\"}]
      }
    }]
  }"

echo ""
echo "========================================="
echo "배포 완료!"
echo "접속 주소: https://api.ssucheckmate.com"
echo "CNAME TTL 60초 반영 후 접속 가능합니다."
echo "========================================="
