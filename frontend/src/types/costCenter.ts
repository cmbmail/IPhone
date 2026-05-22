export interface CostCenter {
  id: number
  parentId: number | null
  name: string
  type: number
  branchName: string | null
  orgCode: string | null
  costCenter: string | null
  level: number
}
