import { useState, useMemo } from 'react'
import { useAuthStore } from '@/stores/authStore'
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
import { UploadOutlined, DownloadOutlined, EditOutlined, ReloadOutlined, LinkOutlined } from '@ant-design/icons'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  phoneOwnershipApi,
  type PhoneOwnership,
  type ImportCompareItem,
} from '@/api/phoneOwnership'
import { ApiGet } from '@/api/request'
import type { OrgStructure } from '@/types/org'
import { snapshotApi } from '@/api/snapshot'
import type { PhoneSnapshot } from '@/types/snapshot'
import {
  SNAPSHOT_STATUS_LABELS,
  SNAPSHOT_STATUS_COLORS,
  ALLOC_STATUS_LABELS,
} from '@/types/snapshot'

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
    // Read token from Zustand store (works outside React via getState)
    const token = useAuthStore.getState().token
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
  const columns = useMemo(() => [
    { title: '电话号码', dataIndex: 'phoneNumber', key: 'phoneNumber', width: 150 },
    {
      title: '一级分行',
      dataIndex: 'level1BranchName',
      key: 'level1BranchName',
      width: 130,
      render: (v: string) => v || '-',
    },
    {
      title: '二级分行/部门',
      dataIndex: 'level2OrgName',
      key: 'level2OrgName',
      width: 150,
      render: (v: string) => v || '-',
    },
    {
      title: '部门/支行',
      dataIndex: 'level3OrgName',
      key: 'level3OrgName',
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
  ], [])

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
  const snapColumns = useMemo(() => [
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
          { title: '一级分行', dataIndex: 'branchName', key: 'branchName', width: 120, render: (v: string | null) => v || '-' },
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
  ], [])

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
                  <Col span={8}>
                    <Card><Statistic title="号码总数" value={totalCount} /></Card>
                  </Col>
                  <Col span={8}>
                    <Card>
                      <Statistic title="已归属一级分行" value={content.filter((c) => c.level1BranchName).length} valueStyle={{ color: '#52c41a' }} />
                    </Card>
                  </Col>
                  <Col span={8}>
                    <Card>
                      <Statistic title="已归属二级分行" value={content.filter((c) => c.level2OrgName).length} valueStyle={{ color: '#1890ff' }} />
                    </Card>
                  </Col>
                </Row>
                <Card>
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
