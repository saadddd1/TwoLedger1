# 账本App 项目开发与上线规划文档

## 1. 项目简介

这是一个支持自动记账、资产管理和云数据同步的记账应用。
后端采用 Node.js (Express) 使用 Serverless 或传统服务器部署，存储使用 SQLite (后续可按需向 MySQL/PostgreSQL 迁移)。
Android 客户端主要由 Jetpack Compose 构建，通过辅助功能和通知监听来实现账单拦截与半自动记账效果。

## 2. 软件架构设计

该应用包含两大部分：

### 2.1 Android 客户端 (app 目录)
- **UI框架**: Jetpack Compose 现代化声明式界面渲染。
- **数据存储**: Room Database 用于本地存储资产（`Item`）和流水账单（`AutoBill`）。
- **后台机制**:
  - `AccessibilityMonitorService`: 通过辅助功能抓取第三方应用支付完成界面的元素。
  - `NotificationMonitorService`: 监听系统状态栏中的支付通知进行快捷记账。
- **网路拉取**: Retrofit 负责调用后端接口。
- **安全与签名**: 已经配置了 V1~V4 签名机制生成可用 APK 包 (`debug.keystore`)。

### 2.2 后端服务 (backend 目录)
- **框架**: Node.js + Express
- **数据库**: SQLite (`ledger.db`) 用于用户数据存储。
- **主要模块**:
  - **Auth**: `auth.controller.js` 负责处理手机号码、微信号的登录逻辑，并派发 JWT Token。
  - **Vip**: `vip.controller.js` 用于触发支付宝/微信接口（Mock）生成订单，以及监听第三方回调写入 VIP 会员状态。
  - **Sync**: `sync.controller.js` 检测权限（VIP拦截），实现对端全量同步备份包的 JSON 上传和下载。

## 3. 会员与付费机制设计

根据业务设计，APP 的基础记账功能完全免费，但高级特权属于付费会员。

- **功能拦截点**: 任何涉及到云端 API (`ApiClient.apiService`) 操作的如数据同步、VIP开通等，都强制需要通过 `AuthSession.isLoggedIn` 判断。
- **数据同步**: 在后端的 `sync.controller.js` 设置了硬性检查，只有数据库中 `is_vip=1` 且 `vip_expire_at` 大于当前的请求才予以处理上云与下发。
- **收费标准 (预估)**: 挂载到阿里/微信后，分为包月(¥6.00) 和包年(¥60.00)。

## 4. 后续环境部署步骤（给你的实操指南）

### 4.1 阿里云服务器购买及配置
1. 去阿里云 (Aliyun) 选择一台基础型 ECS 实例（由于后端用了轻量 Node.js 和 sqlite3，2C2G 足以支撑十万人）。
2. 在服务器上安装 **Node.js**:
   ```bash
   curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
   sudo apt-get install -y nodejs
   ```
3. 开放阿里云入站端口：在安全组规则中添加开放 **8080** 端口。

### 4.2 后端代码部署
1. 将 `backend` 文件夹打包传到服务器。
2. 安装依赖并持续运行（可使用 PM2）：
   ```bash
   cd backend
   npm install
   npm install -g pm2
   pm2 start src/index.js --name ledger-api
   ```
3. 配置真实参数：后期你需要去 `backend/src/controllers/auth.controller.js` 填入真实微信的小程序 AppID/Secret；去 `vip.controller.js` 集成真实支付宝商户 SDK 平台密钥。

### 4.3 客户端联调
打开 Android 项目下的 `app/src/main/java/com/example/ledger/network/ApiClient.kt` 文件。
将其中的 `BASE_URL = "http://your-aliyun-server-ip.com"` 替换为你刚刚阿里云 ECS 分配的公网IP地址即可。

## 5. 项目待优化项 (TO-DO)
- [] 客户端 `LoginScreen` 目前使用 `delay(1000)` 虚拟请求并Mock成功，需要解开注释放开 Retrofit 真实网络请求。
- [] 使用微信开放平台申请移动应用（AppID），集成微信原生的 OpenAPI 以取代测试的 Mock ID。
- [] 将本地打包生成的 APK，放到各大应用市场（应用宝/酷安）或利用云服务器的域名做简易分发网页进行下载分发。

> 该项目结构及文档已自动生成并伴代码存储入库。
