import { ApiGet, ApiPost, ApiPut } from './request'

export interface FeeAllocationItem {
  id: number
  billMonth: string
  allocationLevel: number
  parentOrgId: number
  parentOrgName: string
  orgId: number
  orgName: string
  orgType: number
  orgTypeName: string
  phoneCount: number
  platformUsageFee: number
  numberMonthlyRent: number
  domesticCharge: number
  internationalCharge: number
  recordingFee: number
  ringtoneFee: number
  flashSmsFee: number
  totalAmount: number
  percentage: number
  status: number
  statusName: string
  confirmedBy: string | null
  confirmedAt: string | null
}

export interface LevelResponse {
  billMonth: string
  allocationLevel: number
  levelName: string
  levelDescription: string
  totalCount: number
  totalPhones: number
  totalAmount: number
  totalPlatformUsageFee: number
  totalNumberMonthlyRent: number
  totalDomesticCharge: number
  totalInternationalCharge: number
  totalRecordingFee: number
  totalRingtoneFee: number
  totalFlashSmsFee: number
  calculated: boolean
  items: FeeAllocationItem[]
}

export const feeAllocationApi = {
  calculate: (billMonth: string, level: number) =>
    ApiPost<LevelResponse>(`/fee-allocations/calculate?billMonth=${billMonth}&level=${level}`),

  getLevel1: (billMonth: string) =>
    ApiGet<LevelResponse>(`/fee-allocations/level1?billMonth=${billMonth}`),

  getLevel2: (billMonth: string, parentOrgId?: number) =>
    ApiGet<LevelResponse>(`/fee-allocations/level2?billMonth=${billMonth}${parentOrgId ? `&parentOrgId=${parentOrgId}` : ''}`),

  getLevel3: (billMonth: string, parentOrgId?: number) =>
    ApiGet<LevelResponse>(`/fee-allocations/level3?billMonth=${billMonth}${parentOrgId ? `&parentOrgId=${parentOrgId}` : ''}`),

  confirm: (id: number) =>
    ApiPut<FeeAllocationItem>(`/fee-allocations/${id}/confirm`),

  reject: (id: number) =>
    ApiPut<FeeAllocationItem>(`/fee-allocations/${id}/reject`),
}
