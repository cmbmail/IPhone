export interface AuditLogEntry {
  id: number
  module: string
  operation: string
  operator: string
  targetType: string | null
  targetId: string | null
  ipAddress: string | null
  status: string
  costTime: number
  createdAt: string
}
