import { request } from './request'

export const invoiceApi = {
  getList: (params?: { billMonth?: string; status?: string; page?: number; size?: number }) =>
    request.get('/invoices', { params }),
  getById: (id: number) => request.get(`/invoices/${id}`),
  confirm: (id: number) => request.post(`/invoices/${id}/confirm`),
  delete: (id: number) => request.delete(`/invoices/${id}`),
}
