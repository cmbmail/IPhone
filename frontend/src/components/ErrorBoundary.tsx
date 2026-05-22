import { Component, ReactNode } from 'react'
import { Result, Button } from 'antd'

interface Props {
  children: ReactNode
}

interface State {
  hasError: boolean
  error: Error | null
}

/**
 * Global error boundary to catch rendering errors
 * and show a friendly fallback UI instead of white screen.
 */
export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = { hasError: false, error: null }
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error }
  }

  handleReset = () => {
    this.setState({ hasError: false, error: null })
  }

  render() {
    if (this.state.hasError) {
      return (
        <Result
          status="error"
          title="页面渲染出错"
          subTitle={this.state.error?.message || '发生了未知错误'}
          extra={[
            <Button key="retry" type="primary" onClick={this.handleReset}>
              重试
            </Button>,
            <Button key="home" onClick={() => window.location.href = '/'}>
              返回首页
            </Button>,
          ]}
        />
      )
    }
    return this.props.children
  }
}
