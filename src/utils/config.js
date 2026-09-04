// 后端接口地址：
// - 微信开发者工具模拟器：http://localhost:9091
// - 真机预览/真机调试：改为电脑的局域网 IP（ipconfig 查看），手机与电脑必须同一 Wi-Fi
//
// 真机联调排错清单（连不上/头像不显示时按序检查）：
// 1. 手机与电脑在同一 Wi-Fi 网段（校园网/公司网常开启 AP 隔离，会导致互 ping 不通，换手机热点即可）
// 2. baseUrl 与 ipconfig 查到的 IPv4 地址一致（电脑 IP 变了这里要跟着改）
// 3. Windows 防火墙可能拦截 9091 入站：控制面板 → 防火墙 → 高级设置 → 入站规则放行 9091 端口
// 4. 微信开发者工具勾选「不校验合法域名」，真机调试打开「调试」开关
//
// 头像等文件地址不用关心主机部分：数据库里的老数据（http://localhost:9091/files/xxx）
// 与新上传的相对路径（/api/files/xxx）都会经 request.js 的 resolveFileUrl() 归一成当前 baseUrl 完整地址
export const baseUrl = 'http://192.168.1.10:9091'
