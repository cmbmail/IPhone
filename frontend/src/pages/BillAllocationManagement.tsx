import { useState } from 'react'
import { Table, Button, Card, Select, Statistic, Row, Col, Tabs, Tag, Progress } from 'antd'
import { useQuery } from '@tanstack/react-query'
import { request } from '@/api/request'
import { ApiGet } from '@/api/request'

const { Option } = Select

interface BillAllocationSummary {
  branchName: string
  platformUsageFee: number
  numberMonthlyRent: number
  outboundDuration: number
  transferOutboundDuration: number
  domesticCharge: number
  internationalCharge: number
  feeSubtotal: number
  recordingFee: number
  ringtoneFee: number
  flashSmsFee: number
  totalAmount: number
}

interface BranchAllocItem {
  branchOrgId: number | null
  branchName: string
  phoneCount: number
  totalChargeAmount: number
  allocatedCount: number
  anomalyCount: number
  unallocatedCount: number
  chargePercentage: number
}

interface BranchAllocResponse {
  billMonth: string
  snapshotMonth: string
  totalBranches: number
  totalPhones: number
  totalAmount: number
  branches: BranchAllocItem[]
}

const BillAllocationManagement = () => {
  const [activeTab, setActiveTab] = useState('summary')
  const [billMonth, setBillMonth] = useState<string>(new Date().toISOString().slice(0, 7))

  const months = Array.from({ length: 12 }, (_, i) => {
    const d = new Date()
    d.setMonth(d.getMonth() - i)
    return d.toISOString().slice(0, 7)
  })

  // ==================== Summary data (by bill_raw branch) ====================
  const {
    data: summaryData,
    isLoading,
    refetch,
  } = useQuery({
    queryKey: ['bill-allocation-summary', billMonth],
    queryFn: async () => {
      const res = await request.get('/bill-allocations/allocation-summary', {
        params: { billMonth },
      })
      return (res.data?.data || []) as BillAllocationSummary[]
    },
  })

  // ==================== Branch allocation data (from snapshot) ====================
  const {
    data: branchData,
    isLoading: branchLoading,
    refetch: refetchBranch,
  } = useQuery({
    queryKey: ['branch-allocation', billMonth],
    queryFn: async () => {
      const res = await ApiGet<BranchAllocResponse>('/bill-allocations/branch-allocation', {
        params: { billMonth },
      })
      return res
    },
  })

  const fmtMoney = (val: number) => `\u00a5${(val || 0).toFixed(2)}`
  const fmtDuration = (val: number) => {
    if (!val) return '-'
    const h = Math.floor(val / 3600)
    const m = Math.floor((val % 3600) / 60)
    const s = val % 60
    return h > 0 ? `${h}h${m}m${s}s` : m > 0 ? `${m}m${s}s` : `${s}s`
  }

  // ==================== Summary tab columns ====================
  const columns = [
    { title: '分行', dataIndex: 'branchName', key: 'branchName', width: 120, fixed: 'left' as const, render: (v: string) => v || '未分配' },
    { title: '平台使用费', dataIndex: 'platformUsageFee', key: 'platformUsageFee', width: 120, align: 'right' as const, render: (v: number) => fmtMoney(v) },
    { title: '码号月租费', dataIndex: 'numberMonthlyRent', key: 'numberMonthlyRent', width: 120, align: 'right' as const, render: (v: number) => fmtMoney(v) },
    { title: '国内外呼时长', dataIndex: 'outboundDuration', key: 'outboundDuration', width: 130, align: 'right' as const, render: (v: number) => fmtDuration(v) },
    { title: '转接呼叫时长', dataIndex: 'transferOutboundDuration', key: 'transferOutboundDuration', width: 130, align: 'right' as const, render: (v: number) => fmtDuration(v) },
    { title: '国内费用', dataIndex: 'domesticCharge', key: 'domesticCharge', width: 110, align: 'right' as const, render: (v: number) => fmtMoney(v) },
    { title: '国际费用', dataIndex: 'internationalCharge', key: 'internationalCharge', width: 110, align: 'right' as const, render: (v: number) => fmtMoney(v) },
    { title: '费用小计', dataIndex: 'feeSubtotal', key: 'feeSubtotal', width: 120, align: 'right' as const, render: (v: number) => <span style={{ fontWeight: 600 }}>{fmtMoney(v)}</span> },
    { title: '录音费用', dataIndex: 'recordingFee', key: 'recordingFee', width: 110, align: 'right' as const, render: (v: number) => fmtMoney(v) },
    { title: '彩铃费用', dataIndex: 'ringtoneFee', key: 'ringtoneFee', width: 110, align: 'right' as const, render: (v: number) => fmtMoney(v) },
    { title: '闪信费用', dataIndex: 'flashSmsFee', key: 'flashSmsFee', width: 110, align: 'right' as const, render: (v: number) => fmtMoney(v) },
    { title: '合计', dataIndex: 'totalAmount', key: 'totalAmount', width: 130, align: 'right' as const, fixed: 'right' as const, render: (v: number) => <span style={{ fontWeight: 700, color: '#1890ff', fontSize: 14 }}>{fmtMoney(v)}</span> },
  ]

  const data: BillAllocationSummary[] = summaryData || []
  const totalAmount = data.reduce((sum, r) => sum + (r.totalAmount || 0), 0)
  const totalFeeSubtotal = data.reduce((sum, r) => sum + (r.feeSubtotal || 0), 0)
  const totalRecording = data.reduce((sum, r) => sum + (r.recordingFee || 0), 0)
  const totalRingtone = data.reduce((sum, r) => sum + (r.ringtoneFee || 0), 0)
  const totalFlash = data.reduce((sum, r) => sum + (r.flashSmsFee || 0), 0)

  // ==================== Branch tab columns ====================
  const branchColumns = [
    {
      title: '分行',
      dataIndex: 'branchName',
      key: 'branchName',
      width: 130,
      fixed: 'left' as const,
      render: (v: string) => v || '未归属',
    },
    {
      title: '号码数',
      dataIndex: 'phoneCount',
      key: 'phoneCount',
      width: 80,
      align: 'right' as const,
    },
    {
      title: '已分摊',
      dataIndex: 'allocatedCount',
      key: 'allocatedCount',
      width: 80,
      align: 'right' as const,
      render: (v: number) => <span style={{ color: '#52c41a' }}>{v}</span>,
    },
    {
      title: '异常',
      dataIndex: 'anomalyCount',
      key: 'anomalyCount',
      width: 70,
      align: 'right' as const,
      render: (v: number) => v > 0 ? <Tag color="red">{v}</Tag> : <span>{v}</span>,
    },
    {
      title: '未分摊',
      dataIndex: 'unallocatedCount',
      key: 'unallocatedCount',
      width: 80,
      align: 'right' as const,
      render: (v: number) => v > 0 ? <Tag color="orange">{v}</Tag> : <span>{v}</span>,
    },
    {
      title: '费用合计',
      dataIndex: 'totalChargeAmount',
      key: 'totalChargeAmount',
      width: 130,
      align: 'right' as const,
      render: (v: number) => <span style={{ fontWeight: 700, color: '#1890ff', fontSize: 14 }}>{fmtMoney(v)}</span>,
    },
    {
      title: '占比',
      dataIndex: 'chargePercentage',
      key: 'chargePercentage',
      width: 160,
      render: (v: number) => <Progress percent={v || 0} size="small" format={(p) => `${p?.toFixed(1)}%`} />,
    },
  ]

  const branchItems: BranchAllocItem[] = branchData?.branches || []

  return (
    <div>
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <Card><Statistic title="账单月份" value={billMonth} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="费用小计" value={fmtMoney(totalFeeSubtotal)} valueStyle={{ color: '#1890ff' }} /></Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="增值费用" value={`录音\u00a5${totalRecording.toFixed(0)} 彩铃\u00a5${totalRingtone.toFixed(0)} 闪信\u00a5${totalFlash.toFixed(0)}`} valueStyle={{ fontSize: 16 }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="合计金额" value={fmtMoney(totalAmount)} valueStyle={{ color: '#cf1322', fontWeight: 700 }} /></Card>
        </Col>
      </Row>

      <Card>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <Select value={billMonth} onChange={setBillMonth} style={{ width: 150 }}>
            {months.map((m) => (<Option key={m} value={m}>{m}</Option>))}
          </Select>
          <Button onClick={() => { refetch(); refetchBranch() }}>刷新</Button>
        </div>

        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          items={[
            {
              key: 'summary',
              label: '费用明细',
              children: (
                <Table
                  columns={columns}
                  dataSource={data}
                  loading={isLoading}
                  rowKey="branchName"
                  scroll={{ x: 1500 }}
                  pagination={false}
                  size="middle"
                  bordered
                  summary={(pageData) => {
                    let tPlat = 0, tRent = 0, tDom = 0, tIntl = 0, tSub = 0, tRec = 0, tRing = 0, tFlash = 0, tTotal = 0
                    pageData.forEach((r) => {
                      tPlat += r.platformUsageFee || 0; tRent += r.numberMonthlyRent || 0
                      tDom += r.domesticCharge || 0; tIntl += r.internationalCharge || 0
                      tSub += r.feeSubtotal || 0; tRec += r.recordingFee || 0
                      tRing += r.ringtoneFee || 0; tFlash += r.flashSmsFee || 0; tTotal += r.totalAmount || 0
                    })
                    return (
                      <Table.Summary.Row style={{ background: '#fafafa', fontWeight: 700 }}>
                        <Table.Summary.Cell index={0}>合计</Table.Summary.Cell>
                        <Table.Summary.Cell index={1} align="right">{fmtMoney(tPlat)}</Table.Summary.Cell>
                        <Table.Summary.Cell index={2} align="right">{fmtMoney(tRent)}</Table.Summary.Cell>
                        <Table.Summary.Cell index={3} align="right">-</Table.Summary.Cell>
                        <Table.Summary.Cell index={4} align="right">-</Table.Summary.Cell>
                        <Table.Summary.Cell index={5} align="right">{fmtMoney(tDom)}</Table.Summary.Cell>
                        <Table.Summary.Cell index={6} align="right">{fmtMoney(tIntl)}</Table.Summary.Cell>
                        <Table.Summary.Cell index={7} align="right">{fmtMoney(tSub)}</Table.Summary.Cell>
                        <Table.Summary.Cell index={8} align="right">{fmtMoney(tRec)}</Table.Summary.Cell>
                        <Table.Summary.Cell index={9} align="right">{fmtMoney(tRing)}</Table.Summary.Cell>
                        <Table.Summary.Cell index={10} align="right">{fmtMoney(tFlash)}</Table.Summary.Cell>
                        <Table.Summary.Cell index={11} align="right"><span style={{ color: '#1890ff', fontWeight: 700, fontSize: 14 }}>{fmtMoney(tTotal)}</span></Table.Summary.Cell>
                      </Table.Summary.Row>
                    )
                  }}
                />
              ),
            },
            {
              key: 'branch',
              label: `分行分摊（快照）`,
              children: (
                <>
                  {branchData && (
                    <Row gutter={16} style={{ marginBottom: 16 }}>
                      <Col span={6}>
                        <Card size="small"><Statistic title="快照月份" value={branchData.snapshotMonth} /></Card>
                      </Col>
                      <Col span={6}>
                        <Card size="small"><Statistic title="分行数" value={branchData.totalBranches} /></Card>
                      </Col>
                      <Col span={6}>
                        <Card size="small"><Statistic title="号码总数" value={branchData.totalPhones} /></Card>
                      </Col>
                      <Col span={6}>
                        <Card size="small"><Statistic title="费用总计" value={fmtMoney(branchData.totalAmount)} valueStyle={{ color: '#cf1322', fontWeight: 700 }} /></Card>
                      </Col>
                    </Row>
                  )}
                  <Table
                    columns={branchColumns}
                    dataSource={branchItems}
                    loading={branchLoading}
                    rowKey="branchName"
                    scroll={{ x: 800 }}
                    pagination={{ pageSize: 50, showQuickJumper: true }}
                    size="middle"
                    bordered
                    summary={(pageData) => {
                      let tPhones = 0, tAlloc = 0, tAnomaly = 0, tUnalloc = 0, tCharge = 0
                      pageData.forEach((r) => {
                        tPhones += r.phoneCount; tAlloc += r.allocatedCount
                        tAnomaly += r.anomalyCount; tUnalloc += r.unallocatedCount
                        tCharge += r.totalChargeAmount || 0
                      })
                      return (
                        <Table.Summary.Row style={{ background: '#fafafa', fontWeight: 700 }}>
                          <Table.Summary.Cell index={0}>合计</Table.Summary.Cell>
                          <Table.Summary.Cell index={1} align="right">{tPhones}</Table.Summary.Cell>
                          <Table.Summary.Cell index={2} align="right"><span style={{ color: '#52c41a' }}>{tAlloc}</span></Table.Summary.Cell>
                          <Table.Summary.Cell index={3} align="right">{tAnomaly > 0 ? <Tag color="red">{tAnomaly}</Tag> : tAnomaly}</Table.Summary.Cell>
                          <Table.Summary.Cell index={4} align="right">{tUnalloc > 0 ? <Tag color="orange">{tUnalloc}</Tag> : tUnalloc}</Table.Summary.Cell>
                          <Table.Summary.Cell index={5} align="right"><span style={{ color: '#1890ff', fontWeight: 700, fontSize: 14 }}>{fmtMoney(tCharge)}</span></Table.Summary.Cell>
                          <Table.Summary.Cell index={6}>100%</Table.Summary.Cell>
                        </Table.Summary.Row>
                      )
                    }}
                  />
                </>
              ),
            },
          ]}
        />
      </Card>
    </div>
  )
}

export default BillAllocationManagement
