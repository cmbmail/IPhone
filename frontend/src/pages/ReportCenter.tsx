import { useState } from 'react'
import { Card, Select, Button, Tabs, Row, Col, Statistic, Table, message } from 'antd'
import { useQuery } from '@tanstack/react-query'
import { reportApi } from '@/api/report'

const { Option } = Select

const ReportCenter = () => {
  const [billMonth, setBillMonth] = useState<string>(new Date().toISOString().slice(0, 7))
  const [startDate, setStartDate] = useState<string>('')
  const [endDate, setEndDate] = useState<string>('')

  const months = Array.from({ length: 12 }, (_, i) => {
    const d = new Date()
    d.setMonth(d.getMonth() - i)
    return d.toISOString().slice(0, 7)
  })

  const { data: phoneAssetData, isLoading: phoneAssetLoading } = useQuery({
    queryKey: ['phone-asset-report', billMonth],
    queryFn: () => reportApi.getPhoneAssetReport(billMonth),
  })

  const { data: allocationData, isLoading: allocationLoading } = useQuery({
    queryKey: ['bill-allocation-report', billMonth],
    queryFn: () => reportApi.getBillAllocationReport(billMonth),
  })

  const { data: anomalyData, isLoading: anomalyLoading } = useQuery({
    queryKey: ['anomaly-report', billMonth],
    queryFn: () => reportApi.getAnomalyBillReport(billMonth),
  })

  const {
    data: workOrderData,
    isLoading: workOrderLoading,
    refetch: refetchWorkOrder,
  } = useQuery({
    queryKey: ['work-order-report', startDate, endDate],
    queryFn: () => {
      if (!startDate || !endDate) {
        message.warning('请选择日期范围')
        throw new Error('需要日期范围')
      }
      return reportApi.getWorkOrderReport(startDate, endDate)
    },
    enabled: !!startDate && !!endDate,
  })

  const phoneAssetReport = phoneAssetData?.data?.data || {}
  const allocationReport = allocationData?.data?.data || {}
  const anomalyReport = anomalyData?.data?.data || {}
  const workOrderReport = workOrderData?.data?.data || {}

  const orgColumns = [
    { title: '组织', dataIndex: 'orgName', key: 'orgName' },
    { title: '电话数量', dataIndex: 'phoneCount', key: 'phoneCount' },
    {
      title: '总金额',
      dataIndex: 'totalAmount',
      key: 'totalAmount',
      render: (val: number) => `¥${val?.toFixed(2) || '0.00'}`,
    },
  ]

  const anomalyColumns = [
    { title: '组织', dataIndex: 'orgName', key: 'orgName' },
    {
      title: '差异金额',
      dataIndex: 'diffAmount',
      key: 'diffAmount',
      render: (val: number) => (
        <span style={{ color: val > 0 ? 'red' : 'green' }}>¥{val?.toFixed(2)}</span>
      ),
    },
    {
      title: '差异率',
      dataIndex: 'diffPercentage',
      key: 'diffPercentage',
      render: (val: number) => `${val?.toFixed(2)}%`,
    },
    { title: '状态', dataIndex: 'status', key: 'status' },
  ]

  const workOrderColumns = [
    { title: '类型', dataIndex: 'type', key: 'type' },
    { title: '数量', dataIndex: 'count', key: 'count' },
    { title: '已完成', dataIndex: 'completed', key: 'completed' },
    { title: '待处理', dataIndex: 'pending', key: 'pending' },
  ]

  const tabItems = [
    {
      key: 'phone-asset',
      label: '电话资产报表',
      children: (
        <>
          <Row gutter={16} style={{ marginBottom: 16 }}>
            <Col span={6}>
              <Card>
                <Statistic title="电话总数" value={phoneAssetReport.totalPhones || 0} />
              </Card>
            </Col>
            <Col span={6}>
              <Card>
                <Statistic
                  title="活跃电话"
                  value={phoneAssetReport.activePhones || 0}
                  valueStyle={{ color: '#3f8600' }}
                />
              </Card>
            </Col>
            <Col span={6}>
              <Card>
                <Statistic
                  title="空闲电话"
                  value={phoneAssetReport.idlePhones || 0}
                  valueStyle={{ color: '#1890ff' }}
                />
              </Card>
            </Col>
            <Col span={6}>
              <Card>
                <Statistic
                  title="总资产价值"
                  value={`¥${(phoneAssetReport.totalValue || 0).toFixed(2)}`}
                />
              </Card>
            </Col>
          </Row>
          <Card title="组织电话分布">
            <Table
              columns={orgColumns}
              dataSource={phoneAssetReport.orgDetails || []}
              loading={phoneAssetLoading}
              rowKey="orgName"
              pagination={false}
            />
          </Card>
        </>
      ),
    },
    {
      key: 'allocation',
      label: '账单分摊报表',
      children: (
        <>
          <Row gutter={16} style={{ marginBottom: 16 }}>
            <Col span={6}>
              <Card>
                <Statistic
                  title="总金额"
                  value={`¥${(allocationReport.totalAmount || 0).toFixed(2)}`}
                />
              </Card>
            </Col>
            <Col span={6}>
              <Card>
                <Statistic
                  title="已分摊金额"
                  value={`¥${(allocationReport.allocatedAmount || 0).toFixed(2)}`}
                  valueStyle={{ color: '#3f8600' }}
                />
              </Card>
            </Col>
            <Col span={6}>
              <Card>
                <Statistic
                  title="待处理金额"
                  value={`¥${(allocationReport.pendingAmount || 0).toFixed(2)}`}
                  valueStyle={{ color: '#faad14' }}
                />
              </Card>
            </Col>
            <Col span={6}>
              <Card>
                <Statistic title="已分摊组织" value={allocationReport.allocatedOrgs || 0} />
              </Card>
            </Col>
          </Row>
          <Card title="组织分摊明细">
            <Table
              columns={orgColumns}
              dataSource={allocationReport.orgDetails || []}
              loading={allocationLoading}
              rowKey="orgName"
              pagination={false}
            />
          </Card>
        </>
      ),
    },
    {
      key: 'anomaly',
      label: '异常报表',
      children: (
        <>
          <Row gutter={16} style={{ marginBottom: 16 }}>
            <Col span={6}>
              <Card>
                <Statistic
                  title="异常数"
                  value={anomalyReport.anomalyCount || 0}
                  valueStyle={{ color: '#cf1322' }}
                />
              </Card>
            </Col>
            <Col span={6}>
              <Card>
                <Statistic
                  title="总差异"
                  value={`¥${(anomalyReport.totalDiff || 0).toFixed(2)}`}
                  valueStyle={{ color: '#cf1322' }}
                />
              </Card>
            </Col>
            <Col span={6}>
              <Card>
                <Statistic
                  title="待解决"
                  value={anomalyReport.pendingCount || 0}
                  valueStyle={{ color: '#faad14' }}
                />
              </Card>
            </Col>
            <Col span={6}>
              <Card>
                <Statistic
                  title="已解决"
                  value={anomalyReport.resolvedCount || 0}
                  valueStyle={{ color: '#3f8600' }}
                />
              </Card>
            </Col>
          </Row>
          <Card title="异常明细">
            <Table
              columns={anomalyColumns}
              dataSource={anomalyReport.anomalyDetails || []}
              loading={anomalyLoading}
              rowKey="orgName"
              pagination={false}
            />
          </Card>
        </>
      ),
    },
    {
      key: 'work-order',
      label: '工单报表',
      children: (
        <>
          <Card style={{ marginBottom: 16 }}>
            <Select
              value={startDate}
              onChange={setStartDate}
              style={{ width: 150, marginRight: 16 }}
              placeholder="开始日期"
            >
              {months.map((m) => (
                <Option key={m} value={m}>
                  {m}
                </Option>
              ))}
            </Select>
            <Select
              value={endDate}
              onChange={setEndDate}
              style={{ width: 150, marginRight: 16 }}
              placeholder="结束日期"
            >
              {months.map((m) => (
                <Option key={m} value={m}>
                  {m}
                </Option>
              ))}
            </Select>
            <Button onClick={() => refetchWorkOrder()}>查询</Button>
          </Card>
          <Row gutter={16} style={{ marginBottom: 16 }}>
            <Col span={6}>
              <Card>
                <Statistic title="工单总数" value={workOrderReport.totalOrders || 0} />
              </Card>
            </Col>
            <Col span={6}>
              <Card>
                <Statistic
                  title="已完成"
                  value={workOrderReport.completedOrders || 0}
                  valueStyle={{ color: '#3f8600' }}
                />
              </Card>
            </Col>
            <Col span={6}>
              <Card>
                <Statistic
                  title="待处理"
                  value={workOrderReport.pendingOrders || 0}
                  valueStyle={{ color: '#faad14' }}
                />
              </Card>
            </Col>
            <Col span={6}>
              <Card>
                <Statistic
                  title="完成率"
                  value={`${(((workOrderReport.completedOrders || 0) / (workOrderReport.totalOrders || 1)) * 100).toFixed(1)}%`}
                />
              </Card>
            </Col>
          </Row>
          <Card title="按类型分类工单">
            <Table
              columns={workOrderColumns}
              dataSource={workOrderReport.orderByType || []}
              loading={workOrderLoading}
              rowKey="type"
              pagination={false}
            />
          </Card>
        </>
      ),
    },
  ]

  return (
    <div>
      <Card style={{ marginBottom: 16 }}>
        <Select value={billMonth} onChange={setBillMonth} style={{ width: 150 }}>
          {months.map((m) => (
            <Option key={m} value={m}>
              {m}
            </Option>
          ))}
        </Select>
      </Card>

      <Tabs defaultActiveKey="phone-asset" items={tabItems} />
    </div>
  )
}

export default ReportCenter
