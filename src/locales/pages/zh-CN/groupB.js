/**
 * 分组 B 语言包：学院信息/教务通知/考试安排/教室安排/专业/班级。
 * 键命名空间：pages.college / pages.notice / pages.examplan / pages.roomplan / pages.speciality / pages.classes
 */
export default {
  pages: {
    college: {
      id: '序号',
      nameLabel: '学院名称',
      namePlaceholder: '请输入学院名称',
      dialogTitle: '学院信息',
      ruleNameRequired: '请输入学院名称',
    },

    notice: {
      id: '序号',
      searchPlaceholder: '请输入标题查询',
      dialogTitle: '信息',
      title: '标题',
      content: '内容',
      time: '创建时间',
      creator: '创建人',
      ruleTitleRequired: '请输入标题',
      ruleContentRequired: '请输入内容',
    },

    examplan: {
      id: '序号',
      searchPlaceholder: '请输入标题查询',
      dialogTitle: '信息',
      title: '标题',
      content: '内容',
      time: '创建时间',
      ruleTitleRequired: '请输入标题',
      ruleContentRequired: '请输入内容',
    },

    roomplan: {
      id: '序号',
      searchPlaceholder: '请输入教室名称',
      statusPlaceholder: '请选择状态',
      dialogTitle: '信息',
      name: '教室名称',
      status: '使用状态',
      num: '容纳人数',
      description: '使用说明',
      contentLabel: '教室说明',
      free: '空闲',
      occupied: '占用',
      ruleNameRequired: '请输入教室名称',
      ruleStatusRequired: '请选择状态',
      ruleNumRequired: '请输入容纳人数',
      ruleContentRequired: '请输入使用说明',
    },

    speciality: {
      id: '序号',
      searchPlaceholder: '请输入专业名称',
      dialogTitle: '信息',
      name: '专业名称',
      content: '专业描述',
      college: '所属学院',
      collegePlaceholder: '请选择所属学院',
      score: '学分限定',
      ruleNameRequired: '请输入专业名称',
    },

    classes: {
      id: '序号',
      searchPlaceholder: '请输入班级名称',
      dialogTitle: '信息',
      name: '班级名称',
      content: '班级描述',
      speciality: '所属专业',
      specialityPlaceholder: '请选择专业',
      teacher: '班主任',
      teacherPlaceholder: '请选择教师',
      ruleNameRequired: '请输入班级名称',
    },
  },
}
