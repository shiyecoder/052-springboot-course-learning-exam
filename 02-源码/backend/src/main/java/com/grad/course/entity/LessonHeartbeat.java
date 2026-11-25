package com.grad.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 学习心跳上报（表 lesson_heartbeat）
 */
@Data
@TableName("lesson_heartbeat")
public class LessonHeartbeat {
    /** 学习心跳上报 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学习心跳上报 */
    private Long enrollmentId;

    /** 学习心跳上报 */
    private Long chapterId;

    /** 学习心跳上报 */
    private LocalDateTime reportTime;

    /** 学习心跳上报 */
    private Integer deltaSec;

    /** 学习心跳上报 */
    private Integer abnormalFlag;

    /** 学习心跳上报 */
    private String deviceType;

    /** 学习心跳上报 */
    private LocalDateTime createTime;
}
