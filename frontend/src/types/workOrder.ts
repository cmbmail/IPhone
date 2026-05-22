export interface WorkOrder {
  id: number
  workOrderNo: string
  orderType: number
  status: number
  priority: number
  title: string | null
  requesterName: string | null
  handlerName: string | null
  createdAt: string
}
