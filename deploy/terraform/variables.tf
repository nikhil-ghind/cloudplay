variable "region" {
  description = "AWS region to deploy the CloudPlay cluster into."
  type        = string
  default     = "us-east-1"
}

variable "cluster_name" {
  description = "Name of the EKS cluster."
  type        = string
  default     = "cloudplay"
}

variable "cluster_version" {
  description = "Kubernetes control plane version."
  type        = string
  default     = "1.29"
}

variable "vpc_cidr" {
  description = "CIDR block for the cluster VPC."
  type        = string
  default     = "10.42.0.0/16"
}

variable "availability_zones" {
  description = "AZs to spread the cluster across."
  type        = list(string)
  default     = ["us-east-1a", "us-east-1b", "us-east-1c"]
}

# ---- System / control-plane node pool (runs the Spring Boot microservices) ----
variable "system_instance_type" {
  description = "Instance type for the general-purpose services node pool."
  type        = string
  default     = "m6i.xlarge"
}

variable "system_min_size" {
  type    = number
  default = 2
}

variable "system_max_size" {
  type    = number
  default = 6
}

variable "system_desired_size" {
  type    = number
  default = 3
}

# ---- GPU node pool (hosts the game streamers) ----
variable "gpu_instance_type" {
  description = "GPU instance type for streaming nodes (e.g. g5.xlarge = 1x A10G)."
  type        = string
  default     = "g5.xlarge"
}

variable "gpu_min_size" {
  description = "Minimum GPU nodes; the cluster autoscaler scales between min/max based on session demand surfaced by the orchestrator."
  type        = number
  default     = 1
}

variable "gpu_max_size" {
  type    = number
  default = 50
}

variable "gpu_desired_size" {
  type    = number
  default = 2
}

variable "tags" {
  description = "Additional tags applied to all resources."
  type        = map(string)
  default     = {}
}
