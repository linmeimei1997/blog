<template>
  <div class="article-detail">
    <el-card v-loading="loading">
      <template #header v-if="article">
        <div class="article-header">
          <div>
            <h1 class="article-title">{{ article.title }}</h1>
            <div class="article-meta">
              <span><el-icon><User /></el-icon> {{ article.authorName }}</span>
              <span><el-icon><Folder /></el-icon> {{ article.categoryName }}</span>
              <span><el-icon><View /></el-icon> {{ article.viewCount }} 阅读</span>
              <span><el-icon><Timer /></el-icon> {{ formatDate(article.createTime) }}</span>
            </div>
            <div class="article-tags" v-if="article.tags?.length">
              <el-tag v-for="tag in article.tags" :key="tag.id" size="small" class="tag">
                {{ tag.name }}
              </el-tag>
            </div>
          </div>
          <div class="article-actions">
            <el-button @click="$router.back()">
              <el-icon><Back /></el-icon>
              返回
            </el-button>
            <el-button type="primary" @click="handleEdit">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
          </div>
        </div>
      </template>
      
      <div class="article-summary" v-if="article?.summary">
        <el-alert :title="article.summary" type="info" :closable="false" />
      </div>
      
      <div class="article-content" v-if="article">
        <MdPreview :modelValue="article.content" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { MdPreview } from 'md-editor-v3'
import 'md-editor-v3/lib/preview.css'
import { getArticleById } from '@/api/article'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const article = ref(null)

const formatDate = (date) => {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm') : '-'
}

const loadArticle = async () => {
  loading.value = true
  try {
    const data = await getArticleById(route.params.id)
    article.value = data
  } catch (error) {
    ElMessage.error('加载文章失败')
  } finally {
    loading.value = false
  }
}

const handleEdit = () => {
  router.push(`/articles/edit/${route.params.id}`)
}

onMounted(loadArticle)
</script>

<style scoped lang="scss">
.article-detail {
  .article-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
  }
  
  .article-title {
    font-size: 24px;
    font-weight: 600;
    margin-bottom: 16px;
    color: #303133;
  }
  
  .article-meta {
    display: flex;
    gap: 20px;
    color: #909399;
    font-size: 14px;
    margin-bottom: 12px;
    
    span {
      display: flex;
      align-items: center;
      gap: 4px;
    }
  }
  
  .article-tags {
    .tag {
      margin-right: 8px;
    }
  }
  
  .article-actions {
    display: flex;
    gap: 12px;
  }
  
  .article-summary {
    margin-bottom: 24px;
  }
  
  .article-content {
    line-height: 1.8;
    
    :deep(img) {
      max-width: 100%;
    }
  }
}
</style>
