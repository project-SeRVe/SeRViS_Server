# ============================================================
# S3 버킷 + serve-core IRSA 설정
# serve-core가 S3에 암호화 바이너리를 저장하기 위한 리소스
# ============================================================

# --- S3 버킷 ---
resource "aws_s3_bucket" "servis_artifacts" {
  bucket = "servis-artifacts"

  tags = {
    Name = "${var.project_name}-artifacts"
  }
}

# 버킷 외부 공개 차단 (암호화 데이터이므로 퍼블릭 접근 완전 차단)
resource "aws_s3_bucket_public_access_block" "servis_artifacts" {
  bucket = aws_s3_bucket.servis_artifacts.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# --- serve-core용 S3 IAM Policy ---
resource "aws_iam_policy" "serve_core_s3" {
  name = "${var.project_name}-serve-core-s3-policy"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "s3:PutObject",
          "s3:GetObject",
          "s3:DeleteObject"
        ]
        Resource = "${aws_s3_bucket.servis_artifacts.arn}/*"
      },
      {
        Effect   = "Allow"
        Action   = ["s3:ListBucket"]
        Resource = aws_s3_bucket.servis_artifacts.arn
      }
    ]
  })
}

# --- serve-core IRSA Role (LB Controller와 동일한 패턴) ---
resource "aws_iam_role" "serve_core" {
  name = "${var.project_name}-serve-core-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = {
        Federated = local.oidc_provider_arn
      }
      Action = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = {
          "${local.oidc_provider_url}:aud" = "sts.amazonaws.com"
          "${local.oidc_provider_url}:sub" = "system:serviceaccount:serve:serve-core-sa"
        }
      }
    }]
  })

  tags = {
    Name = "${var.project_name}-serve-core-role"
  }
}

resource "aws_iam_role_policy_attachment" "serve_core_s3" {
  policy_arn = aws_iam_policy.serve_core_s3.arn
  role       = aws_iam_role.serve_core.name
}

# --- serve 네임스페이스 ---
resource "kubernetes_namespace" "serve" {
  metadata {
    name = "serve"
  }

  depends_on = [aws_eks_node_group.this]
}

# --- serve-core ServiceAccount (LB Controller와 동일한 패턴) ---
resource "kubernetes_service_account" "serve_core" {
  metadata {
    name      = "serve-core-sa"
    namespace = kubernetes_namespace.serve.metadata[0].name

    annotations = {
      "eks.amazonaws.com/role-arn" = aws_iam_role.serve_core.arn
    }
  }

  depends_on = [kubernetes_namespace.serve]
}
