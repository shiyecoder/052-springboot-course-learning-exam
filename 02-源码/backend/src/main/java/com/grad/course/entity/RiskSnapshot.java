package com.grad.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 风险计算快照（表 risk_snapshot）
 */
@Data
@TableName("risk_snapshot")
public class RiskSnapshot {
    /** 风险计算快照 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 风险计算快照 */
    private String snapshotNo;

    /** 风险计算快照 */
    private Long studentId;

    /** 风险计算快照 */
    private Long courseId;

    /** 风险计算快照 */
    private Long enrollmentId;

    /** 风险计算快照 */
    private Long ruleId;

    /** 风险计算快照 */
    private String ruleVersion;

    /** 风险计算快照 */
    private BigDecimal rawWatch;

    /** 风险计算快照 */
    private BigDecimal rawSubmit;

    /** 风险计算快照 */
    private BigDecimal rawQuiz;

    /** 风险计算快照 */
    private BigDecimal rawLogin;

    /** 风险计算快照 */
    private BigDecimal rawDiscuss;

    /** 风险计算快照 */
    private BigDecimal scoreWatch;

    /** 风险计算快照 */
    private BigDecimal scoreSubmit;

    /** 风险计算快照 */
    private BigDecimal scoreQuiz;

    /** 风险计算快照 */
    private BigDecimal scoreLogin;

    /** 风险计算快照 */
    private BigDecimal scoreDiscuss;

    /** 风险计算快照 */
    private BigDecimal riskScore;

    /** 风险计算快照 */
    private String riskLevel;

    /** 风险计算快照 */
    private String gapText;

    /** 风险计算快照 */
    private String suggestion;

    /** 风险计算快照 */
    private String missingFlag;

    /** 风险计算快照 */
    private LocalDateTime calcTime;

    /** 风险计算快照 */
    private Long calcBy;

    /** 风险计算快照 */
    private Integer adoptFlag;

    /** 风险计算快照 */
    private LocalDateTime createTime;

    /** 风险计算快照 */
    @TableLogic
    private Integer deleted;
}
