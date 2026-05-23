import { ApiGet, ApiPost, type PagedData } from './request'
import type { WorkOrder } from '@/types/workOrder'

export const workOrderApi = {
  getList: (params?: Record<string, unknown>) =>
    ApiGet<PagedData<WorkOrder>>('/work-orders', { params }),
  getById: (id: number) => ApiGet<WorkOrder>(`/work-orders/${id}`),
  create: (data: Record<string, unknown>) => ApiPost('/work-orders', data),
  accept: (id: number) => ApiPost(`/work-orders/${id}/accept`),
  process: (id: number) => ApiPost(`/work-orders/${id}/process`),
  complete: (id: number, remark?: string) =>
    ApiPost(`/work-orders/${id}/complete`, null, { params: { remark } }),
  reject: (id: number, reason: string) =>
    ApiPost(`/work-orders/${id}/reject`, null, { params: { reason } }),
  executeItem: (itemId: number) => ApiPost(`/work-orders/items/${itemId}/execute`),
  batchSplit: (id: number) => ApiPost(`/work-orders/${id}/batch-split`),
}
