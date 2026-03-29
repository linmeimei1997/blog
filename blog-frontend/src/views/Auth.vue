<template>
  <div class="auth-container">
    <!-- 二次元动漫背景 -->
    <AnimeBackground />
    
    <!-- 主内容区 -->
    <div class="auth-content">
      <!-- 左侧标语（桌面端显示） -->
      <div class="auth-slogan">
        <h1 class="slogan-title">
          <span class="gradient-text">Blog Space</span>
        </h1>
        <p class="slogan-subtitle">记录生活，分享知识，AI 相伴</p>
        <div class="slogan-features">
          <div class="feature-item">
            <el-icon><Document /></el-icon>
            <span>博客创作</span>
          </div>
          <div class="feature-item">
            <el-icon><Reading /></el-icon>
            <span>知识库</span>
          </div>
          <div class="feature-item">
            <el-icon><ChatDotRound /></el-icon>
            <span>AI 助手</span>
          </div>
        </div>
      </div>
      
      <!-- 右侧表单 -->
      <div class="auth-box">
        <!-- 表单卡片 -->
        <div class="auth-card">
          <!-- 标签切换 -->
          <div class="auth-tabs">
            <div 
              :class="['tab-item', { active: isLogin }]"
              @click="isLogin = true"
            >
              登录
            </div>
            <div 
              :class="['tab-item', { active: !isLogin }]"
              @click="isLogin = false"
            >
              注册
            </div>
            <div class="tab-indicator" :style="{ left: isLogin ? '0%' : '50%' }"></div>
          </div>
          
          <!-- 登录表单 -->
          <div v-show="isLogin" class="form-container">
            <el-form
              ref="loginFormRef"
              :model="loginForm"
              :rules="loginRules"
              class="auth-form"
              @keyup.enter="handleLogin"
            >
              <el-form-item prop="username">
                <el-input
                  v-model="loginForm.username"
                  placeholder="用户名"
                  size="large"
                  :prefix-icon="User"
                  class="anime-input"
                />
              </el-form-item>
              
              <el-form-item prop="password">
                <el-input
                  v-model="loginForm.password"
                  type="password"
                  placeholder="密码"
                  size="large"
                  :prefix-icon="Lock"
                  show-password
                  class="anime-input"
                />
              </el-form-item>
              
              <div class="form-options">
                <el-checkbox v-model="rememberMe">记住我</el-checkbox>
                <a href="#" class="forgot-link" @click.prevent="forgotPassword">忘记密码?</a>
              </div>
              
              <el-form-item>
                <el-button
                  type="primary"
                  size="large"
                  class="submit-btn"
                  :loading="loading"
                  @click="handleLogin"
                >
                  <span v-if="!loading">立即登录</span>
                  <AnimeLoading v-else text="正在登录" />
                </el-button>
              </el-form-item>
            </el-form>
          </div>
          
          <!-- 注册表单 -->
          <div v-show="!isLogin" class="form-container">
            <el-form
              ref="registerFormRef"
              :model="registerForm"
              :rules="registerRules"
              class="auth-form"
              @keyup.enter="handleRegister"
            >
              <el-form-item prop="username">
                <el-input
                  v-model="registerForm.username"
                  placeholder="用户名 (3-20个字符)"
                  size="large"
                  :prefix-icon="User"
                  class="anime-input"
                />
              </el-form-item>
              
              <el-form-item prop="nickname">
                <el-input
                  v-model="registerForm.nickname"
                  placeholder="昵称 (选填)"
                  size="large"
                  :prefix-icon="Avatar"
                  class="anime-input"
                />
              </el-form-item>
              
              <el-form-item prop="password">
                <el-input
                  v-model="registerForm.password"
                  type="password"
                  placeholder="密码 (6-20个字符)"
                  size="large"
                  :prefix-icon="Lock"
                  show-password
                  class="anime-input"
                />
              </el-form-item>
              
              <el-form-item prop="confirmPassword">
                <el-input
                  v-model="registerForm.confirmPassword"
                  type="password"
                  placeholder="确认密码"
                  size="large"
                  :prefix-icon="Key"
                  show-password
                  class="anime-input"
                />
              </el-form-item>
              
              <el-form-item>
                <el-button
                  type="primary"
                  size="large"
                  class="submit-btn"
                  :loading="loading"
                  @click="handleRegister"
                >
                  <span v-if="!loading">立即注册</span>
                  <AnimeLoading v-else text="正在注册" />
                </el-button>
              </el-form-item>
            </el-form>
          </div>
          
          <!-- 社交登录 -->
          <div class="social-login">
            <div class="divider">
              <span>其他方式</span>
            </div>
            <div class="social-icons">
              <div class="social-icon wechat" @click="socialLogin('wechat')">
                <el-icon><ChatDotRound /></el-icon>
              </div>
              <div class="social-icon github" @click="socialLogin('github')">
                <el-icon><Platform /></el-icon>
              </div>
              <div class="social-icon qq" @click="socialLogin('qq')">
                <el-icon><ChatLineRound /></el-icon>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 底部提示 -->
        <div class="auth-footer">
          <p v-if="isLogin">
            还没有账号? <a href="#" @click.prevent="isLogin = false">立即注册</a>
          </p>
          <p v-else>
            已有账号? <a href="#" @click.prevent="isLogin = true">立即登录</a>
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { login, register } from '@/api/auth'
import AnimeBackground from '@/components/AnimeBackground.vue'
import AnimeLoading from '@/components/AnimeLoading.vue'
import {
  User, Lock, Avatar, Key, Document, Reading,
  ChatDotRound, ChatLineRound, Platform
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const isLogin = ref(true)
const loading = ref(false)
const rememberMe = ref(false)
const loginFormRef = ref(null)
const registerFormRef = ref(null)

const loginForm = reactive({
  username: '',
  password: ''
})

const registerForm = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: ''
})

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在3-20个字符之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在6-20个字符之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  const valid = await loginFormRef.value.validate().catch(() => false)
  if (!valid) return
  
  loading.value = true
  try {
    const result = await userStore.login(loginForm)
    console.log('登录成功:', result)
    ElMessage.success('登录成功')
    
    // 判断是否是移动端访问，跳转到对应首页
    const isMobile = window.innerWidth <= 768 || route.query.mobile === 'true'
    if (isMobile) {
      router.push('/m/home')
    } else {
      router.push('/')
    }
  } catch (error) {
    console.error('登录失败:', error)
    ElMessage.error(error.message || '登录失败')
  } finally {
    loading.value = false
  }
}

