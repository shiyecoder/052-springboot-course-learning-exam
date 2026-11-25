package com.grad.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 课程（表 course）
 */
@Data
@TableName("course")
public class Course {
    /** 课程 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 课程 */
    private String courseNo;

    /** 课程 */
    private String courseName;

    /** 课程 */
    private String coverUrl;

    /** 课程 */
    private String intro;

    /** 课程 */
    private Long teacherId;

    /** 课程 */
    private Long termId;

    /** 课程 */
    private BigDecimal credit;

    /** 课程 */
    private Integer totalHours;

    /** 课程 */
    private String courseType;

    /** 课程 */
    private Integer capacity;

    /** 课程 */
    private Integer enrollCount;

    /** 课程 */
    private String status;

    /** 课程 */
    private Long createBy;

    /** 课程 */
    private LocalDateTime createTime;

    /** 课程 */
    private Long updateBy;

    /** 课程 */
    private LocalDateTime updateTime;

    /** 课程 */
    @TableLogic
    private Integer deleted;

    /** 课程 */
    @Version
    private Integer version;
}
