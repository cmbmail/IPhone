import { ApiGet, ApiPost, ApiPut, ApiDelete, type PagedData } from './request'

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
  getAll: (params?: Record<string, unknown>) =>
    ApiGet<PagedData<Announcement>>('/announcements', { params }),

  getLatest: () => ApiGet<Announcement[]>('/announcements/latest'),

  getById: (id: number) => ApiGet<Announcement>(`/announcements/${id}`),

  create: (data: Record<string, unknown>) => ApiPost('/announcements', data),

  update: (id: number, data: Record<string, unknown>) => ApiPut(`/announcements/${id}`, data),

  delete: (id: number) => ApiDelete(`/announcements/${id}`),

  publish: (id: number) => ApiPost(`/announcements/${id}/publish`),

  archive: (id: number) => ApiPost(`/announcements/${id}/archive`),
}
