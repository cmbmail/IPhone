import axios from 'axios'
import type { Employee, CreateEmployeeDTO, UpdateEmployeeDTO } from '@/types/employee'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

export const employeeApi = {
  getAll: (params?: { page?: number; size?: number }) =>
    request.get<{ code: number; data: { content: Employee[]; totalElements: number } }>('/employees', { params }),

  getById: (id: number) => request.get<Employee>(`/employees/${id}`),

  getByNo: (employeeNo: string) => request.get<Employee>(`/employees/by-no/${employeeNo}`),

  getByOrg: (orgId: number) => request.get<Employee[]>(`/employees/by-org/${orgId}`),

  getActive: () => request.get<Employee[]>('/employees/active'),

  create: (data: CreateEmployeeDTO) => request.post<Employee>('/employees', data),

  update: (id: number, data: UpdateEmployeeDTO) => request.put<Employee>(`/employees/${id}`, data),

  delete: (id: number) => request.delete(`/employees/${id}`),

  countByOrg: (orgId: number) => request.get<number>(`/employees/count/${orgId}`)
}
