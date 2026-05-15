export interface CostCenter {
  id: number
  orgId: number
  costCenterName: string
  costCenterCode: string
  status: 'active' | 'inactive'
  createdBy: string
  createdAt: string
  updatedBy: string
  updatedAt: string
}

export interface CreateCostCenterDTO {
  orgId: number
  costCenterName: string
  costCenterCode: string
  status?: 'active' | 'inactive'
}

export interface UpdateCostCenterDTO {
  costCenterName?: string
  costCenterCode?: string
  status?: 'active' | 'inactive'
}
