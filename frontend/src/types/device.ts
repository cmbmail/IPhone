export interface PhoneDevice {
  id: number
  macAddress: string
  model: string | null
  brand: string | null
  purchaseDate: string | null
  orgId: number | null
  orgName: string | null
  assignedEmployeeNo: string | null
  assignedEmployeeName: string | null
  status: number // 0=库存 1=在用 2=停用 3=维修中 4=报废
  remark: string | null
  boundPhoneCount: number
  createdAt: string
  updatedAt: string
}

export interface BoundPhoneDTO {
  phoneId: number
  phoneNumber: string
  extensionNumber: string | null
  status: number
  employeeNo: string | null
  employeeName: string | null
  orgId: number | null
  orgName: string | null
  lineOrder: number
  createdAt: string
}

export interface Device {
  id: number
  deviceId: string // business key like "DEV-001"
  deviceName: string | null
  deviceType: number // 1=IP电话 2=软电话 3=ATA 4=网关
  model: string | null
  macAddress: string | null
  ipAddress: string | null
  phoneNumber: string | null
  extensionNumber: string | null
  status: number // 1=在线 2=离线 3=未注册 4=已禁用
  firmwareVersion: string | null
  lastCheckinTime: string | null
  remark: string | null
  createdAt: string
  updatedAt: string
}

export interface DeviceOperation {
  id: number
  deviceId: string
  operationType: number // 1=重启 2=配置同步 3=固件升级 4=恢复出厂 5=注册
  status: number // 0=待处理 1=处理中 2=已完成 3=失败
  params: string | null
  result: string | null
  operator: string | null
  errorMessage: string | null
  createdAt: string
}
