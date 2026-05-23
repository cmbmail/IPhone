import { ApiGet, ApiPost, type PagedData } from './request'

export interface SubsidiaryReconciliation {
  id: number
  billMonth: string
  orgId: number
  orgName: string
  totalPhoneCount: number
  totalBillAmount: number
  invoiceAmount: number
  diffAmount: number
  diffPercentage: number
  status: number
  subsidiaryConfirm: number
  subsidiaryConfirmBy: string
  subsidiaryConfirmAt: string
  groupConfirm: number
  groupConfirmBy: string
  groupConfirmAt: string
  createdAt: string
  updatedAt: string
}

export interface ReconciliationSummary {
  totalOrgs: number
  totalAmount: number
  matchedCount: number
  mismatchedCount: number
  pendingCount: number
}

export const reconciliationApi = {
  getReconciliations: (params: {
    billMonth: string
    orgId?: number
    page?: number
    size?: number
  }) => ApiGet<PagedData<SubsidiaryReconciliation>>('/reconciliations', { params }),

  getReconciliation: (id: number) => ApiGet<SubsidiaryReconciliation>(`/reconciliations/${id}`),

  getPending: (orgId: number) =>
    ApiGet<SubsidiaryReconciliation[]>('/reconciliations/pending', { params: { orgId } }),

  generateReconciliation: (billMonth: string) =>
    ApiPost('/reconciliations/generate', null, { params: { billMonth } }),

  subsidiaryConfirm: (id: number) => ApiPost(`/reconciliations/${id}/subsidiary-confirm`),

  groupConfirm: (id: number) => ApiPost(`/reconciliations/${id}/group-confirm`),

  getSummary: (billMonth: string) =>
    ApiGet<ReconciliationSummary>('/reconciliations/summary', { params: { billMonth } }),
}
