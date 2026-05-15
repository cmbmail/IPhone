import { useState } from 'react'
import { Table, Button, Modal, Form, Input, Select, Tag, message, Space } from 'antd'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { phoneApi } from '@/api/phone'
import { orgApi } from '@/api/org'
import type { PhoneNumber, CreatePhoneDTO, UpdatePhoneDTO, PhoneAllocationRequest, PhoneReclaimRequest } from '@/types/phone'

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
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false)
  const [isAllocateModalOpen, setIsAllocateModalOpen] = useState(false)
  const [isReclaimModalOpen, setIsReclaimModalOpen] = useState(false)
  const [selectedPhone, setSelectedPhone] = useState<number | null>(null)
  const [createForm] = Form.useForm()
  const [allocateForm] = Form.useForm()
  const [reclaimForm] = Form.useForm()
  const queryClient = useQueryClient()

  const { data: phonesData, isLoading } = useQuery({
    queryKey: ['phones'],
    queryFn: async () => {
      const response = await phoneApi.getAll({ page: 0, size: 100 })
      return response.data.data
    }
  })

  const { data: orgsData } = useQuery({
    queryKey: ['orgs'],
    queryFn: async () => {
      const response = await orgApi.getAll()
      return response.data
    }
  })

  const createMutation = useMutation({
    mutationFn: (data: CreatePhoneDTO) => phoneApi.create(data),
    onSuccess: () => {
      message.success('Phone created successfully')
      queryClient.invalidateQueries({ queryKey: ['phones'] })
      setIsCreateModalOpen(false)
      createForm.resetFields()
    }
  })

  const allocateMutation = useMutation({
    mutationFn: (data: PhoneAllocationRequest) => phoneApi.allocate(data),
    onSuccess: () => {
      message.success('Phone allocated successfully')
      queryClient.invalidateQueries({ queryKey: ['phones'] })
      setIsAllocateModalOpen(false)
      allocateForm.resetFields()
    }
  })

  const reclaimMutation = useMutation({
    mutationFn: (data: PhoneReclaimRequest) => phoneApi.reclaim(data),
    onSuccess: () => {
      message.success('Phone reclaimed successfully')
      queryClient.invalidateQueries({ queryKey: ['phones'] })
      setIsReclaimModalOpen(false)
      reclaimForm.resetFields()
    }
  })

  const handleCreateSubmit = (values: any) => {
    createMutation.mutate(values)
  }

  const handleAllocateSubmit = (values: any) => {
    allocateMutation.mutate(values)
  }

  const handleReclaimSubmit = (values: any) => {
    reclaimMutation.mutate(values)
  }

  const openAllocateModal = (id: number) => {
    setSelectedPhone(id)
    allocateForm.setFieldsValue({ phoneId: id })
    setIsAllocateModalOpen(true)
  }

  const openReclaimModal = (id: number) => {
    setSelectedPhone(id)
    reclaimForm.setFieldsValue({ phoneId: id })
    setIsReclaimModalOpen(true)
  }

  const columns = [
    { title: 'Phone Number', dataIndex: 'phoneNumber', key: 'phoneNumber', width: 140 },
    { title: 'User ID', dataIndex: 'userId', key: 'userId', width: 120 },
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
      width: 180,
      render: (_: any, record: PhoneNumber) => (
        <Space>
          {record.status === 'idle' && (
            <Button size="small" type="primary" onClick={() => openAllocateModal(record.id)}>
              Allocate
            </Button>
          )}
          {record.status === 'active' && (
            <Button size="small" danger onClick={() => openReclaimModal(record.id)}>
              Reclaim
            </Button>
          )}
        </Space>
      )
    }
  ]

  return (
    <div>
      <Button
        type="primary"
        onClick={() => {
          createForm.resetFields()
          setIsCreateModalOpen(true)
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
        open={isCreateModalOpen}
        onCancel={() => setIsCreateModalOpen(false)}
        onOk={() => createForm.submit()}
      >
        <Form form={createForm} onFinish={handleCreateSubmit} layout="vertical">
          <Form.Item name="phoneNumber" label="Phone Number" rules={[{ required: true }]}>
            <Input placeholder="Enter phone number" />
          </Form.Item>
          <Form.Item name="extensionNumber" label="Extension Number">
            <Input placeholder="Enter extension number" />
          </Form.Item>
          <Form.Item name="orgId" label="Organization">
            <Select placeholder="Select organization">
              {orgsData?.map((org: any) => (
                <Option key={org.id} value={org.id}>
                  {org.name}
                </Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="remark" label="Remark">
            <Input.TextArea />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="Allocate Phone"
        open={isAllocateModalOpen}
        onCancel={() => setIsAllocateModalOpen(false)}
        onOk={() => allocateForm.submit()}
      >
        <Form form={allocateForm} onFinish={handleAllocateSubmit} layout="vertical">
          <Form.Item name="phoneId" label="Phone ID" hidden>
            <Input type="number" />
          </Form.Item>
          <Form.Item name="userId" label="User ID" rules={[{ required: true }]}>
            <Input placeholder="Enter user ID" />
          </Form.Item>
          <Form.Item name="orgId" label="Organization" rules={[{ required: true }]}>
            <Select placeholder="Select organization">
              {orgsData?.map((org: any) => (
                <Option key={org.id} value={org.id}>
                  {org.name}
                </Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="extensionNumber" label="Extension Number">
            <Input placeholder="Enter extension number" />
          </Form.Item>
          <Form.Item name="workOrderNo" label="Work Order No">
            <Input placeholder="Enter work order number" />
          </Form.Item>
          <Form.Item name="remark" label="Remark">
            <Input.TextArea />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="Reclaim Phone"
        open={isReclaimModalOpen}
        onCancel={() => setIsReclaimModalOpen(false)}
        onOk={() => reclaimForm.submit()}
      >
        <Form form={reclaimForm} onFinish={handleReclaimSubmit} layout="vertical">
          <Form.Item name="phoneId" label="Phone ID" hidden>
            <Input type="number" />
          </Form.Item>
          <Form.Item name="reason" label="Reason">
            <Input.TextArea placeholder="Enter reclaim reason" />
          </Form.Item>
          <Form.Item name="workOrderNo" label="Work Order No">
            <Input placeholder="Enter work order number" />
          </Form.Item>
          <Form.Item name="remark" label="Remark">
            <Input.TextArea />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default PhoneManagement
