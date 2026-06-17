import { useState } from 'react'
import { Table, Button, Card, Select, Statistic, Row, Col, Tabs, Tag, Progress, Space, message, Popconfirm } from 'antd'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { ApiGet } from '@/api/request'
import { feeAllocationApi, type LevelResponse, type FeeAllocationItem } from '@/api/feeAllocation'

const { Option } = Select

interface BranchAllocItem {
  branchOrgId: number | null
  branchName: string
  phoneCount: number
  totalChargeAmount: number
  platformUsageFee: number
  numberMonthlyRent: number
  domesticCharge: number
  internationalCharge: number
  callAmount: number
  recordingFee: number
  ringtoneFee: number
  flashSmsFee: number
  feeSubtotal: number
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
  totalPlatformUsageFee: number
  totalNumberMonthlyRent: number
  totalDomesticCharge: number
  totalInternationalCharge: number
  totalCallAmount: number
  totalRecordingFee: number
  totalRingtoneFee: number
  totalFlashSmsFee: number
  branches: BranchAllocItem[]
}

// ==================== Allocation Level Tab Component ====================
const AllocationLevelTab = ({ level, billMonth }: { level: number; billMonth: string }) => {
  const queryClient = useQueryClient()
  const [selectedParent, setSelectedParent] = useState<number | undefined>(undefined)

  const levelKey = `alloc-level-${level}`
  const levelName = level === 1 ? '一次分摊' : level === 2 ? '二次分摊' : '三次分摊'
  const levelDesc = level === 1 ? '总行 → 一级分行' : level === 2 ? '一级分行 → 二级分行/部门/综合支行' : '二级分行 → 部门/综合支行/零专支行'

  const apiFn = level === 1 ? feeAllocationApi.getLevel1 : level === 2 ? feeAllocationApi.getLevel2 : feeAllocationApi.getLevel3

  const { data, isLoading } = useQuery({
    queryKey: [levelKey, billMonth, selectedParent],
    queryFn: () => apiFn(billMonth, selectedParent),
  })

  const calcMutation = useMutation({
    mutationFn: () => feeAllocationApi.calculate(billMonth, level),
    onSuccess: () => {
      message.success(`${levelName}计算完成`)
      queryClient.invalidateQueries({ queryKey: [levelKey] })
    },
    onError: () => message.error('计算失败'),
  })

  const confirmMutation = useMutation({
    mutationFn: (id: number) => feeAllocationApi.confirm(id),
    onSuccess: () => {
      message.success('已确认')
      queryClient.invalidateQueries({ queryKey: [levelKey] })
    },
    onError: () => message.error('确认失败'),
  })

  const rejectMutation = useMutation({
    mutationFn: (id: number) => feeAllocationApi.reject(id),
    onSuccess: () => {
      message.success('已驳回')
      queryClient.invalidateQueries({ queryKey: [levelKey] })
    },
    onError: () => message.error('驳回失败'),
  })

  const fmtMoney = (v: number) => `\u00a5${(v || 0).toFixed(2)}`

  const items: FeeAllocationItem[] = data?.items || []

  const columns = [
    { title: '组织名称', dataIndex: 'orgName', key: 'orgName', width: 140, fixed: 'left' as const, render: (v: string) => v || '未归属' },
    { title: '类型', dataIndex: 'orgTypeName', key: 'orgTypeName', width: 80, render: (v: string) => <Tag>{v || '-'}</Tag> },
    ...(level >= 2 ? [{ title: '上级组织', dataIndex: 'parentOrgName', key: 'parentOrgName', width: 120 }] : []),
    { title: '号码数', dataIndex: 'phoneCount', key: 'phoneCount', width: 70, align: 'right' as const },
    { title: '平台使用费', dataIndex: 'platformUsageFee', key: 'platformUsageFee', width: 110, align: 'right' as const, render: (v: number) => fmtMoney(v) },
    { title: '月租费', dataIndex: 'numberMonthlyRent', key: 'numberMonthlyRent', width: 100, align: 'right' as const, render: (v: number) => fmtMoney(v) },
    { title: '国内费用', dataIndex: 'domesticCharge', key: 'domesticCharge', width: 100, align: 'right' as const, render: (v: number) => fmtMoney(v) },
    { title: '国际费用', dataIndex: 'internationalCharge', key: 'internationalCharge', width: 100, align: 'right' as const, render: (v: number) => fmtMoney(v) },
    { title: '录音费', dataIndex: 'recordingFee', key: 'recordingFee', width: 90, align: 'right' as const, render: (v: number) => fmtMoney(v) },
    { title: '彩铃费', dataIndex: 'ringtoneFee', key: 'ringtoneFee', width: 90, align: 'right' as const, render: (v: number) => fmtMoney(v) },
    { title: '闪信费', dataIndex: 'flashSmsFee', key: 'flashSmsFee', width: 90, align: 'right' as const, render: (v: number) => fmtMoney(v) },
    {
      title: '费用合计', dataIndex: 'totalAmount', key: 'totalAmount', width: 120, align: 'right' as const, fixed: 'right' as const,
      render: (v: number) => <span style={{ fontWeight: 700, color: '#1890ff', fontSize: 14 }}>{fmtMoney(v)}</span>,
    },
    { title: '占比', dataIndex: 'percentage', key: 'percentage', width: 90, align: 'right' as const, render: (v: number) => `${(v || 0).toFixed(1)}%` },
    {
      title: '状态', dataIndex: 'statusName', key: 'statusName', width: 80,
      render: (v: string, r: FeeAllocationItem) =>
        r.status === 1 ? <Tag color="green">{v}</Tag> : r.status === 2 ? <Tag color="red">{v}</Tag> : <Tag color="blue">{v}</Tag>,
    },
    {
      title: '操作', key: 'action', width: 140, fixed: 'right' as const,
      render: (_: unknown, r: FeeAllocationItem) => r.status === 0 ? (
        <Space size="small">
          <Popconfirm title="确认该分摊结果？" onConfirm={() => confirmMutation.mutate(r.id)}>
            <Button type="link" size="small">确认</Button>
          </Popconfirm>
          <Popconfirm title="驳回该分摊结果？" onConfirm={() => rejectMutation.mutate(r.id)}>
            <Button type="link" size="small" danger>驳回</Button>
          </Popconfirm>
        </Space>
      ) : '-',
    },
  ]

  // Get unique parent names for dropdown (level 2 & 3)
  const parentOptions = [...new Map(items.map(i => [i.parentOrgId, i.parentOrgName])).entries()]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
        <Space>
          <span style={{ fontWeight: 600 }}>{levelName}：{levelDesc}</span>
          {level >= 2 && (
            <Select
              placeholder="选择上级组织"
              style={{ width: 180 }}
              value={selectedParent}
              onChange={setSelectedParent}
              allowClear
            >
              {parentOptions.map(([id, name]) => (
                <Option key={id} value={Number(id)}>{name}</Option>
              ))}
            </Select>
          )}
        </Space>
        <Space>
          <Button type="primary" loading={calcMutation.isPending} onClick={() => calcMutation.mutate()}>
            计算{levelName}
          </Button>
          <span style={{ color: '#999' }}>
            {data?.calculated ? `${data.totalCount} 条记录` : '未计算'}
          </span>
        </Space>
      </div>

      {/* Summary stats */}
      {data?.calculated && (
        <Row gutter={12} style={{ marginBottom: 12 }}>
          <Col span={4}><Card size="small"><Statistic title="组织数" value={data.totalCount} /></Card></Col>
          <Col span={4}><Card size="small"><Statistic title="号码数" value={data.totalPhones} /></Card></Col>
          <Col span={5}><Card size="small"><Statistic title="费用合计" value={data.totalAmount} precision={2} prefix="\u00a5" valueStyle={{ color: '#cf1322', fontWeight: 700 }} /></Card></Col>
          <Col span={4}><Card size="small"><Statistic title="平台使用费" value={data.totalPlatformUsageFee} precision={2} prefix="\u00a5" /></Card></Col>
          <Col span={4}><Card size="small"><Statistic title="月租费" value={data.totalNumberMonthlyRent} precision={2} prefix="\u00a5" /></Card></Col>
          <Col span={3}><Card size="small"><Statistic title="录音费" value={data.totalRecordingFee} precision={2} prefix="\u00a5" /></Card></Col>
        </Row>
      )}

      <Table
        columns={columns}
        dataSource={items}
        loading={isLoading}
        rowKey="orgId"
        scroll={{ x: 1600 }}
        pagination={{ pageSize: 50, showQuickJumper: true }}
        size="middle"
        bordered
        summary={(pageData) => {
          const arr = pageData as FeeAllocationItem[]
          if (!arr.length) return null
          let tPhones = 0, tPlat = 0, tRent = 0, tDom = 0, tIntl = 0, tRec = 0, tRing = 0, tFlash = 0, tTotal = 0
          arr.forEach(r => {
            tPhones += r.phoneCount || 0; tPlat += r.platformUsageFee || 0; tRent += r.numberMonthlyRent || 0
            tDom += r.domesticCharge || 0; tIntl += r.internationalCharge || 0
            tRec += r.recordingFee || 0; tRing += r.ringtoneFee || 0; tFlash += r.flashSmsFee || 0; tTotal += r.totalAmount || 0
          })
          return (
            <Table.Summary.Row style={{ background: '#fafafa', fontWeight: 700 }}>
              <Table.Summary.Cell index={0}>合计</Table.Summary.Cell>
              <Table.Summary.Cell index={1} />
              {level >= 2 && <Table.Summary.Cell index={2} />}
              <Table.Summary.Cell index={level >= 2 ? 3 : 2} align="right">{tPhones}</Table.Summary.Cell>
              <Table.Summary.Cell index={level >= 2 ? 4 : 3} align="right">{fmtMoney(tPlat)}</Table.Summary.Cell>
              <Table.Summary.Cell index={level >= 2 ? 5 : 4} align="right">{fmtMoney(tRent)}</Table.Summary.Cell>
              <Table.Summary.Cell index={level >= 2 ? 6 : 5} align="right">{fmtMoney(tDom)}</Table.Summary.Cell>
              <Table.Summary.Cell index={level >= 2 ? 7 : 6} align="right">{fmtMoney(tIntl)}</Table.Summary.Cell>
              <Table.Summary.Cell index={level >= 2 ? 8 : 7} align="right">{fmtMoney(tRec)}</Table.Summary.Cell>
              <Table.Summary.Cell index={level >= 2 ? 9 : 8} align="right">{fmtMoney(tRing)}</Table.Summary.Cell>
              <Table.Summary.Cell index={level >= 2 ? 10 : 9} align="right">{fmtMoney(tFlash)}</Table.Summary.Cell>
              <Table.Summary.Cell index={level >= 2 ? 11 : 10} align="right">
                <span style={{ color: '#1890ff', fontWeight: 700 }}>{fmtMoney(tTotal)}</span>
              </Table.Summary.Cell>
              <Table.Summary.Cell index={level >= 2 ? 12 : 11}>100%</Table.Summary.Cell>
              <Table.Summary.Cell index={level >= 2 ? 13 : 12} />
              <Table.Summary.Cell index={level >= 2 ? 14 : 13} />
            </Table.Summary.Row>
          )
        }}
      />
    </div>
  )
}

