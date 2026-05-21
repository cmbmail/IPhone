import { request } from './request'

export const notificationApi = {
  // H-08: userId removed - server now resolves from JWT token to prevent IDOR
  getList: (page = 0, size = 20) =>
    request.get('/notifications', { params: { page, size } }),
  getUnreadCount: () =>
    request.get('/notifications/unread-count'),
  markAsRead: (id: number) =>
    request.post(`/notifications/${id}/read`),
  markAllAsRead: () =>
    request.post('/notifications/read-all'),
}
