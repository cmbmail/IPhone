import React, { useState } from 'react'
import { Layout, Menu, Avatar, Dropdown, theme } from 'antd'
import {
  DashboardOutlined,
  TeamOutlined,
  UserOutlined,
  SafetyOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  PhoneOutlined,
  FileTextOutlined,
  LogoutOutlined,
  DownOutlined
} from '@ant-design/icons'
import { useNavigate, useLocation, Outlet } from 'react-router-dom'
import { message } from 'antd'

const { Header, Sider, Content } = Layout

const LayoutComponent = () => {
  const [collapsed, setCollapsed] = useState(false)
  const navigate = useNavigate()
  const location = useLocation()
  const {
    token: { colorBgContainer, borderRadiusLG },
  } = theme.useToken()

  const username = localStorage.getItem('username') || 'admin'
  const realName = localStorage.getItem('realName') || '系统管理员'

  const menuItems = [
    {
      key: '/dashboard',
      icon: <DashboardOutlined />,
      label: '仪表盘',
    },
    {
      key: '/system',
      icon: <SafetyOutlined />,
      label: '系统管理',
      children: [
        { key: '/org', icon: <TeamOutlined />, label: '组织架构' },
        { key: '/employee', icon: <UserOutlined />, label: '员工管理' },
        { key: '/role', icon: <SafetyOutlined />, label: '角色管理' },
        { key: '/menu', icon: <MenuFoldOutlined />, label: '菜单管理' },
      ],
    },
    {
      key: '/phone',
      icon: <PhoneOutlined />,
      label: '号码管理',
    },
    {
      key: '/order',
      icon: <FileTextOutlined />,
      label: '工单管理',
    },
  ]

  const handleLogout = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('realName')
    message.success('已退出登录')
    navigate('/login', { replace: true })
  }

  const userItems = [
    {
      key: '1',
      label: '个人信息',
      icon: <UserOutlined />,
    },
    {
      type: 'divider',
    },
    {
      key: '2',
      label: '退出登录',
      icon: <LogoutOutlined />,
      onClick: handleLogout,
    },
  ]

  const getSelectedKeys = () => {
    const path = location.pathname
    if (path === '/' || path === '') {
      return ['/dashboard']
    }
    return [path]
  }

  const getOpenKeys = () => {
    const path = location.pathname
    if (path.startsWith('/org') || path.startsWith('/employee') || path.startsWith('/role') || path.startsWith('/menu')) {
      return ['/system']
    }
    return []
  }

  return (
    <Layout style={{ height: '100vh' }}>
      <Sider trigger={null} collapsible collapsed={collapsed}>
        <div style={{
          height: 64,
          margin: 16,
          background: 'rgba(255, 255, 255, 0.2)',
          borderRadius: 8,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: 'white',
          fontSize: collapsed ? 14 : 18,
          fontWeight: 'bold',
        }}>
          {collapsed ? 'CMB' : '招商银行'}
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={getSelectedKeys()}
          defaultOpenKeys={getOpenKeys()}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
        />
      </Sider>
      <Layout>
        <Header style={{
          padding: '0 24px',
          background: colorBgContainer,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
        }}>
          <div style={{ display: 'flex', alignItems: 'center' }}>
            {React.createElement(collapsed ? MenuUnfoldOutlined : MenuFoldOutlined, {
              className: 'trigger',
              onClick: () => setCollapsed(!collapsed),
              style: { fontSize: 18, cursor: 'pointer' },
            })}
            <span style={{ marginLeft: 16, fontSize: 18, fontWeight: 'bold' }}>
              电话业务管理系统
            </span>
          </div>
          <Dropdown menu={{ items: userItems }}>
            <div style={{ cursor: 'pointer', display: 'flex', alignItems: 'center' }}>
              <Avatar icon={<UserOutlined />} style={{ marginRight: 8 }} />
              <span>{realName}</span>
              <DownOutlined style={{ marginLeft: 8 }} />
            </div>
          </Dropdown>
        </Header>
        <Content
          style={{
            margin: '24px 16px',
            padding: 24,
            minHeight: 280,
            background: colorBgContainer,
            borderRadius: borderRadiusLG,
          }}
        >
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}

export default LayoutComponent
