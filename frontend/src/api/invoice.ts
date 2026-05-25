import { ApiGet, ApiPost, ApiDelete, type PagedData, request } from './request'

export interface Invoice {
  id: number
  invoiceNo: string
  sourceOrgName: string
  amount: number | null
  taxAmount: number | null
  invoiceDate: string
  billMonth: string
  status: number
  employeeName?: string
  employeeNo?: string
  orgName?: string
  createdAt?: string
}

export interface BatchUploadResult {
  total: number
  success: number
  failed: number
  details: Array<{
    fileName: string
    status: 'success' | 'failed'
    reason?: string
    invoiceId?: number
    invoiceNo?: string
    matchedOrg?: string
  }>
}

export const invoiceApi = {
  getList: (params?: {
    billMonth?: string
    status?: string | number
    page?: number
    size?: number
  }) => ApiGet<PagedData<Invoice>>('/invoices', { params }),
  getById: (id: number) => ApiGet<Invoice>(`/invoices/${id}`),
  confirm: (id: number) => ApiPost(`/invoices/${id}/confirm`),
  delete: (id: number) => ApiDelete(`/invoices/${id}`),
  batchUpload: (files: File[], billMonth: string) => {
    const formData = new FormData()
    files.forEach((file) => formData.append('files', file))
    formData.append('billMonth', billMonth)
    return request.post<unknown, BatchUploadResult>('/invoices/batch-upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
}
