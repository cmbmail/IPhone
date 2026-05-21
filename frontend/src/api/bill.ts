import { request } from './request'

export interface BillUploadResponse {
  code: number
  data: number
  message: string
}

export const billApi = {
  importBills: (billMonth: string, file: File, operator?: string) => {
    const formData = new FormData()
    formData.append('billMonth', billMonth)
    formData.append('file', file)
    if (operator) formData.append('operator', operator)
    return request.post<BillUploadResponse>('/bills/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  importAndAllocate: (billMonth: string, file: File, operator?: string) => {
    const formData = new FormData()
    formData.append('billMonth', billMonth)
    formData.append('file', file)
    if (operator) formData.append('operator', operator)
    return request.post('/bills/import-and-allocate', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  delete: (billMonth: string, chargeType: string, password: string) => request.delete('/bills', { data: { billMonth, chargeType, password } }),
}