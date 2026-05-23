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
  message,
  Upload,
  Row,
  Col,
  Statistic,
} from 'antd'
import { UploadOutlined, DownloadOutlined, EditOutlined } from '@ant-design/icons'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  phoneOwnershipApi,
  type PhoneOwnership,
  type ImportCompareItem,
} from '@/api/phoneOwnership'
import { request } from '@/api/request'

const PhoneOwnershipPage = () => {
  const [keyword, setKeyword] = useState('')
  const [branchOrgId, setBranchOrgId] = useState<number | undefined>(undefined)
  const [page, setPage] = useState(0)
  const [size] = useState(20)

  const [editModalOpen, setEditModalOpen] = useState(false)
  const [editRecord, setEditRecord] = useState<PhoneOwnership | null>(null)
  const [editBranch, setEditBranch] = useState<number | undefined>(undefined)
  const [editDept, setEditDept] = useState<number | undefined>(undefined)
  const [editRemark, setEditRemark] = useState('')

  const [compareModalOpen, setCompareModalOpen] = useState(false)
  const [compareData, setCompareData] = useState<ImportCompareItem[]>([])

  const queryClient = useQueryClient()

  const { data: listData, isLoading } = useQuery({
    queryKey: ['phone-ownership', keyword, branchOrgId, page],
    queryFn: async () => {
      const res = await phoneOwnershipApi.search({
        keyword: keyword || undefined,
        branchOrgId,
        page,
        size,
      })
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
  const branches = orgs.filter((o: any) => o.level === 1)

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
    onSuccess: (res: any) => {
      const count = res.data?.data || 0
      message.success(`导入成功，共更新 ${count} 条记录`)
      setCompareModalOpen(false)
      queryClient.invalidateQueries({ queryKey: ['phone-ownership'] })
    },
    onError: () => message.error('导入确认失败'),
  })

  const handleImport = async (file: File) => {
    try {
      const res = await phoneOwnershipApi.importCompare(file)
      const items: ImportCompareItem[] = res.data?.data || []
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
    const token = localStorage.getItem('token')
    const url = `/api/phone-ownership/export?token=${token}`
    window.open(url, '_blank')
  }

  const openEdit = (record: PhoneOwnership) => {
    setEditRecord(record)
    setEditBranch(record.branchOrgId || undefined)
    setEditDept(record.deptOrgId || undefined)
    setEditRemark(record.remark || '')
    setEditModalOpen(true)
  }

  const depts = editBranch ? orgs.filter((o: any) => o.parentId === editBranch) : []

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

  const totalCount = listData?.data?.totalElements || 0
  const content: PhoneOwnership[] = listData?.data?.content || []

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

  return (
    <div>
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={8}>
          <Card>
            <Statistic title="号码总数" value={totalCount} />
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic
              title="已归属分行"
              value={content.filter((c) => c.branchName).length}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic
              title="已归属部门"
              value={content.filter((c) => c.deptName).length}
              valueStyle={{ color: '#1890ff' }}
            />
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
              placeholder="搜索号码/分行/部门"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              style={{ width: 220 }}
              onSearch={() => setPage(0)}
            />
            <Select
              placeholder="筛选分行"
              value={branchOrgId}
              onChange={(v) => {
                setBranchOrgId(v)
                setPage(0)
              }}
              style={{ width: 150 }}
              allowClear
            >
              {branches.map((b: any) => (
                <Select.Option key={b.id} value={b.id}>
                  {b.branchName || b.name}
                </Select.Option>
              ))}
            </Select>
          </Space>
          <Space>
            <Upload
              accept=".csv,.txt"
              showUploadList={false}
              beforeUpload={(file) => {
                handleImport(file)
                return false
              }}
            >
              <Button icon={<UploadOutlined />}>导入</Button>
            </Upload>
            <Button icon={<DownloadOutlined />} onClick={handleExport}>
              导出
            </Button>
          </Space>
        </div>

        <Table
          columns={columns}
          dataSource={content}
          loading={isLoading}
          rowKey="id"
          pagination={{
            current: page + 1,
            pageSize: size,
            total: listData?.data?.totalElements || 0,
            onChange: (p) => setPage(p - 1),
          }}
        />
      </Card>

      <Modal
        title={`编辑号码归属 - ${editRecord?.phoneNumber || ''}`}
        open={editModalOpen}
        onCancel={() => setEditModalOpen(false)}
        onOk={() => updateMutation.mutate()}
        confirmLoading={updateMutation.isPending}
        width={500}
      >
        <div style={{ marginBottom: 16 }}>
          <div style={{ marginBottom: 8, fontWeight: 500 }}>归属分行</div>
          <Select
            style={{ width: '100%' }}
            placeholder="选择分行"
            value={editBranch}
            onChange={(v) => {
              setEditBranch(v)
              setEditDept(undefined)
            }}
            allowClear
          >
            {branches.map((b: any) => (
              <Select.Option key={b.id} value={b.id}>
                {b.branchName || b.name}
              </Select.Option>
            ))}
          </Select>
        </div>
        <div style={{ marginBottom: 16 }}>
          <div style={{ marginBottom: 8, fontWeight: 500 }}>归属部门</div>
          <Select
            style={{ width: '100%' }}
            placeholder="选择部门"
            value={editDept}
            onChange={setEditDept}
            allowClear
          >
            {depts.map((d: any) => (
              <Select.Option key={d.id} value={d.id}>
                {d.name}
              </Select.Option>
            ))}
          </Select>
        </div>
        <div>
          <div style={{ marginBottom: 8, fontWeight: 500 }}>备注</div>
          <Input.TextArea
            rows={3}
            value={editRemark}
            onChange={(e) => setEditRemark(e.target.value)}
            placeholder="输入备注"
          />
        </div>
      </Modal>

      <Modal
        title={`导入比对结果（新增 ${newCount} / 变更 ${diffCount} / 不变 ${sameCount}）`}
        open={compareModalOpen}
        onCancel={() => setCompareModalOpen(false)}
        width={900}
        footer={[
          <Button key="cancel" onClick={() => setCompareModalOpen(false)}>
            取消
          </Button>,
          <Button
            key="confirm"
            type="primary"
            onClick={() => confirmImportMutation.mutate(compareData)}
            loading={confirmImportMutation.isPending}
          >
            确认导入
          </Button>,
        ]}
      >
        <Table
          columns={compareColumns}
          dataSource={compareData}
          rowKey="phoneNumber"
          size="small"
          pagination={{ pageSize: 10 }}
          scroll={{ y: 400 }}
        />
      </Modal>
    </div>
  )
}

export default PhoneOwnershipPage
