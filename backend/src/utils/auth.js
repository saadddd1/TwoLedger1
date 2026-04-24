const jwt = require('jsonwebtoken');

const JWT_SECRET = process.env.JWT_SECRET || 'ledger_super_secret_key_change_in_production';

function generateToken(userId) {
  return jwt.sign({ userId }, JWT_SECRET, { expiresIn: '30d' });
}

function verifyToken(token) {
  try {
    return jwt.verify(token, JWT_SECRET);
  } catch (error) {
    return null;
  }
}

// 提取并验证 token 的中间件
function authenticateToken(req, res, next) {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1]; // "Bearer TOKEN"

  if (!token) return res.status(401).json({ error: '未提供认证Token' });

  const decoded = verifyToken(token);
  if (!decoded) return res.status(403).json({ error: 'Token无效或已过期' });

  req.user = decoded; // { userId }
  next();
}

module.exports = {
  generateToken,
  verifyToken,
  authenticateToken
};
