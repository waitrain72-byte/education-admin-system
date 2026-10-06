package com.example.support;

import com.example.common.Constants;
import com.example.entity.Account;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 单元测试里模拟「当前登录人」：TokenUtils.getCurrentUser() 优先读请求属性里缓存的账号，
 * 这里直接把账号放进一个模拟请求，Service 的数据隔离、身份判断就能按指定角色走。
 * 用完务必调用 {@link #clear()}，否则会串到其他测试。
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Account as(String role, Integer id, String name) {
        Account account = new Account();
        account.setId(id);
        account.setRole(role);
        account.setName(name);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(Constants.CURRENT_USER, account);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        return account;
    }

    public static void clear() {
        RequestContextHolder.resetRequestAttributes();
    }
}
