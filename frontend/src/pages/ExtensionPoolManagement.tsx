import { useState } from 'react'
import { Table, Button, Card, Select, Tag, Space, Input, Modal, Form, Statistic, Row, Col, message } from 'antd'
import { UserAddOutlined, RollbackOutlined } from '@ant-design/icons'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { extensionNumberApi, type ExtensionNumber } from '@/api/extensionNumber'
import { request } from '@/api/request'

const STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '可分配', color: 'green' },
  2: { label: '闲置(无电话)', color: 'orange' },
  1: { label: '已占用', color: 'blue' },
}

const ExtensionPoolManagement = () => {
  const [keyword, setKeyword] = useState('')
  const [statusFilter, setStatusFilter] = useState<number | undefined>(undefined)
  const [deptFilter, setDeptFilter] = useState<number | undefined>(undefined)
  const [page, setPage] = useState(0)
  const [size] = useState(20)

  const [allocateModalOpen, setAllocateModalOpen] = useState(false)
  const [selectedExt, setSelectedExt] = useState<ExtensionNumber | null>(null)
  const [form] = Form.useForm()

  const queryClient = useQueryClient()

  const { data: listData, isLoading } = useQuery({
    queryKey: ['extension-numbers', keyword, statusFilter, deptFilter, page],
    queryFn: async () => {
      const res = await extensionNumberApi.search({
        keyword: keyword || undefined,
        status: statusFilter || undefined,
        deptOrgId: deptFilter || undefined,
        page, size,
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
  const depts = orgs.filter((o: any) => o.level >= 2)

  const allocateMutation = useMutation({
    mutationFn: async () => {
      if (!selectedExt) return
      const values = await form.validateFields()
      return extensionNumberApi.allocate(selectedExt.id, {
        userName: values.userName,
        deptOrgId: values.deptOrgId,
        deptName: orgs.find((o: any) => o.id === values.deptOrgId)?.name,
        phoneNumber: values.phoneNumber,
      })
    },
    onSuccess: () => {
      message.success('分配成功，已生成工单')
      setAllocateModalOpen(false)
      form.resetFields()
      queryClient.invalidateQueries({ queryKey: ['extension-numbers'] })
    },
    onError: () => message.error('分配失败'),
  })

  const reclaimMutation = useMutation({
    mutationFn: (id: number) => extensionNumberApi.reclaim(id),
    onSuccess: () => {
      message.success('回收成功，已生成工单')
      queryClient.invalidateQueries({ queryKey: ['extension-numbers'] })
    },
    onError: () => message.error('回收失败'),
  })

  const handleAllocate = (record: ExtensionNumber) => {
    setSelectedExt(record)
    form.resetFields()
    if (record.phoneNumber) form.setFieldsValue({ phoneNumber: record.phoneNumber })
    setAllocateModalOpen(true)
  }

  const handleReclaim = (record: ExtensionNumber) => {
    Modal.confirm({
      title: '确认回收',
      content: `确认回收分机号 ${record.extensionNumber}？此操作将生成回收工单。`,
      onOk: () => reclaimMutation.mutate(record.id),
    })
  }

  const content: ExtensionNumber[] = listData?.data?.content || []

  const columns = [
    {
      title: '分机号', dataIndex: 'extensionNumber', key: 'extensionNumber', width: 120,
      render: (v: string, r: ExtensionNumber) => (
        <span style={{ fontWeight: r.status === 0 ? 600 : 400, color: r.status === 0 ? '#52c41a' : undefined }}>
          {v}
        </span>
      ),
    },
    {
      title: '使用人', dataIndex: 'userName', key: 'userName', width: 120,
      render: (v: string) => v || <span style={{ color: '#bfbfbf' }}>空</span>,
    },
    {
      title: '分行', dataIndex: 'branchName', key: 'branchName', width: 120,
      render: (v: string) => v || '-',
    },
    {
      title: '使用部门', dataIndex: 'deptName', key: 'deptName', width: 150,
      render: (v: string) => v || '-',
    },
    {
      title: '电话号码', dataIndex: 'phoneNumber', key: 'phoneNumber', width: 150,
      render: (v: string) => v || '-',
    },
    {
      title: '状态', dataIndex: 'status', key: 'status', width: 100,
      render: (s: string) => {
        const m = STATUS_MAP[s] || { label: s, color: 'default' }
        return <Tag color={m.color}>{m.label}</Tag>
      },
    },
    {
      title: '操作', key: 'actions', width: 160,
      render: (_: unknown, record: ExtensionNumber) => (
        <Space size="small">
          {record.status !== 1 && (
            <Button size="small" type="primary" icon={<UserAddOutlined />} onClick={() => handleAllocate(record)}>分配</Button>
          )}
          {record.status === 1 && (
            <Button size="small" danger icon={<RollbackOutlined />} onClick={() => handleReclaim(record)}>回收</Button>
          )}
        </Space>
      ),
    },
  ]

  const availableCount = content.filter(c => c.status === 0).length
  const idleCount = content.filter(c => c.status === 2).length
  const allocatedCount = content.filter(c => c.status === 1).length

  const subDepts = orgs.filter((o: any) => o.level >= 2)

  return (
    <div>
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <Card><Statistic title="可分配" value={availableCount} valueStyle={{ color: '#52c41a' }} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="闲置" value={idleCount} valueStyle={{ color: '#faad14' }} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="已占用" value={allocatedCount} valueStyle={{ color: '#1890ff' }} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="总数" value={listData?.data?.totalElements || 0} /></Card>
        </Col>
      </Row>

      <Card>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <Space>
            <Input.Search placeholder="搜索分机号/使用人/部门/电话" value={keyword} onChange={e => { setKeyword(e.target.value); setPage(0) }} style={{ width: 260 }} />
            <Select placeholder="状态筛选" value={statusFilter} onChange={v => { setStatusFilter(v); setPage(0) }} style={{ width: 120 }} allowClear>
              <Select.Option value={0}>可分配</Select.Option>
              <Select.Option value={2}>闲置</Select.Option>
              <Select.Option value={1}>已占用</Select.Option>
            </Select>
            <Select placeholder="部门筛选" value={deptFilter} onChange={v => { setDeptFilter(v); setPage(0) }} style={{ width: 150 }} allowClear>
              {subDepts.map((d: any) => <Select.Option key={d.id} value={d.id}>{d.name}</Select.Option>)}
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
            pageSize: size,
            total: listData?.data?.totalElements || 0,
            onChange: (p) => setPage(p - 1),
          }}
        />
      </Card>

      <Modal
        title={`分配分机号 - ${selectedExt?.extensionNumber || ''}`}
        open={allocateModalOpen}
        onCancel={() => { setAllocateModalOpen(false); form.resetFields() }}
        onOk={() => allocateMutation.mutate()}
        confirmLoading={allocateMutation.isPending}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="userName" label="使用人" rules={[{ required: true, message: '请输入使用人' }]}>
            <Input placeholder="输入使用人姓名" />
          </Form.Item>
          <Form.Item name="deptOrgId" label="使用部门">
            <Select placeholder="选择部门" allowClear>
              {subDepts.map((d: any) => <Select.Option key={d.id} value={d.id}>{d.name}</Select.Option>)}
            </Select>
          </Form.Item>
          <Form.Item name="phoneNumber" label="电话号码">
            <Input placeholder="关联电话号码" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default ExtensionPoolManagement
