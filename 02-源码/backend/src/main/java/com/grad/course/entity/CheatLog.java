package com.grad.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 防作弊记录（表 cheat_log）
 */
@Data
@TableName("cheat_log")
public class CheatLog {
    /** 防作弊记录 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 防作弊记录 */
    private Long examRecordId;

    /** 防作弊记录 */
    private String eventType;

    /** 防作弊记录 */
    private LocalDateTime eventTime;

    /** 防作弊记录 */
    private Integer durationSec;

    /** 防作弊记录 */
    private Integer totalCount;

    /** 防作弊记录 */
    private String deviceInfo;

    /** 防作弊记录 */
    private String ipAddr;

    /** 防作弊记录 */
    private String handleResult;

    /** 防作弊记录 */
    private Long handlerId;

    /** 防作弊记录 */
    private Long createBy;

    /** 防作弊记录 */
    private LocalDateTime createTime;

    /** 防作弊记录 */
    private Long updateBy;

    /** 防作弊记录 */
    private LocalDateTime updateTime;

    /** 防作弊记录 */
    @TableLogic
    private Integer deleted;

    /** 防作弊记录 */
    @Version
    private Integer version;
}
