export interface OrgStructure {
  id: number
  parent_id: number | null
  name: string
  type: 'group' | 'subsidiary' | 'dept'
  level: number
  sort_order: number
  path: string
  status: 'active' | 'inactive'
  created_by: string
  created_at: string
  updated_by: string
  branch_name: string | null
  org_code: string | null
  cost_center: string | null
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
