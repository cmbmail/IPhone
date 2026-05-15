import React, { useEffect, useState } from 'react'
import { Table, Button, Modal, Form, Input, Select, message, Space, Popconfirm, Tag, Card } from 'antd'
import { PlusOutlined, CheckCircleOutlined, PauseCircleOutlined, PlayCircleOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons'
import { getPhonePage, addPhone, assignPhone, activatePhone, suspendPhone, resumePhone, recyclePhone, getPhoneById } from '../api/index.js'

const statusMap = {
  UNASSIGNED: { color: 'default', text: '未分配' },
  ASSIGNED: { color: 'blue', text: '已分配' },
  IN_USE: { color: 'green', text: '使用中' },
  SUSPENDED: { color: 'orange', text: '已停用' },
  RECYCLED: { color: 'red', text: '已回收' }
}

const PhoneList = () => {
  const [data, setData] = useState([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [assignModalVisible, setAssignModalVisible] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [form] = Form.useForm()
  const [assignForm] = Form.useForm()
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 })
  const [searchForm] = Form.useForm()

  const columns = [
    {
      title: '电话号码',
      dataIndex: 'phoneNumber',
      key: 'phoneNumber',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status) => {
        const s = statusMap[status] || { color: 'default', text: status }
        return <Tag color={s.color}>{s.text}</Tag>
      },
    },
    {
      title: '员工',
      dataIndex: 'employeeName',
      key: 'employeeName',
    },
    {
      title: '备注',
      dataIndex: 'remark',
      key: 'remark',
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space size="small">
          {record.status === 'UNASSIGNED' && (
            <Button
              type="link"
              size="small"
              onClick={() => handleAssign(record.id)}
            >
              分配
            </Button>
          )}
          {record.status === 'ASSIGNED' && (
            <Button
              type="link"
              size="small"
              icon={<CheckCircleOutlined />}
              onClick={() => handleActivate(record.id)}
            >
              激活
            </Button>
          )}
          {(record.status === 'ASSIGNED' || record.status === 'IN_USE') && (
            <Button
              type="link"
              size="small"
              icon={<PauseCircleOutlined />}
              onClick={() => handleSuspend(record.id)}
            >
              停用
            </Button>
          )}
          {record.status === 'SUSPENDED' && (
            <Button
              type="link"
              size="small"
              icon={<PlayCircleOutlined />}
              onClick={() => handleResume(record.id)}
            >
              恢复
            </Button>
          )}
          {(record.status === 'UNASSIGNED' || record.status === 'SUSPENDED') && (
            <Button
              type="link"
              size="small"
              icon={<ReloadOutlined />}
              onClick={() => handleRecycle(record.id)}
            >
              回收
            </Button>
          )}
        </Space>
      ),
    },
  ]

  const loadData = async (page = 1, pageSize = 10, params = {}) => {
    setLoading(true)
    try {
      const res = await getPhonePage({ current: page, size: pageSize, ...params })
      setData(res.data?.records || [])
      setPagination({
        current: page,
        pageSize,
        total: res.data?.total || 0
      })
    } catch (error) {
      console.error('加载号码列表失败:', error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [])

  const handleSearch = () => {
    const values = searchForm.getFieldsValue()
    loadData(1, 10, values)
  }

  const handleAdd = () => {
    setEditingId(null)
    form.resetFields()
    setModalVisible(true)
  }

  const handleAssign = (id) => {
    setEditingId(id)
    assignForm.resetFields()
    setAssignModalVisible(true)
  }

  const handleActivate = async (id) => {
    try {
      await activatePhone(id)
      message.success('激活成功')
      loadData(pagination.current, pagination.pageSize)
    } catch (error) {
      console.error('激活失败:', error)
    }
  }

  const handleSuspend = async (id) => {
    try {
      await suspendPhone(id)
      message.success('停用成功')
      loadData(pagination.current, pagination.pageSize)
    } catch (error) {
      console.error('停用失败:', error)
    }
  }

  const handleResume = async (id) => {
    try {
      await resumePhone(id)
      message.success('恢复成功')
      loadData(pagination.current, pagination.pageSize)
    } catch (error) {
      console.error('恢复失败:', error)
    }
  }

  const handleRecycle = async (id) => {
    try {
      await recyclePhone(id)
      message.success('回收成功')
      loadData(pagination.current, pagination.pageSize)
    } catch (error) {
      console.error('回收失败:', error)
    }
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      await addPhone(values)
      message.success('添加成功')
      setModalVisible(false)
      loadData(pagination.current, pagination.pageSize)
    } catch (error) {
      console.error('保存失败:', error)
    }
  }

  const handleAssignSubmit = async () => {
    try {
      const values = await assignForm.validateFields()
      await assignPhone(editingId, values)
      message.success('分配成功')
      setAssignModalVisible(false)
      loadData(pagination.current, pagination.pageSize)
    } catch (error) {
      console.error('分配失败:', error)
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
          <Form.Item name="phoneNumber" label="电话号码">
            <Input placeholder="请输入电话号码" />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select placeholder="请选择状态" allowClear>
              <Select.Option value="UNASSIGNED">未分配</Select.Option>
              <Select.Option value="ASSIGNED">已分配</Select.Option>
              <Select.Option value="IN_USE">使用中</Select.Option>
              <Select.Option value="SUSPENDED">已停用</Select.Option>
              <Select.Option value="RECYCLED">已回收</Select.Option>
            </Select>
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
          新增号码
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
        title="新增号码"
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="phoneNumber"
            label="电话号码"
            rules={[{ required: true, message: '请输入电话号码' }]}
          >
            <Input placeholder="请输入电话号码" />
          </Form.Item>
          <Form.Item
            name="remark"
            label="备注"
          >
            <Input.TextArea rows={3} placeholder="请输入备注" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="分配号码"
        open={assignModalVisible}
        onOk={handleAssignSubmit}
        onCancel={() => setAssignModalVisible(false)}
        destroyOnClose
      >
        <Form form={assignForm} layout="vertical">
          <Form.Item
            name="employeeId"
            label="员工ID"
            rules={[{ required: true, message: '请输入员工ID' }]}
          >
            <Input type="number" placeholder="请输入员工ID" />
          </Form.Item>
          <Form.Item
            name="employeeName"
            label="员工姓名"
            rules={[{ required: true, message: '请输入员工姓名' }]}
          >
            <Input placeholder="请输入员工姓名" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default PhoneList
