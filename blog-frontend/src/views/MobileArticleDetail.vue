<template>
  <div class="mobile-article-detail">
    <!-- 顶部导航 -->
    <div class="mobile-header">
      <div class="header-title">文章详情</div>
      <div class="header-actions" v-if="isAuthor">
        <el-button type="primary" size="small" round @click="goEdit">
          <el-icon size="14"><Edit /></el-icon>
          编辑
        </el-button>
      </div>
    </div>

    <!-- 文章内容 -->
    <div class="article-content">
      <!-- 封面图 -->
      <div v-if="article.coverImage" class="article-cover">
        <img :src="article.coverImage" alt="封面" />
      </div>

      <!-- 标题区 -->
      <div class="article-header">
        <h1 class="article-title">{{ article.title }}</h1>
        <div class="article-meta">
          <div class="author-info">
            <el-avatar :size="36" :src="article.authorAvatar || defaultAvatar" />
            <div class="author-text">
              <span class="author-name">{{ article.authorName || '博主' }}</span>
              <span class="publish-time">{{ formatTime(article.createTime) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 正文 -->
      <div class="article-body" v-html="renderedContent"></div>

      <!-- 标签 -->
      <div v-if="article.tags && article.tags.length" class="article-tags">
        <span v-for="tag in article.tags" :key="tag.id" class="tag-item">
          #{{ tag.name }}
        </span>
      </div>

      <!-- 底部占位 -->
      <div class="bottom-placeholder"></div>
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
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Edit, Delete, HomeFilled, Document, Folder, ChatDotRound
} from '@element-plus/icons-vue'
import { getArticleById, deleteArticle } from '@/api/article'
import { useUserStore } from '@/stores/user'
import MarkdownIt from 'markdown-it'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const articleId = computed(() => route.params.id)
const defaultAvatar = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'

// Markdown 渲染器
const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true
})

// 自定义图片渲染规则 - 确保图片路径正确
md.renderer.rules.image = function(tokens, idx, options, env, self) {
  const token = tokens[idx]
  const srcIndex = token.attrIndex('src')
  let src = ''
  if (srcIndex >= 0) {
    src = token.attrs[srcIndex][1]
    // 如果是相对路径（以 / 开头），确保能正确访问
    if (src.startsWith('/') && !src.startsWith('/api')) {
      // /uploads/images/xxx.jpg 路径由 vite 代理处理，无需修改
    }
  }
  const altIndex = token.attrIndex('alt')
  const alt = altIndex >= 0 ? token.attrs[altIndex][1] : ''
  
  // 渲染带样式的图片
  return `<img src="${src}" alt="${alt}" style="max-width:100%;border-radius:8px;margin:16px 0;display:block;" />`
}

// 文章数据
const article = ref({})

// UI 状态
const tabbarItems = [
  { path: '/m/home', label: '首页', icon: 'HomeFilled' },
  { path: '/m/articles', label: '文章', icon: 'Document' },
  { path: '/m/ai', label: 'AI助手', icon: 'ChatDotRound' },
  { path: '/m/categories', label: '分类', icon: 'Folder' }
]

// 计算属性
const isAuthor = computed(() => {
  return article.value.authorId === userStore.userId
})

const renderedContent = computed(() => {
  return md.render(article.value.content || '')
})

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  return dayjs(time).fromNow()
}

// 加载文章
const loadArticle = async () => {
  try {
    const res = await getArticleById(articleId.value)
    article.value = res
  } catch (error) {
    console.error('加载文章失败:', error)
    ElMessage.error('文章加载失败')
  }
}

// 编辑文章
const goEdit = () => {
  router.push(`/m/editor/${articleId.value}`)
}

// 删除文章
const handleDelete = () => {
  ElMessageBox.confirm('确定要删除这篇文章吗？', '提示', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteArticle(articleId.value)
      ElMessage.success('删除成功')
      router.back()
    } catch (error) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

onMounted(() => {
  loadArticle()
})
</script>

<style scoped lang="scss">
.mobile-article-detail {
  min-height: 100vh;
  background: #fff;
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

    .el-button {
      background: rgba(255, 255, 255, 0.2);
      border-color: rgba(255, 255, 255, 0.3);
      color: #fff;

      &:hover {
        background: rgba(255, 255, 255, 0.3);
      }
    }
  }
}

// 文章内容
.article-content {
  padding-top: 50px;
  min-height: 100vh;
  padding-bottom: 80px;
}

// 封面图
.article-cover {
  width: 100%;
  height: 240px;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

// 文章头部
.article-header {
  padding: 20px 16px;

  .article-title {
    font-size: 24px;
    font-weight: 700;
    color: #333;
    line-height: 1.4;
    margin-bottom: 16px;
  }

  .article-meta {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .author-info {
      display: flex;
      align-items: center;
      gap: 10px;

      .author-text {
        display: flex;
        flex-direction: column;

        .author-name {
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
    }
  }
}

// 文章正文
.article-body {
  padding: 0 16px 20px;
  font-size: 16px;
  line-height: 1.8;
  color: #333;

  :deep(h1) {
    font-size: 22px;
    margin: 24px 0 16px;
  }

  :deep(h2) {
    font-size: 20px;
    margin: 20px 0 12px;
  }

  :deep(h3) {
    font-size: 18px;
    margin: 16px 0 10px;
  }

  :deep(p) {
    margin-bottom: 16px;
  }

  :deep(img) {
    max-width: 100%;
    border-radius: 8px;
    margin: 16px 0;
  }

  :deep(code) {
    background: #f5f5f5;
    padding: 2px 6px;
    border-radius: 4px;
    font-family: monospace;
  }

  :deep(pre) {
    background: #f8f8f8;
    padding: 16px;
    border-radius: 8px;
    overflow-x: auto;
    margin: 16px 0;

    code {
      background: none;
      padding: 0;
    }
  }

  :deep(blockquote) {
    border-left: 4px solid #FF69B4;
    padding-left: 16px;
    margin: 16px 0;
    color: #666;
  }

  :deep(ul), :deep(ol) {
    padding-left: 24px;
    margin-bottom: 16px;
  }

  :deep(li) {
    margin-bottom: 8px;
  }
}

// 标签
.article-tags {
  padding: 0 16px 20px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;

  .tag-item {
    font-size: 14px;
    color: #FF69B4;
  }
}

.bottom-placeholder {
  height: 20px;
}

// 底部导航
.mobile-tabbar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 60px;
  background: #fff;
  border-top: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-around;
  z-index: 99;

  .tabbar-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 4px;
    color: #999;
    font-size: 10px;
    padding: 6px 0;
    flex: 1;

    &.active {
      color: #FF69B4;
    }

    .el-icon {
      font-size: 22px;
    }

    span {
      font-size: 12px;
    }
  }
}
</style>
