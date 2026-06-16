import { useState } from 'react'
import {
  Table,
  Button,
  Card,
  Select,
  Tag,
  Space,
  Modal,
  message,
  Upload,
  Row,
  Col,
  Statistic,
  Alert,
} from 'antd'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { invoiceApi, type Invoice, type BatchUploadResult } from '@/api/invoice'
import { ApiGet } from '@/api/request'
import type { OrgStructure } from '@/types/org'
import { UploadOutlined, FilePdfOutlined, DeleteOutlined, CheckOutlined } from '@ant-design/icons'

const { Option } = Select

// Backend status: 0=PENDING, 1=DISTRIBUTED, 2=READ, 3=CONFIRMED
const STATUS_COLORS: Record<number, string> = {
  0: 'warning',
  1: 'success',
  2: 'processing',
  3: 'default',
}

const STATUS_NAMES: Record<number, string> = {
  0: '待处理',
  1: '已分发',
  2: '已读',
  3: '已确认',
}

interface InvoiceListParams {
  billMonth: string
  page: number
  size: number
  status?: string | number
}

const InvoiceManagement = () => {
  const [billMonth, setBillMonth] = useState<string>(new Date().toISOString().slice(0, 7))
  const [status, setStatus] = useState<number | ''>('')
  const [isUploadModalOpen, setIsUploadModalOpen] = useState(false)
  const [fileList, setFileList] = useState<File[]>([])
  const [uploading, setUploading] = useState(false)
  const [uploadResult, setUploadResult] = useState<BatchUploadResult | null>(null)
  const queryClient = useQueryClient()

  // Convert "2026-05" to "202605" for backend
  const billMonthForApi = billMonth.replace('-', '')

  const months = Array.from({ length: 12 }, (_, i) => {
    const d = new Date()
    d.setMonth(d.getMonth() - i)
    return d.toISOString().slice(0, 7)
  })

  useQuery({
    queryKey: ['orgs'],
    queryFn: async () => {
      const response = await ApiGet<OrgStructure[]>('/orgs')
      return response || []
    },
  })

  const {
    data: allocationData,
    isLoading,
    refetch,
  } = useQuery({
    queryKey: ['invoices', billMonthForApi, status],
    queryFn: async () => {
      const params: InvoiceListParams = { billMonth: billMonthForApi, page: 0, size: 100 }
      if (status !== '') params.status = status
      return invoiceApi.getList(params)
    },
  })

  const { data: statsData } = useQuery({
    queryKey: ['invoice-stats', billMonthForApi],
    queryFn: () =>
      ApiGet<Record<string, number>>('/invoices/statistics', {
        params: { billMonth: billMonthForApi },
      }),
  })

  const confirmMutation = useMutation({
    mutationFn: (id: number) => invoiceApi.confirm(id),
    onSuccess: () => {
      message.success('发票已确认')
      queryClient.invalidateQueries({ queryKey: ['invoices'] })
      queryClient.invalidateQueries({ queryKey: ['invoice-stats'] })
    },
    onError: () => message.error('确认发票失败'),
  })

  const deleteMutation = useMutation({
    mutationFn: (id: number) => invoiceApi.delete(id),
    onSuccess: () => {
      message.success('发票已删除')
      queryClient.invalidateQueries({ queryKey: ['invoices'] })
      queryClient.invalidateQueries({ queryKey: ['invoice-stats'] })
    },
    onError: () => message.error('删除发票失败'),
  })

  const handleBatchUpload = async () => {
    if (fileList.length === 0) {
      message.warning('请选择至少一个PDF文件')
      return
    }
    setUploading(true)
    setUploadResult(null)
    try {
      const result = await invoiceApi.batchUpload(fileList, billMonthForApi)
      setUploadResult(result)
      if (result.success > 0) {
        message.success(`成功上传 ${result.success} 个发票`)
        queryClient.invalidateQueries({ queryKey: ['invoices'] })
        queryClient.invalidateQueries({ queryKey: ['invoice-stats'] })
      }
      if (result.failed > 0) {
        message.warning(`${result.failed} 个文件上传失败，请查看详情`)
      }
    } catch (error: unknown) {
      const msg = error instanceof Error ? error.message : '批量上传失败'
      message.error(msg)
    } finally {
      setUploading(false)
    }
  }

  const handleConfirm = (record: Invoice) => {
    Modal.confirm({
      title: '确认发票',
      content: `确认发票 ${record.invoiceNo}？`,
      onOk: () => confirmMutation.mutate(record.id),
    })
  }

  const handleDelete = (record: Invoice) => {
    Modal.confirm({
      title: '删除发票',
      content: `删除发票 ${record.invoiceNo}？`,
      onOk: () => deleteMutation.mutate(record.id),
    })
  }

  const columns = [
    { title: '发票编号', dataIndex: 'invoiceNo', key: 'invoiceNo', width: 180 },
    { title: '来源组织', dataIndex: 'sourceOrgName', key: 'sourceOrgName', width: 150 },
    {
      title: '金额',
      dataIndex: 'amount',
      key: 'amount',
      width: 120,
      render: (val: number | null) => (val !== null ? '\u00A5' + val.toFixed(2) : '-'),
    },
    {
      title: '税额',
      dataIndex: 'taxAmount',
      key: 'taxAmount',
      width: 100,
      render: (val: number | null) => (val !== null ? '\u00A5' + val.toFixed(2) : '-'),
    },
    { title: '开票日期', dataIndex: 'invoiceDate', key: 'invoiceDate', width: 120 },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: number) => (
        <Tag color={STATUS_COLORS[status] || 'default'}>{STATUS_NAMES[status]}</Tag>
      ),
    },
    {
      title: '操作',
      key: 'actions',
      width: 150,
      render: (_: unknown, record: Invoice) => (
        <Space>
          {(record.status === 0 || record.status === 1) && (
            <Button
              size="small"
              type="primary"
              icon={<CheckOutlined />}
              onClick={() => handleConfirm(record)}
            >
              确认
            </Button>
          )}
          <Button
            size="small"
            danger
            icon={<DeleteOutlined />}
            onClick={() => handleDelete(record)}
          >
            删除
          </Button>
        </Space>
      ),
    },
  ]

  const invoices = allocationData?.content || []
  const stats = statsData

  const closeUploadModal = () => {
    setIsUploadModalOpen(false)
    setFileList([])
    setUploadResult(null)
  }

  return (
    <div>
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <Card>
            <Statistic title="总数" value={stats?.total || 0} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="待处理"
              value={stats?.pending || 0}
              valueStyle={{ color: '#faad14' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="已分发"
              value={stats?.distributed || 0}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="已确认"
              value={stats?.confirmed || 0}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
      </Row>

      <Card>
        <Space style={{ marginBottom: 16 }}>
          <Select value={billMonth} onChange={setBillMonth} style={{ width: 150 }}>
            {months.map((m) => (
              <Option key={m} value={m}>
                {m}
              </Option>
            ))}
          </Select>
          <Select
            value={status}
            onChange={setStatus}
            style={{ width: 150 }}
            allowClear
            placeholder="选择状态"
          >
            <Option value={0}>待处理</Option>
            <Option value={1}>已分发</Option>
            <Option value={2}>已读</Option>
            <Option value={3}>已确认</Option>
          </Select>
          <Button
            type="primary"
            icon={<UploadOutlined />}
            onClick={() => setIsUploadModalOpen(true)}
          >
            批量上传
          </Button>
          <Button onClick={() => refetch()}>刷新</Button>
        </Space>

        <Table
          columns={columns}
          dataSource={invoices}
          loading={isLoading}
          rowKey="id"
          pagination={{ pageSize: 20, total: allocationData?.totalElements, showQuickJumper: true, showSizeChanger: true }}
        />
      </Card>

      <Modal
        title="批量上传发票"
        open={isUploadModalOpen}
        onCancel={closeUploadModal}
        width={640}
        footer={[
          <Button key="cancel" onClick={closeUploadModal}>
            关闭
          </Button>,
          <Button
            key="upload"
            type="primary"
            loading={uploading}
            disabled={fileList.length === 0}
            onClick={handleBatchUpload}
          >
            上传 {fileList.length > 0 ? `(${fileList.length}个文件)` : ''}
          </Button>,
        ]}
      >
        <Space direction="vertical" style={{ width: '100%' }} size="middle">
          <div>
            <div style={{ marginBottom: 8, fontWeight: 500 }}>账单月份</div>
            <Select value={billMonth} onChange={setBillMonth} style={{ width: '100%' }}>
              {months.map((m) => (
                <Option key={m} value={m}>
                  {m}
                </Option>
              ))}
            </Select>
          </div>

          <Alert
            type="info"
            showIcon
            message="文件命名规则：分行名_其他信息.pdf，例如 北京_202605_001.pdf"
            description="系统将自动从文件名前缀匹配分行。如未匹配到，将分配至默认组织。"
          />

          <Upload.Dragger
            accept=".pdf"
            multiple
            beforeUpload={(file) => {
              setFileList((prev) => [...prev, file])
              return false // prevent auto upload
            }}
            onRemove={(file) => {
              setFileList((prev) =>
                prev.filter((f) => f.name !== file.name || f.size !== file.size)
              )
              return false
            }}
            fileList={fileList.map((f, idx) => ({
              uid: `${idx}`,
              name: f.name,
              status: 'done' as const,
            }))}
          >
            <p className="ant-upload-drag-icon">
              <FilePdfOutlined />
            </p>
            <p className="ant-upload-text">点击或拖拽PDF文件上传</p>
            <p className="ant-upload-hint">支持同时选择多个PDF文件</p>
          </Upload.Dragger>

          {uploadResult && (
            <div>
              <Alert
                type={uploadResult.failed > 0 ? 'warning' : 'success'}
                showIcon
                message={`上传完成：成功 ${uploadResult.success} 个，失败 ${uploadResult.failed} 个`}
                style={{ marginBottom: 8 }}
              />
              {uploadResult.details.length > 0 && (
                <Table
                  size="small"
                  pagination={false}
                  dataSource={uploadResult.details.map((d, i) => ({ ...d, key: i }))}
                  columns={[
                    { title: '文件名', dataIndex: 'fileName', key: 'fileName' },
                    {
                      title: '匹配组织',
                      dataIndex: 'matchedOrg',
                      key: 'matchedOrg',
                      render: (v: string) => v || '-',
                    },
                    {
                      title: '状态',
                      dataIndex: 'status',
                      key: 'status',
                      render: (v: string) =>
                        v === 'success' ? (
                          <Tag color="success">成功</Tag>
                        ) : (
                          <Tag color="error">失败</Tag>
                        ),
                    },
                    {
                      title: '原因',
                      dataIndex: 'reason',
                      key: 'reason',
                      render: (v: string) => v || '-',
                    },
                  ]}
                />
              )}
            </div>
          )}
        </Space>
      </Modal>
    </div>
  )
}

export default InvoiceManagement
