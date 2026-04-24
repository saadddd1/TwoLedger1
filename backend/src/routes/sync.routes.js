const express = require('express');
const router = express.Router();
const syncController = require('../controllers/sync.controller');
const { authenticateToken } = require('../utils/auth');

// 全局前置：数据同步需要登录
router.use(authenticateToken);

// 将客户端的 SQLite 数据库备份/同步数据上传到服务器
router.post('/upload', syncController.uploadSyncData);

// 客户端从服务器拉取云端数据恢复
router.get('/download', syncController.downloadSyncData);

module.exports = router;
