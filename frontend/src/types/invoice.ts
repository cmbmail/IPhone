export interface Invoice {
  id: number
  billMonth: string
  status: string
  totalAmount: number | null
  operator: string
  createdAt: string
}
