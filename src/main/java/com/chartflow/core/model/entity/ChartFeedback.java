package com.chartflow.core.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 图表反馈表
 * @TableName chart_feedback
 */
@TableName(value ="chart_feedback")
@Data
public class ChartFeedback {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 表结果ID
     */
    private Long chartId;

    /**
     * 反馈用户ID
     */
    private Long userId;

    /**
     * 评分：1-5分
     */
    private Integer rating;

    /**
     * 反馈意见
     */
    private String comment;

    /**
     * 创建时间
     */
    private Date createTime;

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
