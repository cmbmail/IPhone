export interface Device {
  id: number
  deviceId: string
  deviceType: number
  model: string | null
  status: number
  ipAddress: string | null
  firmwareVersion: string | null
  lastCheckin: string | null
  createdAt: string
}

export interface DeviceOperation {
  id: number
  deviceId: string
  operationType: string
  status: number
  operator: string
  createdAt: string
}
