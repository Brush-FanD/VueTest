import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './styles/theme.css'
import App from './App.vue'
import router from './router'

const app = createApp(App)

// 图标按需在组件内 import 即可，全量注册会让打包体积凭空多出几百 KB
app.use(ElementPlus)
app.use(router)

app.mount('#app')