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
      return response.data
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
    mutationFn: (data: CreateAreaCodeMappingDTO) => areaCodeApi.create(data),
    onSuccess: () => {
      message.success('Area code mapping created successfully')
      queryClient.invalidateQueries({ queryKey: ['areaCodes'] })
      setIsModalOpen(false)
      form.resetFields()
    }
  })

  const deleteMutation = useMutation({
    mutationFn: (id: number) => areaCodeApi.delete(id),
    onSuccess: () => {
      message.success('Area code mapping deleted successfully')
      queryClient.invalidateQueries({ queryKey: ['areaCodes'] })
    }
  })

  const handleSubmit = (values: any) => {
    createMutation.mutate(values as CreateAreaCodeMappingDTO)
  }

  const handleDelete = (id: number) => {
    Modal.confirm({
      title: 'Confirm Delete',
      content: 'Are you sure you want to delete this area code mapping?',
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
      title: 'Area Code',
      dataIndex: 'areaCode',
      key: 'areaCode',
      width: 120,
      render: (code: string) => <Tag color="blue">{code}</Tag>
    },
    {
      title: 'Organization',
      dataIndex: 'orgId',
      key: 'orgId',
      render: (orgId: number) => getOrgName(orgId)
    },
    {
      title: 'Priority',
      dataIndex: 'priority',
      key: 'priority',
      width: 100,
      sorter: (a: AreaCodeOrgMapping, b: AreaCodeOrgMapping) => a.priority - b.priority
    },
    { title: 'Created By', dataIndex: 'createdBy', key: 'createdBy', width: 120 },
    {
      title: 'Actions',
      key: 'actions',
      width: 100,
      render: (_: any, record: AreaCodeOrgMapping) => (
        <Button size="small" danger onClick={() => handleDelete(record.id)}>
          Delete
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
        Add Area Code Mapping
      </Button>

      <Table
        columns={columns}
        dataSource={mappings}
        loading={isLoading}
        rowKey="id"
      />

      <Modal
        title="Add Area Code Mapping"
        open={isModalOpen}
        onCancel={() => setIsModalOpen(false)}
        onOk={() => form.submit()}
      >
        <Form form={form} onFinish={handleSubmit} layout="vertical">
          <Form.Item
            name="areaCode"
            label="Area Code"
            rules={[{ required: true }, { pattern: /^0\d{2,3}$/, message: 'Format: 010, 021, etc.' }]}
          >
            <Input placeholder="e.g., 010" />
          </Form.Item>

          <Form.Item name="orgId" label="Organization" rules={[{ required: true }]}>
            <Select placeholder="Select organization">
              {orgs?.map((org: OrgStructure) => (
                <Option key={org.id} value={org.id}>
                  {org.name}
                </Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item name="priority" label="Priority" initialValue={1}>
            <InputNumber min={1} max={100} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default AreaCodeManagement
