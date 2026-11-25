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
 * 学习进度明细（表 lesson_progress）
 */
@Data
@TableName("lesson_progress")
public class LessonProgress {
    /** 学习进度明细 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学习进度明细 */
    private Long enrollmentId;

    /** 学习进度明细 */
    private Long chapterId;

    /** 学习进度明细 */
    private Integer watchedSeconds;

    /** 学习进度明细 */
    private BigDecimal watchRate;

    /** 学习进度明细 */
    private Integer playTimes;

    /** 学习进度明细 */
    private LocalDateTime firstLearnTime;

    /** 学习进度明细 */
    private LocalDateTime lastHeartTime;

    /** 学习进度明细 */
    private Integer finishFlag;

    /** 学习进度明细 */
    private String deviceType;

    /** 学习进度明细 */
    private Integer abnormalFlag;

    /** 学习进度明细 */
    private Long createBy;

    /** 学习进度明细 */
    private LocalDateTime createTime;

    /** 学习进度明细 */
    private Long updateBy;

    /** 学习进度明细 */
    private LocalDateTime updateTime;

    /** 学习进度明细 */
    @TableLogic
    private Integer deleted;

    /** 学习进度明细 */
    @Version
    private Integer version;
}
