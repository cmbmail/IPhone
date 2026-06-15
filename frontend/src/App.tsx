import { useState, lazy, Suspense } from 'react'
import {
  Spin,
  ConfigProvider,
  Menu,
  Button,
  Avatar,
  Input,
  Dropdown,
  Modal,
  Form,
  Popconfirm,
  message,
} from 'antd'
import { BrowserRouter, Routes, Route, Navigate, useNavigate, useLocation } from 'react-router-dom'
import {
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  LogoutOutlined,
  LockOutlined,
  TeamOutlined,
  WalletOutlined,
  PhoneOutlined,
  AppstoreOutlined,
  EnvironmentOutlined,
  DesktopOutlined,
  CheckOutlined,
  BarChartOutlined,
  CarryOutOutlined,
  AccountBookOutlined,
  SwapOutlined,
  AuditOutlined,
  ApartmentOutlined,
  SafetyCertificateOutlined,
  FileTextOutlined,
  NotificationOutlined,
} from '@ant-design/icons'
import zhCN from 'antd/locale/zh_CN'
import { PrivateRoute } from '@/components/PrivateRoute'
import { PermissionGuard } from '@/components/PermissionGuard'
import { useAuthStore } from '@/stores/authStore'
import { usePermission } from '@/hooks/usePermission'
import { ROLE_LABEL } from '@/types/auth'
import type { Role } from '@/types/auth'
import { authApi } from '@/api/auth'
import Login from '@/pages/Login'

import NotificationPopover from '@/components/NotificationPopover'
import GlobalSearch from '@/components/GlobalSearch'
import { ErrorBoundary } from '@/components/ErrorBoundary'

// Lazy-loaded page components for code-splitting
const Dashboard = lazy(() => import('@/pages/Dashboard'))
const OrgManagement = lazy(() => import('@/pages/OrgManagement'))
const CostCenterManagement = lazy(() => import('@/pages/CostCenterManagement'))
const PhoneManagement = lazy(() => import('@/pages/PhoneManagement'))
const PhoneOwnershipPage = lazy(() => import('@/pages/PhoneOwnership'))
const ExtensionPoolManagement = lazy(() => import('@/pages/ExtensionPoolManagement'))
const AreaCodeManagement = lazy(() => import('@/pages/AreaCodeManagement'))
const BillAllocationManagement = lazy(() => import('@/pages/BillAllocationManagement'))
const InvoiceManagement = lazy(() => import('@/pages/InvoiceManagement'))
const SubsidiaryReconciliationPage = lazy(() => import('@/pages/SubsidiaryReconciliationPage'))
const BillManagement = lazy(() => import('@/pages/BillManagement'))
const ReportCenter = lazy(() => import('@/pages/ReportCenter'))
const WorkOrderManagement = lazy(() => import('@/pages/WorkOrderManagement'))
const DeviceManagement = lazy(() => import('@/pages/DeviceManagement'))
const RoleManagement = lazy(() => import('@/pages/RoleManagement'))
const UserManagement = lazy(() => import('@/pages/UserManagement'))
const AuditLogManagement = lazy(() => import('@/pages/AuditLogManagement'))
const AnnouncementManagement = lazy(() => import('@/pages/AnnouncementManagement'))
const ChangePassword = lazy(() => import('@/pages/ChangePassword'))

// Layout components replaced with plain divs for reliable flex layout

// Menu items with permission metadata
// allowedRoles: who can see this menu (admin always passes)
// permission: alternative to role check (admin always passes)
const menuItems = [
  {
    key: 'sub-work-orders',
    icon: <CarryOutOutlined />,
    label: '工单管理',
    children: [{ key: '/work-orders', icon: <CarryOutOutlined />, label: '工单管理', permission: 'wo:view' }],
  },
  {
    key: 'sub-phones',
    icon: <PhoneOutlined />,
    label: '号码管理',
    children: [
      { key: '/phones', icon: <PhoneOutlined />, label: '号码资源', permission: 'phone:view' },
      { key: '/extensionNumber-pools', icon: <AppstoreOutlined />, label: '分机池', permission: 'ext:view' },
      { key: '/area-codes', icon: <EnvironmentOutlined />, label: '区号管理', permission: 'areacode:view' },
      { key: '/devices', icon: <DesktopOutlined />, label: '设备管理', permission: 'device:view' },
      { key: '/phone-ownership', icon: <ApartmentOutlined />, label: '号码归属', permission: 'phone:view' },
    ],
  },
  {
    key: 'sub-bills',
    icon: <AccountBookOutlined />,
    label: '费用管理',
    children: [
      { key: '/cost-centers', icon: <WalletOutlined />, label: '成本中心', permission: 'cost:view' },
      { key: '/bills', icon: <AccountBookOutlined />, label: '账单管理', permission: 'bill:view' },
      { key: '/bill-allocations', icon: <SwapOutlined />, label: '账单分摊', permission: 'bill:allocate' },
      { key: '/invoices', icon: <CheckOutlined />, label: '发票管理', permission: 'inv:view' },
      { key: '/reconciliations', icon: <AuditOutlined />, label: '子公司对账', permission: 'recon:view' },
    ],
  },
  { key: '/orgs', icon: <TeamOutlined />, label: '用户管理', permission: 'org:view' },
  {
    key: 'sub-system',
    icon: <BarChartOutlined />,
    label: '系统管理',
    children: [
      { key: '/reports', icon: <BarChartOutlined />, label: '报表中心', permission: 'rpt:view' },
      { key: '/audit-logs', icon: <FileTextOutlined />, label: '审计日志', allowedRoles: ['admin'] as Role[] },
      { key: '/roles', icon: <SafetyCertificateOutlined />, label: '角色管理', allowedRoles: ['admin'] as Role[] },
      { key: '/announcements', icon: <NotificationOutlined />, label: '通知公告', anyRole: ['admin', 'ops'] as Role[] },
    ],
  },
]

