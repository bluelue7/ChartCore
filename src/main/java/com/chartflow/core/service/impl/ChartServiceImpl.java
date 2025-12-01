package com.chartflow.core.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chartflow.core.model.entity.Chart;
import com.chartflow.core.service.ChartService;
import com.chartflow.core.mapper.ChartMapper;
import org.springframework.stereotype.Service;

/**
* @author bluelue7
* @description 针对表【chart(图表信息表)】的数据库操作Service实现
* @createDate 2025-12-01 15:14:26
*/
@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
    implements ChartService{

}




