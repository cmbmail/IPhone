import { useAuthStore } from '@/stores/authStore'
import type { Role } from '@/types/auth'
import { ROLE_LEVEL } from '@/types/auth'

/**
 * 权限判断 Hook
 * - hasRole: 是否拥有指定角色（admin拥有所有权限）
 * - hasAnyRole: 是否拥有任一角色
 * - hasPermission: 是否拥有指定权限码
 * - hasAnyPermission: 是否拥有任一权限码
 * - isRoleLevel: 当前角色级别是否 <= 指定角色（数字越小权限越大）
 */
export function usePermission() {
  const user = useAuthStore((s) => s.user)
  const permissions = user?.permissions ?? []
  const role = user?.role

  const hasRole = (r: Role) => role === 'admin' || role === r

  const hasAnyRole = (roles: Role[]) => role === 'admin' || (role != null && roles.includes(role as Role))

  const hasPermission = (code: string) => role === 'admin' || permissions.includes(code)

  const hasAnyPermission = (codes: string[]) =>
    role === 'admin' || codes.some((c) => permissions.includes(c))

  const isRoleLevel = (maxLevel: Role) => {
    if (!role) return false
    return ROLE_LEVEL[role] <= ROLE_LEVEL[maxLevel]
  }

  return { hasRole, hasAnyRole, hasPermission, hasAnyPermission, isRoleLevel, role, permissions }
}
