import { useState, useEffect } from 'react'
import { Table, Card, Input, Button, Space, Tag, Modal, Select, message, Popconfirm } from 'antd'
import {
  SearchOutlined,
  ReloadOutlined,
  KeyOutlined,
  StopOutlined,
  CheckCircleOutlined,
  DeleteOutlined,
} from '@ant-design/icons'
import { userApi, type UserVO } from '@/api/user'
import { request } from '@/api/request'

const UserManagement = () => {
  const [users, setUsers] = useState<UserVO[]>([])
  const [loading, setLoading] = useState(false)
  const [searchText, setSearchText] = useState('')
  const [roleModalOpen, setRoleModalOpen] = useState(false)
  const [currentUser, setCurrentUser] = useState<UserVO | null>(null)
  const [roles, setRoles] = useState<{ id: number; name: string; code: string }[]>([])
  const [selectedRoleId, setSelectedRoleId] = useState<number | null>(null)
  const [roleLoading, setRoleLoading] = useState(false)

  const fetchUsers = async () => {
    setLoading(true)
    try {
      const res = await userApi.getAll()
      setUsers(res.data.data || [])
    } catch (e: any) {
      message.error(e.response?.data?.message || '加载用户列表失败')
    } finally {
      setLoading(false)
    }
  }

  const fetchRoles = async () => {
    try {
      const res = await request.get('/roles/active')
      setRoles(res.data.data || [])
    } catch {
      // ignore
    }
  }

  useEffect(() => {
    fetchUsers()
  }, [])

  const handleResetPassword = async (id: number) => {
    try {
      await userApi.resetPassword(id)
      message.success('密码已重置为默认密码')
    } catch (e: any) {
      message.error(e.response?.data?.message || '重置失败')
    }
  }

  const handleToggleStatus = async (id: number, action: 'disable' | 'enable') => {
    try {
      if (action === 'disable') {
        await userApi.disable(id)
        message.success('用户已禁用')
      } else {
        await userApi.enable(id)
        message.success('用户已启用')
      }
      fetchUsers()
    } catch (e: any) {
      message.error(e.response?.data?.message || '操作失败')
    }
  }

  const handleDelete = async (id: number) => {
    try {
      await userApi.delete(id)
      message.success('用户已删除')
      fetchUsers()
    } catch (e: any) {
      message.error(e.response?.data?.message || '删除失败')
    }
  }

  const openRoleModal = (user: UserVO) => {
    setCurrentUser(user)
    setSelectedRoleId(user.roleId)
    fetchRoles()
    setRoleModalOpen(true)
  }

  const handleUpdateRole = async () => {
    if (!currentUser || !selectedRoleId) return
    setRoleLoading(true)
    try {
      await userApi.updateRole(currentUser.employeeId, selectedRoleId)
      message.success('角色已更新')
      setRoleModalOpen(false)
      fetchUsers()
    } catch (e: any) {
      message.error(e.response?.data?.message || '角色更新失败')
    } finally {
      setRoleLoading(false)
    }
  }

  const filteredUsers = users.filter(
    (u) =>
      !searchText ||
      u.name?.includes(searchText) ||
      u.username?.includes(searchText) ||
      u.orgName?.includes(searchText)
  )

  const columns = [
    { title: '用户名', dataIndex: 'username', key: 'username', width: 120 },
    { title: '姓名', dataIndex: 'name', key: 'name', width: 100 },
    { title: '所属组织', dataIndex: 'orgName', key: 'orgName', width: 150 },
    {
      title: '角色',
      dataIndex: 'roleName',
      key: 'roleName',
      width: 120,
      render: (v: string) => (v ? <Tag color="blue">{v}</Tag> : '-'),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (s: string) =>
        s === 1 ? <Tag color="success">启用</Tag> : <Tag color="default">禁用</Tag>,
    },
    {
      title: '操作',
      key: 'action',
      width: 320,
      render: (_: any, record: UserVO) => (
        <Space size="small">
          <Button
            size="small"
            icon={<KeyOutlined />}
            onClick={() => handleResetPassword(record.employeeId)}
          >
            重置密码
          </Button>
          <Button size="small" onClick={() => openRoleModal(record)}>
            分配角色
          </Button>
          {record.status === 1 ? (
            <Popconfirm
              title="确定禁用此用户？"
              onConfirm={() => handleToggleStatus(record.employeeId, 'disable')}
              okText="确定"
              cancelText="取消"
            >
              <Button size="small" danger icon={<StopOutlined />}>
                禁用
              </Button>
            </Popconfirm>
          ) : (
            <Button
              size="small"
              type="primary"
              icon={<CheckCircleOutlined />}
              onClick={() => handleToggleStatus(record.employeeId, 'enable')}
            >
              启用
            </Button>
          )}
          <Popconfirm
            title="确定删除此用户？"
            description="删除后不可恢复"
            onConfirm={() => handleDelete(record.employeeId)}
            okText="确定"
            cancelText="取消"
          >
            <Button size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <Card>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
          <Input
            placeholder="搜索用户名/姓名/组织"
            prefix={<SearchOutlined />}
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
            style={{ width: 300 }}
            allowClear
          />
          <Button icon={<ReloadOutlined />} onClick={fetchUsers}>
            刷新
          </Button>
        </div>

        <Table
          columns={columns}
          dataSource={filteredUsers}
          rowKey="employeeId"
          loading={loading}
          pagination={{ pageSize: 20, showSizeChanger: true, showTotal: (t) => `共 ${t} 条` }}
          scroll={{ x: 800 }}
        />
      </Card>

      <Modal
        title={`分配角色 - ${currentUser?.name || currentUser?.username}`}
        open={roleModalOpen}
        onOk={handleUpdateRole}
        onCancel={() => setRoleModalOpen(false)}
        confirmLoading={roleLoading}
        okText="确认"
        cancelText="取消"
      >
        <div style={{ marginTop: 16 }}>
          <div style={{ marginBottom: 8 }}>选择角色：</div>
          <Select
            value={selectedRoleId}
            onChange={setSelectedRoleId}
            style={{ width: '100%' }}
            placeholder="请选择角色"
            options={roles.map((r) => ({ value: r.id, label: r.name }))}
          />
        </div>
      </Modal>
    </div>
  )
}

export default UserManagement
