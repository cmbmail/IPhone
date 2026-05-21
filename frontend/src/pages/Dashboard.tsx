import { useState, useEffect } from 'react'
import { Card, Row, Col, Statistic, Progress, Table, Space, Tag, Spin } from 'antd'
import { PhoneOutlined, TeamOutlined, FileTextOutlined, DesktopOutlined } from '@ant-design/icons'
import { statisticsApi, type PhoneStatistics, type DeviceStatistics } from '@/api/statistics'
import { request } from '@/api/request'

interface OrgStat { id: number; name: string; sort_order: number }

const Dashboard = () => {
  const [phoneStats, setPhoneStats] = useState<PhoneStatistics | null>(null)
  const [deviceStats, setDeviceStats] = useState<DeviceStatistics | null>(null)
  const [orgCount, setOrgCount] = useState(0)
  const [userCount, setUserCount] = useState(0)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true)
      try {
        const [phoneRes, deviceRes, orgRes, userRes] = await Promise.allSettled([
          statisticsApi.getPhoneStats(),
          statisticsApi.getDeviceStats(),
          request.get('/orgs'),
          request.get('/users'),
        ])
        if (phoneRes.status === 'fulfilled') setPhoneStats(phoneRes.value.data.data)
        if (deviceRes.status === 'fulfilled') setDeviceStats(deviceRes.value.data.data)
        if (orgRes.status === 'fulfilled') setOrgCount((orgRes.value.data.data || []).length)
        if (userRes.status === 'fulfilled') setUserCount((userRes.value.data.data || []).length)
      } catch {
        // Silently fail, show zeros
      } finally {
        setLoading(false)
      }
    }
    fetchData()
  }, [])

  const totalPhones = phoneStats?.totalCount || 0
  const activePhones = phoneStats?.allocatedCount || 0
  const idlePhones = phoneStats?.idleCount || 0
  const phoneActiveRate = totalPhones > 0 ? Math.round(activePhones / totalPhones * 100) : 0

  const totalDevices = deviceStats?.totalCount || 0
  const onlineDevices = deviceStats?.onlineCount || 0
  const offlineDevices = deviceStats?.offlineCount || 0
  const deviceOnlineRate = deviceStats?.onlineRate || 0

  const recentBills: {id: number; orgName: string; month: string; amount: number; status: string}[] = []

  const columns = [
    { title: '组织', dataIndex: 'orgName', key: 'orgName' },
    { title: '账期', dataIndex: 'month', key: 'month' },
    { title: '金额', dataIndex: 'amount', key: 'amount', render: (v: number) => `¥${v.toLocaleString()}` },
    { title: '状态', dataIndex: 'status', key: 'status', render: (s: string) => {
      let color = 'default'
      if (s === '已导入') color = 'processing'
      if (s === '已分摊') color = 'success'
      if (s === '已确认') color = 'success'
      return <Tag color={color}>{s}</Tag>
    }},
  ]

  if (loading) {
    return <div style={{ textAlign: 'center', padding: 100 }}><Spin size="large" /></div>
  }

  return (
    <div>
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}>
          <Card>
            <Statistic title="号码总数" value={totalPhones} prefix={<PhoneOutlined />} valueStyle={{ color: '#1890ff' }} />
            <div style={{ marginTop: 16 }}>
              <Progress percent={phoneActiveRate} status="active" strokeColor="#52c41a" />
              <div style={{ marginTop: 8, fontSize: 12, color: '#8c8c8c' }}>
                在用: {activePhones} | 空闲: {idlePhones} | 停机: {phoneStats?.stoppedCount || 0}
              </div>
            </div>
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="组织数量" value={orgCount} prefix={<TeamOutlined />} valueStyle={{ color: '#52c41a' }} />
            <div style={{ marginTop: 16 }}>
              <Progress percent={orgCount > 0 ? Math.round(orgCount / 15 * 100) : 0} strokeColor="#52c41a" />
              <div style={{ marginTop: 8, fontSize: 12, color: '#8c8c8c' }}>
                用户总数: {userCount}
              </div>
            </div>
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="设备总数" value={totalDevices} prefix={<DesktopOutlined />} valueStyle={{ color: '#722ed1' }} />
            <div style={{ marginTop: 16 }}>
              <Progress percent={Math.round(deviceOnlineRate)} strokeColor="#722ed1" />
              <div style={{ marginTop: 8, fontSize: 12, color: '#8c8c8c' }}>
                在线: {onlineDevices} | 离线: {offlineDevices} | 未注册: {deviceStats?.unregisteredCount || 0}
              </div>
            </div>
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="号码状态分布" value={phoneStats?.statusDistribution ? Object.keys(phoneStats?.statusDistribution).length : 0} prefix={<FileTextOutlined />} valueStyle={{ color: '#faad14' }} />
            <div style={{ marginTop: 16 }}>
              <Space>
                {phoneStats?.statusDistribution && Object.entries(phoneStats?.statusDistribution).map(([k, v]) => (
                  <div key={k} style={{ textAlign: 'center' }}>
                    <div style={{ fontSize: 16, fontWeight: 600, color: '#595959' }}>{v}</div>
                    <div style={{ fontSize: 11, color: '#8c8c8c' }}>{k}</div>
                  </div>
                ))}
              </Space>
            </div>
          </Card>
        </Col>
      </Row>

      <Row gutter={16}>
        <Col span={24}>
          <Card title="最近账单">
            <Table columns={columns} dataSource={recentBills} rowKey="id" pagination={false} />
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default Dashboard
