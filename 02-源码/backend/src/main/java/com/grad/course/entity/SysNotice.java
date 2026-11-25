package com.grad.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 公告（表 sys_notice）
 */
@Data
@TableName("sys_notice")
public class SysNotice {
    /** 公告 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 公告 */
    private String title;

    /** 公告 */
    private String content;

    /** 公告 */
    private String noticeType;

    /** 公告 */
    private LocalDateTime publishTime;

    /** 公告 */
    private Long publisherId;

    /** 公告 */
    private String status;

    /** 公告 */
    private LocalDateTime createTime;

    /** 公告 */
    @TableLogic
    private Integer deleted;
}
