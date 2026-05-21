export interface Invoice {
  id: number
  bill_month: string
  status: string
  total_amount: number | null
  operator: string
  created_at: string
}
