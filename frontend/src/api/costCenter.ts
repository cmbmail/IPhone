import { request } from './request'

export const costCenterApi = {
  importCostCenter: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/orgs/import-cost-center', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
}
