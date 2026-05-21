import { request } from './request'
import type { PhoneNumber, CreatePhoneDTO, UpdatePhoneDTO, PhoneAllocationRequest, PhoneReclaimRequest, PhoneStatusChangeRequest, PhoneSurrenderRequest, PhoneReserveRequest, PhoneChangeRequest } from '@/types/phone'

export const phoneApi = {
  getAll: (params?: { page?: number; size?: number }) =>
    request.get<{ code: number; data: { content: PhoneNumber[]; total_elements: number } }>('/phones', { params }),

  getById: (id: number) => request.get<PhoneNumber>(`/phones/${id}`),

  getByNumber: (phoneNumber: string) => request.get<PhoneNumber>(`/phones/number/${phoneNumber}`),

  getByUser: (userId: number) => request.get<PhoneNumber[]>(`/phones/user/${userId}`),

  getByStatus: (status: string, params?: { page?: number; size?: number }) =>
    request.get<{ code: number; data: { content: PhoneNumber[]; total_elements: number } }>(`/phones/status/${status}`, { params }),

  getHistory: (id: number, params?: { page?: number; size?: number }) =>
    request.get<{ code: number; data: { content: any[]; total_elements: number } }>(`/phones/${id}/history`, { params }),

  getIdle: () => request.get<PhoneNumber[]>('/phones/idle'),

  create: (data: CreatePhoneDTO) => request.post<PhoneNumber>('/phones', data),

  update: (id: number, data: UpdatePhoneDTO) => request.put<PhoneNumber>(`/phones/${id}`, data),

  allocate: (data: PhoneAllocationRequest) => request.post<PhoneNumber>('/phones/allocate', data),

  reclaim: (data: PhoneReclaimRequest) => request.post<PhoneNumber>('/phones/reclaim', data),

  changeStatus: (data: PhoneStatusChangeRequest) => request.post<PhoneNumber>('/phones/status', data),

  surrender: (data: PhoneSurrenderRequest) => request.post<any>('/phones/surrender', data),

  reserve: (data: PhoneReserveRequest) => request.post<PhoneNumber>('/phones/reserve', data),

  release: (data: PhoneReserveRequest) => request.post<PhoneNumber>('/phones/release', data),

  changeUser: (data: PhoneChangeRequest) => request.post<PhoneNumber>('/phones/change-user', data),

  changeOrg: (data: PhoneChangeRequest) => request.post<PhoneNumber>('/phones/change-org', data),

  changeNumber: (data: PhoneChangeRequest) => request.post<PhoneNumber>('/phones/change-number', data),

  changeExtension: (data: PhoneChangeRequest) => request.post<PhoneNumber>('/phones/change-extension', data),

  batchChange: (data: PhoneChangeRequest) => request.post<PhoneNumber>('/phones/batch-change', data)
}
