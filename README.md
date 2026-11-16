# 校园树洞

基于微信小程序、Spring Boot 和 MySQL 的课程项目。目前已实现树洞列表、本地发布、我的树洞和删除功能。

## 项目结构

```text
campus-treehole/
├── backend/campus-treehole/    Spring Boot 后端（Maven 项目）
│   ├── src/main/java/          后端启动类、配置和树洞业务代码
│   │   └── .../post/           按 controller、service、repository、entity、dto 分包
│   ├── src/main/resources/     应用配置
│   ├── src/test/               后端测试
│   ├── scripts/                本地数据库初始化脚本
│   ├── pom.xml                 Maven 项目配置
│   └── README.md               后端配置和接口说明
├── miniprogram/                微信小程序前端
│   ├── pages/index/            树洞广场和发布页面
│   ├── pages/mine/             我的树洞和删除页面
│   ├── utils/request.js        后端请求工具
│   └── app.*                   小程序入口和全局配置
├── docs/sql/                   数据库建表脚本
└── .gitignore                  本地配置与构建产物忽略规则
```

## 本地运行

1. 准备 JDK 17、MySQL 8.4、IDEA 和微信开发者工具。用 IDEA 打开根目录后，后端 Maven 项目位于 `backend/campus-treehole/pom.xml`。
2. 按[后端说明](backend/campus-treehole/README.md)创建本地数据库及配置，并对 `campus_treehole` 数据库执行 [post 建表脚本](docs/sql/001_create_post.sql)。本地配置包含数据库密码，已被 Git 忽略。
3. 在 IDEA 中运行 `CampusTreeholeApplication`。默认接口地址为 `http://127.0.0.1:18081/api/posts`。
4. 在微信开发者工具中打开 `miniprogram` 目录并编译。本地 HTTP 调试需要在开发者工具的本地设置中勾选不校验合法域名、TLS 版本及 HTTPS 证书。

发布、我的树洞和删除功能目前仅在 `local` 开发模式可用，使用固定测试身份，服务仅监听本机。微信登录和点赞功能尚未实现。手机真机或服务器部署需要另外配置可访问的后端地址和小程序网络设置。
