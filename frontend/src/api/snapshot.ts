import { ApiGet, ApiPost } from './request'
import type { PagedData } from './request'
import type { PhoneSnapshot, SnapshotStats } from '@/types/snapshot'

export const snapshotApi = {
  /** Get available snapshot months */
  getMonths: () => ApiGet<string[]>('/snapshots/months'),

  /** Get snapshots paged */
  getSnapshots: (params: {
    month: string
    page?: number
    size?: number
    status?: number
    orgId?: number
    branchOrgId?: number
  }) => ApiGet<PagedData<PhoneSnapshot>>('/snapshots', { params }),

  /** Get snapshot statistics for a month */
  getStats: (month: string) => ApiGet<SnapshotStats>(`/snapshots/${month}/stats`),

  /** Get snapshot count by month */
  getCount: (month: string) =>
    ApiGet<{ total: number; active: number; stopped: number; cancelled: number }>(
      `/snapshots/${month}/count`
    ),

  /** Get single snapshot */
  getSnapshot: (id: number) => ApiGet<PhoneSnapshot>(`/snapshots/${id}`),

  /** Trigger snapshot generation for a month */
  trigger: (month: string) => ApiPost<void>('/snapshots/trigger', null, { params: { month } }),

  /** Regenerate snapshot for a month */
  regenerate: (month: string) => ApiPost<void>(`/snapshots/${month}/regenerate`),

  /** Link snapshot month to bill month */
  linkToBill: (snapshotMonth: string, billMonth: string) =>
    ApiPost<{ linkedCount: number; snapshotMonth: string; billMonth: string }>(
      '/snapshots/link-bill',
      null,
      { params: { snapshotMonth, billMonth } }
    ),

  /** Get snapshots linked to a bill month */
  getByBillMonth: (billMonth: string) =>
    ApiGet<PhoneSnapshot[]>(`/snapshots/by-bill/${billMonth}`),
}
