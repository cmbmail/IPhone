export interface SubsidiaryReconciliation {
  id: number
  billMonth: string
  subsidiaryOrgId: number
  totalAmount: number
  invoiceCount: number
  reconciliationStatus: number // 0=待确认 1=子公司已确认 2=集团已确认
  subsidiaryConfirmBy: string | null
  subsidiaryConfirmAt: string | null
  groupConfirmBy: string | null
  groupConfirmAt: string | null
  remark: string | null
  createdBy: string
  createdAt: string
  updatedBy: string
  updatedAt: string
}
