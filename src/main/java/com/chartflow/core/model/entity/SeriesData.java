package com.chartflow.core.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 系列数据
 * 用于 AIResult 中的 series 字段
 */
@Data
public class SeriesData implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 系列名称
     */
    private String name;

    /**
     * 数据列表
     */
    private List<Object> data;
}