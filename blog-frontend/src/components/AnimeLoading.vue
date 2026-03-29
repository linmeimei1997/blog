<template>
  <div class="anime-loading">
    <div class="loading-container">
      <!-- 跑步的二次元角色 -->
      <div class="running-character">
        <svg viewBox="0 0 100 120" class="character-svg">
          <!-- 头发（飘动） -->
          <path class="hair" d="M30,25 Q20,35 15,50 Q10,65 20,75 L80,75 Q90,65 85,50 Q80,35 70,25 Q50,15 30,25" fill="#FFE4E1"/>
          <path class="hair-back" d="M15,50 Q5,60 10,75 Q15,90 25,85" fill="#FFB6C1"/>
          
          <!-- 身体 -->
          <ellipse cx="50" cy="85" rx="20" ry="25" fill="#E6E6FA"/>
          
          <!-- 头部 -->
          <ellipse cx="50" cy="45" rx="22" ry="24" fill="#FFE4E1"/>
          
          <!-- 眼睛（跑步时的表情） -->
          <ellipse cx="40" cy="42" rx="4" ry="5" fill="#333"/>
          <ellipse cx="60" cy="42" rx="4" ry="5" fill="#333"/>
          <circle cx="41" cy="41" r="1.5" fill="#fff"/>
          <circle cx="61" cy="41" r="1.5" fill="#fff"/>
          
          <!-- 嘴巴（喘气） -->
          <ellipse cx="50" cy="55" rx="3" ry="4" fill="#FF69B4"/>
          
          <!-- 腮红 -->
          <ellipse cx="32" cy="50" rx="4" ry="3" fill="#FFB6C1" opacity="0.6"/>
          <ellipse cx="68" cy="50" rx="4" ry="3" fill="#FFB6C1" opacity="0.6"/>
          
          <!-- 手臂（跑步摆动） -->
          <path class="arm-left" d="M35,75 Q20,70 15,60" fill="none" stroke="#FFE4E1" stroke-width="6" stroke-linecap="round"/>
          <path class="arm-right" d="M65,75 Q80,70 85,60" fill="none" stroke="#FFE4E1" stroke-width="6" stroke-linecap="round"/>
          
          <!-- 腿部（跑步动作） -->
          <path class="leg-left" d="M40,105 Q35,115 30,110" fill="none" stroke="#FFE4E1" stroke-width="7" stroke-linecap="round"/>
          <path class="leg-right" d="M60,105 Q70,115 75,110" fill="none" stroke="#FFE4E1" stroke-width="7" stroke-linecap="round"/>
          
          <!-- 鞋子 -->
          <ellipse cx="28" cy="112" rx="6" ry="4" fill="#FF69B4"/>
          <ellipse cx="77" cy="112" rx="6" ry="4" fill="#FF69B4"/>
        </svg>
      </div>
      
      <!-- 跑动效果 -->
      <div class="running-effects">
        <div class="dust dust-1"></div>
        <div class="dust dust-2"></div>
        <div class="dust dust-3"></div>
        <div class="speed-line line-1"></div>
        <div class="speed-line line-2"></div>
        <div class="speed-line line-3"></div>
      </div>
      
      <!-- 加载文字 -->
      <div class="loading-text">
        <span class="text">{{ text }}</span>
        <span class="dots">
          <span class="dot">.</span>
          <span class="dot">.</span>
          <span class="dot">.</span>
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  text: {
    type: String,
    default: '正在登录'
  }
})
</script>

<style scoped lang="scss">
.anime-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 200px;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

// 跑步角色
.running-character {
  position: relative;
  width: 100px;
  height: 120px;
  animation: bounce 0.5s ease-in-out infinite;
}

.character-svg {
  width: 100%;
  height: 100%;
  filter: drop-shadow(0 5px 10px rgba(0, 0, 0, 0.2));
}

// 头发飘动动画
.hair {
  animation: hair-wave 0.3s ease-in-out infinite;
  transform-origin: center;
}

