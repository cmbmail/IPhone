import request from '../utils/request.js'

export const login = (data) => {
  return request({
    url: '/auth/login',
    method: 'POST',
    data
  })
}

export const getOrgList = () => {
  return request({
    url: '/orgs',
    method: 'GET'
  })
}

export const getOrgById = (id) => {
  return request({
    url: `/orgs/${id}`,
    method: 'GET'
  })
}

export const addOrg = (data) => {
  return request({
    url: '/orgs',
    method: 'POST',
    data
  })
}

export const updateOrg = (id, data) => {
  return request({
    url: `/orgs/${id}`,
    method: 'PUT',
    data
  })
}

export const deleteOrg = (id) => {
  return request({
    url: `/orgs/${id}`,
    method: 'DELETE'
  })
}

export const getEmployeePage = (params) => {
  return request({
    url: '/employees',
    method: 'GET',
    params
  })
}

export const getEmployeeById = (id) => {
  return request({
    url: `/employees/${id}`,
    method: 'GET'
  })
}

export const addEmployee = (data) => {
  return request({
    url: '/employees',
    method: 'POST',
    data
  })
}

export const updateEmployee = (id, data) => {
  return request({
    url: `/employees/${id}`,
    method: 'PUT',
    data
  })
}

export const deleteEmployee = (id) => {
  return request({
    url: `/employees/${id}`,
    method: 'DELETE'
  })
}

export const getRoleList = () => {
  return request({
    url: '/roles',
    method: 'GET'
  })
}

export const getRoleById = (id) => {
  return request({
    url: `/roles/${id}`,
    method: 'GET'
  })
}

export const addRole = (data) => {
  return request({
    url: '/roles',
    method: 'POST',
    data
  })
}

export const updateRole = (id, data) => {
  return request({
    url: `/roles/${id}`,
    method: 'PUT',
    data
  })
}

export const deleteRole = (id) => {
  return request({
    url: `/roles/${id}`,
    method: 'DELETE'
  })
}

export const getMenuList = () => {
  return request({
    url: '/auth/menu',
    method: 'GET'
  })
}

export const getPhonePage = (params) => {
  return request({
    url: '/phones',
    method: 'GET',
    params
  })
}

export const getPhoneById = (id) => {
  return request({
    url: `/phones/${id}`,
    method: 'GET'
  })
}

export const addPhone = (data) => {
  return request({
    url: '/phones',
    method: 'POST',
    data
  })
}

export const assignPhone = (data) => {
  return request({
    url: '/phones/allocate',
    method: 'POST',
    data
  })
}

export const recyclePhone = (data) => {
  return request({
    url: '/phones/reclaim',
    method: 'POST',
    data
  })
}

export const getOrderPage = (params) => {
  return request({
    url: '/work-orders',
    method: 'GET',
    params
  })
}

export const getOrderById = (id) => {
  return request({
    url: `/work-orders/${id}`,
    method: 'GET'
  })
}

export const addOrder = (data) => {
  return request({
    url: '/work-orders',
    method: 'POST',
    data
  })
}

export const acceptOrder = (id) => {
  return request({
    url: `/work-orders/${id}/status`,
    method: 'PUT',
    data: { status: 'ACCEPTED' }
  })
}

export const completeOrder = (id) => {
  return request({
    url: `/work-orders/${id}/status`,
    method: 'PUT',
    data: { status: 'COMPLETED' }
  })
}

export const cancelOrder = (id) => {
  return request({
    url: `/work-orders/${id}/status`,
    method: 'PUT',
    data: { status: 'CANCELLED' }
  })
}
