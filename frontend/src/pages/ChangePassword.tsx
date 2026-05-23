import { useState } from 'react'
import { Form, Input, Button, Card, message } from 'antd'
import { useNavigate } from 'react-router-dom'
import { authApi } from '@/api/auth'
import { useAuthStore } from '@/stores/authStore'

const ChangePassword = () => {
  const [loading, setLoading] = useState(false)
  const [form] = Form.useForm()
  const navigate = useNavigate()
  const { clearAuth } = useAuthStore()

  const handleSubmit = async (values: { oldPassword: string; newPassword: string }) => {
    setLoading(true)
    try {
      await authApi.changePassword({
        oldPassword: values.oldPassword,
        newPassword: values.newPassword,
      })
      message.success('密码修改成功，请重新登录')
      clearAuth()
      navigate('/login')
    } catch (error: unknown) {
      const msg = error instanceof Error ? error.message : '密码修改失败'
      message.error(msg)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div
      style={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        minHeight: '100vh',
        background: 'var(--bg-primary)',
      }}
    >
      <Card title="首次登录 - 修改密码" style={{ width: 420 }}>
        <Form form={form} layout="vertical" onFinish={handleSubmit} style={{ marginTop: 16 }}>
          <Form.Item
            name="oldPassword"
            label="当前密码"
            rules={[{ required: true, message: '请输入当前密码' }]}
          >
            <Input.Password placeholder="请输入当前密码" />
          </Form.Item>
          <Form.Item
            name="newPassword"
            label="新密码"
            rules={[
              { required: true, message: '请输入新密码' },
              { min: 8, message: '密码长度至少8位' },
              {
                pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*]).{8,}$/,
                message: '需包含大小写字母、数字和特殊字符',
              },
            ]}
          >
            <Input.Password placeholder="请输入新密码（至少8位，含大小写+数字+特殊字符）" />
          </Form.Item>
          <Form.Item
            name="confirmPassword"
            label="确认新密码"
            dependencies={['newPassword']}
            rules={[
              { required: true, message: '请再次输入新密码' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('newPassword') === value) return Promise.resolve()
                  return Promise.reject(new Error('两次输入的密码不一致'))
                },
              }),
            ]}
          >
            <Input.Password placeholder="请再次输入新密码" />
          </Form.Item>
          <Form.Item style={{ marginBottom: 0 }}>
            <Button type="primary" htmlType="submit" loading={loading} block>
              确认修改
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  )
}

export default ChangePassword
