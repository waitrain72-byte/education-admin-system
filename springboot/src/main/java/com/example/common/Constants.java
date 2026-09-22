package com.example.common;

/*定义一个常量*/
public interface Constants {

    String TOKEN = "token";

    String USER_DEFAULT_PASSWORD = "123456";

    /**
     * 当前登录用户在 request 属性中的缓存键。
     * JwtInterceptor 校验 token 时已查过一次账号，放进请求属性供本次请求内复用，
     * 避免鉴权切面、日志切面、防重切面与各 Service 的数据隔离钩子各查一遍同一行。
     */
    String CURRENT_USER = "currentUser";

}
