import { useState } from 'react'
import { BrowserRouter, Routes, Route, Navigate, useNavigate, useLocation } from 'react-router-dom'
import { ConfigProvider, Menu, Button, Avatar, Badge, Input, Dropdown, Modal, Form, Popconfirm, message } from 'antd'
import { MenuFoldOutlined, MenuUnfoldOutlined, BellOutlined, SearchOutlined, LogoutOutlined, LockOutlined, TeamOutlined, WalletOutlined, PhoneOutlined, AppstoreOutlined, EnvironmentOutlined, DesktopOutlined, CheckOutlined, BarChartOutlined, CarryOutOutlined, AccountBookOutlined, SwapOutlined, AuditOutlined, ApartmentOutlined, SafetyCertificateOutlined, FileTextOutlined, NotificationOutlined } from '@ant-design/icons'
import zhCN from 'antd/locale/zh_CN'
import { PrivateRoute } from '@/components/PrivateRoute'
import { useAuthStore } from '@/stores/authStore'
import { authApi } from '@/api/auth'
import Login from '@/pages/Login'
import Dashboard from '@/pages/Dashboard'
import OrgManagement from '@/pages/OrgManagement'
import CostCenterManagement from '@/pages/CostCenterManagement'
import PhoneManagement from '@/pages/PhoneManagement'
import PhoneOwnershipPage from '@/pages/PhoneOwnership'
import ExtensionPoolManagement from '@/pages/ExtensionPoolManagement'
import AreaCodeManagement from '@/pages/AreaCodeManagement'
import BillAllocationManagement from '@/pages/BillAllocationManagement'
import InvoiceManagement from '@/pages/InvoiceManagement'
import SubsidiaryReconciliationPage from '@/pages/SubsidiaryReconciliationPage'
import BillManagement from '@/pages/BillManagement'
import ReportCenter from '@/pages/ReportCenter'
import WorkOrderManagement from '@/pages/WorkOrderManagement'
import DeviceManagement from '@/pages/DeviceManagement'
import RoleManagement from '@/pages/RoleManagement'
import UserManagement from '@/pages/UserManagement'
import AuditLogManagement from '@/pages/AuditLogManagement'
import AnnouncementManagement from '@/pages/AnnouncementManagement'
import ChangePassword from '@/pages/ChangePassword'
import NotificationPopover from '@/components/NotificationPopover'

// Layout components replaced with plain divs for reliable flex layout

const menuItems = [
  { key: 'sub-work-orders', icon: <CarryOutOutlined />, label: '工单管理', children: [
    { key: '/work-orders', icon: <CarryOutOutlined />, label: '工单管理' },
  ]},
  { key: 'sub-phones', icon: <PhoneOutlined />, label: '号码资源', children: [
    { key: '/phones', icon: <PhoneOutlined />, label: '电话号码' },
    { key: '/phone-ownership', icon: <ApartmentOutlined />, label: '号码归属' },
    { key: '/extension-pools', icon: <AppstoreOutlined />, label: '分机池' },
    { key: '/area-codes', icon: <EnvironmentOutlined />, label: '区号管理' },
    { key: '/devices', icon: <DesktopOutlined />, label: '设备管理' },
  ]},
  { key: 'sub-bills', icon: <AccountBookOutlined />, label: '费用管理', children: [
    { key: '/cost-centers', icon: <WalletOutlined />, label: '成本中心' },
    { key: '/bills', icon: <AccountBookOutlined />, label: '账单管理' },
    { key: '/bill-allocations', icon: <SwapOutlined />, label: '账单分摊' },
    { key: '/invoices', icon: <CheckOutlined />, label: '发票管理' },
    { key: '/reconciliations', icon: <AuditOutlined />, label: '子公司对账' },
  ]},
  { key: '/orgs', icon: <TeamOutlined />, label: '用户管理' },
  { key: 'sub-system', icon: <BarChartOutlined />, label: '系统管理', children: [
    { key: '/reports', icon: <BarChartOutlined />, label: '报表中心' },
    { key: '/audit-logs', icon: <FileTextOutlined />, label: '审计日志' },
    { key: '/roles', icon: <SafetyCertificateOutlined />, label: '角色管理' },
    { key: '/announcements', icon: <NotificationOutlined />, label: '通知公告' },
  ]},
]

const pageTitleMap: Record<string, string> = {
  '/dashboard': '系统看板',
  '/work-orders': '工单管理',
  '/phones': '电话号码',
  '/phone-ownership': '号码归属',
  '/extension-pools': '分机池',
  '/area-codes': '区号管理',
  '/devices': '设备管理',
  '/cost-centers': '成本中心',
  '/bills': '账单管理',
  '/bill-allocations': '账单分摊',
  '/invoices': '发票管理',
  '/reconciliations': '子公司对账',
  '/orgs': '用户管理',
  '/reports': '报表中心',
  '/user-management': '用户管理',
  '/audit-logs': '审计日志',
  '/roles': '角色管理',
  '/announcements': '通知公告',
}

