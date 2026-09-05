// 后端接口地址：从 .env.development / .env.production 读取 VITE_API_BASE_URL，
// 改环境只改 env 文件，不再动源码（找不到时回退 localhost，保证冷启动可用）：
// - npm run dev:mp-weixin  → 读 .env.development（模拟器/真机预览）
// - npm run build:mp-weixin → 读 .env.production（生产构建，发布前改成备案域名）
//
// 真机联调排错清单（连不上/头像不显示时按序检查）：
// 1. 手机与电脑在同一 Wi-Fi 网段（校园网/公司网常开启 AP 隔离，会导致互 ping 不通，换手机热点即可）
// 2. env 文件里的 IP 与 ipconfig 查到的 IPv4 地址一致（电脑 IP 变了这里要跟着改）
// 3. Windows 防火墙可能拦截 9091 入站：控制面板 → 防火墙 → 高级设置 → 入站规则放行 9091 端口
// 4. 微信开发者工具勾选「不校验合法域名」，真机调试打开「调试」开关
//
// 头像等文件地址不用关心主机部分：数据库里的老数据（http://localhost:9091/files/xxx）
// 与新上传的相对路径（/api/files/xxx）都会经 request.js 的 resolveFileUrl() 归一成当前 baseUrl 完整地址
export const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:9091'
