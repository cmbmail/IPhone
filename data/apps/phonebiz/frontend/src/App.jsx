import React from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import Login from './pages/Login.jsx'
import Layout from './components/Layout.jsx'
import ProtectedRoute from './components/ProtectedRoute.jsx'
import OrgList from './pages/OrgList.jsx'
import EmployeeList from './pages/EmployeeList.jsx'
import RoleList from './pages/RoleList.jsx'
import MenuList from './pages/MenuList.jsx'
import PhoneList from './pages/PhoneList.jsx'
import OrderList from './pages/OrderList.jsx'
import Dashboard from './pages/Dashboard.jsx'

function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/" element={
        <ProtectedRoute>
          <Layout />
        </ProtectedRoute>
      }>
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={<Dashboard />} />
        <Route path="org" element={<OrgList />} />
        <Route path="employee" element={<EmployeeList />} />
        <Route path="role" element={<RoleList />} />
        <Route path="menu" element={<MenuList />} />
        <Route path="phone" element={<PhoneList />} />
        <Route path="order" element={<OrderList />} />
      </Route>
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  )
}

export default App
