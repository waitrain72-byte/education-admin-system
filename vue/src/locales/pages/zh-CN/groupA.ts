/**
 * 分组 A 语言包：管理员/教师/学生管理 + 三个个人信息页。
 * 键命名空间：pages.admin / pages.teacher / pages.student / pages.adminPerson / pages.teacherPerson / pages.studentPerson
 */
export default {
    pages: {
        admin: {
            searchPlaceholder: '请输入账号查询',
            id: '序号',
            account: '账号',
            username: '用户名',
            name: '姓名',
            phone: '电话',
            email: '邮箱',
            avatar: '头像',
            role: '角色',
            dialogTitle: '管理员',
            uploadAvatar: '上传头像',
            ruleUsernameRequired: '请输入账号',
            resetConfirm: '确定将账号 {username} 的密码重置为 123456 吗？',
            resetSuccess: '密码已重置为 123456',
        },

        teacher: {
            searchPlaceholder: '请输入账号查询',
            id: '序号',
            account: '账号',
            username: '用户名',
            name: '姓名',
            phone: '电话',
            email: '邮箱',
            avatar: '头像',
            role: '角色',
            title: '职称',
            dialogTitle: '教师信息',
            uploadAvatar: '上传头像',
            ruleUsernameRequired: '请输入账号',
            resetConfirm: '确定将账号 {username} 的密码重置为 123456 吗？',
            resetSuccess: '密码已重置为 123456',
        },

        student: {
            searchPlaceholder: '请输入账号查询',
            id: '序号',
            account: '账号',
            username: '用户名',
            name: '姓名',
            avatar: '头像',
            role: '角色',
            college: '学院',
            collegePlaceholder: '请选择学院',
            speciality: '专业',
            specialityPlaceholder: '请选择专业',
            classes: '班级',
            classesPlaceholder: '请选择班级',
            score: '学分',
            dialogTitle: '学生信息',
            uploadAvatar: '上传头像',
            ruleUsernameRequired: '请输入账号',
            resetConfirm: '确定将账号 {username} 的密码重置为 123456 吗？',
            resetSuccess: '密码已重置为 123456',
        },
    },
}
