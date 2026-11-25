package com.grad.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 讨论回复（表 discussion_reply）
 */
@Data
@TableName("discussion_reply")
public class DiscussionReply {
    /** 讨论回复 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 讨论回复 */
    private Long postId;

    /** 讨论回复 */
    private Long authorId;

    /** 讨论回复 */
    private String content;

    /** 讨论回复 */
    private Integer isAdopted;

    /** 讨论回复 */
    private Integer likeCount;

    /** 讨论回复 */
    private String status;

    /** 讨论回复 */
    private LocalDateTime createTime;

    /** 讨论回复 */
    @TableLogic
    private Integer deleted;
}
