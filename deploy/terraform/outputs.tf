output "cluster_name" {
  description = "Name of the EKS cluster."
  value       = module.eks.cluster_name
}

output "cluster_endpoint" {
  description = "EKS API server endpoint."
  value       = module.eks.cluster_endpoint
}

output "cluster_certificate_authority_data" {
  description = "Base64-encoded cluster CA certificate."
  value       = module.eks.cluster_certificate_authority_data
  sensitive   = true
}

output "region" {
  description = "AWS region the cluster runs in."
  value       = var.region
}

output "vpc_id" {
  description = "ID of the cluster VPC."
  value       = module.vpc.vpc_id
}

output "gpu_node_group_name" {
  description = "Name of the GPU managed node group hosting the streamers."
  value       = "gpu"
}

output "configure_kubectl" {
  description = "Command to update your kubeconfig for this cluster."
  value       = "aws eks update-kubeconfig --region ${var.region} --name ${var.cluster_name}"
}
