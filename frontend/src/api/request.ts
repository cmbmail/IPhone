import axios, { type AxiosRequestConfig } from 'axios'
import { message } from 'antd'

const baseURL = '/api'

/** Unified API response wrapper from backend: { code, message, data, timestamp } */
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
  timestamp?: number
}

/** Paged data wrapper from backend */
export interface PagedData<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

// Convert snake_case string to camelCase
function toCamelCase(str: string): string {
  return str.replace(/_([a-z])/g, (_, letter: string) => letter.toUpperCase())
}

// Recursively convert object keys from snake_case to camelCase
function keysToCamelCase(obj: unknown): unknown {
  if (obj === null || obj === undefined) return obj
  if (Array.isArray(obj)) return obj.map(keysToCamelCase)
  if (typeof obj === 'object') {
    const result: Record<string, unknown> = {}
    for (const key of Object.keys(obj as Record<string, unknown>)) {
      result[toCamelCase(key)] = keysToCamelCase((obj as Record<string, unknown>)[key])
    }
    return result
  }
  return obj
}

// Convert camelCase string to snake_case
function toSnakeCase(str: string): string {
  return str.replace(/[A-Z]/g, (letter) => `_${letter.toLowerCase()}`)
}

// Recursively convert object keys from camelCase to snake_case
function keysToSnakeCase(obj: unknown): unknown {
  if (obj === null || obj === undefined) return obj
  if (Array.isArray(obj)) return obj.map(keysToSnakeCase)
  if (typeof obj === 'object') {
    const result: Record<string, unknown> = {}
    for (const key of Object.keys(obj as Record<string, unknown>)) {
      result[toSnakeCase(key)] = keysToSnakeCase((obj as Record<string, unknown>)[key])
    }
    return result
  }
  return obj
}

const request = axios.create({
  baseURL,
  timeout: 10000,
})

// Read token from Zustand persist storage (auth-storage in localStorage)
function getStoredToken(): string | null {
  try {
    const raw = localStorage.getItem('auth-storage')
    if (raw) {
      const parsed = JSON.parse(raw)
      return parsed?.state?.token || null
    }
  } catch {
    // ignore
  }
  return null
}

// Update token in Zustand persist storage
function updateStoredToken(newToken: string): void {
  try {
    const raw = localStorage.getItem('auth-storage')
    if (raw) {
      const parsed = JSON.parse(raw)
      parsed.state.token = newToken
      localStorage.setItem('auth-storage', JSON.stringify(parsed))
    }
  } catch {
    // ignore
  }
}

request.interceptors.request.use(
  (config) => {
    const token = getStoredToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    // Convert request body keys from camelCase to snake_case for JSON payloads
    if (config.data && typeof config.data === 'object' && !(config.data instanceof FormData)) {
      config.data = keysToSnakeCase(config.data)
    }

    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response) => {
    // Convert all response data keys from snake_case to camelCase
    if (response.data) {
      response.data = keysToCamelCase(response.data)
    }
    return response
  },
  async (error) => {
    const originalRequest = error.config

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true
      try {
        const currentToken = getStoredToken()
        if (currentToken) {
          const refreshRes = await axios.post(`${baseURL}/auth/refresh`, null, {
            headers: { Authorization: `Bearer ${currentToken}` },
          })
          if (refreshRes.data?.data?.token) {
            const newToken = refreshRes.data.data.token
            updateStoredToken(newToken)
            originalRequest.headers.Authorization = `Bearer ${newToken}`
            return request(originalRequest)
          }
        }
      } catch {
        // Refresh failed, proceed to logout
      }
      // Clear auth and redirect to login
      localStorage.removeItem('auth-storage')
      localStorage.removeItem('expiresIn')
      localStorage.removeItem('loginTime')
      window.location.href = '/login'
    }

    if (error.response?.status === 403) {
      // Check if it's a force password change response
      const data = error.response.data
      if (data?.code === 1007) {
        window.location.href = '/login?forceChangePassword=true'
      } else {
        localStorage.removeItem('auth-storage')
        localStorage.removeItem('expiresIn')
        localStorage.removeItem('loginTime')
        window.location.href = '/login'
      }
    }

    // Network error / timeout handling
    if (!error.response) {
      message.error(
        error.code === 'ECONNABORTED' ? '请求超时，请稍后重试' : '网络连接异常，请检查网络'
      )
    } else if (error.response.status >= 500) {
      message.error('服务器错误，请稍后重试')
    } else if (error.response.status === 429) {
      message.warning('操作过于频繁，请稍后重试')
    }

    return Promise.reject(error)
  }
)

export { request }

// ---- Type-safe API helpers ----
// These automatically unwrap ApiResponse<T>.data so callers get T directly.

/** GET request, returns inner data directly */
export async function ApiGet<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  const res = await request.get<ApiResponse<T>>(url, config)
  return (res.data as ApiResponse<T>).data
}

/** POST request, returns inner data directly */
export async function ApiPost<T>(
  url: string,
  data?: unknown,
  config?: AxiosRequestConfig
): Promise<T> {
  const res = await request.post<ApiResponse<T>>(url, data, config)
  return (res.data as ApiResponse<T>).data
}

/** PUT request, returns inner data directly */
export async function ApiPut<T>(
  url: string,
  data?: unknown,
  config?: AxiosRequestConfig
): Promise<T> {
  const res = await request.put<ApiResponse<T>>(url, data, config)
  return (res.data as ApiResponse<T>).data
}

/** DELETE request, returns inner data directly */
export async function ApiDelete<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  const res = await request.delete<ApiResponse<T>>(url, config)
  return (res.data as ApiResponse<T>).data
}
