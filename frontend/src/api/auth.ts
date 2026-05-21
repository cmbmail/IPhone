import { request } from './request'

export const authApi = {
  login: (username: string, password: string) =>
    request.post('/auth/login', { username, password }),

  getCurrentUser: () =>
    request.get('/auth/me'),

  changePassword: (oldPassword: string, newPassword: string) =>
    request.post('/auth/change-password', { oldPassword, newPassword }),

  health: () =>
    request.get('/auth/health'),
}
