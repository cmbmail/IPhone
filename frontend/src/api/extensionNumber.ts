import { ApiGet, ApiPost, type PagedData } from './request'

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
  search: (params: {
    keyword?: string
    status?: string
    deptOrgId?: number
    page?: number
    size?: number
  }) => ApiGet<PagedData<ExtensionNumber>>('/extension-numbers', { params }),

  allocate: (
    id: number,
    data: { employeeName: string; deptOrgId?: number; deptName?: string; phoneNumber?: string }
  ) => ApiPost(`/extension-numbers/${id}/allocate`, null, { params: data }),

  reclaim: (id: number) => ApiPost(`/extension-numbers/${id}/reclaim`),
}
