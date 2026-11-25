package com.grad.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户角色（表 sys_user_role）
 */
@Data
@TableName("sys_user_role")
public class SysUserRole {
    /** 用户角色 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户角色 */
    private Long userId;

    /** 用户角色 */
    private Long roleId;
}
