package com.grad.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 字典数据（表 sys_dict_data）
 */
@Data
@TableName("sys_dict_data")
public class SysDictData {
    /** 字典数据 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 字典数据 */
    private String dictType;

    /** 字典数据 */
    private String dictLabel;

    /** 字典数据 */
    private String dictValue;

    /** 字典数据 */
    private String tagType;

    /** 字典数据 */
    private Integer sortNo;

    /** 字典数据 */
    private Integer status;

    /** 字典数据 */
    private LocalDateTime createTime;

    /** 字典数据 */
    @TableLogic
    private Integer deleted;
}
