package com.grad.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 教学班（表 sys_class）
 */
@Data
@TableName("sys_class")
public class SysClass {
    /** 教学班 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 教学班 */
    private String classNo;

    /** 教学班 */
    private String className;

    /** 教学班 */
    private Long courseId;

    /** 教学班 */
    private Long teacherId;

    /** 教学班 */
    private Long termId;

    /** 教学班 */
    private String major;

    /** 教学班 */
    private Integer studentCount;

    /** 教学班 */
    private Integer capacity;

    /** 教学班 */
    private String schedule;

    /** 教学班 */
    private String status;

    /** 教学班 */
    private Long createBy;

    /** 教学班 */
    private LocalDateTime createTime;

    /** 教学班 */
    private Long updateBy;

    /** 教学班 */
    private LocalDateTime updateTime;

    /** 教学班 */
    @TableLogic
    private Integer deleted;

    /** 教学班 */
    @Version
    private Integer version;
}
