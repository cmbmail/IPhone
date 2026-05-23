import { ApiGet, ApiPost, type PagedData } from './request'

export interface AppNotification {
  id: number
  title: string
  content: string
  message: string
  type: string
  status: number
  isRead: boolean
  createdAt: string
}

export const notificationApi = {
  getList: (page = 0, size = 20) =>
    ApiGet<PagedData<AppNotification>>('/notifications', { params: { page, size } }),
  getUnreadCount: () => ApiGet<number>('/notifications/unread-count'),
  markAsRead: (id: number) => ApiPost(`/notifications/${id}/read`),
  markAllAsRead: () => ApiPost('/notifications/read-all'),
}
