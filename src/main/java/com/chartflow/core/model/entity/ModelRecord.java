package com.chartflow.core.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 大模型调用记录表
 * @TableName model_record
 */
@TableName(value ="model_record")
@Data
public class ModelRecord {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联的图表分析任务ID（chart.id）
     */
    private Long chartId;

    /**
     * 发起调用的用户ID（冗余字段，便于统计）
     */
    private Long userId;

    /**
     * 使用的模型名称，如 qwen2:7b
     */
    private String modelName;

    /**
     * 调用类型：intent（意图解析）/codeGen（代码生成）/insight（洞察生成）
     */
    private String invocationType;

    /**
     * 输入Token数量
     */
    private Integer inputTokens;

    /**
     * 输出Token数量
     */
    private Integer outputTokens;

    /**
     * 总计Token数量
     */
    private Integer totalTokens;

    /**
     * 调用耗时（毫秒）
     */
    private Integer costMs;

    /**
     * 调用状态：running/success/failed
     */
    private String status;

    /**
     * 请求内容（Prompt等，可选，便于调试）
     */
    private String requestContent;

    /**
     * 响应内容（生成的代码或文本，可选）
     */
    private String responseContent;

    /**
     * 错误信息（失败时记录）
     */
    private String errorMsg;

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
