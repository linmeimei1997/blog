import request from '@/utils/request'

export const getChatSessions = () => {
  return request.get('/ai/sessions')
}

export const getChatMessages = (sessionId) => {
  return request.get(`/ai/sessions/${sessionId}/messages`)
}

export const deleteChatSession = (sessionId) => {
  return request.delete(`/ai/sessions/${sessionId}`)
}

// SSE 流式对话
export const chatStream = (message, sessionId, onMessage, onError, onComplete) => {
  const params = new URLSearchParams()
  params.append('message', message)
  if (sessionId) {
    params.append('sessionId', sessionId)
  }
  
  // 添加 token 到 URL（EventSource 不支持自定义 headers）
  const token = localStorage.getItem('token')
  if (token) {
    params.append('token', token)
  }
  
  const url = `/api/ai/chat/stream?${params.toString()}`
  console.log('Connecting to SSE:', url)
  
  const eventSource = new EventSource(url)
  let isCompleted = false
  
  eventSource.onopen = () => {
    console.log('SSE connection opened')
  }
  
  eventSource.onmessage = (event) => {
    console.log('SSE message received:', event.data)
    if (event.data) {
      onMessage?.(event.data)
    }
  }
  
  eventSource.onerror = (error) => {
    console.error('SSE error:', error)
    if (!isCompleted) {
      onError?.(new Error('连接失败，请检查网络或后端服务'))
    }
    eventSource.close()
  }
  
  eventSource.addEventListener('complete', () => {
    console.log('SSE complete')
    isCompleted = true
    onComplete?.()
    eventSource.close()
  })
  
  // 超时处理
  setTimeout(() => {
    if (!isCompleted) {
      console.warn('SSE timeout')
      onError?.(new Error('请求超时'))
      eventSource.close()
    }
  }, 300000) // 5分钟超时
  
  return eventSource
}
