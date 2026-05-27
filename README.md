# 开放实验室管理系统的设计与实现

基于 Spring Boot 3.2 + Vue 3 + AI 的开放实验室管理系统，提供实验室预约、设备管理、安全检查、AI 智能分析等完整功能。

## 技术栈

### 后端
- **框架**: Spring Boot 3.2.4
- **安全**: Spring Security + JWT
- **ORM**: MyBatis-Plus 3.5.5
- **数据库**: MySQL 8.x
- **缓存**: Caffeine (本地缓存) / Redis (可选)
- **邮件**: Spring Mail
- **AOP**: AspectJ (操作日志)
- **Java 版本**: 21

### 前端
- **框架**: Vue 3
- **构建工具**: Vite
- **UI 组件**: Element Plus
- **状态管理**: Pinia
- **路由**: Vue Router

### AI 服务
- **Python**: 3.9+
- **故障预测**: Prophet 时间序列分析
- **智能问答**: BERT 模型
- **安全检测**: YOLOv8
- **排课优化**: 遗传算法

## 项目结构
├── lab-backend/              # 后端 (Spring Boot)
│   ├── src/main/java/com/lab/
│   │   ├── controller/       # REST API 控制器
│   │   ├── service/          # 业务逻辑层
│   │   ├── mapper/           # MyBatis-Plus Mapper
│   │   ├── entity/           # 实体类
│   │   ├── config/           # 配置类
│   │   ├── security/         # JWT 认证过滤器
│   │   ├── annotation/       # 自定义注解
│   │   ├── aspect/           # AOP 切面 (操作日志)
│   │   └── scheduled/        # 定时任务
│   └── src/main/resources/
│       ├── application.yml   # 配置文件
│       └── sql/              # 数据库脚本
├── lab-frontend/             # 前端 (Vue 3)
│   ├── src/api/              # API 接口
│   ├── src/views/            # 页面组件
│   ├── src/router/           # 路由配置
│   └── src/stores/           # 状态管理
├── ai-service/               # AI 微服务 (Python)
│   ├── app.py                # Flask 服务入口
│   ├── module_fault_online.py    # 故障预测
│   ├── module_qa_bert.py         # 智能问答
│   ├── module_safety.py          # 安全检测
│   └── module_schedule_ga.py     # 排课优化
└── AI-project/               # AI 模型与数据处理

## 功能模块

### 用户管理
- 用户注册/登录 (JWT 认证)
- 角色权限管理 (管理员/教师/学生)
- 邮箱验证码找回密码

### 实验室管理
- 实验室信息管理
- 实验室预约与审批
- 实验室使用记录
- 实验室安全检查

### 设备管理
- 设备台账管理
- 设备借用/归还
- 设备报修管理
- 设备出入库记录

### 教学管理
- 实验任务管理
- 学生考勤管理
- 师生绑定管理
- 实验报告提交

### AI 智能功能
- **故障预测**: 基于 Prophet 的设备故障预测
- **智能问答**: 基于 BERT 的知识问答
- **安全检测**: 基于 YOLOv8 的实验室安全监控
- **排课优化**: 基于遗传算法的智能排课

### 系统管理
- 操作日志记录
- 系统配置管理
- 数据库备份
- 公告管理

## 快速开始

### 环境要求
- JDK 21+
- Maven 3.8+
- Node.js 18+
- MySQL 8.0+
- Python 3.9+ (AI 服务)

### 后端启动

```bash
# 1. 创建数据库并导入 SQL 脚本
mysql -u root -p < lab-backend/src/main/resources/sql/schema_complete.sql

# 2. 修改配置文件
# 编辑 lab-backend/src/main/resources/application.yml
# 配置数据库连接、邮件服务器等信息

# 3. 启动后端
cd lab-backend
mvn clean install
mvn spring-boot:run
```

后端默认运行在 `http://localhost:8080`

### 前端启动

```bash
cd lab-frontend
npm install
npm run dev
```

前端默认运行在 `http://localhost:5173`

### AI 服务启动 (可选)

```bash
cd ai-service
pip install -r requirements.txt
python app.py
```

## 配置说明

### application.yml 主要配置项

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/lab_management
    username: root
    password: your_password
  mail:
    host: smtp.example.com
    username: your_email@example.com
    password: your_email_password
```

## 数据库

数据库脚本位于 `lab-backend/src/main/resources/sql/`:
- `schema_complete.sql` - 完整数据库结构
- `schema_core.sql` - 核心表结构
- `schema_simple.sql` - 简化表结构

## 许可证

本项目仅供学习和研究使用。
