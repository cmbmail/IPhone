import { useState } from 'react'
import { Table, Button, Modal, Form, Input, Select, Tag, message, Space, Drawer, Timeline, Descriptions, InputNumber, Popconfirm } from 'antd'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { phoneApi } from '@/api/phone'
import { orgApi } from '@/api/org'
import type { PhoneNumber, CreatePhoneDTO, PhoneAllocationRequest, PhoneReclaimRequest, PhoneStatusChangeRequest, PhoneSurrenderRequest, PhoneReserveRequest, PhoneChangeRequest } from '@/types/phone'

const { Option } = Select

const STATUS_COLORS: Record<number, string> = {
  0: 'default', 1: 'success', 2: 'warning', 3: 'error', 4: 'processing', 5: 'error'
}
const STATUS_NAMES: Record<number, string> = {
  0: '空闲', 1: '使用中', 2: '停用', 3: '已拆机', 4: '已预留', 5: '已禁用'
}

const ACTION_NAMES: Record<string, string> = {
  allocate: '分配', reclaim: '回收', surrender: '拆机', trouble: '停机', restore: '复机',
  change_user: '过户', change_org: '转移', change_number: '换号', change_extension: '换分机号',
  reserve: '预留', release: '释放', disable: '禁用', enable: '启用'
}

