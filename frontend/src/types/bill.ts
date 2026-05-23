export interface BillRaw {
  id: number
  billMonth: string
  chargeType: number  // 0=电话 1=录音 2=彩铃 3=闪信
  importStatus: number  // 0=待处理 1=已处理 2=错误
  phoneNumber: string
  extensionNumber: string | null
  employeeNo: string | null
  employeeName: string | null
  orgId: number | null
  orgName: string | null
  deptName: string | null
  city: string | null
  chargeAmount: number | null
  platformUsageFee: number | null
  numberMonthlyRent: number | null
  outboundDuration: number | null
  transferOutboundDuration: number | null
  domesticCharge: number | null
  internationalDuration: number | null
  internationalCharge: number | null
  recordingFee: number | null
  ringtoneFee: number | null
  flashSmsFee: number | null
  sendCount: number | null
  activationTime: string | null
  deactivationTime: string | null
  days: number | null
  importedAt: string
  createdBy: string
  createdAt: string
}

export interface BillAllocation {
  id: number
  billMonth: string
  branchName: string | null
  chargeType: number
  chargeAmount: number
  platformUsageFee: number | null
  numberMonthlyRent: number | null
  outboundDuration: number | null
  transferOutboundDuration: number | null
  domesticCharge: number | null
  internationalCharge: number | null
  feeSubtotal: number | null
  recordingFee: number | null
  ringtoneFee: number | null
  flashSmsFee: number | null
  totalAmount: number | null
  orgId: number | null
  orgName: string | null
  anomalyFlag: number  // 0=正常 1=异常
  createdAt: string
}
