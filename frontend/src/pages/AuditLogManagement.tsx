import { useState, useEffect } from 'react'
import { Table, Card, Input, Select, Tag, Button } from 'antd'
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons'
import { auditLogApi } from '@/api/auditLog'
import type { AuditLogEntry } from '@/types/auditLog'
import dayjs from 'dayjs'

const AuditLogManagement = () => {
  const [logs, setLogs] = useState<AuditLogEntry[]>([])
  const [loading, setLoading] = useState(false)
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(0)
  const [moduleFilter, setModuleFilter] = useState<string | undefined>()
  const [operatorFilter, setOperatorFilter] = useState('')

  const fetchLogs = async (p = 0) => {
    setLoading(true)
    try {
      const res = await auditLogApi.search({
        module: moduleFilter,
        operator: operatorFilter || undefined,
        page: p,
        size: 20,
      })
      const data = res.data.data
      setLogs(data.content || [])
      setTotal(data.totalElements || 0)
      setPage(p)
    } catch {
      /* ignore */
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchLogs()
  }, [])

  const columns = [
    {
      title: '时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 150,
      render: (v: string) => (v ? dayjs(v).format('MM-DD HH:mm:ss') : '-'),
    },
    {
      title: '模块',
      dataIndex: 'module',
      key: 'module',
      width: 80,
      render: (v: string) => <Tag>{v}</Tag>,
    },
    { title: '操作', dataIndex: 'operation', key: 'operation', width: 110 },
    { title: '操作人', dataIndex: 'operator', key: 'operator', width: 80 },
    {
      title: '目标',
      key: 'target',
      width: 140,
      render: (_: unknown, r: AuditLogEntry) =>
        r.targetType ? r.targetType + '#' + (r.targetId || '') : '-',
    },
    { title: 'IP', dataIndex: 'ipAddress', key: 'ipAddress', width: 110 },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 60,
      render: (v: string) =>
        v === 'SUCCESS' ? <Tag color="success">成功</Tag> : <Tag color="error">失败</Tag>,
    },
    {
      title: '耗时',
      dataIndex: 'costTime',
      key: 'costTime',
      width: 60,
      render: (v: number) => v + 'ms',
    },
  ]

  // Extract unique modules from API data
  useEffect(() => {
    if (auditData?.content) {
      const uniqueModules = [
        ...new Set(auditData.content.map((item: any) => item.module).filter(Boolean)),
      ]
      if (uniqueModules.length > 0) setModules(uniqueModules as string[])
    }
  }, [auditData])

  const [modules, setModules] = useState<string[]>([
    'org',
    'employee',
    'user',
    'phone',
    'device',
    'bill',
    'invoice',
    'role',
    'work-order',
    'reconciliation',
    'system',
  ])

  return (
    <Card>
      <div style={{ display: 'flex', gap: 12, marginBottom: 16, flexWrap: 'wrap' }}>
        <Select
          placeholder="模块"
          allowClear
          style={{ width: 120 }}
          value={moduleFilter}
          onChange={setModuleFilter}
          options={modules.map((m) => ({ value: m, label: m }))}
        />
        <Input
          placeholder="操作人"
          value={operatorFilter}
          onChange={(e) => setOperatorFilter(e.target.value)}
          style={{ width: 150 }}
          allowClear
        />
        <Button type="primary" icon={<SearchOutlined />} onClick={() => fetchLogs(0)}>
          查询
        </Button>
        <Button
          icon={<ReloadOutlined />}
          onClick={() => {
            setModuleFilter(undefined)
            setOperatorFilter('')
            fetchLogs(0)
          }}
        >
          重置
        </Button>
      </div>
      <Table
        columns={columns}
        dataSource={logs}
        rowKey="id"
        loading={loading}
        pagination={{
          current: page + 1,
          total,
          pageSize: 20,
          onChange: (p) => fetchLogs(p - 1),
          showTotal: (t) => '共 ' + t + ' 条',
        }}
        scroll={{ x: 900 }}
        size="small"
      />
    </Card>
  )
}

export default AuditLogManagement
