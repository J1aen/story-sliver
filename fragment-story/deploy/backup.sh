#!/bin/bash
# 匿名故事碎片墙备份脚本：数据库 + 头像图片目录
# 为什么连图片一起备份：图片是用户数据，只备数据库会丢头像
BACKUP_DIR=/home/ubuntu/story/backups
mkdir -p "$BACKUP_DIR"

# 1. 备份 MySQL 数据库
# ⚠️ <数据库密码> 是占位符：部署到服务器前替换为实际密码，不要把真实密码提交到 git
mysqldump -ustory -p<数据库密码> story_sliver > "$BACKUP_DIR/story_$(date +%F_%H%M).sql"

# 2. 备份头像图片目录
tar -czf "$BACKUP_DIR/headimage_$(date +%F_%H%M).tar.gz" -C /home/ubuntu/story headimage 2>/dev/null

# 3. 只保留最近 14 天的备份
find "$BACKUP_DIR" -name '*.sql' -mtime +14 -delete
find "$BACKUP_DIR" -name '*.tar.gz' -mtime +14 -delete

# 定时执行：crontab -e 添加一行
# 0 3 * * * /home/ubuntu/story/backup.sh
