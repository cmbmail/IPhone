export interface WorkOrderItem {
  id: number
  workOrderId: number
  itemType: number
  targetRefId: number | null
  action: string | null
  fromValue: string | null
  toValue: string | null
  status: number
  executedAt: string | null
  errorMessage: string | null
  remark: string | null
  description: string | null
}

export interface WorkOrder {
  id: number
  workOrderNo: string
  type: number
  status: number
  priority: number
  title: string | null
  description: string | null
  requesterId: number | null
  requesterName: string | null
  handlerId: number | null
  handlerName: string | null
  batchId: string | null
  completedAt: string | null
  remark: string | null
  rejectReason: string | null
  createdAt: string
  updatedAt: string | null
  items?: WorkOrderItem[]
}
