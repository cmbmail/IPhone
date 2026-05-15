import { useState } from 'react'
import { Table, Button, Modal, Form, Input, Select, DatePicker, Switch, message } from 'antd'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { employeeApi } from '@/api/employee'
import { orgApi } from '@/api/org'
import type { Employee, CreateEmployeeDTO, UpdateEmployeeDTO } from '@/types/employee'
import type { OrgStructure } from '@/types/org'

const { Option } = Select

const EmployeeManagement = () => {
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingEmployee, setEditingEmployee] = useState<Employee | null>(null)
  const [form] = Form.useForm()
  const queryClient = useQueryClient()

  const { data: employeesData, isLoading } = useQuery({
    queryKey: ['employees'],
    queryFn: async () => {
      const response = await employeeApi.getAll({ page: 0, size: 100 })
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
    mutationFn: (data: CreateEmployeeDTO) => employeeApi.create(data),
    onSuccess: () => {
      message.success('Employee created successfully')
      queryClient.invalidateQueries({ queryKey: ['employees'] })
      setIsModalOpen(false)
      form.resetFields()
    }
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateEmployeeDTO }) =>
      employeeApi.update(id, data),
    onSuccess: () => {
      message.success('Employee updated successfully')
      queryClient.invalidateQueries({ queryKey: ['employees'] })
      setIsModalOpen(false)
      form.resetFields()
    }
  })

  const deleteMutation = useMutation({
    mutationFn: (id: number) => employeeApi.delete(id),
    onSuccess: () => {
      message.success('Employee deactivated successfully')
      queryClient.invalidateQueries({ queryKey: ['employees'] })
    }
  })

  const handleEdit = (record: Employee) => {
    setEditingEmployee(record)
    form.setFieldsValue({
      ...record,
      entryDate: record.entryDate ? record.entryDate.split('T')[0] : null
    })
    setIsModalOpen(true)
  }

  const handleDelete = (id: number) => {
    Modal.confirm({
      title: 'Confirm Deactivate',
      content: 'Are you sure you want to deactivate this employee?',
      onOk: () => deleteMutation.mutate(id)
    })
  }

  const handleSubmit = (values: any) => {
    const data = {
      ...values,
      entryDate: values.entryDate?.format('YYYY-MM-DD')
    }

    if (editingEmployee) {
      updateMutation.mutate({ id: editingEmployee.id, data })
    } else {
      createMutation.mutate(data as CreateEmployeeDTO)
    }
  }

  const columns = [
    { title: 'Employee No', dataIndex: 'employeeNo', key: 'employeeNo', width: 120 },
    { title: 'Name', dataIndex: 'name', key: 'name' },
    { title: 'Position', dataIndex: 'position', key: 'position' },
    { title: 'Phone', dataIndex: 'phone', key: 'phone' },
    { title: 'Email', dataIndex: 'email', key: 'email' },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <span style={{ color: status === 'active' ? 'green' : 'red' }}>{status}</span>
      )
    },
    { title: 'Virtual', dataIndex: 'isVirtual', key: 'isVirtual', render: (v: boolean) => (v ? 'Yes' : 'No') },
    {
      title: 'Actions',
      key: 'actions',
      render: (_: any, record: Employee) => (
        <>
          <Button size="small" onClick={() => handleEdit(record)} style={{ marginRight: 8 }}>
            Edit
          </Button>
          <Button size="small" danger onClick={() => handleDelete(record.id)}>
            Deactivate
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
          setEditingEmployee(null)
          form.resetFields()
          setIsModalOpen(true)
        }}
        style={{ marginBottom: 16 }}
      >
        Add Employee
      </Button>

      <Table
        columns={columns}
        dataSource={employeesData?.content}
        loading={isLoading}
        rowKey="id"
        pagination={{
          total: employeesData?.totalElements,
          pageSize: 20
        }}
      />

      <Modal
        title={editingEmployee ? 'Edit Employee' : 'Add Employee'}
        open={isModalOpen}
        onCancel={() => {
          setIsModalOpen(false)
          form.resetFields()
        }}
        onOk={() => form.submit()}
        width={600}
      >
        <Form form={form} onFinish={handleSubmit} layout="vertical">
          {!editingEmployee && (
            <Form.Item name="employeeNo" label="Employee No" rules={[{ required: true }]}>
              <Input placeholder="6 alphanumeric characters, e.g., EMP001" maxLength={6} />
            </Form.Item>
          )}

          <Form.Item name="name" label="Name" rules={[{ required: true }]}>
            <Input />
          </Form.Item>

          <Form.Item name="orgId" label="Organization" rules={[{ required: true }]}>
            <Select placeholder="Select organization">
              {orgsData?.map(org => (
                <Option key={org.id} value={org.id}>
                  {org.name} ({org.type})
                </Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item name="position" label="Position">
            <Input />
          </Form.Item>

          <Form.Item name="phone" label="Phone">
            <Input />
          </Form.Item>

          <Form.Item name="email" label="Email">
            <Input type="email" />
          </Form.Item>

          <Form.Item name="entryDate" label="Entry Date">
            <Input type="date" />
          </Form.Item>

          {!editingEmployee && (
            <Form.Item name="isVirtual" label="Virtual Employee" valuePropName="checked">
              <Switch />
            </Form.Item>
          )}
        </Form>
      </Modal>
    </div>
  )
}

export default EmployeeManagement
