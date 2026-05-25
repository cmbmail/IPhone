import { request, ApiDelete } from './request'

export interface BillImportResult {
  importedCount: number
  billMonth: string
  monthDistribution: Record<string, number>
  allocationStatus?: string
}

export const billApi = {
  importBills: (billMonth: string, file: File, operator?: string) => {
    const formData = new FormData()
    formData.append('billMonth', billMonth)
    formData.append('file', file)
    if (operator) formData.append('operator', operator)
    return request.post<BillImportResult>('/bills/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  importAndAllocate: (billMonth: string, file: File, operator?: string) => {
    const formData = new FormData()
    formData.append('billMonth', billMonth)
    formData.append('file', file)
    if (operator) formData.append('operator', operator)
    return request.post<BillImportResult>('/bills/import-and-allocate', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  delete: (billMonth: string, chargeType: string, password: string) =>
    ApiDelete('/bills', { data: { billMonth, chargeType, password } }),
}
