import { useState, useMemo, useCallback } from 'react'
import {
  WORK_ORDER_STATUS_COLORS as STATUS_COLORS,
  WORK_ORDER_STATUS_NAMES as STATUS_NAMES,
  WORK_ORDER_TYPE_NAMES as TYPE_NAMES,
  WORK_ORDER_TYPE_COLORS as TYPE_COLORS,
  WORK_ORDER_ITEM_TYPE_NAMES as ITEM_TYPE_NAMES,
  WORK_ORDER_ITEM_STATUS_NAMES as ITEM_STATUS_NAMES,
  WORK_ORDER_ITEM_STATUS_COLORS as ITEM_STATUS_COLORS,
} from '@/constants/workOrder'
import {
  Table,
  Button,
  Card,
  Select,
  Tag,
  Space,
  Modal,
  message,
  Input,
  Row,
  Col,
  Statistic,
  Drawer,
  Descriptions,
  Timeline,
  Form,
  Dropdown,
} from 'antd'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { workOrderApi } from '@/api/workOrder'
import { PlusOutlined } from '@ant-design/icons'
import { ApiGet, type PagedData } from '@/api/request'
import type { WorkOrder, WorkOrderItem } from '@/types/workOrder'

const { TextArea } = Input

const PASTE_FIELDS = [
  'extensionNumber',
  'employeeName',
  'employeeNo',
  'macAddresses',
  'branchName',
  'deptName',
  'remark',
]


/** 已完成/已归档/已取消的工单归入历史 */
const isHistorical = (s: number) => s === 3 || s === 4 || s === 5

