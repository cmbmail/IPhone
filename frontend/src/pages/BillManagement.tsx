import { useState } from 'react'
import {
  Card,
  Select,
  Button,
  Upload,
  Modal,
  message,
  Space,
  Row,
  Col,
  Statistic,
  Table,
  Tabs,
  Tag,
  Tooltip,
  Input,
} from 'antd'
import {
  UploadOutlined,
  FileTextOutlined,
  PhoneOutlined,
  AudioOutlined,
  NotificationOutlined,
  ThunderboltOutlined,
  DeleteOutlined,
  ExclamationCircleOutlined,
} from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { ColumnsType } from 'antd/es/table'
import { billApi } from '@/api/bill'
import { ApiGet } from '@/api/request'

const { Option } = Select

interface BillRecord {
  id: number
  billMonth: string
  fileName: string
  phoneNumber: string
  employeeNo: string
  deptName: string
  extensionNumber: string
  platformUsageFee: number
  numberMonthlyRent: number
  outboundDuration: number
  transferOutboundDuration: number
  domesticCharge: number
  internationalDuration: number
  internationalCharge: number
  chargeAmount: number
  chargeType: number
  allocationTime: string | null
  activationTime: string | null
  deactivationTime: string | null
  billingStartDate: string | null
  billingEndDate: string | null
  days: number
  sendCount: number | null
  remark: string
  importStatus: number
  importedBy: string
  importedAt: string
  city: string | null
  subNumber: string | null
}

interface PageData {
  content: BillRecord[]
  totalElements: number
  totalPages: number
  number: number
}

const CHARGE_TABS = [
  { key: '0', label: '号码费用', icon: <PhoneOutlined />, color: '#1677ff' },
  { key: '1', label: '录音费用', icon: <AudioOutlined />, color: '#52c41a' },
  { key: '2', label: '彩铃费用', icon: <NotificationOutlined />, color: '#eb2f96' },
  { key: '3', label: '闪信费用', icon: <ThunderboltOutlined />, color: '#fa8c16' },
]

const fmtMoney = (v: number | null | undefined) => {
  if (v === null || v === undefined) return '-'
  return `¥${Number(v).toFixed(2)}`
}

const fmtDuration = (v: number | null | undefined) => {
  if (v === null || v === undefined || v === 0) return '-'
  const h = Math.floor(v / 3600)
  const m = Math.floor((v % 3600) / 60)
  const s = v % 60
  if (h > 0) return `${h}时${m}分${s}秒`
  if (m > 0) return `${m}分${s}秒`
  return `${s}秒`
}

// 号码费用列
const phoneColumns: ColumnsType<BillRecord> = [
  {
    title: '电话号码',
    dataIndex: 'phoneNumber',
    key: 'pn',
    width: 140,
    fixed: 'left',
    render: (v: string) => <span style={{ fontFamily: 'monospace' }}>{v || '-'}</span>,
    align: 'center',
  },
  {
    title: '平台使用费',
    dataIndex: 'platformUsageFee',
    key: 'pf',
    width: 110,
    align: 'right',
    render: (v: number) => fmtMoney(v),
  },
  {
    title: '码号月租费',
    dataIndex: 'numberMonthlyRent',
    key: 'mr',
    width: 110,
    align: 'right',
    render: (v: number) => fmtMoney(v),
  },
  {
    title: '外呼时长',
    dataIndex: 'outboundDuration',
    key: 'od',
    width: 100,
    align: 'right',
    render: (v: number) => fmtDuration(v),
  },
  {
    title: '转接外呼时长',
    dataIndex: 'transferOutboundDuration',
    key: 'tod',
    width: 120,
    align: 'right',
    render: (v: number) => fmtDuration(v),
  },
  {
    title: '国内费用',
    dataIndex: 'domesticCharge',
    key: 'dc',
    width: 100,
    align: 'right',
    render: (v: number) => fmtMoney(v),
  },
  {
    title: '国际时长',
    dataIndex: 'internationalDuration',
    key: 'id2',
    width: 100,
    align: 'right',
    render: (v: number) => fmtDuration(v),
  },
  {
    title: '国际费用',
    dataIndex: 'internationalCharge',
    key: 'ic',
    width: 100,
    align: 'right',
    render: (v: number) => fmtMoney(v),
  },
  {
    title: '费用小计',
    dataIndex: 'chargeAmount',
    key: 'total',
    width: 110,
    align: 'right',
    render: (v: number) => <span style={{ fontWeight: 700, color: '#1677ff' }}>{fmtMoney(v)}</span>,
  },
  {
    title: '备注',
    dataIndex: 'remark',
    key: 'rm',
    width: 150,
    ellipsis: true,
    render: (v: string) => <Tooltip title={v}>{v || '-'}</Tooltip>,
    align: 'center',
  },
]

