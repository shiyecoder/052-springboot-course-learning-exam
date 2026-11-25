package com.grad.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色菜单（表 sys_role_menu）
 */
@Data
@TableName("sys_role_menu")
public class SysRoleMenu {
    /** 角色菜单 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角色菜单 */
    private Long roleId;

    /** 角色菜单 */
    private Long menuId;
}
