import { useState, useCallback } from 'react'
import { Table, Button, Card, Select, Tag, Space, Modal, message, Input, Row, Col, Statistic, Drawer, Descriptions, Timeline, Form, Dropdown } from 'antd'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { workOrderApi } from '@/api/workOrder'
import { useAuthStore } from '@/stores/authStore'
import { PlusOutlined, CheckOutlined, EyeOutlined } from '@ant-design/icons'
import { request } from '@/api/request'
import type { WorkOrder, WorkOrderItem } from '@/types/workOrder'

const { Option } = Select
const { TextArea } = Input

const STATUS_COLORS: Record<number, string> = { 0: 'warning', 1: 'processing', 2: 'processing', 3: 'success', 4: 'default', 5: 'error' }
const STATUS_NAMES: Record<number, string> = { 0: '待处理', 1: '挂起', 2: '处理中', 3: '已完成', 4: '已归档', 5: '已取消' }
const PRIORITY_COLORS: Record<number, string> = { 1: 'success', 2: 'default', 3: 'warning', 4: 'error' }
const PRIORITY_NAMES: Record<number, string> = { 1: '低', 2: '普通', 3: '高', 4: '紧急' }
const TYPE_NAMES: Record<number, string> = { 1: '新增', 2: '变更', 3: '解绑', 4: '座机绑定', 5: '号码拆机' }
const TYPE_COLORS: Record<number, string> = { 1: 'green', 2: 'blue', 3: 'orange', 4: 'purple', 5: 'red' }
const ITEM_TYPE_NAMES: Record<number, string> = { 1: '号码', 2: '设备', 3: '员工' }
const ITEM_STATUS_NAMES: Record<number, string> = { 0: '待执行', 1: '执行中', 2: '已完成', 3: '失败', 4: '已跳过' }
const ITEM_STATUS_COLORS: Record<number, string> = { 0: 'processing', 1: 'processing', 2: 'success', 3: 'error', 4: 'default' }

/** 已完成/已归档/已取消的工单归入历史 */
const isHistorical = (s: number) => s === 3 || s === 4 || s === 5

