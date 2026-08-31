package com.example.controller;

import com.example.common.Result;
import com.example.common.annotation.RequirePermission;
import com.example.service.PermissionService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 权限接口（RBAC）：
 *   selectAll / updateRolePermissions —— 权限设置页专用，仅 ADMIN 可用；
 *   my —— 返回当前登录用户拥有的权限码集合，供前端按钮/菜单控制使用（任一登录角色可调）。
 */
@RestController
@RequestMapping("/permission")
public class PermissionController {

    @Resource
    private PermissionService permissionService;

    /**
     * 查询全部权限点与各角色的授权情况（权限设置页数据源），仅 ADMIN
     */
    @RequirePermission("permission:manage")
    @GetMapping("/selectAll")
    public Result selectAll() {
        Map<String, Object> data = permissionService.selectAllWithRoles();
        return Result.success(data);
    }

    /**
     * 保存某角色的授权集合（整体替换），仅 ADMIN
     */
    @RequirePermission("permission:manage")
    @PutMapping("/updateRolePermissions")
    public Result updateRolePermissions(@RequestBody Map<String, Object> body) {
        String roleCode = (String) body.get("roleCode");
        @SuppressWarnings("unchecked")
        List<String> permissionCodes = (List<String>) body.get("permissionCodes");
        permissionService.updateRolePermissions(roleCode, permissionCodes);
        return Result.success();
    }

    /**
     * 返回当前登录用户的权限码集合（登录即可），前端据此控制按钮/接口可见性，
     * 与后端接口级鉴权保持一致。
     */
    @GetMapping("/my")
    public Result my() {
        return Result.success(permissionService.currentPermissions());
    }
}
