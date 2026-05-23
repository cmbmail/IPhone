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

export interface UpdateExtensionPoolDTO {
  startNumber?: string
  endNumber?: string
}
