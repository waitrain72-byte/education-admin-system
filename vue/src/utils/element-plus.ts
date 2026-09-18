/**
 * Element Plus 运行时导入的唯一收敛点。
 *
 * 背景：`import { ElMessage } from 'element-plus'` 这类「桶导入」会把整个
 * element-plus/es 索引拉进依赖图，使 tree-shaking 失效——实测打包出的
 * element-plus chunk 达 824 KB，内含 145 个组件定义（ElCascader、ElTransfer、
 * ElTour、ElWatermark…），而本项目实际只用到约 36 个。
 *
 * vite.config.ts 里的 unplugin-vue-components 只能改写**模板**中的标签，
 * 管不到 JS 里的值导入，因此这些命令式 API 必须显式走深导入。
 * 深导入路径属于 Element Plus 的内部结构，集中在此便于日后升级时统一调整。
 *
 * 样式不需要在这里引：main.ts 已统一引入 element-plus/dist/index.css。
 */
export { ElMessage } from 'element-plus/es/components/message/index'
export { ElMessageBox } from 'element-plus/es/components/message-box/index'
export { ElNotification } from 'element-plus/es/components/notification/index'
export { ElDropdown, ElDropdownMenu, ElDropdownItem } from 'element-plus/es/components/dropdown/index'
