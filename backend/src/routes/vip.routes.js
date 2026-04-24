const express = require('express');
const router = express.Router();
const vipController = require('../controllers/vip.controller');
const { authenticateToken } = require('../utils/auth');

// 获取支付参数（需要登录）
router.post('/pay', authenticateToken, vipController.createVipOrder);

// 支付结果回调 (给支付宝/微信服务器的回调接口，不需要Token验证)
router.post('/callback/alipay', vipController.alipayCallback);
router.post('/callback/wechat', vipController.wechatCallback);

module.exports = router;
