export interface CostCenter {
  id: number
  parent_id: number | null
  name: string
  type: string
  branch_name: string | null
  org_code: string | null
  cost_center: string | null
  level: number
}
