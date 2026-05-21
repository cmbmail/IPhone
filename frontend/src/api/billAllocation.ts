import { request } from './request'

export interface BillAllocation {
  id: number
  billMonth: string
  orgId: number
  orgName: string
  totalAmount: number
  allocateAmount: number
  diffAmount: number
  anomalyFlag: boolean
  anomalyReason: string
  adminConfirmOrg: string
  adminConfirmOrgBy: string
  adminConfirmOrgAt: string
  adminConfirmAmount: string
  adminConfirmAmountBy: string
  adminConfirmAmountAt: string
  financeConfirmAnomaly: string
  financeConfirmAnomalyBy: string
  financeConfirmAnomalyAt: string
  financeConfirmSubmit: string
  financeConfirmSubmitBy: string
  financeConfirmSubmitAt: string
  createdAt: string
  updatedAt: string
}

export interface AllocationRequest {
  billMonth: string
  page?: number
  size?: number
}

export const billAllocationApi = {
  getAllocations: (params: AllocationRequest) =>
    request.get<{ code: number; data: { content: BillAllocation[]; total_elements: number } }>('/bill-allocations', { params }),

  getAnomalies: (params: AllocationRequest) =>
    request.get<{ code: number; data: { content: BillAllocation[]; total_elements: number } }>('/bill-allocations/anomalies', { params }),

  getPendingOrgConfirm: (params: AllocationRequest) =>
    request.get<{ code: number; data: { content: BillAllocation[]; total_elements: number } }>('/bill-allocations/pending-org-confirm', { params }),

  getPendingAmountConfirm: (params: AllocationRequest) =>
    request.get<{ code: number; data: { content: BillAllocation[]; total_elements: number } }>('/bill-allocations/pending-amount-confirm', { params }),

  getPendingFinanceConfirm: (params: AllocationRequest) =>
    request.get<{ code: number; data: { content: BillAllocation[]; total_elements: number } }>('/bill-allocations/pending-finance-confirm', { params }),

  getPendingSubmit: (params: AllocationRequest) =>
    request.get<{ code: number; data: { content: BillAllocation[]; total_elements: number } }>('/bill-allocations/pending-submit', { params }),

  confirmOrg: (id: number, status: string) =>
    request.post(`/bill-allocations/${id}/confirm-org`, null, { params: { status } }),

  confirmAmount: (id: number, status: string) =>
    request.post(`/bill-allocations/${id}/confirm-amount`, null, { params: { status } }),

  confirmAnomaly: (id: number, status: string) =>
    request.post(`/bill-allocations/${id}/confirm-anomaly`, null, { params: { status } }),

  submit: (id: number) =>
    request.post(`/bill-allocations/${id}/submit`),

  reject: (id: number, reason?: string) =>
    request.post(`/bill-allocations/${id}/reject`, null, { params: { reason } })
}