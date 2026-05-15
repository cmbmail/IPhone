import React, { useEffect, useState } from 'react'
import { Table, Button, Modal, Form, Input, Select, message, Space, Popconfirm, Card } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, SearchOutlined } from '@ant-design/icons'
import { getEmployeePage, addEmployee, updateEmployee, deleteEmployee, getEmployeeById, getOrgList, getRoleList } from '../api/index.js'

const EmployeeList = () => {
  const [data, setData] = useState([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [form] = Form.useForm()
  const [orgList, setOrgList] = useState([])
  const [roleList, setRoleList] = useState([])
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 })
  const [searchForm] = Form.useForm()

  const columns = [
    {
      title: '工号',
      dataIndex: 'empNo',
      key: 'empNo',
    },
    {
      title: '用户名',
      dataIndex: 'username',
      key: 'username',
    },
    {
      title: '真实姓名',
      dataIndex: 'realName',
      key: 'realName',
    },
    {
      title: '手机号',
      dataIndex: 'phone',
      key: 'phone',
    },
    {
      title: '邮箱',
      dataIndex: 'email',
      key: 'email',
    },
    {
      title: '性别',
      dataIndex: 'gender',
      key: 'gender',
      render: (gender) => {
        const genderMap = { 0: '未知', 1: '男', 2: '女' }
        return genderMap[gender] || '未知'
      },
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status) => (
        <span style={{ color: status === 1 ? '#52c41a' : '#ff4d4f' }}>
          {status === 1 ? '启用' : '禁用'}
        </span>
      ),
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space size="middle">
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record.id)}
          >
            编辑
          </Button>
          <Popconfirm
            title="确定要删除该员工吗？"
            onConfirm={() => handleDelete(record.id)}
            okText="确定"
            cancelText="取消"
          >
            <Button type="link" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  const loadData = async (page = 1, pageSize = 10, params = {}) => {
    setLoading(true)
    try {
      const res = await getEmployeePage({ current: page, size: pageSize, ...params })
      setData(res.data?.records || [])
      setPagination({
        current: page,
        pageSize,
        total: res.data?.total || 0
      })
    } catch (error) {
      console.error('加载员工列表失败:', error)
    } finally {
      setLoading(false)
    }
  }

  const loadOptions = async () => {
    try {
      const [orgRes, roleRes] = await Promise.all([getOrgList(), getRoleList()])
      setOrgList(orgRes.data || [])
      setRoleList(roleRes.data || [])
    } catch (error) {
      console.error('加载选项失败:', error)
    }
  }

  useEffect(() => {
    loadData()
    loadOptions()
  }, [])

  const handleSearch = () => {
    const values = searchForm.getFieldsValue()
    loadData(1, 10, values)
  }

  const handleAdd = () => {
    setEditingId(null)
    form.resetFields()
    form.setFieldsValue({ status: 1, gender: 0 })
    setModalVisible(true)
  }

  const handleEdit = async (id) => {
    try {
      const res = await getEmployeeById(id)
      form.setFieldsValue(res.data)
      setEditingId(id)
      setModalVisible(true)
    } catch (error) {
      console.error('加载员工详情失败:', error)
    }
  }

  const handleDelete = async (id) => {
    try {
      await deleteEmployee(id)
      message.success('删除成功')
      loadData(pagination.current, pagination.pageSize)
    } catch (error) {
      console.error('删除员工失败:', error)
    }
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      if (editingId) {
        await updateEmployee({ ...values, id: editingId })
        message.success('更新成功')
      } else {
        await addEmployee(values)
        message.success('添加成功')
      }
      setModalVisible(false)
      loadData(pagination.current, pagination.pageSize)
    } catch (error) {
      console.error('保存失败:', error)
    }
  }

  const handleTableChange = (pagination) => {
    const values = searchForm.getFieldsValue()
    loadData(pagination.current, pagination.pageSize, values)
  }

  return (
    <div>
      <Card style={{ marginBottom: 16 }} size="small">
        <Form
          form={searchForm}
          layout="inline"
          onFinish={handleSearch}
        >
          <Form.Item name="username" label="用户名">
            <Input placeholder="请输入用户名" />
          </Form.Item>
          <Form.Item name="realName" label="真实姓名">
            <Input placeholder="请输入真实姓名" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" icon={<SearchOutlined />} htmlType="submit">
              搜索
            </Button>
          </Form.Item>
        </Form>
      </Card>

      <div style={{ marginBottom: 16 }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
          新增员工
        </Button>
      </div>

      <Table
        columns={columns}
        dataSource={data}
        rowKey="id"
        loading={loading}
        pagination={{
          current: pagination.current,
          pageSize: pagination.pageSize,
          total: pagination.total,
          showTotal: (total) => `共 ${total} 条`,
        }}
        onChange={handleTableChange}
      />

      <Modal
        title={editingId ? '编辑员工' : '新增员工'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="empNo"
            label="工号"
            rules={[{ required: true, message: '请输入工号' }]}
          >
            <Input placeholder="请输入工号" />
          </Form.Item>
          <Form.Item
            name="username"
            label="用户名"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input placeholder="请输入用户名" />
          </Form.Item>
          {!editingId && (
            <Form.Item
              name="password"
              label="密码"
              rules={[{ required: true, message: '请输入密码' }]}
            >
              <Input.Password placeholder="请输入密码" />
            </Form.Item>
          )}
          <Form.Item
            name="realName"
            label="真实姓名"
            rules={[{ required: true, message: '请输入真实姓名' }]}
          >
            <Input placeholder="请输入真实姓名" />
          </Form.Item>
          <Form.Item
            name="orgId"
            label="所属组织"
          >
            <Select placeholder="请选择组织">
              {orgList.map(org => (
                <Select.Option key={org.id} value={org.id}>
                  {org.orgName}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item
            name="phone"
            label="手机号"
          >
            <Input placeholder="请输入手机号" />
          </Form.Item>
          <Form.Item
            name="email"
            label="邮箱"
          >
            <Input placeholder="请输入邮箱" />
          </Form.Item>
          <Form.Item
            name="gender"
            label="性别"
          >
            <Select>
              <Select.Option value={0}>未知</Select.Option>
              <Select.Option value={1}>男</Select.Option>
              <Select.Option value={2}>女</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item
            name="status"
            label="状态"
          >
            <Select>
              <Select.Option value={1}>启用</Select.Option>
              <Select.Option value={0}>禁用</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item
            name="remark"
            label="备注"
          >
            <Input.TextArea rows={3} placeholder="请输入备注" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default EmployeeList
