import { useState } from 'react'
import { Table, Button, Modal, Form, Input, InputNumber, Select, Tag, message } from 'antd'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { areaCodeApi } from '@/api/areaCode'
import { orgApi } from '@/api/org'
import type { AreaCodeOrgMapping, CreateAreaCodeMappingDTO } from '@/types/areaCode'
import type { OrgStructure } from '@/types/org'

const { Option } = Select

const AreaCodeManagement = () => {
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [form] = Form.useForm()
  const queryClient = useQueryClient()

  const { data: mappings, isLoading } = useQuery({
    queryKey: ['areaCodes'],
    queryFn: async () => {
      const response = await areaCodeApi.getAll()
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
    mutationFn: (data: CreateAreaCodeMappingDTO) => areaCodeApi.create(data),
    onSuccess: () => {
      message.success('区号映射创建成功')
      queryClient.invalidateQueries({ queryKey: ['areaCodes'] })
      setIsModalOpen(false)
      form.resetFields()
    }
  })

  const deleteMutation = useMutation({
    mutationFn: (id: number) => areaCodeApi.delete(id),
    onSuccess: () => {
      message.success('区号映射删除成功')
      queryClient.invalidateQueries({ queryKey: ['areaCodes'] })
    }
  })

  const handleSubmit = (values: any) => {
    createMutation.mutate(values as CreateAreaCodeMappingDTO)
  }

  const handleDelete = (id: number) => {
    Modal.confirm({
      title: '确认删除',
      content: '确定要删除此区号映射吗？',
      onOk: () => deleteMutation.mutate(id)
    })
  }

  const getOrgName = (orgId: number) => {
    const org = orgs?.find((o: OrgStructure) => o.id === orgId)
    return org ? org.name : orgId
  }

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
    {
      title: '区号',
      dataIndex: 'areaCode',
      key: 'areaCode',
      width: 120,
      render: (code: string) => <Tag color="blue">{code}</Tag>
    },
    {
      title: '组织',
      dataIndex: 'orgId',
      key: 'orgId',
      render: (orgId: number) => getOrgName(orgId)
    },
    {
      title: '优先级',
      dataIndex: 'priority',
      key: 'priority',
      width: 100,
      sorter: (a: AreaCodeOrgMapping, b: AreaCodeOrgMapping) => a.priority - b.priority
    },
    { title: '创建人', dataIndex: 'createdBy', key: 'createdBy', width: 120 },
    {
      title: '操作',
      key: 'actions',
      width: 100,
      render: (_: any, record: AreaCodeOrgMapping) => (
        <Button size="small" danger onClick={() => handleDelete(record.id)}>
          删除
        </Button>
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
        添加区号映射
      </Button>

      <Table
        columns={columns}
        dataSource={mappings}
        loading={isLoading}
        rowKey="id"
      />

      <Modal
        title="添加区号映射"
        open={isModalOpen}
        onCancel={() => setIsModalOpen(false)}
        onOk={() => form.submit()}
      >
        <Form form={form} onFinish={handleSubmit} layout="vertical">
          <Form.Item
            name="areaCode"
            label="区号"
            rules={[{ required: true }, { pattern: /^0\d{2,3}$/, message: '格式：010，021 等' }]}
          >
            <Input placeholder="例如：010" />
          </Form.Item>

          <Form.Item name="orgId" label="组织" rules={[{ required: true }]}>
            <Select placeholder="选择组织">
              {orgs?.map((org: OrgStructure) => (
                <Option key={org.id} value={org.id}>
                  {org.name}
                </Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item name="priority" label="优先级" initialValue={1}>
            <InputNumber min={1} max={100} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default AreaCodeManagement
