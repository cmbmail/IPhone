export interface AreaCodeOrgMapping {
  id: number
  areaCode: string
  orgId: number
  orgName?: string
  priority: number
  createdAt: string
  createdBy: string
  updatedAt: string
  updatedBy?: string
}

export interface CreateAreaCodeMappingDTO {
  areaCode: string
  orgId: number
  priority?: number
}

export interface UpdateAreaCodeMappingDTO {
  orgId?: number
  priority?: number
}
