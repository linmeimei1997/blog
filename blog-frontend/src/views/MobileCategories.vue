<template>
  <div class="mobile-categories">
    <!-- 顶部导航 -->
    <div class="mobile-header">
      <el-icon size="22" @click="$router.back()"><ArrowLeft /></el-icon>
      <div class="header-title">分类</div>
      <el-icon size="22" @click="showAdd = true"><Plus /></el-icon>
    </div>

    <!-- 分类网格 -->
    <div class="category-grid">
      <div
        v-for="category in categories"
        :key="category.id"
        class="category-card"
        :style="{ background: getGradient(category.id) }"
        @click="goToCategory(category.id)"
      >
        <div class="category-icon">
          <el-icon size="32" color="#fff"><Folder /></el-icon>
        </div>
        <div class="category-info">
          <h3>{{ category.name }}</h3>
          <p>{{ category.articleCount || 0 }} 篇文章</p>
        </div>
      </div>
    </div>

    <!-- 标签云 -->
    <div class="tags-section">
      <h3 class="section-title">热门标签</h3>
      <div class="tag-cloud">
        <span
          v-for="tag in tags"
          :key="tag.id"
          class="tag-item"
          :style="{ fontSize: getTagSize(tag.articleCount), opacity: getTagOpacity(tag.articleCount) }"
          @click="goToTag(tag.id)"
        >
          {{ tag.name }}
        </span>
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

    <!-- 添加分类弹窗 -->
    <el-dialog
      v-model="showAdd"
      title="新建分类"
      width="90%"
      class="add-dialog"
    >
      <el-input
        v-model="newCategoryName"
        placeholder="输入分类名称"
        maxlength="20"
        show-word-limit
      />
      <template #footer>
        <el-button @click="showAdd = false">取消</el-button>
        <el-button type="primary" @click="addCategory">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft, Plus, Folder,
  HomeFilled, Document, ChatLineRound
} from '@element-plus/icons-vue'
import { getCategoryList, createCategory } from '@/api/category'
import { getTagList } from '@/api/tag'

const router = useRouter()

// 添加分类
const showAdd = ref(false)
const newCategoryName = ref('')

// 底部导航
const tabbarItems = [
  { path: '/m/home', label: '首页', icon: 'HomeFilled' },
  { path: '/m/articles', label: '文章', icon: 'Document' },
  { path: '/m/ai', label: 'AI助手', icon: 'ChatDotRound' },
  { path: '/m/categories', label: '分类', icon: 'Folder' }
]

// 分类和标签
const categories = ref([])
const tags = ref([])

// 渐变色数组
const gradients = [
  'linear-gradient(135deg, #FF69B4 0%, #FFB6C1 100%)',
  'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
  'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
  'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
  'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
  'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
  'linear-gradient(135deg, #30cfd0 0%, #330867 100%)',
  'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)'
]

const getGradient = (id) => {
  return gradients[id % gradients.length]
}

const getTagSize = (count) => {
  const size = 12 + (count || 0) * 0.5
  return Math.min(Math.max(size, 12), 20) + 'px'
}

const getTagOpacity = (count) => {
  const opacity = 0.5 + (count || 0) * 0.05
  return Math.min(Math.max(opacity, 0.5), 1)
}

// 加载数据
const loadData = async () => {
  try {
    const [catRes, tagRes] = await Promise.all([
      getCategoryList(),
      getTagList()
    ])
    categories.value = catRes
    tags.value = tagRes.sort((a, b) => (b.articleCount || 0) - (a.articleCount || 0))
  } catch (error) {
    console.error('加载数据失败:', error)
  }
}

// 添加分类
const addCategory = async () => {
  if (!newCategoryName.value.trim()) {
    ElMessage.warning('请输入分类名称')
    return
  }
  try {
    await createCategory({ name: newCategoryName.value })
    ElMessage.success('创建成功')
    showAdd.value = false
    newCategoryName.value = ''
    loadData()
  } catch (error) {
    ElMessage.error('创建失败')
  }
}

// 跳转
const goToCategory = (id) => {
  router.push(`/m/articles?category=${id}`)
}

const goToTag = (id) => {
  router.push(`/m/articles?tag=${id}`)
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.mobile-categories {
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

// 分类网格
.category-grid {
  padding: 66px 12px 12px;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.category-card {
  border-radius: 16px;
  padding: 20px;
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 10px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);

  &:active {
    transform: scale(0.98);
  }

  .category-icon {
    width: 56px;
    height: 56px;
    background: rgba(255, 255, 255, 0.2);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    backdrop-filter: blur(10px);
  }

  .category-info {
    h3 {
      font-size: 15px;
      font-weight: 600;
      margin-bottom: 4px;
    }

    p {
      font-size: 12px;
      opacity: 0.9;
    }
  }
}

// 标签区域
.tags-section {
  background: #fff;
  margin: 12px;
  border-radius: 16px;
  padding: 20px;

  .section-title {
    font-size: 16px;
    font-weight: 600;
    color: #333;
    margin-bottom: 16px;
  }
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;

  .tag-item {
    padding: 8px 16px;
    background: #f5f5f5;
    border-radius: 20px;
    color: #666;
    transition: all 0.3s;

    &:active {
      background: linear-gradient(135deg, #FF69B4 0%, #FFB6C1 100%);
      color: #fff;
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

// 弹窗样式
.add-dialog {
  :deep(.el-dialog) {
    border-radius: 16px;
  }
}
</style>
