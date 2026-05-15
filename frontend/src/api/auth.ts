import axios from 'axios'
import type { LoginRequest, LoginResponse } from '@/types/auth'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

request.interceptors.response.use(
  response => {
    return response
  },
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export const authApi = {
  login: (data: LoginRequest) => request.post<LoginResponse>('/auth/login', data),

  getCurrentUser: () => request.get<LoginResponse.UserInfo>('/auth/me'),

  changePassword: (data: { oldPassword: string; newPassword: string }) =>
    request.post('/auth/change-password', data),

  health: () => request.get<{ status: string; service: string }>('/auth/health')
}
