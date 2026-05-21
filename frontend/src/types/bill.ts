export interface BillRaw {
  id: number
  bill_month: string
  charge_type: string
  phone_number: string
  employee_name: string | null
  org_name: string | null
  charge_amount: number | null
  imported_at: string
}

export interface BillAllocation {
  id: number
  bill_month: string
  charge_type: string
  charge_amount: number
  org_id: number | null
  org_name: string | null
  anomaly_flag: boolean
  created_at: string
}
