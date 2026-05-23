import { useState } from 'react'
import {
  Table,
  Button,
  Card,
  Select,
  Tag,
  Space,
  Modal,
  Form,
  Input,
  message,
  Drawer,
  Typography,
} from 'antd'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { announcementApi, Announcement } from '@/api/announcement'
import { PlusOutlined, EyeOutlined, EditOutlined } from '@ant-design/icons'

const { Option } = Select
const { TextArea } = Input
const { Paragraph } = Typography

const TYPE_NAMES: Record<string, string> = {
  1: '系统公告',
  2: '维护通知',
  3: '政策变更',
  4: '运营通知',
  5: '其他',
}

const TYPE_COLORS: Record<string, string> = {
  1: 'blue',
  2: 'orange',
  3: 'purple',
  4: 'cyan',
  5: 'default',
}

const PRIORITY_NAMES: Record<string, string> = {
  1: '低',
  2: '普通',
  3: '高',
  4: '紧急',
}

const PRIORITY_COLORS: Record<string, string> = {
  1: '#8c8c8c',
  2: '#1677ff',
  3: '#fa8c16',
  4: '#ff4d4f',
}

const STATUS_NAMES: Record<string, string> = {
  0: '草稿',
  1: '已发布',
  2: '已归档',
}

const STATUS_COLORS: Record<string, string> = {
  0: 'default',
  1: 'success',
  2: 'warning',
}

