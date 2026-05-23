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

export type PhoneStatus = 0 | 1 | 2 | 3 | 4 | 5

export interface PhoneHistory {
  id: number
  phoneId: number
  phoneNumber: string
  action: PhoneAction
  fromStatus?: number
  toStatus?: number
  fromEmployeeNo?: string
  toEmployeeNo?: string
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
  finalEmployeeNo?: string
  finalOrg?: string
  surrenderDate: string
  surrenderType: number  // 1=拆机 2=注销
  operator: string
  workOrderNo?: string
  remark?: string
  archivedAt: string
}

export interface CreatePhoneDTO {
  phoneNumber: string
  employeeNo?: string
  extensionNumber?: string
  extensionType?: 'auto' | 'manual'
  orgId?: number
  remark?: string
}

export interface UpdatePhoneDTO {
  remark?: string
}

export interface PhoneAllocationRequest {
  phoneId: number
  employeeNo: string
  orgId: number
  extensionNumber?: string
  workOrderNo?: string
  remark?: string
}

export interface PhoneReclaimRequest {
  phoneId: number
  reason?: string
  workOrderNo?: string
  remark?: string
}

export interface PhoneStatusChangeRequest {
  phoneId: number
  newStatus: number
  workOrderNo?: string
  remark?: string
}

export interface PhoneSurrenderRequest {
  phoneId: number
  surrenderType: number  // 1=拆机 2=注销
  workOrderNo?: string
  remark?: string
}

export interface PhoneReserveRequest {
  phoneId: number
  workOrderNo?: string
  remark?: string
}

export interface PhoneChangeRequest {
  phoneId: number
  employeeNo?: string
  orgId?: number
  phoneNumber?: string
  extensionNumber?: string
  workOrderNo?: string
  remark?: string
}

export interface PhoneQueryDTO {
  phoneNumber?: string
  employeeNo?: string
  status?: number
  orgId?: number
  page?: number
  pageSize?: number
}
