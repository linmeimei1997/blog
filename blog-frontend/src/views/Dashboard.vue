<template>
  <div class="dashboard">
    <!-- Canvas 波浪动画背景 -->
    <WaveBackground />
    
    <!-- 欢迎区域 -->
    <div class="welcome-section">
      <div class="welcome-content">
        <div class="welcome-text">
          <h1 class="gradient-text">👋 欢迎回来, {{ userStore.nickname || userStore.username }}</h1>
          <p class="welcome-subtitle">今天也是充满创造力的一天！</p>
        </div>
        <div class="welcome-date">
          <div class="date-box">
            <span class="day">{{ currentDay }}</span>
            <span class="month">{{ currentMonth }}</span>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6" v-for="(stat, index) in statCards" :key="index">
        <el-card class="stat-card" :class="`stat-card-${index}`">
          <div class="stat-content">
            <div class="stat-icon-wrapper" :style="{ background: stat.gradient }">
              <el-icon size="28" color="#fff">
                <component :is="stat.icon" />
              </el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stat.value }}</div>
              <div class="stat-label">{{ stat.label }}</div>
            </div>
          </div>
          <div class="stat-trend" :class="stat.trend > 0 ? 'up' : 'down'">
            <el-icon><ArrowUp v-if="stat.trend > 0" /><ArrowDown v-else /></el-icon>
            <span>{{ Math.abs(stat.trend) }}%</span>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 快捷操作 -->
    <div class="quick-actions-section">
      <h3 class="section-title">
        <el-icon><StarFilled /></el-icon>
        快捷操作
      </h3>
      <div class="action-cards">
        <div 
          v-for="(action, index) in quickActions" 
          :key="index"
          class="action-card"
          :style="{ background: action.gradient }"
          @click="$router.push(action.path)"
        >
          <div class="action-icon">
            <el-icon size="32" color="#fff">
              <component :is="action.icon" />
            </el-icon>
          </div>
          <div class="action-text">
            <h4>{{ action.title }}</h4>
            <p>{{ action.desc }}</p>
          </div>
          <div class="action-arrow">
            <el-icon color="#fff" size="20"><ArrowRight /></el-icon>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 内容区域 -->
    <el-row :gutter="20" class="content-row">
      <el-col :span="14">
        <el-card class="content-card latest-articles">
          <template #header>
            <div class="card-header">
              <div class="header-title">
                <el-icon><Clock /></el-icon>
                <span>最新文章</span>
              </div>
              <el-button text type="primary" @click="$router.push('/articles')">
                查看全部
                <el-icon class="el-icon--right"><ArrowRight /></el-icon>
              </el-button>
            </div>
          </template>
          
          <div v-if="latestArticles.length === 0" class="empty-state">
            <el-icon size="48" color="#dcdfe6"><Document /></el-icon>
            <p>还没有文章，快去写一篇吧！</p>
            <el-button type="primary" @click="$router.push('/articles/create')">
              <el-icon><Plus /></el-icon>
              写文章
            </el-button>
          </div>
          
          <div v-else class="article-list">
            <div 
              v-for="article in latestArticles.slice(0, 5)" 
              :key="article.id"
              class="article-item"
              @click="$router.push(`/articles/${article.id}`)"
            >
              <div class="article-info">
                <h4 class="article-title">{{ article.title }}</h4>
                <div class="article-meta">
                  <span class="meta-item">
                    <el-icon><View /></el-icon>
                    {{ article.viewCount || 0 }}
                  </span>
                  <span class="meta-item">
                    <el-icon><Calendar /></el-icon>
                    {{ formatDate(article.createTime) }}
                  </span>
                </div>
              </div>
              <el-tag :type="article.status === 1 ? 'success' : 'info'" size="small">
                {{ article.status === 1 ? '已发布' : '草稿' }}
              </el-tag>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="10">
        <el-card class="content-card ai-assistant-intro">
          <div class="ai-intro-content">
            <div class="ai-avatar">
              <div class="ai-avatar-inner">
                <el-icon size="40" color="#fff"><ChatDotRound /></el-icon>
              </div>
              <div class="ai-pulse"></div>
            </div>
            <h3>AI 智能助手</h3>
            <p>你的专属创作伙伴，可以帮你：</p>
            <ul class="feature-list">
              <li><el-icon><Check /></el-icon> 自动生成博客文章</li>
              <li><el-icon><Check /></el-icon> 写读书笔记（支持图文）</li>
              <li><el-icon><Check /></el-icon> 知识库智能问答</li>
              <li><el-icon><Check /></el-icon> 文章润色优化</li>
            </ul>
            <el-button type="primary" class="ai-btn" @click="openAIChat">
              <el-icon><ChatDotRound /></el-icon>
              开始对话
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getLatestArticles } from '@/api/article'
import { getCategoryList } from '@/api/category'
import { getTagList } from '@/api/tag'
import dayjs from 'dayjs'
import WaveBackground from '@/components/WaveBackground.vue'
import {
  Document, View, Folder, CollectionTag, Plus, ArrowRight,
  Clock, Calendar, StarFilled, ChatDotRound, Check, ArrowUp, ArrowDown
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const stats = ref({
  articles: 0,
  views: 0,
  categories: 0,
  tags: 0
})

const latestArticles = ref([])

const currentDay = computed(() => dayjs().format('DD'))
const currentMonth = computed(() => dayjs().format('MMM'))

const statCards = computed(() => [
  {
    icon: 'Document',
    value: stats.value.articles,
    label: '文章总数',
    gradient: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    trend: 12
  },
  {
    icon: 'View',
    value: stats.value.views,
    label: '总阅读量',
    gradient: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
    trend: 8
  },
  {
    icon: 'Folder',
    value: stats.value.categories,
    label: '分类数量',
    gradient: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
    trend: -3
  },
  {
    icon: 'CollectionTag',
    value: stats.value.tags,
    label: '标签数量',
    gradient: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
    trend: 15
  }
])

const quickActions = [
  {
    icon: 'Edit',
    title: '写文章',
    desc: '记录你的想法',
    gradient: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    path: '/articles/create'
  },
  {
    icon: 'Upload',
    title: '传文档',
    desc: '添加到知识库',
    gradient: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
    path: '/knowledge'
  },
  {
    icon: 'FolderAdd',
    title: '建分类',
    desc: '整理内容结构',
    gradient: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
    path: '/categories'
  },
  {
    icon: 'ChatDotRound',
    title: '问AI',
    desc: '智能创作助手',
    gradient: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
    path: '#ai-chat'
  }
]

const formatDate = (date) => {
  return date ? dayjs(date).format('MM-DD') : '-'
}

const openAIChat = () => {
  // 触发自定义事件打开AI助手
  window.dispatchEvent(new CustomEvent('open-ai-chat'))
}

const loadData = async () => {
  try {
    const [articles, categories, tags] = await Promise.all([
      getLatestArticles(10),
      getCategoryList(),
      getTagList()
    ])
    
    latestArticles.value = articles
    stats.value.articles = articles.length
    stats.value.views = articles.reduce((sum, a) => sum + (a.viewCount || 0), 0)
    stats.value.categories = categories.length
    stats.value.tags = tags.length
  } catch (error) {
    console.error('加载数据失败:', error)
  }
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.dashboard {
  position: relative;
  min-height: calc(100vh - 100px);
}

// 欢迎区域
.welcome-section {
  position: relative;
  z-index: 1;
  margin-bottom: 24px;
  
  .welcome-content {
    display: flex;
    justify-content: space-between;
    align-items: center;
    background: rgba(255, 255, 255, 0.9);
    backdrop-filter: blur(10px);
    border-radius: 20px;
    padding: 30px 40px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  }
  
  .welcome-text {
    h1 {
      font-size: 28px;
      font-weight: 700;
      margin-bottom: 8px;
      
      &.gradient-text {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
        background-clip: text;
      }
    }
    
    .welcome-subtitle {
      color: #888;
      font-size: 14px;
    }
  }
  
  .welcome-date {
    .date-box {
      display: flex;
      flex-direction: column;
      align-items: center;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border-radius: 16px;
      padding: 15px 25px;
      color: #fff;
      box-shadow: 0 10px 30px rgba(102, 126, 234, 0.3);
      
      .day {
        font-size: 36px;
        font-weight: 700;
        line-height: 1;
      }
      
      .month {
        font-size: 14px;
        text-transform: uppercase;
        margin-top: 4px;
      }
    }
  }
}

// 统计卡片
.stats-row {
  position: relative;
  z-index: 1;
  margin-bottom: 24px;
}

.stat-card {
  border-radius: 20px;
  border: none;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
  overflow: hidden;
  
  &:hover {
    transform: translateY(-5px);
    box-shadow: 0 12px 40px rgba(0, 0, 0, 0.1);
  }
  
  :deep(.el-card__body) {
    padding: 24px;
  }
  
  .stat-content {
    display: flex;
    align-items: center;
    gap: 16px;
  }
  
  .stat-icon-wrapper {
    width: 56px;
    height: 56px;
    border-radius: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 8px 20px rgba(0, 0, 0, 0.15);
  }
  
  .stat-info {
    flex: 1;
    
    .stat-value {
      font-size: 28px;
      font-weight: 700;
      color: #303133;
      line-height: 1;
    }
    
    .stat-label {
      font-size: 13px;
      color: #909399;
      margin-top: 6px;
    }
  }
  
  .stat-trend {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
    font-weight: 600;
    padding: 4px 10px;
    border-radius: 20px;
    
    &.up {
      color: #67c23a;
      background: rgba(103, 194, 58, 0.1);
    }
    
    &.down {
      color: #f56c6c;
      background: rgba(245, 108, 108, 0.1);
    }
  }
}

// 快捷操作
.quick-actions-section {
  position: relative;
  z-index: 1;
  margin-bottom: 24px;
  
  .section-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 18px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 16px;
    
    .el-icon {
      color: #667eea;
    }
  }
  
  .action-cards {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;
  }
  
  .action-card {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 24px;
    border-radius: 20px;
    cursor: pointer;
    transition: all 0.3s ease;
    position: relative;
    overflow: hidden;
    
    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: linear-gradient(135deg, rgba(255,255,255,0.2) 0%, transparent 100%);
      opacity: 0;
      transition: opacity 0.3s ease;
    }
    
    &:hover {
      transform: translateY(-5px) scale(1.02);
      box-shadow: 0 15px 40px rgba(0, 0, 0, 0.2);
      
      &::before {
        opacity: 1;
      }
      
      .action-arrow {
        transform: translateX(5px);
      }
    }
    
    .action-icon {
      width: 56px;
      height: 56px;
      background: rgba(255, 255, 255, 0.2);
      border-radius: 16px;
      display: flex;
      align-items: center;
      justify-content: center;
      backdrop-filter: blur(10px);
    }
    
    .action-text {
      flex: 1;
      color: #fff;
      
      h4 {
        font-size: 16px;
        font-weight: 600;
        margin-bottom: 4px;
      }
      
      p {
        font-size: 12px;
        opacity: 0.9;
      }
    }
    
    .action-arrow {
      transition: transform 0.3s ease;
    }
  }
}

