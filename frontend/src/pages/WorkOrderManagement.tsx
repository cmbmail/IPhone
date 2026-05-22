import { useState } from 'react'
import { Table, Button, Card, Select, Tag, Space, Modal, message, Input, Row, Col, Statistic, Drawer, Descriptions, Timeline } from 'antd'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { workOrderApi } from '@/api/workOrder'
import { PlusOutlined, CheckOutlined, EyeOutlined } from '@ant-design/icons'

const { Option } = Select
const { TextArea } = Input

const STATUS_COLORS: Record<string, string> = {
  PENDING: 'warning',
  ACCEPTED: 'processing',
  PROCESSING: 'processing',
  COMPLETED: 'success',
  REJECTED: 'error'
}

const STATUS_NAMES: Record<string, string> = {
  PENDING: '待处理',
  ACCEPTED: '已接受',
  PROCESSING: '处理中',
  COMPLETED: '已完成',
  REJECTED: '已拒绝'
}

const PRIORITY_COLORS: Record<string, string> = {
  HIGH: 'error',
  MEDIUM: 'warning',
  LOW: 'success'
}

const PRIORITY_NAMES: Record<string, string> = {
  HIGH: '高',
  MEDIUM: '中',
  LOW: '低'
}

const TYPE_NAMES: Record<string, string> = {
  PHONE_ASSIGN: '号码分配',
  PHONE_UNASSIGN: '号码回收',
  PHONE_TRANSFER: '号码转移',
  DEVICE_ASSIGN: '设备分配',
  DEVICE_CHECKIN: '设备签入',
  DEVICE_CHECKOUT: '设备签出',
  OTHER: '其他'
}

interface WorkOrderItem {
  id: number
  itemType: string
  description: string
  status: string
  result: string | null
  executedAt: string | null
  error: string | null
}

interface WorkOrder {
  id: number
  workOrderNo: string
  title: string
  description: string
  orderType: string
  priority: string
  status: string
  requesterName: string
  requesterId: number
  handlerName: string | null
  handlerId: number | null
  remark: string | null
  rejectReason: string | null
  createdAt: string
  updatedAt: string
  items?: WorkOrderItem[]
}

