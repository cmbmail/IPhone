import { request } from './request'

export interface ExtensionNumber {
  id: number
  extensionNumber: string
  status: 'ALLOCATED' | 'IDLE' | 'AVAILABLE'
  userName: string | null
  deptName: string | null
  deptOrgId: number | null
  phoneNumber: string | null
  workOrderId: number | null
}

export const extensionNumberApi = {
  search: (params: { keyword?: string; status?: string; deptOrgId?: number; page?: number; size?: number }) =>
    request.get('/extension-numbers', { params }),

  allocate: (id: number, data: { userName: string; deptOrgId?: number; deptName?: string; phoneNumber?: string }) =>
    request.post(`/extension-numbers/${id}/allocate`, null, { params: data }),

  reclaim: (id: number) =>
    request.post(`/extension-numbers/${id}/reclaim`),
}
