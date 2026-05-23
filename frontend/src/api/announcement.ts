import { request } from './request'

export interface Announcement {
  id: number
  title: string
  content: string
  announcementType: number
  priority: number
  status: number
  createdBy: string
  createdAt: string
  updatedAt: string
}

export const announcementApi = {
  getAll: (params?: Record<string, unknown>) => request.get('/announcements', { params }),

  getLatest: () => request.get('/announcements/latest'),

  getById: (id: number) => request.get(`/announcements/${id}`),

  create: (data: Record<string, unknown>) => request.post('/announcements', data),

  update: (id: number, data: Record<string, unknown>) => request.put(`/announcements/${id}`, data),

  delete: (id: number) => request.delete(`/announcements/${id}`),

  publish: (id: number) => request.post(`/announcements/${id}/publish`),

  archive: (id: number) => request.post(`/announcements/${id}/archive`),
}
