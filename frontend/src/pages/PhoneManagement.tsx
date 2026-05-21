import { useState } from 'react'
import { Table, Button, Modal, Form, Input, Select, Tag, message, Space, Dropdown } from 'antd'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { phoneApi } from '@/api/phone'
import { orgApi } from '@/api/org'
import type { PhoneNumber, CreatePhoneDTO, PhoneAllocationRequest, PhoneReclaimRequest, PhoneStatusChangeRequest, PhoneSurrenderRequest, PhoneReserveRequest } from '@/types/phone'

const { Option } = Select

const STATUS_COLORS: Record<string, string> = {
  idle: 'default',
  active: 'success',
  stopped: 'warning',
  cancelled: 'error',
  reserved: 'processing',
  disabled: 'error'
}

const STATUS_NAMES: Record<string, string> = {
  idle: '空闲',
  active: '使用中',
  stopped: '停用',
  cancelled: '已取消',
  reserved: '已预留',
  disabled: '已禁用'
}

const STATUS_OPTIONS = ['idle', 'active', 'stopped', 'cancelled', 'reserved', 'disabled']

const PhoneManagement = () => {
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false)
  const [isAllocateModalOpen, setIsAllocateModalOpen] = useState(false)
  const [isReclaimModalOpen, setIsReclaimModalOpen] = useState(false)
  const [isStatusModalOpen, setIsStatusModalOpen] = useState(false)
  const [isSurrenderModalOpen, setIsSurrenderModalOpen] = useState(false)
  const [selectedPhone, setSelectedPhone] = useState<number | null>(null)
  const [selectedNewStatus, setSelectedNewStatus] = useState<string>('')
  const [createForm] = Form.useForm()
  const [allocateForm] = Form.useForm()
  const [reclaimForm] = Form.useForm()
  const [statusForm] = Form.useForm()
  const [surrenderForm] = Form.useForm()
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
      return response.data.data
    }
  })

  const createMutation = useMutation({
    mutationFn: (data: CreatePhoneDTO) => phoneApi.create(data),
    onSuccess: () => {
      message.success('电话创建成功')
      queryClient.invalidateQueries({ queryKey: ['phones'] })
      setIsCreateModalOpen(false)
      createForm.resetFields()
    }
  })

  const allocateMutation = useMutation({
    mutationFn: (data: PhoneAllocationRequest) => phoneApi.allocate(data),
    onSuccess: () => {
      message.success('电话分配成功')
      queryClient.invalidateQueries({ queryKey: ['phones'] })
      setIsAllocateModalOpen(false)
      allocateForm.resetFields()
    }
  })

  const reclaimMutation = useMutation({
    mutationFn: (data: PhoneReclaimRequest) => phoneApi.reclaim(data),
    onSuccess: () => {
      message.success('电话回收成功')
      queryClient.invalidateQueries({ queryKey: ['phones'] })
      setIsReclaimModalOpen(false)
      reclaimForm.resetFields()
    }
  })

  const statusMutation = useMutation({
    mutationFn: (data: PhoneStatusChangeRequest) => phoneApi.changeStatus(data),
    onSuccess: () => {
      message.success('状态变更成功')
      queryClient.invalidateQueries({ queryKey: ['phones'] })
      setIsStatusModalOpen(false)
      statusForm.resetFields()
    }
  })

  const surrenderMutation = useMutation({
    mutationFn: (data: PhoneSurrenderRequest) => phoneApi.surrender(data),
    onSuccess: () => {
      message.success('电话销户成功')
      queryClient.invalidateQueries({ queryKey: ['phones'] })
      setIsSurrenderModalOpen(false)
      surrenderForm.resetFields()
    }
  })

  const reserveMutation = useMutation({
    mutationFn: (data: PhoneReserveRequest) => phoneApi.reserve(data),
    onSuccess: () => {
      message.success('电话预留成功')
      queryClient.invalidateQueries({ queryKey: ['phones'] })
    }
  })

  const releaseMutation = useMutation({
    mutationFn: (data: PhoneReserveRequest) => phoneApi.release(data),
    onSuccess: () => {
      message.success('电话释放成功')
      queryClient.invalidateQueries({ queryKey: ['phones'] })
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

  const handleStatusSubmit = (values: any) => {
    statusMutation.mutate({ phoneId: selectedPhone, newStatus: selectedNewStatus, ...values })
  }

  const handleSurrenderSubmit = (values: any) => {
    surrenderMutation.mutate(values)
  }

  const handleReserve = (id: number) => {
    reserveMutation.mutate({ phoneId: id })
  }

  const handleRelease = (id: number) => {
    releaseMutation.mutate({ phoneId: id })
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

  const openStatusModal = (id: number, currentStatus: string) => {
    setSelectedPhone(id)
    statusForm.setFieldsValue({ phoneId: id, currentStatus })
    setIsStatusModalOpen(true)
  }

  const openSurrenderModal = (id: number) => {
    setSelectedPhone(id)
    surrenderForm.setFieldsValue({ phoneId: id })
    setIsSurrenderModalOpen(true)
  }

  const getStatusMenuItems = (record: PhoneNumber) => {
    return STATUS_OPTIONS.filter(s => s !== record.status).map(status => ({
      key: status,
      label: `改为 ${STATUS_NAMES[status]}`,
      onClick: () => {
        setSelectedNewStatus(status)
        openStatusModal(record.id, record.status)
      }
    }))
  }

  const columns = [
    { title: '电话号码', dataIndex: 'phoneNumber', key: 'phoneNumber', width: 140 },
    { title: '用户ID', dataIndex: 'userId', key: 'userId', width: 120 },
    { title: '分机号', dataIndex: 'extensionNumber', key: 'extensionNumber', width: 100 },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string) => (
        <Tag color={STATUS_COLORS[status] || 'default'}>
          {STATUS_NAMES[status]}
        </Tag>
      )
    },
    { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
    {
      title: '操作',
      key: 'actions',
      width: 280,
      render: (_: any, record: PhoneNumber) => (
        <Space>
          {record.status === 'idle' && (
            <>
              <Button size="small" type="primary" onClick={() => openAllocateModal(record.id)}>
                分配
              </Button>
              <Button size="small" onClick={() => handleReserve(record.id)}>
                预留
              </Button>
            </>
          )}
          {record.status === 'active' && (
            <>
              <Button size="small" danger onClick={() => openReclaimModal(record.id)}>
                回收
              </Button>
              <Button size="small" onClick={() => openSurrenderModal(record.id)}>
                销户
              </Button>
            </>
          )}
          {record.status === 'reserved' && (
            <>
              <Button size="small" type="primary" onClick={() => openAllocateModal(record.id)}>
                分配
              </Button>
              <Button size="small" onClick={() => handleRelease(record.id)}>
                释放
              </Button>
            </>
          )}
          {record.status === 'stopped' && (
            <Button size="small" onClick={() => openSurrenderModal(record.id)}>
              销户
            </Button>
          )}
          <Dropdown menu={{ items: getStatusMenuItems(record) }}>
            <Button size="small">变更状态</Button>
          </Dropdown>
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
        添加电话
      </Button>

      <Table
        columns={columns}
        dataSource={phonesData?.content}
        loading={isLoading}
        rowKey="id"
        pagination={{
          total: phonesData?.total_elements,
          pageSize: 20
        }}
      />

      <Modal
        title="添加电话"
        open={isCreateModalOpen}
        onCancel={() => setIsCreateModalOpen(false)}
        onOk={() => createForm.submit()}
      >
        <Form form={createForm} onFinish={handleCreateSubmit} layout="vertical">
          <Form.Item name="phoneNumber" label="电话号码" rules={[{ required: true }]}>
            <Input placeholder="输入电话号码" />
          </Form.Item>
          <Form.Item name="extensionNumber" label="分机号码">
            <Input placeholder="输入分机号" />
          </Form.Item>
          <Form.Item name="orgId" label="组织">
            <Select placeholder="选择组织">
              {orgsData?.map((org: any) => (
                <Option key={org.id} value={org.id}>
                  {org.name}
                </Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input.TextArea />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="分配电话"
        open={isAllocateModalOpen}
        onCancel={() => setIsAllocateModalOpen(false)}
        onOk={() => allocateForm.submit()}
      >
        <Form form={allocateForm} onFinish={handleAllocateSubmit} layout="vertical">
          <Form.Item name="phoneId" label="电话ID" hidden>
            <Input type="number" />
          </Form.Item>
          <Form.Item name="userId" label="用户ID" rules={[{ required: true }]}>
            <Input placeholder="输入用户ID" />
          </Form.Item>
          <Form.Item name="orgId" label="组织" rules={[{ required: true }]}>
            <Select placeholder="选择组织">
              {orgsData?.map((org: any) => (
                <Option key={org.id} value={org.id}>
                  {org.name}
                </Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="extensionNumber" label="分机号码">
            <Input placeholder="输入分机号" />
          </Form.Item>
          <Form.Item name="workOrderNo" label="工单号">
            <Input placeholder="输入工单号" />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input.TextArea />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="回收电话"
        open={isReclaimModalOpen}
        onCancel={() => setIsReclaimModalOpen(false)}
        onOk={() => reclaimForm.submit()}
      >
        <Form form={reclaimForm} onFinish={handleReclaimSubmit} layout="vertical">
          <Form.Item name="phoneId" label="电话ID" hidden>
            <Input type="number" />
          </Form.Item>
          <Form.Item name="reason" label="原因">
            <Input.TextArea placeholder="输入回收原因" />
          </Form.Item>
          <Form.Item name="workOrderNo" label="工单号">
            <Input placeholder="输入工单号" />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input.TextArea />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="变更状态"
        open={isStatusModalOpen}
        onCancel={() => setIsStatusModalOpen(false)}
        onOk={() => statusForm.submit()}
      >
        <Form form={statusForm} onFinish={handleStatusSubmit} layout="vertical">
          <Form.Item name="phoneId" label="电话ID" hidden>
            <Input type="number" />
          </Form.Item>
          <Form.Item name="currentStatus" label="当前状态">
            <Input disabled />
          </Form.Item>
          <Form.Item label="新状态">
            <Tag color={STATUS_COLORS[selectedNewStatus] || 'default'}>
              {STATUS_NAMES[selectedNewStatus]}
            </Tag>
          </Form.Item>
          <Form.Item name="workOrderNo" label="工单号">
            <Input placeholder="输入工单号" />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input.TextArea />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="电话销户"
        open={isSurrenderModalOpen}
        onCancel={() => setIsSurrenderModalOpen(false)}
        onOk={() => surrenderForm.submit()}
      >
        <Form form={surrenderForm} onFinish={handleSurrenderSubmit} layout="vertical">
          <Form.Item name="phoneId" label="电话ID" hidden>
            <Input type="number" />
          </Form.Item>
          <Form.Item name="surrenderType" label="销户类型" rules={[{ required: true }]}>
            <Select placeholder="选择销户类型">
              <Option value="surrender">销户</Option>
              <Option value="cancel">取消</Option>
            </Select>
          </Form.Item>
          <Form.Item name="workOrderNo" label="工单号">
            <Input placeholder="输入工单号" />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input.TextArea />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default PhoneManagement
