import { request } from './request'

export const notificationApi = {
  getList: (userId: number, page = 0, size = 20) =>
    request.get('/notifications', { params: { userId, page, size } }),
  getUnreadCount: (userId: number) =>
    request.get('/notifications/unread-count', { params: { userId } }),
  markAsRead: (id: number) =>
    request.post(`/notifications/${id}/read`),
  markAllAsRead: (userId: number) =>
    request.post('/notifications/read-all', null, { params: { userId } }),
}
