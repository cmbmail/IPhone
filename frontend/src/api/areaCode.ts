import { request } from './request'
import type { AreaCodeOrgMapping, CreateAreaCodeMappingDTO } from '@/types/areaCode'

export const areaCodeApi = {
  getAll: () => request.get<AreaCodeOrgMapping[]>('/area-codes'),

  getById: (id: number) => request.get<AreaCodeOrgMapping>(`/area-codes/${id}`),

  getByAreaCode: (areaCode: string) => request.get<AreaCodeOrgMapping[]>(`/area-codes/area/${areaCode}`),

  getByOrg: (orgId: number) => request.get<AreaCodeOrgMapping[]>(`/area-codes/org/${orgId}`),

  matchOrg: (areaCode: string) => request.get<number>(`/area-codes/match/${areaCode}`),

  create: (data: CreateAreaCodeMappingDTO) => request.post<AreaCodeOrgMapping>('/area-codes', data),

  delete: (id: number) => request.delete(`/area-codes/${id}`)
}