const WorkOrderManagement = () => {
  const [status, setStatus] = useState<string>('')
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false)
  const [detailDrawerOpen, setDetailDrawerOpen] = useState(false)
  const [selectedOrder, setSelectedOrder] = useState<WorkOrder | null>(null)
  const [formData, setFormData] = useState({ title: '', description: '', priority: 'MEDIUM', type: 'PHONE_ASSIGN' })
  const queryClient = useQueryClient()

  const { data: orderData, isLoading, refetch } = useQuery({
    queryKey: ['work-orders', status],
    queryFn: async () => {
      const params: Record<string, unknown> = { page: 0, size: 100 }
      if (status) params.status = status
      return workOrderApi.getList(params)
    }
  })

  const acceptMutation = useMutation({
    mutationFn: (id: number) => workOrderApi.accept(id, 1, 'admin'),
    onSuccess: () => {
      message.success('工单已接受')
      queryClient.invalidateQueries({ queryKey: ['work-orders'] })
    },
    onError: () => message.error('接受失败')
  })

  const completeMutation = useMutation({
    mutationFn: (id: number) => workOrderApi.complete(id),
    onSuccess: () => {
      message.success('工单已完成')
      queryClient.invalidateQueries({ queryKey: ['work-orders'] })
    },
    onError: () => message.error('完成失败')
  })

  const rejectMutation = useMutation({
    mutationFn: ({ id, reason }: { id: number; reason: string }) => workOrderApi.reject(id, reason),
    onSuccess: () => {
      message.success('工单已拒绝')
      queryClient.invalidateQueries({ queryKey: ['work-orders'] })
    },
    onError: () => message.error('拒绝失败')
  })

  const createMutation = useMutation({
    mutationFn: (data: typeof formData) => workOrderApi.create(data),
    onSuccess: () => {
      message.success('工单创建成功')
      queryClient.invalidateQueries({ queryKey: ['work-orders'] })
      setIsCreateModalOpen(false)
      setFormData({ title: '', description: '', priority: 'MEDIUM', type: 'PHONE_ASSIGN' })
    },
    onError: () => message.error('创建失败')
  })

  const executeItemMutation = useMutation({
    mutationFn: (itemId: number) => workOrderApi.executeItem(itemId),
    onSuccess: () => {
      message.success('工单项执行成功')
      queryClient.invalidateQueries({ queryKey: ['work-orders'] })
      if (selectedOrder) {
        handleViewDetail(selectedOrder)
      }
    },
    onError: () => message.error('执行失败')
  })

  const handleAccept = (record: WorkOrder) => {
    Modal.confirm({
      title: '接受工单',
      content: `接受工单 ${record.workOrderNo}？`,
      onOk: () => acceptMutation.mutate(record.id)
    })
  }

  const handleComplete = (record: WorkOrder) => {
    Modal.confirm({
      title: '完成工单',
      content: `标记工单 ${record.workOrderNo} 为已完成？`,
      onOk: () => completeMutation.mutate(record.id)
    })
  }

  const handleReject = (record: WorkOrder) => {
    let rejectReason = ''
    Modal.confirm({
      title: '拒绝工单',
      content: (
        <div>
          <p>拒绝工单 {record.workOrderNo}</p>
          <TextArea
            rows={3}
            placeholder="请输入拒绝原因"
            onChange={(e) => { rejectReason = e.target.value }}
          />
        </div>
      ),
      onOk: () => {
        if (rejectReason) rejectMutation.mutate({ id: record.id, reason: rejectReason })
        else message.warning('请输入拒绝原因')
      }
    })
  }

  const handleViewDetail = (record: WorkOrder) => {
    workOrderApi.getById(record.id).then(res => {
      setSelectedOrder(res.data?.data || record)
      setDetailDrawerOpen(true)
    }).catch(() => {
      setSelectedOrder(record)
      setDetailDrawerOpen(true)
    })
  }

  const handleExecuteItem = (itemId: number) => {
    Modal.confirm({
      title: '执行工单项',
      content: '确认要执行此工单项？',
      onOk: () => executeItemMutation.mutate(itemId)
    })
  }

  const columns = [
    { title: '工单号', dataIndex: 'workOrderNo', key: 'workOrderNo', width: 150 },
    { title: '标题', dataIndex: 'title', key: 'title', width: 200 },
    {
      title: '类型',
      dataIndex: 'orderType',
      key: 'type',
      width: 120,
      render: (type: string) => TYPE_NAMES[type] || type
    },
    {
      title: '优先级',
      dataIndex: 'priority',
      key: 'priority',
      width: 100,
      render: (priority: string) => <Tag color={PRIORITY_COLORS[priority] || 'default'}>{PRIORITY_NAMES[priority]}</Tag>
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 120,
      render: (status: string) => <Tag color={STATUS_COLORS[status] || 'default'}>{STATUS_NAMES[status]}</Tag>
    },
    { title: '申请人', dataIndex: 'requesterName', key: 'requesterName', width: 120 },
    { title: '处理人', dataIndex: 'handlerName', key: 'handlerName', width: 120 },
    { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 150 },
    {
      title: '操作',
      key: 'actions',
      width: 280,
      render: (_: unknown, record: WorkOrder) => (
        <Space>
          <Button size="small" icon={<EyeOutlined />} onClick={() => handleViewDetail(record)}>
            详情
          </Button>
          {record.status === 'PENDING' && (
            <Button size="small" type="primary" icon={<CheckOutlined />} onClick={() => handleAccept(record)}>
              接受
            </Button>
          )}
          {(record.status === 'ACCEPTED' || record.status === 'PROCESSING') && (
            <Button size="small" type="primary" onClick={() => handleComplete(record)}>
              完成
            </Button>
          )}
          {record.status !== 'COMPLETED' && record.status !== 'REJECTED' && (
            <Button size="small" danger onClick={() => handleReject(record)}>
              拒绝
            </Button>
          )}
        </Space>
      )
    }
  ]

  const orders: WorkOrder[] = orderData?.data?.data?.content || []
  const pendingCount = orders.filter((o) => o.status === 'PENDING').length
  const processingCount = orders.filter((o) => o.status === 'ACCEPTED' || o.status === 'PROCESSING').length
  const completedCount = orders.filter((o) => o.status === 'COMPLETED').length

  return (
    <div>
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <Card><Statistic title="工单总数" value={orders.length} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="待处理" value={pendingCount} valueStyle={{ color: '#faad14' }} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="处理中" value={processingCount} valueStyle={{ color: '#1890ff' }} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="已完成" value={completedCount} valueStyle={{ color: '#3f8600' }} /></Card>
        </Col>
      </Row>

      <Card>
        <Space style={{ marginBottom: 16 }}>
          <Select value={status} onChange={setStatus} style={{ width: 150 }} allowClear placeholder="选择状态">
            <Option value="PENDING">待处理</Option>
            <Option value="ACCEPTED">已接受</Option>
            <Option value="PROCESSING">处理中</Option>
            <Option value="COMPLETED">已完成</Option>
            <Option value="REJECTED">已拒绝</Option>
          </Select>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setIsCreateModalOpen(true)}>
            创建工单
          </Button>
          <Button onClick={() => refetch()}>刷新</Button>
        </Space>

        <Table
          columns={columns}
          dataSource={orders}
          loading={isLoading}
          rowKey="id"
          pagination={{ pageSize: 20, total: orderData?.data?.data?.totalElements }}
        />
      </Card>

      <Drawer
        title={`工单详情 - ${selectedOrder?.workOrderNo || ''}`}
        open={detailDrawerOpen}
        onClose={() => { setDetailDrawerOpen(false); setSelectedOrder(null) }}
        width={640}
      >
        {selectedOrder && (
          <>
            <Descriptions bordered column={2} size="small" style={{ marginBottom: 24 }}>
              <Descriptions.Item label="工单号">{selectedOrder.workOrderNo}</Descriptions.Item>
              <Descriptions.Item label="标题">{selectedOrder.title}</Descriptions.Item>
              <Descriptions.Item label="类型">{TYPE_NAMES[selectedOrder.orderType] || selectedOrder.orderType}</Descriptions.Item>
              <Descriptions.Item label="优先级"><Tag color={PRIORITY_COLORS[selectedOrder.priority]}>{PRIORITY_NAMES[selectedOrder.priority]}</Tag></Descriptions.Item>
              <Descriptions.Item label="状态"><Tag color={STATUS_COLORS[selectedOrder.status]}>{STATUS_NAMES[selectedOrder.status]}</Tag></Descriptions.Item>
              <Descriptions.Item label="申请人">{selectedOrder.requesterName}</Descriptions.Item>
              <Descriptions.Item label="处理人">{selectedOrder.handlerName || '-'}</Descriptions.Item>
              <Descriptions.Item label="创建时间">{selectedOrder.createdAt}</Descriptions.Item>
              <Descriptions.Item label="描述" span={2}>{selectedOrder.description || '-'}</Descriptions.Item>
              {selectedOrder.rejectReason && (
                <Descriptions.Item label="拒绝原因" span={2}><span style={{ color: '#ff4d4f' }}>{selectedOrder.rejectReason}</span></Descriptions.Item>
              )}
              {selectedOrder.remark && (
                <Descriptions.Item label="备注" span={2}>{selectedOrder.remark}</Descriptions.Item>
              )}
            </Descriptions>

            {selectedOrder.items && selectedOrder.items.length > 0 && (
              <>
                <div style={{ fontWeight: 600, marginBottom: 12, fontSize: 15 }}>工单项</div>
                {selectedOrder.items.map((item) => (
                  <Timeline.Item
                    key={item.id}
                    color={item.status === 'COMPLETED' ? 'green' : item.status === 'FAILED' ? 'red' : 'blue'}
                  >
                    <div style={{ paddingBottom: 8 }}>
                      <div>
                        <Tag>{TYPE_NAMES[item.itemType] || item.itemType}</Tag>
                        <Tag color={item.status === 'COMPLETED' ? 'success' : item.status === 'FAILED' ? 'error' : 'processing'}>
                          {item.status === 'COMPLETED' ? '已完成' : item.status === 'FAILED' ? '失败' : '待执行'}
                        </Tag>
                      </div>
                      <div style={{ color: '#666', marginTop: 4 }}>{item.description}</div>
                      {item.result && <div style={{ color: '#52c41a', marginTop: 4 }}>结果: {item.result}</div>}
                      {item.error && <div style={{ color: '#ff4d4f', marginTop: 4 }}>错误: {item.error}</div>}
                      {item.executedAt && <div style={{ color: '#999', fontSize: 12, marginTop: 4 }}>执行时间: {item.executedAt}</div>}
                      {(item.status === 'PENDING' || item.status === 'FAILED') && (
                        <Button
                          size="small"
                          type="primary"
                          style={{ marginTop: 8 }}
                          loading={executeItemMutation.isPending}
                          onClick={() => handleExecuteItem(item.id)}
                        >
                          执行
                        </Button>
                      )}
                    </div>
                  </Timeline.Item>
                ))}
              </>
            )}
          </>
        )}
      </Drawer>

      <Modal
        title="创建工单"
        open={isCreateModalOpen}
        onCancel={() => setIsCreateModalOpen(false)}
        footer={[
          <Button key="back" onClick={() => setIsCreateModalOpen(false)}>取消</Button>,
          <Button key="submit" type="primary" onClick={() => createMutation.mutate(formData)}>
            创建
          </Button>
        ]}
      >
        <Space direction="vertical" style={{ width: '100%' }}>
          <Input
            value={formData.title}
            onChange={(e) => setFormData({ ...formData, title: e.target.value })}
            placeholder="输入标题"
          />
          <TextArea
            value={formData.description}
            onChange={(e) => setFormData({ ...formData, description: e.target.value })}
            placeholder="输入描述"
            rows={4}
          />
          <Select
            value={formData.priority}
            onChange={(value) => setFormData({ ...formData, priority: value })}
            style={{ width: '100%' }}
          >
            <Option value="LOW">低</Option>
            <Option value="MEDIUM">中</Option>
            <Option value="HIGH">高</Option>
          </Select>
          <Select
            value={formData.type}
            onChange={(value) => setFormData({ ...formData, type: value })}
            style={{ width: '100%' }}
          >
            <Option value="PHONE_ASSIGN">号码分配</Option>
            <Option value="PHONE_UNASSIGN">号码回收</Option>
            <Option value="PHONE_TRANSFER">号码转移</Option>
            <Option value="DEVICE_ASSIGN">设备分配</Option>
          </Select>
        </Space>
      </Modal>
    </div>
  )
}

export default WorkOrderManagement
