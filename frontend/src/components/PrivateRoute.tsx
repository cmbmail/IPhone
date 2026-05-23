import { Navigate } from 'react-router-dom'
import { useAuthStore } from '@/stores/authStore'
import { useEffect, useState } from 'react'
import { authApi } from '@/api/auth'

interface PrivateRouteProps {
  children: React.ReactNode
}

export const PrivateRoute = ({ children }: PrivateRouteProps) => {
  const { isAuthenticated, token, setAuth, clearAuth } = useAuthStore()
  const [verifying, setVerifying] = useState(isAuthenticated && !!token)

  useEffect(() => {
    if (!isAuthenticated || !token) {
      setVerifying(false)
      return
    }

    // Check token expiry from expiresIn in localStorage
    const expiresIn = localStorage.getItem('expiresIn')
    if (expiresIn) {
      const loginTime = localStorage.getItem('loginTime')
      if (loginTime) {
        const elapsed = Date.now() - parseInt(loginTime, 10)
        const expiry = parseInt(expiresIn, 10) * 1000
        if (elapsed > expiry) {
          clearAuth()
          setVerifying(false)
          return
        }
      }
    }

    // Verify token with backend /auth/me
    authApi
      .getCurrentUser()
      .then((res) => {
        const user = res.data.data
        if (user) {
          setAuth(token, user)
        }
      })
      .catch(() => {
        clearAuth()
      })
      .finally(() => {
        setVerifying(false)
      })
  }, [])

  if (verifying) {
    return null
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  return <>{children}</>
}
