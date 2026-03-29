<template>
  <div class="login-container">
    <!-- Canvas 粒子动画背景 -->
    <ParticleBackground />
    
    <!-- 测试用：确认页面已加载 -->
    <div style="position: fixed; top: 10px; right: 10px; background: red; color: white; padding: 10px; z-index: 9999; border-radius: 8px;">
      🎉 动漫小人已加载！
    </div>
    
    <!-- 动漫小人角色 - 移到 container 层级 -->
    <div class="mascot-wrapper">
      <div class="mascot-character">
        <!-- 头部 -->
        <div class="mascot-head">
          <!-- 眼睛 -->
          <div class="mascot-eyes">
            <div class="eye left"></div>
            <div class="eye right"></div>
          </div>
          <!-- 腮红 -->
          <div class="mascot-blush left"></div>
          <div class="mascot-blush right"></div>
          <!-- 嘴巴 -->
          <div class="mascot-mouth"></div>
        </div>
        <!-- 身体 -->
        <div class="mascot-body"></div>
        <!-- 手臂 -->
        <div class="mascot-arm left"></div>
        <div class="mascot-arm right"></div>
      </div>
    </div>
    
    <!-- 浮动装饰元素 -->
    <div class="floating-shapes">
      <div class="shape shape-1"></div>
      <div class="shape shape-2"></div>
      <div class="shape shape-3"></div>
      <div class="shape shape-4"></div>
    </div>
    
    <div class="login-box">
      <div class="login-header">
        <div class="logo-animation">
          <div class="logo-circle">
            <el-icon size="40" color="#fff"><Edit /></el-icon>
          </div>
        </div>
        <h1>Blog Space</h1>
        <p class="subtitle">记录生活，分享知识，AI 相伴</p>
      </div>
      
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        class="login-form"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="用户名"
            size="large"
            :prefix-icon="User"
            class="glow-input"
          />
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            size="large"
            :prefix-icon="Lock"
            show-password
            class="glow-input"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            <span class="btn-text">立即登录</span>
            <el-icon class="btn-icon"><ArrowRight /></el-icon>
          </el-button>
        </el-form-item>
      </el-form>
      
      <div class="login-tips">
        <p>测试账号：admin / admin123</p>
      </div>
      
      <!-- 社交登录 -->
      <div class="social-login">
        <div class="divider">
          <span>其他方式</span>
        </div>
        <div class="social-icons">
          <div class="social-icon wechat">
            <el-icon><ChatDotRound /></el-icon>
          </div>
          <div class="social-icon github">
            <el-icon><Platform /></el-icon>
          </div>
          <div class="social-icon qq">
            <el-icon><ChatLineRound /></el-icon>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock, ArrowRight, Edit, ChatDotRound, Platform, ChatLineRound } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import ParticleBackground from '@/components/ParticleBackground.vue'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const mascotRef = ref(null)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// 动漫小人偷看密码互动
const handlePasswordFocus = () => {
  const eyes = document.querySelectorAll('.mascot-character .eye')
  eyes.forEach(eye => {
    eye.style.transform = 'translateY(-3px) scale(1.2)'
  })
}

const handlePasswordBlur = () => {
  const eyes = document.querySelectorAll('.mascot-character .eye')
  eyes.forEach(eye => {
    eye.style.transform = 'translateY(0) scale(1)'
  })
}

onMounted(() => {
  console.log('页面已加载，尝试显示动漫小人')
  
  // 延迟执行以确保 DOM 已经渲染
  setTimeout(() => {
    const mascotContainer = document.querySelector('.mascot-container')
    console.log('动漫小人容器:', mascotContainer)
    
    if (mascotContainer) {
      const rect = mascotContainer.getBoundingClientRect()
      console.log('小人位置:', rect)
      
      // 检查是否在可视区域内
      if (rect.bottom < 0 || rect.right < 0 || rect.top > window.innerHeight || rect.left > window.innerWidth) {
        console.warn('警告：动漫小人在可视区域外！')
      } else {
        console.log('小人应该在可视区域内')
      }
    }
    
    const passwordInput = document.querySelector('input[type="password"]')
    console.log('密码输入框:', passwordInput)
    if (passwordInput) {
      passwordInput.addEventListener('focus', handlePasswordFocus)
      passwordInput.addEventListener('blur', handlePasswordBlur)
    }
  }, 500)
})

onUnmounted(() => {
  const passwordInput = document.querySelector('input[type="password"]')
  if (passwordInput) {
    passwordInput.removeEventListener('focus', handlePasswordFocus)
    passwordInput.removeEventListener('blur', handlePasswordBlur)
  }
})

