# ============================================================
# AWS Load Balancer Controller (Helm)
# ⚠️ 콘솔에서 겪은 문제:
#   1. VPC ID 자동 감지 실패 (CrashLoopBackOff)
#      → vpcId, region 직접 지정
#   2. IAM 권한 부족 (DescribeListenerAttributes AccessDenied)
#      → IRSA로 전용 IAM Role 연결
# ============================================================

# --- Service Account for LB Controller ---
resource "kubernetes_service_account" "lb_controller" {
  metadata {
    name      = "aws-load-balancer-controller"
    namespace = "kube-system"

    annotations = {
      "eks.amazonaws.com/role-arn" = aws_iam_role.lb_controller.arn
    }

    labels = {
      "app.kubernetes.io/component" = "controller"
      "app.kubernetes.io/name"      = "aws-load-balancer-controller"
    }
  }

  depends_on = [aws_eks_node_group.this]
}

# --- Helm Release ---
resource "helm_release" "lb_controller" {
  name       = "aws-load-balancer-controller"
  repository = "https://aws.github.io/eks-charts"
  chart      = "aws-load-balancer-controller"
  namespace  = "kube-system"
  version    = "1.11.0"

  set {
    name  = "clusterName"
    value = var.cluster_name
  }

  set {
    name  = "serviceAccount.create"
    value = "false"
  }

  set {
    name  = "serviceAccount.name"
    value = "aws-load-balancer-controller"
  }

  # ⚠️ 콘솔에서 겪은 문제: VPC ID 자동 감지 실패
  set {
    name  = "vpcId"
    value = aws_vpc.this.id
  }

  set {
    name  = "region"
    value = var.aws_region
  }

  depends_on = [
    kubernetes_service_account.lb_controller,
    aws_eks_node_group.this,
  ]
}
