package com.grad.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 角色（表 sys_role）
 */
@Data
@TableName("sys_role")
public class SysRole {
    /** 角色 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角色 */
    private String roleName;

    /** 角色 */
    private String roleCode;

    /** 角色 */
    private String dataScope;

    /** 角色 */
    private Integer status;

    /** 角色 */
    private String remark;

    /** 角色 */
    private Long createBy;

    /** 角色 */
    private LocalDateTime createTime;

    /** 角色 */
    private Long updateBy;

    /** 角色 */
    private LocalDateTime updateTime;

    /** 角色 */
    @TableLogic
    private Integer deleted;

    /** 角色 */
    @Version
    private Integer version;
}
