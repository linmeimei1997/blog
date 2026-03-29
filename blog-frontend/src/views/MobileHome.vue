<template>
  <div class="mobile-home">
    <!-- 顶部导航 -->
    <div class="mobile-header">
      <div class="header-title">Blog Space</div>
      <div class="header-actions">
        <el-icon size="22" @click="showSearch = true"><Search /></el-icon>
        <el-avatar
                  :size="28"
                  :src="userStore.avatar || defaultAvatar"
                  @click="$router.push('/m/profile')"
                  style="cursor: pointer"
                />
      </div>
    </div>

    <!-- 搜索弹窗 -->
    <el-dialog
      v-model="showSearch"
      :show-close="false"
      width="90%"
      class="search-dialog"
    >
      <div class="search-box">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索文章..."
          prefix-icon="Search"
          clearable
          @keyup.enter="handleSearch"
        />
        <el-button text @click="showSearch = false">取消</el-button>
      </div>
    </el-dialog>

    <!-- 内容标签页 -->
    <div class="content-tabs">
      <div
        v-for="tab in tabs"
        :key="tab.key"
        class="tab-item"
        :class="{ active: activeTab === tab.key }"
        @click="activeTab = tab.key"
      >
        {{ tab.label }}
      </div>
    </div>

    <!-- 文章列表 -->
    <div class="article-feed" ref="feedContainer" @scroll="handleScroll">
      <div
        v-for="article in articleList"
        :key="article.id"
        class="feed-item"
        @click="goToDetail(article.id)"
      >
        <!-- 用户信息 -->
        <div class="feed-header">
          <el-avatar :size="40" :src="article.authorAvatar || defaultAvatar" />
          <div class="feed-user-info">
            <div class="username">{{ article.authorName || '博主' }}</div>
            <div class="publish-time">{{ formatTime(article.createTime) }}</div>
          </div>
          <el-button
            v-if="article.categoryName"
            size="small"
            round
            class="category-tag"
          >
            {{ article.categoryName }}
          </el-button>
        </div>

        <!-- 文章内容 -->
        <div class="feed-content">
          <h3 class="feed-title">{{ article.title }}</h3>
          <p class="feed-summary">{{ article.summary || stripHtml(article.content) }}</p>
        </div>

        <!-- 封面图 -->
        <div v-if="article.coverImage" class="feed-image">
          <img :src="article.coverImage" alt="封面" />
        </div>

        <!-- 标签 -->
        <div v-if="article.tags && article.tags.length" class="feed-tags">
          <span v-for="tag in article.tags.slice(0, 3)" :key="tag" class="tag-item">
            #{{ tag }}
          </span>
        </div>

        <!-- 互动栏 -->
        <div class="feed-actions">
          <div class="action-item" @click.stop="handleShare(article)">
            <el-icon size="18"><Share /></el-icon>
            <span>分享</span>
          </div>
          <div class="action-item" @click.stop="handleComment(article)">
            <el-icon size="18"><ChatDotRound /></el-icon>
            <span>{{ article.commentCount || '评论' }}</span>
          </div>
          <div class="action-item" @click.stop="handleLike(article)">
            <el-icon size="18" :class="{ liked: article.isLiked }"><Star /></el-icon>
            <span>{{ article.likeCount || '点赞' }}</span>
          </div>
          <div class="action-item">
            <el-icon size="18"><View /></el-icon>
            <span>{{ article.viewCount || 0 }}</span>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div class="empty-feed" v-if="!loading && articleList.length === 0">
        <el-icon size="48" color="#ddd"><Document /></el-icon>
        <p>还没有文章哦~</p>
        <el-button type="primary" round @click="$router.push('/articles/create')">
          写第一篇
        </el-button>
      </div>

      <!-- 加载更多 -->
      <div class="load-more" v-if="loading">
        <el-icon class="loading-icon"><Loading /></el-icon>
        <span>加载中...</span>
      </div>
      <div class="load-more" v-else-if="noMore && articleList.length > 0">
        <span>已经到底了~</span>
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

    <!-- 发布按钮 -->
    <div class="fab-button" @click="$router.push('/m/editor')">
      <el-icon size="28"><Plus /></el-icon>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Search, User, Share, ChatDotRound, Star, View,
  Plus, Loading, HomeFilled, Document, Folder
} from '@element-plus/icons-vue'
import { getLatestArticles } from '@/api/article'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'
import { useUserStore } from '@/stores/user'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

