import { useState } from 'react'
import { Table, Button, Modal, Form, Input, Tag, message, Space, Drawer, Descriptions } from 'antd'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { phoneDeviceApi, type PhoneDeviceDTO } from '@/api/phoneDevice'

const DEV_STATUS_COLORS: Record<number, string> = {
  0: 'default',
  1: 'success',
  2: 'warning',
  3: 'processing',
  4: 'error',
}
const DEV_STATUS_NAMES: Record<number, string> = {
  0: '库存',
  1: '使用中',
  2: '停用',
  3: '维修中',
  4: '已报废',
}

interface BoundPhone {
  phoneId: number
  phoneNumber: string
  extensionNumber: string
  status: string
  lineOrder: number
}

const DeviceManagement = () => {
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(20)
  const [selectedDevice, setSelectedDevice] = useState<PhoneDeviceDTO | null>(null)
  const [detailOpen, setDetailOpen] = useState(false)
  const [modalType, setModalType] = useState('')
  const [form] = Form.useForm()
  const qc = useQueryClient()

  const { data: devicesData, isLoading } = useQuery({
    queryKey: ['phone-devices', page, pageSize],
    queryFn: async () => {
      const r = await phoneDeviceApi.getList({ page, size: pageSize })
      return r
    },
  })

  const { data: boundPhones } = useQuery({
    queryKey: ['device-phones', selectedDevice?.id],
    queryFn: async () => {
      if (!selectedDevice) return []
      const r = await phoneDeviceApi.getBoundPhones(selectedDevice.id)
      return (r as unknown as BoundPhone[]) || []
    },
    enabled: !!selectedDevice && detailOpen,
  })

  const onSuccess = (msg: string) => {
    message.success(msg)
    qc.invalidateQueries({ queryKey: ['phone-devices'] })
    setModalType('')
    form.resetFields()
  }

  const createMut = useMutation({
    mutationFn: (d: Record<string, unknown>) => phoneDeviceApi.create(d),
    onSuccess: () => onSuccess('话机录入成功'),
  })
  const assignMut = useMutation({
    mutationFn: (d: Record<string, unknown>) => phoneDeviceApi.assign(selectedDevice!.id, d),
    onSuccess: () => onSuccess('分配成功'),
  })
  const reclaimMut = useMutation({
    mutationFn: (d: Record<string, unknown>) => phoneDeviceApi.reclaim(selectedDevice!.id, d),
    onSuccess: () => onSuccess('回收成功'),
  })
  const deactivateMut = useMutation({
    mutationFn: (d: Record<string, unknown>) => phoneDeviceApi.deactivate(selectedDevice!.id, d),
    onSuccess: () => onSuccess('停用成功'),
  })
  const reactivateMut = useMutation({
    mutationFn: (_d: unknown) => phoneDeviceApi.reactivate(selectedDevice!.id),
    onSuccess: () => onSuccess('恢复成功'),
  })
  const repairMut = useMutation({
    mutationFn: (d: Record<string, unknown>) => phoneDeviceApi.repair(selectedDevice!.id, d),
    onSuccess: () => onSuccess('送修成功'),
  })
  const repairDoneMut = useMutation({
    mutationFn: (_d: unknown) => phoneDeviceApi.repairDone(selectedDevice!.id),
    onSuccess: () => onSuccess('修复成功'),
  })
  const retireMut = useMutation({
    mutationFn: (d: Record<string, unknown>) => phoneDeviceApi.retire(selectedDevice!.id, d),
    onSuccess: () => onSuccess('报废成功'),
  })
  const editMut = useMutation({
    mutationFn: (d: Record<string, unknown>) => phoneDeviceApi.update(selectedDevice!.id, d),
    onSuccess: () => onSuccess('修改成功'),
  })
  const bindMut = useMutation({
    mutationFn: (d: Record<string, unknown>) => phoneDeviceApi.bindPhone(selectedDevice!.id, d),
    onSuccess: () => onSuccess('绑定成功'),
  })
  const unbindMut = useMutation({
    mutationFn: (phoneId: number) => phoneDeviceApi.unbindPhone(selectedDevice!.id, phoneId),
    onSuccess: () => onSuccess('解绑成功'),
  })

  const handleSubmit = (values: Record<string, unknown>) => {
    switch (modalType) {
      case 'create':
        createMut.mutate(values)
        break
      case 'assign':
        assignMut.mutate(values)
        break
      case 'reclaim':
        reclaimMut.mutate(values)
        break
      case 'deactivate':
        deactivateMut.mutate(values)
        break
      case 'repair':
        repairMut.mutate(values)
        break
      case 'retire':
        retireMut.mutate(values)
        break
      case 'edit':
        editMut.mutate(values)
        break
      case 'bindPhone':
        bindMut.mutate(values)
        break
    }
  }

  const openModal = (type: string, device: PhoneDeviceDTO | null = null) => {
    setSelectedDevice(device)
    form.resetFields()
    if (device) form.setFieldsValue(device)
    setModalType(type)
  }

  const getActionButtons = (r: PhoneDeviceDTO) => {
    const btns: React.ReactNode[] = []
    switch (r.status) {
      case 0:
        btns.push(
          <Button key="assign" size="small" type="primary" onClick={() => openModal('assign', r)}>
            分配
          </Button>,
          <Button key="deactivate" size="small" danger onClick={() => openModal('deactivate', r)}>
            停用
          </Button>,
          <Button key="retire" size="small" danger onClick={() => openModal('retire', r)}>
            报废
          </Button>
        )
        break
      case 1:
        btns.push(
          <Button key="reclaim" size="small" onClick={() => openModal('reclaim', r)}>
            回收
          </Button>,
          <Button key="deactivate" size="small" danger onClick={() => openModal('deactivate', r)}>
            停用
          </Button>,
          <Button key="repair" size="small" onClick={() => openModal('repair', r)}>
            送修
          </Button>,
          <Button key="retire" size="small" danger onClick={() => openModal('retire', r)}>
            报废
          </Button>
        )
        break
      case 2:
        btns.push(
          <Button
            key="reactivate"
            size="small"
            type="primary"
            onClick={() => reactivateMut.mutate(r)}
          >
            恢复
          </Button>,
          <Button key="retire" size="small" danger onClick={() => openModal('retire', r)}>
            报废
          </Button>
        )
        break
      case 3:
        btns.push(
          <Button
            key="repairDone"
            size="small"
            type="primary"
            onClick={() => repairDoneMut.mutate(r)}
          >
            修复
          </Button>,
          <Button key="retire" size="small" danger onClick={() => openModal('retire', r)}>
            报废
          </Button>
        )
        break
    }
    return btns
  }

  const columns = [
    {
      title: 'MAC地址',
      dataIndex: 'macAddress',
      key: 'macAddress',
      width: 140,
      render: (t: string, r: PhoneDeviceDTO) => (
        <a
          onClick={() => {
            setSelectedDevice(r)
            setDetailOpen(true)
          }}
        >
          {t}
        </a>
      ),
    },
    { title: '型号', dataIndex: 'model', key: 'model', width: 100 },
    { title: '品牌', dataIndex: 'brand', key: 'brand', width: 80 },
    { title: '使用人', dataIndex: 'assignedEmployeeNo', key: 'assignedEmployeeNo', width: 100 },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 90,
      render: (s: number) => <Tag color={DEV_STATUS_COLORS[s]}>{DEV_STATUS_NAMES[s]}</Tag>,
    },
    { title: '绑定号码数', dataIndex: 'boundPhoneCount', key: 'boundPhoneCount', width: 100 },
    {
      title: '操作',
      key: 'actions',
      width: 280,
      render: (_: unknown, r: PhoneDeviceDTO) => (
        <Space size={4} wrap>
          {getActionButtons(r)}
        </Space>
      ),
    },
  ]

  const getModalTitle = () => {
    const map: Record<string, string> = {
      create: '录入话机',
      assign: '分配话机',
      reclaim: '回收话机',
      deactivate: '停用话机',
      repair: '送修话机',
      retire: '报废话机',
      edit: '编辑话机',
      bindPhone: '绑定号码',
    }
    return map[modalType] || '操作'
  }

  const renderModalFields = () => {
    switch (modalType) {
      case 'create':
        return (
          <>
            <Form.Item name="macAddress" label="MAC地址" rules={[{ required: true }]}>
              <Input placeholder="如 A4B1C2D3E4F5" />
            </Form.Item>
            <Form.Item name="model" label="型号">
              <Input placeholder="话机型号" />
            </Form.Item>
            <Form.Item name="brand" label="品牌">
              <Input placeholder="品牌" />
            </Form.Item>
            <Form.Item name="orgId" label="归属组织" rules={[{ required: true }]}>
              <Input type="number" placeholder="组织ID" />
            </Form.Item>
            <Form.Item name="remark" label="备注">
              <Input.TextArea />
            </Form.Item>
          </>
        )
      case 'assign':
        return (
          <>
            <Form.Item name="assignedEmployeeNo" label="员工工号" rules={[{ required: true }]}>
              <Input placeholder="输入员工工号" />
            </Form.Item>
            <Form.Item name="remark" label="备注">
              <Input.TextArea />
            </Form.Item>
          </>
        )
      case 'reclaim':
      case 'deactivate':
      case 'repair':
      case 'retire':
        return (
          <Form.Item name="reason" label="原因">
            <Input.TextArea />
          </Form.Item>
        )
      case 'edit':
        return (
          <>
            <Form.Item name="model" label="型号">
              <Input />
            </Form.Item>
            <Form.Item name="brand" label="品牌">
              <Input />
            </Form.Item>
            <Form.Item name="remark" label="备注">
              <Input.TextArea />
            </Form.Item>
          </>
        )
      case 'bindPhone':
        return (
          <>
            <Form.Item name="extensionNumber" label="分机号" rules={[{ required: true }]}>
              <Input placeholder="输入要绑定的分机号" />
            </Form.Item>
            <Form.Item name="lineOrder" label="线路序号">
              <Input type="number" placeholder="默认1" />
            </Form.Item>
          </>
        )
      default:
        return null
    }
  }

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <Button type="primary" onClick={() => openModal('create')}>
          录入话机
        </Button>
      </div>

      <Table
        columns={columns}
        dataSource={devicesData?.content}
        loading={isLoading}
        rowKey="id"
        pagination={{
          current: page + 1,
          pageSize,
          total: devicesData?.totalElements,
          showTotal: (t) => `共 ${t} 条`,
          onChange: (p, ps) => { setPage(p - 1); setPageSize(ps); },
            showSizeChanger: true,
        }}
        scroll={{ x: 1100 }}
      />

      <Modal
        title={getModalTitle()}
        open={!!modalType}
        onCancel={() => {
          setModalType('')
          form.resetFields()
        }}
        onOk={() => form.submit()}
        width={480}
        destroyOnClose
      >
        <Form form={form} onFinish={handleSubmit} layout="vertical" style={{ marginTop: 8 }}>
          {renderModalFields()}
        </Form>
      </Modal>

      <Drawer
        title={`话机详情 - ${selectedDevice?.macAddress || ''}`}
        open={detailOpen}
        onClose={() => setDetailOpen(false)}
        width={640}
        destroyOnClose
        extra={
          selectedDevice?.status !== 4 && (
            <Button
              onClick={() => {
                setDetailOpen(false)
                openModal('edit', selectedDevice)
              }}
            >
              编辑
            </Button>
          )
        }
      >
        {selectedDevice && (
          <>
            <Descriptions bordered size="small" column={2} style={{ marginBottom: 24 }}>
              <Descriptions.Item label="MAC地址">{selectedDevice.macAddress}</Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={DEV_STATUS_COLORS[selectedDevice.status]}>
                  {DEV_STATUS_NAMES[selectedDevice.status]}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="型号">{selectedDevice.model || '-'}</Descriptions.Item>
              <Descriptions.Item label="品牌">{selectedDevice.brand || '-'}</Descriptions.Item>
              <Descriptions.Item label="使用人">
                {selectedDevice.assignedEmployeeNo || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="组织">
                {selectedDevice.orgName || selectedDevice.orgId || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="备注" span={2}>
                {selectedDevice.remark || '-'}
              </Descriptions.Item>
            </Descriptions>
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                marginBottom: 12,
              }}
            >
              <h4>已绑定号码 ({boundPhones?.length || 0})</h4>
              {(selectedDevice?.status === 1 || selectedDevice?.status === 0) && (
                <Button
                  size="small"
                  onClick={() => {
                    setDetailOpen(false)
                    openModal('bindPhone', selectedDevice)
                  }}
                >
                  绑定号码
                </Button>
              )}
            </div>
            <Table
              size="small"
              dataSource={boundPhones || []}
              rowKey="phoneId"
              pagination={false}
              columns={[
                { title: '号码', dataIndex: 'phoneNumber', width: 140 },
                { title: '分机号', dataIndex: 'extensionNumber', width: 100 },
                {
                  title: '状态',
                  dataIndex: 'status',
                  width: 80,
                  render: (s: string) => <Tag>{s}</Tag>,
                },
                { title: '线路', dataIndex: 'lineOrder', width: 60 },
                {
                  title: '操作',
                  width: 60,
                  render: (_: unknown, r: BoundPhone) => (
                    <Button size="small" danger onClick={() => unbindMut.mutate(r.phoneId)}>
                      解绑
                    </Button>
                  ),
                },
              ]}
            />
          </>
        )}
      </Drawer>
    </div>
  )
}

export default DeviceManagement
