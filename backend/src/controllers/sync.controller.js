const db = require('../models/db');

// 这个控制器处理云同步，前提是用户已经开通 VIP
exports.uploadSyncData = async (req, res) => {
  const { userId } = req.user;
  const { dataJson } = req.body;

  if (!dataJson) {
    return res.status(400).json({ error: '没有需要同步的数据' });
  }

  try {
    // 检查是否为VIP
    const user = await db.get('SELECT is_vip, vip_expire_at FROM users WHERE user_id = ?', [userId]);
    if (!user || user.is_vip !== 1 || user.vip_expire_at < Date.now()) {
      return res.status(403).json({ error: '此功能仅限VIP可用，请先开通或续费VIP' });
    }

    // 存入云端（我们这里直接将整个 JSON blob 存储在 db，实际大型应用可以存 OSS，数据库里存 URL）
    const existingSync = await db.get('SELECT id FROM sync_data WHERE user_id = ?', [userId]);

    if (existingSync) {
      await db.run('UPDATE sync_data SET data_json = ?, updated_at = ? WHERE user_id = ?', [dataJson, Date.now(), userId]);
    } else {
      await db.run('INSERT INTO sync_data (user_id, data_json, updated_at) VALUES (?, ?, ?)', [userId, dataJson, Date.now()]);
    }

    res.json({ success: true, message: '数据同步成功' });
  } catch (error) {
    console.error('Sync upload error:', error);
    res.status(500).json({ error: '数据同步失败' });
  }
};

exports.downloadSyncData = async (req, res) => {
  const { userId } = req.user;

  try {
    // 检查是否为VIP
    const user = await db.get('SELECT is_vip, vip_expire_at FROM users WHERE user_id = ?', [userId]);
    if (!user || user.is_vip !== 1 || user.vip_expire_at < Date.now()) {
      return res.status(403).json({ error: '此功能仅限VIP可用，请先开通或续费VIP' });
    }

    const syncRecord = await db.get('SELECT data_json, updated_at FROM sync_data WHERE user_id = ?', [userId]);

    if (!syncRecord) {
      return res.json({ dataJson: null, message: '云端暂无备份数据' });
    }

    res.json({ success: true, dataJson: syncRecord.data_json, updatedAt: syncRecord.updated_at });
  } catch (error) {
    console.error('Sync download error:', error);
    res.status(500).json({ error: '数据拉取失败' });
  }
};
