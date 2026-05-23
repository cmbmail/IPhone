export interface OrgStructure {
  id: number
  parentId: number | null
  name: string
  type: 1 | 2 | 3
  level: number
  sortOrder: number
  path: string
  status: 0 | 1
  createdBy: string
  createdAt: string
  updatedBy: string
  branchName: string | null
  orgCode: string | null
  costCenterCode: string | null
  children?: OrgStructure[]
}

export interface CreateOrgDTO {
  parentId: number | null
  name: string
  type: 1 | 2 | 3
  status?: 0 | 1
}

export interface UpdateOrgDTO {
  name?: string
  type?: 1 | 2 | 3
  status?: 0 | 1
}
