import { request } from './request'

export const deviceApi = {
  getList: (params?: { page?: number; size?: number; status?: string }) =>
    request.get('/devices', { params }),
  getById: (id: number) =>
    request.get(`/devices/${id}`),
  create: (data: Record<string, unknown>) =>
    request.post('/devices', data),
  update: (id: number, data: Record<string, unknown>) =>
    request.put(`/devices/${id}`, data),
  delete: (id: number) =>
    request.delete(`/devices/${id}`),
  updateStatus: (deviceId: string, status: string) =>
    request.post(`/devices/${deviceId}/status`, null, { params: { status } }),
  reboot: (deviceId: string) =>
    request.post(`/device-operations/${deviceId}/reboot`),
  syncConfig: (deviceId: string) =>
    request.post(`/device-operations/${deviceId}/sync-config`),
}
