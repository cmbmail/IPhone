export interface Device {
  id: number
  deviceId: string
  deviceType: string
  model: string | null
  status: string
  ipAddress: string | null
  firmwareVersion: string | null
  lastCheckin: string | null
  createdAt: string
}

export interface DeviceOperation {
  id: number
  deviceId: string
  operationType: string
  status: string
  operator: string
  createdAt: string
}
