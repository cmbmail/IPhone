import { request } from './request'

export interface PhoneStatistics {
  totalCount: number
  allocatedCount: number
  idleCount: number
  stoppedCount: number
  cancelledCount: number
  reservedCount: number
  disabledCount: number
  statusDistribution: Record<string, number>
  orgDistribution: Record<string, number>
}

export interface DeviceStatistics {
  totalCount: number
  onlineCount: number
  offlineCount: number
  unregisteredCount: number
  disabledCount: number
  onlineRate: number
}

export interface DashboardStats {
  phoneStats: PhoneStatistics | null
  deviceStats: DeviceStatistics | null
  orgCount: number
  userCount: number
  workOrderPending: number
}

export const statisticsApi = {
  getPhoneStats: () => request.get<{ data: PhoneStatistics }>('/statistics/phones'),
  getDeviceStats: () => request.get<{ data: DeviceStatistics }>('/statistics/devices'),
}
