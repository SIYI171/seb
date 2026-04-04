# SEB Analytics

一个轻量级的网站访问统计分析工具，支持实时数据展示和分享功能。

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen)
![Vue](https://img.shields.io/badge/Vue-3-brightgreen)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)

## 功能特性

- 📊 **实时统计** - 实时在线人数、页面访问量、访客数
- 📈 **数据可视化** - 访问趋势、浏览器分布、操作系统分布、地区分布图表
- 🌍 **IP 地理位置** - 自动解析访客 IP 所在地区
- 🔗 **分享功能** - 生成分享链接，无需登录即可查看统计数据
- 📱 **响应式设计** - 支持桌面端和移动端
- 🔒 **单用户管理** - 简洁的管理员登录，支持多网站管理

## 技术栈

### 后端
- Java 17
- Spring Boot 3.2
- MyBatis Plus
- MySQL 8.0
- MaxMind GeoIP2

### 前端
- Vue 3
- TypeScript
- Tailwind CSS
- shadcn-vue
- ECharts

## 快速开始

### 环境要求

- Java 17+
- Node.js 18+
- MySQL 8.0+
- Maven 3.8+

### 本地开发

#### 1. 克隆项目

```bash
git clone https://github.com/your-username/seb.git
cd seb
```

#### 2. 创建数据库

```sql
CREATE DATABASE seb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

#### 3. 配置后端

编辑 `backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/seb
    username: your_username
    password: your_password
```

#### 4. 下载 GeoIP 数据库

从 [MaxMind](https://dev.maxmind.com/geoip/geolite2-free-geolocation-data) 下载 GeoLite2-City.mmdb，放入 `backend/data/` 目录。

#### 5. 启动后端

```bash
cd backend
mvn spring-boot:run
```

#### 6. 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问 http://localhost:3000 即可使用。

### 生产部署

#### 构建

```bash
# 后端
cd backend
mvn clean package -DskipTests

# 前端
cd frontend
npm run build
```

#### 运行

```bash
java -jar backend/target/seb-backend-1.0.0.jar
```

## Docker 部署

### 使用 Docker Compose（推荐）

```bash
docker-compose up -d
```

访问 http://localhost:3000

### 手动构建

```bash
# 构建镜像
docker build -t seb-analytics .

# 运行容器
docker run -d \
  -p 3000:3000 \
  -p 7322:7322 \
  -e DB_HOST=localhost \
  -e DB_NAME=seb \
  -e DB_USER=root \
  -e DB_PASS=password \
  seb-analytics
```

### 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `DB_HOST` | 数据库地址 | localhost |
| `DB_PORT` | 数据库端口 | 3306 |
| `DB_NAME` | 数据库名称 | seb |
| `DB_USER` | 数据库用户名 | root |
| `DB_PASS` | 数据库密码 | - |
| `SERVER_PORT` | 后端端口 | 7322 |

## 使用说明

### 添加网站

1. 登录管理后台
2. 点击「添加网站」
3. 填写网站名称和域名
4. 获取追踪代码

### 嵌入追踪代码

将以下代码添加到你的网站 `</body>` 标签前：

```html
<script src="https://your-domain.com/tracker/seb.js" data-tracking-id="your_tracking_id"></script>
```

### 分享统计数据

1. 进入网站统计页面
2. 点击「分享」按钮
3. 开启分享功能
4. 复制分享链接

## Nginx 配置示例

```nginx
server {
    listen 80;
    server_name your-domain.com;
    
    root /var/www/seb/dist;
    index index.html;
    
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    location /api/ {
        proxy_pass http://127.0.0.1:7322;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
    
    location /tracker/ {
        root /var/www/seb/dist;
    }
}
```

## 项目结构

```
seb/
├── backend/                    # 后端代码
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       └── resources/
│   └── pom.xml
├── frontend/                   # 前端代码
│   ├── src/
│   ├── public/
│   └── package.json
├── docker-compose.yml
├── Dockerfile
└── README.md
```

## License

[MIT License](LICENSE)

## 致谢

- [Umami](https://umami.is/) - 设计灵感来源
- [MaxMind GeoLite2](https://dev.maxmind.com/geoip/geolite2-free-geolocation-data) - IP 地理位置数据
