<template>
  <div class="article-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="search-bar">
            <el-input
              v-model="searchForm.keyword"
              placeholder="搜索文章标题"
              clearable
              style="width: 300px"
              @keyup.enter="handleSearch"
            >
              <template #append>
                <el-button @click="handleSearch">
                  <el-icon><Search /></el-icon>
                </el-button>
              </template>
            </el-input>
            
            <el-select v-model="searchForm.categoryId" placeholder="选择分类" clearable style="width: 150px; margin-left: 12px;">
              <el-option
                v-for="item in categories"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
            
            <el-select v-model="searchForm.status" placeholder="文章状态" clearable style="width: 150px; margin-left: 12px;">
              <el-option label="草稿" :value="0" />
              <el-option label="已发布" :value="1" />
            </el-select>
          </div>
          
          <el-button type="primary" @click="$router.push('/articles/create')">
            <el-icon><Plus /></el-icon>
            新建文章
          </el-button>
        </div>
      </template>
      
      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <el-link type="primary" @click="handleView(row)">
              {{ row.title }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="分类" width="120" />
        <el-table-column prop="authorName" label="作者" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="阅读量" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" round @click="handleView(row)" class="water-spirit-btn water-spirit-primary" style="margin: 10px 2px">查看</el-button>
            <el-button type="primary" size="small" round @click="handleEdit(row)" class="water-spirit-btn water-spirit-default" style="margin: 10px 2px">编辑</el-button>
            <el-button type="danger" size="small" round @click="handleDelete(row)" class="water-spirit-btn water-spirit-danger" style="margin: 10px 2px">删除</el-button>
            <el-dropdown split-button type="danger" size="small" @command="(format) => handleExport(row, format)" trigger="click" style="margin: 10px 2px">
              导出
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="word">
                    <el-icon><Document /></el-icon> Word 文档
                  </el-dropdown-item>
                  <el-dropdown-item command="pdf">
                    <el-icon><Document /></el-icon> PDF 文档
                  </el-dropdown-item>
                  <el-dropdown-item command="txt">
                    <el-icon><Document /></el-icon> TXT 文本
                  </el-dropdown-item>
                  <el-dropdown-item command="md">
                    <el-icon><Document /></el-icon> Markdown
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>
      
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, Document } from '@element-plus/icons-vue'
import { getArticleList, deleteArticle, exportArticle } from '@/api/article'
import { getCategoryList } from '@/api/category'
import { useUserStore } from '@/stores/user'
import dayjs from 'dayjs'

const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const categories = ref([])

const searchForm = reactive({
  keyword: '',
  categoryId: null,
  status: null
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const formatDate = (date) => {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm') : '-'
}

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      ...searchForm,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    }
    const res = await getArticleList(params)
    tableData.value = res.list
    pagination.total = res.total
  } catch (error) {
    console.error('加载文章列表失败:', error)
  } finally {
    loading.value = false
  }
}

