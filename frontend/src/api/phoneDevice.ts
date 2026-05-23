import { ApiGet, ApiPost, ApiPut, ApiDelete, type PagedData } from './request'

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
    ApiGet<PagedData<PhoneDeviceDTO>>('/phone-devices', { params }),
  getById: (id: number) => ApiGet<PhoneDeviceDTO>(`/phone-devices/${id}`),
  getBoundPhones: (id: number) => ApiGet(`/phone-devices/${id}/phones`),
  getHistory: (id: number) => ApiGet(`/phone-devices/${id}/history`),
  create: (data: Record<string, unknown>) => ApiPost('/phone-devices', data),
  update: (id: number, data: Record<string, unknown>) => ApiPut(`/phone-devices/${id}`, data),
  assign: (id: number, data: Record<string, unknown>) =>
    ApiPost(`/phone-devices/${id}/assign`, data),
  reclaim: (id: number, data?: Record<string, unknown>) =>
    ApiPost(`/phone-devices/${id}/reclaim`, data),
  deactivate: (id: number, data?: Record<string, unknown>) =>
    ApiPost(`/phone-devices/${id}/deactivate`, data),
  reactivate: (id: number) => ApiPost(`/phone-devices/${id}/reactivate`),
  repair: (id: number, data?: Record<string, unknown>) =>
    ApiPost(`/phone-devices/${id}/repair`, data),
  repairDone: (id: number) => ApiPost(`/phone-devices/${id}/repair-done`),
  retire: (id: number, data?: Record<string, unknown>) =>
    ApiPost(`/phone-devices/${id}/retire`, data),
  bindPhone: (id: number, data: Record<string, unknown>) =>
    ApiPost(`/phone-devices/${id}/bind-phone`, data),
  unbindPhone: (id: number, phoneId: number) =>
    ApiDelete(`/phone-devices/${id}/unbind-phone/${phoneId}`),
}
