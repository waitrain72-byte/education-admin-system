package com.example.service;

import com.example.common.enums.ResultCodeEnum;
import com.example.entity.Permission;
import com.example.entity.Role;
import com.example.exception.CustomException;
import com.example.mapper.PermissionMapper;
import com.example.support.CurrentUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * RBAC 权限判定（接口层权限切面的核心）：管理员直接放行、其他角色按授权表判定并缓存，
 * 保存授权后缓存立即失效；管理员的授权不能在页面上改。
 */
@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private PermissionMapper permissionMapper;

    @InjectMocks
    private PermissionService service;

    @AfterEach
    void tearDown() {
        CurrentUser.clear();
    }

    private static Role role(int id, String code) {
        Role r = new Role();
        r.setId(id);
        r.setCode(code);
        return r;
    }

    private static Permission permission(int id, String code) {
        Permission p = new Permission();
        p.setId(id);
        p.setCode(code);
        return p;
    }

    private void studentGranted(String... codes) {
        when(permissionMapper.selectRoleByCode("STUDENT")).thenReturn(role(3, "STUDENT"));
        when(permissionMapper.selectCodesByRoleId(3)).thenReturn(Arrays.asList(codes));
    }

    @Test
    @DisplayName("管理员直接放行，不查授权表（误清授权后也能进权限设置页自救）")
    void adminAlwaysAllowed() {
        CurrentUser.as("ADMIN", 1, "管理员");

        assertTrue(service.hasPermission("permission:manage"));
        assertTrue(service.hasPermission("anything:at-all"));
        verifyNoInteractions(permissionMapper);
    }

    @Test
    @DisplayName("没登录（没有角色）一律拒绝；接口没声明权限码的直接放行")
    void anonymousDeniedBlankCodeAllowed() {
        assertFalse(service.hasPermission("score:view"));
        assertTrue(service.hasPermission(""));
        assertTrue(service.hasPermission(null));
        verifyNoInteractions(permissionMapper);
    }

    @Test
    @DisplayName("学生按授权表判定：授了的放行、没授的拒绝；授权表只查一次，之后读缓存")
    void studentCheckedAgainstGrantsAndCached() {
        CurrentUser.as("STUDENT", 3, "张三");
        studentGranted("score:view", "choice:manage");

        assertTrue(service.hasPermission("score:view"));
        assertTrue(service.hasPermission("choice:manage"));
        assertFalse(service.hasPermission("score:manage"));
        assertFalse(service.hasPermission("student:resetPwd"));
        verify(permissionMapper, times(1)).selectRoleByCode("STUDENT");
        verify(permissionMapper, times(1)).selectCodesByRoleId(3);
    }

    @Test
    @DisplayName("库里没有这个角色：什么权限都没有")
    void unknownRoleHasNothing() {
        CurrentUser.as("GUEST", 9, "访客");
        when(permissionMapper.selectRoleByCode("GUEST")).thenReturn(null);

        assertFalse(service.hasPermission("score:view"));
        verify(permissionMapper, never()).selectCodesByRoleId(anyInt());
    }

    @Test
    @DisplayName("保存授权：先清空再逐条写入，缓存立即失效，新授权马上生效")
    void updateInvalidatesCache() {
        CurrentUser.as("STUDENT", 3, "张三");
        studentGranted("score:view");
        assertFalse(service.hasPermission("homework:manage"));

        when(permissionMapper.selectByCode("score:view")).thenReturn(permission(12, "score:view"));
        when(permissionMapper.selectByCode("homework:manage")).thenReturn(permission(19, "homework:manage"));
        service.updateRolePermissions("STUDENT", Arrays.asList("score:view", "homework:manage", "score:view"));

        verify(permissionMapper).deleteRelationsByRoleId(3);
        verify(permissionMapper).insertRelation(3, 12);
        verify(permissionMapper).insertRelation(3, 19);
        verify(permissionMapper, times(2)).insertRelation(anyInt(), anyInt());

        when(permissionMapper.selectCodesByRoleId(3)).thenReturn(Arrays.asList("score:view", "homework:manage"));
        assertTrue(service.hasPermission("homework:manage"));
        verify(permissionMapper, times(2)).selectCodesByRoleId(3);
    }

    @Test
    @DisplayName("保存授权：传了不存在的权限码整体拒绝（事务回滚）；清空授权也可以")
    void updateRejectsUnknownCode() {
        when(permissionMapper.selectRoleByCode("TEACHER")).thenReturn(role(2, "TEACHER"));
        when(permissionMapper.selectByCode("no:such")).thenReturn(null);

        CustomException e = assertThrows(CustomException.class,
                () -> service.updateRolePermissions("TEACHER", Collections.singletonList("no:such")));
        assertEquals(ResultCodeEnum.PARAM_ERROR.code, e.getCode());

        service.updateRolePermissions("TEACHER", null);
        verify(permissionMapper, times(2)).deleteRelationsByRoleId(2);
        verify(permissionMapper, never()).insertRelation(anyInt(), anyInt());
    }

    @Test
    @DisplayName("管理员的授权不能改；不存在的角色报参数错误")
    void adminGrantsAreReadOnly() {
        when(permissionMapper.selectRoleByCode("ADMIN")).thenReturn(role(1, "ADMIN"));
        when(permissionMapper.selectRoleByCode("NOBODY")).thenReturn(null);

        CustomException admin = assertThrows(CustomException.class,
                () -> service.updateRolePermissions("ADMIN", Collections.emptyList()));
        assertEquals(ResultCodeEnum.PERMISSION_DENIED_ERROR.code, admin.getCode());
        CustomException nobody = assertThrows(CustomException.class,
                () -> service.updateRolePermissions("NOBODY", Collections.emptyList()));
        assertEquals(ResultCodeEnum.PARAM_ERROR.code, nobody.getCode());
        verify(permissionMapper, never()).deleteRelationsByRoleId(any());
        verify(permissionMapper, never()).selectByCode(anyString());
    }

    @Test
    @DisplayName("前端拿到的权限码：管理员是全部权限点，学生是自己的授权，没登录是空的")
    void currentPermissions() {
        CurrentUser.as("ADMIN", 1, "管理员");
        when(permissionMapper.selectAllPermissions()).thenReturn(Arrays.asList(
                permission(1, "dashboard:view"), permission(43, "permission:manage")));
        assertEquals(Arrays.asList("dashboard:view", "permission:manage"), service.currentPermissions());

        CurrentUser.as("STUDENT", 3, "张三");
        studentGranted("score:view", "choice:manage");
        List<String> mine = service.currentPermissions();
        assertEquals(new HashSet<>(Arrays.asList("score:view", "choice:manage")), new HashSet<>(mine));

        CurrentUser.clear();
        assertTrue(service.currentPermissions().isEmpty());
    }
}
