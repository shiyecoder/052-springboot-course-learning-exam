package com.grad.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 站内消息（表 sys_message）
 */
@Data
@TableName("sys_message")
public class SysMessage {
    /** 站内消息 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 站内消息 */
    private Long receiverId;

    /** 站内消息 */
    private String title;

    /** 站内消息 */
    private String content;

    /** 站内消息 */
    private String msgType;

    /** 站内消息 */
    private String bizType;

    /** 站内消息 */
    private Long bizId;

    /** 站内消息 */
    private Integer readFlag;

    /** 站内消息 */
    private LocalDateTime createTime;

    /** 站内消息 */
    @TableLogic
    private Integer deleted;
}
