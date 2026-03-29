<template>
  <div class="mobile-messages">
    <!-- 顶部导航 -->
    <div class="mobile-header">
      <el-icon size="22" @click="$router.back()"><ArrowLeft /></el-icon>
      <div class="header-title">消息</div>
      <el-icon size="22" @click="markAllRead"><Check /></el-icon>
    </div>

    <!-- 消息类型 -->
    <div class="message-tabs">
      <div
        v-for="tab in tabs"
        :key="tab.key"
        class="tab-item"
        :class="{ active: activeTab === tab.key }"
        @click="activeTab = tab.key"
      >
        {{ tab.label }}
        <span v-if="tab.unread" class="badge">{{ tab.unread }}</span>
      </div>
    </div>

    <!-- 消息列表 -->
    <div class="message-list">
      <div
        v-for="msg in messages"
        :key="msg.id"
        class="message-item"
        :class="{ unread: !msg.isRead }"
        @click="readMessage(msg)"
      >
        <div class="msg-avatar">
          <el-avatar :size="48" :src="msg.avatar || defaultAvatar" />
          <div v-if="!msg.isRead" class="unread-dot"></div>
        </div>
        <div class="msg-content">
          <div class="msg-header">
            <span class="msg-title">{{ msg.title }}</span>
            <span class="msg-time">{{ formatTime(msg.time) }}</span>
          </div>
          <p class="msg-text">{{ msg.content }}</p>
        </div>
      </div>

      <!-- 空状态 -->
      <div class="empty-state" v-if="messages.length === 0">
        <el-icon size="48" color="#ddd"><ChatDotRound /></el-icon>
        <p>暂无消息</p>
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
        <el-icon size="24">
          <component :is="item.icon" />
        </el-icon>
        <span>{{ item.label }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft, Check, ChatDotRound,
  HomeFilled, Document, Folder, ChatLineRound
} from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

const router = useRouter()

const defaultAvatar = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'

// 标签页
const tabs = [
  { key: 'all', label: '全部', unread: 0 },
  { key: 'comment', label: '评论', unread: 2 },
  { key: 'like', label: '点赞', unread: 5 },
  { key: 'system', label: '系统', unread: 0 }
]
const activeTab = ref('all')

// 底部导航
const tabbarItems = [
  { path: '/m/home', label: '首页', icon: 'HomeFilled' },
  { path: '/m/articles', label: '文章', icon: 'Document' },
  { path: '/m/categories', label: '分类', icon: 'Folder' },
  { path: '/m/messages', label: '消息', icon: 'ChatLineRound' }
]

// 模拟消息数据
const messages = ref([
  {
    id: 1,
    title: '系统通知',
    content: '欢迎使用 Blog Space，开始你的创作之旅吧！',
    time: new Date(),
    isRead: false,
    avatar: null,
    type: 'system'
  },
  {
    id: 2,
    title: '小明',
    content: '评论了你的文章《欢迎使用 Blog Space》',
    time: new Date(Date.now() - 3600000),
    isRead: false,
    avatar: null,
    type: 'comment'
  },
  {
    id: 3,
    title: '小红',
    content: '点赞了你的文章',
    time: new Date(Date.now() - 7200000),
    isRead: true,
    avatar: null,
    type: 'like'
  }
])

const formatTime = (time) => {
  return dayjs(time).fromNow()
}

const readMessage = (msg) => {
  msg.isRead = true
  // 根据消息类型跳转
  if (msg.type === 'comment' || msg.type === 'like') {
    router.push('/articles/1')
  }
}

const markAllRead = () => {
  messages.value.forEach(msg => msg.isRead = true)
  ElMessage.success('已全部标记为已读')
}

onMounted(() => {
  // 加载真实消息数据
})
</script>

<style scoped lang="scss">
.mobile-messages {
  min-height: 100vh;
  background: #f5f5f5;
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
  justify-content: space-between;
  padding: 0 16px;
  z-index: 100;
  color: #fff;

  .header-title {
    font-size: 17px;
    font-weight: 600;
  }
}

// 消息类型
.message-tabs {
  position: fixed;
  top: 50px;
  left: 0;
  right: 0;
  height: 44px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-around;
  z-index: 99;
  border-bottom: 1px solid #f0f0f0;

  .tab-item {
    font-size: 14px;
    color: #666;
    padding: 8px 12px;
    position: relative;

    &.active {
      color: #FF69B4;
      font-weight: 600;
    }

    .badge {
      position: absolute;
      top: 0;
      right: 0;
      background: #ff4757;
      color: #fff;
      font-size: 10px;
      padding: 2px 6px;
      border-radius: 10px;
      transform: translate(30%, -20%);
    }
  }
}

// 消息列表
.message-list {
  padding-top: 104px;
}

.message-item {
  background: #fff;
  padding: 16px;
  display: flex;
  gap: 12px;
  border-bottom: 1px solid #f5f5f5;

  &:active {
    background: #fafafa;
  }

  &.unread {
    background: #fff8fa;
  }

  .msg-avatar {
    position: relative;

    .unread-dot {
      position: absolute;
      top: 0;
      right: 0;
      width: 10px;
      height: 10px;
      background: #ff4757;
      border-radius: 50%;
      border: 2px solid #fff;
    }
  }

  .msg-content {
    flex: 1;
    min-width: 0;

    .msg-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 6px;

      .msg-title {
        font-size: 15px;
        font-weight: 600;
        color: #333;
      }

      .msg-time {
        font-size: 12px;
        color: #999;
      }
    }

    .msg-text {
      font-size: 14px;
      color: #666;
      line-height: 1.5;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
  }
}

// 空状态
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100px 20px;
  color: #999;

  p {
    margin-top: 16px;
    font-size: 14px;
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
</style>
