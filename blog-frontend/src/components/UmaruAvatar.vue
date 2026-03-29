<template>
  <div class="umaru-avatar" :class="[mood, { talking: isTalking, jumping: isJumping }]">
    <svg viewBox="0 0 200 200" class="umaru-svg">
      <!-- 定义渐变 -->
      <defs>
        <linearGradient id="hairGradient" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" style="stop-color:#F4A460" />
          <stop offset="100%" style="stop-color:#D2691E" />
        </linearGradient>
        <linearGradient id="hoodieGradient" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" style="stop-color:#FF6B6B" />
          <stop offset="100%" style="stop-color:#EE5A5A" />
        </linearGradient>
        <radialGradient id="cheekGradient" cx="50%" cy="50%" r="50%">
          <stop offset="0%" style="stop-color:#FFB6C1" />
          <stop offset="100%" style="stop-color:#FFB6C1;stop-opacity:0" />
        </radialGradient>
      </defs>
      
      <!-- 仓鼠披风（后面） -->
      <ellipse cx="100" cy="160" rx="75" ry="55" fill="url(#hoodieGradient)" />
      <ellipse cx="100" cy="165" rx="60" ry="40" fill="#FFE4E1" opacity="0.3" />
      
      <!-- 左耳 -->
      <ellipse cx="45" cy="70" rx="20" ry="25" fill="url(#hairGradient)" />
      <ellipse cx="45" cy="70" rx="12" ry="15" fill="#FFE4E1" />
      
      <!-- 右耳 -->
      <ellipse cx="155" cy="70" rx="20" ry="25" fill="url(#hairGradient)" />
      <ellipse cx="155" cy="70" rx="12" ry="15" fill="#FFE4E1" />
      
      <!-- 头发 -->
      <ellipse cx="100" cy="90" rx="65" ry="60" fill="url(#hairGradient)" />
      
      <!-- 刘海 -->
      <path d="M 50 70 Q 70 90 90 75 Q 110 90 130 75 Q 150 85 150 70" 
            fill="none" stroke="#D2691E" stroke-width="2" opacity="0.5"/>
      
      <!-- 脸部 -->
      <ellipse cx="100" cy="100" rx="50" ry="45" fill="#FFE4E1" />
      
      <!-- 腮红 -->
      <ellipse cx="65" cy="115" rx="12" ry="8" fill="url(#cheekGradient)" opacity="0.6" />
      <ellipse cx="135" cy="115" rx="12" ry="8" fill="url(#cheekGradient)" opacity="0.6" />
      
      <!-- 眼睛 - 根据心情变化 -->
      <template v-if="mood === 'happy'">
        <!-- 开心时的弯弯眼 -->
        <path d="M 70 95 Q 80 90 90 95" fill="none" stroke="#333" stroke-width="3" stroke-linecap="round" />
        <path d="M 110 95 Q 120 90 130 95" fill="none" stroke="#333" stroke-width="3" stroke-linecap="round" />
      </template>
      <template v-else-if="mood === 'excited'">
        <!-- 兴奋时的星星眼 -->
        <path d="M 75 90 L 80 100 L 85 90 L 80 80 Z" fill="#FFD700" />
        <path d="M 115 90 L 120 100 L 125 90 L 120 80 Z" fill="#FFD700" />
      </template>
      <template v-else-if="mood === 'sleepy'">
        <!-- 困倦时的闭眼 -->
        <path d="M 70 100 Q 80 105 90 100" fill="none" stroke="#333" stroke-width="2" />
        <path d="M 110 100 Q 120 105 130 100" fill="none" stroke="#333" stroke-width="2" />
      </template>
      <template v-else>
        <!-- 默认豆豆眼 -->
        <circle cx="80" cy="100" r="6" fill="#333" />
        <circle cx="120" cy="100" r="6" fill="#333" />
        <!-- 高光 -->
        <circle cx="82" cy="98" r="2" fill="#fff" />
        <circle cx="122" cy="98" r="2" fill="#fff" />
      </template>
      
      <!-- 嘴巴 - 根据心情变化 -->
      <template v-if="mood === 'happy' || mood === 'excited'">
        <path d="M 90 120 Q 100 130 110 120" fill="none" stroke="#FF6B6B" stroke-width="3" stroke-linecap="round" />
      </template>
      <template v-else-if="mood === 'sleepy'">
        <circle cx="100" cy="125" r="3" fill="#FF6B6B" />
      </template>
      <template v-else>
        <path d="M 95 122 Q 100 125 105 122" fill="none" stroke="#FF6B6B" stroke-width="2" stroke-linecap="round" />
      </template>
      
      <!-- 仓鼠披风帽子 -->
      <path d="M 35 130 Q 100 110 165 130 L 165 150 Q 100 140 35 150 Z" fill="url(#hoodieGradient)" />
      
      <!-- 披风上的耳朵 -->
      <ellipse cx="50" cy="125" rx="15" ry="12" fill="#FFE4E1" />
      <ellipse cx="150" cy="125" rx="15" ry="12" fill="#FFE4E1" />
      
      <!-- 小手 -->
      <ellipse cx="55" cy="155" rx="12" ry="10" fill="#FFE4E1" />
      <ellipse cx="145" cy="155" rx="12" ry="10" fill="#FFE4E1" />
      
      <!-- 可乐（当兴奋时出现） -->
      <g v-if="mood === 'excited'" class="cola">
        <rect x="140" y="120" width="20" height="35" rx="3" fill="#DC143C" />
        <rect x="142" y="115" width="16" height="8" rx="2" fill="#8B0000" />
        <text x="150" y="140" font-size="8" fill="#fff" text-anchor="middle">COLA</text>
      </g>
      
      <!-- 游戏手柄（当玩游戏时出现） -->
      <g v-if="mood === 'gaming'" class="gamepad">
        <rect x="130" y="145" width="40" height="25" rx="8" fill="#333" />
        <circle cx="140" cy="157" r="4" fill="#DC143C" />
        <circle cx="160" cy="157" r="4" fill="#4169E1" />
      </g>
    </svg>
    
    <!-- 对话气泡 -->
    <div v-if="message" class="speech-bubble">
      <p>{{ message }}</p>
    </div>
  </div>
