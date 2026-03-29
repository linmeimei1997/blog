<template>
  <div class="ai-assistant" ref="assistantRef">
    <!-- 小埋悬浮球 -->
    <div
      v-if="!isOpen"
      class="umaru-float"
      @click.stop="toggleOpen"
      @mouseenter="handleMouseEnter"
      @mouseleave="handleMouseLeave"
    >
      <UmaruAvatar 
        :mood="currentMood" 
        :is-jumping="isJumping"
        :is-talking="isTalking"
        :message="showMessage ? randomMessage : ''"
      />
      <div v-if="unreadCount > 0" class="unread-badge">{{ unreadCount }}</div>
    </div>
    
    <!-- 对话窗口 -->
    <div v-else class="ai-chat-window" @click.stop>
      <!-- 头部 -->
      <div class="chat-header">
        <div class="chat-title">
          <UmaruAvatar mood="happy" :size="36" />
          <div class="title-text">
            <span class="name">小埋 AI</span>
            <span class="status">在线</span>
          </div>
        </div>
        <div class="chat-actions">
          <el-tooltip content="新对话">
            <el-button link @click="handleNewChat">
              <el-icon size="18"><Plus /></el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip content="历史会话">
            <el-button link @click="showHistory = true">
              <el-icon size="18"><Clock /></el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip content="收起">
            <el-button link @click="toggleOpen">
              <el-icon size="18"><ArrowDown /></el-icon>
            </el-button>
          </el-tooltip>
        </div>
      </div>
      
      <!-- 消息列表 -->
      <div class="chat-messages" ref="messagesRef">
        <div v-if="chatStore.messages.length === 0" class="welcome-message">
          <UmaruAvatar mood="excited" :size="80" class="welcome-avatar" />
          <h3>呀吼！我是小埋 AI 助手</h3>
          <p>我可以帮你：</p>
          <ul>
            <li>📝 搜索和阅读博客文章</li>
            <li>📚 查询知识库文档</li>
            <li>✨ 总结文档内容</li>
            <li>📖 写读书笔记（支持图文混排）</li>
            <li>💡 回答技术问题</li>
          </ul>
        </div>
        
        <template v-else>
          <div
            v-for="(msg, index) in chatStore.messages"
            :key="index"
            v-show="!(msg.role === 'assistant' && !msg.content)"
            :class="['message-item', msg.role]"
          >
            <div class="message-avatar">
              <UmaruAvatar 
                v-if="msg.role === 'assistant'"
                :mood="getMoodForMessage(msg)"
                :size="36" 
              />
              <el-avatar
                v-else
                :size="36"
                :src="userStore.avatar || defaultAvatar"
              />
            </div>
            <div class="message-content">
              <div class="message-text" v-html="formatMessage(msg.content)"></div>
              <div class="message-actions" v-if="msg.role === 'assistant' && msg.content">
                <el-tooltip content="复制">
                  <el-button link size="small" @click="copyMessage(msg.content)">
                    <el-icon><CopyDocument /></el-icon>
                  </el-button>
                </el-tooltip>
                <el-tooltip content="重新生成">
                  <el-button link size="small" @click="regenerate(index)">
                    <el-icon><Refresh /></el-icon>
                  </el-button>
                </el-tooltip>
              </div>
            </div>
          </div>
          
          <!-- 正在输入提示 - 只在加载中且最后一条助手消息为空时显示 -->
          <!-- 注意：空消息框和正在输入提示只会显示其中一个 -->
          <div v-if="chatStore.isLoading && isLastMessageEmpty" class="message-item assistant">
            <div class="message-avatar">
              <UmaruAvatar mood="sleepy" :size="36" />
            </div>
            <div class="message-content">
              <div class="typing-indicator">
                <span></span>
                <span></span>
                <span></span>
              </div>
            </div>
          </div>
        </template>
      </div>
      
      <!-- 输入框 -->
      <div class="chat-input">
        <!-- 已上传图片预览 -->
        <div v-if="uploadedImages.length > 0" class="image-preview-list">
          <div v-for="(img, idx) in uploadedImages" :key="idx" class="image-preview-item">
            <img :src="img.url" alt="预览" />
            <el-button link class="remove-btn" @click="removeImage(idx)">
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
        </div>
        
        <div class="input-row">
          <el-input
            v-model="inputMessage"
            type="textarea"
            :rows="2"
            :placeholder="uploadedImages.length > 0 ? '描述这些图片，或让我帮你写读书笔记...' : '和小埋聊天吧...'"
            resize="none"
            @keydown.enter.prevent="handleSend"
          />
          <div class="input-actions">
            <input
              type="file"
              ref="imageInput"
              style="display: none"
              accept="image/*"
              @change="handleImageSelect"
            />
            <el-button link :disabled="chatStore.isLoading" @click="$refs.imageInput.click()">
              <el-icon><Picture /></el-icon>
            </el-button>
            <el-button
              type="primary"
              :disabled="(!inputMessage.trim() && uploadedImages.length === 0) || chatStore.isLoading"
              @click="handleSend"
            >
              <el-icon><Promotion /></el-icon>
            </el-button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 历史会话抽屉 -->
    <el-drawer
      v-model="showHistory"
      title="历史会话"
      size="300px"
      direction="rtl"
    >
      <div class="session-list">
        <div
          v-for="session in chatStore.sessions"
          :key="session.sessionId"
          :class="['session-item', { active: session.sessionId === chatStore.currentSessionId }]"
          @click="switchSession(session.sessionId)"
        >
          <div class="session-title">{{ session.title }}</div>
          <div class="session-meta">
            <span>{{ session.messageCount }} 条消息</span>
            <span>{{ formatTime(session.lastMessageTime) }}</span>
          </div>
          <el-button
            link
            type="danger"
            class="delete-btn"
            @click.stop="deleteSession(session.sessionId)"
          >
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, watch, onUnmounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useChatStore } from '@/stores/chat'
import UmaruAvatar from './UmaruAvatar.vue'
import dayjs from 'dayjs'
import {
  Plus, Clock, ArrowDown, CopyDocument, Refresh,
  Picture, Promotion, Close, Delete
} from '@element-plus/icons-vue'

