# ============================================================
# ECR Repositories
# ============================================================

resource "aws_ecr_repository" "services" {
  for_each = var.services

  name                 = "serve-${each.key}"
  image_tag_mutability = "MUTABLE"
  force_delete         = true

  image_scanning_configuration {
    scan_on_push = false
  }

  tags = {
    Name = "serve-${each.key}"
  }
}

# --- ECR Lifecycle Policy (오래된 이미지 자동 정리) ---
resource "aws_ecr_lifecycle_policy" "services" {
  for_each = aws_ecr_repository.services

  repository = each.value.name

  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Keep last 10 images"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = 10
      }
      action = {
        type = "expire"
      }
    }]
  })
}