const router = useRouter()
const userStore = useUserStore()

// 默认头像
const defaultAvatar = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'

// 搜索
const showSearch = ref(false)
const searchKeyword = ref('')

// 标签页
const tabs = [
  { key: 'all', label: '全部' },
  { key: 'tech', label: '技术' },
  { key: 'life', label: '生活' },
  { key: 'note', label: '笔记' }
]
const activeTab = ref('all')

// 底部导航
const tabbarItems = [
  { path: '/m/home', label: '首页', icon: 'HomeFilled' },
  { path: '/m/articles', label: '文章', icon: 'Document' },
  { path: '/m/ai', label: 'AI助手', icon: 'ChatDotRound' },
  { path: '/m/categories', label: '分类', icon: 'Folder' }
]

// 文章列表
const articleList = ref([])
const loading = ref(false)
const noMore = ref(false)
const pageNum = ref(1)
const pageSize = 10

// 模拟数据
const mockArticles = [
  {
    id: 1,
    title: '欢迎来到 Blog Space',
    content: '这是一个简洁优雅的博客系统，支持文章管理、分类标签、知识库等功能。',
    summary: '这是一个简洁优雅的博客系统，支持文章管理、分类标签、知识库等功能。',
    createTime: new Date().toISOString(),
    viewCount: 128,
    categoryName: '公告',
    tags: ['博客', '介绍'],
    coverImage: null
  },
  {
    id: 2,
    title: '如何使用 AI 助手',
    content: 'AI 助手可以帮助你自动生成文章、写读书笔记、知识库问答等。点击右下角的 AI 图标即可开始对话。',
    summary: 'AI 助手可以帮助你自动生成文章、写读书笔记、知识库问答等。',
    createTime: new Date(Date.now() - 86400000).toISOString(),
    viewCount: 86,
    categoryName: '教程',
    tags: ['AI', '教程'],
    coverImage: null
  },
  {
    id: 3,
    title: 'Markdown 写作指南',
    content: 'Markdown 是一种轻量级标记语言，让你专注于写作而不是排版。支持标题、列表、代码块等。',
    summary: 'Markdown 是一种轻量级标记语言，让你专注于写作而不是排版。',
    createTime: new Date(Date.now() - 172800000).toISOString(),
    viewCount: 256,
    categoryName: '教程',
    tags: ['Markdown', '写作'],
    coverImage: null
  }
]

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  return dayjs(time).fromNow()
}

// 去除 HTML 标签
const stripHtml = (html) => {
  if (!html) return ''
  return html.replace(/<[^>]+>/g, '').substring(0, 100) + '...'
}

// 加载文章
const loadArticles = async (reset = false) => {
  if (loading.value) return
  loading.value = true

  try {
    if (reset) {
      pageNum.value = 1
      noMore.value = false
    }

    const res = await getLatestArticles(pageSize)
    if (res && res.length > 0) {
      const newArticles = res.map(article => ({
        ...article,
        authorName: '博主',
        authorAvatar: defaultAvatar,
        summary: article.summary || stripHtml(article.content),
        tags: article.tags || ['博客', '分享'],
        commentCount: Math.floor(Math.random() * 50),
        likeCount: Math.floor(Math.random() * 100),
        isLiked: false
      }))

      if (reset) {
        articleList.value = newArticles
      } else {
        articleList.value.push(...newArticles)
      }
      pageNum.value++
    } else {
      noMore.value = true
    }
  } catch (error) {
    console.error('加载文章失败:', error)
    // 如果 API 失败，使用模拟数据
    if (reset) {
      articleList.value = mockArticles.map(article => ({
        ...article,
        authorName: '博主',
        authorAvatar: defaultAvatar,
        summary: article.summary || stripHtml(article.content),
        tags: article.tags || ['博客', '分享'],
        commentCount: Math.floor(Math.random() * 50),
        likeCount: Math.floor(Math.random() * 100),
        isLiked: false
      }))
    }
  } finally {
    loading.value = false
  }
}

// 滚动加载
const handleScroll = (e) => {
  const { scrollTop, scrollHeight, clientHeight } = e.target
  if (scrollTop + clientHeight >= scrollHeight - 50 && !loading.value && !noMore.value) {
    loadArticles()
  }
}

// 搜索
const handleSearch = () => {
  if (!searchKeyword.value.trim()) return
  ElMessage.info(`搜索: ${searchKeyword.value}`)
  showSearch.value = false
}

