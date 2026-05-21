import { useState } from 'react'
import { Table, Button, Card, Select, Tag, Space, Modal, message, Upload, Row, Col, Statistic } from 'antd'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { invoiceApi, Invoice } from '@/api/invoice'
import { orgApi } from '@/api/org'
import { UploadOutlined, FilePdfOutlined, DeleteOutlined, CheckOutlined } from '@ant-design/icons'

const { Option } = Select

const STATUS_COLORS: Record<string, string> = {
  PENDING: 'warning',
  CONFIRMED: 'success',
  DISTRIBUTED: 'processing',
  REJECTED: 'error'
}

const STATUS_NAMES: Record<string, string> = {
  PENDING: '待处理',
  CONFIRMED: '已确认',
  DISTRIBUTED: '已分发',
  REJECTED: '已拒绝'
}

const InvoiceManagement = () => {
  const [billMonth, setBillMonth] = useState<string>(new Date().toISOString().slice(0, 7))
  const [status, setStatus] = useState<string>('')
  const [isUploadModalOpen, setIsUploadModalOpen] = useState(false)
  const [uploading, setUploading] = useState(false)
  const queryClient = useQueryClient()

  const months = Array.from({ length: 12 }, (_, i) => {
    const d = new Date()
    d.setMonth(d.getMonth() - i)
    return d.toISOString().slice(0, 7)
  })

  const { data: orgsData } = useQuery({
    queryKey: ['orgs'],
    queryFn: async () => {
      const response = await orgApi.getAll()
      return response.data.data
    }
  })

  const { data: allocationData, isLoading, refetch } = useQuery({
    queryKey: ['invoices', billMonth, status],
    queryFn: async () => {
      const params: any = { billMonth, page: 0, size: 100 }
      if (status) params.status = status
      return invoiceApi.getInvoices(params)
    }
  })

  const { data: statsData } = useQuery({
    queryKey: ['invoice-stats', billMonth],
    queryFn: () => invoiceApi.getStats(billMonth)
  })

  const confirmMutation = useMutation({
    mutationFn: (id: number) => invoiceApi.confirm(id),
    onSuccess: () => {
      message.success('发票已确认')
      queryClient.invalidateQueries({ queryKey: ['invoices'] })
      queryClient.invalidateQueries({ queryKey: ['invoice-stats'] })
    },
    onError: () => message.error('确认发票失败')
  })

  const deleteMutation = useMutation({
    mutationFn: (id: number) => invoiceApi.delete(id),
    onSuccess: () => {
      message.success('发票已删除')
      queryClient.invalidateQueries({ queryKey: ['invoices'] })
      queryClient.invalidateQueries({ queryKey: ['invoice-stats'] })
    },
    onError: () => message.error('删除发票失败')
  })

  const handleUpload = async (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('billMonth', billMonth)
    formData.append('sourceOrgId', '1')

    setUploading(true)
    try {
      await invoiceApi.upload(formData)
      message.success('发票上传成功')
      queryClient.invalidateQueries({ queryKey: ['invoices'] })
      queryClient.invalidateQueries({ queryKey: ['invoice-stats'] })
      setIsUploadModalOpen(false)
    } catch (error: any) {
      message.error(error.response?.data?.message || '上传失败')
    } finally {
      setUploading(false)
    }
    return false
  }

  const handleConfirm = (record: Invoice) => {
    Modal.confirm({
      title: '确认发票',
      content: `确认发票 ${record.invoiceNo}？`,
      onOk: () => confirmMutation.mutate(record.id)
    })
  }

  const handleDelete = (record: Invoice) => {
    Modal.confirm({
      title: '删除发票',
      content: `删除发票 ${record.invoiceNo}？`,
      onOk: () => deleteMutation.mutate(record.id)
    })
  }

  const columns = [
    { title: '发票编号', dataIndex: 'invoiceNo', key: 'invoiceNo', width: 180 },
    { title: '类型', dataIndex: 'invoiceType', key: 'invoiceType', width: 100 },
    { title: '来源组织', dataIndex: 'sourceOrgName', key: 'sourceOrgName', width: 150 },
    { title: '金额', dataIndex: 'amount', key: 'amount', width: 120,
      render: (val: number) => `¥${val?.toFixed(2) || '0.00'}` },
    { title: '税额', dataIndex: 'taxAmount', key: 'taxAmount', width: 100,
      render: (val: number) => `¥${val?.toFixed(2) || '0.00'}` },
    { title: '总计', dataIndex: 'totalAmount', key: 'totalAmount', width: 120,
      render: (val: number) => `¥${val?.toFixed(2) || '0.00'}` },
    { title: '开票日期', dataIndex: 'issueDate', key: 'issueDate', width: 120 },
    { title: '账单月份', dataIndex: 'billMonth', key: 'billMonth', width: 100 },
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
      width: 150,
      render: (_: any, record: Invoice) => (
        <Space>
          {record.status === 'PENDING' && (
            <Button size="small" type="primary" icon={<CheckOutlined />} onClick={() => handleConfirm(record)}>
              确认
            </Button>
          )}
          <Button size="small" danger icon={<DeleteOutlined />} onClick={() => handleDelete(record)}>
            删除
          </Button>
        </Space>
      )
    }
  ]

  const invoices = allocationData?.data?.data?.content || []
  const stats = statsData?.data?.data

  return (
    <div>
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <Card><Statistic title="总数" value={stats?.total || 0} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="待处理" value={stats?.pending || 0} valueStyle={{ color: '#faad14' }} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="已确认" value={stats?.confirmed || 0} valueStyle={{ color: '#3f8600' }} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="已分发" value={stats?.distributed || 0} valueStyle={{ color: '#1890ff' }} /></Card>
        </Col>
      </Row>

      <Card>
        <Space style={{ marginBottom: 16 }}>
          <Select value={billMonth} onChange={setBillMonth} style={{ width: 150 }}>
            {months.map(m => <Option key={m} value={m}>{m}</Option>)}
          </Select>
          <Select value={status} onChange={setStatus} style={{ width: 150 }} allowClear placeholder="选择状态">
            <Option value="PENDING">待处理</Option>
            <Option value="CONFIRMED">已确认</Option>
            <Option value="DISTRIBUTED">已分发</Option>
          </Select>
          <Button type="primary" icon={<UploadOutlined />} onClick={() => setIsUploadModalOpen(true)}>
            上传发票
          </Button>
          <Button onClick={() => refetch()}>刷新</Button>
        </Space>

        <Table
          columns={columns}
          dataSource={invoices}
          loading={isLoading}
          rowKey="id"
          pagination={{ pageSize: 20, total: allocationData?.data?.data?.totalElements }}
        />
      </Card>

      <Modal
        title="上传发票"
        open={isUploadModalOpen}
        onCancel={() => setIsUploadModalOpen(false)}
        footer={null}
      >
        <Space direction="vertical" style={{ width: '100%' }}>
          <Select value={billMonth} onChange={setBillMonth} style={{ width: '100%' }} placeholder="选择账单月份">
            {months.map(m => <Option key={m} value={m}>{m}</Option>)}
          </Select>
          <Upload.Dragger
            accept=".pdf,.jpg,.jpeg,.png"
            beforeUpload={handleUpload}
            showUploadList={false}
          >
            <p className="ant-upload-drag-icon">
              <FilePdfOutlined />
            </p>
            <p className="ant-upload-text">点击或拖拽发票文件上传</p>
            <p className="ant-upload-hint">支持 PDF、JPG、PNG 格式</p>
          </Upload.Dragger>
        </Space>
      </Modal>
    </div>
  )
}

export default InvoiceManagement
