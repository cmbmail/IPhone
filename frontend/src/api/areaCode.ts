import { ApiGet, ApiPost, ApiPut, ApiDelete } from './request'
import type { AreaCodeOrgMapping, CreateAreaCodeMappingDTO } from '@/types/areaCode'

export const areaCodeApi = {
  getAll: () => ApiGet<AreaCodeOrgMapping[]>('/area-codes'),

  getById: (id: number) => ApiGet<AreaCodeOrgMapping>(`/area-codes/${id}`),

  getByAreaCode: (areaCode: string) => ApiGet<AreaCodeOrgMapping[]>(`/area-codes/area/${areaCode}`),

  getByOrg: (orgId: number) => ApiGet<AreaCodeOrgMapping[]>(`/area-codes/org/${orgId}`),

  matchOrg: (areaCode: string) => ApiGet<number>(`/area-codes/match/${areaCode}`),

  create: (data: CreateAreaCodeMappingDTO) => ApiPost<AreaCodeOrgMapping>('/area-codes', data),

  delete: (id: number) => ApiDelete(`/area-codes/${id}`),

  update: (id: number, data: Record<string, unknown>) => ApiPut(`/area-codes/${id}`, data),
}
