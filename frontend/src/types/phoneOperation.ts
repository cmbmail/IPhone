export interface PhoneAllocationRequest {
  phoneId: number
  userId: string
  orgId: number
  extensionNumber?: string
}

export interface PhoneReclaimRequest {
  phoneId: number
  reason?: string
}
