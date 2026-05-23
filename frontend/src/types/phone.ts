export interface PhoneNumber {
  id: number
  phoneNumber: string
  employeeNo?: string
  extensionNumber?: string
  extensionType?: 'auto' | 'manual'
  isShared: boolean
  isReentry: boolean
  status: number
  orgId?: number
  allocationOrgId?: number
  remark?: string
  version: number
  createdBy: string
  createdAt: string
  updatedBy: string
  updatedAt: string
}

export interface UpdatePhoneDTO {
  remark?: string
}
