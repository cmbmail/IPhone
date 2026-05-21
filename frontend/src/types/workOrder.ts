export interface WorkOrder {
  id: number
  work_order_no: string
  order_type: string
  status: string
  priority: number
  title: string | null
  requester_name: string | null
  handler_name: string | null
  created_at: string
}