</template>

<script setup>
defineProps({
  mood: {
    type: String,
    default: 'normal', // normal, happy, excited, sleepy, gaming
    validator: (value) => ['normal', 'happy', 'excited', 'sleepy', 'gaming'].includes(value)
  },
  isTalking: {
    type: Boolean,
    default: false
  },
  isJumping: {
    type: Boolean,
    default: false
  },
  message: {
    type: String,
    default: ''
  }
})
</script>

<style scoped lang="scss">
.umaru-avatar {
  position: relative;
  width: 80px;
  height: 80px;
  cursor: pointer;
  transition: transform 0.3s ease;
  
  &:hover {
    transform: scale(1.1);
  }
  
  &.jumping {
    animation: jump 0.6s ease infinite;
  }
  
  &.talking {
    .umaru-svg {
      animation: shake 0.3s ease infinite;
    }
  }
}

.umaru-svg {
  width: 100%;
  height: 100%;
  filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.15));
}

@keyframes jump {
  0%, 100% {
    transform: translateY(0) scale(1);
  }
  50% {
    transform: translateY(-15px) scale(1.05);
  }
}

@keyframes shake {
  0%, 100% {
    transform: rotate(0deg);
  }
  25% {
    transform: rotate(-3deg);
  }
  75% {
    transform: rotate(3deg);
  }
}

// 对话气泡
.speech-bubble {
  position: absolute;
  bottom: 90px;
  left: 50%;
  transform: translateX(-50%);
  background: #fff;
  border-radius: 16px;
  padding: 12px 16px;
  min-width: 120px;
  max-width: 200px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  animation: bubblePop 0.3s ease;
  
  &::after {
    content: '';
    position: absolute;
    bottom: -8px;
    left: 50%;
    transform: translateX(-50%);
    border-left: 8px solid transparent;
    border-right: 8px solid transparent;
    border-top: 8px solid #fff;
  }
  
  p {
    margin: 0;
    font-size: 13px;
    color: #333;
    line-height: 1.5;
    text-align: center;
  }
}

@keyframes bubblePop {
  0% {
    opacity: 0;
    transform: translateX(-50%) scale(0.8) translateY(10px);
  }
  100% {
    opacity: 1;
    transform: translateX(-50%) scale(1) translateY(0);
  }
}

// 可乐动画
.cola {
  animation: float 1s ease infinite;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0) rotate(-5deg);
  }
  50% {
    transform: translateY(-5px) rotate(5deg);
  }
}

// 游戏手柄动画
.gamepad {
  animation: gameShake 0.2s ease infinite;
}

@keyframes gameShake {
  0%, 100% {
    transform: translateX(0);
  }
  50% {
    transform: translateX(2px);
  }
}
</style>
