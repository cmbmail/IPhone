import { ApiGet, type PagedData } from './request'
import type { AuditLogEntry } from '@/types/auditLog'

export const auditLogApi = {
  search: (params: { module?: string; operator?: string; page?: number; size?: number }) =>
    ApiGet<PagedData<AuditLogEntry>>('/audit-logs', { params }),
  getRecent: (page = 0, size = 20) =>
    ApiGet<PagedData<AuditLogEntry>>('/audit-logs/recent', { params: { page, size } }),
}
