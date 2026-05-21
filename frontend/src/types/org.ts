export interface OrgStructure {
  id: number
  parentId: number | null
  name: string
  type: 'group' | 'subsidiary' | 'dept'
  level: number
  sortOrder: number
  path: string
  status: 'active' | 'inactive'
  createdBy: string
  createdAt: string
  updatedBy: string
  branchName: string | null
  orgCode: string | null
  costCenter: string | null
  children?: OrgStructure[]
}

export interface CreateOrgDTO {
  parentId: number | null
  name: string
  type: 'group' | 'subsidiary' | 'dept'
  status?: 'active' | 'inactive'
}

export interface UpdateOrgDTO {
  name?: string
  type?: 'group' | 'subsidiary' | 'dept'
  status?: 'active' | 'inactive'
}

export interface OrgTreeNode extends OrgStructure {
  children: OrgTreeNode[]
}
