import { request } from './request'

export const workOrderApi = {
  getList: (params?: { status?: string; page?: number; size?: number }) =>
    request.get('/work-orders', { params }),
  getById: (id: number) =>
    request.get(`/work-orders/${id}`),
  create: (data: Record<string, unknown>) =>
    request.post('/work-orders', data),
  accept: (id: number, handlerId: number, handlerName: string) =>
    request.post(`/work-orders/${id}/accept`, null, { params: { handlerId, handlerName } }),
  process: (id: number) =>
    request.post(`/work-orders/${id}/process`),
  complete: (id: number, remark?: string) =>
    request.post(`/work-orders/${id}/complete`, null, { params: { remark } }),
  reject: (id: number, reason: string) =>
    request.post(`/work-orders/${id}/reject`, null, { params: { reason } }),
  executeItem: (itemId: number) =>
    request.post(`/work-orders/items/${itemId}/execute`),
  batchSplit: (id: number) =>
    request.post(`/work-orders/${id}/batch-split`),
}
