import { useState } from 'react'
import { Table, Button, Modal, Form, Input, Select, Tag, message } from 'antd'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { extensionPoolApi } from '@/api/extensionPool'
import { request } from '@/api/request'
import { orgApi } from '@/api/org'
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
      setIsModalOpen(false)
      form.resetFields()
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

  const handleSubmit = (values: any) => {
    createMutation.mutate(values as CreateExtensionPoolDTO)
  }

  const handleDelete = (id: number) => {
    Modal.confirm({
      title: '确认删除',
      content: '确定要删除此号池吗？',
      onOk: () => deleteMutation.mutate(id)
    })
  }

  const getUsageByPoolId = (poolId: number) => {
    return usageData?.find((u: any) => u.poolId === poolId)
  }

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
    { title: '组织ID', dataIndex: 'orgId', key: 'orgId', width: 120 },
    { title: '起始号码', dataIndex: 'startNumber', key: 'startNumber', width: 120 },
    { title: '结束号码', dataIndex: 'endNumber', key: 'endNumber', width: 120 },
    { title: '分配人', dataIndex: 'allocatedBy', key: 'allocatedBy', width: 120 },
    {
      title: '使用率',
      key: 'usage',
      width: 200,
      render: (_: any, record: ExtensionPool) => {
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
      width: 120,
      render: (_: any, record: ExtensionPool) => (
        <>
          <Button size="small" danger onClick={() => handleDelete(record.id)}>
            删除
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
        添加分机号池
      </Button>

      <Table
        columns={columns}
        dataSource={pools}
        loading={isLoading}
        rowKey="id"
      />

      <Modal
        title="添加分机号池"
        open={isModalOpen}
        onCancel={() => setIsModalOpen(false)}
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
