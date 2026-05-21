export interface PhoneStatistics {
  total_count: number
  allocated_count: number
  idle_count: number
  stopped_count: number
}

export interface DeviceStatistics {
  total_count: number
  online_count: number
  offline_count: number
  online_rate: number
}