const userStore = useUserStore()
const chatStore = useChatStore()

const assistantRef = ref(null)
const isOpen = ref(false)
const showHistory = ref(false)
const inputMessage = ref('')
const messagesRef = ref(null)
const imageInput = ref(null)
const unreadCount = ref(0)
const uploadedImages = ref([])
const justOpened = ref(false)

// 小埋状态
const currentMood = ref('normal')
const isJumping = ref(false)
const isTalking = ref(false)
const showMessage = ref(false)

const defaultAvatar = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'

// 小埋的随机提示语
const umaruMessages = [
  '呀吼~需要帮忙吗？',
  '可乐真好喝~🥤',
  '要一起玩游戏吗？🎮',
  '有什么想问小埋的？',
  '小埋来帮你啦！',
  '想写读书笔记吗？',
  '让小埋帮你找文章~'
]

const randomMessage = ref(umaruMessages[0])

// 计算属性：最后一条助手消息是否为空
const isLastMessageEmpty = computed(() => {
  const lastMsg = chatStore.messages[chatStore.messages.length - 1]
  return lastMsg && lastMsg.role === 'assistant' && !lastMsg.content
})

// 根据消息内容获取小埋表情
const getMoodForMessage = (msg) => {
  const content = msg.content.toLowerCase()
  if (content.includes('成功') || content.includes('完成') || content.includes('好的')) {
    return 'happy'
  } else if (content.includes('错误') || content.includes('失败') || content.includes('抱歉')) {
    return 'sleepy'
  } else if (content.includes('?') || content.includes('？')) {
    return 'normal'
  }
  return 'normal'
}

// 移除已上传图片
const removeImage = (index) => {
  uploadedImages.value.splice(index, 1)
}

// 处理图片选择
const handleImageSelect = async (event) => {
  const file = event.target.files[0]
  if (!file) return
  
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5
  
  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB!')
    return
  }
  
  const formData = new FormData()
  formData.append('file', file)
  
  try {
    const response = await fetch('/api/ai/upload/image', {
      method: 'POST',
      body: formData
    })
    
    const result = await response.json()
    
    if (result.code === 200) {
      uploadedImages.value.push({
        url: result.data,
        name: file.name
      })
      ElMessage.success('图片上传成功')
    } else {
      ElMessage.error(result.message || '上传失败')
    }
  } catch (error) {
    ElMessage.error('图片上传失败: ' + error.message)
  }
  
  event.target.value = ''
}

// 鼠标悬停效果
const handleMouseEnter = () => {
  isJumping.value = true
  showMessage.value = true
  randomMessage.value = umaruMessages[Math.floor(Math.random() * umaruMessages.length)]
}

const handleMouseLeave = () => {
  isJumping.value = false
  showMessage.value = false
}

const toggleOpen = () => {
  // 检查是否登录
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再使用AI助手')
    return
  }
  
  isOpen.value = !isOpen.value
  if (isOpen.value) {
    unreadCount.value = 0
    currentMood.value = 'excited'
    scrollToBottom()
  } else {
    currentMood.value = 'normal'
  }
}

