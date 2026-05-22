import { useState, useRef, useEffect } from 'react'
import { Input, List, Typography, Space, Tag } from 'antd'
import { SearchOutlined, PhoneOutlined, AppstoreOutlined, CarryOutOutlined, TeamOutlined, DesktopOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { searchApi } from '@/api/search'

const { Text } = Typography

const TYPE_CONFIG: Record<string, { icon: React.ReactNode; color: string; label: string }> = {
  'phone': { icon: <PhoneOutlined />, color: 'blue', label: '号码' },
  'extension': { icon: <AppstoreOutlined />, color: 'cyan', label: '分机' },
  'work-order': { icon: <CarryOutOutlined />, color: 'orange', label: '工单' },
  'employee': { icon: <TeamOutlined />, color: 'green', label: '员工' },
  'device': { icon: <DesktopOutlined />, color: 'purple', label: '设备' },
}

interface SearchResult {
  type: string
  id: number
  label: string
  subLabel: string
  route: string
}

const GlobalSearch = () => {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<SearchResult[]>([])
  const [open, setOpen] = useState(false)
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const timerRef = useRef<ReturnType<typeof setTimeout>>()
  const containerRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  const handleSearch = (value: string) => {
    setQuery(value)
    if (timerRef.current) clearTimeout(timerRef.current)
    if (!value.trim()) {
      setResults([])
      setOpen(false)
      return
    }
    timerRef.current = setTimeout(async () => {
      setLoading(true)
      try {
        const res = await searchApi.globalSearch(value.trim())
        const data = res.data?.data || []
        setResults(data)
        setOpen(data.length > 0)
      } catch {
        setResults([])
      } finally {
        setLoading(false)
      }
    }, 300)
  }

  const handleSelect = (item: SearchResult) => {
    setOpen(false)
    setQuery('')
    setResults([])
    navigate(item.route)
  }

  return (
    <div ref={containerRef} style={{ position: 'relative' }}>
      <Input
        prefix={<SearchOutlined style={{ color: 'var(--text-muted)' }} />}
        placeholder="搜索号码、工单、员工..."
        value={query}
        onChange={(e) => handleSearch(e.target.value)}
        onFocus={() => { if (results.length > 0) setOpen(true) }}
        style={{ width: 260, background: 'var(--bg-secondary)', border: '1px solid var(--border-light)' }}
        allowClear
        loading={loading}
      />
      {open && (
        <div style={{
          position: 'absolute', top: '100%', left: 0, right: 0, zIndex: 1000,
          background: 'var(--bg-primary)', borderRadius: 8, boxShadow: '0 6px 16px rgba(0,0,0,0.12)',
          maxHeight: 400, overflow: 'auto', marginTop: 4,
        }}>
          <List
            size="small"
            dataSource={results}
            renderItem={(item) => {
              const config = TYPE_CONFIG[item.type] || TYPE_CONFIG['phone']
              return (
                <List.Item
                  style={{ padding: '8px 12px', cursor: 'pointer' }}
                  onMouseEnter={(e) => (e.currentTarget.style.background = 'var(--bg-secondary)')}
                  onMouseLeave={(e) => (e.currentTarget.style.background = 'transparent')}
                  onClick={() => handleSelect(item)}
                >
                  <Space>
                    <Tag color={config.color} style={{ margin: 0 }}>{config.label}</Tag>
                    <Text strong style={{ color: 'var(--text-primary)' }}>{item.label}</Text>
                    <Text type="secondary" style={{ fontSize: 12 }}>{item.subLabel}</Text>
                  </Space>
                </List.Item>
              )
            }}
          />
        </div>
      )}
    </div>
  )
}

export default GlobalSearch
