import { useState } from 'react'
import { Table, Button, Modal, Form, Input, Select, Tag, message, Drawer } from 'antd'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { phoneApi } from '@/api/phone'
import type { PhoneNumber, CreatePhoneDTO, PhoneHistory } from '@/types/phone'

const { Option } = Select

const STATUS_COLORS: Record<string, string> = {
  idle: 'default',
  active: 'success',
  stopped: 'warning',
  cancelled: 'error',
  reserved: 'processing',
  disabled: 'error'
}

const STATUS_OPTIONS = ['idle', 'active', 'stopped', 'cancelled', 'reserved', 'disabled']

const PhoneManagement = () => {
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [historyDrawerOpen, setHistoryDrawerOpen] = useState(false)
  const [selectedPhoneId, setSelectedPhoneId] = useState<number | null>(null)
  const [form] = Form.useForm()
  const queryClient = useQueryClient()

  const { data: phonesData, isLoading } = useQuery({
    queryKey: ['phones'],
    queryFn: async () => {
      const response = await phoneApi.getAll({ page: 0, size: 100 })
      return response.data.data
    }
  })

  const { data: historyData } = useQuery({
    queryKey: ['phoneHistory', selectedPhoneId],
    queryFn: async () => {
      if (!selectedPhoneId) return null
      const response = await phoneApi.getHistory(selectedPhoneId, { page: 0, size: 50 })
      return response.data.data
    },
    enabled: !!selectedPhoneId
  })

  const createMutation = useMutation({
    mutationFn: (data: CreatePhoneDTO) => phoneApi.create(data),
    onSuccess: () => {
      message.success('Phone created successfully')
      queryClient.invalidateQueries({ queryKey: ['phones'] })
      setIsModalOpen(false)
      form.resetFields()
    }
  })

  const handleSubmit = (values: any) => {
    createMutation.mutate(values as CreatePhoneDTO)
  }

  const showHistory = (phoneId: number) => {
    setSelectedPhoneId(phoneId)
    setHistoryDrawerOpen(true)
  }

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
    { title: 'Phone Number', dataIndex: 'phoneNumber', key: 'phoneNumber', width: 130 },
    { title: 'User ID', dataIndex: 'userId', key: 'userId', width: 100 },
    { title: 'Extension', dataIndex: 'extensionNumber', key: 'extensionNumber', width: 100 },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string) => (
        <Tag color={STATUS_COLORS[status] || 'default'}>{status}</Tag>
      )
    },
    { title: 'Remark', dataIndex: 'remark', key: 'remark', ellipsis: true },
    {
      title: 'Actions',
      key: 'actions',
      width: 120,
      render: (_: any, record: PhoneNumber) => (
        <>
          <Button size="small" onClick={() => showHistory(record.id)} style={{ marginRight: 8 }}>
            History
          </Button>
        </>
      )
    }
  ]

  const historyColumns = [
    { title: 'Action', dataIndex: 'action', key: 'action', width: 120 },
    { title: 'From Status', dataIndex: 'fromStatus', key: 'fromStatus', width: 100 },
    { title: 'To Status', dataIndex: 'toStatus', key: 'toStatus', width: 100 },
    { title: 'Operator', dataIndex: 'operator', key: 'operator', width: 100 },
    { title: 'Time', dataIndex: 'operatedAt', key: 'operatedAt', width: 170 },
    { title: 'Remark', dataIndex: 'remark', key: 'remark', ellipsis: true }
  ]

  return (
    <div>
      <Button
        type="primary"
        onClick={() => {
          form.resetFields()
          setIsModalOpen(true)
        }}
        style={{ marginBottom: 16 }}
      >
        Add Phone
      </Button>

      <Table
        columns={columns}
        dataSource={phonesData?.content}
        loading={isLoading}
        rowKey="id"
        pagination={{
          total: phonesData?.totalElements,
          pageSize: 20
        }}
      />

      <Modal
        title="Add Phone"
        open={isModalOpen}
        onCancel={() => setIsModalOpen(false)}
        onOk={() => form.submit()}
      >
        <Form form={form} onFinish={handleSubmit} layout="vertical">
          <Form.Item name="phoneNumber" label="Phone Number" rules={[{ required: true }]}>
            <Input placeholder="11 digit phone number" />
          </Form.Item>

          <Form.Item name="userId" label="User ID">
            <Input />
          </Form.Item>

          <Form.Item name="extensionNumber" label="Extension Number">
            <Input />
          </Form.Item>

          <Form.Item name="extensionType" label="Extension Type">
            <Select>
              <Option value="auto">Auto</Option>
              <Option value="manual">Manual</Option>
            </Select>
          </Form.Item>

          <Form.Item name="remark" label="Remark">
            <Input.TextArea />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        title="Phone History"
        placement="right"
        width={600}
        onClose={() => setHistoryDrawerOpen(false)}
        open={historyDrawerOpen}
      >
        <Table
          columns={historyColumns}
          dataSource={historyData?.content}
          rowKey="id"
          pagination={false}
        />
      </Drawer>
    </div>
  )
}

export default PhoneManagement
