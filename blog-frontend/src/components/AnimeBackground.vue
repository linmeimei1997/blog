<template>
  <div class="anime-background">
    <!-- 天空渐变 -->
    <div class="sky"></div>
    
    <!-- 云朵 -->
    <div class="clouds">
      <div v-for="i in 5" :key="i" class="cloud" :class="`cloud-${i}`"></div>
    </div>
    
    <!-- 樱花飘落 -->
    <div class="sakura-container">
      <div v-for="i in 30" :key="`sakura-${i}`" class="sakura" :style="getSakuraStyle(i)"></div>
    </div>
    
    <!-- 二次元角色剪影 -->
    <div class="anime-characters">
      <!-- 左侧角色 -->
      <div class="character character-left">
        <svg viewBox="0 0 200 400" class="character-svg">
          <!-- 头发 -->
          <path d="M60,80 Q40,120 35,180 Q30,250 50,300 L150,300 Q170,250 165,180 Q160,120 140,80 Q100,60 60,80" fill="#FFE4E1"/>
          <path d="M35,180 Q20,220 25,280 Q30,320 50,350" fill="#FFB6C1"/>
          <path d="M165,180 Q180,220 175,280 Q170,320 150,350" fill="#FFB6C1"/>
          <!-- 身体 -->
          <ellipse cx="100" cy="320" rx="45" ry="70" fill="#E6E6FA"/>
          <!-- 头部 -->
          <ellipse cx="100" cy="150" rx="50" ry="55" fill="#FFE4E1"/>
          <!-- 眼睛 -->
          <ellipse cx="80" cy="145" rx="8" ry="10" fill="#333"/>
          <ellipse cx="120" cy="145" rx="8" ry="10" fill="#333"/>
          <circle cx="82" cy="143" r="3" fill="#fff"/>
          <circle cx="122" cy="143" r="3" fill="#fff"/>
          <!-- 嘴巴 -->
          <path d="M90,170 Q100,175 110,170" fill="none" stroke="#FF69B4" stroke-width="2"/>
          <!-- 蝴蝶结 -->
          <path d="M85,80 L70,70 L85,90 L100,80 L115,90 L130,70 L115,80" fill="#FF69B4"/>
        </svg>
      </div>
      
      <!-- 右侧角色 -->
      <div class="character character-right">
        <svg viewBox="0 0 200 400" class="character-svg">
          <!-- 头发 -->
          <path d="M50,70 Q30,100 25,150 Q20,200 40,250 L160,250 Q180,200 175,150 Q170,100 150,70 Q100,50 50,70" fill="#87CEEB"/>
          <!-- 呆毛 -->
          <path d="M100,50 Q110,20 120,30 Q110,40 100,50" fill="#87CEEB"/>
          <!-- 身体 -->
          <ellipse cx="100" cy="300" rx="40" ry="65" fill="#98FB98"/>
          <!-- 头部 -->
          <ellipse cx="100" cy="140" rx="48" ry="52" fill="#FFE4E1"/>
          <!-- 眼睛 -->
          <ellipse cx="75" cy="135" rx="7" ry="9" fill="#333"/>
          <ellipse cx="125" cy="135" rx="7" ry="9" fill="#333"/>
          <circle cx="77" cy="133" r="2.5" fill="#fff"/>
          <circle cx="127" cy="133" r="2.5" fill="#fff"/>
          <!-- 嘴巴 -->
          <ellipse cx="100" cy="165" rx="6" ry="4" fill="#FF69B4"/>
        </svg>
      </div>
    </div>
    
    <!-- 地面 -->
    <div class="ground"></div>
    
    <!-- 星星闪烁 -->
    <div class="stars">
      <div v-for="i in 20" :key="`star-${i}`" class="star" :style="getStarStyle(i)"></div>
    </div>
  </div>
</template>

<script setup>
const getSakuraStyle = (i) => {
  const left = Math.random() * 100
  const delay = Math.random() * 5
  const duration = 3 + Math.random() * 4
  const size = 10 + Math.random() * 15
  
  return {
    left: `${left}%`,
    animationDelay: `${delay}s`,
    animationDuration: `${duration}s`,
    width: `${size}px`,
    height: `${size}px`
  }
}

const getStarStyle = (i) => {
  const left = Math.random() * 100
  const top = Math.random() * 60
  const delay = Math.random() * 3
  const size = 2 + Math.random() * 3
  
  return {
    left: `${left}%`,
    top: `${top}%`,
    animationDelay: `${delay}s`,
    width: `${size}px`,
    height: `${size}px`
  }
}
</script>

<style scoped lang="scss">
.anime-background {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  z-index: 0;
}

// 天空渐变
.sky {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(180deg, 
    #FFB6C1 0%, 
    #FFC0CB 20%, 
    #FFE4E1 40%, 
    #FFF0F5 60%, 
    #E6E6FA 80%, 
    #DDA0DD 100%
  );
}

