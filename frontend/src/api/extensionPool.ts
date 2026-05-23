import { request } from './request'
import type { ExtensionPool, CreateExtensionPoolDTO } from '@/types/extensionPool'

export const extensionPoolApi = {
  getAll: () => request.get<ExtensionPool[]>('/extension-pools'),

  getById: (id: number) => request.get<ExtensionPool>(`/extension-pools/${id}`),

  getByOrg: (orgId: number) => request.get<ExtensionPool[]>(`/extension-pools/org/${orgId}`),

  getUsage: (id: number) =>
    request.get<{
      poolId: number
      totalCount: number
      usedCount: number
      idleCount: number
      usageRate: number
      status: string
    }>(`/extension-pools/${id}/usage`),

  create: (data: CreateExtensionPoolDTO) => request.post<ExtensionPool>('/extension-pools', data),

  delete: (id: number) => request.delete(`/extension-pools/${id}`),

  update: (id: number, data: Record<string, unknown>) =>
    request.put(`/extension-pools/${id}`, data),
}
