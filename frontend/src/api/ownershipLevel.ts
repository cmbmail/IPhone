import { ApiGet } from './request'

export interface OwnershipLevelItem {
  orgId: number
  orgName: string
  orgType: number
  orgTypeName: string
  parentOrgId: number | null
  parentOrgName: string
  phoneCount: number
  allocatedCount: number
}

export interface LevelSummaryResponse {
  level: number
  levelName: string
  levelDescription: string
  totalOrgs: number
  totalPhones: number
  totalAllocated: number
  items: OwnershipLevelItem[]
}

export const ownershipLevelApi = {
  getByLevel: (level: number, parentOrgId?: number) =>
    ApiGet<LevelSummaryResponse>('/phone-ownership/by-level', {
      params: { level, parentOrgId },
    }),
}
