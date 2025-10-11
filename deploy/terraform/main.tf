###############################################################################
# CloudPlay infrastructure: an EKS cluster with two managed node groups —
#   * system pool  : runs the gateway / orchestrator / signaling / registry pods
#   * gpu pool     : GPU instances that host the WebRTC game streamers
#
# The GPU pool is sized between gpu_min_size and gpu_max_size and reclaimed by
# the Kubernetes Cluster Autoscaler. The session-orchestrator's autoscaler
# expresses session demand as a custom metric; combined with the streamer pods'
# GPU resource requests, this drives the GPU node group up and down.
###############################################################################

locals {
  name = var.cluster_name
  tags = merge({ Project = "cloudplay" }, var.tags)

  # /20 subnets carved out of the VPC CIDR, one per AZ.
  private_subnets = [for i, az in var.availability_zones : cidrsubnet(var.vpc_cidr, 4, i)]
  public_subnets  = [for i, az in var.availability_zones : cidrsubnet(var.vpc_cidr, 4, i + 8)]
}

# --------------------------------------------------------------------------- #
# Networking
# --------------------------------------------------------------------------- #
module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "~> 5.7"

  name = "${local.name}-vpc"
  cidr = var.vpc_cidr

  azs             = var.availability_zones
  private_subnets = local.private_subnets
  public_subnets  = local.public_subnets

  enable_nat_gateway   = true
  single_nat_gateway   = true
  enable_dns_hostnames = true

  # Tags required by the AWS load balancer controller & cluster autoscaler.
  public_subnet_tags = {
    "kubernetes.io/role/elb"                      = "1"
    "kubernetes.io/cluster/${var.cluster_name}"   = "shared"
  }
  private_subnet_tags = {
    "kubernetes.io/role/internal-elb"             = "1"
    "kubernetes.io/cluster/${var.cluster_name}"   = "shared"
  }

  tags = local.tags
}

# --------------------------------------------------------------------------- #
# EKS cluster + managed node groups
# --------------------------------------------------------------------------- #
module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "~> 20.8"

  cluster_name    = var.cluster_name
  cluster_version = var.cluster_version

  cluster_endpoint_public_access = true

  vpc_id     = module.vpc.vpc_id
  subnet_ids = module.vpc.private_subnets

  enable_irsa = true

  eks_managed_node_groups = {
    system = {
      ami_type       = "AL2_x86_64"
      instance_types = [var.system_instance_type]
      min_size       = var.system_min_size
      max_size       = var.system_max_size
      desired_size   = var.system_desired_size

      labels = {
        "cloudplay.io/pool" = "system"
      }
    }

    gpu = {
      # GPU-optimized AMI with the NVIDIA device plugin / drivers.
      ami_type       = "AL2_x86_64_GPU"
      instance_types = [var.gpu_instance_type]
      min_size       = var.gpu_min_size
      max_size       = var.gpu_max_size
      desired_size   = var.gpu_desired_size

      labels = {
        "cloudplay.io/pool" = "gpu"
        "nvidia.com/gpu"    = "present"
      }

      # Taint so only streamer pods (with a matching toleration) land on GPUs.
      taints = [{
        key    = "cloudplay.io/gpu"
        value  = "true"
        effect = "NO_SCHEDULE"
      }]

      # Let the cluster autoscaler manage this group.
      tags = {
        "k8s.io/cluster-autoscaler/enabled"                = "true"
        "k8s.io/cluster-autoscaler/${var.cluster_name}"    = "owned"
      }
    }
  }

  tags = local.tags
}

# --------------------------------------------------------------------------- #
# Kubernetes provider wired to the new cluster (for bootstrapping the namespace)
# --------------------------------------------------------------------------- #
provider "kubernetes" {
  host                   = module.eks.cluster_endpoint
  cluster_ca_certificate = base64decode(module.eks.cluster_certificate_authority_data)

  exec {
    api_version = "client.authentication.k8s.io/v1beta1"
    command     = "aws"
    args        = ["eks", "get-token", "--cluster-name", var.cluster_name, "--region", var.region]
  }
}

resource "kubernetes_namespace" "cloudplay" {
  metadata {
    name = "cloudplay"
    labels = {
      "app.kubernetes.io/part-of" = "cloudplay"
    }
  }

  depends_on = [module.eks]
}