// 云朵
.clouds {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.cloud {
  position: absolute;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 100px;
  animation: float-cloud linear infinite;
  
  &::before,
  &::after {
    content: '';
    position: absolute;
    background: rgba(255, 255, 255, 0.8);
    border-radius: 100px;
  }
}

.cloud-1 {
  width: 100px;
  height: 40px;
  top: 10%;
  left: -100px;
  animation-duration: 25s;
  
  &::before {
    width: 50px;
    height: 50px;
    top: -25px;
    left: 10px;
  }
  
  &::after {
    width: 60px;
    height: 40px;
    top: -15px;
    right: 10px;
  }
}

.cloud-2 {
  width: 80px;
  height: 35px;
  top: 25%;
  left: -80px;
  animation-duration: 30s;
  animation-delay: 5s;
  
  &::before {
    width: 40px;
    height: 40px;
    top: -20px;
    left: 15px;
  }
  
  &::after {
    width: 50px;
    height: 35px;
    top: -10px;
    right: 15px;
  }
}

.cloud-3 {
  width: 120px;
  height: 45px;
  top: 15%;
  left: -120px;
  animation-duration: 35s;
  animation-delay: 10s;
  
  &::before {
    width: 60px;
    height: 60px;
    top: -30px;
    left: 20px;
  }
  
  &::after {
    width: 70px;
    height: 45px;
    top: -20px;
    right: 20px;
  }
}

.cloud-4 {
  width: 90px;
  height: 38px;
  top: 35%;
  left: -90px;
  animation-duration: 28s;
  animation-delay: 15s;
}

.cloud-5 {
  width: 110px;
  height: 42px;
  top: 5%;
  left: -110px;
  animation-duration: 32s;
  animation-delay: 20s;
}

@keyframes float-cloud {
  from {
    transform: translateX(0);
  }
  to {
    transform: translateX(calc(100vw + 200px));
  }
}

// 樱花飘落
.sakura-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.sakura {
  position: absolute;
  top: -20px;
  background: radial-gradient(circle, #FFB6C1 0%, #FFC0CB 50%, transparent 70%);
  border-radius: 50% 0 50% 50%;
  opacity: 0.8;
  animation: fall-sakura linear infinite;
  
  &::before {
    content: '';
    position: absolute;
    width: 100%;
    height: 100%;
    background: radial-gradient(circle, #FFC0CB 0%, transparent 70%);
    border-radius: 0 50% 50% 50%;
    transform: rotate(45deg);
  }
}

@keyframes fall-sakura {
  0% {
    transform: translateY(0) rotate(0deg) translateX(0);
    opacity: 0.8;
  }
  25% {
    transform: translateY(25vh) rotate(90deg) translateX(20px);
  }
  50% {
    transform: translateY(50vh) rotate(180deg) translateX(-20px);
  }
  75% {
    transform: translateY(75vh) rotate(270deg) translateX(20px);
    opacity: 0.6;
  }
  100% {
    transform: translateY(100vh) rotate(360deg) translateX(0);
    opacity: 0;
  }
}

// 二次元角色
.anime-characters {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.character {
  position: absolute;
  bottom: 10%;
  width: 200px;
  height: 400px;
  animation: breathe 3s ease-in-out infinite;
  
  &.character-left {
    left: 5%;
    animation-delay: 0s;
  }
  
  &.character-right {
    right: 5%;
    animation-delay: 1.5s;
  }
}

.character-svg {
  width: 100%;
  height: 100%;
  filter: drop-shadow(0 10px 20px rgba(0, 0, 0, 0.2));
}

@keyframes breathe {
  0%, 100% {
    transform: translateY(0) scale(1);
  }
  50% {
    transform: translateY(-10px) scale(1.02);
  }
}

// 地面
.ground {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 20%;
  background: linear-gradient(180deg, 
    transparent 0%, 
    rgba(144, 238, 144, 0.3) 30%,
    rgba(144, 238, 144, 0.6) 100%
  );
}

// 星星
.stars {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.star {
  position: absolute;
  background: #fff;
  border-radius: 50%;
  animation: twinkle 2s ease-in-out infinite;
  box-shadow: 0 0 10px rgba(255, 255, 255, 0.8);
}

@keyframes twinkle {
  0%, 100% {
    opacity: 0.3;
    transform: scale(1);
  }
  50% {
    opacity: 1;
    transform: scale(1.2);
  }
}

// 手机端适配
@media (max-width: 768px) {
  .character {
    width: 120px;
    height: 240px;
    
    &.character-left {
      left: -30px;
    }
    
    &.character-right {
      right: -30px;
    }
  }
  
  .sakura {
    opacity: 0.5;
  }
}
</style>
