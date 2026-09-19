<template>
  <div class="car-page">
    <div class="car-search">
      <div class="car-search-bar">
        <span>🔍</span>
        <input
          v-model="keyword"
          class="car-search-input"
          placeholder="搜索品牌或车型..."
        />
      </div>
    </div>

    <div class="section">
      <div class="section-header">
        <span>热门品牌</span>
      </div>
      <div class="brand-scroll">
        <div v-for="brand in brandList" :key="brand.name" class="brand-item" :class="{ active: selectedBrand === brand.name }" @click="selectBrand(brand.name)">
          <div class="brand-avatar">{{ brand.icon }}</div>
          <span class="brand-name">{{ brand.name }}</span>
        </div>
      </div>
    </div>

    <div class="section">
      <div class="filter-row"><span class="filter-label">价格</span>
        <div class="filter-scroll">
          <span v-for="p in priceFilters" :key="p" class="filter-chip" :class="{ active: selectedPrice === p }" @click="selectedPrice = p">{{ p }}</span>
        </div>
      </div>
      <div class="filter-row"><span class="filter-label">类型</span>
        <div class="filter-scroll">
          <span v-for="t in typeFilters" :key="t" class="filter-chip" :class="{ active: selectedType === t }" @click="selectedType = t">{{ t }}</span>
        </div>
      </div>
      <div class="filter-row"><span class="filter-label">能源</span>
        <div class="filter-scroll">
          <span v-for="e in energyFilters" :key="e" class="filter-chip" :class="{ active: selectedEnergy === e }" @click="selectedEnergy = e">{{ e }}</span>
        </div>
      </div>
    </div>

    <div class="section result-section">
      <div class="section-header">
        <span>共 {{ filteredCars.length }} 款车型</span>
        <span class="section-link" @click="resetFilters">重置筛选</span>
      </div>
      <div v-if="filteredCars.length" class="car-grid">
        <div v-for="car in filteredCars" :key="car.id" class="car-card" @click="goDetail(car)">
          <div class="car-img-box">
            <img :src="car.img" :alt="car.name" loading="lazy" @error="onImgError($event)" />
            <span class="car-emoji-bg">🚗</span>
            <span class="car-badge">{{ car.price }}</span>
          </div>
          <div class="car-info">
            <div class="car-name">{{ car.name }}</div>
            <div class="car-sub">{{ car.desc }}</div>
            <div class="car-tags">
              <span v-for="t in car.tags" :key="t" class="car-tag">{{ t }}</span>
            </div>
          </div>
        </div>
      </div>
      <div v-else class="empty-state">
        <span style="font-size:40px">🚗</span>
        <p>没有找到匹配的车型</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { brandList, energyFilters, filterCars, priceFilters, typeFilters } from '../data/cars'

const router = useRouter()

const keyword = ref('')
const selectedBrand = ref('')
const selectedPrice = ref('全部')
const selectedType = ref('全部')
const selectedEnergy = ref('全部')

const filteredCars = computed(() => filterCars({
  brand: selectedBrand.value,
  priceRange: selectedPrice.value,
  type: selectedType.value,
  energy: selectedEnergy.value,
  keyword: keyword.value
}))

const selectBrand = (name) => { selectedBrand.value = selectedBrand.value === name ? '' : name }

const resetFilters = () => {
  keyword.value = ''
  selectedBrand.value = ''
  selectedPrice.value = '全部'
  selectedType.value = '全部'
  selectedEnergy.value = '全部'
}

const goDetail = (car) => { router.push({ name: 'CarDetail', params: { carId: car.id } }) }

// 图片加载失败时显示备用
const onImgError = (e) => {
  e.target.style.display = 'none'
  const parent = e.target.parentNode
  if (!parent.querySelector('.img-fallback')) {
    const fb = document.createElement('span')
    fb.className = 'img-fallback'
    fb.textContent = '🚗'
    fb.style.cssText = 'font-size:48px;position:absolute;inset:0;display:flex;align-items:center;justify-content:center;'
    parent.appendChild(fb)
  }
}

</script>

<style scoped>
.car-page { padding-bottom: 16px; }

