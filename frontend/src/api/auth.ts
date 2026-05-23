import { request } from './request'

export const authApi = {
  login: (data: { username: string; password: string }) => request.post('/auth/login', data),

  getCurrentUser: () => request.get('/auth/me'),

  changePassword: (data: { oldPassword: string; newPassword: string }) =>
    request.post('/auth/change-password', data),

  health: () => request.get('/auth/health'),
}
