export interface AuditLogEntry {
  id: number
  module: string
  operation: string
  operator: string
  target_type: string | null
  target_id: string | null
  ip_address: string | null
  status: string
  cost_time: number
  created_at: string
}
