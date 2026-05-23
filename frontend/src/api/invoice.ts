import { ApiGet, ApiPost, ApiDelete, type PagedData } from './request'

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
}
