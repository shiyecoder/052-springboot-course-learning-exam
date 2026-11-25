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
 * 考试（表 exam）
 */
@Data
@TableName("exam")
public class Exam {
    /** 考试 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 考试 */
    private String examNo;

    /** 考试 */
    private Long courseId;

    /** 考试 */
    private String examName;

    /** 考试 */
    private String examType;

    /** 考试 */
    private LocalDateTime startTime;

    /** 考试 */
    private LocalDateTime endTime;

    /** 考试 */
    private Integer durationMin;

    /** 考试 */
    private BigDecimal totalScore;

    /** 考试 */
    private String paperMode;

    /** 考试 */
    private Integer attemptLimit;

    /** 考试 */
    private String cheatLevel;

    /** 考试 */
    private String status;

    /** 考试 */
    private Long createBy;

    /** 考试 */
    private LocalDateTime createTime;

    /** 考试 */
    private Long updateBy;

    /** 考试 */
    private LocalDateTime updateTime;

    /** 考试 */
    @TableLogic
    private Integer deleted;

    /** 考试 */
    @Version
    private Integer version;
}
