<template>
  <div class="category-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>分类管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新建分类
          </el-button>
        </div>
      </template>
      
      <el-table :data="tableData" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="分类名称" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="sortOrder" label="排序" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" @click="handleEdit(row)" size="small" round class="water-spirit-btn water-spirit-default" style="margin: 17px 0 0 2px">编辑</el-button>
            <el-button type="danger" @click="handleDelete(row)" size="small" round class="water-spirit-btn water-spirit-danger" style="margin: 17px 0 0 2px">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    
    <!-- 编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑分类' : '新建分类'"
      width="500px"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入分类描述"
          />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCategoryList, createCategory, updateCategory, deleteCategory } from '@/api/category'
import dayjs from 'dayjs'

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref(null)

const form = reactive({
  id: null,
  name: '',
  description: '',
  sortOrder: 0
})

const rules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

const formatDate = (date) => {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm') : '-'
}

const loadData = async () => {
  loading.value = true
  try {
    tableData.value = await getCategoryList()
  } catch (error) {
    console.error('加载分类失败:', error)
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  isEdit.value = false
  Object.assign(form, {
    id: null,
    name: '',
    description: '',
    sortOrder: 0
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    saving.value = true
    try {
      if (isEdit.value) {
        await updateCategory(form.id, form)
      } else {
        await createCategory(form)
      }
      ElMessage.success('保存成功')
      dialogVisible.value = false
      loadData()
    } catch (error) {
      ElMessage.error('保存失败')
    } finally {
      saving.value = false
    }
  })
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除这个分类吗？', '提示', {
      type: 'warning'
    })
    await deleteCategory(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.category-page {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
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
  
  // 皮卡丘风格操作按钮 - 提高优先级
  :deep(.el-table .el-button--small) {
    position: relative !important;
    padding: 6px 14px !important;
    font-size: 13px !important;
    
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

@keyframes headerShine {
  0%, 100% {
    transform: translateX(-100%) rotate(45deg);
  }
  50% {
    transform: translateX(100%) rotate(45deg);
  }
}

@keyframes earWiggle {
  0%, 100% {
    transform: translateY(-50%) rotate(-25deg);
  }
  50% {
    transform: translateY(-50%) rotate(25deg);
  }
}

@keyframes floatShake {
  0%, 100% {
    transform: translateY(-2px) scale(1.05);
  }
  50% {
    transform: translateY(-4px) scale(1.08);
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
</style>
