import { request } from './request'

export interface ReportData {
  [key: string]: any
}

export const reportApi = {
  getPhoneAssetReport: (billMonth: string) =>
    request.get<{ code: number; data: ReportData }>('/reports/phone-asset', { params: { billMonth } }),

  getBillAllocationReport: (billMonth: string) =>
    request.get<{ code: number; data: ReportData }>('/reports/bill-allocation', { params: { billMonth } }),

  getWorkOrderReport: (startTime: string, endTime: string) =>
    request.get<{ code: number; data: ReportData }>('/reports/work-order', { params: { startTime, endTime } }),

  getAnomalyBillReport: (billMonth: string) =>
    request.get<{ code: number; data: ReportData }>('/reports/anomaly-bill', { params: { billMonth } })
}