const handleRegister = async () => {
  const valid = await registerFormRef.value.validate().catch(() => false)
  if (!valid) return
  
  loading.value = true
  try {
    const result = await register({
      username: registerForm.username,
      password: registerForm.password,
      nickname: registerForm.nickname || registerForm.username
    })
    
    // 注册成功后自动登录
    userStore.setToken(result.token)
    userStore.userInfo = result.user
    
    ElMessage.success('注册成功')
    
    // 判断是否是移动端访问，跳转到对应首页
    const isMobile = window.innerWidth <= 768 || route.query.mobile === 'true'
    if (isMobile) {
      router.push('/m/home')
    } else {
      router.push('/')
    }
  } catch (error) {
    console.error('注册失败:', error)
    ElMessage.error(error.message || '注册失败')
  } finally {
    loading.value = false
  }
}

const forgotPassword = () => {
  ElMessage.info('请联系管理员重置密码')
}

const socialLogin = (type) => {
  ElMessage.info(`${type}登录功能开发中...`)
}
</script>

<style scoped lang="scss">
.auth-container {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
}

.auth-content {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

// 左侧标语
.auth-slogan {
  flex: 1;
  max-width: 500px;
  padding: 40px;
  color: #fff;
  display: none;
  
  .slogan-title {
    font-size: 48px;
    font-weight: 700;
    margin-bottom: 20px;
    
    .gradient-text {
      background: linear-gradient(135deg, #FF69B4 0%, #FFB6C1 50%, #FFC0CB 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
      text-shadow: 0 0 30px rgba(255, 105, 180, 0.5);
    }
  }
  
  .slogan-subtitle {
    font-size: 20px;
    margin-bottom: 40px;
    opacity: 0.9;
    color: #666;
  }
  
  .slogan-features {
    display: flex;
    gap: 30px;
    
    .feature-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 8px;
      padding: 20px;
      background: rgba(255, 255, 255, 0.9);
      border-radius: 16px;
      backdrop-filter: blur(10px);
      transition: all 0.3s ease;
      color: #333;
      
      &:hover {
        transform: translateY(-5px);
        box-shadow: 0 10px 30px rgba(255, 105, 180, 0.3);
      }
      
      .el-icon {
        font-size: 28px;
        color: #FF69B4;
      }
      
      span {
        font-size: 14px;
        font-weight: 500;
      }
    }
  }
}

// 右侧表单
.auth-box {
  width: 100%;
  max-width: 420px;
}

.auth-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 40px;
  box-shadow: 
    0 25px 80px rgba(0, 0, 0, 0.15),
    0 0 0 1px rgba(255, 255, 255, 0.5) inset;
}