const AppLayout = ({ children }: { children: React.ReactNode }) => {
  const navigate = useNavigate()
  const location = useLocation()
  const [collapsed, setCollapsed] = useState(false)
  const [openKeys, setOpenKeys] = useState<string[]>([])
  const { user, clearAuth } = useAuthStore()
  const [pwdModalOpen, setPwdModalOpen] = useState(false)
  const [pwdLoading, setPwdLoading] = useState(false)
  const [pwdForm] = Form.useForm()
  const [userMenuOpen, setUserMenuOpen] = useState(false)
  const [logoutConfirmOpen, setLogoutConfirmOpen] = useState(false)

  const handleUserMenuClick = ({ key }: { key: string }) => {
    if (key === 'change-password') {
      setUserMenuOpen(false)
      pwdForm.resetFields()
      setPwdModalOpen(true)
    } else if (key === 'logout') {
      setUserMenuOpen(false)
      setTimeout(() => setLogoutConfirmOpen(true), 100)
    }
  }

  const handleChangePassword = async (values: { oldPassword: string; newPassword: string }) => {
    setPwdLoading(true)
    try {
      await authApi.changePassword({ oldPassword: values.oldPassword, newPassword: values.newPassword })
      message.success('密码修改成功，请重新登录')
      setPwdModalOpen(false)
      clearAuth()
      navigate('/login')
    } catch (error: any) {
      message.error(error.response?.data?.message || '密码修改失败')
    } finally {
      setPwdLoading(false)
    }
  }

  const handleLogout = () => {
    setLogoutConfirmOpen(false)
    clearAuth()
    navigate('/login')
  }

  const pageTitle = pageTitleMap[location.pathname] || '系统看板'

  const userDropdownItems = [
    { key: 'change-password', icon: <LockOutlined />, label: '修改密码' },
    { type: 'divider' as const },
    { key: 'logout', icon: <LogoutOutlined />, label: '退出登录', danger: true },
  ]

  return (
    <div className="app-layout">
      <div className={`sidebar ${collapsed ? "collapsed" : ""}`}>
        <div className="sidebar-logo" style={{ cursor: 'pointer' }} onClick={() => navigate('/dashboard')}>
          <div className="sidebar-logo-icon">P</div>
          <span className="sidebar-logo-text">PhoneBiz</span>
        </div>
        <div className="sidebar-menu">
          <Menu
            theme="dark" mode="inline"
            selectedKeys={[location.pathname === '/dashboard' ? '' : location.pathname]}
            openKeys={openKeys}
            onOpenChange={setOpenKeys}
            items={menuItems}
            onClick={({ key }) => navigate(key)}
            style={{ background: 'transparent', border: 'none' }}
          />
        </div>
        <div className="sidebar-toggle">
          <Button
            type="text"
            icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            onClick={() => setCollapsed(!collapsed)}
            className="toggle-btn"
            style={{ color: 'var(--text-sidebar)' }}
          >

          </Button>
        </div>
      </div>

      <div className="main-wrapper">
        <div className="header">
          <div className="header-left">
            <div className="page-heading">
              <h1>{pageTitle}</h1>
              <span>
                {location.pathname === '/dashboard' ? '查看系统运营概况和数据统计' : '管理您的企业通信资源'}
              </span>
            </div>
          </div>

          <div className="header-right">
            <div className="search-box">
              <SearchOutlined style={{ fontSize: 16, color: 'var(--text-muted)' }} />
              <input type="text" placeholder="搜索..." />
            </div>

            <NotificationPopover />

            <Dropdown
              menu={{ items: userDropdownItems, onClick: handleUserMenuClick }}
              placement="bottomRight"
              trigger={['click']}
              open={userMenuOpen}
              onOpenChange={(open) => {
                setUserMenuOpen(open)
                if (!open) setLogoutConfirmOpen(false)
              }}
            >
              <div className="user-info" style={{ cursor: 'pointer' }}>
                <Avatar style={{
                  background: 'linear-gradient(145deg, var(--accent) 0%, var(--accent-dark) 100%)',
                  width: 32, height: 32, borderRadius: '50%', fontWeight: 600, fontSize: 13,
                }}>
                  {user?.username?.charAt(0).toUpperCase() || 'U'}
                </Avatar>
                <div style={{ display: 'flex', flexDirection: 'column', lineHeight: 'normal', flexShrink: 0 }}>
                  <span style={{ fontSize: 13, fontWeight: 500, color: 'var(--text-primary)' }}>{user?.username || '用户'}</span>
                  <span style={{ fontSize: 11, color: 'var(--text-muted)' }}>管理员</span>
                </div>
              </div>
            </Dropdown>

            <Popconfirm
              title="退出系统"
              description="确定要退出当前账号吗？"
              open={logoutConfirmOpen}
              onOpenChange={setLogoutConfirmOpen}
              onConfirm={handleLogout}
              onCancel={() => setLogoutConfirmOpen(false)}
              okText="确定"
              cancelText="取消"
              okButtonProps={{ danger: true }}
              placement="bottomRight"
            >
              <span />
            </Popconfirm>
          </div>
        </div>

        <div className="content">{children}</div>
      </div>

      <Modal
        title="修改密码"
        open={pwdModalOpen}
        onCancel={() => setPwdModalOpen(false)}
        footer={null}
        width={420}
        destroyOnClose
      >
        <Form form={pwdForm} layout="vertical" onFinish={handleChangePassword} style={{ marginTop: 16 }}>
          <Form.Item name="oldPassword" label="当前密码" rules={[{ required: true, message: '请输入当前密码' }]}>
            <Input.Password placeholder="请输入当前密码" />
          </Form.Item>
          <Form.Item name="newPassword" label="新密码" rules={[
            { required: true, message: '请输入新密码' },
            { min: 8, message: '密码长度至少8位' },
            { pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*]).{8,}$/, message: '需包含大小写字母、数字和特殊字符' },
          ]}>
            <Input.Password placeholder="请输入新密码（至少8位）" />
          </Form.Item>
          <Form.Item name="confirmPassword" label="确认新密码" dependencies={['newPassword']} rules={[
            { required: true, message: '请再次输入新密码' },
            ({ getFieldValue }) => ({
              validator(_, value) {
                if (!value || getFieldValue('newPassword') === value) return Promise.resolve()
                return Promise.reject(new Error('两次输入的密码不一致'))
              },
            }),
          ]}>
            <Input.Password placeholder="请再次输入新密码" />
          </Form.Item>
          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Button style={{ marginRight: 8 }} onClick={() => setPwdModalOpen(false)}>取消</Button>
            <Button type="primary" htmlType="submit" loading={pwdLoading}>确认修改</Button>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

