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
echo "[1/7] Terraform 출력값 가져오는 중..."
RDS_HOST=$(terraform output -raw rds_address)
ECR_REGISTRY=$(terraform output -json deploy_info | jq -r '.ecr_registry')
PUBLIC_SUBNETS=$(terraform output -json deploy_info | jq -r '.public_subnets')
CLUSTER_NAME=$(terraform output -raw eks_cluster_name)

echo "  RDS: ${RDS_HOST}"
echo "  ECR: ${ECR_REGISTRY}"
echo "  Subnets: ${PUBLIC_SUBNETS}"

# 2. kubeconfig 설정
echo "[2/7] kubeconfig 설정 중..."
aws eks update-kubeconfig --name ${CLUSTER_NAME} --region ap-northeast-2

# 3. ECR 로그인
echo "[3/7] ECR 로그인 중..."
aws ecr get-login-password --region ap-northeast-2 | \
  docker login --username AWS --password-stdin ${ECR_REGISTRY}

# 4. Docker 빌드 + Push (M칩 맥북용 amd64 빌드)
echo "[4/7] Docker 이미지 빌드 + Push 중..."
docker buildx build --platform linux/amd64 -f Dockerfile.auth -t ${ECR_REGISTRY}/serve-auth:latest --push .
docker buildx build --platform linux/amd64 -f Dockerfile.team -t ${ECR_REGISTRY}/serve-team:latest --push .
docker buildx build --platform linux/amd64 -f Dockerfile.core -t ${ECR_REGISTRY}/serve-core:latest --push .

# 5. K8s 매니페스트의 동적 값 교체
echo "[5/7] K8s 매니페스트 업데이트 중..."

# secrets.yaml의 DB_HOST 업데이트
sed -i '' "s|DB_HOST:.*|DB_HOST: \"${RDS_HOST}\"|" k8s-manifests/secrets.yaml

# deployment.yaml의 ECR 이미지 URL 업데이트
for svc in auth team core; do
  sed -i '' "s|image:.*serve-${svc}:.*|image: ${ECR_REGISTRY}/serve-${svc}:latest|" k8s-manifests/${svc}/deployment.yaml
done

# ingress.yaml의 서브넷 업데이트
sed -i '' "s|alb.ingress.kubernetes.io/subnets:.*|alb.ingress.kubernetes.io/subnets: \"${PUBLIC_SUBNETS}\"|" k8s-manifests/ingress.yaml

# 6. K8s 배포
echo "[6/7] K8s 리소스 배포 중..."
kubectl apply -f k8s-manifests/namespace.yaml
kubectl apply -f k8s-manifests/secrets.yaml
kubectl apply -f k8s-manifests/auth/
kubectl apply -f k8s-manifests/team/
kubectl apply -f k8s-manifests/core/
kubectl apply -f k8s-manifests/ingress.yaml

# 7. 상태 확인
echo "[7/7] 배포 상태 확인 중..."
echo ""
echo "--- Pods ---"
kubectl get pods -n serve
echo ""
echo "--- Services ---"
kubectl get svc -n serve
echo ""
echo "--- Ingress ---"
kubectl get ingress -n serve
echo ""
echo "========================================="
echo "배포 완료! ALB DNS가 나타나면 접근 가능합니다."
echo "ALB 프로비저닝에 2-3분 소요될 수 있습니다."
echo "========================================="
