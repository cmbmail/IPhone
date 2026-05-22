export interface Invoice {
  id: number
  billMonth: string
  status: number
  totalAmount: number | null
  operator: string
  createdAt: string
}
