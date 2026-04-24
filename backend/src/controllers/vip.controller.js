const db = require('../models/db');

function generateOrderId() {
  return 'ORD' + Date.now() + Math.floor(Math.random() * 1000);
}

exports.createVipOrder = async (req, res) => {
  const { userId } = req.user;
  const { planId, payMethod } = req.body;

  let amount = 0;
  let durationMs = 0;

  if (planId === '1') {
    amount = 6.00;
    durationMs = 30 * 24 * 3600 * 1000;
  } else if (planId === '12') {
    amount = 60.00;
    durationMs = 365 * 24 * 3600 * 1000; // 一年
  } else {
    return res.status(400).json({ error: '无效的套餐ID' });
  }

  const orderId = generateOrderId();

  try {
    await db.run('INSERT INTO orders (order_id, user_id, plan_id, amount, status, pay_method, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)', [
      orderId, userId, planId, amount, 'pending', payMethod, Date.now()
    ]);

    // TODO: 调用真实的支付宝/微信统一下单接口获取支付链接或调起参数
    const mockPayUrl = `alipays://platformapi/startapp?appId=20000067&url=http://mock-pay-success.com`;

    res.json({
      orderId,
      payUrl: mockPayUrl
    });
  } catch (error) {
    console.error('Create order error:', error);
    res.status(500).json({ error: '订单创建失败' });
  }
};

// 支付回调处理逻辑
const handlePaymentSuccess = async (orderId) => {
  const order = await db.get('SELECT * FROM orders WHERE order_id = ? AND status = "pending"', [orderId]);
  if (!order) return false;

  await db.run('UPDATE orders SET status = "paid", paid_at = ? WHERE order_id = ?', [Date.now(), orderId]);

  let durationMs = 0;
  if (order.plan_id === '1') durationMs = 30 * 24 * 3600 * 1000;
  if (order.plan_id === '12') durationMs = 365 * 24 * 3600 * 1000;

  const user = await db.get('SELECT vip_expire_at FROM users WHERE user_id = ?', [order.user_id]);
  let newExpireAt = Date.now() + durationMs;
  if (user && user.vip_expire_at > Date.now()) {
    newExpireAt = user.vip_expire_at + durationMs;
  }

  await db.run('UPDATE users SET is_vip = 1, vip_expire_at = ? WHERE user_id = ?', [newExpireAt, order.user_id]);
  return true;
};

// 支付宝异步回调
exports.alipayCallback = async (req, res) => {
  // TODO: 验签支付宝的数据
  // mock 假回调
  const { out_trade_no, trade_status } = req.body;
  
  if (trade_status === 'TRADE_SUCCESS') {
    await handlePaymentSuccess(out_trade_no);
    res.send('success');
  } else {
    res.send('fail');
  }
};

// 微信异步回调
exports.wechatCallback = async (req, res) => {
  // TODO: 解析和验签微信的 XML 或 JSON 支付回调数据
  res.send('<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>');
};
