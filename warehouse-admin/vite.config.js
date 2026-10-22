import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { viteSingleFile } from 'vite-plugin-singlefile'

/**
 * 单文件构建说明（支持 file:// 双击打开）：
 *
 * - viteSingleFile() 把 JS / CSS 全部内联进 index.html，页面不再发起任何外部资源请求，
 *   因此不会被浏览器对 file:// 的跨域策略拦截。
 * - 内联脚本必须是 <script type="module">（插件默认行为），原因有两点：
 *     1) Vite/Rollup 的 ESM 产物依赖模块作用域：改成普通 script 会落到全局作用域，
 *        触发 "Identifier '$' has already been declared" 之类的重复声明错误。
 *     2) module 脚本自带 defer 语义，会在 DOM 解析完成后再执行，
 *        保证 Vue 的 mount('#app') 能找到目标元素。
 *   内联 module 没有 fetch 过程，所以 file:// 下可以正常执行。
 * - 路由使用 hash 模式、接口使用绝对地址（见 src/router、src/utils/request.js），
 *   这三者共同支撑“双击 html 即可打开”。
 */
export default defineConfig({
  base: './',
  plugins: [vue(), viteSingleFile()],
  server: {
    port: 5174,
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  }
})
