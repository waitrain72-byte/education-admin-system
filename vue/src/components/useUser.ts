
import { ref, readonly } from 'vue'

// 用户状态
const user = ref(JSON.parse(localStorage.getItem('xm-user') || '{}'))

// 更新用户信息
const updateUser = (newUser: any) => {
    if (newUser) {
        user.value = newUser
        localStorage.setItem('xm-user', JSON.stringify(newUser))
    } else {
        // 从 localStorage 刷新
        user.value = JSON.parse(localStorage.getItem('xm-user') || '{}')
    }
}

// 局部更新用户信息（只更新某些字段）
const patchUser = (data: Partial<typeof user.value>) => {
    user.value = { ...user.value, ...data }
    localStorage.setItem('xm-user', JSON.stringify(user.value))
}

// 清除用户信息（登出）
const clearUser = () => {
    user.value = {}
    localStorage.removeItem('xm-user')
}

// 刷新用户信息
const refreshUser = () => {
    user.value = JSON.parse(localStorage.getItem('xm-user') || '{}')
}

export const useUser = () => {
    return {
        user: readonly(user), // 只读，防止外部直接修改
        updateUser,
        patchUser,
        clearUser,
        refreshUser
    }
}
