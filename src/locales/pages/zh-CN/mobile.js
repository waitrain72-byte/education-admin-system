/**
 * 小程序专用词条：底部标签栏、移动端特有的页面与提示；
 * 以及 Web 端核心包里改版后新增、小程序核心包还没有的登录 / 注册词条（键名与 Web 端一致）。
 * 与 shell / space / campus / console（从 Web 端同步过来的模块）深合并。
 */
export default {
  tab: {
    home: '首页',
    courses: '课程',
    schedule: '日程',
    console: '后台',
    messages: '消息',
    mine: '我的',
  },

  login: {
    tagline: '课程、日程、消息，一处办好',
    heading: '登录',
    hint: '输入账号和密码即可，系统会自动识别你是学生、教师还是管理员',
    account: '账号',
    password: '密码',
    captcha: '验证码',
    captchaAlt: '图形验证码，点击换一张',
    pickRole: '请选择登录身份',
    pickRoleHint: '这个账号对应多个身份，请选择本次登录的身份，再输入一次验证码。',
    welcomeBack: '欢迎回来，{name}',
  },

  register: {
    heading: '注册学生账号',
    hint: '教师和管理员账号由教务后台统一创建',
    confirm: '确认密码',
  },

  mobile: {
    consoleNote: '选课、成绩、考勤、作业、评价等教学记录，以及权限与日志，请在电脑上的教务后台处理',
    warningNormal: '正常',
    file: {
      fromChat: '从微信聊天记录选择文件',
      fromDevice: '从本机选择文件',
      fromAlbum: '从相册选择图片',
      uploading: '上传中 {p}%',
      open: '打开',
    },
    datetime: {
      date: '选择日期',
      time: '选择时间',
      incomplete: '请把日期和时间都选上',
    },
  },

  // 课表字段（库里按中文存）的显示文字，英文界面下也能读
  timetable: {
    weekday: { 1: '周一', 2: '周二', 3: '周三', 4: '周四', 5: '周五', 6: '周六', 7: '周日' },
    weekdayLong: { 1: '星期一', 2: '星期二', 3: '星期三', 4: '星期四', 5: '星期五', 6: '星期六', 7: '星期日' },
    segment: { 1: '第一大节', 2: '第二大节', 3: '第三大节', 4: '第四大节', 5: '第五大节' },
    type: { required: '必修', elective: '选修' },
  },
}
