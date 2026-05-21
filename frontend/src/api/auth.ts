import axios from "axios"
import type { LoginRequest, LoginResponse } from "@/types/auth"

const baseURL = import.meta.env.DEV ? "/api" : "/phonebiz"

const request = axios.create({
  baseURL,
  timeout: 10000,
})

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("token")
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

request.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem("token")
      localStorage.removeItem("expiresIn")
      localStorage.removeItem("auth-storage")
      window.location.href = "/login"
    }
    return Promise.reject(error)
  },
)

export const authApi = {
  login: (data: LoginRequest) =>
    request.post<LoginResponse>("/auth/login", data),

  getCurrentUser: () =>
    request.get<LoginResponse["user"]>("/auth/me"),

  changePassword: (data: { oldPassword: string; newPassword: string }) =>
    request.post("/auth/change-password", data),

  health: () =>
    request.get<{ status: string; service: string }>("/auth/health"),
}
