# ChartCore - 智能数据分析系统

基于 Java SpringBoot 的智能数据分析系统，支持通过 AI 自动生成数据可视化图表和分析报告。

## 功能特性

### 核心功能
- **智能图表生成**：上传 Excel 数据文件，AI 自动分析并生成 ECharts 可视化图表
- **三种生成模式**：同步生成、异步线程池生成、MQ 消息队列异步生成
- **Prompt 模板管理**：支持自定义分析模板，灵活控制 AI 输出格式
- **邮件通知**：图表生成完成后自动发送邮件通知用户
- **任务日志**：完整记录图表生成任务状态和执行时间
- **模型调用记录**：记录 AI 模型调用详情，包括 Token 消耗

### 用户管理
- 用户注册、登录、注销
- 密码加密存储
- 权限控制（普通用户/管理员）

### 数据集管理
- Excel 文件上传和解析
- 数据集去重校验
- 数据持久化存储

## 技术栈

### 主流框架 & 特性
- Spring Boot 2.7.x
- Spring MVC
- MyBatis + MyBatis Plus 数据访问（开启分页）
- Spring AOP 切面编程
- Spring Session

### 数据存储
- MySQL 数据库
- Redis 内存数据库（限流、缓存）
- RabbitMQ 消息队列

### AI 集成
- Ollama 本地大模型服务
- 支持自定义 Prompt 模板

### 工具类
- Easy Excel 表格处理
- Hutool 工具库
- Apache Commons Lang3 工具类
- Lombok 注解

### 接口文档
- Swagger + Knife4j 接口文档

## 快速上手

### 环境要求
- JDK 1.8+
- MySQL 5.7+
- Redis 6.0+
- RabbitMQ 3.8+（可选，用于 MQ 模式）
- Ollama（本地 AI 模型）

### 配置步骤

1. **修改数据库配置** (`application.yml`)：
```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/chart_flow_db
    username: root
    password: 123456
```

2. **执行数据库脚本**：
```bash
执行 sql/create_table.sql 创建数据库表
```

3. **启动 Ollama 服务**：
```bash
ollama run qwen2.5:7b
```

4. **启动项目**：
```bash
mvn spring-boot:run
```

5. **访问接口文档**：
```
http://localhost:8101/api/doc.html
```

## 核心 API

### 图表相关

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/chart/gen` | POST | 同步生成图表 |
| `/api/chart/gen/async` | POST | 异步生成图表（线程池） |
| `/api/chart/gen/async/mq` | POST | 异步生成图表（MQ） |
| `/api/chart/my` | GET | 获取我的图表列表 |
| `/api/chart/{id}` | GET | 获取图表详情 |
| `/api/chart/add` | POST | 创建图表 |
| `/api/chart/delete` | POST | 删除图表 |

### 用户相关

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/user/register` | POST | 用户注册 |
| `/api/user/login` | POST | 用户登录 |
| `/api/user/logout` | POST | 用户注销 |
| `/api/user/my` | GET | 获取当前用户信息 |

### Prompt 模板相关

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/prompt/list` | GET | 获取模板列表 |
| `/api/prompt/add` | POST | 添加模板 |
| `/api/prompt/update` | POST | 更新模板 |
| `/api/prompt/delete` | POST | 删除模板 |

### 数据集相关

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/dataset/upload` | POST | 上传数据集 |
| `/api/dataset/my` | GET | 获取我的数据集 |
| `/api/dataset/{id}` | GET | 获取数据集详情 |

## AI 响应格式

AI 生成的内容需按照以下格式输出：

```
【【【【【
{前端 Echarts V5 的 option 配置对象js代码，直接以{"title": {开头}
【【【【【
{明确的数据分析结论、越详细越好}
```

## 项目结构

```
src/main/java/com/chartflow/core/
├── annotation/          # 自定义注解
├── aop/                 # AOP 切面
├── bizmq/               # 业务消息队列（图表生成）
├── common/              # 通用响应和工具类
├── config/              # 配置类
├── constant/            # 常量定义
├── controller/          # REST API 控制器
├── exception/           # 异常处理
├── manager/             # 业务管理器（AI、存储等）
├── mapper/              # MyBatis Mapper
├── model/               # 数据模型（DTO、Entity、VO）
├── mq/                  # MQ 示例代码
├── service/             # 业务服务接口和实现
├── utils/               # 工具类
└── MainApplication.java # 启动类
```

## 配置说明

### Ollama 配置
```yaml
ollama:
  base-url: http://localhost:11434/v1
  model: qwen2.5:7b
```

### 邮件服务配置（环境变量）
```yaml
mcp:
  email:
    server-path: ./mcp_server/mcp_email_server.py
    smtp-server: smtp.qq.com
    smtp-port: 587
    smtp-user: ${MCP_SMTP_USER:}
    smtp-password: ${MCP_SMTP_PASSWORD:}
```

## 注意事项

1. 启动前请确保 Ollama 服务已运行
2. 邮件服务需要配置正确的 SMTP 账号密码
3. MQ 模式需要先启动 RabbitMQ 服务
4. 上传的 Excel 文件大小限制为 50MB

## License

MIT License