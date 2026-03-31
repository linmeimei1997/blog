import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getChatSessions, getChatMessages, deleteChatSession, chatStream, renameSession } from '@/api/ai'

export const useChatStore = defineStore('chat', () => {
  // State
  const sessions = ref([])
  const currentSessionId = ref('')
  const messages = ref([])
  const isLoading = ref(false)
  const error = ref(null)
  const streamingContent = ref('')
  const isStreaming = ref(false)
  
  // 会话搜索
  const searchQuery = ref('')

  // Getters
  const currentSession = computed(() => {
    return sessions.value.find(s => s.sessionId === currentSessionId.value)
  })
  
  // 过滤后的会话列表
  const filteredSessions = computed(() => {
    if (!searchQuery.value) return sessions.value
    const query = searchQuery.value.toLowerCase()
    return sessions.value.filter(s => 
      s.title?.toLowerCase().includes(query)
    )
  })
  
  // 会话统计
  const sessionStats = computed(() => {
    return {
      total: sessions.value.length,
      today: sessions.value.filter(s => {
        const today = new Date().toDateString()
        const sessionDate = new Date(s.lastMessageTime).toDateString()
        return today === sessionDate
      }).length
    }
  })

  // Actions
  const fetchSessions = async () => {
    try {
      error.value = null
      const data = await getChatSessions()
      sessions.value = data || []
      return data
    } catch (e) {
      error.value = e.message
      throw e
    }
  }

  const fetchMessages = async (sessionId) => {
    try {
      error.value = null
      currentSessionId.value = sessionId
      const data = await getChatMessages(sessionId)
      messages.value = data || []
      return data
    } catch (e) {
      error.value = e.message
      throw e
    }
  }

  const createNewSession = () => {
    currentSessionId.value = ''
    messages.value = []
    streamingContent.value = ''
    error.value = null
  }
  
  const updateSessionTitle = async (sessionId, title) => {
    try {
      await renameSession(sessionId, title)
      const session = sessions.value.find(s => s.sessionId === sessionId)
      if (session) {
        session.title = title
      }
    } catch (e) {
      console.error('重命名会话失败:', e)
    }
  }

  const removeSession = async (sessionId) => {
    try {
      await deleteChatSession(sessionId)
      sessions.value = sessions.value.filter(s => s.sessionId !== sessionId)
      if (currentSessionId.value === sessionId) {
        createNewSession()
      }
    } catch (e) {
      error.value = e.message
      throw e
    }
  }

  const sendMessage = async (content, options = {}) => {
    const { onChunk, onComplete, onError } = options
    isLoading.value = true
    isStreaming.value = true
    error.value = null
    streamingContent.value = ''
    
    // 添加用户消息
    const userMsg = {
      role: 'user',
      content,
      createTime: new Date().toISOString()
    }
    messages.value.push(userMsg)
    
    // 预先添加助手消息占位
    const assistantMsg = {
      role: 'assistant',
      content: '',
      createTime: new Date().toISOString(),
      isStreaming: true
    }
    messages.value.push(assistantMsg)
    const assistantMsgIndex = messages.value.length - 1
    
    let assistantContent = ''
    let hasError = false
    let abortController = null
    
    const abort = () => {
      if (abortController) {
        abortController.abort()
      }
    }
    
    return new Promise((resolve, reject) => {
      const startTime = Date.now()
      
      chatStream(
        content,
        currentSessionId.value || undefined,
        (chunk) => {
          assistantContent += chunk
          streamingContent.value = assistantContent
          messages.value[assistantMsgIndex].content = assistantContent
          onChunk?.(chunk, assistantContent)
        },
        (err) => {
          hasError = true
          isLoading.value = false
          isStreaming.value = false
          error.value = err.message
          messages.value[assistantMsgIndex].content = assistantContent || '发送失败：' + err.message
          messages.value[assistantMsgIndex].isError = true
          messages.value[assistantMsgIndex].isStreaming = false
          onError?.(err)
          reject(err)
        },
        async () => {
          if (!hasError) {
            const duration = Date.now() - startTime
            isLoading.value = false
            isStreaming.value = false
            messages.value[assistantMsgIndex].isStreaming = false
            messages.value[assistantMsgIndex].duration = duration
            
            // 刷新会话列表
            await fetchSessions()
            onComplete?.(assistantContent, duration)
            resolve(assistantContent)
          }
        }
      )
    })
  }
  
  // 重新生成消息
  const regenerateMessage = async (messageIndex, options = {}) => {
    const userMsg = messages.value[messageIndex - 1]
    if (!userMsg || userMsg.role !== 'user') {
      throw new Error('无法重新生成：上一条不是用户消息')
    }
    
    // 删除当前及之后的助手消息
    messages.value = messages.value.slice(0, messageIndex)
    
    // 重新发送
    return sendMessage(userMsg.content, options)
  }
  
  // 清空错误
  const clearError = () => {
    error.value = null
  }

  return {
    sessions,
    currentSessionId,
    messages,
    isLoading,
    isStreaming,
    error,
    streamingContent,
    searchQuery,
    currentSession,
    filteredSessions,
    sessionStats,
    fetchSessions,
    fetchMessages,
    createNewSession,
    updateSessionTitle,
    removeSession,
    sendMessage,
    regenerateMessage,
    clearError
  }
})
