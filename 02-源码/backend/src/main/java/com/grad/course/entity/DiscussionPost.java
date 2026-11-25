package com.grad.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 讨论主题（表 discussion_post）
 */
@Data
@TableName("discussion_post")
public class DiscussionPost {
    /** 讨论主题 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 讨论主题 */
    private String postNo;

    /** 讨论主题 */
    private Long courseId;

    /** 讨论主题 */
    private Long chapterId;

    /** 讨论主题 */
    private Long authorId;

    /** 讨论主题 */
    private String title;

    /** 讨论主题 */
    private String content;

    /** 讨论主题 */
    private Integer isTop;

    /** 讨论主题 */
    private Integer isEssence;

    /** 讨论主题 */
    private Integer isSolved;

    /** 讨论主题 */
    private Integer replyCount;

    /** 讨论主题 */
    private Long adoptReplyId;

    /** 讨论主题 */
    private String status;

    /** 讨论主题 */
    private LocalDateTime createTime;

    /** 讨论主题 */
    private LocalDateTime updateTime;

    /** 讨论主题 */
    @TableLogic
    private Integer deleted;
}
