const sqlite3 = require('sqlite3').verbose();
const path = require('path');

const dbPath = path.resolve(__dirname, '../../ledger.db');
const db = new sqlite3.Database(dbPath);

function initDb() {
  db.serialize(() => {
    // 用户表
    db.run(`
      CREATE TABLE IF NOT EXISTS users (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id TEXT UNIQUE,
        phone TEXT UNIQUE,
        wechat_openid TEXT UNIQUE,
        wechat_unionid TEXT,
        is_vip INTEGER DEFAULT 0,
        vip_expire_at INTEGER,
        created_at INTEGER
      )
    `);

    // 同步数据表 (每个用户一个最新快照，或多条记录)
    db.run(`
      CREATE TABLE IF NOT EXISTS sync_data (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id TEXT UNIQUE,
        data_json TEXT,
        updated_at INTEGER
      )
    `);

    // 订单表
    db.run(`
      CREATE TABLE IF NOT EXISTS orders (
        order_id TEXT PRIMARY KEY,
        user_id TEXT,
        plan_id TEXT,
        amount REAL,
        status TEXT, -- 'pending', 'paid', 'failed'
        pay_method TEXT,
        created_at INTEGER,
        paid_at INTEGER
      )
    `);
  });
}

// 封装为 Promise，方便使用
const run = (sql, params = []) => {
  return new Promise((resolve, reject) => {
    db.run(sql, params, function (err) {
      if (err) reject(err);
      else resolve(this); // this.lastID, this.changes
    });
  });
};

const get = (sql, params = []) => {
  return new Promise((resolve, reject) => {
    db.get(sql, params, (err, row) => {
      if (err) reject(err);
      else resolve(row);
    });
  });
};

const all = (sql, params = []) => {
  return new Promise((resolve, reject) => {
    db.all(sql, params, (err, rows) => {
      if (err) reject(err);
      else resolve(rows);
    });
  });
};

module.exports = {
  db,
  initDb,
  run,
  get,
  all
};
