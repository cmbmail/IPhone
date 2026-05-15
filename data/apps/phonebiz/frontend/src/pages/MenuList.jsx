import React, { useEffect, useState } from 'react'
import { Table, Button, Modal, Form, Input, Select, message, Space, Popconfirm } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'
import { getMenuList, addMenu, updateMenu, deleteMenu, getMenuById } from '../api/index.js'

const MenuList = () => {
  const [data, setData] = useState([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [form] = Form.useForm()

  const columns = [
    {
      title: '菜单名称',
      dataIndex: 'menuName',
      key: 'menuName',
    },
    {
      title: '菜单编码',
      dataIndex: 'menuCode',
      key: 'menuCode',
    },
    {
      title: '菜单类型',
      dataIndex: 'menuType',
      key: 'menuType',
    },
    {
      title: '路由路径',
      dataIndex: 'path',
      key: 'path',
    },
    {
      title: '图标',
      dataIndex: 'icon',
      key: 'icon',
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
            title="确定要删除该菜单吗？"
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
      const res = await getMenuList()
      setData(res.data || [])
    } catch (error) {
      console.error('加载菜单列表失败:', error)
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
      const res = await getMenuById(id)
      form.setFieldsValue(res.data)
      setEditingId(id)
      setModalVisible(true)
    } catch (error) {
      console.error('加载菜单详情失败:', error)
    }
  }

  const handleDelete = async (id) => {
    try {
      await deleteMenu(id)
      message.success('删除成功')
      loadData()
    } catch (error) {
      console.error('删除菜单失败:', error)
    }
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      if (editingId) {
        await updateMenu({ ...values, id: editingId })
        message.success('更新成功')
      } else {
        await addMenu(values)
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
          新增菜单
        </Button>
      </div>

      <Table
        columns={columns}
        dataSource={data}
        rowKey="id"
        loading={loading}
      />

      <Modal
        title={editingId ? '编辑菜单' : '新增菜单'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="menuName"
            label="菜单名称"
            rules={[{ required: true, message: '请输入菜单名称' }]}
          >
            <Input placeholder="请输入菜单名称" />
          </Form.Item>
          <Form.Item
            name="menuCode"
            label="菜单编码"
            rules={[{ required: true, message: '请输入菜单编码' }]}
          >
            <Input placeholder="请输入菜单编码" />
          </Form.Item>
          <Form.Item
            name="menuType"
            label="菜单类型"
          >
            <Select placeholder="请选择菜单类型">
              <Select.Option value="directory">目录</Select.Option>
              <Select.Option value="menu">菜单</Select.Option>
              <Select.Option value="button">按钮</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item
            name="path"
            label="路由路径"
          >
            <Input placeholder="请输入路由路径" />
          </Form.Item>
          <Form.Item
            name="icon"
            label="图标"
          >
            <Input placeholder="请输入图标" />
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

export default MenuList
