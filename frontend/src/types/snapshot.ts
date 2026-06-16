export interface PhoneSnapshot {
  id: number
  snapshotMonth: string
  billMonth: string | null
  phoneId: number
  phoneNumber: string
  extensionNumber: string | null
  status: number
  orgId: number | null
  branchOrgId: number | null
  orgName: string | null
  branchName: string | null
  costCenterCode: string | null
  employeeNo: string | null
  employeeName: string | null
  isSurrendered: boolean
  isAllocatable: boolean
  allocationStatus: number
  createdAt: string
}

export interface SnapshotStats {
  total: number
  byStatus: Record<number, number>
  byAllocationStatus: Record<number, number>
  byBranch: Record<string, number>
}

// Status labels
export const SNAPSHOT_STATUS_LABELS: Record<number, string> = {
  0: '空闲',
  1: '在用',
  2: '停机',
  3: '注销',
  4: '预留',
  5: '禁用',
}

export const SNAPSHOT_STATUS_COLORS: Record<number, string> = {
  0: 'default',
  1: 'green',
  2: 'orange',
  3: 'red',
  4: 'blue',
  5: 'default',
}

export const ALLOC_STATUS_LABELS: Record<number, string> = {
  0: '未分摊',
  1: '已分摊',
  2: '分摊异常',
}
