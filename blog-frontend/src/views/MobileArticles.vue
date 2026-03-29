<template>
  <div class="mobile-articles">
    <!-- 顶部导航 -->
    <div class="mobile-header">
      <el-icon size="22" @click="$router.back()"><ArrowLeft /></el-icon>
      <div class="header-title">文章</div>
      <el-icon size="22" @click="showSearch = true"><Search /></el-icon>
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

    <!-- 分类筛选 -->
    <div class="filter-bar">
      <div class="filter-scroll">
        <div
          v-for="cat in categories"
          :key="cat.id"
          class="filter-item"
          :class="{ active: activeCategory === cat.id }"
          @click="activeCategory = cat.id"
        >
          {{ cat.name }}
        </div>
      </div>
    </div>

    <!-- 文章列表 -->
    <div class="article-list" ref="listContainer" @scroll="handleScroll">
      <div
        v-for="article in articleList"
        :key="article.id"
        class="article-card"
        @click="goToDetail(article.id)"
      >
        <div class="card-content">
          <h3 class="article-title">{{ article.title }}</h3>
          <p class="article-summary">{{ article.summary || stripHtml(article.content) }}</p>
          <div class="article-meta">
            <span class="meta-item">
              <el-icon><Calendar /></el-icon>
              {{ formatDate(article.createTime) }}
            </span>
            <span class="meta-item">
              <el-icon><View /></el-icon>
              {{ article.viewCount || 0 }}
            </span>
            <el-tag size="small" :type="article.status === 1 ? 'success' : 'info'">
              {{ article.status === 1 ? '已发布' : '草稿' }}
            </el-tag>
          </div>
        </div>
        <div v-if="article.coverImage" class="card-image">
          <img :src="article.coverImage" alt="封面" />
        </div>
      </div>

      <!-- 加载状态 -->
      <div class="load-more" v-if="loading">
        <el-icon class="loading-icon"><Loading /></el-icon>
        <span>加载中...</span>
      </div>
      <div class="load-more" v-else-if="noMore">
        <span>没有更多了</span>
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
  ArrowLeft, Search, Calendar, View, Plus, Loading,
  HomeFilled, Document, Folder, ChatLineRound
} from '@element-plus/icons-vue'
import { getArticleList } from '@/api/article'
import { getCategoryList } from '@/api/category'
import dayjs from 'dayjs'

const router = useRouter()

// 搜索
const showSearch = ref(false)
const searchKeyword = ref('')

// 分类
const categories = ref([{ id: 0, name: '全部' }])
const activeCategory = ref(0)

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

// 格式化日期
const formatDate = (date) => {
  return date ? dayjs(date).format('MM-DD') : '-'
}

// 去除 HTML
const stripHtml = (html) => {
  if (!html) return ''
  return html.replace(/<[^>]+>/g, '').substring(0, 80) + '...'
}

// 加载分类
const loadCategories = async () => {
  try {
    const res = await getCategoryList()
    categories.value = [{ id: 0, name: '全部' }, ...res]
  } catch (error) {
    console.error('加载分类失败:', error)
  }
}

// 加载文章
const loadArticles = async (reset = false) => {
  if (loading.value) return
  loading.value = true

  try {
    if (reset) {
      pageNum.value = 1
      articleList.value = []
      noMore.value = false
    }

    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize
    }
    if (activeCategory.value > 0) {
      params.categoryId = activeCategory.value
    }

    const res = await getArticleList(params)
    if (res && res.list && res.list.length > 0) {
      articleList.value.push(...res.list)
      pageNum.value++
      if (res.list.length < pageSize) {
        noMore.value = true
      }
    } else {
      noMore.value = true
    }
  } catch (error) {
    console.error('加载文章失败:', error)
    ElMessage.error('加载失败')
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

// 监听分类切换
watch(activeCategory, () => {
  loadArticles(true)
})

onMounted(() => {
  loadCategories()
  loadArticles(true)
})
</script>

<style scoped lang="scss">
.mobile-articles {
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

// 分类筛选
.filter-bar {
  position: fixed;
  top: 50px;
  left: 0;
  right: 0;
  height: 48px;
  background: #fff;
  z-index: 99;
  border-bottom: 1px solid #f0f0f0;
  overflow-x: auto;

  &::-webkit-scrollbar {
    display: none;
  }

  .filter-scroll {
    display: flex;
    gap: 8px;
    padding: 8px 16px;
    white-space: nowrap;
  }

  .filter-item {
    padding: 6px 16px;
    border-radius: 16px;
    font-size: 13px;
    color: #666;
    background: #f5f5f5;

    &.active {
      background: linear-gradient(135deg, #FF69B4 0%, #FFB6C1 100%);
      color: #fff;
    }
  }
}

// 文章列表
.article-list {
  padding-top: 106px;
  min-height: calc(100vh - 106px);
}

.article-card {
  background: #fff;
  margin-bottom: 8px;
  padding: 16px;
  display: flex;
  gap: 12px;

  &:active {
    background: #fafafa;
  }

  .card-content {
    flex: 1;
    min-width: 0;

    .article-title {
      font-size: 15px;
      font-weight: 600;
      color: #333;
      line-height: 1.4;
      margin-bottom: 8px;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .article-summary {
      font-size: 13px;
      color: #666;
      line-height: 1.5;
      margin-bottom: 10px;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .article-meta {
      display: flex;
      align-items: center;
      gap: 12px;

      .meta-item {
        display: flex;
        align-items: center;
        gap: 4px;
        font-size: 12px;
        color: #999;

        .el-icon {
          font-size: 13px;
        }
      }
    }
  }

  .card-image {
    width: 80px;
    height: 80px;
    border-radius: 8px;
    overflow: hidden;
    flex-shrink: 0;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
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