const pageTitleMap: Record<string, string> = {
  '/dashboard': '系统看板',
  '/work-orders': '工单管理',
  '/phones': '号码资源',
  '/phone-ownership': '号码归属',
  '/extensionNumber-pools': '分机池',
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
  const { hasPermission, hasAnyRole } = usePermission()
  const [pwdModalOpen, setPwdModalOpen] = useState(false)
  const [pwdLoading, setPwdLoading] = useState(false)
  const [pwdForm] = Form.useForm()
  const [userMenuOpen, setUserMenuOpen] = useState(false)
  const [logoutConfirmOpen, setLogoutConfirmOpen] = useState(false)

  // Filter menu items based on permissions
  const filteredMenuItems = menuItems
    .map((group) => {
      if ('children' in group && group.children) {
        const filteredChildren = group.children.filter((item) => {
          if (item.allowedRoles) return hasAnyRole(item.allowedRoles)
          if (item.permission) return hasPermission(item.permission)
          if ((item as { anyRole?: Role[] }).anyRole) return hasAnyRole((item as { anyRole?: Role[] }).anyRole!)
          return true
        })
        return filteredChildren.length > 0 ? { ...group, children: filteredChildren } : null
      }
      // Top-level item (no children)
      if ((group as { allowedRoles?: Role[] }).allowedRoles) return hasAnyRole((group as { allowedRoles?: Role[] }).allowedRoles!) ? group : null
      if ((group as { permission?: string }).permission) return hasPermission((group as { permission?: string }).permission!) ? group : null
      return group
    })
    .filter(Boolean)

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
      await authApi.changePassword({
        oldPassword: values.oldPassword,
        newPassword: values.newPassword,
      })
      message.success('密码修改成功，请重新登录')
      setPwdModalOpen(false)
      clearAuth()
      navigate('/login')
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } } }
      message.error(err.response?.data?.message || '密码修改失败')
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
      <div className={`sidebar ${collapsed ? 'collapsed' : ''}`}>
        <div
          className="sidebar-logo"
          style={{ cursor: 'pointer' }}
          onClick={() => navigate('/dashboard')}
        >
          <div className="sidebar-logo-icon">P</div>
          <span className="sidebar-logo-text">PhoneBiz</span>
        </div>
        <div className="sidebar-menu">
          <Menu
            theme="dark"
            mode="inline"
            selectedKeys={[location.pathname === '/dashboard' ? '' : location.pathname]}
            openKeys={openKeys}
            onOpenChange={setOpenKeys}
            items={filteredMenuItems}
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
          ></Button>
        </div>
      </div>

      <div className="main-wrapper">
        <div className="header">
          <div className="header-left">
            <div className="page-heading">
              <h1>{pageTitle}</h1>
              <span>
                {location.pathname === '/dashboard'
                  ? '查看系统运营概况和数据统计'
                  : '管理您的企业通信资源'}
              </span>
            </div>
          </div>
          <div className="header-right">
            <GlobalSearch />

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
                <Avatar
                  style={{
                    background:
                      'linear-gradient(145deg, var(--accent) 0%, var(--accent-dark) 100%)',
                    width: 32,
                    height: 32,
                    borderRadius: '50%',
                    fontWeight: 600,
                    fontSize: 13,
                  }}
                >
                  {user?.username?.charAt(0).toUpperCase() || 'U'}
                </Avatar>
                <div
                  style={{
                    display: 'flex',
                    flexDirection: 'column',
                    lineHeight: 'normal',
                    flexShrink: 0,
                  }}
                >
                  <span style={{ fontSize: 13, fontWeight: 500, color: 'var(--text-primary)' }}>
                    {user?.username || '用户'}
                  </span>
                  <span style={{ fontSize: 11, color: 'var(--text-muted)' }}>{user?.role ? ROLE_LABEL[user.role] : '用户'}</span>
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
        <Form
          form={pwdForm}
          layout="vertical"
          onFinish={handleChangePassword}
          style={{ marginTop: 16 }}
        >
          <Form.Item
            name="oldPassword"
            label="当前密码"
            rules={[{ required: true, message: '请输入当前密码' }]}
          >
            <Input.Password placeholder="请输入当前密码" />
          </Form.Item>
          <Form.Item
            name="newPassword"
            label="新密码"
            rules={[
              { required: true, message: '请输入新密码' },
              { min: 8, message: '密码长度至少8位' },
              {
                pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*]).{8,}$/,
                message: '需包含大小写字母、数字和特殊字符',
              },
            ]}
          >
            <Input.Password placeholder="请输入新密码（至少8位）" />
          </Form.Item>
          <Form.Item
            name="confirmPassword"
            label="确认新密码"
            dependencies={['newPassword']}
            rules={[
              { required: true, message: '请再次输入新密码' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('newPassword') === value) return Promise.resolve()
                  return Promise.reject(new Error('两次输入的密码不一致'))
                },
              }),
            ]}
          >
            <Input.Password placeholder="请再次输入新密码" />
          </Form.Item>
          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Button style={{ marginRight: 8 }} onClick={() => setPwdModalOpen(false)}>
              取消
            </Button>
            <Button type="primary" htmlType="submit" loading={pwdLoading}>
              确认修改
            </Button>
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

