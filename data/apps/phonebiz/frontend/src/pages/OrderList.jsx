import React, { useEffect, useState } from 'react'
import { Table, Button, Modal, Form, Input, Select, message, Space, Popconfirm, Tag, Card, Descriptions } from 'antd'
import { PlusOutlined, EyeOutlined, SearchOutlined } from '@ant-design/icons'
import { getOrderPage, addOrder, acceptOrder, approveOrder, rejectOrder, completeOrder, cancelOrder, reopenOrder, getOrderById } from '../api/index.js'

const statusMap = {
  PENDING: { color: 'orange', text: '待处理' },
  PROCESSING: { color: 'blue', text: '处理中' },
  APPROVED: { color: 'cyan', text: '已审批' },
  COMPLETED: { color: 'green', text: '已完成' },
  REJECTED: { color: 'red', text: '已拒绝' },
  CANCELLED: { color: 'default', text: '已取消' }
}

const typeMap = {
  NEW_PHONE: '新开号码',
  CHANGE_PHONE: '更换号码',
  CANCEL_PHONE: '注销号码',
  SUSPEND_PHONE: '停机',
  RESUME_PHONE: '复机'
}

const priorityMap = {
  1: { color: 'default', text: '普通' },
  2: { color: 'orange', text: '紧急' },
  3: { color: 'red', text: '加急' }
}