const PhoneManagement = () => {
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(20)
  const [searchNumber, setSearchNumber] = useState('')
  const [filterStatus, setFilterStatus] = useState<number | undefined>(undefined)
  const [filterOrgId, setFilterOrgId] = useState<number | undefined>(undefined)

  const [selectedPhone, setSelectedPhone] = useState<PhoneNumber | null>(null)
  const [detailOpen, setDetailOpen] = useState(false)
  const [modalType, setModalType] = useState<string>('')
  const [form] = Form.useForm()
  const qc = useQueryClient()

  const { data: phonesData, isLoading } = useQuery({
    queryKey: ['phones', page, pageSize, filterStatus, filterOrgId],
    queryFn: async () => {
      const params: any = { page, size: pageSize }
      if (filterStatus !== undefined) params.status = filterStatus
      if (filterOrgId) params.orgId = filterOrgId
      const res = await phoneApi.getAll(params)
      return res.data.data
    }
  })

  const { data: orgsData } = useQuery({
    queryKey: ['orgs'],
    queryFn: async () => { const r = await orgApi.getAll(); return r.data.data }
  })

  const { data: historyData } = useQuery({
    queryKey: ['phone-history', selectedPhone?.id],
    queryFn: async () => {
      if (!selectedPhone) return null
      const r = await phoneApi.getHistory(selectedPhone.id, { page: 0, size: 50 })
      return r.data.data?.content || []
    },
    enabled: !!selectedPhone && detailOpen
  })

  const mut = (fn: any, msg: string) => useMutation({
    mutationFn: fn,
    onSuccess: () => { message.success(msg); qc.invalidateQueries({ queryKey: ['phones'] }); setModalType(''); form.resetFields() }
  })

  const allocateMut = mut((d: PhoneAllocationRequest) => phoneApi.allocate(d), '分配成功')
  const reclaimMut = mut((d: PhoneReclaimRequest) => phoneApi.reclaim(d), '回收成功')
  const surrenderMut = mut((d: PhoneSurrenderRequest) => phoneApi.surrender(d), '拆机成功')
  const reserveMut = mut((d: PhoneReserveRequest) => phoneApi.reserve(d), '预留成功')
  const releaseMut = mut((d: PhoneReserveRequest) => phoneApi.release(d), '释放成功')
  const statusMut = mut((d: PhoneStatusChangeRequest) => phoneApi.changeStatus(d), '状态变更成功')
  const changeUserMut = mut((d: PhoneChangeRequest) => phoneApi.changeUser(d), '过户成功')
  const changeOrgMut = mut((d: PhoneChangeRequest) => phoneApi.changeOrg(d), '转移成功')
  const changeNumberMut = mut((d: PhoneChangeRequest) => phoneApi.changeNumber(d), '换号成功')
  const changeExtMut = mut((d: PhoneChangeRequest) => phoneApi.changeExtension(d), '换分机号成功')

  const openModal = (type: string, phone: PhoneNumber) => {
    setSelectedPhone(phone)
    form.resetFields()
    form.setFieldsValue({ phoneId: phone.id })
    setModalType(type)
  }

  const handleSubmit = (values: any) => {
    const data = { ...values, phoneId: selectedPhone?.id }
    switch (modalType) {
      case 'allocate': allocateMut.mutate(data); break
      case 'reclaim': reclaimMut.mutate(data); break
      case 'surrender': surrenderMut.mutate(data); break
      case 'reserve': reserveMut.mutate(data); break
      case 'release': releaseMut.mutate(data); break
      case 'changeUser': changeUserMut.mutate(data); break
      case 'changeOrg': changeOrgMut.mutate(data); break
      case 'changeNumber': changeNumberMut.mutate(data); break
      case 'changeExtension': changeExtMut.mutate(data); break
      case 'status': statusMut.mutate(data); break
    }
  }

  const openDetail = (phone: PhoneNumber) => {
    setSelectedPhone(phone)
    setDetailOpen(true)
  }

  const getActionButtons = (r: PhoneNumber) => {
    const btns: React.ReactNode[] = []
    switch (r.status) {
      case 0:
        btns.push(
          <Button key="alloc" size="small" type="primary" onClick={() => openModal('allocate', r)}>分配</Button>,
          <Button key="reserve" size="small" onClick={() => openModal('reserve', r)}>预留</Button>,
          <Button key="disable" size="small" danger onClick={() => openModal('status', r)}>禁用</Button>,
          <Button key="surrender" size="small" danger onClick={() => openModal('surrender', r)}>拆机</Button>,
        )
        break
      case 1:
        btns.push(
          <Button key="reclaim" size="small" danger onClick={() => openModal('reclaim', r)}>回收</Button>,
          <Button key="changeUser" size="small" onClick={() => openModal('changeUser', r)}>过户</Button>,
          <Button key="changeNumber" size="small" onClick={() => openModal('changeNumber', r)}>换号</Button>,
          <Button key="changeOrg" size="small" onClick={() => openModal('changeOrg', r)}>转移</Button>,
          <Button key="changeExt" size="small" onClick={() => openModal('changeExtension', r)}>换分机号</Button>,
          <Button key="stop" size="small" onClick={() => { setSelectedPhone(r); form.setFieldsValue({ phoneId: r.id, newStatus: 2 }); setModalType('status') }}>停机</Button>,
          <Button key="surrender" size="small" danger onClick={() => openModal('surrender', r)}>拆机</Button>,
        )
        break
      case 2:
        btns.push(
          <Button key="restore" size="small" type="primary" onClick={() => { setSelectedPhone(r); form.setFieldsValue({ phoneId: r.id, newStatus: 1 }); setModalType('status') }}>复机</Button>,
          <Button key="reclaim" size="small" onClick={() => openModal('reclaim', r)}>回收</Button>,
          <Button key="surrender" size="small" danger onClick={() => openModal('surrender', r)}>拆机</Button>,
        )
        break
      case 4:
        btns.push(
          <Button key="release" size="small" onClick={() => openModal('release', r)}>释放</Button>,
          <Button key="surrender" size="small" danger onClick={() => openModal('surrender', r)}>拆机</Button>,
        )
        break
      case 5:
        btns.push(
          <Button key="enable" size="small" type="primary" onClick={() => { setSelectedPhone(r); form.setFieldsValue({ phoneId: r.id, newStatus: 0 }); setModalType('status') }}>解除禁用</Button>,
          <Button key="surrender" size="small" danger onClick={() => openModal('surrender', r)}>拆机</Button>,
        )
        break
    }
    return btns
  }

  const columns = [
    { title: '电话号码', dataIndex: 'phoneNumber', key: 'phoneNumber', width: 140, render: (t: string, r: PhoneNumber) => <a onClick={() => openDetail(r)}>{t}</a> },
    { title: '使用人', dataIndex: 'userId', key: 'userId', width: 100 },
    { title: '分机号', dataIndex: 'extensionNumber', key: 'extensionNumber', width: 100 },
    { title: '组织ID', dataIndex: 'orgId', key: 'orgId', width: 80 },
    { title: '状态', dataIndex: 'status', key: 'status', width: 90, render: (s: number) => <Tag color={STATUS_COLORS[s]}>{STATUS_NAMES[s]}</Tag> },
    {
      title: '操作', key: 'actions', width: 320,
      render: (_: any, r: PhoneNumber) => <Space size={4} wrap>{getActionButtons(r)}</Space>
    }
  ]

  const getModalTitle = () => {
    const map: Record<string, string> = {
      allocate: '分配号码', reclaim: '回收号码', surrender: '拆机', reserve: '预留号码',
      release: '释放预留', changeUser: '过户', changeOrg: '转移', changeNumber: '换号',
      changeExtension: '换分机号', status: '状态变更'
    }
    return map[modalType] || '操作'
  }

  const renderModalFields = () => {
    switch (modalType) {
      case 'allocate':
        return (
          <>
            <Form.Item name="userId" label="使用人工号" rules={[{ required: true }]}><Input placeholder="输入员工工号" /></Form.Item>
            <Form.Item name="orgId" label="归属组织" rules={[{ required: true }]}>
              <Select placeholder="选择组织">{orgsData?.map((o: any) => <Option key={o.id} value={o.id}>{o.name}</Option>)}</Select>
            </Form.Item>
            <Form.Item name="extensionNumber" label="分机号"><Input placeholder="自动分配则留空" /></Form.Item>
            <Form.Item name="remark" label="备注"><Input.TextArea /></Form.Item>
          </>
        )
      case 'reclaim':
        return (
          <>
            <Form.Item name="reason" label="回收原因"><Input.TextArea placeholder="输入回收原因" /></Form.Item>
            <Form.Item name="remark" label="备注"><Input.TextArea /></Form.Item>
          </>
        )
      case 'surrender':
        return (
          <>
            <Form.Item name="surrenderType" label="拆机类型" rules={[{ required: true }]}>
              <Select><Option value="surrender">拆机</Option><Option value="cancel">取消</Option></Select>
            </Form.Item>
            <Form.Item name="remark" label="备注"><Input.TextArea /></Form.Item>
          </>
        )
      case 'reserve':
        return <Form.Item name="remark" label="预留原因" rules={[{ required: true }]}><Input.TextArea /></Form.Item>
      case 'release':
        return <Form.Item name="remark" label="备注"><Input.TextArea /></Form.Item>
      case 'changeUser':
        return (
          <>
            <Form.Item name="userId" label="新使用人工号" rules={[{ required: true }]}><Input placeholder="输入新使用人员工号" /></Form.Item>
            <Form.Item name="extensionNumber" label="新分机号"><Input placeholder="留空自动判定" /></Form.Item>
            <Form.Item name="remark" label="备注"><Input.TextArea /></Form.Item>
          </>
        )
      case 'changeOrg':
        return (
          <>
            <Form.Item name="orgId" label="新归属组织" rules={[{ required: true }]}>
              <Select placeholder="选择新组织">{orgsData?.map((o: any) => <Option key={o.id} value={o.id}>{o.name}</Option>)}</Select>
            </Form.Item>
            <Form.Item name="remark" label="备注"><Input.TextArea /></Form.Item>
          </>
        )
      case 'changeNumber':
        return (
          <>
            <Form.Item name="phoneNumber" label="新电话号码" rules={[{ required: true }]}><Input placeholder="输入新号码(需为空闲状态)" /></Form.Item>
            <Form.Item name="remark" label="备注"><Input.TextArea /></Form.Item>
          </>
        )
      case 'changeExtension':
        return (
          <>
            <Form.Item name="extensionNumber" label="新分机号" rules={[{ required: true }]}><Input placeholder="输入新分机号" /></Form.Item>
            <Form.Item name="remark" label="备注"><Input.TextArea /></Form.Item>
          </>
        )
      case 'status':
        return (
          <>
            <Form.Item label="目标状态"><Tag color={STATUS_COLORS[form.getFieldValue('newStatus')]}>{STATUS_NAMES[form.getFieldValue('newStatus')]}</Tag></Form.Item>
            <Form.Item name="remark" label="备注"><Input.TextArea /></Form.Item>
          </>
        )
      default: return null
    }
  }

  return (
    <div>
      <div style={{ display: 'flex', gap: 12, marginBottom: 16, flexWrap: 'wrap', alignItems: 'center' }}>
        <Input.Search placeholder="搜索号码/分机号" style={{ width: 220 }} allowClear onSearch={v => { setSearchNumber(v); setPage(0) }} />
        <Select placeholder="状态筛选" allowClear style={{ width: 140 }} onChange={v => { setFilterStatus(v); setPage(0) }}>
          {Object.entries(STATUS_NAMES).map(([k, v]) => <Option key={k} value={Number(k)}>{v}</Option>)}
        </Select>
        <Select placeholder="组织筛选" allowClear style={{ width: 180 }} onChange={v => { setFilterOrgId(v); setPage(0) }}>
          {orgsData?.map((o: any) => <Option key={o.id} value={o.id}>{o.name}</Option>)}
        </Select>
      </div>

      <Table columns={columns} dataSource={phonesData?.content} loading={isLoading} rowKey="id"
        pagination={{ current: page + 1, pageSize, total: phonesData?.totalElements, showSizeChanger: true, showTotal: t => `共 ${t} 条`,
          onChange: (p, ps) => { setPage(p - 1); setPageSize(ps) } }}
        scroll={{ x: 1200 }}
      />

      <Modal title={getModalTitle()} open={!!modalType} onCancel={() => { setModalType(''); form.resetFields() }}
        onOk={() => form.submit()} confirmLoading={allocateMut.isLoading || reclaimMut.isLoading || surrenderMut.isLoading || changeUserMut.isLoading || changeOrgMut.isLoading || changeNumberMut.isLoading || changeExtMut.isLoading || statusMut.isLoading || reserveMut.isLoading || releaseMut.isLoading}
        width={480} destroyOnClose>
        <Form form={form} onFinish={handleSubmit} layout="vertical" style={{ marginTop: 8 }}>
          {renderModalFields()}
        </Form>
      </Modal>

      <Drawer title={`号码详情 - ${selectedPhone?.phoneNumber}`} open={detailOpen} onClose={() => setDetailOpen(false)} width={640} destroyOnClose>
        {selectedPhone && (
          <>
            <Descriptions bordered size="small" column={2} style={{ marginBottom: 24 }}>
              <Descriptions.Item label="号码">{selectedPhone.phoneNumber}</Descriptions.Item>
              <Descriptions.Item label="状态"><Tag color={STATUS_COLORS[selectedPhone.status]}>{STATUS_NAMES[selectedPhone.status]}</Tag></Descriptions.Item>
              <Descriptions.Item label="使用人">{selectedPhone.userId || '-'}</Descriptions.Item>
              <Descriptions.Item label="分机号">{selectedPhone.extensionNumber || '-'}</Descriptions.Item>
              <Descriptions.Item label="分机号类型">{selectedPhone.extensionType === 'auto' ? '自动(跟人)' : selectedPhone.extensionType === 'manual' ? '手动(跟号)' : '-'}</Descriptions.Item>
              <Descriptions.Item label="组织ID">{selectedPhone.orgId || '-'}</Descriptions.Item>
              <Descriptions.Item label="二次入库">{selectedPhone.isReentry ? '是' : '否'}</Descriptions.Item>
              <Descriptions.Item label="版本号">{selectedPhone.version}</Descriptions.Item>
              <Descriptions.Item label="备注" span={2}>{selectedPhone.remark || '-'}</Descriptions.Item>
              <Descriptions.Item label="创建时间" span={2}>{selectedPhone.createdAt}</Descriptions.Item>
            </Descriptions>
            <h4 style={{ marginBottom: 12 }}>操作历史</h4>
            <Timeline items={(historyData || []).map((h: any) => ({
              color: h.action === 'surrender' ? 'red' : h.action === 'allocate' ? 'green' : 'blue',
              children: (
                <div>
                  <div><strong>{ACTION_NAMES[h.action] || h.action}</strong> <span style={{ color: '#999', fontSize: 12 }}>{h.operatedAt}</span></div>
                  <div style={{ fontSize: 12, color: '#666' }}>
                    {h.fromStatus != null && h.toStatus != null && <span>{STATUS_NAMES[h.fromStatus] || h.fromStatus} → {STATUS_NAMES[h.toStatus] || h.toStatus} | </span>}
                    {h.fromUser && h.toUser && <span>{h.fromUser} → {h.toUser} | </span>}
                    操作人: {h.operator} {h.remark && `| ${h.remark}`}
                  </div>
                </div>
              )
            }))} />
          </>
        )}
      </Drawer>
    </div>
  )
}

export default PhoneManagement
