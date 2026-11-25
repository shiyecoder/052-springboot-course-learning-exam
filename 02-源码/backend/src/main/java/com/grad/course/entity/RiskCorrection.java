package com.grad.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 风险人工修正留痕（表 risk_correction）
 */
@Data
@TableName("risk_correction")
public class RiskCorrection {
    /** 风险人工修正留痕 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 风险人工修正留痕 */
    private Long snapshotId;

    /** 风险人工修正留痕 */
    private String originLevel;

    /** 风险人工修正留痕 */
    private BigDecimal originScore;

    /** 风险人工修正留痕 */
    private String newLevel;

    /** 风险人工修正留痕 */
    private BigDecimal newScore;

    /** 风险人工修正留痕 */
    private String reason;

    /** 风险人工修正留痕 */
    private Long correctBy;

    /** 风险人工修正留痕 */
    private LocalDateTime correctTime;

    /** 风险人工修正留痕 */
    private LocalDateTime createTime;
}
