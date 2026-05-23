import { useState } from 'react'
import { Card, Table, Button, Modal, Input, Select, Form, message, Space, Upload } from 'antd'
import {
  UploadOutlined,
  PlusOutlined,
  SearchOutlined,
  FileExcelOutlined,
  DownloadOutlined,
} from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { costCenterApi } from '@/api/costCenter'
import type { CostCenter } from '@/types/costCenter'
import { orgApi } from '@/api/org'
import type { OrgStructure } from '@/types/org'

const CostCenterManagement = () => {
  const [isImportModalOpen, setIsImportModalOpen] = useState(false)
  const [isAddModalOpen, setIsAddModalOpen] = useState(false)
  const [searchText, setSearchText] = useState('')
  const [addForm] = Form.useForm()
  const queryClient = useQueryClient()

  const { data: allOrgs, isLoading } = useQuery({
    queryKey: ['all-orgs'],
    queryFn: async () => {
      const res = await orgApi.getAll()
      return res.data?.data || []
    },
  })

  const departments = (allOrgs || []).filter((o: OrgStructure) => o.type === 3)

  const filteredData = searchText
    ? departments.filter(
        (d: CostCenter) =>
          (d.name || '').includes(searchText) ||
          (d.branchName || '').includes(searchText) ||
          (d.costCenterCode || '').includes(searchText) ||
          (d.orgCode || '').includes(searchText)
      )
    : departments

  const importMutation = useMutation({
    mutationFn: (file: File) => costCenterApi.importCostCenter(file),
    onSuccess: () => {
      message.success('成本中心导入成功')
      setIsImportModalOpen(false)
      queryClient.invalidateQueries({ queryKey: ['all-orgs'] })
    },
    onError: (e: any) => {
      const msg = e?.response?.data?.message || e?.response?.data?.errors || '导入失败'
      message.error(typeof msg === 'string' ? msg : '导入失败')
    },
  })

  const createMutation = useMutation({
    mutationFn: (data: {
      name: string
      parent_id?: number
      branch_name?: string
      org_code?: string
      cost_center?: string
    }) => {
      return orgApi.create({ ...data, type: 'dept' })
    },
    onSuccess: () => {
      message.success('新增成功')
      setIsAddModalOpen(false)
      addForm.resetFields()
      queryClient.invalidateQueries({ queryKey: ['all-orgs'] })
    },
    onError: (e: any) => {
      const msg = e?.response?.data?.message || e?.response?.data?.errors || '新增失败'
      message.error(typeof msg === 'string' ? msg : '新增失败')
    },
  })

  const handleAdd = () => {
    addForm.validateFields().then((values) => {
      createMutation.mutate(values)
    })
  }

  const handleDownloadTemplate = () => {
    const headers = ['部门名称', '分行', '组织机构代码', '成本中心编码']
    const csvContent = '\uFEFF' + headers.join(',') + '\n'
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '成本中心导入模板.csv'
    a.click()
    URL.revokeObjectURL(url)
  }

  const columns = [
    {
      title: '分行',
      dataIndex: 'branchName',
      key: 'branchName',
      width: 160,
      align: 'center' as const,
      render: (v: string) => v || <span style={{ color: '#ccc' }}>-</span>,
    },
    {
      title: '部门名称',
      dataIndex: 'name',
      key: 'name',
      width: 180,
      align: 'center' as const,
      render: (v: string) => <span style={{ fontWeight: 500 }}>{v}</span>,
    },
    {
      title: '机构代码',
      dataIndex: 'orgCode',
      key: 'orgCode',
      width: 200,
      align: 'center' as const,
      render: (v: string) => v || <span style={{ color: '#ccc' }}>-</span>,
    },
    {
      title: '成本中心',
      dataIndex: 'costCenterCode',
      key: 'costCenterCode',
      width: 160,
      align: 'center' as const,
      render: (v: string) => v || <span style={{ color: '#ccc' }}>-</span>,
    },
  ]

  return (
    <div>
      <Card size="small" style={{ marginBottom: 16 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Input
            placeholder="搜索分行/部门/成本中心..."
            prefix={<SearchOutlined />}
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
            style={{ width: 240 }}
            allowClear
          />
          <Space>
            <Button icon={<UploadOutlined />} onClick={() => setIsImportModalOpen(true)}>
              导入
            </Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={() => setIsAddModalOpen(true)}>
              新增
            </Button>
          </Space>
        </div>
      </Card>

      <Card size="small">
        <Table<CostCenter>
          rowKey="id"
          columns={columns}
          dataSource={filteredData}
          loading={isLoading}
          size="middle"
          pagination={{
            pageSize: 50,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条`,
          }}
          scroll={{ x: 700 }}
        />
      </Card>

      <Modal
        title="导入成本中心"
        open={isImportModalOpen}
        onCancel={() => setIsImportModalOpen(false)}
        footer={null}
        width={480}
      >
        <Space direction="vertical" style={{ width: '100%', gap: 12 }}>
          <div
            style={{
              background: '#f6f8fa',
              padding: 14,
              borderRadius: 8,
              fontSize: 13,
              color: '#555',
            }}
          >
            <div style={{ fontWeight: 600, marginBottom: 6, color: '#333' }}>Excel格式说明</div>
            <ul style={{ margin: 0, paddingLeft: 18 }}>
              <li>
                第1列：<strong>部门名称</strong>（必须与系统中的部门名称一致）
              </li>
              <li>
                第2列：<strong>分行</strong>
              </li>
              <li>
                第3列：<strong>组织机构代码</strong>
              </li>
              <li>
                第4列：<strong>成本中心编码</strong>
              </li>
            </ul>
            <div style={{ marginTop: 8 }}>系统根据部门名称自动匹配并更新，已有数据会被覆盖。</div>
          </div>
          <Button icon={<DownloadOutlined />} onClick={handleDownloadTemplate}>
            下载导入模板
          </Button>
          <Upload.Dragger
            accept=".xlsx,.xls"
            beforeUpload={(file) => {
              importMutation.mutate(file)
              return false
            }}
            showUploadList={false}
          >
            <p className="ant-upload-drag-icon">
              <FileExcelOutlined style={{ fontSize: 40, color: '#1677ff' }} />
            </p>
            <p className="ant-upload-text">点击或拖拽Excel文件到此处</p>
          </Upload.Dragger>
        </Space>
      </Modal>

      <Modal
        title="新增成本中心"
        open={isAddModalOpen}
        onCancel={() => {
          setIsAddModalOpen(false)
          addForm.resetFields()
        }}
        onOk={handleAdd}
        okText="保存"
        confirmLoading={createMutation.isPending}
        width={440}
      >
        <Form form={addForm} layout="vertical">
          <Form.Item
            name="name"
            label="部门名称"
            rules={[{ required: true, message: '请输入部门名称' }]}
          >
            <Input placeholder="如：技术部" />
          </Form.Item>
          <Form.Item
            name="parent_id"
            label="上级机构"
            rules={[{ required: true, message: '请选择上级机构' }]}
          >
            <Select
              placeholder="选择上级机构"
              showSearch
              optionFilterProp="children"
              fieldNames={{ label: 'name', value: 'id' }}
              options={(allOrgs || []).filter((o: OrgStructure) => o.type !== 3)}
            />
          </Form.Item>
          <Form.Item name="branch_name" label="分行">
            <Input placeholder="如：武汉分行" />
          </Form.Item>
          <Form.Item name="org_code" label="机构代码">
            <Input placeholder="如：914201007178" />
          </Form.Item>
          <Form.Item name="cost_center" label="成本中心">
            <Input placeholder="如：CC-WH-001" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default CostCenterManagement
