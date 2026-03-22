# ============================================================
# ACM 인증서 + Route 53 도메인 연결
# ============================================================

# Hosted Zone 신규 생성 (기존 Hosted Zone이 없는 경우)
# ⚠️ terraform apply 후 아래 '네임서버 수동 업데이트' 단계 필수
resource "aws_route53_zone" "this" {
  name = "ssucheckmate.com"
  tags = { Name = "${var.project_name}-hosted-zone" }

  # terraform destroy 시에도 삭제하지 않음 → 네임서버 고정
  lifecycle {
    prevent_destroy = true
  }
}

# ACM 인증서 발급 (DNS 검증 방식)
resource "aws_acm_certificate" "this" {
  domain_name       = "api.ssucheckmate.com"
  validation_method = "DNS"

  lifecycle {
    create_before_destroy = true
  }

  tags = { Name = "${var.project_name}-acm-cert" }
}

# DNS 검증 레코드 자동 생성
resource "aws_route53_record" "acm_validation" {
  for_each = {
    for dvo in aws_acm_certificate.this.domain_validation_options : dvo.domain_name => {
      name   = dvo.resource_record_name
      record = dvo.resource_record_value
      type   = dvo.resource_record_type
    }
  }

  zone_id = aws_route53_zone.this.zone_id
  name    = each.value.name
  type    = each.value.type
  ttl     = 60
  records = [each.value.record]
}

# 검증 완료까지 대기 (terraform apply 중 블로킹)
# ⚠️ 네임서버가 업데이트되지 않으면 이 단계에서 타임아웃 발생
resource "aws_acm_certificate_validation" "this" {
  certificate_arn         = aws_acm_certificate.this.arn
  validation_record_fqdns = [for record in aws_route53_record.acm_validation : record.fqdn]
}

# ACM ARN을 SSM에 저장 (deploy.sh에서 참조)
resource "aws_ssm_parameter" "acm_arn" {
  name      = "/servis/acm-arn"
  type      = "String"
  value     = aws_acm_certificate_validation.this.certificate_arn
  overwrite = true
  tags      = { Name = "${var.project_name}-acm-arn" }
}

# Hosted Zone ID를 SSM에 저장 (deploy.sh에서 참조)
resource "aws_ssm_parameter" "hosted_zone_id" {
  name      = "/servis/hosted-zone-id"
  type      = "String"
  value     = aws_route53_zone.this.zone_id
  overwrite = true
  tags      = { Name = "${var.project_name}-hosted-zone-id" }
}
