import { useState } from 'react'
import { Badge, Button, List, Popover, Spin, Empty } from 'antd'
import { BellOutlined } from '@ant-design/icons'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { notificationApi, type AppNotification } from '@/api/notification'
import type { PagedData } from '@/api/request'

interface NotificationItem {
  id: number
  title: string
  message: string
  content: string
  status: number
  createdAt: string
}

interface UnreadCountData {
  unreadCount: number
}

const NotificationPopover = () => {
  const [open, setOpen] = useState(false)
  const qc = useQueryClient()

  const { data: unreadData } = useQuery({
    queryKey: ['notifications-unread'],
    queryFn: async () => {
      const r = await notificationApi.getUnreadCount()
      return r
    },
    refetchInterval: 30000,
  })

  const { data: listData, isLoading } = useQuery({
    queryKey: ['notifications-list'],
    queryFn: async () => {
      const r = await notificationApi.getList(0, 20)
      return r.content || []
    },
    enabled: open,
  })

  const readMut = useMutation({
    mutationFn: (id: number) => notificationApi.markAsRead(id),
    onSuccess: () =>
      qc.invalidateQueries({ queryKey: ['notifications-list', 'notifications-unread'] }),
  })

  const readAllMut = useMutation({
    mutationFn: () => notificationApi.markAllAsRead(),
    onSuccess: () =>
      qc.invalidateQueries({ queryKey: ['notifications-list', 'notifications-unread'] }),
  })

  const unreadCount =
    typeof unreadData === 'number'
      ? unreadData
      : (unreadData as UnreadCountData | undefined)?.unreadCount || 0
  const notifications: NotificationItem[] = ((listData as PagedData<AppNotification> | undefined)
    ?.content || []) as unknown as NotificationItem[]

  const content = (
    <div style={{ width: 360, maxHeight: 400, overflow: 'auto' }}>
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: 8,
          padding: '0 4px',
        }}
      >
        <span style={{ fontWeight: 600 }}>消息通知</span>
        {unreadCount > 0 && (
          <Button type="link" size="small" onClick={() => readAllMut.mutate()}>
            全部已读
          </Button>
        )}
      </div>
      {isLoading ? (
        <Spin style={{ display: 'block', margin: '20px auto' }} />
      ) : notifications.length === 0 ? (
        <Empty description="暂无消息" image={Empty.PRESENTED_IMAGE_SIMPLE} />
      ) : (
        <List
          size="small"
          dataSource={notifications}
          renderItem={(item: NotificationItem) => (
            <List.Item
              style={{
                background: item.status === 0 ? '#f6f8fa' : 'transparent',
                padding: '8px 12px',
                cursor: item.status === 0 ? 'pointer' : 'default',
                borderRadius: 4,
              }}
              onClick={() => item.status === 0 && readMut.mutate(item.id)}
            >
              <List.Item.Meta
                title={
                  <span style={{ fontSize: 13, fontWeight: item.status === 0 ? 600 : 400 }}>
                    {item.title || '系统通知'}
                  </span>
                }
                description={
                  <span style={{ fontSize: 12 }}>
                    {item.message || item.content}
                    {item.createdAt && (
                      <span style={{ color: '#999', marginLeft: 8 }}>
                        {new Date(item.createdAt).toLocaleString()}
                      </span>
                    )}
                  </span>
                }
              />
            </List.Item>
          )}
        />
      )}
    </div>
  )

  return (
    <Popover
      content={content}
      trigger="click"
      open={open}
      onOpenChange={setOpen}
      placement="bottomRight"
    >
      <Badge count={unreadCount} size="small" offset={[-2, 2]}>
        <Button
          type="text"
          icon={<BellOutlined style={{ fontSize: 18 }} />}
          style={{ color: 'var(--text-secondary)', width: 42, height: 42 }}
        />
      </Badge>
    </Popover>
  )
}

export default NotificationPopover
