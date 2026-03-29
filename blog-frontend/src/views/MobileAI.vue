<template>
  <div class="mobile-ai">
    <!-- 顶部导航 -->
    <div class="mobile-header">
      <div class="header-title">AI 助手</div>
      <div class="header-actions">
        <el-icon size="20" @click="handleNewChat"><Plus /></el-icon>
        <el-icon size="20" @click="showHistory = true"><Clock /></el-icon>
      </div>
    </div>

    <!-- 聊天区域 -->
    <div class="chat-container" ref="chatContainer">
      <!-- 欢迎消息 -->
      <div class="welcome-message" v-if="chatStore.messages.length === 0">
        <div class="ai-avatar-large">
          <el-icon size="48" color="#fff"><ChatDotRound /></el-icon>
        </div>
        <h3>我是你的 AI 助手</h3>
        <p>可以帮你：</p>
        <ul>
          <li>📝 搜索和阅读博客文章</li>
          <li>📚 查询知识库文档</li>
          <li>✨ 总结文档内容</li>
          <li>📖 写读书笔记</li>
          <li>💡 回答技术问题</li>
        </ul>
      </div>

      <!-- 消息列表 -->
      <template v-else>
        <div
          v-for="(msg, index) in chatStore.messages"
          :key="index"
          v-show="!(msg.role === 'assistant' && !msg.content)"
          class="message-item"
          :class="{ 'user-message': msg.role === 'user', 'ai-message': msg.role === 'assistant' }"
        >
          <div class="message-avatar">
            <el-avatar
              :size="36"
              :src="msg.role === 'user' ? userAvatar : aiAvatar"
            />
          </div>
          <div class="message-content">
            <div class="message-bubble">
              <div class="message-text" v-html="formatMessage(msg.content)"></div>
            </div>
            <div class="message-actions" v-if="msg.role === 'assistant' && msg.content">
              <el-icon size="14" @click="copyMessage(msg.content)"><CopyDocument /></el-icon>
            </div>
          </div>
        </div>

        <!-- 正在输入提示 -->
        <div v-if="chatStore.isLoading && isLastMessageEmpty" class="message-item ai-message">
          <div class="message-avatar">
            <el-avatar :size="36" :src="aiAvatar" />
          </div>
          <div class="message-content">
            <div class="message-bubble">
              <div class="loading-dots">
                <span></span><span></span><span></span>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- 输入区域 -->
    <div class="input-area">
      <!-- 已上传图片预览 -->
      <div v-if="uploadedImages.length > 0" class="image-preview-list">
        <div v-for="(img, idx) in uploadedImages" :key="idx" class="image-preview-item">
          <img :src="img.url" alt="预览" />
          <el-icon class="remove-btn" @click="removeImage(idx)"><Close /></el-icon>
        </div>
      </div>

      <div class="input-box">
        <input
          type="file"
          ref="imageInput"
          style="display: none"
          accept="image/*"
          @change="handleImageSelect"
        />
        <el-icon :size="20" class="toolbar-icon" @click="$refs.imageInput.click()">
          <Picture />
        </el-icon>
        <el-input
          v-model="inputMessage"
          type="textarea"
          :rows="1"
          :placeholder="uploadedImages.length > 0 ? '描述图片...' : '输入消息...'"
          resize="none"
          @keydown.enter.prevent="sendMessage"
        />
        <el-button
          type="primary"
          circle
          :disabled="(!inputMessage.trim() && uploadedImages.length === 0) || chatStore.isLoading"
          @click="sendMessage"
        >
          <el-icon><Promotion /></el-icon>
        </el-button>
      </div>
    </div>

    <!-- 底部导航 -->
    <div class="mobile-tabbar">
      <div
        v-for="item in tabbarItems"
        :key="item.path"
        class="tabbar-item"
        :class="{ active: $route.path === item.path }"
        @click="$router.push(item.path)"
      >
        <el-icon size="22">
          <component :is="item.icon" />
        </el-icon>
        <span>{{ item.label }}</span>
      </div>
    </div>

    <!-- 历史会话抽屉 -->
    <el-drawer
      v-model="showHistory"
      title="历史会话"
      size="80%"
      direction="rtl"
    >
      <div class="session-list">
        <div
          v-for="session in chatStore.sessions"
          :key="session.sessionId"
          :class="['session-item', { active: session.sessionId === chatStore.currentSessionId }]"
          @click="switchSession(session.sessionId)"
        >
          <div class="session-title">{{ session.title || '新对话' }}</div>
          <div class="session-meta">
            <span>{{ session.messageCount || 0 }} 条消息</span>
            <span>{{ formatTime(session.lastMessageTime) }}</span>
          </div>
          <el-icon class="delete-btn" @click.stop="deleteSession(session.sessionId)">
            <Delete />
          </el-icon>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ChatDotRound, Plus, Clock, Picture, Promotion,
  CopyDocument, Close, Delete, HomeFilled, Document, Folder
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useChatStore } from '@/stores/chat'
import dayjs from 'dayjs'