// ==================== Main Component ====================
const BillAllocationManagement = () => {
  const [activeTab, setActiveTab] = useState('detail')
  const [billMonth, setBillMonth] = useState<string>('2026-05')

  const months = Array.from({ length: 12 }, (_, i) => {
    const d = new Date()
    d.setMonth(d.getMonth() - i)
    return d.toISOString().slice(0, 7)
  })

  // ==================== Branch allocation data (from snapshot) ====================
  const {
    data: branchData,
    isLoading,
    refetch,
  } = useQuery({
    queryKey: ['branch-allocation', billMonth],
    queryFn: () => ApiGet<BranchAllocResponse>('/bill-allocations/branch-allocation', { params: { billMonth } }),
  })

  const fmtMoney = (val: number) => `\u00a5${(val || 0).toFixed(2)}`

  const branchItems: BranchAllocItem[] = branchData?.branches || []

  // ==================== Fee detail columns (full breakdown by branch) ====================
  const feeColumns = [
    { title: '分行', dataIndex: 'branchName', key: 'branchName', width: 110, fixed: 'left' as const, render: (v: string) => v || '未归属' },
    { title: '号码数', dataIndex: 'phoneCount', key: 'phoneCount', width: 70, align: 'right' as const },
    { title: '平台使用费', dataIndex: 'platformUsageFee', key: 'platformUsageFee', width: 110, align: 'right' as const, render: (v: number) => fmtMoney(v) },
    { title: '码号月租费', dataIndex: 'numberMonthlyRent', key: 'numberMonthlyRent', width: 110, align: 'right' as const, render: (v: number) => fmtMoney(v) },
    { title: '国内费用', dataIndex: 'domesticCharge', key: 'domesticCharge', width: 100, align: 'right' as const, render: (v: number) => fmtMoney(v) },
    { title: '国际费用', dataIndex: 'internationalCharge', key: 'internationalCharge', width: 100, align: 'right' as const, render: (v: number) => fmtMoney(v) },
    { title: '通话费', dataIndex: 'callAmount', key: 'callAmount', width: 100, align: 'right' as const, render: (v: number) => fmtMoney(v) },
    {
      title: '费用小计', dataIndex: 'feeSubtotal', key: 'feeSubtotal', width: 120, align: 'right' as const,
      render: (v: number) => <span style={{ fontWeight: 600 }}>{fmtMoney(v)}</span>,
    },
    { title: '录音费', dataIndex: 'recordingFee', key: 'recordingFee', width: 90, align: 'right' as const, render: (v: number) => fmtMoney(v) },
    { title: '彩铃费', dataIndex: 'ringtoneFee', key: 'ringtoneFee', width: 90, align: 'right' as const, render: (v: number) => fmtMoney(v) },
    { title: '闪信费', dataIndex: 'flashSmsFee', key: 'flashSmsFee', width: 90, align: 'right' as const, render: (v: number) => fmtMoney(v) },
    {
      title: '合计', dataIndex: 'totalChargeAmount', key: 'totalChargeAmount', width: 120, align: 'right' as const, fixed: 'right' as const,
      render: (v: number) => <span style={{ fontWeight: 700, color: '#1890ff', fontSize: 14 }}>{fmtMoney(v)}</span>,
    },
  ]

  // Summary row renderer for fee table
  const feeSummaryRow = (pageData: BranchAllocItem[]) => {
    let tPlat = 0, tRent = 0, tDom = 0, tIntl = 0, tCall = 0, tSub = 0, tRec = 0, tRing = 0, tFlash = 0, tTotal = 0, tPhones = 0
    pageData.forEach(r => {
      tPlat += r.platformUsageFee || 0; tRent += r.numberMonthlyRent || 0
      tDom += r.domesticCharge || 0; tIntl += r.internationalCharge || 0
      tCall += r.callAmount || 0; tSub += r.feeSubtotal || 0
      tRec += r.recordingFee || 0; tRing += r.ringtoneFee || 0
      tFlash += r.flashSmsFee || 0; tTotal += r.totalChargeAmount || 0
      tPhones += r.phoneCount || 0
    })
    return (
      <Table.Summary.Row style={{ background: '#fafafa', fontWeight: 700 }}>
        <Table.Summary.Cell index={0}>合计</Table.Summary.Cell>
        <Table.Summary.Cell index={1} align="right">{tPhones}</Table.Summary.Cell>
        <Table.Summary.Cell index={2} align="right">{fmtMoney(tPlat)}</Table.Summary.Cell>
        <Table.Summary.Cell index={3} align="right">{fmtMoney(tRent)}</Table.Summary.Cell>
        <Table.Summary.Cell index={4} align="right">{fmtMoney(tDom)}</Table.Summary.Cell>
        <Table.Summary.Cell index={5} align="right">{fmtMoney(tIntl)}</Table.Summary.Cell>
        <Table.Summary.Cell index={6} align="right">{fmtMoney(tCall)}</Table.Summary.Cell>
        <Table.Summary.Cell index={7} align="right">{fmtMoney(tSub)}</Table.Summary.Cell>
        <Table.Summary.Cell index={8} align="right">{fmtMoney(tRec)}</Table.Summary.Cell>
        <Table.Summary.Cell index={9} align="right">{fmtMoney(tRing)}</Table.Summary.Cell>
        <Table.Summary.Cell index={10} align="right">{fmtMoney(tFlash)}</Table.Summary.Cell>
        <Table.Summary.Cell index={11} align="right">
          <span style={{ color: '#1890ff', fontWeight: 700, fontSize: 14 }}>{fmtMoney(tTotal)}</span>
        </Table.Summary.Cell>
      </Table.Summary.Row>
    )
  }

  return (
    <div>
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={4}>
          <Card><Statistic title="账单月份" value={billMonth} /></Card>
        </Col>
        <Col span={5}>
          <Card><Statistic title="快照月份" value={branchData?.snapshotMonth || '-'} /></Card>
        </Col>
        <Col span={5}>
          <Card><Statistic title="分行数/号码数" value={`${branchData?.totalBranches || 0} / ${branchData?.totalPhones || 0}`} /></Card>
        </Col>
        <Col span={5}>
          <Card>
            <Statistic title="费用小计 / 增值费用"
              value={`${fmtMoney(branchData?.totalPlatformUsageFee || 0)}`}
              valueStyle={{ fontSize: 14 }}
            />
          </Card>
        </Col>
        <Col span={5}>
          <Card><Statistic title="合计金额" value={fmtMoney(branchData?.totalAmount || 0)} valueStyle={{ color: '#cf1322', fontWeight: 700 }} /></Card>
        </Col>
      </Row>

      <Card>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <Select value={billMonth} onChange={setBillMonth} style={{ width: 150 }}>
            {months.map(m => <Option key={m} value={m}>{m}</Option>)}
          </Select>
          <Button onClick={() => refetch()}>刷新</Button>
        </div>

        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          items={[
            {
              key: 'detail',
              label: '费用明细',
              children: (
                <Table
                  columns={feeColumns}
                  dataSource={branchItems}
                  loading={isLoading}
                  rowKey="branchName"
                  scroll={{ x: 1300 }}
                  pagination={{ pageSize: 50, showQuickJumper: true }}
                  size="middle"
                  bordered
                  summary={feeSummaryRow}
                />
              ),
            },
            {
              key: 'alloc1',
              label: '一次分摊',
              children: <AllocationLevelTab level={1} billMonth={billMonth} />,
            },
            {
              key: 'alloc2',
              label: '二次分摊',
              children: <AllocationLevelTab level={2} billMonth={billMonth} />,
            },
            {
              key: 'alloc3',
              label: '三次分摊',
              children: <AllocationLevelTab level={3} billMonth={billMonth} />,
            },
          ]}
        />
      </Card>
    </div>
  )
}

export default BillAllocationManagement
