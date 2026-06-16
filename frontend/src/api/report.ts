import { ApiGet } from './request'

export interface ReportData {
  [key: string]: string | number | boolean | null
}

export const reportApi = {
  getPhoneAssetReport: (billMonth: string) =>
    ApiGet<ReportData>('/reports/phone-asset', { params: { billMonth } }),

  getBillAllocationReport: (billMonth: string) =>
    ApiGet<ReportData>('/reports/bill-allocation', { params: { billMonth } }),

  getWorkOrderReport: (startTime: string, endTime: string) =>
    ApiGet<ReportData>('/reports/work-order', { params: { startTime, endTime } }),

  getAnomalyBillReport: (billMonth: string) =>
    ApiGet<ReportData>('/reports/anomaly-bill', { params: { billMonth } }),
}
