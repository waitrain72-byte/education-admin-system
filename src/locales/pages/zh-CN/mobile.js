/**
 * 小程序专用词条：底部标签栏、移动端特有的页面与提示、课表字段的显示文字。
 * 与 groupA/B/C、shell / space / campus / console（从 Web 端同步过来的模块）深合并。
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
