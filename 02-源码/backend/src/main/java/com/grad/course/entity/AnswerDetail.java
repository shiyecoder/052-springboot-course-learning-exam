package com.grad.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 答题明细（表 answer_detail）
 */
@Data
@TableName("answer_detail")
public class AnswerDetail {
    /** 答题明细 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 答题明细 */
    private Long examRecordId;

    /** 答题明细 */
    private Long questionId;

    /** 答题明细 */
    private Integer sortNo;

    /** 答题明细 */
    private String questionType;

    /** 答题明细 */
    private String answerContent;

    /** 答题明细 */
    private Integer correctFlag;

    /** 答题明细 */
    private BigDecimal score;

    /** 答题明细 */
    private BigDecimal fullScore;

    /** 答题明细 */
    private String comment;

    /** 答题明细 */
    private Long gradeBy;

    /** 答题明细 */
    private LocalDateTime gradeTime;

    /** 答题明细 */
    private BigDecimal firstScore;

    /** 答题明细 */
    private LocalDateTime createTime;

    /** 答题明细 */
    private LocalDateTime updateTime;

    /** 答题明细 */
    @TableLogic
    private Integer deleted;
}
