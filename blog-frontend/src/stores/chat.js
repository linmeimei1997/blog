import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getChatSessions, getChatMessages, deleteChatSession, chatStream } from '@/api/ai'

export const useChatStore = defineStore('chat', () => {
  // State
  const sessions = ref([])
  const currentSessionId = ref('')
  const messages = ref([])
  const isLoading = ref(false)

  // Getters
  const currentSession = computed(() => {
    return sessions.value.find(s => s.sessionId === currentSessionId.value)
  })

  // Actions
  const fetchSessions = async () => {
    const data = await getChatSessions()
    sessions.value = data
    return data
  }

  const fetchMessages = async (sessionId) => {
    currentSessionId.value = sessionId
    const data = await getChatMessages(sessionId)
    messages.value = data
    return data
  }

  const createNewSession = () => {
    currentSessionId.value = ''
    messages.value = []
  }

  const removeSession = async (sessionId) => {
    await deleteChatSession(sessionId)
    sessions.value = sessions.value.filter(s => s.sessionId !== sessionId)
    if (currentSessionId.value === sessionId) {
      createNewSession()
    }
  }

  const sendMessage = async (content, onChunk) => {
    isLoading.value = true
    
    // 添加用户消息
    messages.value.push({
      role: 'user',
      content,
      createTime: new Date().toISOString()
    })
    
    // 预先添加助手消息占位（用于流式更新）
    const assistantMsgIndex = messages.value.length
    const assistantMsg = {
      role: 'assistant',
      content: '',
      createTime: new Date().toISOString()
    }
    messages.value.push(assistantMsg)
    
    let assistantContent = ''
    let hasError = false
    
    return new Promise((resolve, reject) => {
      chatStream(
        content,
        currentSessionId.value || undefined,
        (chunk) => {
          assistantContent += chunk
          // 使用索引更新，确保响应式
          messages.value[assistantMsgIndex].content = assistantContent
          onChunk?.(chunk, assistantContent)
        },
        (error) => {
          hasError = true
          isLoading.value = false
          // 更新错误信息到助手消息
          messages.value[assistantMsgIndex].content = '发送失败：' + error.message
          reject(error)
        },
        async () => {
          if (!hasError) {
            isLoading.value = false
            await fetchSessions()
            resolve(assistantContent)
          }
        }
      )
    })
  }

  return {
    sessions,
    currentSessionId,
    messages,
    isLoading,
    currentSession,
    fetchSessions,
    fetchMessages,
    createNewSession,
    removeSession,
    sendMessage
  }
})