const loadCategories = async () => {
  try {
    categories.value = await getCategoryList()
  } catch (error) {
    console.error('加载分类失败:', error)
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  loadData()
}

const handleSizeChange = (val) => {
  pagination.pageSize = val
  loadData()
}

const handlePageChange = (val) => {
  pagination.pageNum = val
  loadData()
}

const handleView = (row) => {
  router.push(`/articles/${row.id}`)
}

const handleEdit = (row) => {
  router.push(`/articles/edit/${row.id}`)
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除这篇文章吗？', '提示', {
      type: 'warning'
    })
    await deleteArticle(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 导出文章
const handleExport = async (row, format) => {
  try {
    const url = exportArticle(row.id, format)
    
    // 使用 fetch 下载，携带认证信息
    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${useUserStore().token}`
      }
    })
    
    if (!response.ok) {
      throw new Error('导出失败')
    }
    
    // 获取文件名
    const contentDisposition = response.headers.get('content-disposition')
    let filename = `${row.title}.${format === 'md' ? 'md' : format}`
    if (contentDisposition) {
      const match = contentDisposition.match(/filename\*=UTF-8''(.+)/)
      if (match && match[1]) {
        filename = decodeURIComponent(match[1])
      }
    }
    
    // 下载文件
    const blob = await response.blob()
    const downloadUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = filename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(downloadUrl)
    
    ElMessage.success(`${filename} 导出成功`)
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败：' + error.message)
  }
}

onMounted(() => {
  loadData()
  loadCategories()
})
</script>

<style scoped lang="scss">
.article-list {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .search-bar {
    display: flex;
    align-items: center;
  }
  
  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
  
  // 二次元可爱风格表格
  :deep(.el-table) {
    border-radius: 12px;
    overflow: hidden;
    box-shadow: 0 4px 16px rgba(255, 182, 193, 0.3);
    background: linear-gradient(135deg, #FFF5F7 0%, #FFFFFF 100%);
    
    // 表头
    .el-table__header-wrapper {
      th.el-table__cell {
        background: linear-gradient(135deg, #FFB6C1 0%, #FFC0CB 100%) !important;
        color: #fff;
        font-weight: bold;
        font-size: 14px;
        border: none;
        box-shadow: 0 2px 4px rgba(255, 182, 193, 0.3);
        
        &::before {
          content: '';
          position: absolute;
          top: 0;
          left: 0;
          width: 100%;
          height: 100%;
          background: linear-gradient(
            45deg,
            transparent 30%,
            rgba(255, 255, 255, 0.2) 50%,
            transparent 70%
          );
          animation: headerShine 3s ease-in-out infinite;
        }
      }
    }
    
    // 表格行
    .el-table__body tr {
      transition: all 0.3s ease;
      
      &:hover {
        transform: scale(1.01);
        box-shadow: 0 2px 8px rgba(255, 182, 193, 0.4);
        
        td {
          background: linear-gradient(135deg, #FFE4E9 0%, #FFF0F5 100%) !important;
        }
      }
      
      td.el-table__cell {
        border-color: #FFD1DC;
        color: #555;
        font-size: 13px;
        padding: 12px 0;
        
        // 斑马纹
        &:nth-child(even) {
          background-color: rgba(255, 228, 233, 0.3);
        }
      }
    }
    
    // 空状态
    .el-table__empty-block {
      .el-table__empty-text {
        color: #FFB6C1;
        font-size: 14px;
      }
      
      .el-icon {
        color: #FFB6C1;
      }
    }
  }
  
  // 卡片样式
  :deep(.el-card) {
    border-radius: 16px;
    border: 2px solid #FFD1DC;
    box-shadow: 0 6px 20px rgba(255, 182, 193, 0.4);
    background: linear-gradient(135deg, #FFF5F7 0%, #FFFFFF 100%);
    
    .el-card__header {
      background: linear-gradient(135deg, #FFB6C1 0%, #FFC0CB 100%);
      border-bottom: 2px solid #FF9AA8;
      padding: 18px 20px;
      
      .card-header {
        .search-bar {
          .el-input__wrapper {
            border-radius: 20px;
            box-shadow: 0 2px 8px rgba(255, 182, 193, 0.3);
            
            .el-input__inner {
              font-size: 13px;
            }
          }
          
          .el-button {
            border-radius: 20px;
          }
        }
      }
    }
  }
  
  // 可爱风格按钮
  :deep(.el-button) {
    border-radius: 20px !important;
    font-weight: bold;
    transition: all 0.3s ease;
    
    // 主要按钮 - 粉色渐变
    &[type="primary"] {
      background: linear-gradient(135deg, #FF6B9D 0%, #FF85A2 100%) !important;
      border: none !important;
      box-shadow: 0 3px 10px rgba(255, 107, 157, 0.4);
      
      &:hover {
        background: linear-gradient(135deg, #FF85A2 0%, #FF9FB1 100%) !important;
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(255, 107, 157, 0.6);
      }
      
      &:active {
        transform: translateY(1px);
        box-shadow: 0 2px 6px rgba(255, 107, 157, 0.3);
      }
    }
    
    // 危险按钮 - 红色渐变
    &[type="danger"] {
      background: linear-gradient(135deg, #FF6B6B 0%, #FF8A8A 100%) !important;
      border: none !important;
      box-shadow: 0 3px 10px rgba(255, 107, 107, 0.4);
      
      &:hover {
        background: linear-gradient(135deg, #FF8A8A 0%, #FFA5A5 100%) !important;
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(255, 107, 107, 0.6);
      }
      
      &:active {
        transform: translateY(1px);
        box-shadow: 0 2px 6px rgba(255, 107, 107, 0.3);
      }
    }
    
    // 默认按钮 - 浅蓝渐变
    &:not([type]) {
      background: linear-gradient(135deg, #E3F2FD 0%, #BBDEFB 100%) !important;
      border: 2px solid #90CAF9 !important;
      color: #1976D2 !important;
      box-shadow: 0 2px 6px rgba(144, 202, 249, 0.4);
      
      &:hover {
        background: linear-gradient(135deg, #BBDEFB 0%, #90CAF9 100%) !important;
        transform: translateY(-2px);
        box-shadow: 0 3px 8px rgba(144, 202, 249, 0.6);
      }
      
      &:active {
        transform: translateY(1px);
        box-shadow: 0 1px 4px rgba(144, 202, 249, 0.3);
      }
    }
    
    // Link 按钮
    &[link] {
      border-radius: 8px !important;
      
      &[type="primary"] {
        color: #FF6B9D !important;
        
        &:hover {
          background: rgba(255, 107, 157, 0.1) !important;
          transform: translateY(-1px);
        }
      }
      
      &[type="danger"] {
        color: #FF6B6B !important;
        
        &:hover {
          background: rgba(255, 107, 107, 0.1) !important;
          transform: translateY(-1px);
        }
      }
    }
  }
  
  // 水精灵风格操作按钮 - 提高优先级
  :deep(.el-table .el-button--small) {
    position: relative !important;
    padding: 6px 14px !important;
    font-size: 13px !important;
    border-radius: 20px !important;
    
    &::before,
    &::after {
      content: '' !important;
      position: absolute !important;
      top: -10px !important;
      width: 6px !important;
      height: 12px !important;
      border-radius: 50% !important;
      transform-origin: bottom center !important;
      animation: earWiggle 2s ease-in-out infinite !important;
      z-index: 1 !important;
    }
    
    &::before {
      left: 8px !important;
    }
    
    &::after {
      right: 8px !important;
      animation-direction: reverse !important;
    }
    
    &:hover {
      transform: translateY(-2px) scale(1.05) !important;
      animation: floatShake 0.4s ease-in-out !important;
      
      &::before,
      &::after {
        animation-duration: 0.4s !important;
      }
    }
    
    &:active {
      transform: translateY(1px) scale(0.98) !important;
    }
    
    // 查看按钮 - 蓝色水精灵
    &[type="primary"] {
      background: linear-gradient(135deg, #4FC3F7 0%, #29B6F6 100%) !important;
      border: 2px solid #03A9F4 !important;
      color: white !important;
      box-shadow: 0 3px 10px rgba(79, 195, 247, 0.4) !important;
      
      // 测试用：强制显示背景色
      &:not([disabled]) {
        background: linear-gradient(135deg, #4FC3F7 0%, #29B6F6 100%) !important;
      }
      
      &::before,
      &::after {
        background: linear-gradient(135deg, #4FC3F7 0%, #29B6F6 100%) !important;
        border: 2px solid #03A9F4 !important;
      }
      
      // 水精灵高光效果
      &::after {
        background: linear-gradient(135deg, #81D4FA 0%, #4FC3F7 100%) !important;
        border-color: #0288D1 !important;
      }
      
      &:hover {
        background: linear-gradient(135deg, #29B6F6 0%, #03A9F4 100%) !important;
        box-shadow: 0 5px 15px rgba(79, 195, 247, 0.6) !important;
      }
    }
    
    // 编辑按钮 - 黄色水精灵
    &:not([type]) {
      background: linear-gradient(135deg, #FFF176 0%, #FFEE58 100%) !important;
      border: 2px solid #FDD835 !important;
      color: #5D4037 !important;
      box-shadow: 0 3px 10px rgba(255, 241, 118, 0.5) !important;
      
      &::before,
      &::after {
        background: linear-gradient(135deg, #FFF176 0%, #FFEE58 100%) !important;
        border: 2px solid #FDD835 !important;
      }
      
      // 水精灵高光效果
      &::after {
        background: linear-gradient(135deg, #FFF59D 0%, #FFF176 100%) !important;
        border-color: #FBC02D !important;
      }
      
      &:hover {
        background: linear-gradient(135deg, #FFEE58 0%, #FDD835 100%) !important;
        box-shadow: 0 5px 15px rgba(255, 241, 118, 0.7) !important;
      }
    }
    
    // 删除按钮 - 红色水精灵
    &[type="danger"] {
      background: linear-gradient(135deg, #FF8A80 0%, #FF5252 100%) !important;
      border: 2px solid #F44336 !important;
      color: white !important;
      box-shadow: 0 3px 10px rgba(255, 138, 128, 0.4) !important;
      
      &::before,
      &::after {
        background: linear-gradient(135deg, #FF8A80 0%, #FF5252 100%) !important;
        border: 2px solid #F44336 !important;
      }
      
      // 水精灵高光效果
      &::after {
        background: linear-gradient(135deg, #FFCDD2 0%, #FF8A80 100%) !important;
        border-color: #D32F2F !important;
      }
      
      &:hover {
        background: linear-gradient(135deg, #FF5252 0%, #F44336 100%) !important;
        box-shadow: 0 5px 15px rgba(255, 138, 128, 0.6) !important;
      }
    }
  }
}
  // 分页器样式
  :deep(.el-pagination) {
    .el-pager li {
      background: #FFF5F7;
      border-radius: 8px;
      
      &.is-active {
        background: linear-gradient(135deg, #FF6B9D 0%, #FF85A2 100%);
      }
      
      &:hover {
        background: #FFE4E9;
      }
    }
    
    .btn-prev,
    .btn-next {
      background: #FFF5F7;
      border-radius: 8px;
      
      &:hover {
        background: #FFE4E9;
      }
    }
  }
  
  // 皮卡丘按钮样式
  :deep(.el-dropdown) {
    .el-button {
      background: linear-gradient(135deg, #FFD700 0%, #FFC107 100%) !important;
      border: 2px solid #FFA000 !important;
      color: #3E2723 !important;
      font-weight: bold;
      font-size: 14px;
      position: relative;
      transition: all 0.3s ease;
      box-shadow: 0 3px 6px rgba(255, 193, 7, 0.4);
      padding: 8px 16px;
      
      // 耳朵 - 左耳
      &::before {
        content: '';
        position: absolute;
        top: -12px;
        left: 8px;
        width: 8px;
        height: 16px;
        background: linear-gradient(135deg, #FFD700 0%, #FFC107 100%);
        border: 2px solid #FFA000;
        border-radius: 50%;
        transform-origin: bottom center;
        animation: earWiggle 2s ease-in-out infinite;
        z-index: 1;
      }
      
      // 耳朵 - 右耳（黑色尖端）
      &::after {
        content: '';
        position: absolute;
        top: -12px;
        right: 8px;
        width: 8px;
        height: 16px;
        background: linear-gradient(135deg, #FFD700 0%, #FFC107 100%);
        border: 2px solid #FFA000;
        border-radius: 50%;
        transform-origin: bottom center;
        animation: earWiggle 2s ease-in-out infinite reverse;
        z-index: 1;
      }
      
      &:hover {
        transform: translateY(-2px) scale(1.05);
        box-shadow: 0 5px 10px rgba(255, 193, 7, 0.6);
        animation: floatShake 0.4s ease-in-out;
        
        &::before,
        &::after {
          animation-duration: 0.4s;
        }
      }
      
      &:active {
        transform: translateY(1px) scale(0.98);
        box-shadow: 0 2px 4px rgba(255, 193, 7, 0.3);
      }
    }
    
    // 下拉箭头按钮保持红色
    .el-button--danger.el-dropdown__caret-button {
      background: linear-gradient(135deg, #FF6B6B 0%, #FF5252 100%) !important;
      border-color: #D32F2F !important;
      color: white !important;
      
      &:hover {
        background: linear-gradient(135deg, #FF8A80 0%, #FF6B6B 100%) !important;
      }
    }
  }
  
  // 皮卡丘动画关键帧
@keyframes earWiggle {
  0%, 100% {
    transform: rotate(-5deg);
  }
  50% {
    transform: rotate(5deg);
  }
}

@keyframes floatShake {
  0%, 100% {
    transform: translateY(-3px) rotate(0deg);
  }
  25% {
    transform: translateY(-3px) rotate(-2deg);
  }
  75% {
    transform: translateY(-3px) rotate(2deg);
  }
}

@keyframes headerShine {
  0%, 100% {
    transform: translateX(-100%) rotate(45deg);
  }
  50% {
    transform: translateX(100%) rotate(45deg);
  }
}

// 腮红呼吸效果
.pikachu-blush {
  position: absolute;
  width: 8px;
  height: 8px;
  background: radial-gradient(circle, rgba(255, 107, 107, 0.6) 0%, transparent 70%);
  border-radius: 50%;
  top: 50%;
  transform: translateY(-50%);
  animation: blushBreath 2s ease-in-out infinite;
  
  &.left {
    left: 5px;
  }
  
  &.right {
    right: 5px;
  }
}

@keyframes blushBreath {
  0%, 100% {
    opacity: 0.4;
    transform: translateY(-50%) scale(1);
  }
  50% {
    opacity: 0.8;
    transform: translateY(-50%) scale(1.2);
  }
}

// 尾巴摇摆效果
.pikachu-tail {
  position: absolute;
  right: -15px;
  top: 50%;
  width: 20px;
  height: 8px;
  background: linear-gradient(90deg, #FFD700 0%, #FFC107 100%);
  transform: translateY(-50%) rotate(-30deg);
  transform-origin: left center;
  animation: tailWag 1.5s ease-in-out infinite;
  border-radius: 0 4px 4px 0;
  
  &::before {
    content: '';
    position: absolute;
    right: -5px;
    top: 0;
    width: 8px;
    height: 8px;
    background: #FFC107;
    border-radius: 50%;
  }
}

@keyframes tailWag {
  0%, 100% {
    transform: translateY(-50%) rotate(-30deg);
  }
  50% {
    transform: translateY(-50%) rotate(30deg);
  }
}
</style>

<style lang="scss">
// 全局水精灵按钮样式
.el-table .el-button--small {
  position: relative !important;
  border-radius: 20px !important;
  
  // 添加耳朵伪元素
  &::before,
  &::after {
    content: '' !important;
    position: absolute !important;
    top: -10px !important;
    width: 6px !important;
    height: 12px !important;
    border-radius: 50% !important;
    transform-origin: bottom center !important;
    animation: earWiggle 2s ease-in-out infinite !important;
    z-index: 1 !important;
  }
  
  &::before {
    left: 8px !important;
  }
  
  &::after {
    right: 8px !important;
    animation-direction: reverse !important;
  }
  
  &:hover {
    transform: translateY(-2px) scale(1.05) !important;
    animation: floatShake 0.4s ease-in-out !important;
    
    &::before,
    &::after {
      animation-duration: 0.4s !important;
    }
  }
  
  &:active {
    transform: translateY(1px) scale(0.98) !important;
  }
}

// 查看按钮 - 蓝色水精灵
.el-table .el-button--small[type="primary"] {
  background: linear-gradient(135deg, #4FC3F7 0%, #29B6F6 100%) !important;
  border: 2px solid #03A9F4 !important;
  color: white !important;
  box-shadow: 0 3px 10px rgba(79, 195, 247, 0.4) !important;
  
  &::before,
  &::after {
    background: linear-gradient(135deg, #4FC3F7 0%, #29B6F6 100%) !important;
    border: 2px solid #03A9F4 !important;
  }
  
  // 水精灵高光效果
  &::after {
    background: linear-gradient(135deg, #81D4FA 0%, #4FC3F7 100%) !important;
    border-color: #0288D1 !important;
  }
  
  &:hover {
    background: linear-gradient(135deg, #29B6F6 0%, #03A9F4 100%) !important;
    box-shadow: 0 5px 15px rgba(79, 195, 247, 0.6) !important;
  }
}

// 编辑按钮 - 黄色水精灵
.el-table .el-button--small:not([type]) {
  background: linear-gradient(135deg, #FFF176 0%, #FFEE58 100%) !important;
  border: 2px solid #FDD835 !important;
  color: #5D4037 !important;
  box-shadow: 0 3px 10px rgba(255, 241, 118, 0.5) !important;
  
  &::before,
  &::after {
    background: linear-gradient(135deg, #FFF176 0%, #FFEE58 100%) !important;
    border: 2px solid #FDD835 !important;
  }
  
  // 水精灵高光效果
  &::after {
    background: linear-gradient(135deg, #FFF59D 0%, #FFF176 100%) !important;
    border-color: #FBC02D !important;
  }
  
  &:hover {
    background: linear-gradient(135deg, #FFEE58 0%, #FDD835 100%) !important;
    box-shadow: 0 5px 15px rgba(255, 241, 118, 0.7) !important;
  }
}

// 删除按钮 - 红色水精灵
.el-table .el-button--small[type="danger"] {
  background: linear-gradient(135deg, #FF8A80 0%, #FF5252 100%) !important;
  border: 2px solid #F44336 !important;
  color: white !important;
  box-shadow: 0 3px 10px rgba(255, 138, 128, 0.4) !important;
  
  &::before,
  &::after {
    background: linear-gradient(135deg, #FF8A80 0%, #FF5252 100%) !important;
    border: 2px solid #F44336 !important;
  }
  
  // 水精灵高光效果
  &::after {
    background: linear-gradient(135deg, #FFCDD2 0%, #FF8A80 100%) !important;
    border-color: #D32F2F !important;
  }
  
  &:hover {
    background: linear-gradient(135deg, #FF5252 0%, #F44336 100%) !important;
    box-shadow: 0 5px 15px rgba(255, 138, 128, 0.6) !important;
  }
}
</style>

<style lang="scss">
// 水精灵按钮 - 使用类名直接应用
.water-spirit-btn {
  position: relative !important;
  border-radius: 20px !important;
  padding: 6px 12px !important;
  font-size: 13px !important;
  line-height: 1 !important;
  
  // 耳朵伪元素
  &::before,
  &::after {
    content: '' !important;
    position: absolute !important;
    top: -10px !important;
    width: 6px !important;
    height: 12px !important;
    border-radius: 50% !important;
    transform-origin: bottom center !important;
    animation: earWiggle 2s ease-in-out infinite !important;
    z-index: 1 !important;
  }
  
  &::before {
    left: 8px !important;
  }
  
  &::after {
    right: 8px !important;
    animation-direction: reverse !important;
  }
  
  &:hover {
    transform: translateY(-2px) scale(1.05) !important;
    animation: floatShake 0.4s ease-in-out !important;
    
    &::before,
    &::after {
      animation-duration: 0.4s !important;
    }
  }
  
  &:active {
    transform: translateY(1px) scale(0.98) !important;
  }
}

// 蓝色水精灵 - 查看按钮
.water-spirit-primary {
  background: linear-gradient(135deg, #4FC3F7 0%, #29B6F6 100%) !important;
  border: 2px solid #03A9F4 !important;
  color: white !important;
  box-shadow: 0 3px 10px rgba(79, 195, 247, 0.4) !important;
  
  &::before,
  &::after {
    background: linear-gradient(135deg, #4FC3F7 0%, #29B6F6 100%) !important;
    border: 2px solid #03A9F4 !important;
  }
  
  // 高光效果
  &::after {
    background: linear-gradient(135deg, #81D4FA 0%, #4FC3F7 100%) !important;
    border-color: #0288D1 !important;
  }
  
  &:hover {
    background: linear-gradient(135deg, #29B6F6 0%, #03A9F4 100%) !important;
    box-shadow: 0 5px 15px rgba(79, 195, 247, 0.6) !important;
  }
}

// 黄色水精灵 - 编辑按钮
.water-spirit-default {
  background: linear-gradient(135deg, #FFF176 0%, #FFEE58 100%) !important;
  border: 2px solid #FDD835 !important;
  color: #5D4037 !important;
  box-shadow: 0 3px 10px rgba(255, 241, 118, 0.5) !important;
  
  &::before,
  &::after {
    background: linear-gradient(135deg, #FFF176 0%, #FFEE58 100%) !important;
    border: 2px solid #FDD835 !important;
  }
  
  // 高光效果
  &::after {
    background: linear-gradient(135deg, #FFF59D 0%, #FFF176 100%) !important;
    border-color: #FBC02D !important;
  }
  
  &:hover {
    background: linear-gradient(135deg, #FFEE58 0%, #FDD835 100%) !important;
    box-shadow: 0 5px 15px rgba(255, 241, 118, 0.7) !important;
  }
}

// 红色水精灵（火精灵） - 删除按钮
.water-spirit-danger {
  background: linear-gradient(135deg, #FF8A80 0%, #FF5252 100%) !important;
  border: 2px solid #F44336 !important;
  color: white !important;
  box-shadow: 0 3px 10px rgba(255, 138, 128, 0.4) !important;
  
  &::before,
  &::after {
    background: linear-gradient(135deg, #FF8A80 0%, #FF5252 100%) !important;
    border: 2px solid #F44336 !important;
  }
  
  // 高光效果
  &::after {
    background: linear-gradient(135deg, #FFCDD2 0%, #FF8A80 100%) !important;
    border-color: #D32F2F !important;
  }
  
  &:hover {
    background: linear-gradient(135deg, #FF5252 0%, #F44336 100%) !important;
    box-shadow: 0 5px 15px rgba(255, 138, 128, 0.6) !important;
  }
}

// 下拉按钮 - 添加黄色耳朵和尾巴效果（增强优先级）
.el-dropdown .el-button--small,
body .el-dropdown .el-button--small {
  position: relative !important;
  
  // 左耳朵（黄色）
  &::before {
    content: '' !important;
    position: absolute !important;
    top: -10px !important;
    left: 8px !important;
    width: 6px !important;
    height: 12px !important;
    border-radius: 50% !important;
    transform-origin: bottom center !important;
    animation: earWiggle 2s ease-in-out infinite !important;
    z-index: 10 !important;
    background: linear-gradient(135deg, #FFF176 0%, #FFEE58 100%) !important;
    border: 2px solid #FDD835 !important;
  }
  
  // 尾巴（右侧单耳，黄色）
  &::after {
    content: '' !important;
    position: absolute !important;
    top: -8px !important;
    right: 10px !important;
    width: 8px !important;
    height: 14px !important;
    border-radius: 50% !important;
    transform-origin: bottom center !important;
    animation: tailWiggle 2s ease-in-out infinite !important;
    z-index: 10 !important;
    background: linear-gradient(135deg, #FFF176 0%, #FFEE58 100%) !important;
    border: 2px solid #FDD835 !important;
  }
  
  &:hover {
    transform: translateY(-2px) scale(1.05) !important;
    
    &::before,
    &::after {
      animation-duration: 0.4s !important;
    }
  }
}
</style>
