const express = require('express');
const router = express.Router();
const authController = require('../controllers/auth.controller');

// 手机号登录/注册
router.post('/login/phone', authController.loginByPhone);

// 微信登录/注册
router.post('/login/wechat', authController.loginByWechat);

module.exports = router;
