<template>
  <div class="not-found">
    <span class="icon">🚧</span>
    <p class="title">页面走丢了</p>
    <p class="desc">你访问的页面不存在或已下线</p>
    <button class="back-btn" @click="router.replace('/home')">返回首页</button>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<style scoped>
.not-found {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 10px; padding: 120px 24px; min-height: 100%;
  background: var(--c-bg); text-align: center;
}
.icon { font-size: 56px; }
.title { font-size: 16px; font-weight: 600; color: var(--c-text); }
.desc { font-size: 13px; color: var(--c-text-3); }
.back-btn {
  margin-top: 10px; border: none; border-radius: 999px;
  padding: 10px 26px; font-size: 14px; font-family: inherit;
  color: #fff; cursor: pointer;
  background: linear-gradient(135deg, var(--c-brand), var(--c-brand-2));
  box-shadow: 0 8px 20px rgba(224, 30, 38, .28);
}
</style>