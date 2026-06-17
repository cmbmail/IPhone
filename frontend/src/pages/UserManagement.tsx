import { useState } from 'react'
import { Table, Card, Input, Button, Space, Tag, Modal, Select, message, Popconfirm } from 'antd'
import {
  SearchOutlined,
  ReloadOutlined,
  KeyOutlined,
  StopOutlined,
  CheckCircleOutlined,
  DeleteOutlined,
} from '@ant-design/icons'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { userApi, type UserVO } from '@/api/user'
import { roleApi } from '@/api/role'

const UserManagement = () => {
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(20)
  const [searchText, setSearchText] = useState('')
  const [searchKey, setSearchKey] = useState('')
  const [roleModalOpen, setRoleModalOpen] = useState(false)
  const [currentUser, setCurrentUser] = useState<UserVO | null>(null)
  const [selectedRoleId, setSelectedRoleId] = useState<number | null>(null)

  const queryClient = useQueryClient()

  // Server-side paged query
  const { data: pagedData, isLoading } = useQuery({
    queryKey: ['users-paged', page, pageSize, searchKey],
    queryFn: () => userApi.getPaged({ page, size: pageSize, keyword: searchKey || undefined }),
  })

  const { data: roles } = useQuery({
    queryKey: ['roles-active'],
    queryFn: () => roleApi.getActive(),
  })

  const users: UserVO[] = (pagedData as any)?.content || []
  const total: number = (pagedData as any)?.totalElements || 0

  const refresh = () => queryClient.invalidateQueries({ queryKey: ['users-paged'] })

  const handleSearch = (value: string) => {
    setSearchKey(value)
    setPage(0)
  }

  const handleResetPassword = useMutation({
    mutationFn: (employeeId: number) => userApi.resetPassword(employeeId),
    onSuccess: () => message.success('密码已重置为默认密码'),
    onError: (e: unknown) => message.error(e instanceof Error ? e.message : '重置失败'),
  })

  const handleToggleStatus = useMutation({
    mutationFn: ({ id, action }: { id: number; action: 'disable' | 'enable' }) =>
      action === 'disable' ? userApi.disable(id) : userApi.enable(id),
    onSuccess: () => { message.success('操作成功'); refresh() },
    onError: (e: unknown) => message.error(e instanceof Error ? e.message : '操作失败'),
  })

  const handleDelete = useMutation({
    mutationFn: (employeeId: number) => userApi.delete(employeeId),
    onSuccess: () => { message.success('用户已删除'); refresh() },
    onError: (e: unknown) => message.error(e instanceof Error ? e.message : '删除失败'),
  })

  const handleUpdateRole = useMutation({
    mutationFn: () => {
      if (!currentUser || !selectedRoleId) throw new Error('Missing data')
      return userApi.updateRole(currentUser.employeeId, selectedRoleId)
    },
    onSuccess: () => { message.success('角色已更新'); setRoleModalOpen(false); refresh() },
    onError: (e: unknown) => message.error(e instanceof Error ? e.message : '角色更新失败'),
  })

  const openRoleModal = (user: UserVO) => {
    setCurrentUser(user)
    setSelectedRoleId(user.roleId)
    setRoleModalOpen(true)
  }

  const columns = [
    { title: '用户名', dataIndex: 'username', key: 'username', width: 120 },
    { title: '姓名', dataIndex: 'name', key: 'name', width: 100 },
    { title: '所属组织', dataIndex: 'orgName', key: 'orgName', width: 150 },
    {
      title: '角色', dataIndex: 'roleName', key: 'roleName', width: 120,
      render: (v: string) => v ? <Tag color="blue">{v}</Tag> : '-',
    },
    {
      title: '状态', dataIndex: 'status', key: 'status', width: 80,
      render: (s: number) => s === 1 ? <Tag color="success">启用</Tag> : <Tag color="default">禁用</Tag>,
    },
    {
      title: '操作', key: 'action', width: 320,
      render: (_: unknown, record: UserVO) => (
        <Space size="small">
          <Button size="small" icon={<KeyOutlined />} onClick={() => handleResetPassword.mutate(record.employeeId)}>重置密码</Button>
          <Button size="small" onClick={() => openRoleModal(record)}>分配角色</Button>
          {record.status === 1 ? (
            <Popconfirm title="确定禁用此用户？" onConfirm={() => handleToggleStatus.mutate({ id: record.employeeId, action: 'disable' })} okText="确定" cancelText="取消">
              <Button size="small" danger icon={<StopOutlined />}>禁用</Button>
            </Popconfirm>
          ) : (
            <Button size="small" type="primary" icon={<CheckCircleOutlined />} onClick={() => handleToggleStatus.mutate({ id: record.employeeId, action: 'enable' })}>启用</Button>
          )}
          <Popconfirm title="确定删除此用户？" description="删除后不可恢复" onConfirm={() => handleDelete.mutate(record.employeeId)} okText="确定" cancelText="取消">
            <Button size="small" danger icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <Card>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
          <Input.Search
            placeholder="搜索用户名/姓名/工号"
            prefix={<SearchOutlined />}
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
            onSearch={handleSearch}
            style={{ width: 300 }}
            allowClear
          />
          <Button icon={<ReloadOutlined />} onClick={() => refresh()}>刷新</Button>
        </div>

        <Table
          columns={columns}
          dataSource={users}
          rowKey="employeeId"
          loading={isLoading}
          scroll={{ x: 800 }}
          pagination={{
            current: page + 1,
            pageSize,
            total,
            onChange: (p, ps) => { setPage(p - 1); setPageSize(ps) },
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (t) => `共 ${t} 条`,
          }}
        />
      </Card>

      <Modal
        title={`分配角色 - ${currentUser?.name || currentUser?.username}`}
        open={roleModalOpen}
        onOk={() => handleUpdateRole.mutate()}
        onCancel={() => setRoleModalOpen(false)}
        confirmLoading={handleUpdateRole.isPending}
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
            options={(roles || []).map((r) => ({ value: r.id, label: r.name }))}
          />
        </div>
      </Modal>
    </div>
  )
}

export default UserManagement
