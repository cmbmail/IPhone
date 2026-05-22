import { useState, useEffect } from 'react'
import { Card, Row, Col, Statistic, Progress, Table, Space, Tag, Spin, List, Modal, Typography } from 'antd'
import { PhoneOutlined, TeamOutlined, FileTextOutlined, DesktopOutlined } from '@ant-design/icons'
import { statisticsApi, type PhoneStatistics, type DeviceStatistics } from '@/api/statistics'
import { request } from '@/api/request'
import { announcementApi, type Announcement } from '@/api/announcement'

const ANNOUNCEMENT_TYPE_NAMES: Record<string, string> = {
  SYSTEM: '系统公告', MAINTENANCE: '维护通知', POLICY: '政策变更', OPERATION: '运营通知', OTHER: '其他',
}
const ANNOUNCEMENT_TYPE_COLORS: Record<string, string> = {
  SYSTEM: 'blue', MAINTENANCE: 'orange', POLICY: 'purple', OPERATION: 'cyan', OTHER: 'default',
}
const WO_STATUS_NAMES: Record<string, string> = {
  PENDING: '待处理', ACCEPTED: '已接受', PROCESSING: '处理中', COMPLETED: '已完成', REJECTED: '已拒绝',
}
const WO_STATUS_COLORS: Record<string, string> = {
  PENDING: 'warning', ACCEPTED: 'processing', PROCESSING: 'processing', COMPLETED: 'success', REJECTED: 'error',
}

interface RecentBill {
  id: number
  orgName: string
  month: string
  amount: number
  status: string
}

interface WorkOrder {
  id: number
  workOrderNo: string
  title: string
  priority: string
  status: string
}

