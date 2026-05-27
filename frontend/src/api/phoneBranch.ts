import { ApiGet, ApiPost } from './request'
import type { PhoneNumber } from '@/types/phone'

export interface BranchPoolStats {
  poolCount: number
  totalCount: number
  systemPoolCount: number
}

export const phoneBranchApi = {
  /** Get system pool phones (not allocated to any branch) */
  getSystemPool: () => ApiGet<PhoneNumber[]>('/phones/system-pool'),

  /** Get branch pool phones (allocated to branch but not to any dept) */
  getBranchPool: (branchOrgId: number) =>
    ApiGet<PhoneNumber[]>(`/phones/branch-pool/${branchOrgId}`),

  /** Get branch pool statistics */
  getBranchPoolStats: (branchOrgId: number) =>
    ApiGet<BranchPoolStats>(`/phones/branch-pool-stats/${branchOrgId}`),

  /** Phase 1: Allocate phones from system pool to a branch */
  branchAllocate: (data: { phoneIds: number[]; branchOrgId: number; remark?: string }) =>
    ApiPost<PhoneNumber[]>('/phones/branch-allocate', data),

  /** Phase 2: Allocate branch pool phones to a department */
  deptAllocate: (data: { phoneIds: number[]; deptOrgId: number; remark?: string }) =>
    ApiPost<PhoneNumber[]>('/phones/dept-allocate', data),

  /** Revoke phones from dept back to branch pool */
  deptRevoke: (data: { phoneIds: number[]; branchOrgId: number; remark?: string }) =>
    ApiPost<PhoneNumber[]>('/phones/dept-revoke', data),

  /** Revoke phones from branch back to system pool */
  branchRevoke: (data: { phoneIds: number[]; branchOrgId: number; remark?: string }) =>
    ApiPost<PhoneNumber[]>('/phones/branch-revoke', data),
}
