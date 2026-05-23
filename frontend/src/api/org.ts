import { request } from './request'
import type { OrgStructure, CreateOrgDTO, UpdateOrgDTO } from '@/types/org'

export const orgApi = {
  getTree: () => request.get<OrgStructure[]>('/orgs/tree'),

  getAll: () => request.get<OrgStructure[]>('/orgs'),

  getById: (id: number) => request.get<OrgStructure>(`/orgs/${id}`),

  getChildren: (id: number) => request.get<OrgStructure[]>(`/orgs/${id}/children`),

  create: (data: CreateOrgDTO) => request.post<OrgStructure>('/orgs', data),

  update: (id: number, data: UpdateOrgDTO) => request.put<OrgStructure>(`/orgs/${id}`, data),

  delete: (id: number) => request.delete(`/orgs/${id}`),

  importCostCenter: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/orgs/import-cost-center', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
}
