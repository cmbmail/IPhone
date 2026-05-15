import { useState } from 'react'
import { Table, Button, Modal, Form, Input, Select, message } from 'antd'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { orgApi } from '@/api/org'
import type { OrgStructure, CreateOrgDTO, UpdateOrgDTO } from '@/types/org'

const { Option } = Select

const OrgManagement = () => {
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingOrg, setEditingOrg] = useState<OrgStructure | null>(null)
  const [form] = Form.useForm()
  const queryClient = useQueryClient()

  const { data: orgs, isLoading } = useQuery({
    queryKey: ['orgs'],
    queryFn: async () => {
      const response = await orgApi.getAll()
      return response.data
    }
  })

  const createMutation = useMutation({
    mutationFn: (data: CreateOrgDTO) => orgApi.create(data),
    onSuccess: () => {
      message.success('Organization created successfully')
      queryClient.invalidateQueries({ queryKey: ['orgs'] })
      setIsModalOpen(false)
      form.resetFields()
    }
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateOrgDTO }) =>
      orgApi.update(id, data),
    onSuccess: () => {
      message.success('Organization updated successfully')
      queryClient.invalidateQueries({ queryKey: ['orgs'] })
      setIsModalOpen(false)
      form.resetFields()
    }
  })

  const deleteMutation = useMutation({
    mutationFn: (id: number) => orgApi.delete(id),
    onSuccess: () => {
      message.success('Organization deleted successfully')
      queryClient.invalidateQueries({ queryKey: ['orgs'] })
    }
  })

  const handleEdit = (record: OrgStructure) => {
    setEditingOrg(record)
    form.setFieldsValue(record)
    setIsModalOpen(true)
  }

  const handleDelete = (id: number) => {
    Modal.confirm({
      title: 'Confirm Delete',
      content: 'Are you sure you want to delete this organization?',
      onOk: () => deleteMutation.mutate(id)
    })
  }

  const handleSubmit = (values: any) => {
    if (editingOrg) {
      updateMutation.mutate({ id: editingOrg.id, data: values })
    } else {
      createMutation.mutate(values as CreateOrgDTO)
    }
  }

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id' },
    { title: 'Name', dataIndex: 'name', key: 'name' },
    { title: 'Type', dataIndex: 'type', key: 'type' },
    { title: 'Level', dataIndex: 'level', key: 'level' },
    { title: 'Status', dataIndex: 'status', key: 'status' },
    {
      title: 'Actions',
      key: 'actions',
      render: (_: any, record: OrgStructure) => (
        <>
          <Button size="small" onClick={() => handleEdit(record)}>Edit</Button>
          <Button size="small" danger onClick={() => handleDelete(record.id)} style={{ marginLeft: 8 }}>
            Delete
          </Button>
        </>
      )
    }
  ]

  return (
    <div>
      <Button type="primary" onClick={() => { setEditingOrg(null); setIsModalOpen(true); }} style={{ marginBottom: 16 }}>
        Add Organization
      </Button>
      <Table columns={columns} dataSource={orgs} loading={isLoading} rowKey="id" />

      <Modal
        title={editingOrg ? 'Edit Organization' : 'Add Organization'}
        open={isModalOpen}
        onCancel={() => { setIsModalOpen(false); form.resetFields(); }}
        onOk={() => form.submit()}
      >
        <Form form={form} onFinish={handleSubmit} layout="vertical">
          <Form.Item name="parentId" label="Parent Organization">
            <Input type="number" />
          </Form.Item>
          <Form.Item name="name" label="Name" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="type" label="Type" rules={[{ required: true }]}>
            <Select>
              <Option value="group">Group</Option>
              <Option value="subsidiary">Subsidiary</Option>
              <Option value="dept">Department</Option>
            </Select>
          </Form.Item>
          <Form.Item name="status" label="Status" initialValue="active">
            <Select>
              <Option value="active">Active</Option>
              <Option value="inactive">Inactive</Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default OrgManagement
