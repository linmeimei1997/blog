<template>
  <div class="mobile-editor">
    <!-- 顶部导航 -->
    <div class="mobile-header">
      <el-icon size="22" @click="goBack"><ArrowLeft /></el-icon>
      <div class="header-title">{{ isEdit ? '编辑文章' : '写文章' }}</div>
      <el-button type="primary" size="small" round :loading="publishing" @click="showPublish = true">
        发布
      </el-button>
    </div>

    <!-- 编辑器区域 -->
    <div class="editor-container">
      <!-- 标题输入 -->
      <div class="title-input">
        <input
          v-model="article.title"
          type="text"
          placeholder="请输入标题..."
          maxlength="100"
        />
        <span class="title-count">{{ article.title.length }}/100</span>
      </div>

      <!-- 分类和标签 -->
      <div class="meta-bar">
        <div class="meta-item" @click="showCategoryPicker = true">
          <el-icon><Folder /></el-icon>
          <span>{{ selectedCategoryName || '选择分类' }}</span>
          <el-icon><ArrowRight /></el-icon>
        </div>
        <div class="meta-item" @click="showTagPicker = true">
          <el-icon><CollectionTag /></el-icon>
          <span>{{ selectedTags.length > 0 ? selectedTags.join(', ') : '添加标签' }}</span>
          <el-icon><ArrowRight /></el-icon>
        </div>
      </div>

      <!-- 正文编辑 -->
      <div class="content-editor">
        <textarea
          v-model="article.content"
          placeholder="开始写作..."
          @input="handleInput"
        />
        <div class="editor-toolbar">
          <div class="toolbar-item" @click="insertMarkdown('# ')">
            <span>H1</span>
          </div>
          <div class="toolbar-item" @click="insertMarkdown('## ')">
            <span>H2</span>
          </div>
          <div class="toolbar-item" @click="insertMarkdown('**', '**')">
            <span>B</span>
          </div>
          <div class="toolbar-item" @click="insertMarkdown('*', '*')">
            <span>I</span>
          </div>
          <div class="toolbar-item" @click="insertMarkdown('- ')">
            <el-icon><List /></el-icon>
          </div>
          <div class="toolbar-item" @click="insertMarkdown('> ')">
            <el-icon><ChatDotRound /></el-icon>
          </div>
          <div class="toolbar-item" @click="insertMarkdown('```\n', '\n```')">
            <el-icon><Document /></el-icon>
          </div>
          <div class="toolbar-item" @click="showImageUploader = true">
            <el-icon><Picture /></el-icon>
          </div>
        </div>
      </div>

      <!-- 字数统计 -->
      <div class="word-count">
        共 {{ wordCount }} 字
      </div>
    </div>

    <!-- 发布弹窗 -->
    <el-dialog
      v-model="showPublish"
      title="发布文章"
      width="90%"
      class="publish-dialog"
    >
      <div class="publish-options">
        <div class="option-item">
          <span class="label">封面图</span>
          <div class="cover-uploader" @click="uploadCover">
            <img v-if="article.coverImage" :src="article.coverImage" />
            <div v-else class="upload-placeholder">
              <el-icon size="32"><Plus /></el-icon>
              <span>点击上传</span>
            </div>
          </div>
        </div>
        <div class="option-item">
          <span class="label">文章摘要</span>
          <el-input
            v-model="article.summary"
            type="textarea"
            :rows="3"
            placeholder="输入文章摘要，不填则自动提取"
            maxlength="200"
            show-word-limit
          />
        </div>
        <div class="option-item">
          <span class="label">发布状态</span>
          <el-radio-group v-model="article.status">
            <el-radio :label="1">立即发布</el-radio>
            <el-radio :label="0">保存草稿</el-radio>
          </el-radio-group>
        </div>
      </div>
      <template #footer>
        <el-button @click="showPublish = false">取消</el-button>
        <el-button type="primary" :loading="publishing" @click="publish">
          确认{{ article.status === 1 ? '发布' : '保存' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 分类选择器 -->
    <el-drawer
      v-model="showCategoryPicker"
      title="选择分类"
      size="80%"
      direction="btt"
    >
      <div class="picker-list">
        <div
          v-for="cat in categories"
          :key="cat.id"
          class="picker-item"
          :class="{ active: article.categoryId === cat.id }"
          @click="selectCategory(cat)"
        >
          <span>{{ cat.name }}</span>
          <el-icon v-if="article.categoryId === cat.id"><Check /></el-icon>
        </div>
      </div>
      <div class="picker-actions">
        <el-button type="primary" @click="showNewCategory = true">
          新建分类
        </el-button>
      </div>
    </el-drawer>

    <!-- 标签选择器 -->
    <el-drawer
      v-model="showTagPicker"
      title="选择标签"
      size="80%"
      direction="btt"
    >
      <div class="tag-input-area">
        <el-input
          v-model="newTagInput"
          placeholder="输入标签名称，按回车添加"
          @keyup.enter="addNewTag"
        >
          <template #append>
            <el-button @click="addNewTag">添加</el-button>
          </template>
        </el-input>
      </div>
      <div class="tag-cloud">
        <span
          v-for="tag in allTags"
          :key="tag.id"
          class="tag-item"
          :class="{ selected: selectedTags.includes(tag.name) }"
          @click="toggleTag(tag.name)"
        >
          {{ tag.name }}
        </span>
      </div>
    </el-drawer>

    <!-- 新建分类弹窗 -->
    <el-dialog
      v-model="showNewCategory"
      title="新建分类"
      width="90%"
    >
      <el-input
        v-model="newCategoryName"
        placeholder="输入分类名称"
        maxlength="20"
        show-word-limit
      />
      <template #footer>
        <el-button @click="showNewCategory = false">取消</el-button>
        <el-button type="primary" @click="createNewCategory">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, Folder, CollectionTag, ArrowRight,
  List, Document, Picture, Plus, Check, ChatDotRound
} from '@element-plus/icons-vue'
import { createArticle, updateArticle, getArticleById } from '@/api/article'
import { getCategoryList, createCategory } from '@/api/category'
import { getTagList } from '@/api/tag'

const router = useRouter()
const route = useRoute()

// 编辑模式
const isEdit = computed(() => !!route.params.id)
const articleId = computed(() => route.params.id)

// 文章数据
const article = ref({
  title: '',
  content: '',
  summary: '',
  categoryId: null,
  tags: [],
  coverImage: '',
  status: 1
})

// UI 状态
const publishing = ref(false)
const showPublish = ref(false)
const showCategoryPicker = ref(false)
const showTagPicker = ref(false)
const showNewCategory = ref(false)
const showImageUploader = ref(false)

// 数据列表
const categories = ref([])
const allTags = ref([])
const newTagInput = ref('')
const newCategoryName = ref('')

// 计算属性
const selectedCategoryName = computed(() => {
  const cat = categories.value.find(c => c.id === article.value.categoryId)
  return cat?.name
})

const selectedTags = computed(() => article.value.tags)

const wordCount = computed(() => {
  return article.value.content.length
})

// 加载数据
const loadData = async () => {
  try {
    const [cats, tags] = await Promise.all([
      getCategoryList(),
      getTagList()
    ])
    categories.value = cats
    allTags.value = tags

    // 如果是编辑模式，加载文章
    if (isEdit.value) {
      const articleData = await getArticleById(articleId.value)
      article.value = {
        title: articleData.title,
        content: articleData.content,
        summary: articleData.summary || '',
        categoryId: articleData.categoryId,
        tags: articleData.tags || [],
        coverImage: articleData.coverImage || '',
        status: articleData.status
      }
    }
  } catch (error) {
    console.error('加载数据失败:', error)
  }
}

// 返回
const goBack = () => {
  if (article.value.title || article.value.content) {
    ElMessageBox.confirm('文章未保存，确定要离开吗？', '提示', {
      confirmButtonText: '离开',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      router.back()
    }).catch(() => {})
  } else {
    router.back()
  }
}

// 插入 Markdown
const insertMarkdown = (before, after = '') => {
  const textarea = document.querySelector('.content-editor textarea')
  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const text = article.value.content
  const selected = text.substring(start, end)

  article.value.content = text.substring(0, start) + before + selected + after + text.substring(end)

  setTimeout(() => {
    textarea.focus()
    const newCursor = start + before.length + selected.length
    textarea.setSelectionRange(newCursor, newCursor)
  }, 0)
}

// 处理输入
const handleInput = () => {
  // 可以在这里做自动保存
}

// 选择分类
const selectCategory = (cat) => {
  article.value.categoryId = cat.id
  showCategoryPicker.value = false
}

// 创建新分类
const createNewCategory = async () => {
  if (!newCategoryName.value.trim()) {
    ElMessage.warning('请输入分类名称')
    return
  }
  try {
    const res = await createCategory({ name: newCategoryName.value })
    categories.value.push(res)
    article.value.categoryId = res.id
    newCategoryName.value = ''
    showNewCategory.value = false
    showCategoryPicker.value = false
    ElMessage.success('创建成功')
  } catch (error) {
    ElMessage.error('创建失败')
  }
}

// 切换标签
const toggleTag = (tagName) => {
  const index = article.value.tags.indexOf(tagName)
  if (index > -1) {
    article.value.tags.splice(index, 1)
  } else if (article.value.tags.length < 5) {
    article.value.tags.push(tagName)
  } else {
    ElMessage.warning('最多选择5个标签')
  }
}

// 添加新标签
const addNewTag = () => {
  const name = newTagInput.value.trim()
  if (!name) return
  if (article.value.tags.length >= 5) {
    ElMessage.warning('最多选择5个标签')
    return
  }
  if (!article.value.tags.includes(name)) {
    article.value.tags.push(name)
    if (!allTags.value.find(t => t.name === name)) {
      allTags.value.push({ id: Date.now(), name })
    }
  }
  newTagInput.value = ''
}

// 上传封面
const uploadCover = () => {
  ElMessage.info('封面上传功能开发中')
}

// 发布文章
const publish = async () => {
  if (!article.value.title.trim()) {
    ElMessage.warning('请输入标题')
    return
  }
  if (!article.value.content.trim()) {
    ElMessage.warning('请输入正文')
    return
  }

  publishing.value = true
  try {
    if (isEdit.value) {
      await updateArticle(articleId.value, article.value)
      ElMessage.success('更新成功')
    } else {
      await createArticle(article.value)
      ElMessage.success('发布成功')
    }
    router.push('/m/articles')
  } catch (error) {
    console.error('发布失败:', error)
    ElMessage.error('发布失败')
  } finally {
    publishing.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.mobile-editor {
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
  justify-content: space-between;
  padding: 0 16px;
  z-index: 100;
  color: #fff;

  .header-title {
    font-size: 17px;
    font-weight: 600;
  }

  .el-button {
    background: rgba(255, 255, 255, 0.2);
    border-color: rgba(255, 255, 255, 0.3);
    color: #fff;

    &:hover {
      background: rgba(255, 255, 255, 0.3);
    }
  }
}

// 编辑器容器
.editor-container {
  padding-top: 50px;
  padding-bottom: 60px;
}

// 标题输入
.title-input {
  position: relative;
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;

  input {
    width: 100%;
    border: none;
    font-size: 20px;
    font-weight: 600;
    color: #333;
    outline: none;

    &::placeholder {
      color: #ccc;
    }
  }

  .title-count {
    position: absolute;
    right: 16px;
    bottom: 16px;
    font-size: 12px;
    color: #999;
  }
}

// 元信息栏
.meta-bar {
  display: flex;
  border-bottom: 1px solid #f0f0f0;

  .meta-item {
    flex: 1;
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px 16px;
    font-size: 14px;
    color: #666;

    &:first-child {
      border-right: 1px solid #f0f0f0;
    }

    span {
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .el-icon:last-child {
      color: #ccc;
    }
  }
}

// 内容编辑
.content-editor {
  position: relative;

  textarea {
    width: 100%;
    min-height: calc(100vh - 250px);
    padding: 16px;
    border: none;
    font-size: 16px;
    line-height: 1.8;
    color: #333;
    outline: none;
    resize: none;

    &::placeholder {
      color: #ccc;
    }
  }

  .editor-toolbar {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    height: 50px;
    background: #fff;
    border-top: 1px solid #f0f0f0;
    display: flex;
    align-items: center;
    justify-content: space-around;
    padding: 0 8px;

    .toolbar-item {
      width: 40px;
      height: 40px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 8px;
      color: #666;
      font-size: 14px;
      font-weight: 600;

      &:active {
        background: #f5f5f5;
        color: #FF69B4;
      }

      .el-icon {
        font-size: 20px;
      }
    }
  }
}

// 字数统计
.word-count {
  position: fixed;
  right: 16px;
  bottom: 60px;
  font-size: 12px;
  color: #999;
  background: rgba(255, 255, 255, 0.9);
  padding: 4px 12px;
  border-radius: 12px;
}

// 发布弹窗
.publish-dialog {
  :deep(.el-dialog) {
    border-radius: 16px;
  }

  .publish-options {
    .option-item {
      margin-bottom: 20px;

      .label {
        display: block;
        font-size: 14px;
        color: #333;
        margin-bottom: 12px;
        font-weight: 500;
      }

      .cover-uploader {
        width: 100%;
        height: 160px;
        background: #f5f5f5;
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        overflow: hidden;

        img {
          width: 100%;
          height: 100%;
          object-fit: cover;
        }

        .upload-placeholder {
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 8px;
          color: #999;

          span {
            font-size: 14px;
          }
        }
      }
    }
  }
}

// 选择器列表
.picker-list {
  .picker-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px;
    border-bottom: 1px solid #f5f5f5;
    font-size: 15px;
    color: #333;

    &.active {
      color: #FF69B4;
    }
  }
}

.picker-actions {
  padding: 16px;

  .el-button {
    width: 100%;
  }
}

// 标签输入
.tag-input-area {
  padding: 16px;
  border-bottom: 1px solid #f5f5f5;
}

// 标签云
.tag-cloud {
  padding: 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;

  .tag-item {
    padding: 8px 16px;
    background: #f5f5f5;
    border-radius: 20px;
    font-size: 14px;
    color: #666;

    &.selected {
      background: linear-gradient(135deg, #FF69B4 0%, #FFB6C1 100%);
      color: #fff;
    }
  }
}
</style>