// 录音费用列
const recordingColumns: ColumnsType<BillRecord> = [
  {
    title: '分机号',
    dataIndex: 'extensionNumber',
    key: 'ext',
    width: 120,
    render: (v: string) => <span style={{ fontFamily: 'monospace' }}>{v || '-'}</span>,
    align: 'center',
  },
  {
    title: '电话号码',
    dataIndex: 'phoneNumber',
    key: 'pn',
    width: 140,
    render: (v: string) => <span style={{ fontFamily: 'monospace' }}>{v || '-'}</span>,
    align: 'center',
  },
  {
    title: '开启时间',
    dataIndex: 'activationTime',
    key: 'at',
    width: 110,
    render: (v: string) => v || '-',
    align: 'center',
  },
  {
    title: '关闭时间',
    dataIndex: 'deactivationTime',
    key: 'dt',
    width: 110,
    render: (v: string) => v || '-',
    align: 'center',
  },
  {
    title: '天数',
    dataIndex: 'days',
    key: 'days',
    width: 80,
    align: 'center',
    render: (v: number) => (v !== null && v !== undefined ? `${v}天` : '-'),
  },
  {
    title: '费用小计',
    dataIndex: 'chargeAmount',
    key: 'total',
    width: 120,
    align: 'right',
    render: (v: number) => <span style={{ fontWeight: 700, color: '#52c41a' }}>{fmtMoney(v)}</span>,
  },
]

// 通用列：状态 + 导入时间

// 彩铃费用列
const ringtoneColumns: ColumnsType<BillRecord> = [
  {
    title: '分机号',
    dataIndex: 'extensionNumber',
    key: 'ext',
    width: 120,
    render: (v: string) => <span style={{ fontFamily: 'monospace' }}>{v || '-'}</span>,
    align: 'center',
  },
  {
    title: '电话号码',
    dataIndex: 'phoneNumber',
    key: 'pn',
    width: 140,
    render: (v: string) => <span style={{ fontFamily: 'monospace' }}>{v || '-'}</span>,
    align: 'center',
  },
  {
    title: '开通时间',
    dataIndex: 'activationTime',
    key: 'at',
    width: 110,
    render: (v: string) => v || '-',
    align: 'center',
  },
  {
    title: '费用',
    dataIndex: 'chargeAmount',
    key: 'total',
    width: 120,
    align: 'right',
    render: (v: number) => <span style={{ fontWeight: 700, color: '#eb2f96' }}>{fmtMoney(v)}</span>,
  },
]

