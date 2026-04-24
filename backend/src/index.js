require('dotenv').config();
const express = require('express');
const cors = require('cors');
const authRoutes = require('./routes/auth.routes');
const syncRoutes = require('./routes/sync.routes');
const vipRoutes = require('./routes/vip.routes');
const db = require('./models/db');

const app = express();

app.use(cors());
app.use(express.json({ limit: '50mb' }));

// 初始化数据库
db.initDb();

// 路由
app.use('/api/auth', authRoutes);
app.use('/api/sync', syncRoutes);
app.use('/api/vip', vipRoutes);

// 全局错误处理
app.use((err, req, res, next) => {
  console.error('Error:', err);
  res.status(500).json({ error: err.message || '内部服务器错误' });
});

const PORT = process.env.PORT || 8080;
app.listen(PORT, () => {
  console.log(`Backend server is running on port ${PORT}`);
});
