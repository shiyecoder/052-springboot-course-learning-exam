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
 * 选课记录（表 enrollment）
 */
@Data
@TableName("enrollment")
public class Enrollment {
    /** 选课记录 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 选课记录 */
    private String enrollNo;

    /** 选课记录 */
    private Long courseId;

    /** 选课记录 */
    private Long studentId;

    /** 选课记录 */
    private Long classId;

    /** 选课记录 */
    private String sourceType;

    /** 选课记录 */
    private LocalDateTime enrollTime;

    /** 选课记录 */
    private BigDecimal progressRate;

    /** 选课记录 */
    private Integer studyMinutes;

    /** 选课记录 */
    private LocalDateTime lastStudyTime;

    /** 选课记录 */
    private String status;

    /** 选课记录 */
    private String quitReason;

    /** 选课记录 */
    private Long createBy;

    /** 选课记录 */
    private LocalDateTime createTime;

    /** 选课记录 */
    private Long updateBy;

    /** 选课记录 */
    private LocalDateTime updateTime;

    /** 选课记录 */
    @TableLogic
    private Integer deleted;

    /** 选课记录 */
    @Version
    private Integer version;
}