const WorkOrderManagement = () => {
  const [status] = useState<number | ''>('')
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false)
  const [detailDrawerOpen, setDetailDrawerOpen] = useState(false)
  const [selectedOrder, setSelectedOrder] = useState<WorkOrder | null>(null)
  const [selectedType, setSelectedType] = useState(1)
  const [extOptions, setExtOptions] = useState<{ label: string; value: string }[]>([])
  const [extLoading, setExtLoading] = useState(false)
  const [form] = Form.useForm()
  const queryClient = useQueryClient()

  const {
    data: orderData,
    isLoading,
    refetch,
  } = useQuery({
    queryKey: ['work-orders', status],
    queryFn: async () => {
      const params: Record<string, unknown> = { page: 0, size: 100 }
      if (status !== '') params.status = status
      return workOrderApi.getList(params)
    },
  })

  const completeMutation = useMutation({
    mutationFn: (id: number) => workOrderApi.complete(id),
    onSuccess: () => {
      message.success('工单已完成')
      queryClient.invalidateQueries({ queryKey: ['work-orders'] })
    },
    onError: () => message.error('完成失败'),
  })

  const createMutation = useMutation({
    mutationFn: (data: Record<string, unknown>) => workOrderApi.create(data),
    onSuccess: () => {
      message.success('工单创建成功')
      queryClient.invalidateQueries({ queryKey: ['work-orders'] })
      setIsCreateModalOpen(false)
      form.resetFields()
    },
    onError: () => message.error('创建失败'),
  })

  const executeItemMutation = useMutation({
    mutationFn: (itemId: number) => workOrderApi.executeItem(itemId),
    onSuccess: () => {
      message.success('工单项执行成功')
      queryClient.invalidateQueries({ queryKey: ['work-orders'] })
      if (selectedOrder) handleViewDetail(selectedOrder)
    },
    onError: () => message.error('执行失败'),
  })

  const handleComplete = (record: WorkOrder) => {
    Modal.confirm({
      title: '完成工单',
      content: `标记工单 ${record.workOrderNo} 为已完成？`,
      onOk: () => completeMutation.mutate(record.id),
    })
  }
  const handleViewDetail = (record: WorkOrder) => {
    workOrderApi
      .getById(record.id)
      .then((res) => {
        setSelectedOrder(res || record)
        setDetailDrawerOpen(true)
      })
      .catch(() => {
        message.error('加载工单详情失败')
        setSelectedOrder(record)
        setDetailDrawerOpen(true)
      })
  }
  const handleExecuteItem = (itemId: number) => {
    Modal.confirm({
      title: '执行工单项',
      content: '确认要执行此工单项？',
      onOk: () => executeItemMutation.mutate(itemId),
    })
  }

  // 搜索分机号
  const handleExtSearch = useCallback(async (value: string) => {
    if (!value || value.length < 1) {
      setExtOptions([])
      return
    }
    setExtLoading(true)
    try {
      const res = await ApiGet<PagedData<{ extensionNumber: string; employeeName: string | null }>>(
        '/extension-numbers',
        {
          params: { keyword: value, page: 0, size: 20 },
        }
      )
      const content = res?.content || []
      setExtOptions(
        content.map((e: { extensionNumber: string; employeeName: string | null }) => ({
          label: `${e.extensionNumber} ${e.employeeName ? '(' + e.employeeName + ')' : ''}`,
          value: e.extensionNumber,
        }))
      )
    } catch {
      message.error('搜索分机号失败')
      setExtOptions([])
    }
    setExtLoading(false)
  }, [])

  // 选中分机号后自动填充
  const handleExtSelect = useCallback(
    async (value: string) => {
      try {
        const res = await ApiGet<Record<string, unknown>>(`/extension-numbers/detail/${value}`)
        const d = res
        if (d) {
          const macs = Array.isArray(d.macAddresses) ? (d.macAddresses as string[]).join(', ') : ''
          form.setFieldsValue({
            employeeName: (d.employeeName as string) || '',
            macAddresses: macs,
            branchName: (d.branchName as string) || '',
            deptName: (d.deptName as string) || '',
          })
        }
      } catch {
        message.error('获取分机详情失败')
      }
    },
    [form]
  )

  // 从Excel粘贴: tab分隔依次填入分机号、使用人、员工ID、MAC、分行、部门、备注
  const handleFormPaste = useCallback(
    (e: React.ClipboardEvent) => {
      const text = e.clipboardData.getData('text/plain')
      if (!text || !text.includes('\t')) return
      e.preventDefault()
      const values = text.replace(/\r?\n$/, '').split('\t')
      const patch: Record<string, string> = {}
      PASTE_FIELDS.forEach((field, i) => {
        if (values[i] !== undefined && values[i] !== '') {
          patch[field] = values[i].trim()
        }
      })
      form.setFieldsValue(patch)
      message.success(`已粘贴 ${Object.keys(patch).length} 个字段`)
      if (patch.extensionNumber) {
        handleExtSelect(patch.extensionNumber)
      }
    },
    [form, handleExtSelect]
  )

  // 选择工单类型后，进入创建表单
  const handleTypeSelect = (type: number) => {
    setSelectedType(type)
    form.resetFields()
    form.setFieldsValue({ type })
    setIsCreateModalOpen(true)
  }

  // --- 列定义 ---
  // 后端 order_type → 前端 orderType
  const commonColumns = [
    { title: '工单号', dataIndex: 'workOrderNo', key: 'workOrderNo', width: 150 },
    { title: '标题', dataIndex: 'title', key: 'title', width: 200 },
    {
      title: '类型',
      dataIndex: 'orderType',
      key: 'orderType',
      width: 110,
      render: (t: number) => <Tag color={TYPE_COLORS[t] || 'default'}>{TYPE_NAMES[t] || t}</Tag>,
    },
    { title: '申请人', dataIndex: 'requesterName', key: 'requesterName', width: 90 },
    { title: '处理人', dataIndex: 'handlerName', key: 'handlerName', width: 90 },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 120,
      render: (v: string) =>
        v
          ? v
              .replace(/^[0-9]{4}-/, '')
              .replace('T', ' ')
              .substring(0, 11)
          : '-',
    },
  ]

  // 进行中工单列：操作列含状态Tag + 按钮
  const activeColumns = [
    ...commonColumns,
    {
      title: '操作',
      key: 'actions',
      width: 160,
      render: (_: unknown, record: WorkOrder) => {
        const s = record.status
        return (
          <Space>
            <Tag
              color={STATUS_COLORS[s] || 'default'}
              style={{ cursor: 'pointer' }}
              onClick={() => handleViewDetail(record)}
            >
              {STATUS_NAMES[s] || s}
            </Tag>
            {(s === 1 || s === 2) && (
              <Button size="small" type="primary" onClick={() => handleComplete(record)}>
                完成
              </Button>
            )}
          </Space>
        )
      },
    },
  ]

  // 历史工单列：操作列仅含状态Tag
  const historyColumns = [
    ...commonColumns,
    {
      title: '操作',
      key: 'actions',
      width: 120,
      render: (_: unknown, record: WorkOrder) => {
        const s = record.status
        return (
          <Tag
            color={STATUS_COLORS[s] || 'default'}
            style={{ cursor: 'pointer' }}
            onClick={() => handleViewDetail(record)}
          >
            {STATUS_NAMES[s] || s}
          </Tag>
        )
      },
    },
  ]

  const orders: WorkOrder[] = orderData?.content || []
  const activeOrders = orders.filter((o) => !isHistorical(o.status))
  const historyOrders = orders.filter((o) => isHistorical(o.status))
  const pendingCount = activeOrders.filter((o) => o.status === 0).length
  const suspendedCount = activeOrders.filter((o) => o.status === 1).length
  const processingCount = activeOrders.filter((o) => o.status === 2).length

  return (
    <div>
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <Card>
            <Statistic title="待处理" value={pendingCount} valueStyle={{ color: '#faad14' }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="挂起" value={suspendedCount} valueStyle={{ color: '#1890ff' }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="处理中" value={processingCount} valueStyle={{ color: '#4096ff' }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="历史工单"
              value={historyOrders.length}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
      </Row>

      {/* 进行中工单 */}
      <Card title="进行中" style={{ marginBottom: 16 }}>
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: 16,
          }}
        >
          <Space>
            <Button onClick={() => refetch()}>刷新</Button>
          </Space>
          <Dropdown
            menu={{
              items: Object.entries(TYPE_NAMES).map(([value, label]) => ({
                key: value,
                label: <Tag color={TYPE_COLORS[Number(value)]}>{label}</Tag>,
              })),
              onClick: ({ key }) => handleTypeSelect(Number(key)),
            }}
            trigger={['click']}
          >
            <Button type="primary" icon={<PlusOutlined />}>
              新建
            </Button>
          </Dropdown>
        </div>
        <Table
          columns={activeColumns}
          dataSource={activeOrders}
          loading={isLoading}
          rowKey="id"
          pagination={false}
          size="small"
          onHeaderRow={() => ({ style: { textAlign: 'center' } })}
        />
      </Card>

      {/* 历史工单 */}
      <Card title="历史工单">
        <Table
          columns={historyColumns}
          dataSource={historyOrders}
          rowKey="id"
          pagination={{ pageSize: 10, showQuickJumper: true }}
          size="small"
          onHeaderRow={() => ({ style: { textAlign: 'center' } })}
        />
      </Card>

      {/* 创建工单表单 */}
      <Modal
        title={`新建${TYPE_NAMES[selectedType] || ''}工单`}
        open={isCreateModalOpen}
        onCancel={() => {
          setIsCreateModalOpen(false)
          form.resetFields()
        }}
        onOk={() => {
          form.validateFields().then((values) => createMutation.mutate(values))
        }}
        confirmLoading={createMutation.isPending}
        width={560}
      >
        <Form
          form={form}
          layout="vertical"
          initialValues={{ type: selectedType }}
          onPaste={handleFormPaste}
        >
          <Form.Item name="type" label="工单类型">
            <Input disabled />
          </Form.Item>

          <Form.Item
            name="extensionNumber"
            label="分机号"
            rules={[{ required: true, message: '请选择分机号' }]}
          >
            <Select
              showSearch
              filterOption={false}
              onSearch={handleExtSearch}
              onSelect={handleExtSelect}
              loading={extLoading}
              placeholder="输入分机号搜索"
              options={extOptions}
              notFoundContent={extLoading ? '搜索中...' : '无结果'}
            />
          </Form.Item>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="employeeName" label="使用人">
                <Input placeholder="使用人姓名" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="employeeNo" label="员工ID">
                <Input placeholder="员工ID" />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item name="macAddresses" label="MAC">
            <Input placeholder="MAC地址（可粘贴或自动填充）" />
          </Form.Item>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="branchName" label="分行">
                <Input placeholder="自动填充或粘贴" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="deptName" label="部门">
                <Input placeholder="部门（可修改）" />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item name="remark" label="备注">
            <TextArea rows={3} placeholder="备注信息" />
          </Form.Item>
        </Form>
      </Modal>

      {/* 工单详情抽屉 */}
      <Drawer
        title={`工单详情 - ${selectedOrder?.workOrderNo || ''}`}
        open={detailDrawerOpen}
        onClose={() => {
          setDetailDrawerOpen(false)
          setSelectedOrder(null)
        }}
        width={640}
      >
        {selectedOrder && (
          <>
            <Descriptions bordered column={2} size="small" style={{ marginBottom: 24 }}>
              <Descriptions.Item label="工单号">{selectedOrder.workOrderNo}</Descriptions.Item>
              <Descriptions.Item label="标题">{selectedOrder.title}</Descriptions.Item>
              <Descriptions.Item label="类型">
                <Tag color={TYPE_COLORS[selectedOrder.orderType]}>
                  {TYPE_NAMES[selectedOrder.orderType] || selectedOrder.orderType}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={STATUS_COLORS[selectedOrder.status]}>
                  {STATUS_NAMES[selectedOrder.status] || selectedOrder.status}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="申请人">
                {selectedOrder.requesterName || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="处理人">
                {selectedOrder.handlerName || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="创建时间">
                {selectedOrder.createdAt
                  ? selectedOrder.createdAt
                      .replace(/^[0-9]{4}-/, '')
                      .replace('T', ' ')
                      .substring(0, 11)
                  : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="描述" span={2}>
                {selectedOrder.description || '-'}
              </Descriptions.Item>
              {selectedOrder.remark && (
                <Descriptions.Item label="备注" span={2}>
                  {selectedOrder.remark}
                </Descriptions.Item>
              )}
            </Descriptions>
            {selectedOrder.items && selectedOrder.items.length > 0 && (
              <>
                <div style={{ fontWeight: 600, marginBottom: 12, fontSize: 15 }}>工单项</div>
                {selectedOrder.items.map((item: WorkOrderItem) => (
                  <Timeline.Item
                    key={item.id}
                    color={item.status === 2 ? 'green' : item.status === 3 ? 'red' : 'blue'}
                  >
                    <div style={{ paddingBottom: 8 }}>
                      <div>
                        <Tag>{ITEM_TYPE_NAMES[item.itemType] || item.itemType}</Tag>
                        <Tag color={ITEM_STATUS_COLORS[item.status] || 'default'}>
                          {ITEM_STATUS_NAMES[item.status] || item.status}
                        </Tag>
                      </div>
                      {item.description && (
                        <div style={{ color: '#666', marginTop: 4 }}>{item.description}</div>
                      )}
                      {item.action && !item.description && (
                        <div style={{ color: '#666', marginTop: 4 }}>操作: {item.action}</div>
                      )}
                      {item.fromValue && item.toValue && (
                        <div style={{ color: '#999', marginTop: 2 }}>
                          {item.fromValue} → {item.toValue}
                        </div>
                      )}
                      {item.errorMessage && (
                        <div style={{ color: '#ff4d4f', marginTop: 4 }}>
                          错误: {item.errorMessage}
                        </div>
                      )}
                      {(item.status === 0 || item.status === 3) && (
                        <Button
                          size="small"
                          type="primary"
                          style={{ marginTop: 8 }}
                          loading={executeItemMutation.isPending}
                          onClick={() => handleExecuteItem(item.id)}
                        >
                          执行
                        </Button>
                      )}
                    </div>
                  </Timeline.Item>
                ))}
              </>
            )}
          </>
        )}
      </Drawer>
    </div>
  )
}

export default WorkOrderManagement
