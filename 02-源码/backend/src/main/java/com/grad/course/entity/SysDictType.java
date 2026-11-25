package com.grad.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 字典类型（表 sys_dict_type）
 */
@Data
@TableName("sys_dict_type")
public class SysDictType {
    /** 字典类型 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 字典类型 */
    private String dictName;

    /** 字典类型 */
    private String dictType;

    /** 字典类型 */
    private Integer status;

    /** 字典类型 */
    private String remark;

    /** 字典类型 */
    private LocalDateTime createTime;

    /** 字典类型 */
    private LocalDateTime updateTime;

    /** 字典类型 */
    @TableLogic
    private Integer deleted;
}
