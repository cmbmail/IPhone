export interface BillRaw {
  id: number
  billMonth: string
  chargeType: number
  phoneNumber: string
  employeeName: string | null
  orgName: string | null
  chargeAmount: number | null
  importStatus: number
  importedAt: string
}

export interface BillAllocation {
  id: number
  billMonth: string
  chargeType: number
  chargeAmount: number
  orgId: number | null
  orgName: string | null
  anomalyFlag: boolean
  createdAt: string
}
