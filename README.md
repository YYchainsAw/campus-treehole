# 校园树洞

基于微信小程序、Spring Boot 和 MySQL 的课程项目。目前支持本地账号注册登录、树洞发布、分类筛选、点赞、评论和管理自己的树洞；微信登录已有基础代码，尚需真实账号联调。

## 项目结构

```text
campus-treehole/
├── backend/campus-treehole/    Spring Boot 后端（Maven 项目）
│   ├── src/main/java/          后端启动类、配置和树洞业务代码
│   │   └── .../campus_treehole/ 按 controller、service、repository、entity、dto 等分层
│   ├── src/main/resources/     应用配置
│   ├── src/test/               后端测试
│   ├── scripts/                本地数据库初始化脚本
│   ├── pom.xml                 Maven 项目配置
│   └── README.md               后端配置和接口说明
├── miniprogram/                微信小程序前端
│   ├── pages/index/            树洞广场和发布页面
│   ├── pages/mine/             我的树洞和删除页面
│   ├── pages/comments/         评论页面
│   ├── utils/request.js        后端请求工具
│   └── app.*                   小程序入口和全局配置
├── docs/sql/                   数据库建表脚本
└── .gitignore                  本地配置与构建产物忽略规则
```

## 本地运行

1. 准备 JDK 17、MySQL 8.4、IDEA 和微信开发者工具。用 IDEA 打开根目录后，后端 Maven 项目位于 `backend/campus-treehole/pom.xml`，项目 SDK 和 Maven 使用的 JDK 均设置为 17。Maven 首次运行需要联网下载依赖。
2. 按[后端说明](backend/campus-treehole/README.md)创建本地数据库及配置，再用 IDEA 数据库工具连接 MySQL，选中 `campus_treehole` 数据库，依次打开并执行 `docs/sql` 下 001 至 006 的 SQL 文件。迁移前停止应用写入；`005_post_category.sql` 只执行一次，已有 `category` 字段时跳过。本地配置包含数据库密码，已被 Git 忽略。
3. 在 IDEA 中运行 `CampusTreeholeApplication`。默认接口地址为 `http://127.0.0.1:18081/api/posts`。
4. 在微信开发者工具中打开 `miniprogram` 目录并编译。本地 HTTP 调试需要在开发者工具的本地设置中勾选不校验合法域名、TLS 版本及 HTTPS 证书。

在“我的树洞”可以注册和登录本地演示账号，账号仅在 `local` 模式可用。未登录时该模式使用固定测试身份，服务仅监听本机。真实微信登录需要匹配小程序账号的 AppID 和 AppSecret，参见[后端说明](backend/campus-treehole/README.md)。手机真机或服务器部署需要另外配置可访问的 HTTPS 后端地址和小程序网络设置。

## 课程演示流程

1. 打开“我的树洞”，注册并登录一个本地账号。
2. 返回广场，选择分类并发布一条树洞，使用分类按钮筛选。
3. 给树洞点赞，进入评论页面发表一条评论。
4. 在“我的树洞”查看并删除刚发布的内容，回到广场确认已消失。
5. 如需展示不同用户的数据隔离，退出后注册另一个账号；本地测试身份和每个账号的数据独立。
