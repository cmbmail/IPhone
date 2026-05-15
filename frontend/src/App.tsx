import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { ConfigProvider } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import { PrivateRoute } from '@/components/PrivateRoute'
import Login from '@/pages/Login'
import OrgManagement from '@/pages/OrgManagement'
import EmployeeManagement from '@/pages/EmployeeManagement'
import CostCenterManagement from '@/pages/CostCenterManagement'
import PhoneManagement from '@/pages/PhoneManagement'

const App = () => {
  return (
    <ConfigProvider locale={zhCN}>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route
            path="/orgs"
            element={
              <PrivateRoute>
                <OrgManagement />
              </PrivateRoute>
            }
          />
          <Route
            path="/employees"
            element={
              <PrivateRoute>
                <EmployeeManagement />
              </PrivateRoute>
            }
          />
          <Route
            path="/cost-centers"
            element={
              <PrivateRoute>
                <CostCenterManagement />
              </PrivateRoute>
            }
          />
          <Route
            path="/phones"
            element={
              <PrivateRoute>
                <PhoneManagement />
              </PrivateRoute>
            }
          />
          <Route path="/" element={<Navigate to="/orgs" replace />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </ConfigProvider>
  )
}

export default App
