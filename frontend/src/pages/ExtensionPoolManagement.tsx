import { useState } from 'react'
import { Table, Button, Card, Select, Tag, Space, Input, Modal, Form, Statistic, Row, Col, message, Dropdown } from 'antd'
import { UserAddOutlined, RollbackOutlined, SwapOutlined, LinkOutlined } from '@ant-design/icons'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { extensionNumberApi, type ExtensionNumber } from '@/api/extensionNumber'
import { request } from '@/api/request'

// 状态定义: 0=可用(电话号码为空时显示闲置) 1=占用 2=分配中
const getStatusDisplay = (record: ExtensionNumber) => {
  if (record.status === 0) {
    // 可用状态：电话号为空显示"闲置"，否则显示"可用"
    return record.phoneNumber
      ? { label: '可用', color: 'green' }
      : { label: '闲置', color: 'default' }
  }
  if (record.status === 1) return { label: '占用', color: 'blue' }
  if (record.status === 2) return { label: '分配中', color: 'orange' }
  return { label: '未知', color: 'default' }
}

const ExtensionPoolManagement = () => {
  const [keyword, setKeyword] = useState('')
  const [statusFilter, setStatusFilter] = useState<number | undefined>(undefined)
  const [deptFilter, setDeptFilter] = useState<number | undefined>(undefined)
  const [page, setPage] = useState(0)
  const [size] = useState(20)

  // 操作弹窗
  const [actionModalOpen, setActionModalOpen] = useState(false)
  const [selectedExt, setSelectedExt] = useState<ExtensionNumber | null>(null)

  // 分配弹窗
  const [allocateModalOpen, setAllocateModalOpen] = useState(false)
  const [form] = Form.useForm()

  const queryClient = useQueryClient()

  const { data: listData, isLoading } = useQuery({
    queryKey: ['extension-numbers', keyword, statusFilter, deptFilter, page],
    queryFn: async () => {
      const res = await extensionNumberApi.search({
        keyword: keyword || undefined,
        status: statusFilter !== undefined ? statusFilter : undefined,
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
  const subDepts = orgs.filter((o: any) => o.level >= 2)

  const allocateMutation = useMutation({
    mutationFn: async () => {
      if (!selectedExt) return
      const values = await form.validateFields()
      return extensionNumberApi.allocate(selectedExt.id, {
        employeeName: values.employeeName,
        deptOrgId: values.deptOrgId,
        deptName: orgs.find((o: any) => o.id === values.deptOrgId)?.name,
        phoneNumber: values.phoneNumber,
      })
    },
    onSuccess: () => {
      message.success('分配请求已提交，将生成工单')
      setAllocateModalOpen(false)
      setActionModalOpen(false)
      form.resetFields()
      queryClient.invalidateQueries({ queryKey: ['extension-numbers'] })
    },
    onError: () => message.error('分配失败'),
  })

  const reclaimMutation = useMutation({
    mutationFn: (id: number) => extensionNumberApi.reclaim(id),
    onSuccess: () => {
      message.success('回收请求已提交，将生成工单')
      setActionModalOpen(false)
      queryClient.invalidateQueries({ queryKey: ['extension-numbers'] })
    },
    onError: () => message.error('回收失败'),
  })

  // 点击分机号 → 打开操作弹窗
  const handleExtClick = (record: ExtensionNumber) => {
    setSelectedExt(record)
    setActionModalOpen(true)
  }

  // 分配
  const handleAllocate = () => {
    setActionModalOpen(false)
    form.resetFields()
    if (selectedExt?.phoneNumber) form.setFieldsValue({ phoneNumber: selectedExt.phoneNumber })
    setAllocateModalOpen(true)
  }

  // 回收
  const handleReclaim = () => {
    if (!selectedExt) return
    setActionModalOpen(false)
    Modal.confirm({
      title: '确认回收',
      content: `确认回收分机号 ${selectedExt.extensionNumber}？此操作将生成回收工单。`,
      onOk: () => reclaimMutation.mutate(selectedExt.id),
    })
  }

  // 变更操作（暂只弹窗提示，工单页面未完成）
  const handleChange = (changeType: string) => {
    setActionModalOpen(false)
    const labels: Record<string, string> = {
      phone: '变更电话号码',
      user: '变更使用人',
      dept: '变更使用部门',
    }
    Modal.info({
      title: `${labels[changeType]}`,
      content: `分机号 ${selectedExt?.extensionNumber} 的${labels[changeType]}操作将生成工单，工单功能开发中，敬请期待。`,
    })
  }

  const content: ExtensionNumber[] = listData?.data?.content || []

  const columns = [
    {
      title: '分机号', dataIndex: 'extensionNumber', key: 'extensionNumber', width: 120,
      render: (v: string, record: ExtensionNumber) => (
        <a onClick={() => handleExtClick(record)} style={{ fontWeight: 500, color: '#1677ff', cursor: 'pointer' }}>
          {v}
        </a>
      ),
    },
    {
      title: '电话号码', dataIndex: 'phoneNumber', key: 'phoneNumber', width: 150,
      render: (v: string) => v || <span style={{ color: '#bfbfbf' }}>-</span>,
    },
    {
      title: '使用人', dataIndex: 'employeeName', key: 'employeeName', width: 120,
      render: (v: string) => v || <span style={{ color: '#bfbfbf' }}>-</span>,
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
      title: '状态', dataIndex: 'status', key: 'status', width: 100,
      render: (_: number, record: ExtensionNumber) => {
        const display = getStatusDisplay(record)
        return <Tag color={display.color}>{display.label}</Tag>
      },
    },
  ]

  // 统计
  const availableCount = content.filter(c => c.status === 0).length
  const occupiedCount = content.filter(c => c.status === 1).length
  const allocatingCount = content.filter(c => c.status === 2).length

  // 变更下拉菜单
  const changeMenuItems = [
    { key: 'phone', label: '变更电话号码' },
    { key: 'user', label: '变更使用人' },
    { key: 'dept', label: '变更使用部门' },
  ]

  return (
    <div>
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <Card><Statistic title="可用/闲置" value={availableCount} valueStyle={{ color: '#52c41a' }} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="占用" value={occupiedCount} valueStyle={{ color: '#1677ff' }} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="分配中" value={allocatingCount} valueStyle={{ color: '#fa8c16' }} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="总数" value={listData?.data?.totalElements || 0} /></Card>
        </Col>
      </Row>

      <Card>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <Space>
            <Input.Search placeholder="搜索分机号/使用人/部门/电话" value={keyword} onChange={e => { setKeyword(e.target.value); setPage(0) }} style={{ width: 260 }} />
            <Select placeholder="状态筛选" value={statusFilter} onChange={v => { setStatusFilter(v); setPage(0) }} style={{ width: 130 }} allowClear>
              <Select.Option value={0}>可用/闲置</Select.Option>
              <Select.Option value={1}>占用</Select.Option>
              <Select.Option value={2}>分配中</Select.Option>
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

      {/* 操作弹窗：点击分机号后弹出 */}
      <Modal
        title={`分机号 ${selectedExt?.extensionNumber || ''} - 操作`}
        open={actionModalOpen}
        onCancel={() => setActionModalOpen(false)}
        footer={null}
        width={420}
      >
        {selectedExt && (
          <div>
            {/* 分机信息展示 */}
            <div style={{ background: '#fafafa', padding: 16, borderRadius: 8, marginBottom: 20 }}>
              <Row gutter={[8, 8]}>
                <Col span={12}><span style={{ color: '#8c8c8c' }}>分机号：</span>{selectedExt.extensionNumber}</Col>
                <Col span={12}><span style={{ color: '#8c8c8c' }}>电话号码：</span>{selectedExt.phoneNumber || '-'}</Col>
                <Col span={12}><span style={{ color: '#8c8c8c' }}>使用人：</span>{selectedExt.employeeName || '-'}</Col>
                <Col span={12}><span style={{ color: '#8c8c8c' }}>状态：</span>{(() => { const d = getStatusDisplay(selectedExt); return <Tag color={d.color}>{d.label}</Tag> })()}</Col>
                {selectedExt.branchName && <Col span={12}><span style={{ color: '#8c8c8c' }}>分行：</span>{selectedExt.branchName}</Col>}
                {selectedExt.deptName && <Col span={12}><span style={{ color: '#8c8c8c' }}>部门：</span>{selectedExt.deptName}</Col>}
              </Row>
            </div>

            {/* 操作按钮 */}
            <Space direction="vertical" style={{ width: '100%' }} size={12}>
              {/* 分配：仅可用/闲置状态 */}
              {selectedExt.status === 0 && (
                <Button type="primary" icon={<UserAddOutlined />} block onClick={handleAllocate}>
                  分配（生成工单）
                </Button>
              )}

              {/* 变更：仅占用状态 */}
              {selectedExt.status === 1 && (
                <Dropdown menu={{ items: changeMenuItems, onClick: ({ key }) => handleChange(key) }} placement="bottomLeft">
                  <Button icon={<SwapOutlined />} block style={{ borderColor: '#1677ff', color: '#1677ff' }}>
                    变更（生成工单） ▾
                  </Button>
                </Dropdown>
              )}

              {/* 回收：占用或分配中状态 */}
              {(selectedExt.status === 1 || selectedExt.status === 2) && (
                <Button danger icon={<RollbackOutlined />} block onClick={handleReclaim}>
                  回收（生成工单）
                </Button>
              )}

              {/* 分配中状态提示 */}
              {selectedExt.status === 2 && (
                <div style={{ color: '#fa8c16', fontSize: 12, textAlign: 'center' }}>
                  该分机号正在分配中，工单处理完成后状态将自动更新
                </div>
              )}
            </Space>
          </div>
        )}
      </Modal>

      {/* 分配弹窗 */}
      <Modal
        title={`分配分机号 - ${selectedExt?.extensionNumber || ''}`}
        open={allocateModalOpen}
        onCancel={() => { setAllocateModalOpen(false); form.resetFields() }}
        onOk={() => allocateMutation.mutate()}
        confirmLoading={allocateMutation.isPending}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="employeeName" label="使用人" rules={[{ required: true, message: '请输入使用人' }]}>
            <Input placeholder="输入使用人姓名" />
          </Form.Item>
          <Form.Item name="deptOrgId" label="使用部门">
            <Select placeholder="选择部门" allowClear>
              {subDepts.map((d: any) => <Select.Option key={d.id} value={d.id}>{d.name}</Select.Option>)}
            </Select>
          </Form.Item>
          <Form.Item name="phoneNumber" label="外线电话号码">
            <Input placeholder="关联外线电话号码" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default ExtensionPoolManagement