const formatMessage = (content) => {
  if (!content) return ''
  
  let formatted = content.replace(/!\[([^\]]*)\]\(([^\)]+)\)/g, '<img src="$2" alt="$1" style="max-width:100%;border-radius:8px;margin:8px 0;" />')
  formatted = formatted.replace(/\[([^\]]+)\]\(([^\)]+)\)/g, '<a href="$2" target="_blank" style="color:#409EFF;text-decoration:none;">$1</a>')
  formatted = formatted.replace(/\*\*([^\*]+)\*\*/g, '<strong>$1</strong>')
  formatted = formatted.replace(/\*([^\*]+)\*/g, '<em>$1</em>')
  formatted = formatted.replace(/`([^`]+)`/g, '<code style="background:#f5f7fa;padding:2px 4px;border-radius:4px;font-family:monospace;">$1</code>')
  formatted = formatted.replace(/\n/g, '<br>')
  
  return formatted
}

const formatTime = (time) => {
  if (!time) return ''
  return dayjs(time).format('MM-DD HH:mm')
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

const handleSend = async () => {
  let message = inputMessage.value.trim()
  if ((!message && uploadedImages.value.length === 0) || chatStore.isLoading) return
  
  if (uploadedImages.value.length > 0) {
    const imageMarkdown = uploadedImages.value.map(img => `![读书笔记配图](${img.url})`).join('\n')
    message = message ? `${message}

${imageMarkdown}` : `请根据以下图片帮我写一份读书笔记：

