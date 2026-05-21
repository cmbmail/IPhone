import { useState } from 'react'
import { Table, Button, Card, Select, Tag, Space, Modal, message, Input, Row, Col, Statistic } from 'antd'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { deviceApi, Device } from '@/api/device'
import { orgApi } from '@/api/org'
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'

const { Option } = Select

const STATUS_COLORS: Record<string, string> = {
  ONLINE: 'success',
  OFFLINE: 'error',
  IDLE: 'warning',
  MAINTENANCE: 'processing',
  DECOMMISSIONED: 'default'
}

const STATUS_NAMES: Record<string, string> = {
  ONLINE: '在线',
  OFFLINE: '离线',
  IDLE: '空闲',
  MAINTENANCE: '维护中',
  DECOMMISSIONED: '已报废'
}

const DeviceManagement = () => {
  const [status, setStatus] = useState<string>('')
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false)
  const [formData, setFormData] = useState({ deviceId: '', deviceName: '', deviceType: '', model: '', ipAddress: '', orgId: 0 })
  const queryClient = useQueryClient()

  const { data: orgsData } = useQuery({
    queryKey: ['orgs'],
    queryFn: async () => {
      const response = await orgApi.getAll()
      return response.data.data
    }
  })

  const { data: deviceData, isLoading, refetch } = useQuery({
    queryKey: ['devices', status],
    queryFn: async () => {
      const params: any = { page: 0, size: 100 }
      return deviceApi.getDevices(params)
    }
  })

  const createMutation = useMutation({
    mutationFn: (data: typeof formData) => deviceApi.create(data),
    onSuccess: () => {
      message.success('设备创建成功')
      queryClient.invalidateQueries({ queryKey: ['devices'] })
      setIsCreateModalOpen(false)
      setFormData({ deviceId: '', deviceName: '', deviceType: '', model: '', ipAddress: '', orgId: 0 })
    },
    onError: () => message.error('创建失败')
  })

  const updateStatusMutation = useMutation({
    mutationFn: ({ deviceId, status }: { deviceId: string; status: string }) =>
      deviceApi.updateStatus(deviceId, status),
    onSuccess: () => {
      message.success('状态更新成功')
      queryClient.invalidateQueries({ queryKey: ['devices'] })
    },
    onError: () => message.error('状态更新失败')
  })

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deviceApi.delete(id),
    onSuccess: () => {
      message.success('设备删除成功')
      queryClient.invalidateQueries({ queryKey: ['devices'] })
    },
    onError: () => message.error('删除失败')
  })

  const handleStatusChange = (record: Device, newStatus: string) => {
    Modal.confirm({
      title: '更新设备状态',
      content: `将设备 ${record.deviceId} 状态改为 ${STATUS_NAMES[newStatus]}？`,
      onOk: () => updateStatusMutation.mutate({ deviceId: record.deviceId, status: newStatus })
    })
  }

  const handleDelete = (record: Device) => {
    Modal.confirm({
      title: '删除设备',
      content: `删除设备 ${record.deviceId}？`,
      onOk: () => deleteMutation.mutate(record.id)
    })
  }

  const columns = [
    { title: '设备ID', dataIndex: 'deviceId', key: 'deviceId', width: 150 },
    { title: '设备名称', dataIndex: 'deviceName', key: 'deviceName', width: 150 },
    { title: '类型', dataIndex: 'deviceType', key: 'deviceType', width: 120 },
    { title: '型号', dataIndex: 'model', key: 'model', width: 120 },
    { title: 'IP地址', dataIndex: 'ipAddress', key: 'ipAddress', width: 150 },
    { title: '组织', dataIndex: 'orgName', key: 'orgName', width: 150 },
    { title: '用户', dataIndex: 'userName', key: 'userName', width: 120 },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 120,
      render: (status: string) => <Tag color={STATUS_COLORS[status] || 'default'}>{STATUS_NAMES[status]}</Tag>
    },
    {
      title: '操作',
      key: 'actions',
      width: 200,
      render: (_: any, record: Device) => (
        <Space>
          <Select
            value={record.status}
            onChange={(value) => handleStatusChange(record, value)}
            style={{ width: 100 }}
            size="small"
          >
            <Option value="ONLINE">在线</Option>
            <Option value="OFFLINE">离线</Option>
            <Option value="IDLE">空闲</Option>
            <Option value="MAINTENANCE">维护中</Option>
            <Option value="DECOMMISSIONED">已报废</Option>
          </Select>
          <Button size="small" danger icon={<DeleteOutlined />} onClick={() => handleDelete(record)}>
            删除
          </Button>
        </Space>
      )
    }
  ]

  const devices = deviceData?.data?.data?.content || []
  const onlineCount = devices.filter((d: Device) => d.status === 'ONLINE').length
  const offlineCount = devices.filter((d: Device) => d.status === 'OFFLINE').length
  const idleCount = devices.filter((d: Device) => d.status === 'IDLE').length

  return (
    <div>
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <Card><Statistic title="设备总数" value={devices.length} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="在线" value={onlineCount} valueStyle={{ color: '#3f8600' }} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="离线" value={offlineCount} valueStyle={{ color: '#cf1322' }} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="空闲" value={idleCount} valueStyle={{ color: '#faad14' }} /></Card>
        </Col>
      </Row>

      <Card>
        <Space style={{ marginBottom: 16 }}>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setIsCreateModalOpen(true)}>
            添加设备
          </Button>
          <Button onClick={() => refetch()}>刷新</Button>
        </Space>

        <Table
          columns={columns}
          dataSource={devices}
          loading={isLoading}
          rowKey="id"
          pagination={{ pageSize: 20, total: deviceData?.data?.data?.totalElements }}
        />
      </Card>

      <Modal
        title="添加设备"
        open={isCreateModalOpen}
        onCancel={() => setIsCreateModalOpen(false)}
        footer={[
          <Button key="back" onClick={() => setIsCreateModalOpen(false)}>取消</Button>,
          <Button key="submit" type="primary" onClick={() => createMutation.mutate(formData)}>
            添加
          </Button>
        ]}
      >
        <Space direction="vertical" style={{ width: '100%' }}>
          <Input
            label="设备ID"
            value={formData.deviceId}
            onChange={(e) => setFormData({ ...formData, deviceId: e.target.value })}
            placeholder="输入设备ID"
          />
          <Input
            label="设备名称"
            value={formData.deviceName}
            onChange={(e) => setFormData({ ...formData, deviceName: e.target.value })}
            placeholder="输入设备名称"
          />
          <Select
            label="设备类型"
            value={formData.deviceType}
            onChange={(value) => setFormData({ ...formData, deviceType: value })}
          >
            <Option value="IP_PHONE">IP电话</Option>
            <Option value="ATA">ATA</Option>
            <Option value="GATEWAY">网关</Option>
          </Select>
          <Input
            label="型号"
            value={formData.model}
            onChange={(e) => setFormData({ ...formData, model: e.target.value })}
            placeholder="输入型号"
          />
          <Input
            label="IP地址"
            value={formData.ipAddress}
            onChange={(e) => setFormData({ ...formData, ipAddress: e.target.value })}
            placeholder="输入IP地址"
          />
          <Select
            label="组织"
            value={formData.orgId}
            onChange={(value) => setFormData({ ...formData, orgId: value })}
          >
            {orgsData?.map((org: any) => (
              <Option key={org.id} value={org.id}>{org.name}</Option>
            ))}
          </Select>
        </Space>
      </Modal>
    </div>
  )
}

export default DeviceManagement