const AnnouncementManagement = () => {
  const [statusFilter, setStatusFilter] = useState<number | ''>('')
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editRecord, setEditRecord] = useState<Announcement | null>(null)
  const [detailOpen, setDetailOpen] = useState(false)
  const [selectedRecord, setSelectedRecord] = useState<Announcement | null>(null)
  const [form] = Form.useForm()
  const queryClient = useQueryClient()

  const { data: announcementData, isLoading } = useQuery({
    queryKey: ['announcements', statusFilter],
    queryFn: async () => {
      const params: Record<string, unknown> = { page: 0, size: 50 }
      if (statusFilter) params.status = statusFilter
      return announcementApi.getAll(params)
    },
  })

  const createMutation = useMutation({
    mutationFn: (data: Record<string, unknown>) => announcementApi.create(data),
    onSuccess: () => {
      message.success('公告创建成功')
      queryClient.invalidateQueries({ queryKey: ['announcements'] })
      closeModal()
    },
    onError: () => message.error('创建失败'),
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: Record<string, unknown> }) =>
      announcementApi.update(id, data),
    onSuccess: () => {
      message.success('公告更新成功')
      queryClient.invalidateQueries({ queryKey: ['announcements'] })
      closeModal()
    },
    onError: () => message.error('更新失败'),
  })

  const deleteMutation = useMutation({
    mutationFn: (id: number) => announcementApi.delete(id),
    onSuccess: () => {
      message.success('公告删除成功')
      queryClient.invalidateQueries({ queryKey: ['announcements'] })
    },
    onError: () => message.error('删除失败'),
  })

  const publishMutation = useMutation({
    mutationFn: (id: number) => announcementApi.publish(id),
    onSuccess: () => {
      message.success('公告已发布')
      queryClient.invalidateQueries({ queryKey: ['announcements'] })
    },
    onError: () => message.error('发布失败'),
  })

  const archiveMutation = useMutation({
    mutationFn: (id: number) => announcementApi.archive(id),
    onSuccess: () => {
      message.success('公告已归档')
      queryClient.invalidateQueries({ queryKey: ['announcements'] })
    },
    onError: () => message.error('归档失败'),
  })

  const closeModal = () => {
    setIsModalOpen(false)
    setEditRecord(null)
    form.resetFields()
  }

  const handleSubmit = (values: Record<string, unknown>) => {
    if (editRecord) {
      updateMutation.mutate({ id: editRecord.id, data: values })
    } else {
      createMutation.mutate(values)
    }
  }

  const handleEdit = (record: Announcement) => {
    setEditRecord(record)
    form.setFieldsValue({
      title: record.title,
      content: record.content,
      announcementType: record.announcementType,
      priority: record.priority,
      status: record.status,
    })
    setIsModalOpen(true)
  }

  const handleViewDetail = (record: Announcement) => {
    setSelectedRecord(record)
    setDetailOpen(true)
  }

  const columns = [
    {
      title: '标题',
      dataIndex: 'title',
      key: 'title',
      width: 250,
      render: (title: string, record: Announcement) => (
        <Button
          type="link"
          style={{ padding: 0, height: 'auto' }}
          onClick={() => handleViewDetail(record)}
        >
          {title}
        </Button>
      ),
    },
    {
      title: '类型',
      dataIndex: 'announcementType',
      key: 'type',
      width: 120,
      render: (type: string) => (
        <Tag color={TYPE_COLORS[type] || 'default'}>{TYPE_NAMES[type] || type}</Tag>
      ),
    },
    {
      title: '优先级',
      dataIndex: 'priority',
      key: 'priority',
      width: 100,
      render: (p: string) => (
        <Tag color={PRIORITY_COLORS[p] || 'default'}>{PRIORITY_NAMES[p] || p}</Tag>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (s: string) => (
        <Tag color={STATUS_COLORS[s] || 'default'}>{STATUS_NAMES[s] || s}</Tag>
      ),
    },
    { title: '发布人', dataIndex: 'createdBy', key: 'createdBy', width: 100 },
    { title: '发布时间', dataIndex: 'createdAt', key: 'createdAt', width: 160 },
    {
      title: '操作',
      key: 'actions',
      width: 280,
      render: (_: unknown, record: Announcement) => (
        <Space size="small">
          <Button size="small" icon={<EyeOutlined />} onClick={() => handleViewDetail(record)}>
            查看
          </Button>
          <Button size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)}>
            编辑
          </Button>
          {record.status === 0 && (
            <Button size="small" type="primary" onClick={() => publishMutation.mutate(record.id)}>
              发布
            </Button>
          )}
          {record.status === 1 && (
            <Button size="small" onClick={() => archiveMutation.mutate(record.id)}>
              归档
            </Button>
          )}
          <Button
            size="small"
            danger
            onClick={() => {
              Modal.confirm({
                title: '确认删除',
                content: `删除公告「${record.title}」？`,
                onOk: () => deleteMutation.mutate(record.id),
              })
            }}
          >
            删除
          </Button>
        </Space>
      ),
    },
  ]

  const announcements = announcementData?.content || []

  return (
    <div>
      <Card>
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: 16,
          }}
        >
          <Select
            value={statusFilter}
            onChange={setStatusFilter}
            style={{ width: 150 }}
            allowClear
            placeholder="状态筛选"
          >
            <Option value={1}>已发布</Option>
            <Option value={0}>草稿</Option>
            <Option value={2}>已归档</Option>
          </Select>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => {
              setEditRecord(null)
              form.resetFields()
              setIsModalOpen(true)
            }}
          >
            发布公告
          </Button>
        </div>

        <Table
          columns={columns}
          dataSource={announcements}
          loading={isLoading}
          rowKey="id"
          pagination={{ pageSize: 20, total: announcementData?.totalElements }}
        />
      </Card>

      <Drawer
        title={selectedRecord?.title}
        open={detailOpen}
        onClose={() => {
          setDetailOpen(false)
          setSelectedRecord(null)
        }}
        width={640}
      >
        {selectedRecord && (
          <div>
            <Space style={{ marginBottom: 16 }}>
              <Tag color={TYPE_COLORS[selectedRecord.announcementType]}>
                {TYPE_NAMES[selectedRecord.announcementType]}
              </Tag>
              <Tag color={PRIORITY_COLORS[selectedRecord.priority]}>
                {PRIORITY_NAMES[selectedRecord.priority]}
              </Tag>
              <Tag color={STATUS_COLORS[selectedRecord.status]}>
                {STATUS_NAMES[selectedRecord.status]}
              </Tag>
            </Space>
            <div style={{ color: '#999', marginBottom: 16, fontSize: 13 }}>
              发布人: {selectedRecord.createdBy || '-'} | 发布时间: {selectedRecord.createdAt}
            </div>
            <div style={{ borderTop: '1px solid #f0f0f0', paddingTop: 16 }}>
              <Paragraph style={{ whiteSpace: 'pre-wrap', fontSize: 14, lineHeight: 1.8 }}>
                {selectedRecord.content || '无内容'}
              </Paragraph>
            </div>
          </div>
        )}
      </Drawer>

      <Modal
        title={editRecord ? '编辑公告' : '发布公告'}
        open={isModalOpen}
        onCancel={closeModal}
        onOk={() => form.submit()}
        width={640}
      >
        <Form
          form={form}
          onFinish={handleSubmit}
          layout="vertical"
          style={{ marginTop: 16 }}
          initialValues={{ announcementType: 1, priority: 2, status: 0 }}
        >
          <Form.Item name="title" label="标题" rules={[{ required: true, message: '请输入标题' }]}>
            <Input placeholder="输入公告标题" />
          </Form.Item>
          <Form.Item name="announcementType" label="类型" rules={[{ required: true }]}>
            <Select>
              <Option value={1}>系统公告</Option>
              <Option value={2}>维护通知</Option>
              <Option value={3}>政策变更</Option>
              <Option value={4}>运营通知</Option>
              <Option value={5}>其他</Option>
            </Select>
          </Form.Item>
          <Form.Item name="priority" label="优先级" rules={[{ required: true }]}>
            <Select>
              <Option value={1}>紧急</Option>
              <Option value={2}>重要</Option>
              <Option value={3}>普通</Option>
              <Option value={4}>低</Option>
            </Select>
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select>
              <Option value={0}>草稿</Option>
              <Option value={1}>直接发布</Option>
            </Select>
          </Form.Item>
          <Form.Item
            name="content"
            label="内容"
            rules={[{ required: true, message: '请输入内容' }]}
          >
            <TextArea rows={8} placeholder="输入公告内容" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default AnnouncementManagement