// 闪信费用列
const flashSmsColumns: ColumnsType<BillRecord> = [
  {
    title: '月份',
    dataIndex: 'billMonth',
    key: 'bm',
    width: 100,
    render: (v: string) => v || '-',
    align: 'center',
  },
  {
    title: '主号码',
    dataIndex: 'phoneNumber',
    key: 'pn',
    width: 140,
    render: (v: string) => <span style={{ fontFamily: 'monospace' }}>{v || '-'}</span>,
    align: 'center',
  },
  {
    title: '子号码',
    dataIndex: 'subNumber',
    key: 'sn',
    width: 140,
    render: (v: string) => <span style={{ fontFamily: 'monospace' }}>{v || '-'}</span>,
    align: 'center',
  },
  {
    title: '地市',
    dataIndex: 'city',
    key: 'city',
    width: 100,
    render: (v: string) => v || '-',
    align: 'center',
  },
  {
    title: '下发量',
    dataIndex: 'sendCount',
    key: 'sc',
    width: 100,
    align: 'right',
    render: (v: number) =>
      v !== null && v !== undefined ? (
        <span style={{ fontWeight: 700, color: '#fa8c16' }}>{v.toLocaleString()} 条</span>
      ) : (
        '-'
      ),
  },
  {
    title: '费用',
    key: 'fee',
    width: 120,
    align: 'right',
    render: (_: unknown, r: BillRecord) =>
      r.sendCount !== null && r.sendCount !== undefined ? (
        <span style={{ fontWeight: 700, color: '#fa8c16' }}>¥{(r.sendCount * 0.1).toFixed(2)}</span>
      ) : (
        '-'
      ),
  },
]

const commonColumns: ColumnsType<BillRecord> = [
  {
    title: '导入时间',
    dataIndex: 'importedAt',
    key: 'ia',
    width: 160,
    render: (v: string) => (v ? v.replace('T', ' ').substring(0, 19) : '-'),
    align: 'center',
  },
]

const getColumns = (tabKey: string): ColumnsType<BillRecord> => {
  if (tabKey === '0') return [...phoneColumns, ...commonColumns]
  if (tabKey === '1') return [...recordingColumns, ...commonColumns]
  if (tabKey === '2') return [...ringtoneColumns, ...commonColumns]
  if (tabKey === '3') return [...flashSmsColumns, ...commonColumns]
  return [...phoneColumns, ...commonColumns]
}

const getEmptyText = (tabKey: string) => {
  const tab = CHARGE_TABS.find((t) => t.key === tabKey)
  return `暂无${tab?.label || ''}数据，请上传账单文件`
}

