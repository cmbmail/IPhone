import axios from 'axios'
import type { OrgStructure, CreateOrgDTO, UpdateOrgDTO } from '@/types/org'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

export const orgApi = {
  getTree: () => request.get<OrgStructure[]>('/orgs/tree'),

  getAll: () => request.get<OrgStructure[]>('/orgs'),

  getById: (id: number) => request.get<OrgStructure>(`/orgs/${id}`),

  getChildren: (id: number) => request.get<OrgStructure[]>(`/orgs/${id}/children`),

  create: (data: CreateOrgDTO) => request.post<OrgStructure>('/orgs', data),

  update: (id: number, data: UpdateOrgDTO) => request.put<OrgStructure>(`/orgs/${id}`, data),

  delete: (id: number) => request.delete(`/orgs/${id}`)
}
