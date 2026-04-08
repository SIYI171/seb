# SEB Analytics

一个开箱即用的网站统计系统。

适合个人站点、产品官网、落地页和小型项目使用，支持后台查看、公开分享和追踪脚本接入。

## 功能

- 多站点管理
- PV、UV、实时在线
- 访问趋势、浏览器、系统、地区分布
- 热门页面、最近访问、IP 排行
- 会话分析
- 公开分享页
- CSV 导出
- 支持桌面端和移动端

## Release 文件

在 `release` 中会提供：

- `seb-frontend-dist.zip`
- `seb-backend-1.0.0.jar`

你只需要准备：

- Java 17+
- MySQL 8+
- 一个 Web 服务环境（例如 Nginx）

## 快速使用

### 1. 创建数据库

先创建数据库：

```sql
CREATE DATABASE seb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

然后导入初始化文件：

```bash
mysql -u root -p seb < database/seb.sql
```

### 2. 启动后端

运行 release 中的后端包：

```bash
java -jar seb-backend-1.0.0.jar
```

默认端口是 `7322`。

如需自定义数据库连接，可通过环境变量配置：

```bash
SERVER_PORT=7322
DB_HOST=localhost
DB_PORT=3306
DB_NAME=seb
DB_USER=seb
DB_PASS=123456
```

### 3. 部署前端

把 `seb-frontend-dist.zip` 解压到网站目录。

例如：

```text
/var/www/seb/
├── index.html
├── assets/
└── tracker/
    └── seb.js
```

### 4. 配置反向代理

示例：

```nginx
server {
    listen 80;
    server_name your-domain.com;

    root /var/www/seb;
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
        try_files $uri =404;
    }
}
```

### 5. 打开后台

部署完成后，访问你的域名即可进入系统。

首次使用时，先初始化管理员账号，再登录后台添加网站。

## 嵌入统计脚本

把下面的代码放到你的网站中：

```html
<script
  src="https://your-domain.com/tracker/seb.js"
  data-tracking-id="your_tracking_id">
</script>
```

如果采集接口不是默认地址，也可以手动指定：

```html
<script
  src="https://your-domain.com/tracker/seb.js"
  data-tracking-id="your_tracking_id"
  data-endpoint="https://your-domain.com/api/collect">
</script>
```

## 使用方式

1. 登录后台
2. 添加网站
3. 复制追踪脚本并嵌入你的网站
4. 回到后台查看统计
5. 如有需要，可开启分享链接给他人查看

## 分享页

分享页适合展示：

- 访问趋势
- 基础访问构成
- 热门页面和访问概况

分享页不给访客后台操作权限，只用于查看数据。

## License

[MIT License](LICENSE)
