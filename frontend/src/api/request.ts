import axios from "axios"

const baseURL = "/api"

// Convert snake_case string to camelCase
function toCamelCase(str: string): string {
  return str.replace(/_([a-z])/g, (_, letter: string) => letter.toUpperCase())
}

// Recursively convert object keys from snake_case to camelCase
function keysToCamelCase(obj: any): any {
  if (obj === null || obj === undefined) return obj
  if (Array.isArray(obj)) return obj.map(keysToCamelCase)
  if (typeof obj === "object") {
    const result: any = {}
    for (const key of Object.keys(obj)) {
      result[toCamelCase(key)] = keysToCamelCase(obj[key])
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
    const raw = localStorage.getItem("auth-storage")
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
    const raw = localStorage.getItem("auth-storage")
    if (raw) {
      const parsed = JSON.parse(raw)
      parsed.state.token = newToken
      localStorage.setItem("auth-storage", JSON.stringify(parsed))
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
    return config
  },
  (error) => Promise.reject(error),
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
            headers: { Authorization: `Bearer ${currentToken}` }
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
      localStorage.removeItem("auth-storage")
      localStorage.removeItem("expiresIn")
      localStorage.removeItem("loginTime")
      window.location.href = "/login"
    }

    if (error.response?.status === 403) {
      // Check if it's a force password change response
      const data = error.response.data
      if (data?.code === 1007) {
        window.location.href = "/login?forceChangePassword=true"
      } else {
        localStorage.removeItem("auth-storage")
        localStorage.removeItem("expiresIn")
        localStorage.removeItem("loginTime")
        window.location.href = "/login"
      }
    }

    return Promise.reject(error)
  },
)

export { request }
