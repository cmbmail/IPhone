export interface Invoice {
  id: number
  invoiceNo: string
  billMonth: string
  sourceOrgId: number | null
  sourceOrgName: string | null
  recipientOrgId: number | null
  amount: number | null
  taxAmount: number | null
  ocrConfidence: number | null
  status: number
  invoiceDate: string | null
  distributeAt: string | null
  readAt: string | null
  confirmedAt: string | null
  createdBy: string
  createdAt: string
  updatedBy: string
  updatedAt: string
}
