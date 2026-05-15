import { useState } from 'react'
import { Table, Button, Modal, Form, Input, Select, Tag, message } from 'antd'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { extensionPoolApi } from '@/api/extensionPool'
import { orgApi } from '@/api/org'
import type { ExtensionPool, CreateExtensionPoolDTO } from '@/types/extensionPool'
import type { OrgStructure } from '@/types/org'

const { Option } = Select

const STATUS_COLORS: Record<string, string> = {
  green: 'success',
  yellow: 'warning',
  red: 'error'
}

const ExtensionPoolManagement = () => {
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [form] = Form.useForm()
  const queryClient = useQueryClient()

  const { data: pools, isLoading } = useQuery({
    queryKey: ['extensionPools'],
    queryFn: async () => {
      const response = await extensionPoolApi.getAll()
      return response.data
    }
  })

  const { data: usageData } = useQuery({
    queryKey: ['extensionPoolUsage'],
    queryFn: async () => {
      const response = await extensionPoolApi.getAll()
      const pools = response.data
      const usagePromises = pools.map((pool: ExtensionPool) =>
        extensionPoolApi.getUsage(pool.id).then(res => ({ id: pool.id, ...res.data }))
      )
      return Promise.all(usagePromises)
    }
  })

  const { data: orgs } = useQuery({
    queryKey: ['orgs'],
    queryFn: async () => {
      const response = await orgApi.getAll()
      return response.data
    }
  })

  const createMutation = useMutation({
    mutationFn: (data: CreateExtensionPoolDTO) => extensionPoolApi.create(data),
    onSuccess: () => {
      message.success('Extension pool created successfully')
      queryClient.invalidateQueries({ queryKey: ['extensionPools'] })
      queryClient.invalidateQueries({ queryKey: ['extensionPoolUsage'] })
      setIsModalOpen(false)
      form.resetFields()
    }
  })

  const deleteMutation = useMutation({
    mutationFn: (id: number) => extensionPoolApi.delete(id),
    onSuccess: () => {
      message.success('Extension pool deleted successfully')
      queryClient.invalidateQueries({ queryKey: ['extensionPools'] })
      queryClient.invalidateQueries({ queryKey: ['extensionPoolUsage'] })
    }
  })

  const handleSubmit = (values: any) => {
    createMutation.mutate(values as CreateExtensionPoolDTO)
  }

  const handleDelete = (id: number) => {
    Modal.confirm({
      title: 'Confirm Delete',
      content: 'Are you sure you want to delete this extension pool?',
      onOk: () => deleteMutation.mutate(id)
    })
  }

  const getUsageByPoolId = (poolId: number) => {
    return usageData?.find((u: any) => u.id === poolId)
  }

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
    { title: 'Organization ID', dataIndex: 'orgId', key: 'orgId', width: 120 },
    { title: 'Start Number', dataIndex: 'startNumber', key: 'startNumber', width: 120 },
    { title: 'End Number', dataIndex: 'endNumber', key: 'endNumber', width: 120 },
    { title: 'Allocated By', dataIndex: 'allocatedBy', key: 'allocatedBy', width: 120 },
    {
      title: 'Usage',
      key: 'usage',
      width: 200,
      render: (_: any, record: ExtensionPool) => {
        const usage = getUsageByPoolId(record.id)
        if (!usage) return '-'
        return (
          <span>
            {usage.usedCount}/{usage.totalCount} ({usage.usageRate.toFixed(1)}%)
            <Tag color={STATUS_COLORS[usage.status]} style={{ marginLeft: 8 }}>
              {usage.status.toUpperCase()}
            </Tag>
          </span>
        )
      }
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 120,
      render: (_: any, record: ExtensionPool) => (
        <>
          <Button size="small" danger onClick={() => handleDelete(record.id)}>
            Delete
          </Button>
        </>
      )
    }
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
        Add Extension Pool
      </Button>

      <Table
        columns={columns}
        dataSource={pools}
        loading={isLoading}
        rowKey="id"
      />

      <Modal
        title="Add Extension Pool"
        open={isModalOpen}
        onCancel={() => setIsModalOpen(false)}
        onOk={() => form.submit()}
      >
        <Form form={form} onFinish={handleSubmit} layout="vertical">
          <Form.Item name="orgId" label="Organization" rules={[{ required: true }]}>
            <Select placeholder="Select organization">
              {orgs?.map((org: OrgStructure) => (
                <Option key={org.id} value={org.id}>
                  {org.name}
                </Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item name="startNumber" label="Start Number" rules={[{ required: true }]}>
            <Input placeholder="e.g., 100" />
          </Form.Item>

          <Form.Item name="endNumber" label="End Number" rules={[{ required: true }]}>
            <Input placeholder="e.g., 199" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default ExtensionPoolManagement
