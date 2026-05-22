import { useState } from 'react'
import { Table, Button, Modal, Form, Input, Select, Tag, message, Space } from 'antd'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { extensionPoolApi } from '@/api/extensionPool'
import { request } from '@/api/request'
import { orgApi } from '@/api/org'
import { EditOutlined } from '@ant-design/icons'
import type { ExtensionPool, CreateExtensionPoolDTO } from '@/types/extensionPool'
import type { OrgStructure } from '@/types/org'

const { Option } = Select

const STATUS_COLORS: Record<string, string> = {
  green: 'success',
  yellow: 'warning',
  red: 'error'
}

const STATUS_NAMES: Record<string, string> = {
  green: '正常',
  yellow: '警告',
  red: '危险'
}

const ExtensionPoolManagement = () => {
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editRecord, setEditRecord] = useState<ExtensionPool | null>(null)
  const [orgFilter, setOrgFilter] = useState<number | undefined>(undefined)
  const [form] = Form.useForm()
  const queryClient = useQueryClient()

  const { data: pools, isLoading } = useQuery({
    queryKey: ['extensionPools'],
    queryFn: async () => {
      const response = await extensionPoolApi.getAll()
      return response.data.data
    }
  })

  const { data: usageData } = useQuery({
    queryKey: ['extensionPoolUsage'],
    queryFn: async () => {
      const response = await request.get('/extension-pools/all-usage')
      return response.data.data
    }
  })

  const { data: orgs } = useQuery({
    queryKey: ['orgs'],
    queryFn: async () => {
      const response = await orgApi.getAll()
      return response.data.data
    }
  })

  const createMutation = useMutation({
    mutationFn: (data: CreateExtensionPoolDTO) => extensionPoolApi.create(data),
    onSuccess: () => {
      message.success('分机号池创建成功')
      queryClient.invalidateQueries({ queryKey: ['extensionPools'] })
      queryClient.invalidateQueries({ queryKey: ['extensionPoolUsage'] })
      closeModal()
    }
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: Record<string, unknown> }) => extensionPoolApi.update(id, data),
    onSuccess: () => {
      message.success('分机号池更新成功')
      queryClient.invalidateQueries({ queryKey: ['extensionPools'] })
      queryClient.invalidateQueries({ queryKey: ['extensionPoolUsage'] })
      closeModal()
    }
  })

  const deleteMutation = useMutation({
    mutationFn: (id: number) => extensionPoolApi.delete(id),
    onSuccess: () => {
      message.success('分机号池删除成功')
      queryClient.invalidateQueries({ queryKey: ['extensionPools'] })
      queryClient.invalidateQueries({ queryKey: ['extensionPoolUsage'] })
    }
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
      createMutation.mutate(values as CreateExtensionPoolDTO)
    }
  }

  const handleEdit = (record: ExtensionPool) => {
    setEditRecord(record)
    form.setFieldsValue({
      orgId: record.orgId,
      startNumber: record.startNumber,
      endNumber: record.endNumber,
    })
    setIsModalOpen(true)
  }

  const handleDelete = (id: number) => {
    Modal.confirm({
      title: '确认删除',
      content: '确定要删除此号池吗？',
      onOk: () => deleteMutation.mutate(id)
    })
  }

  const getUsageByPoolId = (poolId: number) => {
    return usageData?.find((u: { poolId: number }) => u.poolId === poolId)
  }

  const getOrgName = (orgId: number) => {
    const org = orgs?.find((o: OrgStructure) => o.id === orgId)
    return org ? org.name : String(orgId)
  }

  const filteredPools = orgFilter
    ? pools?.filter((p: ExtensionPool) => p.orgId === orgFilter)
    : pools

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
    {
      title: '组织',
      dataIndex: 'orgId',
      key: 'orgId',
      width: 150,
      render: (orgId: number) => getOrgName(orgId)
    },
    { title: '起始号码', dataIndex: 'startNumber', key: 'startNumber', width: 120 },
    { title: '结束号码', dataIndex: 'endNumber', key: 'endNumber', width: 120 },
    { title: '分配人', dataIndex: 'allocatedBy', key: 'allocatedBy', width: 120 },
    {
      title: '使用率',
      key: 'usage',
      width: 200,
      render: (_: unknown, record: ExtensionPool) => {
        const usage = getUsageByPoolId(record.id)
        if (!usage) return '-'
        return (
          <span>
            {usage.usedCount}/{usage.totalCount} ({usage.usageRate.toFixed(1)}%)
            <Tag color={STATUS_COLORS[usage.status]} style={{ marginLeft: 8 }}>
              {STATUS_NAMES[usage.status]}
            </Tag>
          </span>
        )
      }
    },
    {
      title: '操作',
      key: 'actions',
      width: 160,
      render: (_: unknown, record: ExtensionPool) => (
        <Space>
          <Button size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)}>
            编辑
          </Button>
          <Button size="small" danger onClick={() => handleDelete(record.id)}>
            删除
          </Button>
        </Space>
      )
    }
  ]

  return (
    <div>
      <Space style={{ marginBottom: 16 }}>
        <Button
          type="primary"
          onClick={() => {
            setEditRecord(null)
            form.resetFields()
            setIsModalOpen(true)
          }}
        >
          添加分机号池
        </Button>
        <Select
          value={orgFilter}
          onChange={setOrgFilter}
          style={{ width: 200 }}
          allowClear
          placeholder="按组织筛选"
        >
          {orgs?.map((org: OrgStructure) => (
            <Option key={org.id} value={org.id}>{org.name}</Option>
          ))}
        </Select>
      </Space>

      <Table
        columns={columns}
        dataSource={filteredPools}
        loading={isLoading}
        rowKey="id"
      />

      <Modal
        title={editRecord ? '编辑分机号池' : '添加分机号池'}
        open={isModalOpen}
        onCancel={closeModal}
        onOk={() => form.submit()}
      >
        <Form form={form} onFinish={handleSubmit} layout="vertical">
          <Form.Item name="orgId" label="组织" rules={[{ required: true }]}>
            <Select placeholder="选择组织">
              {orgs?.map((org: OrgStructure) => (
                <Option key={org.id} value={org.id}>
                  {org.name}
                </Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item name="startNumber" label="起始号码" rules={[{ required: true }]}>
            <Input placeholder="例如：100" />
          </Form.Item>

          <Form.Item name="endNumber" label="结束号码" rules={[{ required: true }]}>
            <Input placeholder="例如：199" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default ExtensionPoolManagement
