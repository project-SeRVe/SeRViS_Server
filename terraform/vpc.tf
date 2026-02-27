# ============================================================
# VPC + 서브넷 + NAT Gateway + 라우팅
# ⚠️ 콘솔에서 겪은 문제: Regional NAT GW 생성 시 라우팅 테이블에
#    삭제된 NAT GW ID가 등록되어 blackhole 발생
#    → Terraform은 명시적으로 지정하므로 이 문제 없음
# ============================================================

# --- VPC ---
resource "aws_vpc" "this" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "${var.project_name}-vpc"
  }
}

# --- Internet Gateway ---
resource "aws_internet_gateway" "this" {
  vpc_id = aws_vpc.this.id

  tags = {
    Name = "${var.project_name}-igw"
  }
}

# --- Public Subnets (ALB용) ---
resource "aws_subnet" "public" {
  count = length(var.azs)

  vpc_id                  = aws_vpc.this.id
  cidr_block              = var.public_subnet_cidrs[count.index]
  availability_zone       = var.azs[count.index]
  map_public_ip_on_launch = true

  tags = {
    Name = "${var.project_name}-public-${var.azs[count.index]}"
    # ⚠️ 콘솔에서 겪은 문제: 클러스터명이 안 맞아서 서브넷 자동 감지 실패
    #    태그 값이 정확히 클러스터명과 일치해야 함
    "kubernetes.io/cluster/${var.cluster_name}" = "shared"
    "kubernetes.io/role/elb"                    = "1"
  }
}

# --- Private Subnets (EKS 노드 + RDS) ---
resource "aws_subnet" "private" {
  count = length(var.azs)

  vpc_id            = aws_vpc.this.id
  cidr_block        = var.private_subnet_cidrs[count.index]
  availability_zone = var.azs[count.index]

  tags = {
    Name = "${var.project_name}-private-${var.azs[count.index]}"
    "kubernetes.io/cluster/${var.cluster_name}" = "shared"
    "kubernetes.io/role/internal-elb"           = "1"
  }
}

# --- Elastic IP for NAT Gateway ---
resource "aws_eip" "nat" {
  domain = "vpc"

  tags = {
    Name = "${var.project_name}-nat-eip"
  }
}

# --- NAT Gateway (Public Subnet에 배치) ---
resource "aws_nat_gateway" "this" {
  allocation_id = aws_eip.nat.id
  subnet_id     = aws_subnet.public[0].id

  tags = {
    Name = "${var.project_name}-nat"
  }

  depends_on = [aws_internet_gateway.this]
}

# --- Public Route Table ---
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.this.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.this.id
  }

  tags = {
    Name = "${var.project_name}-public-rt"
  }
}

resource "aws_route_table_association" "public" {
  count = length(var.azs)

  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

# --- Private Route Table ---
# ⚠️ 콘솔에서 겪은 문제: 라우팅 테이블이 잘못된 NAT GW를 가리켜 blackhole
#    Terraform은 aws_nat_gateway.this.id를 직접 참조하므로 항상 올바른 대상
resource "aws_route_table" "private" {
  vpc_id = aws_vpc.this.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.this.id
  }

  tags = {
    Name = "${var.project_name}-private-rt"
  }
}

resource "aws_route_table_association" "private" {
  count = length(var.azs)

  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = aws_route_table.private.id
}
