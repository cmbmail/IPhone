import { request } from './request'

export interface PhoneDeviceDTO {
  id: number
  macAddress: string
  model: string
  brand: string
  purchaseDate: string
  orgId: number
  orgName?: string
  assignedEmployeeNo: string
  assignedEmployeeName?: string
  status: number
  boundPhoneCount: number
  remark: string
  createdAt: string
  updatedAt: string
}

export const phoneDeviceApi = {
  getList: (params?: { page?: number; size?: number }) =>
    request.get('/phone-devices', { params }),
  getById: (id: number) =>
    request.get(`/phone-devices/${id}`),
  getBoundPhones: (id: number) =>
    request.get(`/phone-devices/${id}/phones`),
  getHistory: (id: number) =>
    request.get(`/phone-devices/${id}/history`),
  create: (data: Record<string, unknown>) =>
    request.post('/phone-devices', data),
  update: (id: number, data: Record<string, unknown>) =>
    request.put(`/phone-devices/${id}`, data),
  assign: (id: number, data: Record<string, unknown>) =>
    request.post(`/phone-devices/${id}/assign`, data),
  reclaim: (id: number, data?: Record<string, unknown>) =>
    request.post(`/phone-devices/${id}/reclaim`, data),
  deactivate: (id: number, data?: Record<string, unknown>) =>
    request.post(`/phone-devices/${id}/deactivate`, data),
  reactivate: (id: number) =>
    request.post(`/phone-devices/${id}/reactivate`),
  repair: (id: number, data?: Record<string, unknown>) =>
    request.post(`/phone-devices/${id}/repair`, data),
  repairDone: (id: number) =>
    request.post(`/phone-devices/${id}/repair-done`),
  retire: (id: number, data?: Record<string, unknown>) =>
    request.post(`/phone-devices/${id}/retire`, data),
  bindPhone: (id: number, data: Record<string, unknown>) =>
    request.post(`/phone-devices/${id}/bind-phone`, data),
  unbindPhone: (id: number, phoneId: number) =>
    request.delete(`/phone-devices/${id}/unbind-phone/${phoneId}`),
}
