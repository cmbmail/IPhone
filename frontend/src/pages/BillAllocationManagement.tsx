import { useState } from 'react'
import { Table, Button, Card, Select, Tag, Space, Modal, message, Statistic, Row, Col } from 'antd'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { billAllocationApi, BillAllocation } from '@/api/billAllocation'
import { orgApi } from '@/api/org'

const { Option } = Select

const STATUS_COLORS: Record<string, string> = {
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'error'
}

const STATUS_NAMES: Record<string, string> = {
  PENDING: '待处理',
  APPROVED: '已批准',
  REJECTED: '已拒绝'
}

const BillAllocationManagement = () => {
  const [billMonth, setBillMonth] = useState<string>(new Date().toISOString().slice(0, 7))
  const [tabKey, setTabKey] = useState<string>('all')
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

  const getQueryFn = () => {
    const params = { billMonth, page: 0, size: 100 }
    switch (tabKey) {
      case 'anomalies': return () => billAllocationApi.getAnomalies(params)
      case 'pendingOrg': return () => billAllocationApi.getPendingOrgConfirm(params)
      case 'pendingAmount': return () => billAllocationApi.getPendingAmountConfirm(params)
      case 'pendingFinance': return () => billAllocationApi.getPendingFinanceConfirm(params)
      case 'pendingSubmit': return () => billAllocationApi.getPendingSubmit(params)
      default: return () => billAllocationApi.getAllocations(params)
    }
  }

  const { data: allocationData, isLoading, refetch } = useQuery({
    queryKey: ['bill-allocations', billMonth, tabKey],
    queryFn: getQueryFn()
  })

  const confirmOrgMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: string }) =>
      billAllocationApi.confirmOrg(id, status),
    onSuccess: () => {
      message.success('组织确认已更新')
      queryClient.invalidateQueries({ queryKey: ['bill-allocations'] })
    },
    onError: () => message.error('更新确认失败')
  })

  const confirmAmountMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: string }) =>
      billAllocationApi.confirmAmount(id, status),
    onSuccess: () => {
      message.success('金额确认已更新')
      queryClient.invalidateQueries({ queryKey: ['bill-allocations'] })
    },
    onError: () => message.error('更新确认失败')
  })

  const confirmAnomalyMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: string }) =>
      billAllocationApi.confirmAnomaly(id, status),
    onSuccess: () => {
      message.success('异常确认已更新')
      queryClient.invalidateQueries({ queryKey: ['bill-allocations'] })
    },
    onError: () => message.error('异常确认失败')
  })

  const submitMutation = useMutation({
    mutationFn: (id: number) => billAllocationApi.submit(id),
    onSuccess: () => {
      message.success('提交成功')
      queryClient.invalidateQueries({ queryKey: ['bill-allocations'] })
    },
    onError: () => message.error('提交失败')
  })

  const rejectMutation = useMutation({
    mutationFn: ({ id, reason }: { id: number; reason?: string }) =>
      billAllocationApi.reject(id, reason),
    onSuccess: () => {
      message.success('拒绝成功')
      queryClient.invalidateQueries({ queryKey: ['bill-allocations'] })
    },
    onError: () => message.error('拒绝失败')
  })

  const handleConfirmOrg = (record: BillAllocation) => {
    Modal.confirm({
      title: '确认组织',
      content: `确认组织 ${record.orgName}?`,
      onOk: () => confirmOrgMutation.mutate({ id: record.id, status: 'APPROVED' })
    })
  }

  const handleConfirmAmount = (record: BillAllocation) => {
    Modal.confirm({
      title: '确认金额',
      content: `确认金额 ¥${record.allocateAmount}?`,
      onOk: () => confirmAmountMutation.mutate({ id: record.id, status: 'APPROVED' })
    })
  }

  const handleConfirmAnomaly = (record: BillAllocation, action: 'APPROVED' | 'REJECTED') => {
    const text = action === 'APPROVED' ? '确认' : '驳回'
    Modal.confirm({
      title: `${text}异常`,
      content: `${text}组织 ${record.orgName} 的异常分摊？`,
      onOk: () => confirmAnomalyMutation.mutate({ id: record.id, status: action })
    })
  }

  const handleSubmit = (record: BillAllocation) => {
    Modal.confirm({
      title: '提交',
      content: '提交此分摊进行最终审批？',
      onOk: () => submitMutation.mutate(record.id)
    })
  }

  const handleReject = (record: BillAllocation) => {
    Modal.confirm({
      title: '拒绝',
      content: '拒绝此分摊并重置？',
      onOk: () => rejectMutation.mutate({ id: record.id })
    })
  }

  const columns = [
    { title: '组织', dataIndex: 'orgName', key: 'orgName', width: 150 },
    {
      title: '总金额',
      dataIndex: 'totalAmount',
      key: 'totalAmount',
      width: 120,
      render: (val: number) => `¥${val?.toFixed(2) || '0.00'}`
    },
    {
      title: '分摊金额',
      dataIndex: 'allocateAmount',
      key: 'allocateAmount',
      width: 120,
      render: (val: number) => `¥${val?.toFixed(2) || '0.00'}`
    },
    {
      title: '差异金额',
      dataIndex: 'diffAmount',
      key: 'diffAmount',
      width: 120,
      render: (val: number) => {
        const diff = val || 0
        const color = diff === 0 ? 'green' : diff > 0 ? 'red' : 'orange'
        return <span style={{ color }}>¥{diff.toFixed(2)}</span>
      }
    },
    {
      title: '异常',
      dataIndex: 'anomalyFlag',
      key: 'anomalyFlag',
      width: 80,
      render: (flag: boolean) => flag ? <Tag color="error">异常</Tag> : <Tag color="success">正常</Tag>
    },
    {
      title: '异常原因',
      dataIndex: 'anomalyReason',
      key: 'anomalyReason',
      width: 150,
      render: (v: string) => v || '-'
    },
    {
      title: '组织确认',
      dataIndex: 'adminConfirmOrg',
      key: 'adminConfirmOrg',
      width: 100,
      render: (status: string) => status ? <Tag color={STATUS_COLORS[status]}>{STATUS_NAMES[status]}</Tag> : '-'
    },
    {
      title: '金额确认',
      dataIndex: 'adminConfirmAmount',
      key: 'adminConfirmAmount',
      width: 100,
      render: (status: string) => status ? <Tag color={STATUS_COLORS[status]}>{STATUS_NAMES[status]}</Tag> : '-'
    },
    {
      title: '财务确认',
      dataIndex: 'financeConfirmAnomaly',
      key: 'financeConfirmAnomaly',
      width: 100,
      render: (status: string) => status ? <Tag color={STATUS_COLORS[status]}>{STATUS_NAMES[status]}</Tag> : '-'
    },
    {
      title: '操作',
      key: 'actions',
      width: 320,
      render: (_: unknown, record: BillAllocation) => (
        <Space size="small" wrap>
          {record.adminConfirmOrg === 'PENDING' && (
            <>
              <Button size="small" type="primary" onClick={() => handleConfirmOrg(record)}>确认组织</Button>
              <Button size="small" danger onClick={() => handleReject(record)}>拒绝</Button>
            </>
          )}
          {record.adminConfirmAmount === 'PENDING' && (
            <>
              <Button size="small" type="primary" onClick={() => handleConfirmAmount(record)}>确认金额</Button>
              <Button size="small" danger onClick={() => handleReject(record)}>拒绝</Button>
            </>
          )}
          {record.anomalyFlag && !record.financeConfirmAnomaly && (
            <>
              <Button size="small" style={{ background: '#52c41a', color: '#fff', borderColor: '#52c41a' }} onClick={() => handleConfirmAnomaly(record, 'APPROVED')}>确认异常</Button>
              <Button size="small" danger onClick={() => handleConfirmAnomaly(record, 'REJECTED')}>驳回异常</Button>
            </>
          )}
          {record.financeConfirmSubmit === 'PENDING' && (
            <Button size="small" type="primary" onClick={() => handleSubmit(record)}>提交</Button>
          )}
        </Space>
      )
    }
  ]

  const allocations: BillAllocation[] = allocationData?.data?.data?.content || []
  const totalAmount = allocations.reduce((sum: number, a: BillAllocation) => sum + (a.totalAmount || 0), 0)
  const anomalyCount = allocations.filter((a: BillAllocation) => a.anomalyFlag).length
  const pendingCount = allocations.filter((a: BillAllocation) =>
    a.adminConfirmOrg === 'PENDING' || a.adminConfirmAmount === 'PENDING' || a.financeConfirmSubmit === 'PENDING'
  ).length

  return (
    <div>
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <Card><Statistic title="账单月份" value={billMonth} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="总金额" value={`¥${totalAmount.toFixed(2)}`} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="异常数" value={anomalyCount} valueStyle={{ color: anomalyCount > 0 ? '#cf1322' : '#3f8600' }} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="待处理" value={pendingCount} valueStyle={{ color: pendingCount > 0 ? '#faad14' : '#3f8600' }} /></Card>
        </Col>
      </Row>

      <Card>
        <Space style={{ marginBottom: 16 }}>
          <Select value={billMonth} onChange={setBillMonth} style={{ width: 150 }}>
            {months.map(m => <Option key={m} value={m}>{m}</Option>)}
          </Select>
          <Select value={tabKey} onChange={setTabKey} style={{ width: 150 }}>
            <Option value="all">全部</Option>
            <Option value="anomalies">异常记录</Option>
            <Option value="pendingOrg">待组织确认</Option>
            <Option value="pendingAmount">待金额确认</Option>
            <Option value="pendingFinance">待财务确认</Option>
            <Option value="pendingSubmit">待提交</Option>
          </Select>
          <Button onClick={() => refetch()}>刷新</Button>
        </Space>

        <Table
          columns={columns}
          dataSource={allocations}
          loading={isLoading}
          rowKey="id"
          scroll={{ x: 1500 }}
          pagination={{ pageSize: 20, total: allocationData?.data?.data?.totalElements }}
        />
      </Card>
    </div>
  )
}

export default BillAllocationManagement