/** Route wrapper with permission guard */
const GuardedRoute = ({ children, permission, allowedRoles }: {
  children: React.ReactNode
  permission?: string
  allowedRoles?: Role[]
}) => (
  <PrivateRouteWithLayout>
    <PermissionGuard permission={permission} anyRole={allowedRoles}>
      {children}
    </PermissionGuard>
  </PrivateRouteWithLayout>
)

const App = () => (
  <ConfigProvider locale={zhCN}>
    <BrowserRouter>
      <ErrorBoundary>
        <Suspense
          fallback={
            <div
              style={{
                display: 'flex',
                justifyContent: 'center',
                alignItems: 'center',
                height: '100%',
              }}
            >
              <Spin size="large" />
            </div>
          }
        >
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route path="/change-password" element={<ChangePassword />} />
            <Route path="/dashboard" element={<PrivateRouteWithLayout><Dashboard /></PrivateRouteWithLayout>} />
            <Route path="/work-orders" element={<GuardedRoute permission="wo:view"><WorkOrderManagement /></GuardedRoute>} />
            <Route path="/phones" element={<GuardedRoute permission="phone:view"><PhoneManagement /></GuardedRoute>} />
            <Route path="/phone-ownership" element={<GuardedRoute permission="phone:view"><PhoneOwnershipPage /></GuardedRoute>} />
            <Route path="/extensionNumber-pools" element={<GuardedRoute permission="ext:view"><ExtensionPoolManagement /></GuardedRoute>} />
            <Route path="/area-codes" element={<GuardedRoute permission="areacode:view"><AreaCodeManagement /></GuardedRoute>} />
            <Route path="/devices" element={<GuardedRoute permission="device:view"><DeviceManagement /></GuardedRoute>} />
            <Route path="/cost-centers" element={<GuardedRoute permission="cost:view"><CostCenterManagement /></GuardedRoute>} />
            <Route path="/bills" element={<GuardedRoute permission="bill:view"><BillManagement /></GuardedRoute>} />
            <Route path="/bill-allocations" element={<GuardedRoute permission="bill:allocate"><BillAllocationManagement /></GuardedRoute>} />
            <Route path="/invoices" element={<GuardedRoute permission="inv:view"><InvoiceManagement /></GuardedRoute>} />
            <Route path="/reconciliations" element={<GuardedRoute permission="recon:view"><SubsidiaryReconciliationPage /></GuardedRoute>} />
            <Route path="/orgs" element={<GuardedRoute permission="org:view"><OrgManagement /></GuardedRoute>} />
            <Route path="/reports" element={<GuardedRoute permission="rpt:view"><ReportCenter /></GuardedRoute>} />
            <Route path="/roles" element={<GuardedRoute allowedRoles={['admin']}><RoleManagement /></GuardedRoute>} />
            <Route path="/user-management" element={<GuardedRoute allowedRoles={['admin']}><UserManagement /></GuardedRoute>} />
            <Route path="/audit-logs" element={<GuardedRoute allowedRoles={['admin']}><AuditLogManagement /></GuardedRoute>} />
            <Route path="/announcements" element={<PrivateRouteWithLayout><AnnouncementManagement /></PrivateRouteWithLayout>} />
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </Suspense>
      </ErrorBoundary>
    </BrowserRouter>
  </ConfigProvider>
)

export default App
