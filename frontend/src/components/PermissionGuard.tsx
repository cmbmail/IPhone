import { Navigate } from 'react-router-dom'
import { usePermission } from '@/hooks/usePermission'
import type { Role } from '@/types/auth'

interface PermissionGuardProps {
  children: React.ReactNode
  /** 拥有指定权限码即可访问（admin自动通过） */
  permission?: string
  /** 拥有任一角色即可访问（admin自动通过） */
  anyRole?: Role[]
}

/**
 * 路由级权限守卫
 * - 无权限时重定向到 /dashboard
 * - admin 角色自动放行
 */
export function PermissionGuard({ children, permission, anyRole }: PermissionGuardProps) {
  const { hasPermission, hasAnyRole } = usePermission()

  if (permission && !hasPermission(permission)) {
    return <Navigate to="/dashboard" replace />
  }
  if (anyRole && anyRole.length > 0 && !hasAnyRole(anyRole)) {
    return <Navigate to="/dashboard" replace />
  }

  return <>{children}</>
}
