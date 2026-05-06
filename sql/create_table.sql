-- 创建库
create database if not exists chart_flow_db;

-- 切换库
use chart_flow_db;

-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    index idx_userAccount (userAccount)
) comment '用户' collate = utf8mb4_unicode_ci;

-- 图表表
create table if not exists chart
(
    id           bigint auto_increment comment 'id' primary key,
    goal				 text  null comment '分析目标',
    `name`               varchar(128) null comment '图表名称',
    chartData    text  null comment '图表数据',
    chartType	   varchar(128) null comment '图表类型',
    genChart		 text	 null comment '生成的图表数据',
    genResult		 text	 null comment '生成的分析结论',
    status       varchar(128) not null default 'wait' comment 'wait,running,succeed,failed',
    execMessage  text   null comment '执行信息',
    userId       bigint null comment '创建用户 id',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除'
) comment '图表信息表' collate = utf8mb4_unicode_ci;


-- 数据资产管理表，记录用户上传的原始数据集信息
CREATE TABLE `dataset`
(
   `id` bigint(20) NOT NULL AUTO_INCREMENT,
   `name` varchar(256) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '数据集名称',
   `filePath` varchar(1024) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件存储路径',
   `rowCount` int(11) DEFAULT NULL COMMENT '数据行数',
   `columnCount` int(11) DEFAULT NULL COMMENT '数据列数',
   `columnMeta` text COLLATE utf8mb4_unicode_ci COMMENT '字段元信息JSON',
   `userId` bigint(20) NOT NULL COMMENT '上传用户ID',
   `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
   `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
   `isDelete` tinyint(1) NOT NULL DEFAULT '0',
   PRIMARY KEY (`id`),
   KEY `idx_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据集表';

-- 分析模版，预定义或用户保存的分析模板，包含自然语言查询模板、图表类型偏好、常用分析维度等
CREATE TABLE `prompt`
(
     `id` bigint(20) NOT NULL AUTO_INCREMENT,
     `name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板名称',
     `promptQuery` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板自然语言查询（可包含占位符）',
     `userId` bigint(20) DEFAULT NULL COMMENT '创建用户ID，为空表示系统预置',
     `usageCount` int(11) DEFAULT '0' COMMENT '使用次数',
     `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
     `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
     `isDelete` tinyint(1) NOT NULL DEFAULT '0',
     PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分析模板表';

-- 用户交互与反馈表，用户对分析结果（图表、结论）的满意度反馈，可用于评估系统效果或后续优化。
CREATE TABLE `chart_feedback`
(
     `id` bigint(20) NOT NULL AUTO_INCREMENT,
     `chartId` bigint(20) NOT NULL COMMENT '表结果ID',
     `userId` bigint(20) NOT NULL COMMENT '反馈用户ID',
     `rating` int(4) NOT NULL COMMENT '评分：1-5分',
     `comment` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '反馈意见',
     `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
     `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
     `isDelete` tinyint(1) NOT NULL DEFAULT '0',
     PRIMARY KEY (`id`),
     KEY `idx_chartId` (`chartId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图表反馈表';


-- 系统运行监控表
-- task_log  异步任务执行日志，记录大模型调用的请求参数、响应耗时、成功/失败状态，便于性能分析和问题排查。
CREATE TABLE `task_log`
(
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `chartId` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '对应chart.id',
    `status` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'running/success/failed',
    `costMs` int(11) DEFAULT NULL COMMENT '耗时（毫秒）',
    `execMessage` text COLLATE utf8mb4_unicode_ci COMMENT '执行信息',
    `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `isDelete` tinyint(1) NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    KEY `idx_chartId` (`chartId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务执行日志表';


-- model_record  大模型调用记录，记录每次LLM调用的Token消耗、推理耗时、模型版本等，便于成本统计和性能优化。
CREATE TABLE `model_record`
(
   `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
   `chartId` bigint(20) DEFAULT NULL COMMENT '关联的图表分析任务ID（chart.id）',
   `userId` bigint(20) DEFAULT NULL COMMENT '发起调用的用户ID（冗余字段，便于统计）',
   `modelName` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '使用的模型名称，如 qwen2:7b',
   `invocationType` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '调用类型：intent（意图解析）/codeGen（代码生成）/insight（洞察生成）',
   `inputTokens` int(11) DEFAULT NULL COMMENT '输入Token数量',
   `outputTokens` int(11) DEFAULT NULL COMMENT '输出Token数量',
   `totalTokens` int(11) DEFAULT NULL COMMENT '总计Token数量',
   `costMs` int(11) DEFAULT NULL COMMENT '调用耗时（毫秒）',
   `status` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'running' COMMENT '调用状态：running/success/failed',
   `requestContent` text COLLATE utf8mb4_unicode_ci COMMENT '请求内容（Prompt等，可选，便于调试）',
   `responseContent` text COLLATE utf8mb4_unicode_ci COMMENT '响应内容（生成的代码或文本，可选）',
   `errorMsg` text COLLATE utf8mb4_unicode_ci COMMENT '错误信息（失败时记录）',
   `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
   `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
   `isDelete` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
   PRIMARY KEY (`id`),
   KEY `idx_chartId` (`chartId`),
   KEY `idx_userId` (`userId`),
   KEY `idx_status` (`status`),
   KEY `idx_createTime` (`createTime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='大模型调用记录表';
