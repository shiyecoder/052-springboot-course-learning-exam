package com.grad.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 用户（表 sys_user）
 */
@Data
@TableName("sys_user")
public class SysUser {
    /** 用户 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 */
    private String username;

    /** 用户 */
    private String password;

    /** 用户 */
    private String realName;

    /** 用户 */
    private String userNo;

    /** 用户 */
    private String phone;

    /** 用户 */
    private String email;

    /** 用户 */
    private String avatar;

    /** 用户 */
    private String gender;

    /** 用户 */
    private Long deptId;

    /** 用户 */
    private String deptName;

    /** 用户 */
    private String title;

    /** 用户 */
    private Integer status;

    /** 用户 */
    private Integer creditScore;

    /** 用户 */
    private LocalDateTime lastLoginTime;

    /** 用户 */
    private Long createBy;

    /** 用户 */
    private LocalDateTime createTime;

    /** 用户 */
    private Long updateBy;

    /** 用户 */
    private LocalDateTime updateTime;

    /** 用户 */
    @TableLogic
    private Integer deleted;

    /** 用户 */
    @Version
    private Integer version;
}
