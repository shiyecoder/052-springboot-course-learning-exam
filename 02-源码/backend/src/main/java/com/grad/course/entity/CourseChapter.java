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
 * 章节课时（表 course_chapter）
 */
@Data
@TableName("course_chapter")
public class CourseChapter {
    /** 章节课时 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 章节课时 */
    private String chapterNo;

    /** 章节课时 */
    private Long courseId;

    /** 章节课时 */
    private Long parentId;

    /** 章节课时 */
    private String chapterName;

    /** 章节课时 */
    private String nodeType;

    /** 章节课时 */
    private String resourceUrl;

    /** 章节课时 */
    private Integer durationMin;

    /** 章节课时 */
    private BigDecimal minFinishRate;

    /** 章节课时 */
    private Integer sortNo;

    /** 章节课时 */
    private Integer isRequired;

    /** 章节课时 */
    private Integer status;

    /** 章节课时 */
    private Long createBy;

    /** 章节课时 */
    private LocalDateTime createTime;

    /** 章节课时 */
    private Long updateBy;

    /** 章节课时 */
    private LocalDateTime updateTime;

    /** 章节课时 */
    @TableLogic
    private Integer deleted;

    /** 章节课时 */
    @Version
    private Integer version;
}
