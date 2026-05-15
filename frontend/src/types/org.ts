export interface OrgStructure {
  id: number
  parentId: number | null
  name: string
  type: 'group' | 'subsidiary' | 'dept'
  level: number
  path: string
  status: 'active' | 'inactive'
  createdBy: string
  createdAt: string
  updatedBy: string
  updatedAt: string
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
