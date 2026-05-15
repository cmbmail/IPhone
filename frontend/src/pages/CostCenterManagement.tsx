import { useState } from 'react'
import { Table, Button, Modal, Form, Input, Select, message } from 'antd'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { costCenterApi } from '@/api/costCenter'
import { orgApi } from '@/api/org'
import type { CostCenter, CreateCostCenterDTO, UpdateCostCenterDTO } from '@/types/costCenter'
import type { OrgStructure } from '@/types/org'

const { Option } = Select

const CostCenterManagement = () => {
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingCostCenter, setEditingCostCenter] = useState<CostCenter | null>(null)
  const [form] = Form.useForm()
  const queryClient = useQueryClient()

  const { data: costCenters, isLoading } = useQuery({
    queryKey: ['costCenters'],
    queryFn: async () => {
      const response = await costCenterApi.getAll()
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
    mutationFn: (data: CreateCostCenterDTO) => costCenterApi.create(data),
    onSuccess: () => {
      message.success('Cost center created successfully')
      queryClient.invalidateQueries({ queryKey: ['costCenters'] })
      setIsModalOpen(false)
      form.resetFields()
    }
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateCostCenterDTO }) =>
      costCenterApi.update(id, data),
    onSuccess: () => {
      message.success('Cost center updated successfully')
      queryClient.invalidateQueries({ queryKey: ['costCenters'] })
      setIsModalOpen(false)
      form.resetFields()
    }
  })

  const deleteMutation = useMutation({
    mutationFn: (id: number) => costCenterApi.delete(id),
    onSuccess: () => {
      message.success('Cost center deleted successfully')
      queryClient.invalidateQueries({ queryKey: ['costCenters'] })
    }
  })

  const handleEdit = (record: CostCenter) => {
    setEditingCostCenter(record)
    form.setFieldsValue(record)
    setIsModalOpen(true)
  }

  const handleDelete = (id: number) => {
    Modal.confirm({
      title: 'Confirm Delete',
      content: 'Are you sure you want to delete this cost center?',
      onOk: () => deleteMutation.mutate(id)
    })
  }

  const handleSubmit = (values: any) => {
    if (editingCostCenter) {
      updateMutation.mutate({ id: editingCostCenter.id, data: values })
    } else {
      createMutation.mutate(values as CreateCostCenterDTO)
    }
  }

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
    { title: 'Organization ID', dataIndex: 'orgId', key: 'orgId', width: 120 },
    { title: 'Name', dataIndex: 'costCenterName', key: 'costCenterName' },
    { title: 'Code', dataIndex: 'costCenterCode', key: 'costCenterCode', width: 150 },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string) => (
        <span style={{ color: status === 'active' ? 'green' : 'red' }}>{status}</span>
      )
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 150,
      render: (_: any, record: CostCenter) => (
        <>
          <Button size="small" onClick={() => handleEdit(record)} style={{ marginRight: 8 }}>
            Edit
          </Button>
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
          setEditingCostCenter(null)
          form.resetFields()
          setIsModalOpen(true)
        }}
        style={{ marginBottom: 16 }}
      >
        Add Cost Center
      </Button>

      <Table
        columns={columns}
        dataSource={costCenters}
        loading={isLoading}
        rowKey="id"
      />

      <Modal
        title={editingCostCenter ? 'Edit Cost Center' : 'Add Cost Center'}
        open={isModalOpen}
        onCancel={() => {
          setIsModalOpen(false)
          form.resetFields()
        }}
        onOk={() => form.submit()}
      >
        <Form form={form} onFinish={handleSubmit} layout="vertical">
          {!editingCostCenter && (
            <Form.Item name="orgId" label="Organization" rules={[{ required: true }]}>
              <Select placeholder="Select organization">
                {orgs?.map(org => (
                  <Option key={org.id} value={org.id}>
                    {org.name}
                  </Option>
                ))}
              </Select>
            </Form.Item>
          )}

          <Form.Item name="costCenterName" label="Name" rules={[{ required: true }]}>
            <Input />
          </Form.Item>

          <Form.Item name="costCenterCode" label="Code" rules={[{ required: true }]}>
            <Input />
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

export default CostCenterManagement