const BillManagement = () => {
  const [activeTab, setActiveTab] = useState('0')
  const [billMonth, setBillMonth] = useState<string>(new Date().toISOString().slice(0, 7))
  const [isUploadModalOpen, setIsUploadModalOpen] = useState(false)
  const [uploadMode, setUploadMode] = useState<'import' | 'importAndAllocate'>('import')
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false)
  const [deletePassword, setDeletePassword] = useState('')
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(20)
  const queryClient = useQueryClient()

  const chargeType = CHARGE_TABS.find((t) => t.key === activeTab)?.key || activeTab
  const tabConfig = CHARGE_TABS.find((t) => t.key === activeTab)!

  // Convert "2026-05" to "202605" for backend API
  const billMonthForApi = billMonth.replace('-', '')

  const months = Array.from({ length: 12 }, (_, i) => {
    const d = new Date()
    d.setMonth(d.getMonth() - i)
    return d.toISOString().slice(0, 7)
  })

  const { data: billData, isLoading } = useQuery({
    queryKey: ['bills', billMonthForApi, chargeType, page, pageSize],
    queryFn: async () => {
      return ApiGet<PageData>('/bills', {
        params: { billMonth: billMonthForApi, chargeType, page, size: pageSize },
      })
    },
  })

  const deleteMutation = useMutation({
    mutationFn: (password: string) => billApi.delete(billMonthForApi, chargeType, password),
    onSuccess: () => {
      message.success('删除成功')
      setIsDeleteModalOpen(false)
      setDeletePassword('')
      queryClient.invalidateQueries({ queryKey: ['bills'] })
    },
    onError: () => {
      message.error('密码错误，删除失败')
    },
  })

  const columns = getColumns(activeTab)
  const scrollXMap: Record<string, number> = { 0: 1830, 1: 1120, 2: 910, 3: 1030 }
  const scrollX = scrollXMap[activeTab] || 2090

  const importMutation = useMutation({
    mutationFn: ({ file, month }: { file: File; month: string }) =>
      billApi.importBills(month, file),
    onSuccess: () => {
      message.success('导入成功')
      setIsUploadModalOpen(false)
      queryClient.invalidateQueries({ queryKey: ['bills'] })
    },
    onError: (error: unknown) => {
      const msg = error instanceof Error ? error.message : '导入失败'
      message.error(msg)
    },
  })

  const importAndAllocateMutation = useMutation({
    mutationFn: ({ file, month }: { file: File; month: string }) =>
      billApi.importAndAllocate(month, file),
    onSuccess: () => {
      message.success('导入并分摊成功')
      setIsUploadModalOpen(false)
      queryClient.invalidateQueries({ queryKey: ['bills'] })
    },
    onError: (error: unknown) => {
      const msg = error instanceof Error ? error.message : '导入分摊失败'
      message.error(msg)
    },
  })
  const handleUpload = (file: File) => {
    if (uploadMode === 'importAndAllocate') {
      importAndAllocateMutation.mutate({ file, month: billMonthForApi })
    } else {
      importMutation.mutate({ file, month: billMonthForApi })
    }
    return false
  }

  const tabItems = CHARGE_TABS.map((tab) => ({
    key: tab.key,
    label: (
      <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
        {tab.icon}
        {tab.label}
      </span>
    ),
  }))

  return (
    <div>
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <Card size="small">
            <Statistic
              title="当前费用类型"
              value={tabConfig.label}
              valueStyle={{ color: tabConfig.color, fontSize: 20 }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic title="账单月份" value={billMonth} />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic title="记录数" value={billData?.totalElements ?? 0} suffix="条" />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic
              title="总金额"
              value={(billData?.content || []).reduce(
                (s: number, r: BillRecord) => s + (r.chargeAmount || 0),
                0
              )}
              prefix="¥"
              suffix="元"
              valueStyle={{ color: tabConfig.color }}
            />
          </Card>
        </Col>
      </Row>

      <Card>
        <Tabs
          activeKey={activeTab}
          onChange={(key) => {
            setActiveTab(key)
            setPage(0)
          }}
          items={tabItems}
          tabBarExtraContent={
            <Space>
              <Select
                value={billMonth}
                onChange={(v) => {
                  setBillMonth(v)
                  setPage(0)
                }}
                style={{ width: 130 }}
              >
                {months.map((m) => (
                  <Option key={m} value={m}>
                    {m}
                  </Option>
                ))}
              </Select>
              <Button danger icon={<DeleteOutlined />} onClick={() => setIsDeleteModalOpen(true)}>
                删除当月账单
              </Button>
              <Button
                type="primary"
                icon={<UploadOutlined />}
                onClick={() => setIsUploadModalOpen(true)}
              >
                上传账单
              </Button>
            </Space>
          }
        />

        <div style={{ overflowX: 'auto' }}>
          <Table<BillRecord>
            rowKey="id"
            columns={columns}
            dataSource={billData?.content ?? []}
            loading={isLoading}
            locale={{ emptyText: getEmptyText(activeTab) }}
            pagination={{
              current: page + 1,
              pageSize,
              total: billData?.totalElements ?? 0,
              showSizeChanger: true,
              showTotal: (total) => `共 ${total} 条`,
              onChange: (p, ps) => {
                setPage(p - 1)
                setPageSize(ps)
              },
            }}
            size="middle"
            scroll={{ x: scrollX }}
          />
        </div>
      </Card>

      <Modal
        title={`上传${tabConfig.label}账单`}
        open={isUploadModalOpen}
        onCancel={() => setIsUploadModalOpen(false)}
        footer={null}
        width={520}
      >
        <Space direction="vertical" style={{ width: '100%', gap: 16 }}>
          <div
            style={{
              background: '#f6f8fa',
              padding: 16,
              borderRadius: 8,
              fontSize: 13,
              color: '#555',
            }}
          >
            <div style={{ fontWeight: 600, marginBottom: 8, color: '#333' }}>上传说明</div>
            <ul style={{ margin: 0, paddingLeft: 18 }}>
              <li>
                费用类型：<Tag color={tabConfig.color}>{tabConfig.label}</Tag>
              </li>
              <li>
                账单月份：<Tag>{billMonth}</Tag>
              </li>
              <li>支持格式：Excel (.xlsx)</li>
              {activeTab === '0' && (
                <li>
                  Sheet名需含「号码」，列：号码、分配时间、用户ID、部门、平台使用费、码号月租费、外呼时长、转接外呼时长、国内费用、国际时长、国际费用、费用小计、备注
                </li>
              )}
              {activeTab === '1' && (
                <li>
                  Sheet名需含「录音」，列：分机号、号码、开启时间、关闭时间、开始时间、结束时间、天数、费用小计
                </li>
              )}
              {activeTab === '2' && <li>Sheet名需含「彩铃」，列：分机号、号码、开通时间、费用</li>}
              {activeTab === '3' && <li>闪信费用按Excel中「月份」列自动分配到对应月度账单</li>}
              {activeTab === '3' && (
                <li>Sheet名需含「闪信」，列：月份、主号码、子号码、地市、下发量</li>
              )}
            </ul>
          </div>

          <div style={{ marginBottom: 16 }}>
            <Space>
              <Button
                type={uploadMode === 'import' ? 'primary' : 'default'}
                onClick={() => setUploadMode('import')}
              >
                仅导入
              </Button>
              <Button
                type={uploadMode === 'importAndAllocate' ? 'primary' : 'default'}
                onClick={() => setUploadMode('importAndAllocate')}
              >
                导入并分摊
              </Button>
            </Space>
            <div style={{ fontSize: 12, color: '#8c8c8c', marginTop: 4 }}>
              {uploadMode === 'import'
                ? '仅导入账单数据，不自动分摊'
                : '导入账单后自动执行费用分摊到各组织'}
            </div>
          </div>

          <Upload.Dragger accept=".xlsx,.xls" beforeUpload={handleUpload} showUploadList={false}>
            <p className="ant-upload-drag-icon">
              <FileTextOutlined style={{ color: tabConfig.color, fontSize: 48 }} />
            </p>
            <p className="ant-upload-text">点击或拖拽账单文件到此处</p>
            <p className="ant-upload-hint">支持 XLSX 格式</p>
          </Upload.Dragger>
        </Space>
      </Modal>
      <Modal
        title={
          <span>
            <ExclamationCircleOutlined style={{ color: '#ff4d4f', marginRight: 8 }} />
            删除账单确认
          </span>
        }
        open={isDeleteModalOpen}
        onCancel={() => {
          setIsDeleteModalOpen(false)
          setDeletePassword('')
        }}
        onOk={() => deleteMutation.mutate(deletePassword)}
        okText="确认删除"
        cancelText="取消"
        okButtonProps={{
          danger: true,
          loading: deleteMutation.isPending,
          disabled: !deletePassword,
        }}
        width={420}
      >
        <div style={{ marginBottom: 16 }}>
          <div style={{ color: '#ff4d4f', fontWeight: 600, marginBottom: 12 }}>
            请再次确认操作的账单和月份！
          </div>
          <div
            style={{
              background: '#fff7e6',
              padding: 12,
              borderRadius: 6,
              fontSize: 14,
              lineHeight: '22px',
            }}
          >
            <div>
              费用类型：<strong>{tabConfig.label}</strong>
            </div>
            <div>
              账单月份：<strong>{billMonth}</strong>
            </div>
            <div>
              记录数量：<strong>{billData?.totalElements ?? 0} 条</strong>
            </div>
          </div>
        </div>
        <div>
          <div style={{ marginBottom: 6, fontSize: 14, color: '#666' }}>
            请输入登录密码以确认操作：
          </div>
          <Input.Password
            placeholder="请输入密码"
            value={deletePassword}
            onChange={(e) => setDeletePassword(e.target.value)}
            onPressEnter={() => deletePassword && deleteMutation.mutate(deletePassword)}
            autoComplete="new-password"
            autoFocus
          />
        </div>
      </Modal>
    </div>
  )
}

export default BillManagement