const PrivateRouteWithLayout = ({ children }: { children: React.ReactNode }) => (
  <PrivateRoute>
    <AppLayout>{children}</AppLayout>
  </PrivateRoute>
)

const App = () => (
  <ConfigProvider locale={zhCN}>
    <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/change-password" element={<ChangePassword />} />
        <Route path="/dashboard" element={<PrivateRouteWithLayout><Dashboard /></PrivateRouteWithLayout>} />
        <Route path="/work-orders" element={<PrivateRouteWithLayout><WorkOrderManagement /></PrivateRouteWithLayout>} />
        <Route path="/phones" element={<PrivateRouteWithLayout><PhoneManagement /></PrivateRouteWithLayout>} />
        <Route path="/phone-ownership" element={<PrivateRouteWithLayout><PhoneOwnershipPage /></PrivateRouteWithLayout>} />
        <Route path="/extension-pools" element={<PrivateRouteWithLayout><ExtensionPoolManagement /></PrivateRouteWithLayout>} />
        <Route path="/area-codes" element={<PrivateRouteWithLayout><AreaCodeManagement /></PrivateRouteWithLayout>} />
        <Route path="/devices" element={<PrivateRouteWithLayout><DeviceManagement /></PrivateRouteWithLayout>} />
        <Route path="/cost-centers" element={<PrivateRouteWithLayout><CostCenterManagement /></PrivateRouteWithLayout>} />
        <Route path="/bills" element={<PrivateRouteWithLayout><BillManagement /></PrivateRouteWithLayout>} />
        <Route path="/bill-allocations" element={<PrivateRouteWithLayout><BillAllocationManagement /></PrivateRouteWithLayout>} />
        <Route path="/invoices" element={<PrivateRouteWithLayout><InvoiceManagement /></PrivateRouteWithLayout>} />
        <Route path="/reconciliations" element={<PrivateRouteWithLayout><SubsidiaryReconciliationPage /></PrivateRouteWithLayout>} />
        <Route path="/orgs" element={<PrivateRouteWithLayout><OrgManagement /></PrivateRouteWithLayout>} />
        <Route path="/reports" element={<PrivateRouteWithLayout><ReportCenter /></PrivateRouteWithLayout>} />
        <Route path="/roles" element={<PrivateRouteWithLayout><RoleManagement /></PrivateRouteWithLayout>} />
        <Route path="/user-management" element={<PrivateRouteWithLayout><UserManagement /></PrivateRouteWithLayout>} />
        <Route path="/audit-logs" element={<PrivateRouteWithLayout><AuditLogManagement /></PrivateRouteWithLayout>} />
        <Route path="/announcements" element={<PrivateRouteWithLayout><AnnouncementManagement /></PrivateRouteWithLayout>} />
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  </ConfigProvider>
)

export default App
