import { useState } from 'react'
import { Table, Button, Card, Select, Tag, Space, Modal, message, Input, Row, Col, Statistic } from 'antd'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { workOrderApi, WorkOrder } from '@/api/workOrder'
import { PlusOutlined, CheckOutlined, XOutlined, EditOutlined } from '@ant-design/icons'

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

const WorkOrderManagement = () => {
  const [status, setStatus] = useState<string>('')
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false)
  const [formData, setFormData] = useState({ title: '', description: '', priority: 'MEDIUM', type: 'PHONE_ASSIGN' })
  const queryClient = useQueryClient()

  const { data: orderData, isLoading, refetch } = useQuery({
    queryKey: ['work-orders', status],
    queryFn: async () => {
      const params: any = { page: 0, size: 100 }
      if (status) params.status = status
      return workOrderApi.getWorkOrders(params)
    }
  })

  const acceptMutation = useMutation({
    mutationFn: (id: number) => workOrderApi.acceptWorkOrder(id, 1, 'admin'),
    onSuccess: () => {
      message.success('工单已接受')
      queryClient.invalidateQueries({ queryKey: ['work-orders'] })
    },
    onError: () => message.error('接受失败')
  })

  const completeMutation = useMutation({
    mutationFn: (id: number) => workOrderApi.completeWorkOrder(id),
    onSuccess: () => {
      message.success('工单已完成')
      queryClient.invalidateQueries({ queryKey: ['work-orders'] })
    },
    onError: () => message.error('完成失败')
  })

  const rejectMutation = useMutation({
    mutationFn: ({ id, reason }: { id: number; reason: string }) => workOrderApi.rejectWorkOrder(id, reason),
    onSuccess: () => {
      message.success('工单已拒绝')
      queryClient.invalidateQueries({ queryKey: ['work-orders'] })
    },
    onError: () => message.error('拒绝失败')
  })

  const createMutation = useMutation({
    mutationFn: (data: typeof formData) => workOrderApi.createWorkOrder(data),
    onSuccess: () => {
      message.success('工单创建成功')
      queryClient.invalidateQueries({ queryKey: ['work-orders'] })
      setIsCreateModalOpen(false)
      setFormData({ title: '', description: '', priority: 'MEDIUM', type: 'PHONE_ASSIGN' })
    },
    onError: () => message.error('创建失败')
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
    Modal.prompt({
      title: '拒绝工单',
      placeholder: '输入拒绝原因',
      okText: '拒绝',
      cancelText: '取消',
      onOk: (reason) => {
        if (reason) rejectMutation.mutate({ id: record.id, reason })
      }
    })
  }

  const columns = [
    { title: '工单号', dataIndex: 'workOrderNo', key: 'workOrderNo', width: 150 },
    { title: '标题', dataIndex: 'title', key: 'title', width: 200 },
    { title: '类型', dataIndex: 'type', key: 'type', width: 120 },
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
      width: 200,
      render: (_: any, record: WorkOrder) => (
        <Space>
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
            <Button size="small" danger icon={<XOutlined />} onClick={() => handleReject(record)}>
              拒绝
            </Button>
          )}
        </Space>
      )
    }
  ]

  const orders = orderData?.data?.data?.content || []
  const pendingCount = orders.filter((o: WorkOrder) => o.status === 'PENDING').length
  const processingCount = orders.filter((o: WorkOrder) => o.status === 'ACCEPTED' || o.status === 'PROCESSING').length
  const completedCount = orders.filter((o: WorkOrder) => o.status === 'COMPLETED').length

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
          pagination={{ pageSize: 20, total: orderData?.data?.data?.total_elements }}
        />
      </Card>

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
            label="标题"
            value={formData.title}
            onChange={(e) => setFormData({ ...formData, title: e.target.value })}
            placeholder="输入标题"
          />
          <TextArea
            label="描述"
            value={formData.description}
            onChange={(e) => setFormData({ ...formData, description: e.target.value })}
            placeholder="输入描述"
            rows={4}
          />
          <Select
            label="优先级"
            value={formData.priority}
            onChange={(value) => setFormData({ ...formData, priority: value })}
          >
            <Option value="LOW">低</Option>
            <Option value="MEDIUM">中</Option>
            <Option value="HIGH">高</Option>
          </Select>
          <Select
            label="类型"
            value={formData.type}
            onChange={(value) => setFormData({ ...formData, type: value })}
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
