import { request } from './request'

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
  getReconciliations: (params: { billMonth: string; orgId?: number; page?: number; size?: number }) =>
    request.get<{ code: number; data: { content: SubsidiaryReconciliation[]; total_elements: number } }>('/reconciliations', { params }),

  getReconciliation: (id: number) =>
    request.get<{ code: number; data: SubsidiaryReconciliation }>(`/reconciliations/${id}`),

  getPending: (orgId: number) =>
    request.get<{ code: number; data: SubsidiaryReconciliation[] }>('/reconciliations/pending', { params: { orgId } }),

  generateReconciliation: (billMonth: string) =>
    request.post('/reconciliations/generate', null, { params: { billMonth } }),

  subsidiaryConfirm: (id: number) =>
    request.post(`/reconciliations/${id}/subsidiary-confirm`),

  groupConfirm: (id: number) =>
    request.post(`/reconciliations/${id}/group-confirm`),

  getSummary: (billMonth: string) =>
    request.get<{ code: number; data: ReconciliationSummary }>('/reconciliations/summary', { params: { billMonth } })
}