.car-search { padding: 14px 12px 2px; }
.car-search-bar {
  display: flex; align-items: center; gap: 8px;
  background: #fff; border-radius: 999px;
  padding: 10px 16px; box-shadow: var(--sh-1);
}
.car-search-bar span { font-size: 14px; color: var(--c-text-3); }
.car-search-input {
  flex: 1; min-width: 0; border: none; outline: none;
  font-size: 14px; font-family: inherit; color: var(--c-text);
}

.section { margin: 14px 12px 0; }
.section-header {
  display: flex; align-items: center; gap: 6px;
  margin-bottom: 10px; font-size: 15px; font-weight: 700;
}
.section-link { margin-left: auto; font-size: 12px; font-weight: 400; color: var(--c-brand); cursor: pointer; }

.brand-scroll {
  display: flex; gap: 10px; overflow-x: auto; padding: 2px 0 6px;
}
.brand-item {
  flex-shrink: 0; width: 58px;
  display: flex; flex-direction: column; align-items: center; gap: 5px;
  cursor: pointer;
}
.brand-avatar {
  width: 44px; height: 44px; border-radius: 16px;
  display: flex; align-items: center; justify-content: center;
  font-size: 20px; background: #fff; box-shadow: var(--sh-1);
  transition: transform .15s ease, box-shadow .15s ease, background .15s ease;
}
.brand-item:active .brand-avatar { transform: scale(.94); }
.brand-item.active .brand-avatar {
  background: linear-gradient(135deg, var(--c-brand), var(--c-brand-2));
  box-shadow: 0 6px 16px rgba(224, 30, 38, .3);
}
.brand-name { font-size: 11px; color: var(--c-text-2); }
.brand-item.active .brand-name { color: var(--c-brand); font-weight: 600; }

.filter-row { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.filter-label { flex-shrink: 0; width: 28px; font-size: 12px; color: var(--c-text-3); }
.filter-scroll { display: flex; gap: 6px; overflow-x: auto; padding-bottom: 2px; }
.filter-chip {
  flex-shrink: 0; white-space: nowrap; cursor: pointer;
  font-size: 12px; padding: 5px 12px; border-radius: 999px;
  background: #fff; color: var(--c-text-2);
  border: 1px solid var(--c-line);
  transition: background .15s ease, color .15s ease;
}
.filter-chip.active {
  background: var(--c-brand-soft); color: var(--c-brand);
  border-color: var(--c-brand-line); font-weight: 600;
}

.result-section { margin-top: 16px; }
.car-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; }
.car-card {
  background: #fff; border-radius: var(--r-lg); overflow: hidden;
  box-shadow: var(--sh-1); cursor: pointer;
  transition: transform .15s ease, box-shadow .15s ease;
}
.car-card:active { transform: scale(.975); box-shadow: var(--sh-2); }
.car-img-box { position: relative; aspect-ratio: 4 / 3; background: #E8EAEE; overflow: hidden; }
.car-img-box img { position: relative; z-index: 1; width: 100%; height: 100%; object-fit: cover; display: block; }
.car-emoji-bg {
  position: absolute; inset: 0; z-index: 0;
  display: flex; align-items: center; justify-content: center;
  font-size: 34px; background: linear-gradient(135deg, #E8EAEE, #DDE1E7);
}
.car-badge {
  position: absolute; left: 8px; bottom: 8px; z-index: 2;
  font-size: 11px; font-weight: 600; color: #fff;
  padding: 3px 9px; border-radius: 999px;
  background: rgba(0, 0, 0, .48);
  backdrop-filter: blur(4px);
}
.car-info { padding: 9px 10px 11px; }
.car-name { font-size: 13px; font-weight: 600; }
.car-sub { margin-top: 4px; font-size: 11px; color: var(--c-text-3); }
.car-tags { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 7px; }
.car-tag {
  font-size: 10px; padding: 2px 7px; border-radius: 5px;
  color: var(--c-text-2); background: #F1F3F7;
}
.empty-state {
  display: flex; flex-direction: column; align-items: center; gap: 10px;
  padding: 60px 24px; color: var(--c-text-3); font-size: 13px;
}
</style>