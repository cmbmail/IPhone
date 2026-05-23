import { useState } from 'react'
import {
  Table,
  Card,
  Select,
  Tag,
  Space,
  Input,
  Dropdown,
  message,
  Row,
  Col,
  Statistic,
} from 'antd'
import { useQuery } from '@tanstack/react-query'
import { request } from '@/api/request'

// 后端动态计算状态: 0=闲置 1=占用 2=分配中
const STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '闲置', color: 'default' },
  1: { label: '占用', color: 'blue' },
  2: { label: '分配中', color: 'orange' },
}

interface PhoneView {
  id: number
  phoneNumber: string
  employeeNo: string | null
  employeeName: string | null
  extensions: string[]
  macAddresses: string[]
  branchName: string | null
  deptName: string | null
  status: number
  orgId: number | null
}

const PhoneManagement = () => {
  const [page, setPage] = useState(0)
  const [pageSize] = useState(20)
  const [keyword, setKeyword] = useState('')
  const [filterStatus, setFilterStatus] = useState<number | undefined>(undefined)
  const [filterOrgId, setFilterOrgId] = useState<number | undefined>(undefined)

  const { data: listData, isLoading } = useQuery({
    queryKey: ['phone-views', keyword, filterStatus, filterOrgId, page],
    queryFn: async () => {
      const params: Record<string, unknown> = { page, size: pageSize }
      if (keyword) params.keyword = keyword
      if (filterStatus !== undefined) params.status = filterStatus
      if (filterOrgId) params.orgId = filterOrgId
      const res = await request.get('/phone-views', { params })
      return res.data
    },
  })

  const { data: orgsData } = useQuery({
    queryKey: ['orgs'],
    queryFn: async () => {
      const res = await request.get('/orgs')
      return res.data?.data || []
    },
  })

  const orgs: any[] = orgsData || []
  const content: PhoneView[] = listData?.data?.content || []

  // 状态点击菜单
  const statusMenuItems = [
    { key: 'allocate', label: '分配' },
    { key: 'reclaim', label: '回收' },
    { key: 'change', label: '变更' },
    { key: 'cancel', label: '注销' },
  ]

  const handleStatusMenuClick = (_record: PhoneView, { key }: { key: string }) => {
    const labels: Record<string, string> = {
      allocate: '分配',
      reclaim: '回收',
      change: '变更',
      cancel: '注销',
    }
    message.info(`${labels[key]}操作将生成工单，功能开发中`)
  }

  const columns = [
    {
      title: '电话号码',
      dataIndex: 'phoneNumber',
      key: 'phoneNumber',
      width: 140,
      render: (v: string) => <span style={{ fontWeight: 600 }}>{v}</span>,
    },
    {
      title: '使用人',
      dataIndex: 'employeeName',
      key: 'employeeName',
      width: 100,
      render: (v: string) => v || <span style={{ color: '#bfbfbf' }}>-</span>,
    },
    {
      title: '员工ID',
      dataIndex: 'employeeNo',
      key: 'employeeNo',
      width: 100,
      render: (v: string) => v || '-',
    },
    {
      title: '分机号',
      dataIndex: 'extensions',
      key: 'extensions',
      width: 160,
      render: (exts: string[]) =>
        exts.length > 0 ? (
          exts.map((e, i) => (
            <Tag key={i} style={{ marginBottom: 2 }}>
              {e}
            </Tag>
          ))
        ) : (
          <span style={{ color: '#bfbfbf' }}>-</span>
        ),
    },
    {
      title: 'MAC',
      dataIndex: 'macAddresses',
      key: 'macAddresses',
      width: 180,
      render: (macs: string[]) =>
        macs.length > 0 ? (
          macs.map((m, i) => (
            <Tag key={i} color="cyan" style={{ marginBottom: 2, fontSize: 11 }}>
              {m}
            </Tag>
          ))
        ) : (
          <span style={{ color: '#bfbfbf' }}>-</span>
        ),
    },
    {
      title: '分行',
      dataIndex: 'branchName',
      key: 'branchName',
      width: 100,
      render: (v: string) => v || '-',
    },
    {
      title: '部门',
      dataIndex: 'deptName',
      key: 'deptName',
      width: 120,
      render: (v: string) => v || '-',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (s: number, record: PhoneView) => {
        const m = STATUS_MAP[s] || { label: '未知', color: 'default' }
        return (
          <Dropdown
            menu={{
              items: statusMenuItems,
              onClick: (info) => handleStatusMenuClick(record, info),
            }}
            trigger={['click']}
          >
            <Tag color={m.color} style={{ cursor: 'pointer' }}>
              {m.label}
            </Tag>
          </Dropdown>
        )
      },
    },
  ]

  // 统计
  const idleCount = content.filter((c) => c.status === 0).length
  const occupiedCount = content.filter((c) => c.status === 1).length
  const allocatingCount = content.filter((c) => c.status === 2).length

  return (
    <div>
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <Card>
            <Statistic title="闲置" value={idleCount} valueStyle={{ color: '#8c8c8c' }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="占用" value={occupiedCount} valueStyle={{ color: '#1677ff' }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="分配中" value={allocatingCount} valueStyle={{ color: '#fa8c16' }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="总数" value={listData?.data?.totalElements || 0} />
          </Card>
        </Col>
      </Row>

      <Card>
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: 16,
          }}
        >
          <Space>
            <Input.Search
              placeholder="搜索号码/员工ID"
              value={keyword}
              onChange={(e) => {
                setKeyword(e.target.value)
                setPage(0)
              }}
              style={{ width: 220 }}
            />
            <Select
              placeholder="状态筛选"
              value={filterStatus}
              onChange={(v) => {
                setFilterStatus(v)
                setPage(0)
              }}
              style={{ width: 130 }}
              allowClear
            >
              <Select.Option value={0}>闲置</Select.Option>
              <Select.Option value={1}>占用</Select.Option>
              <Select.Option value={2}>分配中</Select.Option>
            </Select>
            <Select
              placeholder="组织筛选"
              value={filterOrgId}
              onChange={(v) => {
                setFilterOrgId(v)
                setPage(0)
              }}
              style={{ width: 150 }}
              allowClear
            >
              {orgs
                .filter((o: any) => o.level >= 2)
                .map((o: any) => (
                  <Select.Option key={o.id} value={o.id}>
                    {o.name}
                  </Select.Option>
                ))}
            </Select>
          </Space>
        </div>

        <Table
          columns={columns}
          dataSource={content}
          loading={isLoading}
          rowKey="id"
          pagination={{
            current: page + 1,
            pageSize,
            total: listData?.data?.totalElements || 0,
            onChange: (p) => setPage(p - 1),
          }}
        />
      </Card>
    </div>
  )
}

export default PhoneManagement
