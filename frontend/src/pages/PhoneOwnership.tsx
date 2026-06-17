import { useState } from 'react'
import {
  Table,
  Button,
  Card,
  Select,
  Space,
  Input,
  Modal,
  Tag,
  Tabs,
  message,
  Upload,
  Row,
  Col,
  Statistic,
  Popconfirm,
} from 'antd'
import { UploadOutlined, DownloadOutlined, EditOutlined, CameraOutlined, ReloadOutlined, LinkOutlined } from '@ant-design/icons'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  phoneOwnershipApi,
  type PhoneOwnership,
  type ImportCompareItem,
} from '@/api/phoneOwnership'
import { ApiGet } from '@/api/request'
import type { OrgStructure } from '@/types/org'
import { snapshotApi } from '@/api/snapshot'
import { ownershipLevelApi, type OwnershipLevelItem, type LevelSummaryResponse } from '@/api/ownershipLevel'
import type { PhoneSnapshot } from '@/types/snapshot'
import {
  SNAPSHOT_STATUS_LABELS,
  SNAPSHOT_STATUS_COLORS,
  ALLOC_STATUS_LABELS,
} from '@/types/snapshot'

// ==================== Level Table Component ====================
const LevelTable = ({ data, loading, summary, onSelect }: {
  data: OwnershipLevelItem[]
  loading: boolean
  summary?: LevelSummaryResponse | null
  onSelect?: (orgId: number) => void
}) => {
  const columns = [
    {
      title: '组织名称', dataIndex: 'orgName', key: 'orgName', width: 160, fixed: 'left' as const,
      render: (v: string, r: OwnershipLevelItem) => onSelect ? (
        <a onClick={() => onSelect(r.orgId)} style={{ fontWeight: 500 }}>{v}</a>
      ) : <span style={{ fontWeight: 500 }}>{v}</span>,
    },
    { title: '类型', dataIndex: 'orgTypeName', key: 'orgTypeName', width: 90, render: (v: string) => <Tag>{v || '-'}</Tag> },
    ...(summary && summary.level >= 2 ? [{ title: '上级组织', dataIndex: 'parentOrgName', key: 'parentOrgName', width: 130 }] : []),
    { title: '号码数', dataIndex: 'phoneCount', key: 'phoneCount', width: 90, align: 'right' as const,
      render: (v: number) => <span style={{ fontWeight: 600, color: '#1890ff' }}>{v}</span>,
    },
    { title: '已归属', dataIndex: 'allocatedCount', key: 'allocatedCount', width: 90, align: 'right' as const,
      render: (v: number) => <span style={{ color: '#52c41a' }}>{v}</span>,
    },
  ]

  return (
    <Table
      columns={columns}
      dataSource={data}
      loading={loading}
      rowKey="orgId"
      scroll={{ x: 500 }}
      pagination={{ pageSize: 50, showQuickJumper: true }}
      size="middle"
      bordered
      summary={(pageData) => {
        const arr = pageData as OwnershipLevelItem[]
        if (!arr.length) return null
        let tPhones = 0, tAlloc = 0
        arr.forEach(r => { tPhones += r.phoneCount; tAlloc += r.allocatedCount })
        return (
          <Table.Summary.Row style={{ background: '#fafafa', fontWeight: 700 }}>
            <Table.Summary.Cell index={0}>合计</Table.Summary.Cell>
            <Table.Summary.Cell index={1} />
            {summary && summary.level >= 2 && <Table.Summary.Cell index={2} />}
            <Table.Summary.Cell index={summary && summary.level >= 2 ? 3 : 2} align="right">
              <span style={{ color: '#1890ff', fontWeight: 700 }}>{tPhones}</span>
            </Table.Summary.Cell>
            <Table.Summary.Cell index={summary && summary.level >= 2 ? 4 : 3} align="right">
              <span style={{ color: '#52c41a' }}>{tAlloc}</span>
            </Table.Summary.Cell>
          </Table.Summary.Row>
        )
      }}
    />
  )
}

