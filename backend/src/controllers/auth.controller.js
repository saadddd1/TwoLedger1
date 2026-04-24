const db = require('../models/db');
const { generateToken } = require('../utils/auth');

function generateUserId() {
  return 'usr_' + Date.now().toString(36) + Math.random().toString(36).substr(2, 5);
}

// 检查并返回 VIP 状态
function checkVipStatus(user) {
  if (user.is_vip === 1 && user.vip_expire_at) {
    if (Date.now() < user.vip_expire_at) {
      return { isVip: true, vipExpireAt: user.vip_expire_at };
    } else {
      // 过期了，这里可以更新数据库，但暂时只返回状态
      return { isVip: false, vipExpireAt: user.vip_expire_at };
    }
  }
  return { isVip: false, vipExpireAt: null };
}

exports.loginByPhone = async (req, res) => {
  const { phone, code } = req.body;

  if (!phone || !code) {
    return res.status(400).json({ error: '手机号和验证码必填' });
  }

  // TODO: 调用阿里云短信接口校验验证码
  // FIXME: 这里使用固定验证码 123456 用于测试
  if (code !== '123456' && phone !== '13800000000') { // 方便审核和测试
     // return res.status(400).json({ error: '验证码错误' });
  }

  try {
    // 查找用户
    let user = await db.get('SELECT * FROM users WHERE phone = ?', [phone]);

    if (!user) {
      // 注册新用户
      const userId = generateUserId();
      await db.run('INSERT INTO users (user_id, phone, created_at) VALUES (?, ?, ?)', [userId, phone, Date.now()]);
      user = await db.get('SELECT * FROM users WHERE user_id = ?', [userId]);
    }

    const { isVip, vipExpireAt } = checkVipStatus(user);
    const token = generateToken(user.user_id);

    res.json({
      token,
      userId: user.user_id,
      isVip,
      vipExpireAt
    });
  } catch (error) {
    console.error('Phone login error:', error);
    res.status(500).json({ error: '服务器内部错误' });
  }
};

exports.loginByWechat = async (req, res) => {
  const { code } = req.body;

  if (!code) {
    return res.status(400).json({ error: '微信授权code必填' });
  }

  try {
    // TODO: 调用微信服务器换取 openid 和 unionid
    // GET https://api.weixin.qq.com/sns/jscode2session?appid=APPID&secret=SECRET&js_code=JSCODE&grant_type=authorization_code
    // 这里 mock openid
    const openid = 'mock_wx_openid_' + code;

    let user = await db.get('SELECT * FROM users WHERE wechat_openid = ?', [openid]);

    if (!user) {
      const userId = generateUserId();
      await db.run('INSERT INTO users (user_id, wechat_openid, created_at) VALUES (?, ?, ?)', [userId, openid, Date.now()]);
      user = await db.get('SELECT * FROM users WHERE user_id = ?', [userId]);
    }

    const { isVip, vipExpireAt } = checkVipStatus(user);
    const token = generateToken(user.user_id);

    res.json({
      token,
      userId: user.user_id,
      isVip,
      vipExpireAt
    });
  } catch (error) {
    console.error('Wechat login error:', error);
    res.status(500).json({ error: '服务器内部错误' });
  }
};
