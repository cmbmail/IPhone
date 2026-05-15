import React, { useEffect, useState } from 'react'
import { Table, Button, Modal, Form, Input, Select, message, Space, Popconfirm } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'
import { getOrgList, addOrg, updateOrg, deleteOrg, getOrgById } from '../api/index.js'

const OrgList = () => {
  const [data, setData] = useState([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [form] = Form.useForm()

  const columns = [
    {
      title: '组织名称',
      dataIndex: 'orgName',
      key: 'orgName',
    },
    {
      title: '组织编码',
      dataIndex: 'orgCode',
      key: 'orgCode',
    },
    {
      title: '组织层级',
      dataIndex: 'orgLevel',
      key: 'orgLevel',
    },
    {
      title: '排序',
      dataIndex: 'sort',
      key: 'sort',
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
      title: '备注',
      dataIndex: 'remark',
      key: 'remark',
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
            title="确定要删除该组织吗？"
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

  const loadData = async () => {
    setLoading(true)
    try {
      const res = await getOrgList()
      setData(res.data || [])
    } catch (error) {
      console.error('加载组织列表失败:', error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [])

  const handleAdd = () => {
    setEditingId(null)
    form.resetFields()
    setModalVisible(true)
  }

  const handleEdit = async (id) => {
    try {
      const res = await getOrgById(id)
      form.setFieldsValue(res.data)
      setEditingId(id)
      setModalVisible(true)
    } catch (error) {
      console.error('加载组织详情失败:', error)
    }
  }

  const handleDelete = async (id) => {
    try {
      await deleteOrg(id)
      message.success('删除成功')
      loadData()
    } catch (error) {
      console.error('删除组织失败:', error)
    }
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      if (editingId) {
        await updateOrg({ ...values, id: editingId })
        message.success('更新成功')
      } else {
        await addOrg(values)
        message.success('添加成功')
      }
      setModalVisible(false)
      loadData()
    } catch (error) {
      console.error('保存失败:', error)
    }
  }

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
          新增组织
        </Button>
      </div>

      <Table
        columns={columns}
        dataSource={data}
        rowKey="id"
        loading={loading}
      />

      <Modal
        title={editingId ? '编辑组织' : '新增组织'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="orgName"
            label="组织名称"
            rules={[{ required: true, message: '请输入组织名称' }]}
          >
            <Input placeholder="请输入组织名称" />
          </Form.Item>
          <Form.Item
            name="orgCode"
            label="组织编码"
            rules={[{ required: true, message: '请输入组织编码' }]}
          >
            <Input placeholder="请输入组织编码" />
          </Form.Item>
          <Form.Item
            name="orgLevel"
            label="组织层级"
            initialValue={1}
          >
            <Select>
              <Select.Option value={1}>一级</Select.Option>
              <Select.Option value={2}>二级</Select.Option>
              <Select.Option value={3}>三级</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item
            name="sort"
            label="排序"
            initialValue={0}
          >
            <Input type="number" placeholder="请输入排序" />
          </Form.Item>
          <Form.Item
            name="status"
            label="状态"
            initialValue={1}
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

export default OrgList
