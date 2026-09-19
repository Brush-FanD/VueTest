const img = (id) => `https://images.unsplash.com/photo-${id}?w=400&h=300&fit=crop`

export const brandList = [
  { name: '比亚迪', icon: '🚗' }, { name: '特斯拉', icon: '⚡' }, { name: '理想', icon: '🚀' },
  { name: '问界', icon: '🏔️' }, { name: '小鹏', icon: '🪽' }, { name: '蔚来', icon: '💙' },
  { name: '小米', icon: '📱' }, { name: '大众', icon: '🏎️' }, { name: '宝马', icon: '🔵' },
  { name: '奔驰', icon: '⭐' }, { name: '奥迪', icon: '🔴' }, { name: '丰田', icon: '🌐' }
]

export const priceFilters = ['全部', '10万以下', '10-20万', '20-30万', '30-50万', '50万以上']
export const typeFilters = ['全部', '轿车', 'SUV', 'MPV', '跑车', '皮卡']
export const energyFilters = ['全部', '燃油', '纯电', '混动', '增程']

export const carList = [
  { id: 1, name: '秦L DM-i', desc: '插电混动·中型轿车', price: '9.98-13.98万', img: img('1503376780353-7e6692767b70'), tags: ['混动', '家用'], brand: '比亚迪', priceRange: '10万以下', type: '轿车', energy: '混动' },
  { id: 2, name: 'Model Y', desc: '纯电·中型SUV', price: '24.99-35.49万', img: img('1533473359331-0135ef1b58d1'), tags: ['纯电', '智能'], brand: '特斯拉', priceRange: '20-30万', type: 'SUV', energy: '纯电' },
  { id: 3, name: '理想 L9', desc: '增程·大型SUV', price: '42.98-45.98万', img: img('1554744511-d6c603f27c54'), tags: ['增程', '家庭'], brand: '理想', priceRange: '30-50万', type: 'SUV', energy: '增程' },
  { id: 4, name: '问界 M9', desc: '增程/纯电·大型SUV', price: '46.98-56.98万', img: img('1552519507-da3b142c6e3d'), tags: ['智驾', '豪华'], brand: '问界', priceRange: '30-50万', type: 'SUV', energy: '增程' },
  { id: 5, name: '小米 SU7', desc: '纯电·中大型轿车', price: '21.59-29.99万', img: img('1502877338535-766e1452684a'), tags: ['性能', '轿跑'], brand: '小米', priceRange: '20-30万', type: '轿车', energy: '纯电' },
  { id: 6, name: '小鹏 G6', desc: '纯电·中型SUV', price: '20.99-27.69万', img: img('1550355291-bbee04a92027'), tags: ['智驾', 'SUV'], brand: '小鹏', priceRange: '20-30万', type: 'SUV', energy: '纯电' },
  { id: 7, name: '蔚来 ES6', desc: '纯电·中型SUV', price: '33.80-39.60万', img: img('1494976388531-d1058494cdd8'), tags: ['换电', '豪华'], brand: '蔚来', priceRange: '30-50万', type: 'SUV', energy: '纯电' },
  { id: 8, name: '帕萨特', desc: '燃油·中型轿车', price: '18.19-25.29万', img: img('1503376780353-7e6692767b70'), tags: ['经典', '商务'], brand: '大众', priceRange: '10-20万', type: '轿车', energy: '燃油' },
  { id: 9, name: '宝马 3系', desc: '燃油·中型轿车', price: '29.99-39.99万', img: img('1552519507-da3b142c6e3d'), tags: ['豪华', '操控'], brand: '宝马', priceRange: '20-30万', type: '轿车', energy: '燃油' },
  { id: 10, name: '奔驰 GLC', desc: '燃油·中型SUV', price: '42.78-53.13万', img: img('1554744511-d6c603f27c54'), tags: ['豪华', '舒适'], brand: '奔驰', priceRange: '30-50万', type: 'SUV', energy: '燃油' },
  { id: 11, name: '奥迪 A6L', desc: '燃油·中大型轿车', price: '42.79-65.68万', img: img('1502877338535-766e1452684a'), tags: ['商务', '科技'], brand: '奥迪', priceRange: '30-50万', type: '轿车', energy: '燃油' },
  { id: 12, name: '凯美瑞', desc: '燃油/混动·中型轿车', price: '17.18-25.98万', img: img('1550355291-bbee04a92027'), tags: ['可靠', '省心'], brand: '丰田', priceRange: '10-20万', type: '轿车', energy: '混动' }
]

export const findCarById = (id) => carList.find((car) => car.id === Number(id))

/**
 * 按品牌 / 价格 / 类型 / 能源筛选，关键字用于搜索车型名、品牌与描述。
 */
export const filterCars = ({ brand, priceRange, type, energy, keyword } = {}) => {
  const text = (keyword || '').trim().toLowerCase()
  return carList.filter((car) => {
    if (brand && car.brand !== brand) return false
    if (priceRange && priceRange !== '全部' && car.priceRange !== priceRange) return false
    if (type && type !== '全部' && car.type !== type) return false
    if (energy && energy !== '全部' && car.energy !== energy) return false
    if (text) {
      const haystack = `${car.name} ${car.brand} ${car.desc} ${car.tags.join(' ')}`.toLowerCase()
      if (!haystack.includes(text)) return false
    }
    return true
  })
}