const OrderList = () => {
  const [data, setData] = useState([])
  const [loading, setLoading] = useState(false)
  const [createModalVisible, setCreateModalVisible] = useState(false)
  const [detailModalVisible, setDetailModalVisible] = useState(false)
  const [detailOrder, setDetailOrder] = useState(null)
  const [createForm] = Form.useForm()
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 })
  const [searchForm] = Form.useForm()

  const columns = [
    {
      title: '工单号',
      dataIndex: 'orderNo',
      key: 'orderNo',
    },
    {
      title: '工单类型',
      dataIndex: 'orderType',
      key: 'orderType',
      render: (type) => typeMap[type] || type,
    },
    {
      title: '标题',
      dataIndex: 'title',
      key: 'title',
    },
    {
      title: '电话号码',
      dataIndex: 'phoneNumber',
      key: 'phoneNumber',
    },
    {
      title: '优先级',
      dataIndex: 'priority',
      key: 'priority',
      render: (priority) => {
        const p = priorityMap[priority] || { color: 'default', text: priority }
        return <Tag color={p.color}>{p.text}</Tag>
      },
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
      title: '处理人',
      dataIndex: 'handlerName',
      key: 'handlerName',
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      render: (time) => time ? new Date(time).toLocaleString() : '-',
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => handleView(record.id)}
          >
            详情
          </Button>
          {record.status === 'PENDING' && (
            <Button
              type="link"
              size="small"
              onClick={() => handleAccept(record.id)}
            >
              受理
            </Button>
          )}
          {record.status === 'PENDING' && (
            <Popconfirm
              title="确定要取消该工单吗？"
              onConfirm={() => handleCancel(record.id)}
              okText="确定"
              cancelText="取消"
            >
              <Button type="link" size="small">
                取消
              </Button>
            </Popconfirm>
          )}
          {record.status === 'PROCESSING' && (
            <Button
              type="link"
              size="small"
              onClick={() => handleApprove(record.id)}
            >
              审批
            </Button>
          )}
          {record.status === 'PROCESSING' && (
            <Button
              type="link"
              size="small"
              onClick={() => handleReject(record.id)}
            >
              拒绝
            </Button>
          )}
          {record.status === 'APPROVED' && (
            <Button
              type="link"
              size="small"
              onClick={() => handleComplete(record.id)}
            >
              完成
            </Button>
          )}
          {(record.status === 'REJECTED' || record.status === 'CANCELLED') && (
            <Button
              type="link"
              size="small"
              onClick={() => handleReopen(record.id)}
            >
              重新打开
            </Button>
          )}
        </Space>
      ),
    },
  ]

  const loadData = async (page = 1, pageSize = 10, params = {}) => {
    setLoading(true)
    try {
      const res = await getOrderPage({ current: page, size: pageSize, ...params })
      setData(res.data?.records || [])
      setPagination({
        current: page,
        pageSize,
        total: res.data?.total || 0
      })
    } catch (error) {
      console.error('加载工单列表失败:', error)
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

  const handleCreate = () => {
    createForm.resetFields()
    createForm.setFieldsValue({ priority: 1 })
    setCreateModalVisible(true)
  }

  const handleView = async (id) => {
    try {
      const res = await getOrderById(id)
      setDetailOrder(res.data)
      setDetailModalVisible(true)
    } catch (error) {
      console.error('加载工单详情失败:', error)
    }
  }

  const handleAccept = async (id) => {
    const userId = localStorage.getItem('userId') || 1
    const realName = localStorage.getItem('realName') || '系统管理员'
    try {
      await acceptOrder(id, { handlerId: userId, handlerName: realName })
      message.success('受理成功')
      loadData(pagination.current, pagination.pageSize)
    } catch (error) {
      console.error('受理失败:', error)
    }
  }

  const handleApprove = async (id) => {
    Modal.confirm({
      title: '审批通过',
      content: (
        <Input.TextArea
          id="approveResult"
          rows={3}
          placeholder="请输入审批意见"
        />
      ),
      onOk: async () => {
        const result = document.getElementById('approveResult')?.value || ''
        try {
          await approveOrder(id, { result })
          message.success('审批成功')
          loadData(pagination.current, pagination.pageSize)
        } catch (error) {
          console.error('审批失败:', error)
        }
      }
    })
  }

  const handleReject = async (id) => {
    Modal.confirm({
      title: '拒绝工单',
      content: (
        <Input.TextArea
          id="rejectReason"
          rows={3}
          placeholder="请输入拒绝原因"
        />
      ),
      onOk: async () => {
        const reason = document.getElementById('rejectReason')?.value || ''
        try {
          await rejectOrder(id, { reason })
          message.success('拒绝成功')
          loadData(pagination.current, pagination.pageSize)
        } catch (error) {
          console.error('拒绝失败:', error)
        }
      }
    })
  }

  const handleComplete = async (id) => {
    Modal.confirm({
      title: '完成工单',
      content: (
        <Input.TextArea
          id="completeResult"
          rows={3}
          placeholder="请输入处理结果"
        />
      ),
      onOk: async () => {
        const result = document.getElementById('completeResult')?.value || ''
        try {
          await completeOrder(id, { result })
          message.success('完成成功')
          loadData(pagination.current, pagination.pageSize)
        } catch (error) {
          console.error('完成失败:', error)
        }
      }
    })
  }

  const handleCancel = async (id) => {
    try {
      await cancelOrder(id)
      message.success('取消成功')
      loadData(pagination.current, pagination.pageSize)
    } catch (error) {
      console.error('取消失败:', error)
    }
  }

  const handleReopen = async (id) => {
    try {
      await reopenOrder(id)
      message.success('重新打开成功')
      loadData(pagination.current, pagination.pageSize)
    } catch (error) {
      console.error('重新打开失败:', error)
    }
  }

  const handleCreateSubmit = async () => {
    try {
      const values = await createForm.validateFields()
      const realName = localStorage.getItem('realName') || '系统管理员'
      await addOrder({
        ...values,
        requesterName: realName
      })
      message.success('创建成功')
      setCreateModalVisible(false)
      loadData(pagination.current, pagination.pageSize)
    } catch (error) {
      console.error('创建失败:', error)
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
          <Form.Item name="orderNo" label="工单号">
            <Input placeholder="请输入工单号" />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select placeholder="请选择状态" allowClear>
              {Object.entries(statusMap).map(([key, value]) => (
                <Select.Option key={key} value={key}>{value.text}</Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="orderType" label="类型">
            <Select placeholder="请选择类型" allowClear>
              {Object.entries(typeMap).map(([key, value]) => (
                <Select.Option key={key} value={key}>{value}</Select.Option>
              ))}
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
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
          新建工单
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
        title="新建工单"
        open={createModalVisible}
        onOk={handleCreateSubmit}
        onCancel={() => setCreateModalVisible(false)}
        destroyOnClose
      >
        <Form form={createForm} layout="vertical">
          <Form.Item
            name="orderType"
            label="工单类型"
            rules={[{ required: true, message: '请选择工单类型' }]}
          >
            <Select placeholder="请选择工单类型">
              {Object.entries(typeMap).map(([key, value]) => (
                <Select.Option key={key} value={key}>{value}</Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item
            name="phoneId"
            label="电话号码ID"
          >
            <Input type="number" placeholder="请输入电话号码ID" />
          </Form.Item>
          <Form.Item
            name="phoneNumber"
            label="电话号码"
            rules={[{ required: true, message: '请输入电话号码' }]}
          >
            <Input placeholder="请输入电话号码" />
          </Form.Item>
          <Form.Item
            name="title"
            label="标题"
            rules={[{ required: true, message: '请输入标题' }]}
          >
            <Input placeholder="请输入标题" />
          </Form.Item>
          <Form.Item
            name="content"
            label="内容"
          >
            <Input.TextArea rows={3} placeholder="请输入内容" />
          </Form.Item>
          <Form.Item
            name="priority"
            label="优先级"
          >
            <Select>
              <Select.Option value={1}>普通</Select.Option>
              <Select.Option value={2}>紧急</Select.Option>
              <Select.Option value={3}>加急</Select.Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="工单详情"
        open={detailModalVisible}
        onCancel={() => setDetailModalVisible(false)}
        footer={null}
        width={700}
      >
        {detailOrder && (
          <Descriptions bordered column={2}>
            <Descriptions.Item label="工单号">{detailOrder.orderNo}</Descriptions.Item>
            <Descriptions.Item label="类型">{typeMap[detailOrder.orderType] || detailOrder.orderType}</Descriptions.Item>
            <Descriptions.Item label="标题">{detailOrder.title}</Descriptions.Item>
            <Descriptions.Item label="电话号码">{detailOrder.phoneNumber}</Descriptions.Item>
            <Descriptions.Item label="优先级">
              {priorityMap[detailOrder.priority]?.text || detailOrder.priority}
            </Descriptions.Item>
            <Descriptions.Item label="状态">
              {statusMap[detailOrder.status]?.text || detailOrder.status}
            </Descriptions.Item>
            <Descriptions.Item label="处理人">{detailOrder.handlerName || '-'}</Descriptions.Item>
            <Descriptions.Item label="结果">{detailOrder.result || '-'}</Descriptions.Item>
            <Descriptions.Item label="内容" span={2}>{detailOrder.content || '-'}</Descriptions.Item>
            <Descriptions.Item label="创建时间" span={2}>
              {detailOrder.createTime ? new Date(detailOrder.createTime).toLocaleString() : '-'}
            </Descriptions.Item>
          </Descriptions>
        )}
      </Modal>
    </div>
  )
}

export default OrderList
