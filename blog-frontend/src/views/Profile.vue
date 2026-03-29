<template>
  <div class="profile-page">
    <el-card class="profile-card">
      <template #header>
        <div class="card-header">
          <span>个人资料</span>
        </div>
      </template>
      
      <el-tabs v-model="activeTab">
        <!-- 基本信息 -->
        <el-tab-pane label="基本信息" name="basic">
          <el-form
            ref="profileFormRef"
            :model="profileForm"
            :rules="profileRules"
            label-width="100px"
            class="profile-form"
          >
            <el-form-item label="头像">
              <div class="avatar-upload">
                <el-upload
                  class="avatar-uploader"
                  action="#"
                  :auto-upload="false"
                  :show-file-list="false"
                  :on-change="handleAvatarChange"
                  accept="image/*"
                >
                  <el-avatar
                    :size="100"
                    :src="profileForm.avatar || defaultAvatar"
                    class="user-avatar cursor-pointer"
                  />
                  <template #tip>
                    <div class="avatar-tip">点击头像更换</div>
                  </template>
                </el-upload>
              </div>
            </el-form-item>
            
            <el-form-item label="用户名">
              <el-input v-model="profileForm.username" disabled />
            </el-form-item>
            
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="profileForm.nickname" placeholder="请输入昵称" />
            </el-form-item>
            
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
            </el-form-item>
            
            <el-form-item label="个性签名" prop="signature">
              <el-input
                v-model="profileForm.signature"
                type="textarea"
                :rows="3"
                placeholder="写点什么来展示自己..."
                maxlength="200"
                show-word-limit
              />
            </el-form-item>
            
            <el-form-item>
              <el-button type="primary" @click="handleSaveProfile" :loading="saving">
                保存修改
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        
        <!-- 修改密码 -->
        <el-tab-pane label="修改密码" name="password">
          <el-form
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            label-width="100px"
            class="password-form"
          >
            <el-form-item label="原密码" prop="oldPassword">
              <el-input
                v-model="passwordForm.oldPassword"
                type="password"
                placeholder="请输入原密码"
                show-password
              />
            </el-form-item>
            
            <el-form-item label="新密码" prop="newPassword">
              <el-input
                v-model="passwordForm.newPassword"
                type="password"
                placeholder="请输入新密码"
                show-password
              />
            </el-form-item>
            
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="passwordForm.confirmPassword"
                type="password"
                placeholder="请再次输入新密码"
                show-password
              />
            </el-form-item>
            
            <el-form-item>
              <el-button type="primary" @click="handleChangePassword" :loading="changingPassword">
                修改密码
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import { getProfile, updateProfile, uploadAvatar, changePassword } from '@/api/user'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const activeTab = ref('basic')
const saving = ref(false)
const changingPassword = ref(false)
const profileFormRef = ref()
const passwordFormRef = ref()

const defaultAvatar = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'