const router = useRouter()
const userStore = useUserStore()
const chatStore = useChatStore()

// 头像
const userAvatar = computed(() => userStore.avatar || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png')
const aiAvatar = 'https://api.dicebear.com/7.x/bottts/svg?seed=AI'

// 底部导航
const tabbarItems = [
  { path: '/m/home', label: '首页', icon: 'HomeFilled' },
  { path: '/m/articles', label: '文章', icon: 'Document' },
  { path: '/m/ai', label: 'AI助手', icon: 'ChatDotRound' },
  { path: '/m/categories', label: '分类', icon: 'Folder' }
]

// 状态
const inputMessage = ref('')
const showHistory = ref(false)
const chatContainer = ref(null)
const imageInput = ref(null)
const uploadedImages = ref([])

// 计算属性：最后一条助手消息是否为空
const isLastMessageEmpty = computed(() => {
  const lastMsg = chatStore.messages[chatStore.messages.length - 1]
  return lastMsg && lastMsg.role === 'assistant' && !lastMsg.content
})

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  return dayjs(time).format('MM-DD HH:mm')
}

// 格式化消息
const formatMessage = (content) => {
  if (!content) return ''
  
  let formatted = content.replace(/!\[([^\]]*)\]\(([^\)]+)\)/g, '<img src="$2" alt="$1" style="max-width:100%;border-radius:8px;margin:8px 0;" />')
  formatted = formatted.replace(/\[([^\]]+)\]\(([^\)]+)\)/g, '<a href="$2" target="_blank" style="color:#FF69B4;text-decoration:none;">$1</a>')
  formatted = formatted.replace(/\*\*([^\*]+)\*\*/g, '<strong>$1</strong>')
  formatted = formatted.replace(/\*([^\*]+)\*/g, '<em>$1</em>')
  formatted = formatted.replace(/`([^`]+)`/g, '<code style="background:#f5f7fa;padding:2px 4px;border-radius:4px;">$1</code>')
  formatted = formatted.replace(/\n/g, '<br>')
  
  return formatted
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight
    }
  })
}

// 发送消息
const sendMessage = async () => {
  let message = inputMessage.value.trim()
  if ((!message && uploadedImages.value.length === 0) || chatStore.isLoading) return

  // 处理图片
  if (uploadedImages.value.length > 0) {
    const imageMarkdown = uploadedImages.value.map(img => `![读书笔记配图](${img.url})`).join('\n')
    message = message ? `${message}

${imageMarkdown}` : `请根据以下图片帮我写一份读书笔记：

