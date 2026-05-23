import { request } from './request'

export interface PhoneOwnership {
  id: number
  phoneNumber: string
  branchOrgId: number | null
  branchName: string | null
  deptOrgId: number | null
  deptName: string | null
  remark: string | null
}

export interface ImportCompareItem {
  phoneNumber: string
  branchName: string
  deptName: string
  remark: string
  existingBranchName: string | null
  existingDeptName: string | null
  existingRemark: string | null
  isNew: boolean
  hasDiff: boolean
}

export const phoneOwnershipApi = {
  search: (params: { keyword?: string; branchOrgId?: number; page?: number; size?: number }) =>
    request.get('/phone-ownership', { params }),

  update: (
    id: number,
    data: { branchOrgId?: number | null; deptOrgId?: number | null; remark?: string }
  ) => request.put(`/phone-ownership/${id}`, null, { params: data }),

  importCompare: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/phone-ownership/import-compare', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  importConfirm: (items: ImportCompareItem[]) =>
    request.post('/phone-ownership/import-confirm', items),

  exportUrl: () => '/api/phone-ownership/export',
}