const Dashboard = () => {
  const [phoneStats, setPhoneStats] = useState<PhoneStatistics | null>(null)
  const [deviceStats, setDeviceStats] = useState<DeviceStatistics | null>(null)
  const [orgCount, setOrgCount] = useState(0)
  const [userCount, setUserCount] = useState(0)
  const [recentBills, setRecentBills] = useState<RecentBill[]>([])
  const [announcements, setAnnouncements] = useState<Announcement[]>([])
  const [workOrders, setWorkOrders] = useState<WorkOrder[]>([])
  const [loading, setLoading] = useState(true)
  const [selectedAnnouncement, setSelectedAnnouncement] = useState<Announcement | null>(null)

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true)
      try {
        const [phoneRes, deviceRes, orgRes, userRes, billRes, annRes, woRes] = await Promise.allSettled([
          statisticsApi.getPhoneStats(),
          statisticsApi.getDeviceStats(),
          request.get('/orgs'),
          request.get('/users'),
          request.get('/bill-allocations', { params: { billMonth: new Date().toISOString().slice(0, 7), page: 0, size: 5 } }),
          announcementApi.getLatest(),
          request.get('/work-orders', { params: { page: 0, size: 10 } }),
        ])
        if (phoneRes.status === 'fulfilled') setPhoneStats(phoneRes.value.data.data)
        if (deviceRes.status === 'fulfilled') setDeviceStats(deviceRes.value.data.data)
        if (orgRes.status === 'fulfilled') setOrgCount((orgRes.value.data.data || []).length)
        if (userRes.status === 'fulfilled') setUserCount((userRes.value.data.data || []).length)
        if (billRes.status === 'fulfilled') {
          const billData = billRes.value.data?.data?.content || []
          const mapped: RecentBill[] = billData.map((b: Record<string, unknown>) => ({
            id: b.id as number,
            orgName: (b.orgName as string) || '-',
            month: (b.billMonth as string) || '-',
            amount: (b.allocateAmount as number) || 0,
            status: b.financeConfirmSubmit === 'APPROVED' ? '已确认' : b.adminConfirmOrg === 'APPROVED' ? '已分摊' : '已导入',
          }))
          setRecentBills(mapped)
        }
        if (annRes.status === 'fulfilled') {
          setAnnouncements(annRes.value.data?.data || [])
        }
        if (woRes.status === 'fulfilled') {
          setWorkOrders(woRes.value.data?.data?.content || [])
        }
      } catch {
        // Silently fail
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

  const billColumns = [
    { title: '组织', dataIndex: 'orgName', key: 'orgName' },
    { title: '账期', dataIndex: 'month', key: 'month' },
    { title: '金额', dataIndex: 'amount', key: 'amount', render: (v: number) => `\u00a5${v.toLocaleString()}` },
    { title: '状态', dataIndex: 'status', key: 'status', render: (s: string) => {
      let color = 'default'
      if (s === '已导入') color = 'processing'
      if (s === '已分摊' || s === '已确认') color = 'success'
      return <Tag color={color}>{s}</Tag>
    }},
  ]

  const woColumns = [
    { title: '工单号', dataIndex: 'workOrderNo', key: 'no', width: 120 },
    { title: '标题', dataIndex: 'title', key: 'title', ellipsis: true },
    {
      title: '优先级', dataIndex: 'priority', key: 'priority', width: 80,
      render: (p: string) => {
        const colors: Record<string, string> = { HIGH: 'error', MEDIUM: 'warning', LOW: 'success' }
        const names: Record<string, string> = { HIGH: '高', MEDIUM: '中', LOW: '低' }
        return <Tag color={colors[p]}>{names[p] || p}</Tag>
      }
    },
    {
      title: '状态', dataIndex: 'status', key: 'status', width: 90,
      render: (s: string) => <Tag color={WO_STATUS_COLORS[s]}>{WO_STATUS_NAMES[s] || s}</Tag>
    },
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
              <Progress percent={orgCount > 0 ? Math.min(Math.round(orgCount / 15 * 100), 100) : 0} strokeColor="#52c41a" />
              <div style={{ marginTop: 8, fontSize: 12, color: '#8c8c8c' }}>用户总数: {userCount}</div>
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
            <Statistic title="号码状态分布" value={phoneStats?.statusDistribution ? Object.keys(phoneStats.statusDistribution).length : 0} prefix={<FileTextOutlined />} valueStyle={{ color: '#faad14' }} />
            <div style={{ marginTop: 16 }}>
              <Space>
                {phoneStats?.statusDistribution && Object.entries(phoneStats.statusDistribution).map(([k, v]) => (
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

      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={18}>
          <Card title="最近账单">
            <Table columns={billColumns} dataSource={recentBills} rowKey="id" pagination={false} locale={{ emptyText: '暂无账单数据' }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card title="通知公告" extra={<a href="/announcements" style={{ fontSize: 13 }}>查看全部</a>}>
            {announcements.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '24px 0', color: '#999' }}>暂无公告</div>
            ) : (
              <List
                dataSource={announcements}
                renderItem={(item: Announcement) => (
                  <List.Item style={{ padding: '8px 0', borderBottom: '1px solid #f0f0f0', cursor: 'pointer' }} onClick={() => setSelectedAnnouncement(item)}>
                    <div style={{ width: '100%' }}>
                      <div style={{ marginBottom: 4 }}>
                        <Tag color={ANNOUNCEMENT_TYPE_COLORS[item.announcementType] || 'default'} style={{ fontSize: 11 }}>
                          {ANNOUNCEMENT_TYPE_NAMES[item.announcementType] || item.announcementType}
                        </Tag>
                      </div>
                      <div style={{ fontSize: 14, fontWeight: 500, marginBottom: 4, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', color: '#1890ff' }}>
                        {item.title}
                      </div>
                      <div style={{ fontSize: 12, color: '#999' }}>
                        {item.createdBy || '系统'} | {item.createdAt ? item.createdAt.replace('T', ' ').substring(0, 16) : '-'}
                      </div>
                    </div>
                  </List.Item>
                )}
              />
            )}
          </Card>
        </Col>
      </Row>

      <Row gutter={16}>
        <Col span={24}>
          <Card title="最近工单" extra={<a href="/work-orders" style={{ fontSize: 13 }}>查看全部</a>}>
            <Table
              columns={woColumns}
              dataSource={workOrders}
              rowKey="id"
              pagination={false}
              size="small"
              locale={{ emptyText: '暂无工单' }}
            />
          </Card>
        </Col>
      </Row>

      <Modal
        title={selectedAnnouncement?.title || '公告详情'}
        open={!!selectedAnnouncement}
        onCancel={() => setSelectedAnnouncement(null)}
        footer={null}
        width={720}
        centered
      >
        {selectedAnnouncement && (
          <div>
            <Space style={{ marginBottom: 16 }}>
              <Tag color={ANNOUNCEMENT_TYPE_COLORS[selectedAnnouncement.announcementType] || 'default'}>
                {ANNOUNCEMENT_TYPE_NAMES[selectedAnnouncement.announcementType] || selectedAnnouncement.announcementType}
              </Tag>
              <Tag color={selectedAnnouncement.priority === 'URGENT' ? 'red' : selectedAnnouncement.priority === 'IMPORTANT' ? 'orange' : 'default'}>
                {{ URGENT: '紧急', IMPORTANT: '重要', NORMAL: '普通' }[selectedAnnouncement.priority] || selectedAnnouncement.priority}
              </Tag>
              <Tag color={selectedAnnouncement.status === 'PUBLISHED' ? 'green' : 'default'}>
                {{ PUBLISHED: '已发布', DRAFT: '草稿', ARCHIVED: '已归档' }[selectedAnnouncement.status] || selectedAnnouncement.status}
              </Tag>
            </Space>
            <div style={{ color: '#999', marginBottom: 16, fontSize: 13 }}>
              发布人: {selectedAnnouncement.createdBy || '系统'} | 发布时间: {selectedAnnouncement.createdAt ? selectedAnnouncement.createdAt.replace('T', ' ').substring(0, 16) : '-'}
            </div>
            <div style={{ borderTop: '1px solid #f0f0f0', paddingTop: 16 }}>
              <Typography.Paragraph style={{ whiteSpace: 'pre-wrap', fontSize: 14, lineHeight: 1.8 }}>
                {selectedAnnouncement.content || '无内容'}
              </Typography.Paragraph>
            </div>
          </div>
        )}
      </Modal>
    </div>
  )
}

export default Dashboard
