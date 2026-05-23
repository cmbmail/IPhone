import React, { useState, useMemo } from 'react'
import {
  Tree,
  Button,
  Modal,
  Form,
  Input,
  Select,
  Space,
  message,
  Popconfirm,
  Tooltip,
  Table,
  Tag,
} from 'antd'
import {
  EditOutlined,
  ApartmentOutlined,
  TeamOutlined,
  KeyOutlined,
  StopOutlined,
  DeleteOutlined,
  CheckCircleOutlined,
} from '@ant-design/icons'
import type { DataNode } from 'antd/es/tree'
import type { OrgStructure } from '@/types/org'
import { orgApi } from '@/api/org'
import { userApi, type UserVO } from '@/api/user'
import { roleApi } from '@/api/role'

const OrgManagement: React.FC = () => {
  const [treeData, setTreeData] = useState<OrgStructure[]>([])
  const [loading, setLoading] = useState(false)
  const [expandedKeys, setExpandedKeys] = useState<string[]>([])
  const [selectedOrgId, setSelectedOrgId] = useState<number | null>(null)
  const [selectedOrgName, setSelectedOrgName] = useState<string>('')

  // Org modals
  const [addChildOpen, setAddChildOpen] = useState(false)
  const [editModalOpen, setEditModalOpen] = useState(false)
  const [editingOrg, setEditingOrg] = useState<OrgStructure | null>(null)
  const [orgForm] = Form.useForm()

  // User modals
  const [editUserOpen, setEditUserOpen] = useState(false)
  const [editingUser, setEditingUser] = useState<UserVO | null>(null)
  const [userForm] = Form.useForm()
  const [activeRoles, setActiveRoles] = useState<{ id: number; name: string; code: string }[]>([])

  React.useEffect(() => {
    roleApi
      .getActive()
      .then((res) => setActiveRoles(res.data?.data || []))
      .catch(() => {})
  }, [])
  const [userListLoading, setUserListLoading] = useState(false)
  const [userList, setUserList] = useState<UserVO[]>([])

  // ======== Org Tree Logic ========

  const fetchTree = async () => {
    setLoading(true)
    try {
      const res = await orgApi.getTree()
      setTreeData(res.data?.data || [])
    } catch {
      message.error('加载组织架构失败')
    } finally {
      setLoading(false)
    }
  }

  React.useEffect(() => {
    fetchTree()
    fetchAllUsers()
  }, [])

  const getAllKeys = (nodes: OrgStructure[]): string[] => {
    const keys: string[] = []
    const collect = (ns: OrgStructure[]) => {
      for (const n of ns) {
        keys.push(String(n.id))
        if (n.children) collect(n.children)
      }
    }
    collect(nodes)
    return keys
  }

  const convertToAntTree = (nodes: OrgStructure[]): DataNode[] => {
    return nodes.map((node) => {
      const isSelected = node.id === selectedOrgId
      return {
        key: String(node.id),
        title: (
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '2px 0' }}>
            <ApartmentOutlined />
            <span
              style={{
                fontWeight: isSelected ? 600 : 500,
                color: isSelected ? '#1677ff' : undefined,
              }}
            >
              {node.name}
            </span>
            <Tooltip title="编辑">
              <Button
                type="link"
                size="small"
                icon={<EditOutlined />}
                onClick={(e) => {
                  e.stopPropagation()
                  openEditModal(node)
                }}
              />
            </Tooltip>
          </div>
        ),
        children:
          node.children && node.children.length > 0 ? convertToAntTree(node.children) : undefined,
      }
    })
  }

  const expandedAllKeys = useMemo(() => getAllKeys(treeData), [treeData])
  React.useEffect(() => {
    setExpandedKeys(expandedAllKeys)
  }, [expandedAllKeys])
  const antTreeData = useMemo(() => convertToAntTree(treeData), [treeData, selectedOrgId])

  const nameInputRef = React.useRef<any>(null)

  const openAddChildForm = () => {
    orgForm.resetFields()
    orgForm.setFieldsValue({ name: '' })
    setAddChildOpen(true)
    setTimeout(() => nameInputRef.current?.focus(), 100)
  }

  const openEditModal = (org: OrgStructure) => {
    setEditingOrg(org)
    let position = 1
    const siblings =
      org.parentId === null || org.parentId === undefined
        ? treeData
        : treeData.find((p) => p.id === org.parentId)?.children || []
    const sorted = [...siblings].sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
    const idx = sorted.findIndex((s) => s.id === org.id)
    if (idx >= 0) position = idx + 1
    orgForm.setFieldsValue({
      name: org.name,
      sort_order: position,
      branch_name: org.branchName,
      org_code: org.orgCode,
      cost_center_code: org.costCenterCodeCode,
    })
    setEditModalOpen(true)
  }

  const handleAdd = async () => {
    try {
      const values = await orgForm.validateFields()
      await orgApi.create({ parent_id: editingOrg?.id ?? null, name: values.name } as Record<
        string,
        unknown
      >)
      message.success(`已在「${editingOrg?.name || ''}」下添加「${values.name}」`)
      setAddChildOpen(false)
      setEditModalOpen(false)
      fetchTree()
    } catch {
      /* validation */
    }
  }

  const handleEdit = async () => {
    if (!editingOrg) return
    try {
      const values = await orgForm.validateFields()
      await orgApi.update(editingOrg.id, {
        name: values.name,
        sort_order: Number(values.sortOrder),
      } as Record<string, unknown>)
      message.success('更新成功')
      setEditModalOpen(false)
      fetchTree()
    } catch {
      /* validation */
    }
  }

  const handleDelete = async (id: number) => {
    try {
      await orgApi.delete(id)
      message.success('删除成功')
      if (selectedOrgId === id) {
        setSelectedOrgId(null)
        setSelectedOrgName('')
        setUserList([])
      }
      fetchTree()
    } catch (e: any) {
      message.error(e?.response?.data?.message || '删除失败')
    }
  }
  // ======== User List Logic ========

  const fetchAllUsers = async () => {
    setUserListLoading(true)
    try {
      const res = await userApi.getAll()
      setUserList(res.data?.data || [])
    } catch {
      setUserList([])
    } finally {
      setUserListLoading(false)
    }
  }

  const fetchUsers = async (orgId: number) => {
    setUserListLoading(true)
    try {
      const res = await userApi.getByOrg(orgId)
      setUserList(res.data?.data || [])
    } catch {
      setUserList([])
    } finally {
      setUserListLoading(false)
    }
  }

  const handleTreeSelect = (selectedKeys: React.Key[]) => {
    if (selectedKeys.length > 0) {
      const orgId = Number(selectedKeys[0])
      setSelectedOrgId(orgId)
      const findName = (nodes: OrgStructure[]): string => {
        for (const n of nodes) {
          if (n.id === orgId) return n.name
          if (n.children) {
            const found = findName(n.children)
            if (found) return found
          }
        }
        return ''
      }
      setSelectedOrgName(findName(treeData))
      fetchUsers(orgId)
    } else {
      setSelectedOrgId(null)
      setSelectedOrgName('')
      fetchAllUsers()
    }
  }

  const handleRoleChange = async (roleId: number) => {
    if (!editingUser) return
    try {
      await userApi.updateRole(editingUser.employeeId, roleId)
      message.success('角色变更成功')
      if (selectedOrgId) {
        fetchUsers(selectedOrgId)
      } else {
        fetchAllUsers()
      }
    } catch (e: any) {
      message.error(e?.response?.data?.message || '角色变更失败')
    }
  }

  const openUserEdit = (user: UserVO) => {
    setEditingUser(user)
    userForm.setFieldsValue({ username: user.username, orgId: user.orgId, roleId: user.roleId })
    setEditUserOpen(true)
  }

  const handleDeptChange = async (newOrgId: number) => {
    if (!editingUser) return
    try {
      await userApi.updateDepartment(editingUser.employeeId, newOrgId)
      message.success('部门变更成功')
      if (selectedOrgId) {
        fetchUsers(selectedOrgId)
      } else {
        fetchAllUsers()
      }
    } catch (e: any) {
      message.error(e?.response?.data?.message || '部门变更失败')
    }
  }

  const handleResetPassword = (user: UserVO) => {
    Modal.confirm({
      title: '重置密码',
      content: `确定要重置用户「${user.name}」的密码吗？重置后默认密码为 Password@123，用户需在下次登录时修改。`,
      onOk: async () => {
        try {
          await userApi.resetPassword(user.employeeId)
          message.success('密码已重置')
        } catch (e: any) {
          message.error(e?.response?.data?.message || '重置失败')
        }
      },
    })
  }

  const handleToggleStatus = async (user: UserVO) => {
    const isActive = user.status === 1
    const action = isActive ? '禁用' : '启用'
    Modal.confirm({
      title: `${action}账号`,
      content: `确定要${action}用户「${user.name}」的账号吗？`,
      okType: isActive ? 'danger' : 'primary',
      onOk: async () => {
        try {
          if (isActive) await userApi.disable(user.employeeId)
          else await userApi.enable(user.employeeId)
          message.success(`账号已${action}`)
          if (selectedOrgId) {
            fetchUsers(selectedOrgId)
          } else {
            fetchAllUsers()
          }
        } catch (e: any) {
          message.error(e?.response?.data?.message || `${action}失败`)
        }
      },
    })
  }

  const handleDeleteUser = (user: UserVO) => {
    Modal.confirm({
      title: '删除账号',
      content: `确定要删除用户「${user.name}」吗？删除后该账号将无法登录，此操作不可撤销。`,
      okType: 'danger',
      onOk: async () => {
        try {
          await userApi.delete(user.employeeId)
          message.success('账号已删除')
          if (selectedOrgId) {
            fetchUsers(selectedOrgId)
          } else {
            fetchAllUsers()
          }
        } catch (e: any) {
          message.error(e?.response?.data?.message || '删除失败')
        }
      },
    })
  }

  // Flatten all orgs for dept selector
  const allOrgs = useMemo(() => {
    const flat: { id: number; name: string; level: number }[] = []
    const collect = (nodes: OrgStructure[], level: number) => {
      for (const n of nodes) {
        flat.push({ id: n.id, name: n.name, level })
        if (n.children) collect(n.children, level + 1)
      }
    }
    collect(treeData, 0)
    return flat
  }, [treeData])

  const userColumns = [
    { title: '姓名', dataIndex: 'name', key: 'name', width: 100, align: 'center' },
    {
      title: '账号',
      dataIndex: 'username',
      key: 'username',
      width: 130,
      align: 'center',
      render: (v: string, _record: UserVO) => (
        <span style={{ fontFamily: 'monospace', fontSize: 13 }}>{v}</span>
      ),
    },
    { title: '部门', dataIndex: 'orgName', key: 'orgName', width: 120, align: 'center' },
    {
      title: '角色',
      dataIndex: 'roleName',
      key: 'roleName',
      width: 100,
      align: 'center',
      render: (v: string) => v || '-',
    },
    {
      title: '修改时间',
      dataIndex: 'updatedAt',
      key: 'updatedAt',
      width: 170,
      align: 'center',
      render: (v: string) =>
        v
          ? new Date(v).toLocaleString('zh-CN', {
              month: '2-digit',
              day: '2-digit',
              hour: '2-digit',
              minute: '2-digit',
            })
          : '-',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 70,
      align: 'center',
      render: (s: string) =>
        s === 1 ? <Tag color="green">启用</Tag> : <Tag color="red">禁用</Tag>,
    },
    {
      title: '操作',
      key: 'actions',
      width: 80,
      align: 'center',
      fixed: 'right' as const,
      render: (_: any, record: UserVO) => (
        <Button type="link" size="small" onClick={() => openUserEdit(record)}>
          操作
        </Button>
      ),
    },
  ]

  return (
    <div style={{ padding: 0 }}>
      <div style={{ marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>组织架构</h2>
      </div>

      <div style={{ display: 'flex', gap: 16 }}>
        {/* Left: Org Tree */}
        <div
          style={{
            flex: '0 0 280px',
            background: '#fff',
            borderRadius: 8,
            padding: '16px 8px',
            border: '1px solid #f0f0f0',
            minHeight: 400,
            overflow: 'auto',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 8 }}></div>
          {treeData.length === 0 && !loading ? (
            <div style={{ textAlign: 'center', padding: 60, color: '#999' }}>
              <ApartmentOutlined style={{ fontSize: 48, marginBottom: 16, color: '#d9d9d9' }} />
              <p>暂无组织架构数据</p>
            </div>
          ) : (
            <Tree
              showLine={{ showLeafIcon: false }}
              showIcon={false}
              blockNode
              treeData={antTreeData}
              expandedKeys={expandedKeys}
              onExpand={(keys) => setExpandedKeys(keys as string[])}
              selectedKeys={selectedOrgId ? [String(selectedOrgId)] : []}
              onSelect={handleTreeSelect}
            />
          )}
        </div>

        {/* Right: User List */}
        <div
          style={{
            flex: 1,
            background: '#fff',
            borderRadius: 8,
            border: '1px solid #f0f0f0',
            minHeight: 400,
            display: 'flex',
            flexDirection: 'column',
          }}
        >
          <>
            <div
              style={{
                padding: '12px 16px',
                borderBottom: '1px solid #f0f0f0',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <TeamOutlined style={{ color: '#1677ff' }} />
                <span style={{ fontWeight: 600 }}>{selectedOrgName || '全部用户'}</span>
                <span style={{ color: '#999', fontSize: 13 }}>用户列表</span>
                <Tag color="blue">{userList.length} 人</Tag>
              </div>
            </div>
            <div style={{ flex: 1, overflow: 'auto' }}>
              <Table
                columns={userColumns}
                dataSource={userList}
                loading={userListLoading}
                rowKey="employeeId"
                size="small"
                pagination={false}
                scroll={{ x: 800 }}
                locale={{ emptyText: '该组织下暂无用户' }}
              />
            </div>
          </>
        </div>
      </div>

      {/* Edit Org Modal */}
      <Modal
        title={
          addChildOpen ? `在「${editingOrg?.name}」下添加子机构` : `编辑「${editingOrg?.name}」`
        }
        open={editModalOpen}
        onCancel={() => {
          setEditModalOpen(false)
          setAddChildOpen(false)
          setEditModalOpen(false)
        }}
        footer={null}
        destroyOnClose
        width={440}
      >
        <Form form={orgForm} layout="vertical" preserve={false}>
          <Form.Item label="机构名称" required>
            <div style={{ display: 'flex', gap: 8 }}>
              <Form.Item
                name="name"
                noStyle
                rules={[{ required: true, message: '请输入机构名称' }]}
              >
                <Input ref={nameInputRef} placeholder="请输入机构名称" autoComplete="off" />
              </Form.Item>
              {!addChildOpen && (
                <Button type="dashed" onClick={openAddChildForm}>
                  添加子机构
                </Button>
              )}
            </div>
          </Form.Item>
          {!addChildOpen && (
            <>
              <Form.Item
                name="sort_order"
                label="排序位置"
                rules={[{ required: true, message: '请输入排序位置' }]}
                tooltip="该节点在同级中的排列位置（数字越小越靠前）。修改后自动重排同级节点"
              >
                <Input type="number" min={1} placeholder="第几个" style={{ width: 200 }} />
              </Form.Item>
              <Form.Item name="branch_name" label="分行">
                <Input placeholder="如：武汉分行" />
              </Form.Item>
              <Form.Item name="org_code" label="组织机构代码">
                <Input placeholder="如：914201007178" />
              </Form.Item>
              <Form.Item name="cost_center" label="成本中心">
                <Input placeholder="如：CC-WH-001" />
              </Form.Item>
            </>
          )}
        </Form>
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginTop: 8,
          }}
        >
          <div></div>
          <Space>
            {!addChildOpen && (
              <Popconfirm
                title={`确定删除「${editingOrg?.name}」？`}
                description="请先确认该机构下无人员关联"
                onConfirm={() => {
                  if (editingOrg) {
                    handleDelete(editingOrg.id, editingOrg.name)
                    setEditModalOpen(false)
                  }
                }}
              >
                <Button danger icon={<DeleteOutlined />}>
                  删除
                </Button>
              </Popconfirm>
            )}
            <Button type="primary" onClick={addChildOpen ? handleAdd : handleEdit}>
              {addChildOpen ? '添加' : '保存'}
            </Button>
          </Space>
        </div>
      </Modal>

      {/* Edit User Modal */}
      <Modal
        title={`编辑用户「${editingUser?.name}」`}
        open={editUserOpen}
        onCancel={() => {
          setEditUserOpen(false)
          setEditingUser(null)
        }}
        footer={null}
        destroyOnClose
        width={440}
      >
        <Form form={userForm} layout="vertical" preserve={false}>
          <Form.Item
            name="username"
            label="账号"
            rules={[{ required: true, message: '请输入账号' }]}
          >
            <Input placeholder="登录账号" />
          </Form.Item>
          <Form.Item name="orgId" label="部门" rules={[{ required: true, message: '请选择部门' }]}>
            <Select
              showSearch
              placeholder="选择部门"
              optionFilterProp="label"
              options={allOrgs.map((o) => ({
                value: o.id,
                label: '\u00A0\u00A0'.repeat(o.level) + o.name,
              }))}
              onChange={(val) => handleDeptChange(val)}
            />
          </Form.Item>
          <Form.Item name="roleId" label="角色">
            <Select
              placeholder="选择角色"
              allowClear
              optionFilterProp="label"
              options={activeRoles.map((r) => ({ value: r.id, label: r.name }))}
              onChange={handleRoleChange}
            />
          </Form.Item>
          <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
            <Button
              icon={<KeyOutlined />}
              onClick={() => editingUser && handleResetPassword(editingUser)}
              block
            >
              重置密码
            </Button>
            {editingUser?.status === 1 ? (
              <Button
                icon={<StopOutlined />}
                danger
                onClick={() => editingUser && handleToggleStatus(editingUser)}
                block
              >
                禁用账号
              </Button>
            ) : (
              <Button
                icon={<CheckCircleOutlined />}
                onClick={() => editingUser && handleToggleStatus(editingUser)}
                block
              >
                启用账号
              </Button>
            )}
          </div>
        </Form>
        <div style={{ marginTop: 24, padding: '12px 0', borderTop: '1px solid #f0f0f0' }}>
          <Popconfirm
            title="删除账号"
            description={`确定要删除用户「${editingUser?.name}」吗？此操作不可撤销。`}
            onConfirm={() => {
              if (editingUser) {
                handleDeleteUser(editingUser)
                setEditUserOpen(false)
              }
            }}
            okText="删除"
            cancelText="取消"
            okButtonProps={{ danger: true }}
          >
            <Button type="text" danger icon={<DeleteOutlined />}>
              删除此账号
            </Button>
          </Popconfirm>
        </div>
      </Modal>
    </div>
  )
}

export default OrgManagement