const profileForm = reactive({
  username: '',
  nickname: '',
  email: '',
  avatar: '',
  signature: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const profileRules = {
  nickname: [
    { max: 50, message: '昵称不能超过50个字符', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  signature: [
    { max: 200, message: '个性签名不能超过200个字符', trigger: 'blur' }
  ]
}

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

// 加载用户资料
const loadProfile = async () => {
  try {
    const data = await getProfile()
    console.log('获取用户资料响应:', data)
    // 响应拦截器已经提取了 res.data，这里直接使用 data
    Object.assign(profileForm, {
      username: data.username || '',
      nickname: data.nickname || '',
      email: data.email || '',
      avatar: data.avatar || '',
      signature: data.signature || ''
    })
  } catch (error) {
    console.error('加载用户资料失败:', error)
    ElMessage.error('加载用户资料失败')
  }
}

// 压缩图片
const compressImage = (file, maxWidth = 800, maxHeight = 800, quality = 0.8) => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.readAsDataURL(file)
    reader.onload = (e) => {
      const img = new Image()
      img.src = e.target.result
      img.onload = () => {
        // 计算缩放比例
        let width = img.width
        let height = img.height
        
        if (width > maxWidth || height > maxHeight) {
          const ratio = Math.min(maxWidth / width, maxHeight / height)
          width = width * ratio
          height = height * ratio
        }
        
        // 创建 canvas
        const canvas = document.createElement('canvas')
        canvas.width = width
        canvas.height = height
        const ctx = canvas.getContext('2d')
        
        // 绘制图片
        ctx.drawImage(img, 0, 0, width, height)
        
        // 转换为 blob
        canvas.toBlob((blob) => {
          if (blob) {
            // 创建新的文件对象
            const compressedFile = new File([blob], file.name, {
              type: 'image/jpeg',
              lastModified: Date.now()
            })
            console.log('压缩前:', (file.size / 1024).toFixed(2), 'KB')
            console.log('压缩后:', (compressedFile.size / 1024).toFixed(2), 'KB')
            resolve(compressedFile)
          } else {
            reject(new Error('图片压缩失败'))
          }
        }, 'image/jpeg', quality)
      }
      img.onerror = reject
    }
    reader.onerror = reject
  })
}

// 处理头像更换
const handleAvatarChange = async (file) => {
  console.log('上传文件信息:', file)
  
  // 获取文件对象（兼容不同格式）
  let rawFile = file.raw || file
  
  if (!rawFile) {
    ElMessage.error('文件获取失败')
    return
  }
  
  console.log('文件类型:', rawFile.type)
  console.log('文件大小:', rawFile.size, 'bytes =', (rawFile.size / 1024 / 1024).toFixed(2), 'MB')
  
  const isImage = rawFile.type && rawFile.type.startsWith('image/')

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return
  }
  
  // 如果图片大于 2MB，自动压缩
  const isLt2M = rawFile.size / 1024 / 1024 < 2
  if (!isLt2M) {
    ElMessage.info('图片较大，正在自动压缩...')
    try {
      rawFile = await compressImage(rawFile, 800, 800, 0.8)
      ElMessage.success(`压缩完成，新大小: ${(rawFile.size / 1024).toFixed(2)}KB`)
    } catch (error) {
      console.error('压缩失败:', error)
      ElMessage.error('图片压缩失败，请选择更小的图片')
      return
    }
  }

  try {
    const avatarUrl = await uploadAvatar(rawFile)
    // 响应拦截器已经处理了 code 判断，成功直接返回 data
    profileForm.avatar = avatarUrl
    // 更新 store 中的头像
    userStore.updateAvatar(avatarUrl)
    ElMessage.success('头像上传成功')
  } catch (error) {
    console.error('头像上传失败:', error)
    ElMessage.error(error.message || '头像上传失败')
  }
}

// 保存资料
const handleSaveProfile = async () => {
  const valid = await profileFormRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const data = await updateProfile({
      nickname: profileForm.nickname,
      email: profileForm.email,
      signature: profileForm.signature
    })
    // 响应拦截器已经处理了 code 判断，成功直接返回 data
    // 更新 store
    userStore.updateProfile({
      nickname: profileForm.nickname,
      avatar: profileForm.avatar,
      signature: profileForm.signature
    })
    ElMessage.success('保存成功')
    // 跳转到主页
    router.push('/')
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// 修改密码
const handleChangePassword = async () => {
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) return

  changingPassword.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    // 响应拦截器已经处理了 code 判断
    ElMessage.success('密码修改成功')
    // 清空表单
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
  } catch (error) {
    console.error('修改失败:', error)
    ElMessage.error(error.message || '修改失败')
  } finally {
    changingPassword.value = false
  }
}

onMounted(() => {
  loadProfile()
})
</script>

<style scoped lang="scss">
.profile-page {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.profile-card {
  .card-header {
    font-size: 18px;
    font-weight: 600;
  }
}

.profile-form,
.password-form {
  max-width: 500px;
  margin-top: 20px;
}

.avatar-upload {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;

  .user-avatar {
    border: 2px solid #e4e7ed;
    transition: all 0.3s;
    
    &:hover {
      border-color: #409eff;
      box-shadow: 0 0 8px rgba(64, 158, 255, 0.4);
    }
  }

  .avatar-tip {
    font-size: 12px;
    color: #909399;
  }
}
</style>
