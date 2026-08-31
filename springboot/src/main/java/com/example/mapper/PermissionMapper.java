package com.example.mapper;

import com.example.entity.Permission;
import com.example.entity.Role;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * RBAC 权限数据访问接口：权限点、角色、角色-权限关联。
 * 表结构固定（仅管理员在权限设置页调整授权关系），无需通用增删改查。
 */
public interface PermissionMapper {

    // ================= 权限点 =================

    @Select("select * from sys_permission order by module, sort_num, id")
    List<Permission> selectAllPermissions();

    @Select("select * from sys_permission where code = #{code}")
    Permission selectByCode(@Param("code") String code);

    // ================= 角色 =================

    @Select("select * from sys_role order by id")
    List<Role> selectAllRoles();

    @Select("select * from sys_role where code = #{code}")
    Role selectRoleByCode(@Param("code") String code);

    // ================= 角色-权限关联 =================

    /**
     * 查询角色拥有的全部权限码（切面鉴权的数据来源）
     */
    @Select("select p.code from sys_role_permission rp " +
            "join sys_permission p on rp.permission_id = p.id " +
            "where rp.role_id = #{roleId}")
    List<String> selectCodesByRoleId(@Param("roleId") Integer roleId);

    /**
     * 保存某角色的授权：先清空旧关联再批量插入（由 Service 层保证事务）
     */
    @Delete("delete from sys_role_permission where role_id = #{roleId}")
    int deleteRelationsByRoleId(@Param("roleId") Integer roleId);

    @Insert("insert into sys_role_permission (role_id, permission_id) values (#{roleId}, #{permissionId})")
    int insertRelation(@Param("roleId") Integer roleId, @Param("permissionId") Integer permissionId);
}
