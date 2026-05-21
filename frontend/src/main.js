import { createApp } from 'vue'
import App from './App.vue'
import ElementPlus from 'element-plus'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import 'normalize.css'
import './assets/styles/tailwind.css'
import './assets/styles/common.scss'
import router from './router/index'

// 导入全局工具
import { message, notify, confirm } from './utils/message'
import { request } from './utils/request'

const app = createApp(App)

// 注册 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 使用插件
app.use(ElementPlus)
app.use(Antd)
app.use(router)

// 挂载全局工具方法
app.config.globalProperties.$message = message
app.config.globalProperties.$notify = notify
app.config.globalProperties.$confirm = confirm
app.config.globalProperties.$request = request

// 挂载应用
app.mount('#app')

// 导出工具供外部使用
export { message, notify, confirm }