${imageMarkdown}`
  }
  
  inputMessage.value = ''
  uploadedImages.value = []
  
  isTalking.value = true
  
  try {
    await chatStore.sendMessage(message, () => {
      scrollToBottom()
    })
  } catch (error) {
    ElMessage.error(error.message || '发送失败')
  } finally {
    isTalking.value = false
  }
}

const handleNewChat = () => {
  chatStore.createNewSession()
}

const switchSession = async (sessionId) => {
  await chatStore.fetchMessages(sessionId)
  showHistory.value = false
  scrollToBottom()
}

const deleteSession = async (sessionId) => {
  try {
    await chatStore.removeSession(sessionId)
    ElMessage.success('删除成功')
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const copyMessage = (content) => {
  navigator.clipboard.writeText(content).then(() => {
    ElMessage.success('已复制到剪贴板')
  })
}

const regenerate = async (index) => {
  const userMsg = chatStore.messages[index - 1]
  if (userMsg && userMsg.role === 'user') {
    inputMessage.value = userMsg.content
    await handleSend()
  }
}

// 监听消息变化
watch(() => chatStore.messages.length, (newVal, oldVal) => {
  if (newVal > oldVal && !isOpen.value) {
    unreadCount.value++
  }
  scrollToBottom()
})

// 监听打开AI聊天事件
const handleOpenChat = () => {
  // 检查是否登录
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再使用AI助手')
    return
  }
  
  justOpened.value = true
  isOpen.value = true
  unreadCount.value = 0
  currentMood.value = 'excited'
  scrollToBottom()
}

// 点击外部区域关闭窗口
const handleClickOutside = (event) => {
  // 如果刚通过其他方式打开，跳过这次点击检测
  if (justOpened.value) {
    justOpened.value = false
    return
  }
  
  if (assistantRef.value && !assistantRef.value.contains(event.target)) {
    if (isOpen.value) {
      isOpen.value = false
      currentMood.value = 'normal'
    }
  }
}

onMounted(() => {
  // 只有登录后才获取会话列表
  if (userStore.isLoggedIn) {
    chatStore.fetchSessions().catch(() => {
      // 忽略错误，可能是未登录
    })
  }
  window.addEventListener('open-ai-chat', handleOpenChat)
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  window.removeEventListener('open-ai-chat', handleOpenChat)
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped lang="scss">
.ai-assistant {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 9999;
}

// 小埋悬浮球
.umaru-float {
  position: relative;
  cursor: pointer;
  
  &:hover {
    .unread-badge {
      transform: scale(1.2);
    }
  }
}

.unread-badge {
  position: absolute;
  top: -5px;
  right: -5px;
  min-width: 20px;
  height: 20px;
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a5a 100%);
  border-radius: 10px;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 6px;
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.4);
  transition: transform 0.3s ease;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
}

// 对话窗口
.ai-chat-window {
  width: 420px;
  height: 620px;
  background: #fff;
  border-radius: 24px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.chat-header {
  padding: 16px 20px;
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a5a 100%);
  color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  
  .chat-title {
    display: flex;
    align-items: center;
    gap: 12px;
    
    .title-text {
      display: flex;
      flex-direction: column;
      
      .name {
        font-weight: 600;
        font-size: 16px;
      }
      
      .status {
        font-size: 12px;
        opacity: 0.9;
        display: flex;
        align-items: center;
        gap: 4px;
        
        &::before {
          content: '';
          width: 6px;
          height: 6px;
          background: #67c23a;
          border-radius: 50%;
        }
      }
    }
  }
  
  .chat-actions {
    display: flex;
    gap: 4px;
    
    .el-button {
      color: #fff;
      
      &:hover {
        background: rgba(255, 255, 255, 0.2);
      }
    }
  }
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: linear-gradient(180deg, #fff5f5 0%, #fff 100%);
  
  .welcome-message {
    text-align: center;
    padding: 30px 20px;
    
    .welcome-avatar {
      margin: 0 auto 20px;
    }
    
    h3 {
      font-size: 20px;
      color: #ff6b6b;
      margin-bottom: 16px;
      font-weight: 600;
    }
    
    p {
      color: #666;
      margin-bottom: 12px;
      font-size: 14px;
    }
    
    ul {
      text-align: left;
      display: inline-block;
      color: #555;
      background: rgba(255, 107, 107, 0.05);
      padding: 16px 24px;
      border-radius: 16px;
      
      li {
        margin: 8px 0;
        padding-left: 8px;
        font-size: 13px;
      }
    }
  }
}

.message-item {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  
  &.user {
    flex-direction: row-reverse;
    
    .message-content {
      background: linear-gradient(135deg, #ff6b6b 0%, #ee5a5a 100%);
      color: #fff;
      border-radius: 20px 20px 4px 20px;
    }
  }
  
  &.assistant {
    .message-content {
      background: #fff;
      border-radius: 20px 20px 20px 4px;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
    }
  }
  
  .message-content {
    max-width: 70%;
    padding: 14px 18px;
    font-size: 14px;
    line-height: 1.6;
    
    .message-actions {
      margin-top: 8px;
      padding-top: 8px;
      border-top: 1px solid rgba(0, 0, 0, 0.06);
      display: flex;
      gap: 8px;
    }
  }
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 8px;
  
  span {
    width: 8px;
    height: 8px;
    background: #ff6b6b;
    border-radius: 50%;
    animation: typing 1.4s infinite;
    
    &:nth-child(2) {
      animation-delay: 0.2s;
    }
    
    &:nth-child(3) {
      animation-delay: 0.4s;
    }
  }
}

@keyframes typing {
  0%, 60%, 100% {
    transform: translateY(0);
  }
  30% {
    transform: translateY(-10px);
  }
}

.chat-input {
  padding: 16px 20px;
  background: #fff;
  border-top: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
  gap: 12px;
  
  .image-preview-list {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
    
    .image-preview-item {
      position: relative;
      width: 60px;
      height: 60px;
      border-radius: 8px;
      overflow: hidden;
      border: 2px solid #ffe4e1;
      
      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }
      
      .remove-btn {
        position: absolute;
        top: 0;
        right: 0;
        padding: 2px;
        background: rgba(255, 107, 107, 0.8);
        color: #fff;
        
        &:hover {
          background: rgba(255, 107, 107, 1);
        }
      }
    }
  }
  
  .input-row {
    display: flex;
    gap: 12px;
    
    :deep(.el-textarea__inner) {
      border-radius: 16px;
      resize: none;
      
      &:focus {
        border-color: #ff6b6b;
      }
    }
    
    .input-actions {
      display: flex;
      align-items: flex-end;
      gap: 4px;
      
      .el-button--primary {
        background: linear-gradient(135deg, #ff6b6b 0%, #ee5a5a 100%);
        border: none;
        border-radius: 12px;
      }
    }
  }
}

.session-list {
  .session-item {
    padding: 16px;
    border-bottom: 1px solid #f0f0f0;
    cursor: pointer;
    position: relative;
    transition: all 0.3s ease;
    border-radius: 12px;
    margin-bottom: 8px;
    
    &:hover {
      background: rgba(255, 107, 107, 0.05);
      
      .delete-btn {
        opacity: 1;
      }
    }
    
    &.active {
      background: rgba(255, 107, 107, 0.1);
      border-left: 3px solid #ff6b6b;
    }
    
    .session-title {
      font-weight: 500;
      color: #333;
      margin-bottom: 8px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    
    .session-meta {
      font-size: 12px;
      color: #999;
      display: flex;
      gap: 16px;
    }
    
    .delete-btn {
      position: absolute;
      right: 12px;
      top: 50%;
      transform: translateY(-50%);
      opacity: 0;
      transition: opacity 0.2s;
    }
  }
}
</style>
