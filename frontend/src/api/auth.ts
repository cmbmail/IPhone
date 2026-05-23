import { ApiGet, ApiPost } from './request'
import type { UserInfo } from '@/types/auth'

export interface LoginData {
  token: string
  user: UserInfo & { needsPasswordChange?: boolean; name?: string }
  expiresIn: number
}

export const authApi = {
  login: (data: { username: string; password: string }) => ApiPost<LoginData>('/auth/login', data),

  getCurrentUser: () => ApiGet<UserInfo>('/auth/me'),

  changePassword: (data: { oldPassword: string; newPassword: string }) =>
    ApiPost('/auth/change-password', data),

  health: () => ApiGet('/auth/health'),
}
