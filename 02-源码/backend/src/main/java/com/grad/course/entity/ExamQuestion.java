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
 * 考试题目（表 exam_question）
 */
@Data
@TableName("exam_question")
public class ExamQuestion {
    /** 考试题目 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 考试题目 */
    private String questionNo;

    /** 考试题目 */
    private Long courseId;

    /** 考试题目 */
    private Long chapterId;

    /** 考试题目 */
    private String questionType;

    /** 考试题目 */
    private String stem;

    /** 考试题目 */
    private String optionsJson;

    /** 考试题目 */
    private String answer;

    /** 考试题目 */
    private String analysis;

    /** 考试题目 */
    private Integer difficulty;

    /** 考试题目 */
    private BigDecimal score;

    /** 考试题目 */
    private Integer useCount;

    /** 考试题目 */
    private String status;

    /** 考试题目 */
    private Long createBy;

    /** 考试题目 */
    private LocalDateTime createTime;

    /** 考试题目 */
    private Long updateBy;

    /** 考试题目 */
    private LocalDateTime updateTime;

    /** 考试题目 */
    @TableLogic
    private Integer deleted;

    /** 考试题目 */
    @Version
    private Integer version;
}
