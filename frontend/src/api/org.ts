import { request, ApiGet, ApiPost, ApiPut, ApiDelete } from './request'
import type { OrgStructure, CreateOrgDTO, UpdateOrgDTO } from '@/types/org'

export const orgApi = {
  getTree: () => ApiGet<OrgStructure[]>('/orgs/tree'),

  getAll: () => ApiGet<OrgStructure[]>('/orgs'),

  getById: (id: number) => ApiGet<OrgStructure>(`/orgs/${id}`),

  getChildren: (id: number) => ApiGet<OrgStructure[]>(`/orgs/${id}/children`),

  create: (data: CreateOrgDTO) => ApiPost<OrgStructure>('/orgs', data),

  update: (id: number, data: UpdateOrgDTO) => ApiPut<OrgStructure>(`/orgs/${id}`, data),

  delete: (id: number) => ApiDelete(`/orgs/${id}`),

  importCostCenter: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/orgs/import-cost-center', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
}
