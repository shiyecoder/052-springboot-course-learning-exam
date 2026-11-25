package com.grad.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 上传文件（表 sys_file）
 */
@Data
@TableName("sys_file")
public class SysFile {
    /** 上传文件 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 上传文件 */
    private String fileName;

    /** 上传文件 */
    private String filePath;

    /** 上传文件 */
    private Long fileSize;

    /** 上传文件 */
    private String fileType;

    /** 上传文件 */
    private String bizType;

    /** 上传文件 */
    private Long bizId;

    /** 上传文件 */
    private Long uploaderId;

    /** 上传文件 */
    private LocalDateTime createTime;

    /** 上传文件 */
    @TableLogic
    private Integer deleted;
}