const handleLogin = async () => {
  if (!formRef.value) return
  
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  
  loading.value = true
  try {
    const result = await userStore.login(form)
    console.log('登录成功:', result)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (error) {
    console.error('登录失败:', error)
    ElMessage.error(error.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
  position: relative;
  overflow: hidden;
}

// 浮动装饰元素
.floating-shapes {
  position: absolute;
  width: 100%;
  height: 100%;
  overflow: hidden;
  pointer-events: none;
}

.shape {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.4;
  animation: float 20s infinite ease-in-out;
}

.shape-1 {
  width: 300px;
  height: 300px;
  background: #ff6b6b;
  top: -100px;
  left: -100px;
  animation-delay: 0s;
}

.shape-2 {
  width: 400px;
  height: 400px;
  background: #4ecdc4;
  bottom: -150px;
  right: -100px;
  animation-delay: -5s;
}

.shape-3 {
  width: 200px;
  height: 200px;
  background: #ffe66d;
  top: 50%;
  left: 10%;
  animation-delay: -10s;
}

.shape-4 {
  width: 250px;
  height: 250px;
  background: #a8e6cf;
  bottom: 20%;
  right: 15%;
  animation-delay: -15s;
}

@keyframes float {
  0%, 100% {
    transform: translate(0, 0) scale(1);
  }
  25% {
    transform: translate(50px, -50px) scale(1.1);
  }
  50% {
    transform: translate(0, -100px) scale(1);
  }
  75% {
    transform: translate(-50px, -50px) scale(0.9);
  }
}

.login-box {
  width: 440px;
  padding: 50px 45px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 24px;
  box-shadow: 
    0 25px 80px rgba(0, 0, 0, 0.15),
    0 0 0 1px rgba(255, 255, 255, 0.3) inset;
  backdrop-filter: blur(20px);
  position: relative;
  z-index: 1;
  animation: slideUp 0.6s ease-out;
  // 确保不会裁剪动漫小人
  overflow: visible;
}

// 动漫小人角色
.mascot-container {
  position: absolute;
  top: -70px;
  left: -90px;
  width: 140px;
  height: 140px;
  z-index: 10;
  pointer-events: none;
}

.mascot-character {
  position: relative;
  width: 100%;
  height: 100%;
  animation: mascotBounce 3s ease-in-out infinite;
}

.mascot-head {
  position: absolute;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  width: 70px;
  height: 70px;
  background: linear-gradient(135deg, #FFD700 0%, #FFC107 100%);
  border-radius: 50%;
  border: 3px solid #FFA000;
  box-shadow: 0 4px 12px rgba(255, 193, 7, 0.4);
  z-index: 2;
}

.mascot-eyes {
  position: absolute;
  top: 25px;
  left: 50%;
  transform: translateX(-50%);
  width: 50px;
  display: flex;
  justify-content: space-between;
}

.eye {
  width: 14px;
  height: 18px;
  background: #3E2723;
  border-radius: 50%;
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
  
  &::before {
    content: '';
    position: absolute;
    top: 3px;
    right: 3px;
    width: 6px;
    height: 6px;
    background: white;
    border-radius: 50%;
  }
}

.mascot-blush {
  position: absolute;
  top: 45px;
  width: 12px;
  height: 8px;
  background: radial-gradient(circle, rgba(255, 107, 107, 0.6) 0%, transparent 70%);
  border-radius: 50%;
  opacity: 0.6;
  animation: blushBreath 2s ease-in-out infinite;
  
  &.left {
    left: 12px;
  }
  
  &.right {
    right: 12px;
  }
}

.mascot-mouth {
  position: absolute;
  top: 52px;
  left: 50%;
  transform: translateX(-50%);
  width: 16px;
  height: 8px;
  background: #D32F2F;
  border-radius: 0 0 16px 16px;
}

.mascot-body {
  position: absolute;
  top: 75px;
  left: 50%;
  transform: translateX(-50%);
  width: 50px;
  height: 40px;
  background: linear-gradient(135deg, #64B5F6 0%, #42A5F5 100%);
  border-radius: 25px;
  border: 3px solid #1E88E5;
  z-index: 1;
}

.mascot-arm {
  position: absolute;
  top: 80px;
  width: 30px;
  height: 10px;
  background: linear-gradient(135deg, #64B5F6 0%, #42A5F5 100%);
  border: 3px solid #1E88E5;
  border-radius: 5px;
  
  &.left {
    left: 20px;
    transform: rotate(30deg);
    transform-origin: right center;
    animation: armWaveLeft 2s ease-in-out infinite;
  }
  
  &.right {
    right: 20px;
    transform: rotate(-30deg);
    transform-origin: left center;
    animation: armWaveRight 2s ease-in-out infinite;
  }
}

@keyframes mascotBounce {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

@keyframes blushBreath {
  0%, 100% {
    opacity: 0.4;
  }
  50% {
    opacity: 0.8;
  }
}

@keyframes armWaveLeft {
  0%, 100% {
    transform: rotate(30deg);
  }
  50% {
    transform: rotate(10deg);
  }
}

@keyframes armWaveRight {
  0%, 100% {
    transform: rotate(-30deg);
  }
  50% {
    transform: rotate(-10deg);
  }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
  
  .logo-animation {
    margin-bottom: 20px;
    
    .logo-circle {
      width: 80px;
      height: 80px;
      margin: 0 auto;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 10px 30px rgba(102, 126, 234, 0.4);
      animation: pulse 2s infinite;
    }
  }
  
  h1 {
    font-size: 32px;
    font-weight: 700;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    margin-bottom: 8px;
    letter-spacing: -0.5px;
  }
  
  .subtitle {
    color: #888;
    font-size: 14px;
    letter-spacing: 1px;
  }
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
    box-shadow: 0 10px 30px rgba(102, 126, 234, 0.4);
  }
  50% {
    transform: scale(1.05);
    box-shadow: 0 15px 40px rgba(102, 126, 234, 0.5);
  }
}

.login-form {
  .glow-input {
    :deep(.el-input__wrapper) {
      border-radius: 12px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
      transition: all 0.3s ease;
      
      &:hover, &:focus-within {
        box-shadow: 0 4px 16px rgba(102, 126, 234, 0.15);
        transform: translateY(-1px);
      }
    }
  }
  
  .login-btn {
    width: 100%;
    border-radius: 12px;
    height: 48px;
    font-size: 16px;
    font-weight: 600;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
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
      box-shadow: 0 10px 25px rgba(102, 126, 234, 0.4);
      
      &::before {
        left: 100%;
      }
      
      .btn-icon {
        transform: translateX(4px);
      }
    }
    
    .btn-text {
      margin-right: 8px;
    }
    
    .btn-icon {
      transition: transform 0.3s ease;
    }
  }
}

.login-tips {
  margin-top: 24px;
  text-align: center;
  color: #999;
  font-size: 13px;
  padding: 12px;
  background: rgba(102, 126, 234, 0.05);
  border-radius: 8px;
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
</style>

<style lang="scss">
// 全局样式 - 动漫小人（不受 scoped 限制）
.mascot-wrapper {
  position: absolute;
  top: 100px;
  left: calc(50% - 290px);
  width: 140px;
  height: 140px;
  z-index: 20;
  pointer-events: none;
}

.mascot-character {
  position: relative;
  width: 100%;
  height: 100%;
  animation: mascotBounce 3s ease-in-out infinite;
}

.mascot-head {
  position: absolute;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  width: 70px;
  height: 70px;
  background: linear-gradient(135deg, #FFD700 0%, #FFC107 100%);
  border-radius: 50%;
  border: 3px solid #FFA000;
  box-shadow: 0 4px 12px rgba(255, 193, 7, 0.4);
  z-index: 2;
}

.mascot-eyes {
  position: absolute;
  top: 25px;
  left: 50%;
  transform: translateX(-50%);
  width: 50px;
  display: flex;
  justify-content: space-between;
}

.eye {
  width: 14px;
  height: 18px;
  background: #3E2723;
  border-radius: 50%;
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
  
  &::before {
    content: '';
    position: absolute;
    top: 3px;
    right: 3px;
    width: 6px;
    height: 6px;
    background: white;
    border-radius: 50%;
  }
}

.mascot-blush {
  position: absolute;
  top: 45px;
  width: 12px;
  height: 8px;
  background: radial-gradient(circle, rgba(255, 107, 107, 0.6) 0%, transparent 70%);
  border-radius: 50%;
  opacity: 0.6;
  animation: blushBreath 2s ease-in-out infinite;
  
  &.left {
    left: 12px;
  }
  
  &.right {
    right: 12px;
  }
}

.mascot-mouth {
  position: absolute;
  top: 52px;
  left: 50%;
  transform: translateX(-50%);
  width: 16px;
  height: 8px;
  background: #D32F2F;
  border-radius: 0 0 16px 16px;
}

.mascot-body {
  position: absolute;
  top: 75px;
  left: 50%;
  transform: translateX(-50%);
  width: 50px;
  height: 40px;
  background: linear-gradient(135deg, #64B5F6 0%, #42A5F5 100%);
  border-radius: 25px;
  border: 3px solid #1E88E5;
  z-index: 1;
}

.mascot-arm {
  position: absolute;
  top: 80px;
  width: 30px;
  height: 10px;
  background: linear-gradient(135deg, #64B5F6 0%, #42A5F5 100%);
  border: 3px solid #1E88E5;
  border-radius: 5px;
  
  &.left {
    left: 20px;
    transform: rotate(30deg);
    transform-origin: right center;
    animation: armWaveLeft 2s ease-in-out infinite;
  }
  
  &.right {
    right: 20px;
    transform: rotate(-30deg);
    transform-origin: left center;
    animation: armWaveRight 2s ease-in-out infinite;
  }
}

@keyframes mascotBounce {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

@keyframes blushBreath {
  0%, 100% {
    opacity: 0.4;
  }
  50% {
    opacity: 0.8;
  }
}

@keyframes armWaveLeft {
  0%, 100% {
    transform: rotate(30deg);
  }
  50% {
    transform: rotate(10deg);
  }
}

@keyframes armWaveRight {
  0%, 100% {
    transform: rotate(-30deg);
  }
  50% {
    transform: rotate(-10deg);
  }
}
</style>
