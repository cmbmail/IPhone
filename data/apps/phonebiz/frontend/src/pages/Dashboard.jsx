import React, { useEffect, useState } from 'react'
import { Row, Col, Card, Statistic, List, Tag } from 'antd'
import {
  TeamOutlined,
  UserOutlined,
  PhoneOutlined,
  FileTextOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  ExclamationCircleOutlined
} from '@ant-design/icons'
import { getPhonePage, getOrderPage, getOrgList, getEmployeePage } from '../api/index.js'

const Dashboard = () => {
  const [stats, setStats] = useState({
    orgCount: 0,
    employeeCount: 0,
    phoneCount: 0,
    orderCount: 0,
    pendingOrders: 0,
    completedOrders: 0
  })
  const [recentOrders, setRecentOrders] = useState([])

  useEffect(() => {
    loadData()
  }, [])

  const loadData = async () => {
    try {
      const [orgRes, empRes, phoneRes, orderRes] = await Promise.all([
        getOrgList(),
        getEmployeePage({ current: 1, size: 1 }),
        getPhonePage({ current: 1, size: 1 }),
        getOrderPage({ current: 1, size: 10 })
      ])

      const orders = orderRes.data?.records || []
      const pendingOrders = orders.filter(o => 
        o.status === 'PENDING' || o.status === 'PROCESSING'
      ).length
      const completedOrders = orders.filter(o => o.status === 'COMPLETED').length

      setStats({
        orgCount: orgRes.data?.length || 0,
        employeeCount: empRes.data?.total || 0,
        phoneCount: phoneRes.data?.total || 0,
        orderCount: orders.length,
        pendingOrders,
        completedOrders
      })

      setRecentOrders(orders.slice(0, 5))
    } catch (error) {
      console.error('加载数据失败:', error)
    }
  }

  const getStatusTag = (status) => {
    const statusMap = {
      PENDING: { color: 'orange', text: '待处理' },
      PROCESSING: { color: 'blue', text: '处理中' },
      APPROVED: { color: 'cyan', text: '已审批' },
      COMPLETED: { color: 'green', text: '已完成' },
      REJECTED: { color: 'red', text: '已拒绝' },
      CANCELLED: { color: 'default', text: '已取消' }
    }
    const s = statusMap[status] || { color: 'default', text: status }
    return <Tag color={s.color}>{s.text}</Tag>
  }

  return (
    <div>
      <h2 style={{ marginBottom: 24 }}>仪表盘</h2>
      
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}>
          <Card>
            <Statistic
              title="组织数量"
              value={stats.orgCount}
              prefix={<TeamOutlined style={{ color: '#1890ff' }} />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="员工数量"
              value={stats.employeeCount}
              prefix={<UserOutlined style={{ color: '#52c41a' }} />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="号码数量"
              value={stats.phoneCount}
              prefix={<PhoneOutlined style={{ color: '#722ed1' }} />}
              valueStyle={{ color: '#722ed1' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="工单数量"
              value={stats.orderCount}
              prefix={<FileTextOutlined style={{ color: '#fa8c16' }} />}
              valueStyle={{ color: '#fa8c16' }}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={16}>
        <Col span={12}>
          <Card title="工单统计" size="small">
            <Row gutter={16}>
              <Col span={12}>
                <Statistic
                  title="待处理"
                  value={stats.pendingOrders}
                  prefix={<ClockCircleOutlined style={{ color: '#faad14' }} />}
                />
              </Col>
              <Col span={12}>
                <Statistic
                  title="已完成"
                  value={stats.completedOrders}
                  prefix={<CheckCircleOutlined style={{ color: '#52c41a' }} />}
                />
              </Col>
            </Row>
          </Card>
        </Col>
        <Col span={12}>
          <Card title="最近工单" size="small">
            <List
              dataSource={recentOrders}
              renderItem={(item) => (
                <List.Item>
                  <List.Item.Meta
                    avatar={<ExclamationCircleOutlined style={{ color: '#faad14', fontSize: 20 }} />}
                    title={item.title}
                    description={
                      <div>
                        <span>{item.orderNo}</span>
                        <span style={{ marginLeft: 16 }}>
                          {item.createTime && new Date(item.createTime).toLocaleString()}
                        </span>
                      </div>
                    }
                  />
                  {getStatusTag(item.status)}
                </List.Item>
              )}
            />
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default Dashboard