.hair-back {
  animation: hair-wave 0.3s ease-in-out infinite reverse;
  transform-origin: center;
}

@keyframes hair-wave {
  0%, 100% {
    transform: translateX(0);
  }
  50% {
    transform: translateX(-3px);
  }
}

// 手臂摆动
.arm-left {
  animation: arm-swing-left 0.5s ease-in-out infinite;
  transform-origin: 35px 75px;
}

.arm-right {
  animation: arm-swing-right 0.5s ease-in-out infinite;
  transform-origin: 65px 75px;
}

@keyframes arm-swing-left {
  0%, 100% {
    transform: rotate(-20deg);
  }
  50% {
    transform: rotate(20deg);
  }
}

@keyframes arm-swing-right {
  0%, 100% {
    transform: rotate(20deg);
  }
  50% {
    transform: rotate(-20deg);
  }
}

// 腿部跑动
.leg-left {
  animation: leg-run-left 0.5s ease-in-out infinite;
  transform-origin: 40px 105px;
}

.leg-right {
  animation: leg-run-right 0.5s ease-in-out infinite;
  transform-origin: 60px 105px;
}

@keyframes leg-run-left {
  0%, 100% {
    transform: rotate(-30deg);
  }
  50% {
    transform: rotate(30deg);
  }
}

@keyframes leg-run-right {
  0%, 100% {
    transform: rotate(30deg);
  }
  50% {
    transform: rotate(-30deg);
  }
}

// 身体弹跳
@keyframes bounce {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-8px);
  }
}

// 跑动效果
.running-effects {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  width: 150px;
  height: 50px;
}

.dust {
  position: absolute;
  bottom: 0;
  width: 8px;
  height: 8px;
  background: rgba(200, 200, 200, 0.5);
  border-radius: 50%;
  animation: dust-fade 0.6s ease-out infinite;
}

.dust-1 {
  left: 20%;
  animation-delay: 0s;
}

.dust-2 {
  left: 50%;
  animation-delay: 0.2s;
}

.dust-3 {
  left: 80%;
  animation-delay: 0.4s;
}

@keyframes dust-fade {
  0% {
    transform: translateX(0) scale(1);
    opacity: 0.6;
  }
  100% {
    transform: translateX(-30px) scale(0.5);
    opacity: 0;
  }
}

.speed-line {
  position: absolute;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(100, 100, 100, 0.3), transparent);
  animation: speed-move 0.4s linear infinite;
}

.line-1 {
  width: 40px;
  top: 30%;
  left: -20px;
  animation-delay: 0s;
}

.line-2 {
  width: 60px;
  top: 50%;
  left: -30px;
  animation-delay: 0.15s;
}

.line-3 {
  width: 50px;
  top: 70%;
  left: -25px;
  animation-delay: 0.3s;
}

@keyframes speed-move {
  0% {
    transform: translateX(0);
    opacity: 0;
  }
  50% {
    opacity: 1;
  }
  100% {
    transform: translateX(-50px);
    opacity: 0;
  }
}

// 加载文字
.loading-text {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 16px;
  color: #FF69B4;
  font-weight: 600;
}

.text {
  background: linear-gradient(135deg, #FF69B4 0%, #FF1493 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.dots {
  display: flex;
}

.dot {
  animation: dot-bounce 1.4s ease-in-out infinite;
  
  &:nth-child(1) {
    animation-delay: 0s;
  }
  
  &:nth-child(2) {
    animation-delay: 0.2s;
  }
  
  &:nth-child(3) {
    animation-delay: 0.4s;
  }
}

@keyframes dot-bounce {
  0%, 60%, 100% {
    transform: translateY(0);
  }
  30% {
    transform: translateY(-10px);
  }
}

// 手机端适配
@media (max-width: 768px) {
  .running-character {
    width: 80px;
    height: 96px;
  }
  
  .loading-text {
    font-size: 14px;
  }
}
</style>
