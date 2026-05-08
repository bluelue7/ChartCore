package com.chartflow.core.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 数据集表
 * @TableName dataset
 */
@TableName(value ="dataset")
@Data
public class Dataset {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 数据集名称
     */
    private String name;

    /**
     * 文件存储路径
     */
    private String filePath;

    /**
     * 数据行数
     */
    private Integer rowCount;

    /**
     * 数据列数
     */
    private Integer columnCount;

    /**
     * 字段元信息JSON
     */
    private String columnMeta;

    /**
     * 上传用户ID
     */
    private Long userId;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;
}
