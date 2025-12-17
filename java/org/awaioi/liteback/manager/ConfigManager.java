package org.awaioi.liteback.manager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.awaioi.liteback.LiteBack;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * 配置管理类
 * 负责管理插件的配置文件加载、保存和重载
 */
public class ConfigManager {
    private final LiteBack plugin;
    private FileConfiguration config;
    private File configFile;
    
    public ConfigManager(LiteBack plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    /**
     * 加载配置文件
     */
    public void loadConfig() {
        if (configFile == null) {
            configFile = new File(plugin.getDataFolder(), "config.yml");
        }
        
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // 检查是否有默认配置
        InputStream defaultConfigStream = plugin.getResource("config.yml");
        if (defaultConfigStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultConfigStream)
            );
            config.setDefaults(defaultConfig);
        }
    }
    
    /**
     * 保存配置文件
     */
    public void saveConfig() {
        if (config == null || configFile == null) {
            return;
        }
        
        try {
            config.save(configFile);
        } catch (IOException ex) {
            plugin.getLogger().warning("无法保存配置文件: " + ex.getMessage());
        }
    }
    
    /**
     * 重载配置文件
     */
    public void reloadConfig() {
        if (configFile == null) {
            configFile = new File(plugin.getDataFolder(), "config.yml");
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        
        InputStream defaultConfigStream = plugin.getResource("config.yml");
        if (defaultConfigStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultConfigStream)
            );
            config.setDefaults(defaultConfig);
        }
    }
    
    /**
     * 获取配置文件
     */
    public FileConfiguration getConfig() {
        if (config == null) {
            loadConfig();
        }
        return config;
    }
    
    // === 基础设置方法 ===
    
    public int getDeathRecordDays() {
        return getConfig().getInt("settings.death-record-days", 7);
    }
    
    public int getMaxDeathRecords() {
        return getConfig().getInt("settings.max-death-records", 5);
    }
    
    public int getOfflineCleanupMinutes() {
        return getConfig().getInt("settings.offline-cleanup-minutes", 60);
    }
    
    public boolean isSafeTeleportCheckEnabled() {
        return getConfig().getBoolean("settings.safe-teleport-check", true);
    }
    
    // === 冷却时间设置方法 ===
    
    public boolean isCooldownEnabled() {
        return getConfig().getBoolean("cooldown.enabled", true);
    }
    
    public int getCooldownDuration() {
        return getConfig().getInt("cooldown.duration", 30);
    }
    
    public boolean isAdminUnlimited() {
        return getConfig().getBoolean("cooldown.admin-unlimited", true);
    }
    
    // === 消息设置方法 ===
    
    public String getDeathRecordedMessage() {
        return getConfig().getString("messages.death-recorded", 
            "&a✅ 死亡点已记录成功 &7| &e坐标: &f{x}, {y}, {z} &7| &b{dim}&a 维度");
    }
    
    public String getTeleportSuccessMessage() {
        return getConfig().getString("messages.teleport-success", 
            "&a✅ 传送成功 &7| &e正在返回死亡点...");
    }
    
    public String getNoDeathRecordMessage() {
        return getConfig().getString("messages.no-death-record", 
            "&c❌ 没有找到死亡记录 &7| &7尝试先死亡一次吧~");
    }
    
    public String getCooldownMessage() {
        return getConfig().getString("messages.cooldown-message", 
            "&c⏰ 冷却中 &7| &e剩余时间: &f{time} &7秒");
    }
    
    public String getUnsafeTargetMessage() {
        return getConfig().getString("messages.unsafe-target", 
            "&c⚠️ 目标位置不安全 &7| &7请联系管理员检查地形");
    }
    
    public String getNoPermissionMessage() {
        return getConfig().getString("messages.no-permission", 
            "&c🚫 权限不足 &7| &7你需要 &e'liteback.back' &7权限");
    }
    
    public String getAlreadyUsingMessage() {
        return getConfig().getString("messages.already-using", 
            "&c🔄 正在使用中 &7| &7请等待当前操作完成");
    }
    
    public String getHelpMessage() {
        return getConfig().getString("messages.help-message", 
            "&8╔═══════════════════════════════╗\n" +
            "&8║ &a⚡ &eLiteBack 死亡回溯插件 &a⚡ &8    ║\n" +
            "&8╠═══════════════════════════════╣\n" +
            "&8║ &7命令: &e/back &7- 返回你的死亡点    ║\n" +
            "&8║ &7管理: &e/back reload &7- 重载配置    ║\n" +
            "&8╚═══════════════════════════════╝");
    }
    
    public String getReloadSuccessMessage() {
        return getConfig().getString("messages.reload-success", 
            "&a✅ 配置文件已重载");
    }
    
    public String getReloadFailedMessage() {
        return getConfig().getString("messages.reload-failed", 
            "&c❌ 重载配置失败: {error}");
    }
    
    public String getCleanupSuccessMessage() {
        return getConfig().getString("messages.cleanup-success", 
            "&a✅ 清理完成 &7| &7已清理过期的死亡记录和离线玩家数据");
    }
    
    public String getWorldNotExistsMessage() {
        return getConfig().getString("messages.world-not-exists", 
            "&c❌ 目标世界不存在 &7| &7无法传送");
    }
    
    public String getTeleportCompleteMessage() {
        return getConfig().getString("messages.teleport-complete", 
            "&a✅ 传送成功");
    }
    
    public String getTeleportFailedMessage() {
        return getConfig().getString("messages.teleport-failed", 
            "&c❌ 传送失败: {error}");
    }
    
    public String getDeathInfoHeader() {
        return getConfig().getString("messages.death-info-header", 
            "&7=== &a你的死亡记录 &7===");
    }
    
    public String getDeathInfoLocation() {
        return getConfig().getString("messages.death-info-location", 
            "&7位置: &e{location}");
    }
    
    public String getDeathInfoDimension() {
        return getConfig().getString("messages.death-info-dimension", 
            "&7维度: &e{dim}");
    }
    
    public String getDeathInfoTime() {
        return getConfig().getString("messages.death-info-time", 
            "&7时间: &e{time}");
    }
    
    public String getDeathInfoCause() {
        return getConfig().getString("messages.death-info-cause", 
            "&7死亡原因: &e{cause}");
    }
    
    // === 安全传送检查设置方法 ===
    
    public List<String> getDisabledBlocks() {
        return getConfig().getStringList("safety-check.disabled-blocks");
    }
    
    public int getMinY() {
        return getConfig().getInt("safety-check.min-y", -64);
    }
    
    public int getMaxY() {
        return getConfig().getInt("safety-check.max-y", 320);
    }
    
    // === 存储优化设置方法 ===
    
    /**
     * 获取文件大小阈值（字节）
     */
    public long getFileSizeThreshold() {
        return getConfig().getLong("settings.file-size-threshold", 5242880L); // 默认5MB
    }
    
    /**
     * 获取缓冲保存间隔（毫秒）
     */
    public long getBufferSaveInterval() {
        return getConfig().getLong("settings.buffer-save-interval", 2000L); // 默认2秒
    }
    
    /**
     * 获取强制保存阈值（毫秒）
     */
    public long getForceSaveThreshold() {
        return getConfig().getLong("settings.force-save-threshold", 30000L); // 默认30秒
    }
    
    /**
     * 是否启用文件大小优化
     */
    public boolean isFileOptimizationEnabled() {
        return getConfig().getBoolean("settings.enable-file-optimization", true);
    }
    
    /**
     * 获取自动检查间隔（分钟）
     */
    public int getAutoCheckInterval() {
        return getConfig().getInt("settings.auto-check-interval", 10); // 默认10分钟
    }
    
    /**
     * 获取备份文件保留天数
     */
    public int getBackupRetentionDays() {
        return getConfig().getInt("settings.backup-retention-days", 3); // 默认3天
    }
    
    /**
     * 获取只有玩家才能使用命令的消息
     */
    public String getOnlyPlayerMessage() {
        return config.getString("messages.only-player", "&c✗ 只有玩家才能使用此命令！");
    }
}