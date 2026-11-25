package com.grad.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 预警规则（表 risk_rule）
 */
@Data
@TableName("risk_rule")
public class RiskRule {
    /** 预警规则 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 预警规则 */
    private String ruleName;

    /** 预警规则 */
    private String ruleVersion;

    /** 预警规则 */
    private BigDecimal weightWatch;

    /** 预警规则 */
    private BigDecimal weightSubmit;

    /** 预警规则 */
    private BigDecimal weightQuiz;

    /** 预警规则 */
    private BigDecimal weightLogin;

    /** 预警规则 */
    private BigDecimal weightDiscuss;

    /** 预警规则 */
    private BigDecimal lowLine;

    /** 预警规则 */
    private BigDecimal focusLine;

    /** 预警规则 */
    private BigDecimal midLine;

    /** 预警规则 */
    private LocalDateTime effectTime;

    /** 预警规则 */
    private String status;

    /** 预警规则 */
    private String remark;

    /** 预警规则 */
    private Long createBy;

    /** 预警规则 */
    private LocalDateTime createTime;

    /** 预警规则 */
    private Long updateBy;

    /** 预警规则 */
    private LocalDateTime updateTime;

    /** 预警规则 */
    @TableLogic
    private Integer deleted;
}
