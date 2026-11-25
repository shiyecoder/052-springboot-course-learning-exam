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
 * 考试答卷（表 exam_record）
 */
@Data
@TableName("exam_record")
public class ExamRecord {
    /** 考试答卷 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 考试答卷 */
    private String recordNo;

    /** 考试答卷 */
    private Long examId;

    /** 考试答卷 */
    private Long studentId;

    /** 考试答卷 */
    private Long enrollmentId;

    /** 考试答卷 */
    private Integer attemptNo;

    /** 考试答卷 */
    private LocalDateTime startTime;

    /** 考试答卷 */
    private LocalDateTime submitTime;

    /** 考试答卷 */
    private BigDecimal objectiveScore;

    /** 考试答卷 */
    private BigDecimal subjectiveScore;

    /** 考试答卷 */
    private BigDecimal totalScore;

    /** 考试答卷 */
    private Integer passFlag;

    /** 考试答卷 */
    private String status;

    /** 考试答卷 */
    private Long createBy;

    /** 考试答卷 */
    private LocalDateTime createTime;

    /** 考试答卷 */
    private Long updateBy;

    /** 考试答卷 */
    private LocalDateTime updateTime;

    /** 考试答卷 */
    @TableLogic
    private Integer deleted;

    /** 考试答卷 */
    @Version
    private Integer version;
}