const WorkOrderManagement = () => {
  const [status, setStatus] = useState<number | ''>('')
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false)
  const [detailDrawerOpen, setDetailDrawerOpen] = useState(false)
  const [selectedOrder, setSelectedOrder] = useState<WorkOrder | null>(null)
  const [selectedType, setSelectedType] = useState(1)
  const [extOptions, setExtOptions] = useState<any[]>([])
  const [extLoading, setExtLoading] = useState(false)
  const [form] = Form.useForm()
  const queryClient = useQueryClient()
  const { user } = useAuthStore()

  const { data: orderData, isLoading, refetch } = useQuery({
    queryKey: ['work-orders', status],
    queryFn: async () => {
      const params: Record<string, unknown> = { page: 0, size: 100 }
      if (status !== '') params.status = status
      return workOrderApi.getList(params)
    }
  })

  const acceptMutation = useMutation({
    mutationFn: (id: number) => workOrderApi.accept(id),
    onSuccess: () => { message.success('工单已接受'); queryClient.invalidateQueries({ queryKey: ['work-orders'] }) },
    onError: () => message.error('接受失败')
  })

  const completeMutation = useMutation({
    mutationFn: (id: number) => workOrderApi.complete(id),
    onSuccess: () => { message.success('工单已完成'); queryClient.invalidateQueries({ queryKey: ['work-orders'] }) },
    onError: () => message.error('完成失败')
  })

  const rejectMutation = useMutation({
    mutationFn: ({ id, reason }: { id: number; reason: string }) => workOrderApi.reject(id, reason),
    onSuccess: () => { message.success('工单已拒绝'); queryClient.invalidateQueries({ queryKey: ['work-orders'] }) },
    onError: () => message.error('拒绝失败')
  })

  const createMutation = useMutation({
    mutationFn: (data: Record<string, unknown>) => workOrderApi.create(data),
    onSuccess: () => { message.success('工单创建成功'); queryClient.invalidateQueries({ queryKey: ['work-orders'] }); setIsCreateModalOpen(false); form.resetFields() },
    onError: () => message.error('创建失败')
  })

  const executeItemMutation = useMutation({
    mutationFn: (itemId: number) => workOrderApi.executeItem(itemId),
    onSuccess: () => { message.success('工单项执行成功'); queryClient.invalidateQueries({ queryKey: ['work-orders'] }); if (selectedOrder) handleViewDetail(selectedOrder) },
    onError: () => message.error('执行失败')
  })

  const handleAccept = (record: WorkOrder) => { Modal.confirm({ title: '接受工单', content: `接受工单 ${record.workOrderNo}？将指派给您（${user?.username || '当前用户'}）处理`, onOk: () => acceptMutation.mutate(record.id) }) }
  const handleComplete = (record: WorkOrder) => { Modal.confirm({ title: '完成工单', content: `标记工单 ${record.workOrderNo} 为已完成？`, onOk: () => completeMutation.mutate(record.id) }) }
  const handleReject = (record: WorkOrder) => { let r = ''; Modal.confirm({ title: '拒绝工单', content: (<div><p>拒绝工单 {record.workOrderNo}</p><TextArea rows={3} placeholder="请输入拒绝原因" onChange={(e) => { r = e.target.value }} /></div>), onOk: () => { if (r) rejectMutation.mutate({ id: record.id, reason: r }); else message.warning('请输入拒绝原因') } }) }
  const handleViewDetail = (record: WorkOrder) => { workOrderApi.getById(record.id).then(res => { setSelectedOrder(res.data?.data || record); setDetailDrawerOpen(true) }).catch(() => { setSelectedOrder(record); setDetailDrawerOpen(true) }) }
  const handleExecuteItem = (itemId: number) => { Modal.confirm({ title: '执行工单项', content: '确认要执行此工单项？', onOk: () => executeItemMutation.mutate(itemId) }) }

  // 搜索分机号
  const handleExtSearch = useCallback(async (value: string) => {
    if (!value || value.length < 1) { setExtOptions([]); return }
    setExtLoading(true)
    try {
      const res = await request.get('/extension-numbers', { params: { keyword: value, page: 0, size: 20 } })
      const content = res.data?.data?.content || []
      setExtOptions(content.map((e: any) => ({ label: `${e.extensionNumber} ${e.employeeName ? '(' + e.employeeName + ')' : ''}`, value: e.extensionNumber })))
    } catch { setExtOptions([]) }
    setExtLoading(false)
  }, [])

  // 选中分机号后自动填充
  const handleExtSelect = useCallback(async (value: string) => {
    try {
      const res = await request.get(`/extension-numbers/detail/${value}`)
      const d = res.data?.data
      if (d) {
        form.setFieldsValue({
          employeeName: d.employeeName || '',
          macAddresses: d.macAddresses?.join(', ') || '',
          branchName: d.branchName || '',
          deptName: d.deptName || '',
        })
      }
    } catch { /* ignore */ }
  }, [form])

  // 从Excel粘贴: tab分隔依次填入分机号、使用人、员工ID、MAC、分行、部门、备注
  const PASTE_FIELDS = ['extensionNumber', 'employeeName', 'employeeNo', 'macAddresses', 'branchName', 'deptName', 'remark']

  const handleFormPaste = useCallback((e: React.ClipboardEvent) => {
    const text = e.clipboardData.getData('text/plain')
    if (!text || !text.includes('\t')) return
    e.preventDefault()
    const values = text.replace(/\r?\n$/, '').split('\t')
    const patch: Record<string, string> = {}
    PASTE_FIELDS.forEach((field, i) => {
      if (values[i] !== undefined && values[i] !== '') {
        patch[field] = values[i].trim()
      }
    })
    form.setFieldsValue(patch)
    message.success(`已粘贴 ${Object.keys(patch).length} 个字段`)
    if (patch.extensionNumber) {
      handleExtSelect(patch.extensionNumber)
    }
  }, [form, handleExtSelect])

  // 选择工单类型后，进入创建表单
  const handleTypeSelect = (type: number) => {
    setSelectedType(type)
    form.resetFields()
    form.setFieldsValue({ type })
    setIsCreateModalOpen(true)
  }

  // --- 列定义 ---
  // 后端 order_type → 前端 orderType
  const commonColumns = [
    { title: '工单号', dataIndex: 'workOrderNo', key: 'workOrderNo', width: 150 },
    { title: '标题', dataIndex: 'title', key: 'title', width: 200 },
    { title: '类型', dataIndex: 'orderType', key: 'orderType', width: 110, render: (t: number) => <Tag color={TYPE_COLORS[t] || 'default'}>{TYPE_NAMES[t] || t}</Tag> },
    { title: '申请人', dataIndex: 'requesterName', key: 'requesterName', width: 90 },
    { title: '处理人', dataIndex: 'handlerName', key: 'handlerName', width: 90 },
    { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 150 },
  ]

  // 进行中工单列：操作列含状态Tag + 按钮
  const activeColumns = [
    ...commonColumns,
    {
      title: '操作', key: 'actions', width: 280,
      render: (_: unknown, record: WorkOrder) => {
        const s = record.status
        return (
          <Space>
            <Tag color={STATUS_COLORS[s] || 'default'}>{STATUS_NAMES[s] || s}</Tag>
            <Button size="small" icon={<EyeOutlined />} onClick={() => handleViewDetail(record)}>详情</Button>
            {s === 0 && <Button size="small" type="primary" icon={<CheckOutlined />} onClick={() => handleAccept(record)}>接受</Button>}
            {(s === 1 || s === 2) && <Button size="small" type="primary" onClick={() => handleComplete(record)}>完成</Button>}
            {!isHistorical(s) && <Button size="small" danger onClick={() => handleReject(record)}>拒绝</Button>}
          </Space>
        )
      }
    }
  ]

  // 历史工单列：操作列仅含状态Tag + 详情
  const historyColumns = [
    ...commonColumns,
    {
      title: '操作', key: 'actions', width: 180,
      render: (_: unknown, record: WorkOrder) => {
        const s = record.status
        return (
          <Space>
            <Tag color={STATUS_COLORS[s] || 'default'}>{STATUS_NAMES[s] || s}</Tag>
            <Button size="small" icon={<EyeOutlined />} onClick={() => handleViewDetail(record)}>详情</Button>
          </Space>
        )
      }
    }
  ]

  const orders: WorkOrder[] = orderData?.data?.data?.content || []
  const activeOrders = orders.filter(o => !isHistorical(o.status))
  const historyOrders = orders.filter(o => isHistorical(o.status))
  const pendingCount = activeOrders.filter(o => o.status === 0).length
  const suspendedCount = activeOrders.filter(o => o.status === 1).length
  const processingCount = activeOrders.filter(o => o.status === 2).length

  return (
    <div>
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}><Card><Statistic title="待处理" value={pendingCount} valueStyle={{ color: '#faad14' }} /></Card></Col>
        <Col span={6}><Card><Statistic title="挂起" value={suspendedCount} valueStyle={{ color: '#1890ff' }} /></Card></Col>
        <Col span={6}><Card><Statistic title="处理中" value={processingCount} valueStyle={{ color: '#4096ff' }} /></Card></Col>
        <Col span={6}><Card><Statistic title="历史工单" value={historyOrders.length} valueStyle={{ color: '#52c41a' }} /></Card></Col>
      </Row>

      {/* 进行中工单 */}
      <Card title="进行中" style={{ marginBottom: 16 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <Space>
            <Button onClick={() => refetch()}>刷新</Button>
          </Space>
          <Dropdown
            menu={{
              items: Object.entries(TYPE_NAMES).map(([value, label]) => ({
                key: value,
                label: <Tag color={TYPE_COLORS[Number(value)]}>{label}</Tag>,
              })),
              onClick: ({ key }) => handleTypeSelect(Number(key)),
            }}
            trigger={['click']}
          >
            <Button type="primary" icon={<PlusOutlined />}>新建</Button>
          </Dropdown>
        </div>
        <Table columns={activeColumns} dataSource={activeOrders} loading={isLoading} rowKey="id" pagination={false} size="small" />
      </Card>

      {/* 历史工单 */}
      <Card title="历史工单">
        <Table columns={historyColumns} dataSource={historyOrders} rowKey="id" pagination={{ pageSize: 10 }} size="small" />
      </Card>

      {/* 创建工单表单 */}
      <Modal
        title={`新建${TYPE_NAMES[selectedType] || ''}工单`}
        open={isCreateModalOpen}
        onCancel={() => { setIsCreateModalOpen(false); form.resetFields() }}
        onOk={() => { form.validateFields().then(values => createMutation.mutate(values)) }}
        confirmLoading={createMutation.isPending}
        width={560}
      >
        <Form form={form} layout="vertical" initialValues={{ type: selectedType }} onPaste={handleFormPaste}>
          <Form.Item name="type" label="工单类型">
            <Input disabled />
          </Form.Item>

          <Form.Item name="extensionNumber" label="分机号" rules={[{ required: true, message: '请选择分机号' }]}>
            <Select
              showSearch
              filterOption={false}
              onSearch={handleExtSearch}
              onSelect={handleExtSelect}
              loading={extLoading}
              placeholder="输入分机号搜索"
              options={extOptions}
              notFoundContent={extLoading ? '搜索中...' : '无结果'}
            />
          </Form.Item>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="employeeName" label="使用人">
                <Input placeholder="使用人姓名" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="employeeNo" label="员工ID">
                <Input placeholder="员工ID" />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item name="macAddresses" label="MAC">
            <Input placeholder="MAC地址（可粘贴或自动填充）" />
          </Form.Item>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="branchName" label="分行">
                <Input placeholder="自动填充或粘贴" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="deptName" label="部门">
                <Input placeholder="部门（可修改）" />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item name="remark" label="备注">
            <TextArea rows={3} placeholder="备注信息" />
          </Form.Item>
        </Form>
      </Modal>

      {/* 工单详情抽屉 */}
      <Drawer title={`工单详情 - ${selectedOrder?.workOrderNo || ''}`} open={detailDrawerOpen} onClose={() => { setDetailDrawerOpen(false); setSelectedOrder(null) }} width={640}>
        {selectedOrder && (
          <>
            <Descriptions bordered column={2} size="small" style={{ marginBottom: 24 }}>
              <Descriptions.Item label="工单号">{selectedOrder.workOrderNo}</Descriptions.Item>
              <Descriptions.Item label="标题">{selectedOrder.title}</Descriptions.Item>
              <Descriptions.Item label="类型"><Tag color={TYPE_COLORS[selectedOrder.orderType]}>{TYPE_NAMES[selectedOrder.orderType] || selectedOrder.orderType}</Tag></Descriptions.Item>
              <Descriptions.Item label="状态"><Tag color={STATUS_COLORS[selectedOrder.status]}>{STATUS_NAMES[selectedOrder.status] || selectedOrder.status}</Tag></Descriptions.Item>
              <Descriptions.Item label="申请人">{selectedOrder.requesterName || '-'}</Descriptions.Item>
              <Descriptions.Item label="处理人">{selectedOrder.handlerName || '-'}</Descriptions.Item>
              <Descriptions.Item label="创建时间">{selectedOrder.createdAt}</Descriptions.Item>
              <Descriptions.Item label="描述" span={2}>{selectedOrder.description || '-'}</Descriptions.Item>
              {selectedOrder.remark && <Descriptions.Item label="备注" span={2}>{selectedOrder.remark}</Descriptions.Item>}
            </Descriptions>
            {selectedOrder.items && selectedOrder.items.length > 0 && (
              <>
                <div style={{ fontWeight: 600, marginBottom: 12, fontSize: 15 }}>工单项</div>
                {selectedOrder.items.map((item: WorkOrderItem) => (
                  <Timeline.Item key={item.id} color={item.status === 2 ? 'green' : item.status === 3 ? 'red' : 'blue'}>
                    <div style={{ paddingBottom: 8 }}>
                      <div><Tag>{ITEM_TYPE_NAMES[item.itemType] || item.itemType}</Tag><Tag color={ITEM_STATUS_COLORS[item.status] || 'default'}>{ITEM_STATUS_NAMES[item.status] || item.status}</Tag></div>
                      {item.description && <div style={{ color: '#666', marginTop: 4 }}>{item.description}</div>}
                      {item.action && !item.description && <div style={{ color: '#666', marginTop: 4 }}>操作: {item.action}</div>}
                      {item.fromValue && item.toValue && <div style={{ color: '#999', marginTop: 2 }}>{item.fromValue} → {item.toValue}</div>}
                      {item.errorMessage && <div style={{ color: '#ff4d4f', marginTop: 4 }}>错误: {item.errorMessage}</div>}
                      {(item.status === 0 || item.status === 3) && <Button size="small" type="primary" style={{ marginTop: 8 }} loading={executeItemMutation.isPending} onClick={() => handleExecuteItem(item.id)}>执行</Button>}
                    </div>
                  </Timeline.Item>
                ))}
              </>
            )}
          </>
        )}
      </Drawer>
    </div>
  )
}

export default WorkOrderManagement
