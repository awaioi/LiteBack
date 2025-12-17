package org.awaioi.liteback.manager;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.awaioi.liteback.LiteBack;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

/**
 * 死亡记录存储管理类
 * 负责管理玩家的死亡记录存储、读取和清理
 */
public class DeathStorageManager {
    private final LiteBack plugin;
    private final ConfigManager configManager;
    private File dataFile;
    private FileConfiguration dataConfig;
    
    // 内存存储：UUID -> 单个死亡记录
    private final Map<UUID, DeathRecord> memoryStorage = new ConcurrentHashMap<>();
    // 在线玩家最后一次传送时间记录
    private final Map<UUID, Long> lastTeleportTime = new ConcurrentHashMap<>();
    // 正在使用/back命令的玩家集合
    private final Set<UUID> usingBackCommand = ConcurrentHashMap.newKeySet();
    
    // === 存储优化相关 ===
    // 待保存到文件的数据缓冲队列
    private final Set<UUID> pendingSaveQueue = ConcurrentHashMap.newKeySet();
    // 最后一次文件保存时间
    private long lastFileSaveTime = 0;
    // 数据修改标记
    private volatile boolean dataModified = false;
    // 保存任务（避免重复调度）
    private volatile boolean saveTaskScheduled = false;
    
    public DeathStorageManager(LiteBack plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        loadData();
    }
    
    /**
     * 死亡记录类
     */
    public static class DeathRecord {
        private final Location location;
        private final long timestamp;
        private final String deathMessage;
        private final String dimension;
        
        public DeathRecord(Location location, long timestamp, String deathMessage) {
            this.location = location;
            this.timestamp = timestamp;
            this.deathMessage = deathMessage;
            this.dimension = location.getWorld().getEnvironment().name();
        }
        
        public Location getLocation() { return location; }
        public long getTimestamp() { return timestamp; }
        public String getDeathMessage() { return deathMessage; }
        public String getDimension() { return dimension; }
        
        public String getFormattedLocation() {
            return String.format("%.0f, %.0f, %.0f", 
                location.getX(), location.getY(), location.getZ());
        }
    }
    