// 内容区域
.content-row {
  position: relative;
  z-index: 1;
}

.content-card {
  border-radius: 20px;
  border: none;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  min-height: 400px;
  
  :deep(.el-card__header) {
    padding: 20px 24px;
    border-bottom: 1px solid #f0f0f0;
  }
  
  :deep(.el-card__body) {
    padding: 24px;
  }
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .header-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 16px;
      font-weight: 600;
      color: #303133;
      
      .el-icon {
        color: #667eea;
      }
    }
  }
}

// 文章列表
.article-list {
  .article-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px 0;
    border-bottom: 1px solid #f5f5f5;
    cursor: pointer;
    transition: all 0.3s ease;
    
    &:last-child {
      border-bottom: none;
    }
    
    &:hover {
      background: rgba(102, 126, 234, 0.03);
      margin: 0 -24px;
      padding-left: 24px;
      padding-right: 24px;
      border-radius: 12px;
      
      .article-title {
        color: #667eea;
      }
    }
    
    .article-info {
      flex: 1;
      
      .article-title {
        font-size: 15px;
        font-weight: 500;
        color: #303133;
        margin-bottom: 8px;
        transition: color 0.3s ease;
      }
      
      .article-meta {
        display: flex;
        gap: 16px;
        
        .meta-item {
          display: flex;
          align-items: center;
          gap: 4px;
          font-size: 12px;
          color: #909399;
          
          .el-icon {
            font-size: 14px;
          }
        }
      }
    }
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #909399;
  
  p {
    margin: 16px 0;
    font-size: 14px;
  }
}

