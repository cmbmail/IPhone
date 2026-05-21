export interface WorkOrder {
  id: number
  workOrderNo: string
  orderType: string
  status: string
  priority: number
  title: string | null
  requesterName: string | null
  handlerName: string | null
  createdAt: string
}