const PhoneOwnershipPage = () => {
  const [activeTab, setActiveTab] = useState('ownership')
  const [keyword, setKeyword] = useState('')
  const [branchOrgId, setBranchOrgId] = useState<number | undefined>(undefined)
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(20)

  const [editModalOpen, setEditModalOpen] = useState(false)
  const [editRecord, setEditRecord] = useState<PhoneOwnership | null>(null)
  const [editBranch, setEditBranch] = useState<number | undefined>(undefined)
  const [editDept, setEditDept] = useState<number | undefined>(undefined)
  const [editRemark, setEditRemark] = useState('')

  const [compareModalOpen, setCompareModalOpen] = useState(false)
  const [compareData, setCompareData] = useState<ImportCompareItem[]>([])

  // Snapshot state
  const [snapMonth, setSnapMonth] = useState<string>('')
  const [snapPage, setSnapPage] = useState(0)
  const [snapPageSize, setSnapPageSize] = useState(20)
  const [snapStatus, setSnapStatus] = useState<number | undefined>(undefined)
  const [linkModalOpen, setLinkModalOpen] = useState(false)
  const [linkBillMonth, setLinkBillMonth] = useState('')

  // Ownership level state
  const [ownershipSubTab, setOwnershipSubTab] = useState('level1')
  const [levelParentId, setLevelParentId] = useState<number | undefined>(undefined)
  const [level2ParentId, setLevel2ParentId] = useState<number | undefined>(undefined)
  const [level3ParentId, setLevel3ParentId] = useState<number | undefined>(undefined)

  const queryClient = useQueryClient()

  // ==================== Ownership data ====================
  const { data: listData, isLoading } = useQuery({
    queryKey: ['phone-ownership', keyword, branchOrgId, page, pageSize],
    queryFn: async () => {
      const res = await phoneOwnershipApi.search({
        keyword: keyword || undefined,
        branchOrgId,
        page,
        size: pageSize,
      })
      return res
    },
  })

  const { data: orgsData } = useQuery({
    queryKey: ['orgs'],
    queryFn: async () => {
      const res = await ApiGet<OrgStructure[]>('/orgs')
      return res || []
    },
  })

  const orgs: OrgStructure[] = orgsData || []
  const branches = orgs.filter((o: OrgStructure) => o.level === 1)

  // ==================== Snapshot data ====================
  const { data: snapMonths } = useQuery({
    queryKey: ['snapshot-months'],
    queryFn: () => snapshotApi.getMonths(),
  })

  const { data: snapData, isLoading: snapLoading } = useQuery({
    queryKey: ['snapshots', snapMonth, snapPage, snapPageSize, snapStatus],
    queryFn: () =>
      snapshotApi.getSnapshots({
        month: snapMonth,
        page: snapPage,
        size: snapPageSize,
        status: snapStatus,
      }),
    enabled: !!snapMonth,
  })

  // Ownership level queries
  const { data: level1Data, isLoading: level1Loading } = useQuery({
    queryKey: ['ownership-level', 1],
    queryFn: () => ownershipLevelApi.getByLevel(1),
  })
  const { data: level2Data, isLoading: level2Loading } = useQuery({
    queryKey: ['ownership-level', 2, level2ParentId],
    queryFn: () => ownershipLevelApi.getByLevel(2, level2ParentId),
    enabled: ownershipSubTab === 'level2',
  })
  const { data: level3Data, isLoading: level3Loading } = useQuery({
    queryKey: ['ownership-level', 3, level3ParentId],
    queryFn: () => ownershipLevelApi.getByLevel(3, level3ParentId),
    enabled: ownershipSubTab === 'level3',
  })

  const { data: snapStats } = useQuery({
    queryKey: ['snapshot-stats', snapMonth],
    queryFn: () => snapshotApi.getStats(snapMonth),
    enabled: !!snapMonth,
  })

  // ==================== Mutations ====================
  const updateMutation = useMutation({
    mutationFn: async () => {
      if (!editRecord) return
      return phoneOwnershipApi.update(editRecord.id, {
        branchOrgId: editBranch,
        deptOrgId: editDept,
        remark: editRemark,
      })
    },
    onSuccess: () => {
      message.success('更新成功')
      setEditModalOpen(false)
      queryClient.invalidateQueries({ queryKey: ['phone-ownership'] })
    },
    onError: () => message.error('更新失败'),
  })

  const confirmImportMutation = useMutation({
    mutationFn: (items: ImportCompareItem[]) => phoneOwnershipApi.importConfirm(items),
    onSuccess: (count: number) => {
      message.success(`导入成功，共更新 ${count} 条记录`)
      setCompareModalOpen(false)
      queryClient.invalidateQueries({ queryKey: ['phone-ownership'] })
    },
    onError: () => message.error('导入确认失败'),
  })

  const triggerMutation = useMutation({
    mutationFn: (month: string) => snapshotApi.trigger(month),
    onSuccess: () => {
      message.success('快照生成已触发')
      queryClient.invalidateQueries({ queryKey: ['snapshot-months'] })
    },
    onError: () => message.error('快照生成失败'),
  })

  const regenerateMutation = useMutation({
    mutationFn: (month: string) => snapshotApi.regenerate(month),
    onSuccess: () => {
      message.success('快照已重新生成')
      queryClient.invalidateQueries({ queryKey: ['snapshots'] })
    },
    onError: () => message.error('快照重新生成失败'),
  })

  const linkMutation = useMutation({
    mutationFn: () => snapshotApi.linkToBill(snapMonth, linkBillMonth),
    onSuccess: (data) => {
      message.success(`已关联 ${data.linkedCount} 条快照到账单月份 ${linkBillMonth}`)
      setLinkModalOpen(false)
      queryClient.invalidateQueries({ queryKey: ['snapshots'] })
    },
    onError: () => message.error('关联失败'),
  })

  // ==================== Handlers ====================
  const handleImport = async (file: File) => {
    try {
      const res = await phoneOwnershipApi.importCompare(file)
      const items: ImportCompareItem[] = (res as unknown as ImportCompareItem[]) || []
      if (items.length === 0) {
        message.warning('文件中无有效数据')
        return
      }
      setCompareData(items)
      setCompareModalOpen(true)
    } catch {
      message.error('导入比对失败，请检查文件格式')
    }
    return false
  }

  const handleExport = () => {
    // Read token from Zustand persist storage
    let token: string | null = null
    try {
      const raw = localStorage.getItem('auth-storage')
      if (raw) { token = JSON.parse(raw)?.state?.token || null }
    } catch { /* ignore */ }
    if (!token) { message.error('未登录或登录已过期'); return }
    const url = '/api/phone-ownership/export?token=' + encodeURIComponent(token)
    window.open(url, '_blank')
  }

  const openEdit = (record: PhoneOwnership) => {
    setEditRecord(record)
    setEditBranch(record.branchOrgId || undefined)
    setEditDept(record.deptOrgId || undefined)
    setEditRemark(record.remark || '')
    setEditModalOpen(true)
  }

  const depts = editBranch ? orgs.filter((o: OrgStructure) => o.parentId === editBranch) : []

  // ==================== Ownership columns ====================
  const columns = [
    { title: '电话号码', dataIndex: 'phoneNumber', key: 'phoneNumber', width: 150 },
    {
      title: '分行',
      dataIndex: 'branchName',
      key: 'branchName',
      width: 150,
      render: (v: string) => v || '-',
    },
    {
      title: '部门',
      dataIndex: 'deptName',
      key: 'deptName',
      width: 150,
      render: (v: string) => v || '-',
    },
    {
      title: '备注',
      dataIndex: 'remark',
      key: 'remark',
      width: 200,
      render: (v: string) => v || '-',
    },
    {
      title: '操作',
      key: 'actions',
      width: 100,
      render: (_: unknown, record: PhoneOwnership) => (
        <Button size="small" type="link" icon={<EditOutlined />} onClick={() => openEdit(record)}>
          编辑
        </Button>
      ),
    },
  ]

  const totalCount = listData?.totalElements || 0
  const content: PhoneOwnership[] = listData?.content || []

  const newCount = compareData.filter((i) => i.isNew).length
  const diffCount = compareData.filter((i) => i.hasDiff).length
  const sameCount = compareData.filter((i) => !i.isNew && !i.hasDiff).length

  const compareColumns = [
    { title: '电话号码', dataIndex: 'phoneNumber', key: 'phoneNumber', width: 130 },
    {
      title: '分行(导入→现有)',
      key: 'branch',
      width: 200,
      render: (_: unknown, r: ImportCompareItem) => (
        <span>
          <span style={{ fontWeight: r.hasDiff ? 600 : 400 }}>{r.branchName || '-'}</span>
          {r.hasDiff && r.existingBranchName !== r.branchName && (
            <>
              <span style={{ color: '#999', margin: '0 4px' }}>←</span>
              <span style={{ color: '#999', textDecoration: 'line-through' }}>
                {r.existingBranchName || '-'}
              </span>
            </>
          )}
        </span>
      ),
    },
    {
      title: '部门(导入→现有)',
      key: 'dept',
      width: 200,
      render: (_: unknown, r: ImportCompareItem) => (
        <span>
          <span style={{ fontWeight: r.hasDiff ? 600 : 400 }}>{r.deptName || '-'}</span>
          {r.hasDiff && r.existingDeptName !== r.deptName && (
            <>
              <span style={{ color: '#999', margin: '0 4px' }}>←</span>
              <span style={{ color: '#999', textDecoration: 'line-through' }}>
                {r.existingDeptName || '-'}
              </span>
            </>
          )}
        </span>
      ),
    },
    {
      title: '备注(导入→现有)',
      key: 'remark',
      width: 200,
      render: (_: unknown, r: ImportCompareItem) => (
        <span>
          <span style={{ fontWeight: r.hasDiff ? 600 : 400 }}>{r.remark || '-'}</span>
          {r.hasDiff && r.existingRemark !== r.remark && (
            <>
              <span style={{ color: '#999', margin: '0 4px' }}>←</span>
              <span style={{ color: '#999', textDecoration: 'line-through' }}>
                {r.existingRemark || '-'}
              </span>
            </>
          )}
        </span>
      ),
    },
    {
      title: '状态',
      key: 'status',
      width: 90,
      render: (_: unknown, r: ImportCompareItem) => {
        if (r.isNew) return <Tag color="green">新增</Tag>
        if (r.hasDiff) return <Tag color="orange">变更</Tag>
        return <Tag>不变</Tag>
      },
    },
  ]

  // ==================== Snapshot columns ====================
  const snapColumns = [
    { title: '号码', dataIndex: 'phoneNumber', key: 'phoneNumber', width: 140, fixed: 'left' as const },
    { title: '分机', dataIndex: 'extensionNumber', key: 'extensionNumber', width: 100, render: (v: string | null) => v || '-' },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (s: number) => <Tag color={SNAPSHOT_STATUS_COLORS[s] || 'default'}>{SNAPSHOT_STATUS_LABELS[s] || s}</Tag>,
    },
    { title: '组织', dataIndex: 'orgName', key: 'orgName', width: 150, render: (v: string | null) => v || '-' },
    { title: '分行', dataIndex: 'branchName', key: 'branchName', width: 120, render: (v: string | null) => v || '-' },
    { title: '成本中心', dataIndex: 'costCenterCode', key: 'costCenterCode', width: 100, render: (v: string | null) => v || '-' },
    { title: '工号', dataIndex: 'employeeNo', key: 'employeeNo', width: 90, render: (v: string | null) => v || '-' },
    { title: '姓名', dataIndex: 'employeeName', key: 'employeeName', width: 100, render: (v: string | null) => v || '-' },
    {
      title: '分摊',
      dataIndex: 'allocationStatus',
      key: 'allocationStatus',
      width: 90,
      render: (s: number) => {
        const colors: Record<number, string> = { 0: 'default', 1: 'green', 2: 'red' }
        return <Tag color={colors[s] || 'default'}>{ALLOC_STATUS_LABELS[s] || s}</Tag>
      },
    },
  ]

  const snapContent: PhoneSnapshot[] = (snapData as any)?.content || []
  const snapTotal: number = (snapData as any)?.totalElements || 0

  // Stats
  const byStatus = (snapStats as any)?.byStatus || {}
  const byAlloc = (snapStats as any)?.byAllocationStatus || {}

  // ==================== Render ====================
  return (
    <div>
      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        items={[
          {
            key: 'ownership',
            label: '号码归属',
            children: (
              <>
                <Row gutter={16} style={{ marginBottom: 16 }}>
                  <Col span={6}>
                    <Card><Statistic title="号码总数" value={totalCount} /></Card>
                  </Col>
                  <Col span={6}>
                    <Card>
                      <Statistic title="已归属分行" value={content.filter((c) => c.branchName).length} valueStyle={{ color: '#52c41a' }} />
                    </Card>
                  </Col>
                  <Col span={6}>
                    <Card>
                      <Statistic title="已归属部门" value={content.filter((c) => c.deptName).length} valueStyle={{ color: '#1890ff' }} />
                    </Card>
                  </Col>
                  <Col span={6}>
                    <Card>
                      <Statistic title="一级分行" value={level1Data?.totalOrgs || 0} suffix="个" />
                    </Card>
                  </Col>
                </Row>
                <Card>
                  <Tabs
                    activeKey={ownershipSubTab}
                    onChange={(k) => setOwnershipSubTab(k)}
                    items={[
                      {
                        key: 'level1',
                        label: '一级分行',
                        children: (
                          <LevelTable
                            data={level1Data?.items || []}
                            loading={level1Loading}
                            summary={level1Data}
                            onSelect={(orgId) => { setLevel2ParentId(orgId); setOwnershipSubTab('level2') }}
                          />
                        ),
                      },
                      {
                        key: 'level2',
                        label: '二级分行/一级部门/综合支行',
                        children: (
                          <div>
                            {level2ParentId && (
                              <div style={{ marginBottom: 12 }}>
                                <Select
                                  placeholder="选择一级分行"
                                  value={level2ParentId}
                                  onChange={(v) => setLevel2ParentId(v)}
                                  style={{ width: 200 }}
                                >
                                  {(level1Data?.items || []).map((i: OwnershipLevelItem) => (
                                    <Select.Option key={i.orgId} value={i.orgId}>{i.orgName}</Select.Option>
                                  ))}
                                </Select>
                              </div>
                            )}
                            <LevelTable
                              data={level2Data?.items || []}
                              loading={level2Loading}
                              summary={level2Data}
                              onSelect={(orgId) => { setLevel3ParentId(orgId); setOwnershipSubTab('level3') }}
                            />
                          </div>
                        ),
                      },
                      {
                        key: 'level3',
                        label: '部门/支行/零专支行',
                        children: (
                          <div>
                            {level2Data?.items && (
                              <div style={{ marginBottom: 12 }}>
                                <Select
                                  placeholder="选择上级组织"
                                  value={level3ParentId}
                                  onChange={(v) => setLevel3ParentId(v)}
                                  style={{ width: 200 }}
                                  allowClear
                                >
                                  {(level2Data.items || []).map((i: OwnershipLevelItem) => (
                                    <Select.Option key={i.orgId} value={i.orgId}>{i.orgName}({i.orgTypeName})</Select.Option>
                                  ))}
                                </Select>
                              </div>
                            )}
                            <LevelTable
                              data={level3Data?.items || []}
                              loading={level3Loading}
                              summary={level3Data}
                            />
                          </div>
                        ),
                      },
                      {
                        key: 'all',
                        label: '全部号码',
                        children: (
                          <div style={{ marginTop: 12 }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
                              <Space>
                                <Input.Search placeholder="搜索号码/分行/部门" value={keyword} onChange={(e) => setKeyword(e.target.value)} style={{ width: 220 }} onSearch={() => setPage(0)} />
                                <Select placeholder="筛选分行" value={branchOrgId} onChange={(v) => { setBranchOrgId(v); setPage(0) }} style={{ width: 150 }} allowClear>
                                  {branches.map((b: OrgStructure) => (<Select.Option key={b.id} value={b.id}>{b.branchName || b.name}</Select.Option>))}
                                </Select>
                              </Space>
                              <Space>
                                <Upload accept=".csv,.txt" showUploadList={false} beforeUpload={(file) => { handleImport(file); return false }}>
                                  <Button icon={<UploadOutlined />}>导入</Button>
                                </Upload>
                                <Button icon={<DownloadOutlined />} onClick={handleExport}>导出</Button>
                              </Space>
                            </div>
                            <Table columns={columns} dataSource={content} loading={isLoading} rowKey="id" pagination={{
                              current: page + 1, pageSize, total: listData?.totalElements || 0,
                              onChange: (p, ps) => { setPage(p - 1); setPageSize(ps) },
                              showSizeChanger: true, showQuickJumper: true,
                            }} />
                          </div>
                        ),
                      },
                    ]}
                  />
                </Card>
              </>
            ),
          },
          {
            key: 'snapshot',
            label: '月度快照',
            children: (
              <>
                <Row gutter={16} style={{ marginBottom: 16 }}>
                  <Col span={6}>
                    <Card><Statistic title="快照总数" value={snapStats?.total || 0} /></Card>
                  </Col>
                  <Col span={6}>
                    <Card><Statistic title="在用" value={byStatus[1] || 0} valueStyle={{ color: '#52c41a' }} /></Card>
                  </Col>
                  <Col span={6}>
                    <Card><Statistic title="停机" value={byStatus[2] || 0} valueStyle={{ color: '#fa8c16' }} /></Card>
                  </Col>
                  <Col span={6}>
                    <Card><Statistic title="已分摊" value={byAlloc[1] || 0} valueStyle={{ color: '#1890ff' }} /></Card>
                  </Col>
                </Row>
                <Card>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
                    <Space>
                      <Select placeholder="选择月份" value={snapMonth || undefined} onChange={(v) => { setSnapMonth(v); setSnapPage(0) }} style={{ width: 130 }}>
                        {(snapMonths || []).map((m: string) => (<Select.Option key={m} value={m}>{m}</Select.Option>))}
                      </Select>
                      <Select placeholder="号码状态" value={snapStatus} onChange={(v) => { setSnapStatus(v); setSnapPage(0) }} style={{ width: 120 }} allowClear>
                        <Select.Option value={1}>在用</Select.Option>
                        <Select.Option value={2}>停机</Select.Option>
                        <Select.Option value={3}>注销</Select.Option>
                      </Select>
                    </Space>
                    <Space>
                      <Button icon={<LinkOutlined />} onClick={() => setLinkModalOpen(true)} disabled={!snapMonth}>关联账单</Button>
                      <Popconfirm title={`确认重新生成 ${snapMonth} 快照？`} onConfirm={() => regenerateMutation.mutate(snapMonth)} okText="确认" cancelText="取消">
                        <Button icon={<ReloadOutlined />} disabled={!snapMonth} loading={regenerateMutation.isPending}>重新生成</Button>
                      </Popconfirm>
                    </Space>
                  </div>
                  <Table
                    columns={snapColumns}
                    dataSource={snapContent}
                    loading={snapLoading}
                    rowKey="id"
                    scroll={{ x: 1000 }}
                    pagination={{
                      current: snapPage + 1, pageSize: snapPageSize, total: snapTotal,
                      onChange: (p, ps) => { setSnapPage(p - 1); setSnapPageSize(ps) },
                      showSizeChanger: true, showQuickJumper: true,
                    }}
                  />
                </Card>
              </>
            ),
          },
        ]}
      />

      {/* Edit Modal */}
      <Modal title={`编辑号码归属 - ${editRecord?.phoneNumber || ''}`} open={editModalOpen} onCancel={() => setEditModalOpen(false)} onOk={() => updateMutation.mutate()} confirmLoading={updateMutation.isPending} width={500}>
        <div style={{ marginBottom: 16 }}>
          <div style={{ marginBottom: 8, fontWeight: 500 }}>归属分行</div>
          <Select style={{ width: '100%' }} placeholder="选择分行" value={editBranch} onChange={(v) => { setEditBranch(v); setEditDept(undefined) }} allowClear>
            {branches.map((b: OrgStructure) => (<Select.Option key={b.id} value={b.id}>{b.branchName || b.name}</Select.Option>))}
          </Select>
        </div>
        <div style={{ marginBottom: 16 }}>
          <div style={{ marginBottom: 8, fontWeight: 500 }}>归属部门</div>
          <Select style={{ width: '100%' }} placeholder="选择部门" value={editDept} onChange={setEditDept} allowClear>
            {depts.map((d: OrgStructure) => (<Select.Option key={d.id} value={d.id}>{d.name}</Select.Option>))}
          </Select>
        </div>
        <div>
          <div style={{ marginBottom: 8, fontWeight: 500 }}>备注</div>
          <Input.TextArea rows={3} value={editRemark} onChange={(e) => setEditRemark(e.target.value)} placeholder="输入备注" />
        </div>
      </Modal>

      {/* Compare Modal */}
      <Modal title={`导入比对结果（新增 ${newCount} / 变更 ${diffCount} / 不变 ${sameCount}）`} open={compareModalOpen} onCancel={() => setCompareModalOpen(false)} width={900}
        footer={[
          <Button key="cancel" onClick={() => setCompareModalOpen(false)}>取消</Button>,
          <Button key="confirm" type="primary" onClick={() => confirmImportMutation.mutate(compareData)} loading={confirmImportMutation.isPending}>确认导入</Button>,
        ]}
      >
        <Table columns={compareColumns} dataSource={compareData} rowKey="phoneNumber" size="small" pagination={{ pageSize: 10, showQuickJumper: true }} scroll={{ y: 400 }} />
      </Modal>

      {/* Link Bill Modal */}
      <Modal title="关联快照到账单月份" open={linkModalOpen} onCancel={() => setLinkModalOpen(false)} onOk={() => linkMutation.mutate()} confirmLoading={linkMutation.isPending} width={400}>
        <div style={{ marginBottom: 16 }}>
          <div style={{ marginBottom: 8 }}>快照月份: <strong>{snapMonth}</strong></div>
          <div style={{ marginBottom: 8, fontWeight: 500 }}>目标账单月份</div>
          <Input placeholder="如 2026-05" value={linkBillMonth} onChange={(e) => setLinkBillMonth(e.target.value)} style={{ width: '100%' }} />
          <div style={{ marginTop: 8, color: '#999', fontSize: 12 }}>关联后，该月份的费用分摊将以快照数据为准</div>
        </div>
      </Modal>
    </div>
  )
}

export default PhoneOwnershipPage
