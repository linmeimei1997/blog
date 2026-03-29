<template>
  <div class="mobile-profile">
    <!-- 顶部导航 -->
    <div class="mobile-header">
      <div class="header-title">个人资料</div>
    </div>

    <!-- 内容区域 -->
    <div class="profile-content">
      <!-- 头像区域 -->
      <div class="avatar-section">
        <div class="avatar-wrapper" @click="triggerAvatarUpload">
          <el-avatar :size="80" :src="profileForm.avatar || defaultAvatar" />
          <div class="avatar-overlay">
            <el-icon size="20"><Camera /></el-icon>
            <span>更换头像</span>
          </div>
        </div>
        <div class="user-name">{{ profileForm.nickname || profileForm.username }}</div>
        <div class="user-signature" v-if="profileForm.signature">{{ profileForm.signature }}</div>
      </div>

      <!-- 基本信息表单 -->
      <div class="form-section">
        <div class="section-title">基本信息</div>
        
        <div class="form-item">
          <label>用户名</label>
          <div class="form-value disabled">{{ profileForm.username }}</div>
        </div>

        <div class="form-item">
          <label>昵称</label>
          <el-input
            v-model="profileForm.nickname"
            placeholder="请输入昵称"
            maxlength="50"
          />
        </div>

        <div class="form-item">
          <label>邮箱</label>
          <el-input
            v-model="profileForm.email"
            placeholder="请输入邮箱"
            type="email"
          />
        </div>

        <div class="form-item">
          <label>个性签名</label>
          <el-input
            v-model="profileForm.signature"
            type="textarea"
            :rows="3"
            placeholder="写点什么来展示自己..."
            maxlength="200"
            show-word-limit
          />
        </div>
      </div>

      <!-- 修改密码 -->
      <div class="form-section">
        <div class="section-title">修改密码</div>
        
        <div class="form-item">
          <label>原密码</label>
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            placeholder="请输入原密码"
            show-password
          />
        </div>

        <div class="form-item">
          <label>新密码</label>
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            placeholder="请输入新密码"
            show-password
          />
        </div>

        <div class="form-item">
          <label>确认密码</label>
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
          />
        </div>

        <el-button
          type="default"
          round
          :loading="changingPassword"
          @click="handleChangePassword"
          style="width: 100%"
        >
          修改密码
        </el-button>
      </div>

      <!-- 保存按钮 -->
      <div class="save-section">
        <el-button
          type="primary"
          round
          :loading="saving"
          @click="handleSaveProfile"
          style="width: 100%"
        >
          保存修改
        </el-button>
      </div>
    </div>

    <!-- 隐藏的头像上传 -->
    <input
      type="file"
      ref="avatarInput"
      style="display: none"
      accept="image/*"
      @change="handleAvatarChange"
    />

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
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Camera, HomeFilled, Document, Folder, ChatDotRound } from '@element-plus/icons-vue'
import { getProfile, updateProfile, uploadAvatar, changePassword } from '@/api/user'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

// 底部导航
const tabbarItems = [
  { path: '/m/home', label: '首页', icon: 'HomeFilled' },
  { path: '/m/articles', label: '文章', icon: 'Document' },
  { path: '/m/ai', label: 'AI助手', icon: 'ChatDotRound' },
  { path: '/m/categories', label: '分类', icon: 'Folder' }
]

const defaultAvatar = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'
const saving = ref(false)
const changingPassword = ref(false)
const avatarInput = ref(null)

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

