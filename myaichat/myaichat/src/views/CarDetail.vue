<template>
  <div class="car-detail">
    <div class="detail-head">
      <button class="back" @click="goBack">‹</button>
      <span class="head-title">{{ car ? car.name : '车型详情' }}</span>
    </div>

    <template v-if="car">
      <div class="hero">
        <img :src="car.img" :alt="car.name" @error="hideImg" />
        <span class="hero-emoji">🚗</span>
      </div>

      <div class="card">
        <h1 class="car-name">{{ car.name }}</h1>
        <p class="car-desc">{{ car.desc }}</p>
        <p class="car-price">{{ car.price }}</p>
        <div class="tags">
          <span v-for="tag in car.tags" :key="tag" class="tag">{{ tag }}</span>
        </div>
      </div>

      <div class="card">
        <div class="card-title">基本信息</div>
        <div class="spec-row"><span>品牌</span><span>{{ car.brand }}</span></div>
        <div class="spec-row"><span>车型级别</span><span>{{ car.type }}</span></div>
        <div class="spec-row"><span>能源类型</span><span>{{ car.energy }}</span></div>
        <div class="spec-row"><span>价格区间</span><span>{{ car.priceRange }}</span></div>
      </div>

      <div class="card">
        <button class="ask-ai" @click="askAi">🤖 问懂车AI：这车值得买吗？</button>
      </div>
    </template>

    <div v-else class="empty">
      <span style="font-size: 48px">🚗</span>
      <p>没有找到这个车型</p>
      <button class="back-btn" @click="router.replace('/cars')">返回选车</button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { findCarById } from '../data/cars'

const route = useRoute()
const router = useRouter()

const car = computed(() => findCarById(route.params.carId))

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.replace('/cars')
  }
}

const hideImg = (event) => {
  event.target.style.display = 'none'
}

/** 带着车型问题跳到 AI 助手页，避免这里再写一套对话逻辑 */
const askAi = () => {
  router.push({ path: '/ai', query: { q: `${car.value.brand}${car.value.name} 值不值得买？` } })
}
</script>

<style scoped>
.car-detail { padding-bottom: 16px; }

.detail-head {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 12px; background: #fff;
  border-bottom: 1px solid var(--c-line);
  position: sticky; top: 0; z-index: 10;
}
.back {
  width: 32px; height: 32px; border-radius: 10px;
  border: none; background: #F4F5F7; color: var(--c-text);
  font-size: 18px; line-height: 1; cursor: pointer;
}
.head-title { flex: 1; font-size: 15px; font-weight: 600; }

.hero {
  position: relative; aspect-ratio: 16 / 10;
  background: linear-gradient(135deg, #E8EAEE, #DDE1E7);
  overflow: hidden; padding: 0;
}
.hero::after { display: none; }
.hero img { position: relative; z-index: 1; width: 100%; height: 100%; object-fit: cover; display: block; }
.hero-emoji {
  position: absolute; inset: 0; z-index: 0;
  display: flex; align-items: center; justify-content: center; font-size: 56px;
}

.card {
  margin: 12px; padding: 14px;
  background: #fff; border-radius: var(--r-lg); box-shadow: var(--sh-1);
}
.car-name { font-size: 18px; font-weight: 700; }
.car-desc { margin-top: 6px; font-size: 13px; line-height: 1.6; color: var(--c-text-2); }
.car-price { margin-top: 8px; font-size: 18px; font-weight: 800; color: var(--c-brand); }
.tags { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 10px; }
.tag {
  font-size: 11px; padding: 3px 9px; border-radius: 999px;
  color: var(--c-brand); background: var(--c-brand-soft);
}

.card-title { font-size: 14px; font-weight: 700; margin-bottom: 10px; }
.spec-row {
  display: flex; justify-content: space-between;
  padding: 9px 0; font-size: 13px;
  border-bottom: 1px solid var(--c-line);
}
.spec-row:last-child { border-bottom: none; }
.spec-row span:first-child { color: var(--c-text-3); }

.ask-ai {
  width: 100%; border: none; border-radius: var(--r-md);
  padding: 13px; font-size: 14px; font-family: inherit;
  color: #fff; cursor: pointer;
  background: linear-gradient(135deg, var(--c-brand), var(--c-brand-2));
  box-shadow: 0 8px 20px rgba(224, 30, 38, .28);
}
.ask-ai:active { transform: scale(.98); }

.empty {
  display: flex; flex-direction: column; align-items: center; gap: 10px;
  padding: 100px 24px; color: var(--c-text-3); font-size: 13px;
}
.back-btn {
  margin-top: 8px; border: none; border-radius: 999px;
  padding: 9px 24px; font-size: 14px; font-family: inherit;
  color: #fff; cursor: pointer;
  background: linear-gradient(135deg, var(--c-brand), var(--c-brand-2));
}
</style>