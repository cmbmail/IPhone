import axios from 'axios'
import type { ExtensionPool, CreateExtensionPoolDTO } from '@/types/extensionPool'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

export const extensionPoolApi = {
  getAll: () => request.get<ExtensionPool[]>('/extension-pools'),

  getById: (id: number) => request.get<ExtensionPool>(`/extension-pools/${id}`),

  getByOrg: (orgId: number) => request.get<ExtensionPool[]>(`/extension-pools/org/${orgId}`),

  getUsage: (id: number) => request.get<{ poolId: number; totalCount: number; usedCount: number; idleCount: number; usageRate: number; status: string }>(`/extension-pools/${id}/usage`),

  create: (data: CreateExtensionPoolDTO) => request.post<ExtensionPool>('/extension-pools', data),

  delete: (id: number) => request.delete(`/extension-pools/${id}`)
}
