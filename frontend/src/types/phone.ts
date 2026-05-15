export interface PhoneNumber {
  id: number
  phoneNumber: string
  userId?: string
  extensionNumber?: string
  extensionType?: 'auto' | 'manual'
  isShared: boolean
  isReentry: boolean
  status: PhoneStatus
  orgId?: number
  allocationOrgId?: number
  remark?: string
  version: number
  createdBy: string
  createdAt: string
  updatedBy: string
  updatedAt: string
}

export type PhoneStatus = 'idle' | 'active' | 'stopped' | 'cancelled' | 'reserved' | 'disabled'

export interface PhoneHistory {
  id: number
  phoneId: number
  phoneNumber: string
  action: PhoneAction
  fromStatus?: PhoneStatus
  toStatus?: PhoneStatus
  fromUser?: string
  toUser?: string
  fromOrg?: string
  toOrg?: string
  workOrderNo?: string
  operator: string
  operatedAt: string
  remark?: string
}

export type PhoneAction =
  | 'allocate'
  | 'reclaim'
  | 'surrender'
  | 'trouble'
  | 'change_user'
  | 'change_org'
  | 'change_number'
  | 'reserve'
  | 'release'
  | 'disable'
  | 'enable'

export interface PhoneSurrenderRecord {
  id: number
  phoneId: number
  phoneNumber: string
  finalUser?: string
  finalOrg?: string
  surrenderDate: string
  surrenderType: 'surrender' | 'cancel'
  operator: string
  workOrderNo?: string
  remark?: string
  archivedAt: string
}

export interface CreatePhoneDTO {
  phoneNumber: string
  userId?: string
  extensionNumber?: string
  extensionType?: 'auto' | 'manual'
  orgId?: number
  remark?: string
}

export interface UpdatePhoneDTO {
  remark?: string
}

export interface PhoneQueryDTO {
  phoneNumber?: string
  userId?: string
  status?: PhoneStatus
  orgId?: number
  page?: number
  pageSize?: number
}