// 标签切换
.auth-tabs {
  position: relative;
  display: flex;
  margin-bottom: 30px;
  border-bottom: 2px solid #f0f0f0;
  
  .tab-item {
    flex: 1;
    text-align: center;
    padding: 15px 0;
    font-size: 18px;
    font-weight: 600;
    color: #999;
    cursor: pointer;
    transition: all 0.3s ease;
    
    &:hover {
      color: #FF69B4;
    }
    
    &.active {
      color: #FF69B4;
    }
  }
  
  .tab-indicator {
    position: absolute;
    bottom: -2px;
    width: 50%;
    height: 3px;
    background: linear-gradient(90deg, #FF69B4, #FFB6C1);
    border-radius: 3px;
    transition: left 0.3s ease;
  }
}

// 表单样式
.auth-form {
  .anime-input {
    :deep(.el-input__wrapper) {
      border-radius: 12px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
      transition: all 0.3s ease;
      
      &:hover, &:focus-within {
        box-shadow: 0 4px 16px rgba(255, 105, 180, 0.15);
        transform: translateY(-1px);
      }
    }
  }
  
  .form-options {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    
    .forgot-link {
      color: #FF69B4;
      font-size: 14px;
      text-decoration: none;
      
      &:hover {
        text-decoration: underline;
      }
    }
  }
  
  .submit-btn {
    width: 100%;
    height: 48px;
    border-radius: 12px;
    font-size: 16px;
    font-weight: 600;
    background: linear-gradient(135deg, #FF69B4 0%, #FF1493 100%);
    border: none;
    position: relative;
    overflow: hidden;
    transition: all 0.3s ease;
    
    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: -100%;
      width: 100%;
      height: 100%;
      background: linear-gradient(90deg, transparent, rgba(255,255,255,0.3), transparent);
      transition: left 0.5s ease;
    }
    
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 10px 25px rgba(255, 105, 180, 0.4);
      
      &::before {
        left: 100%;
      }
    }
  }
}

// 社交登录
.social-login {
  margin-top: 30px;
  
  .divider {
    position: relative;
    text-align: center;
    margin-bottom: 20px;
    
    &::before {
      content: '';
      position: absolute;
      top: 50%;
      left: 0;
      right: 0;
      height: 1px;
      background: linear-gradient(90deg, transparent, #ddd, transparent);
    }
    
    span {
      position: relative;
      background: #fff;
      padding: 0 16px;
      color: #999;
      font-size: 12px;
    }
  }
  
  .social-icons {
    display: flex;
    justify-content: center;
    gap: 20px;
    
    .social-icon {
      width: 44px;
      height: 44px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      transition: all 0.3s ease;
      font-size: 20px;
      
      &.wechat {
        background: #f0f9eb;
        color: #67c23a;
        
        &:hover {
          background: #67c23a;
          color: #fff;
          transform: translateY(-3px);
          box-shadow: 0 8px 20px rgba(103, 194, 58, 0.3);
        }
      }
      
      &.github {
        background: #f4f4f5;
        color: #303133;
        
        &:hover {
          background: #303133;
          color: #fff;
          transform: translateY(-3px);
          box-shadow: 0 8px 20px rgba(48, 49, 51, 0.3);
        }
      }
      
      &.qq {
        background: #ecf5ff;
        color: #409eff;
        
        &:hover {
          background: #409eff;
          color: #fff;
          transform: translateY(-3px);
          box-shadow: 0 8px 20px rgba(64, 158, 255, 0.3);
        }
      }
    }
  }
}

// 底部提示
.auth-footer {
  text-align: center;
  margin-top: 20px;
  color: #666;
  font-size: 14px;
  
  a {
    color: #FF69B4;
    text-decoration: none;
    font-weight: 600;
    
    &:hover {
      text-decoration: underline;
    }
  }
}

// 桌面端适配
@media (min-width: 1024px) {
  .auth-content {
    justify-content: space-between;
    padding: 40px 80px;
  }
  
  .auth-slogan {
    display: block;
  }
  
  .auth-box {
    max-width: 450px;
  }
}

// 手机端适配
@media (max-width: 768px) {
  .auth-content {
    padding: 20px;
  }
  
  .auth-card {
    padding: 30px 20px;
    border-radius: 20px;
  }
  
  .auth-tabs {
    .tab-item {
      font-size: 16px;
      padding: 12px 0;
    }
  }
  
  .social-login {
    .social-icons {
      gap: 15px;
      
      .social-icon {
        width: 40px;
        height: 40px;
        font-size: 18px;
      }
    }
  }
}
</style>
