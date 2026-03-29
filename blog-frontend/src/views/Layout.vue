<template>
  <el-container class="layout-container">
    <el-aside width="260px" class="sidebar">
      <div class="logo">
        <div class="logo-icon">
          <el-icon size="24" color="#fff"><Edit /></el-icon>
        </div>
        <span class="logo-text">Blog Space</span>
      </div>
      
      <div class="user-preview">
        <el-popover
          placement="right"
          :width="200"
          trigger="click"
          popper-class="signature-popover"
        >
          <template #reference>
            <el-avatar 
              :size="48" 
              :src="userStore.avatar || defaultAvatar" 
              class="user-avatar cursor-pointer"
            />
          </template>
          <div class="signature-content">
            <div class="signature-title">✨ 个人签名</div>
            <div class="signature-text">
              {{ userStore.userInfo?.signature || '这个人很懒，还没有写签名~' }}
            </div>
            <div class="signature-decoration">🌸 🌸 🌸</div>
          </div>
        </el-popover>
        <div class="user-info">
          <span class="user-name">{{ userStore.nickname || userStore.username }}</span>
          <span class="user-role">创作者</span>
        </div>
      </div>
      
      <el-menu
        :default-active="$route.path"
        router
        class="sidebar-menu"
        background-color="transparent"
        text-color="#a0aec0"
        active-text-color="#667eea"
      >
        <el-menu-item index="/dashboard" class="menu-item">
          <el-icon><HomeFilled /></el-icon>
          <span>首页</span>
        </el-menu-item>
        
        <el-menu-item index="/articles" class="menu-item">
          <el-icon><Document /></el-icon>
          <span>文章管理</span>
        </el-menu-item>
        
        <el-menu-item index="/categories" class="menu-item">
          <el-icon><Folder /></el-icon>
          <span>分类管理</span>
        </el-menu-item>
        
        <el-menu-item index="/tags" class="menu-item">
          <el-icon><CollectionTag /></el-icon>
          <span>标签管理</span>
        </el-menu-item>
        
        <el-menu-item index="/knowledge" class="menu-item">
          <el-icon><Reading /></el-icon>
          <span>知识库</span>
        </el-menu-item>
        
        <el-menu-item index="/profile" class="menu-item">
          <el-icon><User /></el-icon>
          <span>个人资料</span>
        </el-menu-item>
      </el-menu>
      
      <div class="sidebar-footer">
        <div class="create-btn" @click="$router.push('/articles/create')">
          <el-icon><Plus /></el-icon>
          <span>写文章</span>
        </div>
      </div>
    </el-aside>
    
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <breadcrumb />
        </div>
        <div class="header-right">
          <el-tooltip content="AI 助手">
            <div class="header-icon ai-icon" @click="openAIChat">
              <el-icon size="20"><ChatDotRound /></el-icon>
              <span class="ai-badge">AI</span>
            </div>
          </el-tooltip>
          
          <el-dropdown @command="handleCommand" trigger="click">
            <div class="user-dropdown">
              <el-avatar :size="36" :src="userStore.avatar || defaultAvatar" />
              <span class="user-name">{{ userStore.nickname || userStore.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu class="user-dropdown-menu">
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>
                  <span>个人资料</span>
                </el-dropdown-item>
                <el-dropdown-item command="settings">
                  <el-icon><Setting /></el-icon>
                  <span>系统设置</span>
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  <span>退出登录</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      
      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade-transform" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getProfile } from '@/api/user'
import {
  Document, HomeFilled, Folder, CollectionTag, Reading,
  ArrowDown, User, SwitchButton, Plus, ChatDotRound,
  Edit, Setting
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const defaultAvatar = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'

// 加载用户信息（包括签名）
const loadUserInfo = async () => {
  try {
    const data = await getProfile()
    // 更新 store 中的用户信息
    if (data) {
      userStore.userInfo = { ...userStore.userInfo, ...data }
    }
  } catch (error) {
    console.error('加载用户信息失败:', error)
  }
}

// 页面加载时获取最新用户信息
loadUserInfo()

const handleCommand = (command) => {
  if (command === 'logout') {
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } else if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'settings') {
    ElMessage.info('设置功能开发中...')
  }
}

const openAIChat = () => {
  window.dispatchEvent(new CustomEvent('open-ai-chat'))
}
</script>

<style scoped lang="scss">
.layout-container {
  min-height: 100vh;
  background: #f8fafc;
}

.sidebar {
  background: linear-gradient(180deg, #FFF0F5 0%, #FFE4E1 50%, #FFC0CB 100%);
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  z-index: 100;
  display: flex;
  flex-direction: column;
  box-shadow: 4px 0 24px rgba(255, 105, 180, 0.2);
}

.logo {
  height: 80px;
  display: flex;
  align-items: center;
  padding: 0 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  
  .logo-icon {
    width: 40px;
    height: 40px;
    background: linear-gradient(135deg, #FF69B4 0%, #FFB6C1 100%);
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 12px;
    box-shadow: 0 4px 12px rgba(255, 105, 180, 0.4);
  }
  
  .logo-text {
    color: #FF1493;
    font-size: 20px;
    font-weight: 700;
    letter-spacing: -0.5px;
  }
}

.user-preview {
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid rgba(255, 105, 180, 0.2);
  
  .user-avatar {
    border: 3px solid #FF69B4;
    box-shadow: 0 4px 12px rgba(255, 105, 180, 0.3);
    cursor: pointer;
    transition: all 0.3s ease;
    
    &:hover {
      transform: scale(1.1);
      box-shadow: 0 6px 20px rgba(255, 105, 180, 0.5);
    }
  }
  
  .user-info {
    display: flex;
    flex-direction: column;
    
    .user-name {
      color: #FF1493;
      font-size: 14px;
      font-weight: 600;
    }
    
    .user-role {
      color: #FF69B4;
      font-size: 12px;
      margin-top: 2px;
    }
  }
}

.sidebar-menu {
  flex: 1;
  border-right: none;
  padding: 16px 12px;
  
  :deep(.el-menu-item) {
    height: 48px;
    line-height: 48px;
    border-radius: 12px;
    margin-bottom: 4px;
    transition: all 0.3s ease;
    color: #666;
    
    &:hover {
      background: rgba(255, 105, 180, 0.1);
      color: #FF69B4;
    }
    
    &.is-active {
      background: linear-gradient(135deg, rgba(255, 105, 180, 0.2) 0%, rgba(255, 182, 193, 0.2) 100%);
      font-weight: 600;
      color: #FF1493;
      
      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 50%;
        transform: translateY(-50%);
        width: 4px;
        height: 20px;
        background: linear-gradient(180deg, #FF69B4 0%, #FFB6C1 100%);
        border-radius: 0 4px 4px 0;
      }
    }
    
    .el-icon {
      font-size: 18px;
      margin-right: 12px;
    }
  }
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  
  .create-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    height: 48px;
    background: linear-gradient(135deg, #FF69B4 0%, #FFB6C1 100%);
    border-radius: 12px;
    color: #fff;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s ease;
    box-shadow: 0 4px 12px rgba(255, 105, 180, 0.4);
    
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 20px rgba(255, 105, 180, 0.5);
    }
  }
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  position: sticky;
  top: 0;
  z-index: 99;
  margin-left: 260px;
  width: calc(100% - 260px);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
  
  .header-icon {
    width: 40px;
    height: 40px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: all 0.3s ease;
    background: #f8fafc;
    color: #64748b;
    
    &:hover {
      background: #f1f5f9;
      color: #667eea;
    }
    
    &.ai-icon {
      position: relative;
      background: linear-gradient(135deg, rgba(255, 105, 180, 0.1) 0%, rgba(255, 182, 193, 0.1) 100%);
      color: #FF69B4;
      
      &:hover {
        background: linear-gradient(135deg, #FF69B4 0%, #FFB6C1 100%);
        color: #fff;
      }
      
      .ai-badge {
        position: absolute;
        top: -4px;
        right: -4px;
        background: linear-gradient(135deg, #FF1493 0%, #FF69B4 100%);
        color: #fff;
        font-size: 10px;
        font-weight: 700;
        padding: 2px 6px;
        border-radius: 10px;
      }
    }
  }
  
  .user-dropdown {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 6px 12px;
    border-radius: 12px;
    cursor: pointer;
    transition: all 0.3s ease;
    
    &:hover {
      background: rgba(255, 105, 180, 0.1);
    }
    
    .user-name {
      font-size: 14px;
      font-weight: 500;
      color: #FF1493;
    }
    
    .el-icon {
      color: #FF69B4;
      font-size: 12px;
    }
  }
}

.main-content {
  background: #f8fafc;
  padding: 24px;
  margin-left: 260px;
  min-height: calc(100vh - 60px);
}

// 下拉菜单样式
:deep(.user-dropdown-menu) {
  padding: 8px;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(255, 105, 180, 0.2);
  
  .el-dropdown-menu__item {
    padding: 10px 16px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    gap: 10px;
    
    &:hover {
      background: rgba(255, 105, 180, 0.1);
      color: #FF69B4;
    }
    
    .el-icon {
      font-size: 16px;
    }
  }
}

// 签名弹窗样式
:deep(.signature-popover) {
  background: linear-gradient(135deg, #FFF0F5 0%, #FFE4E1 100%);
  border: 2px solid #FFB6C1;
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(255, 105, 180, 0.3);
  
  .el-popover__title {
    color: #FF1493;
    font-weight: 600;
  }
  
  .signature-content {
    text-align: center;
    padding: 10px;
    
    .signature-title {
      font-size: 16px;
      font-weight: 600;
      color: #FF1493;
      margin-bottom: 12px;
    }
    
    .signature-text {
      font-size: 14px;
      color: #666;
      line-height: 1.6;
      padding: 12px;
      background: rgba(255, 255, 255, 0.7);
      border-radius: 12px;
      min-height: 60px;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    
    .signature-decoration {
      margin-top: 12px;
      font-size: 14px;
      color: #FF69B4;
    }
  }
}

// 页面切换动画
.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.3s ease;
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(-20px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(20px);
}
</style>
