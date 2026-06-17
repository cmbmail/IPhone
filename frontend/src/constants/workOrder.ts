/** 工单状态颜色 */
export const WORK_ORDER_STATUS_COLORS: Record<number, string> = {
  0: 'warning',
  1: 'processing',
  2: 'processing',
  3: 'success',
  4: 'default',
  5: 'error',
}

/** 工单状态名称 */
export const WORK_ORDER_STATUS_NAMES: Record<number, string> = {
  0: '待处理',
  1: '挂起',
  2: '处理中',
  3: '已完成',
  4: '已归档',
  5: '已取消',
}

/** 工单类型名称 */
export const WORK_ORDER_TYPE_NAMES: Record<number, string> = {
  1: '新增',
  2: '变更',
  3: '解绑',
  4: '座机绑定',
  5: '号码拆机',
}

/** 工单类型颜色 */
export const WORK_ORDER_TYPE_COLORS: Record<number, string> = {
  1: 'green',
  2: 'blue',
  3: 'orange',
  4: 'purple',
  5: 'red',
}

/** 工单项类型 */
export const WORK_ORDER_ITEM_TYPE_NAMES: Record<number, string> = {
  1: '号码',
  2: '设备',
  3: '员工',
}

/** 工单项状态名称 */
export const WORK_ORDER_ITEM_STATUS_NAMES: Record<number, string> = {
  0: '待执行',
  1: '执行中',
  2: '已完成',
  3: '失败',
  4: '已跳过',
}

/** 工单项状态颜色 */
export const WORK_ORDER_ITEM_STATUS_COLORS: Record<number, string> = {
  0: 'processing',
  1: 'processing',
  2: 'success',
  3: 'error',
  4: 'default',
}
