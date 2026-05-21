export interface BillRaw {
  id: number
  billMonth: string
  chargeType: string
  phoneNumber: string
  employeeName: string | null
  orgName: string | null
  chargeAmount: number | null
  importedAt: string
}

export interface BillAllocation {
  id: number
  billMonth: string
  chargeType: string
  chargeAmount: number
  orgId: number | null
  orgName: string | null
  anomalyFlag: boolean
  createdAt: string
}
