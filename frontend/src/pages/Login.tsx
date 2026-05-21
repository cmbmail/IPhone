import { useState } from 'react'
import { Form, Input, Button, Card, message } from 'antd'
import { useNavigate } from 'react-router-dom'
import { authApi } from '@/api/auth'
import { useAuthStore } from '@/stores/authStore'

const Login = () => {
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const { setAuth } = useAuthStore()

  const onFinish = async (values: { username: string; password: string }) => {
    setLoading(true)
    try {
      const response = await authApi.login(values)
      const { token, user, expiresIn } = response.data.data

      localStorage.setItem('token', token)
      localStorage.setItem('expiresIn', String(expiresIn))

      setAuth(token, user)

      if (user.needsPasswordChange) {
        message.warning('首次登录，请修改密码')
        navigate('/change-password')
      } else {
        message.success('登录成功')
        navigate('/')
      }
    } catch (error: any) {
      const errorMsg = error.response?.data?.message || '登录失败'
      message.error(errorMsg)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'center',
      minHeight: '100vh',
      background: 'var(--bg-page)'
    }}>
      <Card 
        title="PhoneBiz" 
        style={{ 
          width: 420,
          background: 'var(--bg-card)',
          border: '1px solid var(--border-light)',
          borderRadius: '16px',
          boxShadow: 'var(--shadow-lg)'
        }}
        styles={{
          header: { 
            color: 'var(--text-primary)', 
            fontWeight: 600, 
            fontSize: 24,
            fontFamily: 'Playfair Display, serif',
            borderBottom: '1px solid var(--border-light)'
          }
        }}
      >
        <Form
          name="login"
          onFinish={onFinish}
          autoComplete="off"
          layout="vertical"
        >
          <Form.Item
            label="用户名"
            name="username"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input placeholder="输入用户名" />
          </Form.Item>

          <Form.Item
            label="密码"
            name="password"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password placeholder="输入密码" />
          </Form.Item>

          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading} block>
              登录
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  )
}

export default Login
