package com.grad.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 操作日志（表 sys_oper_log）
 */
@Data
@TableName("sys_oper_log")
public class SysOperLog {
    /** 操作日志 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作日志 */
    private String module;

    /** 操作日志 */
    private String operType;

    /** 操作日志 */
    private String bizNo;

    /** 操作日志 */
    private String content;

    /** 操作日志 */
    private Long operId;

    /** 操作日志 */
    private String operName;

    /** 操作日志 */
    private LocalDateTime operTime;

    /** 操作日志 */
    private String ip;

    /** 操作日志 */
    private LocalDateTime createTime;
}
