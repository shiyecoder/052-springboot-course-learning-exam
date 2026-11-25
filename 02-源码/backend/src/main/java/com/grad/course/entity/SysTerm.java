package com.grad.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 学期（表 sys_term）
 */
@Data
@TableName("sys_term")
public class SysTerm {
    /** 学期 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学期 */
    private String termCode;

    /** 学期 */
    private String termName;

    /** 学期 */
    private LocalDate startDate;

    /** 学期 */
    private LocalDate endDate;

    /** 学期 */
    private Integer weekCount;

    /** 学期 */
    private String status;

    /** 学期 */
    private Long createBy;

    /** 学期 */
    private LocalDateTime createTime;

    /** 学期 */
    private Long updateBy;

    /** 学期 */
    private LocalDateTime updateTime;

    /** 学期 */
    @TableLogic
    private Integer deleted;

    /** 学期 */
    @Version
    private Integer version;
}
