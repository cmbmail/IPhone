import React, { useState, useEffect } from 'react'
import { request } from '../api/request'

import { Table, Button, Modal, Form, Input, Checkbox, Tag, Space, message, Popconfirm, Tooltip, Badge } from 'antd'
import { PlusCircleOutlined, EditOutlined, DeleteOutlined, SafetyCertificateOutlined, ReloadOutlined, UserOutlined, LockOutlined } from '@ant-design/icons'
import type { SysRole, SysPermission, CreateRoleDTO, UpdateRoleDTO } from '@/types/role'
import { roleApi } from '@/api/role'

const RoleManagement: React.FC = () => {
  const [roles, setRoles] = useState<SysRole[]>([])
  const [loading, setLoading] = useState(false)
  const [createModalOpen, setCreateModalOpen] = useState(false)
  const [editModalOpen, setEditModalOpen] = useState(false)
  const [permModalOpen, setPermModalOpen] = useState(false)
  const [userModalOpen, setUserModalOpen] = useState(false)
  const [editingRole, setEditingRole] = useState<SysRole | null>(null)
  const [allPermissions, setAllPermissions] = useState<Record<string, SysPermission[]>>({})
  const [rolePermissionIds, setRolePermissionIds] = useState<number[]>([])
  const [roleUsers, setRoleUsers] = useState<{ id: number; username: string; employee_no: string; role: string; status: string }[]>([])
  const [userLoading, setUserLoading] = useState(false)
  const [form] = Form.useForm()
  const [permForm] = Form.useForm()

  const fetchRoles = async () => {
    setLoading(true)
    try {
      const res = await roleApi.getAll()
      setRoles(res.data?.data || [])
    } catch {
      message.error('加载角色列表失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchRoles() }, [])

  const fetchAllPermissions = async () => {
    try {
      const res = await roleApi.getPermissionsByModule()
      setAllPermissions(res.data?.data || {})
    } catch {
      message.error('加载权限列表失败')
    }
  }

  const fetchRolePermissions = async (roleId: number) => {
    try {
      const res = await roleApi.getPermissions(roleId)
      const perms: SysPermission[] = res.data?.data || []
      setRolePermissionIds(perms.map(p => p.id))
    } catch {
      setRolePermissionIds([])
    }
  }

  // ======== Create / Edit Role ========

  const handleCreate = async () => {
    try {
      const values = await form.validateFields()
      const dto: CreateRoleDTO = {
        name: values.name,
        code: values.code,
        description: values.description,
      }
      await roleApi.create(dto)
      message.success('角色创建成功')
      setCreateModalOpen(false)
      form.resetFields()
      fetchRoles()
    } catch (e: any) {
      if (e?.response?.data?.message) message.error(e.response.data.message)
    }
  }

  const openEdit = (role: SysRole) => {
    setEditingRole(role)
    form.setFieldsValue({ name: role.name, description: role.description, status: role.status })
    setEditModalOpen(true)
  }

  const handleEdit = async () => {
    if (!editingRole) return
    try {
      const values = await form.validateFields()
      const dto: UpdateRoleDTO = {
        name: values.name,
        description: values.description,
        status: values.status,
      }
      await roleApi.update(editingRole.id, dto)
      message.success('角色更新成功')
      setEditModalOpen(false)
      form.resetFields()
      fetchRoles()
    } catch (e: any) {
      if (e?.response?.data?.message) message.error(e.response.data.message)
    }
  }

  const handleDelete = async (role: SysRole) => {
    try {
      await roleApi.delete(role.id)
      message.success('角色已删除')
      fetchRoles()
    } catch (e: any) {
      message.error(e?.response?.data?.message || '删除失败')
    }
  }

  // ======== Permission Assignment ========

  const openPermModal = async (role: SysRole) => {
    setEditingRole(role)
    await fetchAllPermissions()
    await fetchRolePermissions(role.id)
    setPermModalOpen(true)
  }

  const handlePermSave = async () => {
    if (!editingRole) return
    try {
      const dto: UpdateRoleDTO = { permission_ids: rolePermissionIds }
      await roleApi.update(editingRole.id, dto)
      message.success('权限分配已保存')
      setPermModalOpen(false)
    } catch {
      message.error('保存权限失败')
    }
  }

  const togglePermission = (permId: number, checked: boolean) => {
    if (checked) {
      setRolePermissionIds(prev => [...prev, permId])
    } else {
      setRolePermissionIds(prev => prev.filter(id => id !== permId))
    }
  }

  const toggleModule = (module: string, checked: boolean) => {
    const modulePermIds = (allPermissions[module] || []).map(p => p.id)
    if (checked) {
      const newIds = [...new Set([...rolePermissionIds, ...modulePermIds])]
      setRolePermissionIds(newIds)
    } else {
      setRolePermissionIds(prev => prev.filter(id => !modulePermIds.includes(id)))
    }
  }

  const isModuleAllChecked = (module: string) => {
    const modulePermIds = (allPermissions[module] || []).map(p => p.id)
    return modulePermIds.length > 0 && modulePermIds.every(id => rolePermissionIds.includes(id))
  }

  const isModulePartialChecked = (module: string) => {
    const modulePermIds = (allPermissions[module] || []).map(p => p.id)
    const checkedCount = modulePermIds.filter(id => rolePermissionIds.includes(id)).length
    return checkedCount > 0 && checkedCount < modulePermIds.length
  }

  // ======== View Users ========

  const openUserModal = async (role: SysRole) => {
    setEditingRole(role)
    setUserModalOpen(true)
    setUserLoading(true)
    try {
      // Use request utility instead of raw fetch
      const res = await request.get('/roles/' + role.id + '/user-count')
      const countData = await res.json()
      const count = countData?.data
      if (count > 0) {
        // Query users by role_id via /employees endpoint is not available
        // We'll show the count only for now
        setRoleUsers([{ id: 0, username: '', employee_no: '', role: '', status: '' }] as any)
      } else {
        setRoleUsers([])
      }
    } catch {
      setRoleUsers([])
    } finally {
      setUserLoading(false)
    }
  }

  // ======== Columns ========

  const columns = [
    {
      title: '角色名称',
      dataIndex: 'name',
      key: 'name',
      render: (name: string, record: SysRole) => (
        <Space>
          {record.is_system ? <LockOutlined style={{ color: '#faad14' }} /> : <SafetyCertificateOutlined style={{ color: '#1677ff' }} />}
          <span style={{ fontWeight: 500 }}>{name}</span>
          {record.is_system && <Tag color="orange">系统</Tag>}
        </Space>
      )
    },
    { title: '编码', dataIndex: 'code', key: 'code', width: 120, render: (v: string) => <code style={{ background: '#f5f5f5', padding: '1px 6px', borderRadius: 3, fontSize: 12 }}>{v}</code> },
    { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true, render: (v: string) => v || '-' },
    {
      title: '状态', dataIndex: 'status', key: 'status', width: 80,
      render: (s: string) => s === 'active' ? <Badge status="success" text="启用" /> : <Badge status="default" text="停用" />
    },
    {
      title: '操作', key: 'actions', width: 260, fixed: 'right' as const,
      render: (_: any, record: SysRole) => (
        <Space size={4}>
          <Tooltip title="分配权限"><Button size="small" type="link" icon={<SafetyCertificateOutlined />} onClick={() => openPermModal(record)}>权限</Button></Tooltip>
          <Button size="small" type="link" icon={<EditOutlined />} onClick={() => openEdit(record)}>编辑</Button>
          {!record.is_system && (
            <Popconfirm title="确认删除该角色？" description="删除后不可恢复" onConfirm={() => handleDelete(record)} okText="删除" cancelText="取消" okButtonProps={{ danger: true }}>
              <Button size="small" type="link" danger icon={<DeleteOutlined />}>删除</Button>
            </Popconfirm>
          )}
        </Space>
      )
    }
  ]

  const permCountMap: Record<number, number> = {}

  const totalPermissions = Object.values(allPermissions).flat().length

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>角色管理</h2>
        <Space>
          <Button icon={<PlusCircleOutlined />} type="primary" onClick={() => { form.resetFields(); setCreateModalOpen(true) }}>创建角色</Button>
          <Button icon={<ReloadOutlined />} onClick={fetchRoles} loading={loading}>刷新</Button>
        </Space>
      </div>

      <Table
        columns={columns}
        dataSource={roles}
        loading={loading}
        rowKey="id"
        pagination={false}
        locale={{ emptyText: '暂无角色数据' }}
      />

      {/* Create Role Modal */}
      <Modal title="创建角色" open={createModalOpen} onOk={handleCreate} onCancel={() => { setCreateModalOpen(false); form.resetFields() }} okText="创建" destroyOnClose width={500}>
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item name="name" label="角色名称" rules={[{ required: true, message: '请输入角色名称' }]}>
            <Input placeholder="例如：区域经理" maxLength={50} />
          </Form.Item>
          <Form.Item name="code" label="角色编码" rules={[
            { required: true, message: '请输入角色编码' },
            { pattern: /^[a-zA-Z][a-zA-Z0-9_]{1,49}$/, message: '以字母开头，只允许字母数字下划线' }
          ]}>
            <Input placeholder="例如：region_manager" maxLength={50} />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea placeholder="角色描述" rows={3} maxLength={200} />
          </Form.Item>
        </Form>
      </Modal>

      {/* Edit Role Modal */}
      <Modal title={editingRole ? `编辑「${editingRole.name}」` : '编辑角色'} open={editModalOpen} onOk={handleEdit} onCancel={() => { setEditModalOpen(false); form.resetFields() }} okText="保存" destroyOnClose width={500}>
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item name="name" label="角色名称" rules={[{ required: true, message: '请输入角色名称' }]}>
            <Input placeholder="角色名称" maxLength={50} />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea placeholder="角色描述" rows={3} maxLength={200} />
          </Form.Item>
          <Form.Item name="status" label="状态" rules={[{ required: true }]}>
            <select style={{ width: '100%', padding: '4px 11px', border: '1px solid #d9d9d9', borderRadius: 6, height: 32 }}>
              <option value="active">启用</option>
              <option value="inactive">停用</option>
            </select>
          </Form.Item>
        </Form>
      </Modal>

      {/* Permission Assignment Modal */}
      <Modal
        title={editingRole ? `权限分配 — ${editingRole.name}` : '权限分配'}
        open={permModalOpen}
        onCancel={() => setPermModalOpen(false)}
        onOk={handlePermSave}
        okText="保存"
        width={600}
        destroyOnClose
      >
        <div style={{ marginBottom: 12, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span style={{ color: '#666' }}>
            已选择 <strong style={{ color: '#1677ff' }}>{rolePermissionIds.length}</strong> / {totalPermissions} 项权限
          </span>
          <Space>
            <Button size="small" onClick={() => setRolePermissionIds(Object.values(allPermissions).flat().map(p => p.id))}>全选</Button>
            <Button size="small" onClick={() => setRolePermissionIds([])}>清空</Button>
          </Space>
        </div>
        <div style={{ maxHeight: 400, overflow: 'auto', border: '1px solid #f0f0f0', borderRadius: 6, padding: 12 }}>
          {Object.entries(allPermissions).map(([module, perms]) => (
            <div key={module} style={{ marginBottom: 16 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8, paddingBottom: 6, borderBottom: '1px solid #f5f5f5' }}>
                <Checkbox
                  checked={isModuleAllChecked(module)}
                  indeterminate={isModulePartialChecked(module)}
                  onChange={(e) => toggleModule(module, e.target.checked)}
                />
                <strong style={{ fontSize: 14 }}>{module}</strong>
                <span style={{ color: '#999', fontSize: 12 }}>({perms.filter(p => rolePermissionIds.includes(p.id)).length}/{perms.length})</span>
              </div>
              <div style={{ paddingLeft: 24, display: 'flex', flexWrap: 'wrap', gap: '4px 16px' }}>
                {perms.map(perm => (
                  <Checkbox
                    key={perm.id}
                    checked={rolePermissionIds.includes(perm.id)}
                    onChange={(e) => togglePermission(perm.id, e.target.checked)}
                  >
                    {perm.name}
                  </Checkbox>
                ))}
              </div>
            </div>
          ))}
        </div>
      </Modal>

      {/* View Users Modal (simplified) */}
      <Modal
        title={editingRole ? `「${editingRole.name}」用户列表` : '用户列表'}
        open={userModalOpen}
        onCancel={() => setUserModalOpen(false)}
        footer={<Button onClick={() => setUserModalOpen(false)}>关闭</Button>}
        width={500}
      >
        <div style={{ textAlign: 'center', padding: 24, color: '#999' }}>
          <UserOutlined style={{ fontSize: 36, marginBottom: 12 }} />
          <p>用户角色分配功能将在后续版本中完善</p>
          <p style={{ fontSize: 12 }}>当前通过员工管理为用户分配角色</p>
        </div>
      </Modal>
    </div>
  )
}

export default RoleManagement