${imageMarkdown}`
  }

  inputMessage.value = ''
  uploadedImages.value = []
  scrollToBottom()

  try {
    await chatStore.sendMessage(message, () => {
      scrollToBottom()
    })
  } catch (error) {
    ElMessage.error(error.message || '发送失败')
  }
}

// 新对话
const handleNewChat = () => {
  chatStore.createNewSession()
}

// 切换会话
const switchSession = async (sessionId) => {
  await chatStore.fetchMessages(sessionId)
  showHistory.value = false
  scrollToBottom()
}

// 删除会话
const deleteSession = async (sessionId) => {
  try {
    await chatStore.removeSession(sessionId)
    ElMessage.success('删除成功')
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

// 复制消息
const copyMessage = (content) => {
  navigator.clipboard.writeText(content).then(() => {
    ElMessage.success('已复制')
  })
}

// 图片选择
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
    ElMessage.error('图片上传失败')
  }

  event.target.value = ''
}

// 移除图片
const removeImage = (index) => {
  uploadedImages.value.splice(index, 1)
}

// 监听消息变化
watch(() => chatStore.messages.length, () => {
  scrollToBottom()
})

onMounted(() => {
  if (userStore.isLoggedIn) {
    chatStore.fetchSessions().catch(() => {})
  }
  scrollToBottom()
})
</script>

<style scoped lang="scss">
.mobile-ai {
  min-height: 100vh;
  background: #f5f5f5;
  display: flex;
  flex-direction: column;
  padding-bottom: 60px;
}

// 顶部导航
.mobile-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 50px;
  background: linear-gradient(135deg, #FF69B4 0%, #FFB6C1 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  color: #fff;

  .header-title {
    font-size: 17px;
    font-weight: 600;
  }

  .header-actions {
    position: absolute;
    right: 16px;
    display: flex;
    gap: 16px;
  }
}

// 聊天容器
.chat-container {
  flex: 1;
  margin-top: 50px;
  overflow-y: auto;
  padding: 16px;
  padding-bottom: 160px;
}

// 欢迎消息
.welcome-message {
  text-align: center;
  padding: 40px 20px;

  .ai-avatar-large {
    width: 80px;
    height: 80px;
    background: linear-gradient(135deg, #FF69B4 0%, #FFB6C1 100%);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto 20px;
    box-shadow: 0 8px 20px rgba(255, 105, 180, 0.3);
  }

  h3 {
    font-size: 20px;
    color: #FF69B4;
    margin-bottom: 16px;
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
    background: rgba(255, 105, 180, 0.05);
    padding: 16px 24px;
    border-radius: 16px;

    li {
      margin: 8px 0;
      padding-left: 8px;
      font-size: 13px;
    }
  }
}

// 消息列表
.message-item {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;

  &.user-message {
    flex-direction: row-reverse;

    .message-content {
      align-items: flex-end;
    }

    .message-bubble {
      background: linear-gradient(135deg, #FF69B4 0%, #FFB6C1 100%);
      color: #fff;
      border-radius: 16px 16px 4px 16px;
    }
  }

  &.ai-message {
    .message-bubble {
      background: #fff;
      border-radius: 16px 16px 16px 4px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
    }
  }

  .message-content {
    display: flex;
    flex-direction: column;
    gap: 4px;
    max-width: 75%;
  }

  .message-bubble {
    padding: 12px 16px;
    font-size: 14px;
    line-height: 1.6;
  }

  .message-actions {
    display: flex;
    justify-content: flex-end;
    color: #999;
  }
}

// 加载动画
.loading-dots {
  display: flex;
  gap: 4px;
  padding: 4px 8px;

  span {
    width: 8px;
    height: 8px;
    background: #FF69B4;
    border-radius: 50%;
    animation: bounce 1.4s infinite ease-in-out both;

    &:nth-child(1) { animation-delay: -0.32s; }
    &:nth-child(2) { animation-delay: -0.16s; }
  }
}

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

// 输入区域
.input-area {
  position: fixed;
  bottom: 60px;
  left: 0;
  right: 0;
  background: #fff;
  border-top: 1px solid #f0f0f0;
  padding: 12px 16px;
  z-index: 99;

  .image-preview-list {
    display: flex;
    gap: 8px;
    margin-bottom: 8px;

    .image-preview-item {
      position: relative;
      width: 50px;
      height: 50px;
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
        top: 2px;
        right: 2px;
        background: rgba(255, 107, 107, 0.8);
        color: #fff;
        border-radius: 50%;
        padding: 2px;
        font-size: 12px;
      }
    }
  }

  .input-box {
    display: flex;
    gap: 10px;
    align-items: flex-end;

    .toolbar-icon {
      color: #666;
      padding: 10px;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      &:active { color: #FF69B4; }
    }

    .el-textarea {
      flex: 1;

      :deep(.el-textarea__inner) {
        border-radius: 20px;
        padding: 10px 16px;
        min-height: 44px !important;
      }
    }

    .el-button {
      width: 44px;
      height: 44px;
      background: linear-gradient(135deg, #FF69B4 0%, #FFB6C1 100%);
      border: none;

      &:disabled {
        background: #ccc;
      }
    }
  }
}

// 底部导航
.mobile-tabbar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 60px;
  background: #fff;
  display: flex;
  justify-content: space-around;
  align-items: center;
  border-top: 1px solid #f0f0f0;
  z-index: 100;

  .tabbar-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    color: #999;
    font-size: 11px;

    &.active {
      color: #FF69B4;
    }
  }
}

// 会话列表
.session-list {
  padding: 16px;

  .session-item {
    padding: 16px;
    background: #f5f5f5;
    border-radius: 12px;
    margin-bottom: 12px;
    position: relative;

    &.active {
      background: #fff0f5;
      border-left: 3px solid #FF69B4;
    }

    .session-title {
      font-weight: 500;
      color: #333;
      margin-bottom: 8px;
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
      color: #999;

      &:active { color: #ff4757; }
    }
  }
}
</style>
