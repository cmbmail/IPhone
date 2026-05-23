import { useState } from 'react'
import { Table, Button, Card, Select, Tag, Space, Modal, message, Row, Col, Statistic } from 'antd'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  reconciliationApi,
  SubsidiaryReconciliation,
  ReconciliationSummary,
} from '@/api/reconciliation'
import { SyncOutlined, CheckOutlined } from '@ant-design/icons'

const { Option } = Select

const STATUS_COLORS: Record<number, string> = {
  0: 'warning',
  1: 'processing',
  2: 'success',
}

const STATUS_NAMES: Record<number, string> = {
  0: '待处理',
  1: '子公司已确认',
  2: '集团已确认',
}

const SubsidiaryReconciliationPage = () => {
  const [billMonth, setBillMonth] = useState<string>(new Date().toISOString().slice(0, 7))
  const queryClient = useQueryClient()

  const months = Array.from({ length: 12 }, (_, i) => {
    const d = new Date()
    d.setMonth(d.getMonth() - i)
    return d.toISOString().slice(0, 7)
  })

  const {
    data: allocationData,
    isLoading,
    refetch,
  } = useQuery({
    queryKey: ['reconciliations', billMonth],
    queryFn: () => reconciliationApi.getReconciliations({ billMonth, page: 0, size: 100 }),
  })

  const { data: summaryData } = useQuery({
    queryKey: ['reconciliation-summary', billMonth],
    queryFn: () => reconciliationApi.getSummary(billMonth),
  })

  const generateMutation = useMutation({
    mutationFn: () => reconciliationApi.generateReconciliation(billMonth),
    onSuccess: () => {
      message.success('对账已生成')
      queryClient.invalidateQueries({ queryKey: ['reconciliations'] })
      queryClient.invalidateQueries({ queryKey: ['reconciliation-summary'] })
    },
    onError: () => message.error('生成对账失败'),
  })

  const subsidiaryConfirmMutation = useMutation({
    mutationFn: (id: number) => reconciliationApi.subsidiaryConfirm(id),
    onSuccess: () => {
      message.success('子公司已确认')
      queryClient.invalidateQueries({ queryKey: ['reconciliations'] })
    },
    onError: () => message.error('确认失败'),
  })

  const groupConfirmMutation = useMutation({
    mutationFn: (id: number) => reconciliationApi.groupConfirm(id),
    onSuccess: () => {
      message.success('集团已确认')
      queryClient.invalidateQueries({ queryKey: ['reconciliations'] })
    },
    onError: () => message.error('确认失败'),
  })

  const handleSubsidiaryConfirm = (record: SubsidiaryReconciliation) => {
    Modal.confirm({
      title: '子公司确认',
      content: `确认 ${record.orgName} 的对账？`,
      onOk: () => subsidiaryConfirmMutation.mutate(record.id),
    })
  }

  const handleGroupConfirm = (record: SubsidiaryReconciliation) => {
    Modal.confirm({
      title: '集团确认',
      content: `批准 ${record.orgName} 的对账？`,
      onOk: () => groupConfirmMutation.mutate(record.id),
    })
  }

  const columns = [
    { title: '组织', dataIndex: 'orgName', key: 'orgName', width: 180 },
    { title: '电话数量', dataIndex: 'totalPhoneCount', key: 'totalPhoneCount', width: 120 },
    {
      title: '账单金额',
      dataIndex: 'totalBillAmount',
      key: 'totalBillAmount',
      width: 140,
      render: (val: number) => `¥${val?.toFixed(2) || '0.00'}`,
    },
    {
      title: '发票金额',
      dataIndex: 'invoiceAmount',
      key: 'invoiceAmount',
      width: 140,
      render: (val: number) => `¥${val?.toFixed(2) || '0.00'}`,
    },
    {
      title: '差异金额',
      dataIndex: 'diffAmount',
      key: 'diffAmount',
      width: 120,
      render: (val: number) => {
        const color = val === 0 ? 'green' : val > 0 ? 'red' : 'orange'
        return <span style={{ color }}>¥{val?.toFixed(2) || '0.00'}</span>
      },
    },
    {
      title: '差异率',
      dataIndex: 'diffPercentage',
      key: 'diffPercentage',
      width: 100,
      render: (val: number) => {
        const pct = val || 0
        const color = Math.abs(pct) < 1 ? 'green' : pct > 5 ? 'red' : 'orange'
        return <span style={{ color }}>{pct.toFixed(2)}%</span>
      },
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 140,
      render: (status: number) => (
        <Tag color={STATUS_COLORS[status] || 'default'}>{STATUS_NAMES[status]}</Tag>
      ),
    },
    {
      title: '操作',
      key: 'actions',
      width: 200,
      render: (_: any, record: SubsidiaryReconciliation) => (
        <Space>
          {record.subsidiaryConfirm === 0 && (
            <Button
              size="small"
              type="primary"
              icon={<CheckOutlined />}
              onClick={() => handleSubsidiaryConfirm(record)}
            >
              子公司确认
            </Button>
          )}
          {record.subsidiaryConfirm === 1 && record.groupConfirm === 0 && (
            <Button size="small" type="primary" onClick={() => handleGroupConfirm(record)}>
              集团确认
            </Button>
          )}
        </Space>
      ),
    },
  ]

  const reconciliations = allocationData?.data?.data?.content || []
  const summary = summaryData?.data?.data as ReconciliationSummary
  const totalAmount = reconciliations.reduce(
    (sum: number, r: SubsidiaryReconciliation) => sum + (r.totalBillAmount || 0),
    0
  )

  return (
    <div>
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <Card>
            <Statistic title="账单月份" value={billMonth} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="组织总数" value={summary?.totalOrgs || 0} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="总金额" value={`¥${totalAmount.toFixed(2)}`} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="待处理"
              value={summary?.pendingCount || 0}
              valueStyle={{ color: (summary?.pendingCount || 0) > 0 ? '#faad14' : '#3f8600' }}
            />
          </Card>
        </Col>
      </Row>

      <Card>
        <Space style={{ marginBottom: 16 }}>
          <Select value={billMonth} onChange={setBillMonth} style={{ width: 150 }}>
            {months.map((m) => (
              <Option key={m} value={m}>
                {m}
              </Option>
            ))}
          </Select>
          <Button
            type="primary"
            icon={<SyncOutlined spin={generateMutation.isPending} />}
            onClick={() => generateMutation.mutate()}
            loading={generateMutation.isPending}
          >
            生成对账
          </Button>
          <Button onClick={() => refetch()}>刷新</Button>
        </Space>

        <Table
          columns={columns}
          dataSource={reconciliations}
          loading={isLoading}
          rowKey="id"
          pagination={{ pageSize: 20, total: allocationData?.data?.data?.total_elements }}
        />
      </Card>
    </div>
  )
}

export default SubsidiaryReconciliationPage