// 跳转详情
const goToDetail = (id) => {
  router.push(`/m/articles/${id}`)
}

// 分享
const handleShare = (article) => {
  ElMessage.success('分享功能开发中')
}

// 评论
const handleComment = (article) => {
  router.push(`/articles/${article.id}#comments`)
}

// 点赞
const handleLike = (article) => {
  article.isLiked = !article.isLiked
  article.likeCount += article.isLiked ? 1 : -1
  ElMessage.success(article.isLiked ? '已点赞' : '取消点赞')
}

// 监听标签切换
watch(activeTab, () => {
  loadArticles(true)
})

onMounted(() => {
  loadArticles(true)
})
</script>

<style scoped lang="scss">
.mobile-home {
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
  box-shadow: 0 2px 10px rgba(255, 105, 180, 0.3);

  .header-title {
    font-size: 18px;
    font-weight: 700;
    color: #fff;
  }

  .header-actions {
    display: flex;
    gap: 20px;
    color: #fff;
  }
}

// 搜索弹窗
.search-dialog {
  :deep(.el-dialog) {
    margin-top: 10vh !important;
    border-radius: 12px;
  }

  .search-box {
    display: flex;
    gap: 12px;
    align-items: center;
  }
}

// 内容标签页
.content-tabs {
  position: fixed;
  top: 50px;
  left: 0;
  right: 0;
  height: 44px;
  background: #fff;
  display: flex;
  align-items: center;
  padding: 0 16px;
  gap: 24px;
  z-index: 99;
  border-bottom: 1px solid #f0f0f0;

  .tab-item {
    font-size: 14px;
    color: #666;
    padding: 8px 0;
    position: relative;
    white-space: nowrap;

    &.active {
      color: #FF69B4;
      font-weight: 600;

      &::after {
        content: '';
        position: absolute;
        bottom: 0;
        left: 50%;
        transform: translateX(-50%);
        width: 20px;
        height: 3px;
        background: linear-gradient(135deg, #FF69B4 0%, #FFB6C1 100%);
        border-radius: 2px;
      }
    }
  }
}

// 文章列表
.article-feed {
  padding-top: 104px;
  min-height: calc(100vh - 104px);
}

.feed-item {
  background: #fff;
  margin-bottom: 8px;
  padding: 16px;

  &:active {
    background: #fafafa;
  }
}

// 用户信息
.feed-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;

  .feed-user-info {
    flex: 1;

    .username {
      font-size: 15px;
      font-weight: 600;
      color: #333;
    }

    .publish-time {
      font-size: 12px;
      color: #999;
      margin-top: 2px;
    }
  }

  .category-tag {
    font-size: 11px;
    color: #FF69B4;
    border-color: #FF69B4;
  }
}

// 内容
.feed-content {
  margin-bottom: 12px;

  .feed-title {
    font-size: 16px;
    font-weight: 600;
    color: #333;
    line-height: 1.5;
    margin-bottom: 8px;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .feed-summary {
    font-size: 14px;
    color: #666;
    line-height: 1.6;
    display: -webkit-box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
}

// 封面图
.feed-image {
  margin-bottom: 12px;
  border-radius: 8px;
  overflow: hidden;

  img {
    width: 100%;
    height: 180px;
    object-fit: cover;
  }
}

// 标签
.feed-tags {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;

  .tag-item {
    font-size: 13px;
    color: #FF69B4;
  }
}

// 互动栏
.feed-actions {
  display: flex;
  justify-content: space-around;
  padding-top: 12px;
  border-top: 1px solid #f5f5f5;

  .action-item {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 13px;
    color: #666;

    .el-icon {
      color: #999;

      &.liked {
        color: #FF69B4;
      }
    }

    &:active {
      opacity: 0.7;
    }
  }
}

// 空状态
.empty-feed {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  color: #999;

  p {
    margin: 16px 0;
    font-size: 14px;
  }
}

// 加载更多
.load-more {
  text-align: center;
  padding: 20px;
  color: #999;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;

  .loading-icon {
    animation: rotate 1s linear infinite;
  }
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
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

    &:active {
      opacity: 0.7;
    }
  }
}

// 发布按钮
.fab-button {
  position: fixed;
  right: 20px;
  bottom: 80px;
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, #FF69B4 0%, #FFB6C1 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  box-shadow: 0 4px 15px rgba(255, 105, 180, 0.4);
  z-index: 99;

  &:active {
    transform: scale(0.95);
  }
}
</style>
