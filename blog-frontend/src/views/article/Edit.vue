<template>
  <div class="article-edit">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ isEdit ? '编辑文章' : '新建文章' }}</span>
          <div>
            <el-button @click="$router.back()">取消</el-button>
            <el-button type="primary" :loading="saving" @click="handleSave(0)">保存草稿</el-button>
            <el-button type="success" :loading="saving" @click="handleSave(1)">发布文章</el-button>
          </div>
        </div>
      </template>
      
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入文章标题" />
        </el-form-item>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="分类" prop="categoryId">
              <el-select v-model="form.categoryId" placeholder="选择分类" style="width: 100%">
                <el-option
                  v-for="item in categories"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="标签">
              <el-select
                v-model="form.tagIds"
                multiple
                placeholder="选择标签"
                style="width: 100%"
              >
                <el-option
                  v-for="item in tags"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item label="摘要">
          <el-input
            v-model="form.summary"
            type="textarea"
            :rows="3"
            placeholder="请输入文章摘要"
          />
        </el-form-item>
        
        <el-form-item label="内容" prop="content">
          <MdEditor
            v-model="form.content"
            style="height: 500px"
            :toolbarsExclude="['github']"
          />
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { createArticle, updateArticle, getArticleById } from '@/api/article'
import { getCategoryList } from '@/api/category'
import { getTagList } from '@/api/tag'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const saving = ref(false)
const categories = ref([])
const tags = ref([])

const isEdit = computed(() => !!route.params.id)

const form = reactive({
  title: '',
  summary: '',
  content: '',
  categoryId: null,
  tagIds: [],
  status: 0
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}

const loadCategories = async () => {
  try {
    categories.value = await getCategoryList()
  } catch (error) {
    console.error('加载分类失败:', error)
  }
}

const loadTags = async () => {
  try {
    tags.value = await getTagList()
  } catch (error) {
    console.error('加载标签失败:', error)
  }
}

const loadArticle = async () => {
  if (!isEdit.value) return
  
  try {
    const data = await getArticleById(route.params.id)
    Object.assign(form, {
      title: data.title,
      summary: data.summary,
      content: data.content,
      categoryId: data.categoryId,
      tagIds: data.tags?.map(t => t.id) || [],
      status: data.status
    })
  } catch (error) {
    ElMessage.error('加载文章失败')
  }
}

const handleSave = async (status) => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    saving.value = true
    try {
      const data = { ...form, status }
      
      if (isEdit.value) {
        await updateArticle(route.params.id, data)
        ElMessage.success('更新成功')
      } else {
        await createArticle(data)
        ElMessage.success('创建成功')
      }
      
      router.push('/articles')
    } catch (error) {
      ElMessage.error('保存失败')
    } finally {
      saving.value = false
    }
  })
}

onMounted(() => {
  loadCategories()
  loadTags()
  loadArticle()
})
</script>

<style scoped lang="scss">
.article-edit {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}
</style>
