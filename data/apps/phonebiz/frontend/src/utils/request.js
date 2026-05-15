import axios from 'axios'
import { message } from 'antd'

const baseURL = '/phonebiz'

const instance = axios.create({
  baseURL,
  timeout: 30000
})

instance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

instance.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) {
      return res
    }
    if (res.code === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      localStorage.removeItem('realName')
      window.location.href = '/login'
      message.error('登录已过期，请重新登录')
      return Promise.reject(new Error(res.message))
    }
    message.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message))
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      localStorage.removeItem('realName')
      window.location.href = '/login'
      message.error('登录已过期，请重新登录')
    } else {
      message.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default instance