    /**
     * 加载数据文件
     */
    private void loadData() {
        if (dataFile == null) {
            dataFile = new File(plugin.getDataFolder(), "data.yml");
        }
        
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("无法创建数据文件: " + e.getMessage());
            }
        }
        
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        loadMemoryStorage();
    }
    
    /**
     * 从配置文件加载内存存储
     */
    private void loadMemoryStorage() {
        memoryStorage.clear();
        
        for (String uuidStr : dataConfig.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                
                if (dataConfig.contains(uuidStr + ".death-record")) {
                    String recordString = dataConfig.getString(uuidStr + ".death-record");
                    DeathRecord record = parseDeathRecord(recordString);
                    if (record != null) {
                        memoryStorage.put(uuid, record);
                    }
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("无效的UUID在数据文件中: " + uuidStr);
            }
        }
    }
    
    /**
     * 解析死亡记录字符串
     */
    private DeathRecord parseDeathRecord(String recordString) {
        try {
            if (recordString == null || recordString.trim().isEmpty()) {
                return null;
            }
            
            String[] parts = recordString.split("\\|");
            if (parts.length < 5) return null;
            
            String worldName = parts[0];
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            long timestamp = Long.parseLong(parts[4]);
            String deathMessage = parts.length > 5 ? parts[5] : "";
            
            // 检查世界是否存在
            if (plugin.getServer().getWorld(worldName) == null) {
                return null;
            }
            
            Location location = new Location(plugin.getServer().getWorld(worldName), x, y, z);
            return new DeathRecord(location, timestamp, deathMessage);
            
        } catch (Exception e) {
            plugin.getLogger().warning("解析死亡记录失败: " + recordString + ", 错误: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 保存死亡记录（使用缓冲保存策略）
     */
    public void saveDeathRecord(Player player, Location location, String deathMessage) {
        UUID uuid = player.getUniqueId();
        DeathRecord record = new DeathRecord(location, System.currentTimeMillis(), deathMessage);
        
        // 保存最新的死亡记录到内存
        memoryStorage.put(uuid, record);
        
        // 添加到待保存队列
        pendingSaveQueue.add(uuid);
        dataModified = true;
        
        // 尝试异步保存
        scheduleAsyncSave();
        
        plugin.getLogger().info(player.getName() + " 的死亡点已记录: " + record.getFormattedLocation());
    }
    
    /**
     * 获取玩家的死亡记录
     */
    public DeathRecord getDeathRecord(UUID uuid) {
        return memoryStorage.get(uuid);
    }
    
    /**
     * 检查玩家是否有死亡记录
     */
    public boolean hasDeathRecord(UUID uuid) {
        return memoryStorage.containsKey(uuid);
    }
    
    /**
     * 删除玩家的死亡记录
     */
    public void removeDeathRecord(UUID uuid) {
        memoryStorage.remove(uuid);
        saveToFile();
    }
    
    /**
     * 保存到配置文件
     */
    private void saveToFile() {
        try {
            // 清空现有数据
            for (String key : new HashSet<>(dataConfig.getKeys(false))) {
                dataConfig.set(key, null);
            }
            
            // 保存内存存储到文件
            for (Map.Entry<UUID, DeathRecord> entry : memoryStorage.entrySet()) {
                UUID uuid = entry.getKey();
                DeathRecord record = entry.getValue();
                
                String recordString = String.format("%s|%.2f|%.2f|%.2f|%d|%s",
                    record.getLocation().getWorld().getName(),
                    record.getLocation().getX(),
                    record.getLocation().getY(),
                    record.getLocation().getZ(),
                    record.getTimestamp(),
                    record.getDeathMessage().replace("|", "\\|"));
                
                dataConfig.set(uuid.toString() + ".death-record", recordString);
            }
            
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("保存数据文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 设置玩家最后一次传送时间
     */
    public void setLastTeleportTime(UUID uuid) {
        lastTeleportTime.put(uuid, System.currentTimeMillis());
    }
    
    /**
     * 检查玩家是否在冷却中
     */
    public boolean isOnCooldown(UUID uuid) {
        if (!configManager.isCooldownEnabled()) {
            return false;
        }
        
        // 检查是否有无冷却权限
        if (plugin.getServer().getPlayer(uuid) != null) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.hasPermission("liteback.back.unlimited")) {
                return false;
            }
        }
        
        Long lastTime = lastTeleportTime.get(uuid);
        if (lastTime == null) {
            return false;
        }
        
        long cooldownMillis = configManager.getCooldownDuration() * 1000L;
        return System.currentTimeMillis() - lastTime < cooldownMillis;
    }
    
    /**
     * 获取玩家剩余冷却时间(秒)
     */
    public int getRemainingCooldown(UUID uuid) {
        Long lastTime = lastTeleportTime.get(uuid);
        if (lastTime == null) {
            return 0;
        }
        
        long cooldownMillis = configManager.getCooldownDuration() * 1000L;
        long elapsed = System.currentTimeMillis() - lastTime;
        long remaining = cooldownMillis - elapsed;
        
        return remaining > 0 ? (int) Math.ceil(remaining / 1000.0) : 0;
    }
    
    /**
     * 设置玩家正在使用/back命令
     */
    public void setUsingBackCommand(UUID uuid, boolean using) {
        if (using) {
            usingBackCommand.add(uuid);
        } else {
            usingBackCommand.remove(uuid);
        }
    }
    
    /**
     * 检查玩家是否正在使用/back命令
     */
    public boolean isUsingBackCommand(UUID uuid) {
        return usingBackCommand.contains(uuid);
    }
    
    /**
     * 清理过期记录
     */
    public void cleanupExpiredRecords() {
        long currentTime = System.currentTimeMillis();
        long daysToMs = configManager.getDeathRecordDays() * 24L * 60L * 60L * 1000L;
        
        Iterator<Map.Entry<UUID, DeathRecord>> iterator = memoryStorage.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<UUID, DeathRecord> entry = iterator.next();
            DeathRecord record = entry.getValue();
            
            // 移除过期的记录
            if (currentTime - record.getTimestamp() > daysToMs) {
                iterator.remove();
                lastTeleportTime.remove(entry.getKey());
            }
        }
        
        saveToFile();
        plugin.getLogger().info("已清理过期的死亡记录");
    }
    
    /**
     * 清理离线玩家内存数据
     */
    public void cleanupOfflinePlayerData() {
        int cleanupMinutes = configManager.getOfflineCleanupMinutes();
        long cutoffTime = System.currentTimeMillis() - (cleanupMinutes * 60L * 1000L);
        
        Iterator<Map.Entry<UUID, Long>> teleportIterator = lastTeleportTime.entrySet().iterator();
        while (teleportIterator.hasNext()) {
            Map.Entry<UUID, Long> entry = teleportIterator.next();
            UUID uuid = entry.getKey();
            
            // 如果玩家离线且超过清理时间
            if (plugin.getServer().getPlayer(uuid) == null && entry.getValue() < cutoffTime) {
                teleportIterator.remove();
                usingBackCommand.remove(uuid);
            }
        }
    }
    
    /**
     * 重载数据
     */
    public void reload() {
        loadData();
    }
    
    /**
     * 调度异步保存任务（避免频繁写入）
     */
    private void scheduleAsyncSave() {
        // 如果已经有保存任务在队列中，跳过
        if (saveTaskScheduled) {
            return;
        }
        
        // 如果距离上次保存时间超过阈值，立即保存
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFileSaveTime > 30000) { // 30秒阈值
            performAsyncSave();
            return;
        }
        
        // 否则，延迟2秒后执行保存（防抖）
        saveTaskScheduled = true;
        plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            if (dataModified) {
                performAsyncSave();
            }
            saveTaskScheduled = false;
        }, 40L); // 2秒 = 40 ticks
    }
    
    /**
     * 执行异步保存操作
     */
    private void performAsyncSave() {
        if (!dataModified || pendingSaveQueue.isEmpty()) {
            return;
        }
        
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // 保存数据
                saveToFile();
                lastFileSaveTime = System.currentTimeMillis();
                dataModified = false;
                pendingSaveQueue.clear();
                
                plugin.getLogger().fine("死亡记录已缓冲保存到文件");
            } catch (Exception e) {
                plugin.getLogger().warning("异步保存失败: " + e.getMessage());
            }
        });
    }
    
    /**
     * 强制立即保存（用于重要操作，如插件禁用、清理等）
     */
    public void forceSaveNow() {
        if (dataModified) {
            performAsyncSave();
            // 等待保存完成
            int waitCount = 0;
            while (dataModified && waitCount < 10) {
                try {
                    Thread.sleep(100); // 等待100ms
                    waitCount++;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
    
    /**
     * 获取当前数据文件大小（字节）
     */
    public long getDataFileSize() {
        if (dataFile != null && dataFile.exists()) {
            return dataFile.length();
        }
        return 0;
    }
    
    /**
     * 获取格式化后的文件大小
     */
    public String getFormattedFileSize() {
        long bytes = getDataFileSize();
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        }
    }
    
    /**
     * 检查文件大小是否超过阈值
     */
    public boolean isFileSizeOverThreshold() {
        long currentSize = getDataFileSize();
        long threshold = getFileSizeThreshold();
        return currentSize > threshold;
    }
    
    /**
     * 获取文件大小阈值（字节）
     */
    public long getFileSizeThreshold() {
        return configManager.getFileSizeThreshold();
    }
    
    /**
     * 获取缓冲保存间隔（毫秒）
     */
    public long getBufferSaveInterval() {
        return configManager.getBufferSaveInterval();
    }
    
    /**
     * 获取强制保存阈值（毫秒）
     */
    public long getForceSaveThreshold() {
        return configManager.getForceSaveThreshold();
    }
    
    /**
     * 检查是否启用文件优化
     */
    public boolean isFileOptimizationEnabled() {
        return configManager.isFileOptimizationEnabled();
    }
    
    /**
     * 获取自动检查间隔（分钟）
     */
    public int getAutoCheckInterval() {
        return configManager.getAutoCheckInterval();
    }
    
    /**
     * 获取备份保留天数
     */
    public int getBackupRetentionDays() {
        return configManager.getBackupRetentionDays();
    }
    
    /**
     * 执行文件大小检查和优化
     */
    public void performFileSizeOptimization() {
        if (isFileSizeOverThreshold()) {
            plugin.getLogger().info("检测到数据文件过大，开始优化...");
            
            // 先尝试压缩当前文件
            if (compressDataFile()) {
                plugin.getLogger().info("数据文件压缩成功");
                return;
            }
            
            // 如果压缩失败，执行轮转
            if (rotateDataFile()) {
                plugin.getLogger().info("数据文件轮转成功");
                return;
            }
            
            plugin.getLogger().warning("文件大小优化失败");
        }
    }
    
    /**
     * 压缩数据文件（移除过期记录并重新保存）
     */
    private boolean compressDataFile() {
        try {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    // 清理过期记录
                    cleanupExpiredRecords();
                    
                    long oldSize = getDataFileSize();
                    
                    // 强制保存以重新压缩数据
                    forceSaveNow();
                    
                    long newSize = getDataFileSize();
                    long savedSpace = oldSize - newSize;
                    
                    if (savedSpace > 0) {
                        plugin.getLogger().info(String.format("文件压缩完成，节省空间: %s", 
                            formatBytes(savedSpace)));
                    } else {
                        plugin.getLogger().info("文件压缩完成，但未节省空间");
                    }
                    
                } catch (Exception e) {
                    plugin.getLogger().warning("文件压缩过程中发生错误: " + e.getMessage());
                }
            });
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("启动文件压缩失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 轮转数据文件（备份当前文件并重新开始）
     */
    private boolean rotateDataFile() {
        try {
            if (!dataFile.exists()) {
                return false;
            }
            
            // 生成备份文件名
            String timestamp = String.valueOf(System.currentTimeMillis());
            File backupFile = new File(dataFile.getParent(), "data_" + timestamp + ".yml.backup");
            
            // 复制当前文件到备份
            if (copyFile(dataFile, backupFile)) {
                // 清空当前文件并重新保存
                dataConfig = new YamlConfiguration();
                saveToFile();
                
                plugin.getLogger().info("数据文件已轮转，原文件备份为: " + backupFile.getName());
                return true;
            }
            
            return false;
        } catch (Exception e) {
            plugin.getLogger().warning("文件轮转失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 复制文件工具方法
     */
    private boolean copyFile(File source, File destination) {
        try {
            // 简单的文件复制实现
            // 在实际项目中可以使用Apache Commons IO等库
            java.nio.file.Files.copy(
                source.toPath(), 
                destination.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("文件复制失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 格式化字节数显示
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        }
    }
    
    /**
     * 获取文件统计信息
     */
    public String getFileStatistics() {
        long size = getDataFileSize();
        int recordCount = memoryStorage.size();
        String sizeStr = getFormattedFileSize();
        
        return String.format("数据文件统计: 大小=%s, 记录数=%d", sizeStr, recordCount);
    }
}