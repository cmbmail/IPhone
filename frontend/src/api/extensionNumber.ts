import { request } from './request'

export interface ExtensionNumber {
  id: number
  extensionNumber: string
  status: 0 | 1 | 2
  employeeName: string | null
  branchName: string | null
  deptName: string | null
  deptOrgId: number | null
  phoneNumber: string | null
  workOrderId: number | null
}

export const extensionNumberApi = {
  search: (params: { keyword?: string; status?: string; deptOrgId?: number; page?: number; size?: number }) =>
    request.get('/extension-numbers', { params }),

  allocate: (id: number, data: { employeeName: string; deptOrgId?: number; deptName?: string; phoneNumber?: string }) =>
    request.post(`/extension-numbers/${id}/allocate`, null, { params: data }),

  reclaim: (id: number) =>
    request.post(`/extension-numbers/${id}/reclaim`),
}
