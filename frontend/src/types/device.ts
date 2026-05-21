export interface Device {
  id: number
  device_id: string
  device_type: string
  model: string | null
  status: string
  ip_address: string | null
  firmware_version: string | null
  last_checkin: string | null
  created_at: string
}

export interface DeviceOperation {
  id: number
  device_id: string
  operation_type: string
  status: string
  operator: string
  created_at: string
}
