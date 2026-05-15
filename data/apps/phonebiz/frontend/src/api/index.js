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
    url: '/org/list',
    method: 'GET'
  })
}

export const getOrgById = (id) => {
  return request({
    url: `/org/${id}`,
    method: 'GET'
  })
}

export const addOrg = (data) => {
  return request({
    url: '/org',
    method: 'POST',
    data
  })
}

export const updateOrg = (data) => {
  return request({
    url: '/org',
    method: 'PUT',
    data
  })
}

export const deleteOrg = (id) => {
  return request({
    url: `/org/${id}`,
    method: 'DELETE'
  })
}

export const getEmployeePage = (params) => {
  return request({
    url: '/employee/page',
    method: 'GET',
    params
  })
}

export const getEmployeeById = (id) => {
  return request({
    url: `/employee/${id}`,
    method: 'GET'
  })
}

export const addEmployee = (data) => {
  return request({
    url: '/employee',
    method: 'POST',
    data
  })
}

export const updateEmployee = (data) => {
  return request({
    url: '/employee',
    method: 'PUT',
    data
  })
}

export const deleteEmployee = (id) => {
  return request({
    url: `/employee/${id}`,
    method: 'DELETE'
  })
}

export const getRoleList = () => {
  return request({
    url: '/role/list',
    method: 'GET'
  })
}

export const getRoleById = (id) => {
  return request({
    url: `/role/${id}`,
    method: 'GET'
  })
}

export const addRole = (data) => {
  return request({
    url: '/role',
    method: 'POST',
    data
  })
}

export const updateRole = (data) => {
  return request({
    url: '/role',
    method: 'PUT',
    data
  })
}

export const deleteRole = (id) => {
  return request({
    url: `/role/${id}`,
    method: 'DELETE'
  })
}

export const getMenuList = () => {
  return request({
    url: '/menu/list',
    method: 'GET'
  })
}

export const getMenuById = (id) => {
  return request({
    url: `/menu/${id}`,
    method: 'GET'
  })
}

export const addMenu = (data) => {
  return request({
    url: '/menu',
    method: 'POST',
    data
  })
}

export const updateMenu = (data) => {
  return request({
    url: '/menu',
    method: 'PUT',
    data
  })
}

export const deleteMenu = (id) => {
  return request({
    url: `/menu/${id}`,
    method: 'DELETE'
  })
}

export const getPhonePage = (params) => {
  return request({
    url: '/phone/page',
    method: 'GET',
    params
  })
}

export const getPhoneById = (id) => {
  return request({
    url: `/phone/${id}`,
    method: 'GET'
  })
}

export const addPhone = (data) => {
  return request({
    url: '/phone',
    method: 'POST',
    data
  })
}

export const assignPhone = (id, data) => {
  return request({
    url: `/phone/assign/${id}`,
    method: 'PUT',
    data
  })
}

export const activatePhone = (id) => {
  return request({
    url: `/phone/activate/${id}`,
    method: 'PUT'
  })
}

export const suspendPhone = (id) => {
  return request({
    url: `/phone/suspend/${id}`,
    method: 'PUT'
  })
}

export const resumePhone = (id) => {
  return request({
    url: `/phone/resume/${id}`,
    method: 'PUT'
  })
}

export const recyclePhone = (id) => {
  return request({
    url: `/phone/recycle/${id}`,
    method: 'PUT'
  })
}

export const getOrderPage = (params) => {
  return request({
    url: '/order/page',
    method: 'GET',
    params
  })
}

export const getOrderById = (id) => {
  return request({
    url: `/order/${id}`,
    method: 'GET'
  })
}

export const addOrder = (data) => {
  return request({
    url: '/order',
    method: 'POST',
    data
  })
}

export const acceptOrder = (id, data) => {
  return request({
    url: `/order/accept/${id}`,
    method: 'PUT',
    data
  })
}

export const approveOrder = (id, data) => {
  return request({
    url: `/order/approve/${id}`,
    method: 'PUT',
    data
  })
}

export const rejectOrder = (id, data) => {
  return request({
    url: `/order/reject/${id}`,
    method: 'PUT',
    data
  })
}

export const completeOrder = (id, data) => {
  return request({
    url: `/order/complete/${id}`,
    method: 'PUT',
    data
  })
}

export const cancelOrder = (id) => {
  return request({
    url: `/order/cancel/${id}`,
    method: 'PUT'
  })
}

export const reopenOrder = (id) => {
  return request({
    url: `/order/reopen/${id}`,
    method: 'PUT'
  })
}
