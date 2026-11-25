package com.grad.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 试卷题目快照（表 exam_paper_question）
 */
@Data
@TableName("exam_paper_question")
public class ExamPaperQuestion {
    /** 试卷题目快照 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 试卷题目快照 */
    private Long examId;

    /** 试卷题目快照 */
    private Long questionId;

    /** 试卷题目快照 */
    private String questionNo;

    /** 试卷题目快照 */
    private Integer sortNo;

    /** 试卷题目快照 */
    private String questionType;

    /** 试卷题目快照 */
    private String stem;

    /** 试卷题目快照 */
    private String optionsJson;

    /** 试卷题目快照 */
    private String answer;

    /** 试卷题目快照 */
    private String analysis;

    /** 试卷题目快照 */
    private BigDecimal score;

    /** 试卷题目快照 */
    private LocalDateTime createTime;

    /** 试卷题目快照 */
    @TableLogic
    private Integer deleted;
}
