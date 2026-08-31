package com.example.service;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Permission;
import com.example.entity.Role;
import com.example.exception.CustomException;
import com.example.mapper.PermissionMapper;
import com.example.utils.TokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RBAC 权限服务：为 {@link com.example.common.config.PermissionAspect} 提供
 * 「当前登录角色是否拥有某权限码」的判定，并为权限设置页提供查询/保存授权的能力。
 *
 * <p>权限数据加载后在内存中按角色缓存（本项目未引入 Redis，用进程内缓存减少每请求一次的关联查询），
 * 保存授权后主动失效对应角色的缓存，多实例部署场景需通过重启或后续引入分布式缓存来同步。</p>
 */
@Service
public class PermissionService {

    private static final Logger log = LoggerFactory.getLogger(PermissionService.class);

    @Resource
    private PermissionMapper permissionMapper;

    /** 角色权限缓存：roleCode -> 该角色的权限码集合；null 值表示尚未加载 */
    private final Map<String, Set<String>> rolePermissionCache = new ConcurrentHashMap<>();

    /**
     * 判定当前登录角色是否拥有指定权限码。
     * ADMIN 作为超级管理员直接放行（防止误清授权后无法进入权限设置页自救）。
     */
    public boolean hasPermission(String code) {
        if (StrUtil.isBlank(code)) {
            return true;
        }
        String role = TokenUtils.getCurrentUser().getRole();
        if (StrUtil.isBlank(role)) {
            return false;
        }
        if (RoleEnum.ADMIN.name().equals(role)) {
            return true;
        }
        return loadPermissions(role).contains(code);
    }

    /**
     * 读取角色的权限码集合（带进程内缓存）
     */
    private Set<String> loadPermissions(String roleCode) {
        return rolePermissionCache.computeIfAbsent(roleCode, code -> {
            Role role = permissionMapper.selectRoleByCode(code);
            if (role == null) {
                log.warn("权限角色不存在: {}", code);
                return Collections.emptySet();
            }
            List<String> codes = permissionMapper.selectCodesByRoleId(role.getId());
            return Collections.unmodifiableSet(new HashSet<>(codes));
        });
    }

    /**
     * 当前登录用户拥有的权限码集合（供前端控制按钮/接口可见性）。
     * ADMIN 返回全部权限点；其他角色返回其授权集合（读缓存）。
     */
    public List<String> currentPermissions() {
        Account current = TokenUtils.getCurrentUser();
        if (current == null || StrUtil.isBlank(current.getRole())) {
            return Collections.emptyList();
        }
        if (RoleEnum.ADMIN.name().equals(current.getRole())) {
            return permissionMapper.selectAllPermissions().stream()
                    .map(Permission::getCode)
                    .collect(java.util.stream.Collectors.toList());
        }
        return new ArrayList<>(loadPermissions(current.getRole()));
    }

    /**
     * 查询全部权限点与全部角色的授权情况（权限设置页数据源）
     */
    public Map<String, Object> selectAllWithRoles() {
        List<Permission> permissions = permissionMapper.selectAllPermissions();
        List<Role> roles = permissionMapper.selectAllRoles();
        for (Role role : roles) {
            role.setPermissions(new ArrayList<>(loadPermissions(role.getCode())));
        }
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("permissions", permissions);
        data.put("roles", roles);
        return data;
    }

    /**
     * 保存某角色的授权集合：整体替换该角色的权限关联（清空 + 批量插入，事务保证原子性）
     */
    @Transactional
    public void updateRolePermissions(String roleCode, List<String> permissionCodes) {
        Role role = permissionMapper.selectRoleByCode(roleCode);
        if (ObjectUtil.isNull(role)) {
            throw new CustomException(ResultCodeEnum.PARAM_ERROR);
        }
        if (RoleEnum.ADMIN.name().equals(roleCode)) {
            // ADMIN 是超级管理员（切面中放行），授权数据不参与判定，禁止在页面调整避免误导
            throw new CustomException(ResultCodeEnum.PERMISSION_DENIED_ERROR);
        }
        permissionMapper.deleteRelationsByRoleId(role.getId());
        if (permissionCodes != null) {
            Set<String> validCodes = new HashSet<>(permissionCodes);
            for (String code : validCodes) {
                Permission permission = permissionMapper.selectByCode(code);
                if (ObjectUtil.isNull(permission)) {
                    // 传入的权限码不存在：说明数据被并发修改，整体回滚
                    throw new CustomException(ResultCodeEnum.PARAM_ERROR);
                }
                permissionMapper.insertRelation(role.getId(), permission.getId());
            }
        }
        // 授权变更后失效该角色的内存缓存，立即生效
        rolePermissionCache.remove(roleCode);
    }
}
