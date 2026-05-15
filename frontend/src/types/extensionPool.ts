export interface ExtensionPool {
  id: number
  orgId: number
  startNumber: string
  endNumber: string
  allocatedBy: string
  createdAt: string
  updatedAt: string
  updatedBy?: string
}

export interface CreateExtensionPoolDTO {
  orgId: number
  startNumber: string
  endNumber: string
}

export interface UpdateExtensionPoolDTO {
  startNumber?: string
  endNumber?: string
}

export interface ExtensionPoolUsage {
  poolId: number
  totalCount: number
  usedCount: number
  idleCount: number
  usageRate: number
  status: 'green' | 'yellow' | 'red'
}
