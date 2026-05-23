import { request } from './request'

export const auditLogApi = {
  search: (params: { module?: string; operator?: string; page?: number; size?: number }) =>
    request.get('/audit-logs', { params }),
  getRecent: (page = 0, size = 20) => request.get('/audit-logs/recent', { params: { page, size } }),
}
