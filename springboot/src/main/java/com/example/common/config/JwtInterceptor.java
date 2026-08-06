package com.example.common.config;

import cn.hutool.core.util.ObjectUtil;
import com.example.common.Constants;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.Account;
import com.example.exception.CustomException;
import com.example.utils.TokenUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JWT 拦截器：使用配置密钥验签（含过期校验），再校验账号仍存在。
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 从请求 header 中获取 token
        String token = request.getHeader(Constants.TOKEN);
        if (ObjectUtil.isEmpty(token)) {
            // 如果没拿到，从参数里再拿一次
            token = request.getParameter(Constants.TOKEN);
        }
        // 2. 开始执行认证
        if (ObjectUtil.isEmpty(token)) {
            throw new CustomException(ResultCodeEnum.TOKEN_INVALID_ERROR);
        }
        // 3. 使用配置密钥验签（含过期校验），不再依赖数据库密码作为密钥
        String userRole = TokenUtils.verifyToken(token);
        if (ObjectUtil.isNull(userRole)) {
            throw new CustomException(ResultCodeEnum.TOKEN_CHECK_ERROR);
        }
        // 4. 解析 userId-role，并校验账号仍存在
        try {
            String[] parts = userRole.split("-");
            if (parts.length != 2) {
                throw new CustomException(ResultCodeEnum.TOKEN_CHECK_ERROR);
            }
            Account account = TokenUtils.getAccountById(Integer.valueOf(parts[0]), parts[1]);
            if (ObjectUtil.isNull(account)) {
                throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
            }
        } catch (NumberFormatException e) {
            throw new CustomException(ResultCodeEnum.TOKEN_CHECK_ERROR);
        }
        return true;
    }
}