// 加载用户资料
const loadProfile = async () => {
  try {
    const data = await getProfile()
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

// 触发头像上传
const triggerAvatarUpload = () => {
  avatarInput.value?.click()
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
        let width = img.width
        let height = img.height
        
        if (width > maxWidth || height > maxHeight) {
          const ratio = Math.min(maxWidth / width, maxHeight / height)
          width = width * ratio
          height = height * ratio
        }
        
        const canvas = document.createElement('canvas')
        canvas.width = width
        canvas.height = height
        const ctx = canvas.getContext('2d')
        ctx.drawImage(img, 0, 0, width, height)
        
        canvas.toBlob((blob) => {
          if (blob) {
            const compressedFile = new File([blob], file.name, {
              type: 'image/jpeg',
              lastModified: Date.now()
            })
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
const handleAvatarChange = async (event) => {
  const file = event.target.files[0]
  if (!file) return

  const isImage = file.type && file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return
  }

  let uploadFile = file
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    ElMessage.info('图片较大，正在自动压缩...')
    try {
      uploadFile = await compressImage(file, 800, 800, 0.8)
      ElMessage.success(`压缩完成`)
    } catch (error) {
      ElMessage.error('图片压缩失败')
      return
    }
  }

  try {
    const avatarUrl = await uploadAvatar(uploadFile)
    profileForm.avatar = avatarUrl
    userStore.updateAvatar(avatarUrl)
    ElMessage.success('头像上传成功')
  } catch (error) {
    ElMessage.error(error.message || '头像上传失败')
  }

  event.target.value = ''
}

// 保存资料
const handleSaveProfile = async () => {
  saving.value = true
  try {
    await updateProfile({
      nickname: profileForm.nickname,
      email: profileForm.email,
      signature: profileForm.signature
    })
    userStore.updateProfile({
      nickname: profileForm.nickname,
      avatar: profileForm.avatar,
      signature: profileForm.signature
    })
    ElMessage.success('保存成功')
    router.push('/m/home')
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// 修改密码
const handleChangePassword = async () => {
  if (!passwordForm.oldPassword) {
    ElMessage.warning('请输入原密码')
    return
  }
  if (!passwordForm.newPassword || passwordForm.newPassword.length < 6) {
    ElMessage.warning('新密码长度不能少于6位')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }

  changingPassword.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码修改成功')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
  } catch (error) {
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
.mobile-profile {
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
  justify-content: center;
  z-index: 100;
  color: #fff;

  .header-title {
    font-size: 17px;
    font-weight: 600;
  }
}

// 内容区域
.profile-content {
  padding-top: 50px;
}

// 头像区域
.avatar-section {
  background: linear-gradient(135deg, #FF69B4 0%, #FFB6C1 100%);
  padding: 30px 20px;
  text-align: center;
  color: #fff;

  .avatar-wrapper {
    position: relative;
    display: inline-block;
    cursor: pointer;

    .el-avatar {
      border: 3px solid rgba(255, 255, 255, 0.5);
      box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
    }

    .avatar-overlay {
      position: absolute;
      bottom: 0;
      left: 0;
      right: 0;
      background: rgba(0, 0, 0, 0.5);
      border-radius: 0 0 40px 40px;
      padding: 6px 0;
      display: flex;
      flex-direction: column;
      align-items: center;
      font-size: 10px;
      opacity: 0;
      transition: opacity 0.3s;
    }

    &:active .avatar-overlay {
      opacity: 1;
    }
  }

  .user-name {
    font-size: 18px;
    font-weight: 600;
    margin-top: 12px;
  }

  .user-signature {
    font-size: 13px;
    opacity: 0.8;
    margin-top: 6px;
  }
}

// 表单区域
.form-section {
  background: #fff;
  margin: 12px;
  border-radius: 12px;
  padding: 16px;

  .section-title {
    font-size: 15px;
    font-weight: 600;
    color: #333;
    margin-bottom: 16px;
    padding-bottom: 12px;
    border-bottom: 1px solid #f0f0f0;
  }

  .form-item {
    margin-bottom: 16px;

    label {
      display: block;
      font-size: 13px;
      color: #666;
      margin-bottom: 8px;
    }

    .form-value {
      font-size: 15px;
      color: #333;
      padding: 10px 0;

      &.disabled {
        color: #999;
      }
    }

    :deep(.el-input__wrapper) {
      border-radius: 8px;
    }

    :deep(.el-textarea__inner) {
      border-radius: 8px;
    }
  }
}

// 保存按钮
.save-section {
  padding: 16px 12px;

  .el-button {
    background: linear-gradient(135deg, #FF69B4 0%, #FFB6C1 100%);
    border: none;
    font-size: 16px;
    height: 48px;

    &:active {
      opacity: 0.9;
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
</style>
