<template>
  <canvas ref="canvasRef" class="wave-canvas"></canvas>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const canvasRef = ref(null)
let animationId = null

onMounted(() => {
  const canvas = canvasRef.value
  const ctx = canvas.getContext('2d')
  
  let width = canvas.width = canvas.offsetWidth
  let height = canvas.height = canvas.offsetHeight
  
  let waves = []
  const waveCount = 3
  
  class Wave {
    constructor(index, total) {
      this.index = index
      this.total = total
      this.phase = index * Math.PI / total
      this.amplitude = 30 + index * 15
      this.frequency = 0.01 + index * 0.005
      this.speed = 0.02 + index * 0.01
      this.color = this.getColor()
    }
    
    getColor() {
      const colors = [
        'rgba(102, 126, 234, 0.3)',
        'rgba(118, 75, 162, 0.2)',
        'rgba(240, 147, 251, 0.15)'
      ]
      return colors[this.index % colors.length]
    }
    
    draw(ctx, time) {
      ctx.fillStyle = this.color
      ctx.beginPath()
      ctx.moveTo(0, height)
      
      for (let x = 0; x <= width; x += 5) {
        const y = height / 2 + 
          Math.sin(x * this.frequency + time * this.speed + this.phase) * this.amplitude +
          Math.sin(x * this.frequency * 2 + time * this.speed * 1.5) * (this.amplitude / 2)
        ctx.lineTo(x, y)
      }
      
      ctx.lineTo(width, height)
      ctx.lineTo(0, height)
      ctx.closePath()
      ctx.fill()
    }
  }
  
  // 初始化波浪
  for (let i = 0; i < waveCount; i++) {
    waves.push(new Wave(i, waveCount))
  }
  
  let time = 0
  const animate = () => {
    ctx.clearRect(0, 0, width, height)
    
    // 绘制渐变背景
    const gradient = ctx.createLinearGradient(0, 0, 0, height)
    gradient.addColorStop(0, 'rgba(102, 126, 234, 0.05)')
    gradient.addColorStop(0.5, 'rgba(118, 75, 162, 0.03)')
    gradient.addColorStop(1, 'rgba(240, 147, 251, 0.05)')
    ctx.fillStyle = gradient
    ctx.fillRect(0, 0, width, height)
    
    // 绘制波浪
    waves.forEach(wave => wave.draw(ctx, time))
    
    time += 1
    animationId = requestAnimationFrame(animate)
  }
  
  animate()
  
  // 响应式
  const handleResize = () => {
    width = canvas.width = canvas.offsetWidth
    height = canvas.height = canvas.offsetHeight
  }
  
  window.addEventListener('resize', handleResize)
  
  onUnmounted(() => {
    window.removeEventListener('resize', handleResize)
    if (animationId) cancelAnimationFrame(animationId)
  })
})
</script>

<style scoped>
.wave-canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
}
</style>
