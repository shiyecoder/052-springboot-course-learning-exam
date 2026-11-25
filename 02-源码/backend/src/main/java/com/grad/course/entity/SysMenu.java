package com.grad.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 菜单权限（表 sys_menu）
 */
@Data
@TableName("sys_menu")
public class SysMenu {
    /** 菜单权限 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 菜单权限 */
    private Long parentId;

    /** 菜单权限 */
    private String menuName;

    /** 菜单权限 */
    private String path;

    /** 菜单权限 */
    private String component;

    /** 菜单权限 */
    private String perms;

    /** 菜单权限 */
    private String menuType;

    /** 菜单权限 */
    private String icon;

    /** 菜单权限 */
    private Integer sortNo;

    /** 菜单权限 */
    private Integer visible;

    /** 菜单权限 */
    private Integer status;

    /** 菜单权限 */
    private LocalDateTime createTime;

    /** 菜单权限 */
    private LocalDateTime updateTime;

    /** 菜单权限 */
    @TableLogic
    private Integer deleted;
}
