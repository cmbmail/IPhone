export interface Employee {
  id: number
  employeeNo: string
  name: string
  orgId: number
  orgName?: string
  position?: string
  phone?: string
  email?: string
  status: 0 | 1
  entryDate?: string
  leaveDate?: string
  isVirtual: boolean
  createdBy: string
  createdAt: string
  updatedBy: string
  updatedAt: string
}

export interface CreateEmployeeDTO {
  employeeNo: string
  name: string
  orgId: number
  position?: string
  phone?: string
  email?: string
  entryDate?: string
  isVirtual?: boolean
}

export interface UpdateEmployeeDTO {
  name?: string
  orgId?: number
  position?: string
  phone?: string
  email?: string
  status?: 0 | 1
  leaveDate?: string
}

export interface EmployeeQueryDTO {
  employeeNo?: string
  name?: string
  orgId?: number
  status?: string
  isVirtual?: boolean
  page?: number
  pageSize?: number
}
