package org.awaioi.liteback;

import org.awaioi.liteback.command.BackCommand;
import org.awaioi.liteback.listener.PlayerDeathListener;
import org.awaioi.liteback.manager.ConfigManager;
import org.awaioi.liteback.manager.DeathStorageManager;
import org.awaioi.liteback.task.CleanupTask;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;

/**
 * LiteBack - 玩家死亡回溯插件
 * 允许玩家返回到死亡点的Paper插件
 */
public final class LiteBack extends JavaPlugin {
    
    private ConfigManager configManager;
    private DeathStorageManager storageManager;
    private PlayerDeathListener deathListener;
    private BackCommand backCommand;
    private CleanupTask cleanupTask;
    
    @Override
    public void onEnable() {
        // 插件启动逻辑
        getLogger().info("LiteBack 插件正在启动...");
        
        // 创建数据文件夹
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        
        try {
            // 初始化配置管理器
            configManager = new ConfigManager(this);
            getLogger().info("配置管理器已初始化");
            
            // 初始化存储管理器
            storageManager = new DeathStorageManager(this, configManager);
            getLogger().info("存储管理器已初始化");
            
            // 初始化事件监听器
            deathListener = new PlayerDeathListener(this, configManager, storageManager);
            getServer().getPluginManager().registerEvents(deathListener, this);
            getLogger().info("事件监听器已注册");
            
            // 初始化命令处理器
            backCommand = new BackCommand(this, configManager, storageManager, deathListener);
            getCommand("back").setExecutor(backCommand);
            getCommand("back").setTabCompleter(backCommand);
            getLogger().info("/back 命令已注册");
            
            // 启动清理任务
            startCleanupTask();
            getLogger().info("清理任务已启动");
            
            // 显示启动完成消息
            getLogger().info("LiteBack 插件启动完成！版本: " + getDescription().getVersion());
            
            // 显示功能概览
            showStartupInfo();
            
        } catch (Exception e) {
            getLogger().severe("插件启动失败: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        // 插件关闭逻辑
        getLogger().info("LiteBack 插件正在关闭...");
        
        try {
            // 停止清理任务
            if (cleanupTask != null) {
                cleanupTask.cancel();
                getLogger().info("清理任务已停止");
            }
            
            // 保存所有数据
            if (storageManager != null) {
                storageManager.cleanupExpiredRecords();
                getLogger().info("死亡记录已保存");
            }
            
            getLogger().info("LiteBack 插件已安全关闭");
            
        } catch (Exception e) {
            getLogger().warning("插件关闭时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 启动定期清理任务
     */
    private void startCleanupTask() {
        cleanupTask = new CleanupTask(this, configManager, storageManager);
        
        // 每5分钟执行一次清理任务
        cleanupTask.runTaskTimerAsynchronously(this, 6000L, 6000L);
    }
    
    /**
     * 显示插件启动信息
     */
    private void showStartupInfo() {
        String[] info = {
            "============================================",
            "        LiteBack - 死亡回溯插件",
            "============================================",
            "版本: " + getDescription().getVersion(),
            "作者: " + String.join(", ", getDescription().getAuthors()),
            "描述: " + getDescription().getDescription(),
            "",
            "功能特性:",
            "• 记录玩家死亡点坐标和时间",
            "• /back 命令返回死亡点",
            "• 安全传送检查和危险方块检测",
            "• 冷却时间系统防止滥用",
            "• 自动清理过期记录",
            "• 完整的权限系统",
            "• 配置文件支持自定义",
            "",
            "配置文件: config.yml",
            "数据文件: data.yml",
            "",
            "管理员命令:",
            "• /back reload - 重载配置",
            "• /back clean - 清理数据",
            "• /back help - 显示帮助",
            "• /back info - 查看死亡记录",
            "",
            "权限节点:",
            "• liteback.back - 使用/back命令",
            "• liteback.admin - 管理员权限",
            "• liteback.back.unlimited - 无冷却限制",
            "============================================"
        };
        
        Arrays.stream(info).forEach(getLogger()::info);
    }
    
    /**
     * 获取配置管理器
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    /**
     * 获取存储管理器
     */
    public DeathStorageManager getStorageManager() {
        return storageManager;
    }
    
    /**
     * 获取死亡监听器
     */
    public PlayerDeathListener getDeathListener() {
        return deathListener;
    }
    
    /**
     * 重新加载插件
     */
    public void reloadPlugin() {
        try {
            // 重载配置
            configManager.reloadConfig();
            
            // 重载存储数据
            storageManager.reload();
            
            getLogger().info("插件配置已重载");
            
        } catch (Exception e) {
            getLogger().severe("重载插件失败: " + e.getMessage());
            throw new RuntimeException("重载插件失败", e);
        }
    }
}