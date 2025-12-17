package org.awaioi.liteback.task;

import org.awaioi.liteback.LiteBack;
import org.awaioi.liteback.manager.ConfigManager;
import org.awaioi.liteback.manager.DeathStorageManager;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * 清理任务类
 * 定期清理过期的死亡记录和离线玩家数据
 */
public class CleanupTask extends BukkitRunnable {
    private final LiteBack plugin;
    private final ConfigManager configManager;
    private final DeathStorageManager storageManager;
    
    private int cleanupCount = 0;
    private long lastCleanupTime = 0;
    
    public CleanupTask(LiteBack plugin, ConfigManager configManager, DeathStorageManager storageManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.storageManager = storageManager;
    }
    
    @Override
    public void run() {
        try {
            cleanupCount++;
            lastCleanupTime = System.currentTimeMillis();
            
            // 执行清理操作
            performCleanup();
            
            // 每10次清理输出一次统计信息
            if (cleanupCount % 10 == 0) {
                showCleanupStatistics();
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("清理任务执行失败: " + e.getMessage());
        }
    }
    
    /**
     * 执行清理操作
     */
    private void performCleanup() {
        // 清理过期的死亡记录
        storageManager.cleanupExpiredRecords();
        
        // 清理离线玩家数据
        storageManager.cleanupOfflinePlayerData();
        
        // === 新增：文件大小监控和优化 ===
        performFileSizeCheck();
        
        // 记录清理日志
        if (cleanupCount % 5 == 0) {
            plugin.getLogger().info("清理任务执行完成 (第 " + cleanupCount + " 次)");
        }
    }
    
    /**
     * 执行文件大小检查和优化
     */
    private void performFileSizeCheck() {
        try {
            // 每10次清理任务执行一次文件大小检查（每50分钟）
            if (cleanupCount % 10 == 0) {
                
                // 显示当前文件统计信息
                String fileStats = storageManager.getFileStatistics();
                plugin.getLogger().info("文件检查: " + fileStats);
                
                // 检查文件大小是否超过阈值，如果启用自动优化则执行优化
                if (storageManager.isFileSizeOverThreshold()) {
                    plugin.getLogger().warning("数据文件大小超过阈值，开始自动优化...");
                    
                    // 执行文件大小优化
                    storageManager.performFileSizeOptimization();
                    
                    // 优化后再次显示统计信息
                    String newFileStats = storageManager.getFileStatistics();
                    plugin.getLogger().info("优化后: " + newFileStats);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("文件大小检查失败: " + e.getMessage());
        }
    }
    
    /**
     * 显示清理统计信息
     */
    private void showCleanupStatistics() {
        plugin.getLogger().info("=== LiteBack 清理统计 ===");
        plugin.getLogger().info("累计清理次数: " + cleanupCount);
        plugin.getLogger().info("上次清理时间: " + formatTimestamp(lastCleanupTime));
        
        // 统计当前在线玩家死亡记录数量
        int totalRecords = 0;
        int playersWithRecords = 0;
        
        for (org.bukkit.entity.Player player : plugin.getServer().getOnlinePlayers()) {
            if (storageManager.hasDeathRecord(player.getUniqueId())) {
                playersWithRecords++;
                totalRecords += 1; // 现在每个玩家最多只有1个记录
            }
        }
        
        plugin.getLogger().info("在线玩家有死亡记录数量: " + playersWithRecords);
        plugin.getLogger().info("总死亡记录数: " + totalRecords);
        plugin.getLogger().info("========================");
    }
    
    /**
     * 格式化时间戳
     */
    private String formatTimestamp(long timestamp) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            .format(new java.util.Date(timestamp));
    }
    
    /**
     * 强制执行一次清理（管理员命令调用）
     */
    public void forceCleanup() {
        try {
            plugin.getLogger().info("开始执行强制清理...");
            
            int beforeCleanup = cleanupCount;
            performCleanup();
            
            plugin.getLogger().info("强制清理完成，共执行 " + (cleanupCount - beforeCleanup) + " 次清理操作");
            
        } catch (Exception e) {
            plugin.getLogger().severe("强制清理失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 获取清理次数
     */
    public int getCleanupCount() {
        return cleanupCount;
    }
    
    /**
     * 获取上次清理时间
     */
    public long getLastCleanupTime() {
        return lastCleanupTime;
    }
}