// AI 助手介绍
.ai-assistant-intro {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.05) 0%, rgba(118, 75, 162, 0.05) 100%);
  
  :deep(.el-card__body) {
    height: 100%;
    display: flex;
    flex-direction: column;
  }
}

.ai-intro-content {
  text-align: center;
  flex: 1;
  display: flex;
  flex-direction: column;
  
  .ai-avatar {
    position: relative;
    width: 100px;
    height: 100px;
    margin: 0 auto 24px;
    
    .ai-avatar-inner {
      width: 80px;
      height: 80px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      position: relative;
      z-index: 2;
      box-shadow: 0 10px 30px rgba(102, 126, 234, 0.4);
    }
    
    .ai-pulse {
      position: absolute;
      top: 0;
      left: 10px;
      width: 80px;
      height: 80px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border-radius: 50%;
      opacity: 0.3;
      animation: pulse-ring 2s infinite;
    }
  }
  
  h3 {
    font-size: 22px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 8px;
  }
  
  p {
    color: #666;
    margin-bottom: 24px;
  }
  
  .feature-list {
    list-style: none;
    padding: 0;
    margin: 0 0 24px;
    text-align: left;
    
    li {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 10px 0;
      color: #555;
      font-size: 14px;
      
      .el-icon {
        color: #67c23a;
        font-size: 16px;
      }
    }
  }
  
  .ai-btn {
    margin-top: auto;
    height: 48px;
    border-radius: 24px;
    font-size: 16px;
    font-weight: 600;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border: none;
    box-shadow: 0 8px 20px rgba(102, 126, 234, 0.3);
    
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 12px 30px rgba(102, 126, 234, 0.4);
    }
  }
}

@keyframes pulse-ring {
  0% {
    transform: scale(1);
    opacity: 0.3;
  }
  50% {
    transform: scale(1.3);
    opacity: 0.1;
  }
  100% {
    transform: scale(1);
    opacity: 0.3;
  }
}
</style>
