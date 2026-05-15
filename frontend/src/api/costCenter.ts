import axios from 'axios'
import type { CostCenter, CreateCostCenterDTO, UpdateCostCenterDTO } from '@/types/costCenter'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

export const costCenterApi = {
  getAll: () => request.get<CostCenter[]>('/cost-centers'),

  getById: (id: number) => request.get<CostCenter>(`/cost-centers/${id}`),

  getByOrg: (orgId: number) => request.get<CostCenter[]>(`/cost-centers/org/${orgId}`),

  create: (data: CreateCostCenterDTO) => request.post<CostCenter>('/cost-centers', data),

  update: (id: number, data: UpdateCostCenterDTO) =>
    request.put<CostCenter>(`/cost-centers/${id}`, data),

  delete: (id: number) => request.delete(`/cost-centers/${id}`)
}
