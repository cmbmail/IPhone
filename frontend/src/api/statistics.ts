import { request } from './request'

export interface PhoneStatistics {
  total_count: number
  allocated_count: number
  idle_count: number
  stopped_count: number
  cancelled_count: number
  reserved_count: number
  disabled_count: number
  status_distribution: Record<string, number>
  org_distribution: Record<string, number>
}

export interface DeviceStatistics {
  total_count: number
  online_count: number
  offline_count: number
  unregistered_count: number
  disabled_count: number
  online_rate: number
}

export interface DashboardStats {
  phone_stats: PhoneStatistics | null
  device_stats: DeviceStatistics | null
  org_count: number
  user_count: number
  work_order_pending: number
}

export const statisticsApi = {
  getPhoneStats: () => request.get<{ data: PhoneStatistics }>('/statistics/phones'),
  getDeviceStats: () => request.get<{ data: DeviceStatistics }>('/statistics/devices'),
}
