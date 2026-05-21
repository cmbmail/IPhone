import axios from "axios"

const baseURL = import.meta.env.DEV ? "/api" : "/phonebiz"

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
  (response) => {
    // Convert all response data keys from snake_case to camelCase
    if (response.data) {
      response.data = keysToCamelCase(response.data)
    }
    return response
  },
  (error) => {
    if (error.response?.status === 401 || error.response?.status === 403) {
      localStorage.removeItem("token")
      localStorage.removeItem("expiresIn")
      localStorage.removeItem("auth-storage")
      window.location.href = "/login"
    }
    return Promise.reject(error)
  },
)

export { request }
