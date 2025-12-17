package org.awaioi.liteback.command;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.awaioi.liteback.LiteBack;
import org.awaioi.liteback.listener.PlayerDeathListener;
import org.awaioi.liteback.manager.ConfigManager;
import org.awaioi.liteback.manager.DeathStorageManager;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * /back命令处理器
 * 处理玩家死亡回溯传送功能
 */
public class BackCommand implements TabExecutor {
    private final LiteBack plugin;
    private final ConfigManager configManager;
    private final DeathStorageManager storageManager;
    private final PlayerDeathListener deathListener;
    
    public BackCommand(LiteBack plugin, ConfigManager configManager, 
                      DeathStorageManager storageManager, PlayerDeathListener deathListener) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.storageManager = storageManager;
        this.deathListener = deathListener;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            String onlyPlayerMessage = configManager.getOnlyPlayerMessage().replace("&", "§");
            sender.sendMessage(onlyPlayerMessage);
            return true;
        }
        
        Player player = (Player) sender;
        
        // 处理参数
        if (args.length > 0) {
            return handleArguments(player, args);
        }
        
        // 执行普通/back命令
        return executeBackCommand(player);
    }
    
    /**
     * 处理命令参数
     */
    private boolean handleArguments(Player player, String[] args) {
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "reload":
                return handleReloadCommand(player);
            case "help":
                return handleHelpCommand(player);
            case "info":
                return handleInfoCommand(player);
            case "clean":
                return handleCleanCommand(player);
            default:
                String noPermissionMessage = configManager.getNoPermissionMessage().replace("&", "§");
                player.sendMessage(noPermissionMessage);
                return true;
        }
    }
    
    /**
     * 处理reload子命令
     */
    private boolean handleReloadCommand(Player player) {
        if (!player.hasPermission("liteback.admin")) {
            String noPermissionMessage = configManager.getNoPermissionMessage().replace("&", "§");
            player.sendMessage(noPermissionMessage);
            return true;
        }
        
        try {
            configManager.reloadConfig();
            storageManager.reload();
            String successMessage = configManager.getReloadSuccessMessage().replace("&", "§");
            player.sendMessage(successMessage);
            plugin.getLogger().info(player.getName() + " 重载了插件配置");
        } catch (Exception e) {
            String errorMessage = configManager.getReloadFailedMessage()
                .replace("{error}", e.getMessage())
                .replace("&", "§");
            player.sendMessage(errorMessage);
            plugin.getLogger().warning("重载配置失败: " + e.getMessage());
        }
        
        return true;
    }
    
    /**
     * 处理help子命令
     */
    private boolean handleHelpCommand(Player player) {
        String helpMessage = configManager.getHelpMessage()
            .replace("&", "§");
        player.sendMessage(helpMessage);
        return true;
    }
    
    /**
     * 处理info子命令
     */
    private boolean handleInfoCommand(Player player) {
        UUID uuid = player.getUniqueId();
        
        if (!storageManager.hasDeathRecord(uuid)) {
            String noDeathRecordMessage = configManager.getNoDeathRecordMessage().replace("&", "§");
            player.sendMessage(noDeathRecordMessage);
            return true;
        }
        
        DeathStorageManager.DeathRecord record = storageManager.getDeathRecord(uuid);
        if (record != null) {
            String infoMessage = "§7=== §a你的死亡记录 §7===\n" +
                "§7位置: §e" + record.getFormattedLocation() + "\n" +
                "§7维度: §e" + record.getDimension() + "\n" +
                "§7时间: §e" + formatTimestamp(record.getTimestamp()) + "\n" +
                "§7死亡原因: §e" + (record.getDeathMessage().isEmpty() ? "未知" : record.getDeathMessage());
            player.sendMessage(infoMessage);
        }
        
        return true;
    }
    
    /**
     * 处理clean子命令（管理员清理）
     */
    private boolean handleCleanCommand(Player player) {
        if (!player.hasPermission("liteback.admin.cleanup")) {
            String noPermissionMessage = configManager.getNoPermissionMessage().replace("&", "§");
            player.sendMessage(noPermissionMessage);
            return true;
        }
        
        // 执行清理
        storageManager.cleanupExpiredRecords();
        storageManager.cleanupOfflinePlayerData();
        
        String successMessage = configManager.getCleanupSuccessMessage().replace("&", "§");
        player.sendMessage(successMessage);
        plugin.getLogger().info(player.getName() + " 执行了数据清理");
        
        return true;
    }
    
    /**
     * 执行主要的/back命令
     */
    private boolean executeBackCommand(Player player) {
        UUID uuid = player.getUniqueId();
        
        // 检查权限
        if (!player.hasPermission("liteback.back")) {
            String noPermissionMessage = configManager.getNoPermissionMessage().replace("&", "§");
            player.sendMessage(noPermissionMessage);
            return true;
        }
        
        // 检查是否正在使用命令
        if (storageManager.isUsingBackCommand(uuid)) {
            String alreadyUsingMessage = configManager.getAlreadyUsingMessage().replace("&", "§");
            player.sendMessage(alreadyUsingMessage);
            return true;
        }
        
        // 检查是否有死亡记录
        if (!storageManager.hasDeathRecord(uuid)) {
            String noDeathRecordMessage = configManager.getNoDeathRecordMessage().replace("&", "§");
            player.sendMessage(noDeathRecordMessage);
            return true;
        }
        
        // 检查冷却时间
        if (storageManager.isOnCooldown(uuid)) {
            int remainingTime = storageManager.getRemainingCooldown(uuid);
            String cooldownMessage = configManager.getCooldownMessage()
                .replace("{time}", String.valueOf(remainingTime))
                .replace("&", "§");
            player.sendMessage(cooldownMessage);
            return true;
        }
        
        // 获取死亡记录
        DeathStorageManager.DeathRecord record = storageManager.getDeathRecord(uuid);
        if (record == null) {
            String noDeathRecordMessage = configManager.getNoDeathRecordMessage().replace("&", "§");
            player.sendMessage(noDeathRecordMessage);
            return true;
        }
        
        // 检查目标世界是否存在
        World targetWorld = plugin.getServer().getWorld(record.getLocation().getWorld().getName());
        if (targetWorld == null) {
            String worldNotExistsMessage = configManager.getWorldNotExistsMessage().replace("&", "§");
            player.sendMessage(worldNotExistsMessage);
            return true;
        }
        
        // 执行传送
        performTeleport(player, record);
        
        return true;
    }
    
    /**
     * 执行传送操作
     */
    private void performTeleport(Player player, DeathStorageManager.DeathRecord record) {
        UUID uuid = player.getUniqueId();
        
        // 标记正在使用命令
        storageManager.setUsingBackCommand(uuid, true);
        
        // 发送传送开始消息
        String successMessage = configManager.getTeleportSuccessMessage().replace("&", "§");
        player.sendMessage(successMessage);
        
        // 播放传送音效和粒子效果
        playTeleportEffects(player);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Location originalLocation = record.getLocation();
                    Location safeLocation = deathListener.findSafeLocation(originalLocation);
                    
                    // 检查安全位置
                    if (!deathListener.isLocationSafe(safeLocation)) {
                        String unsafeTargetMessage = configManager.getUnsafeTargetMessage().replace("&", "§");
                        player.sendMessage(unsafeTargetMessage);
                        storageManager.setUsingBackCommand(uuid, false);
                        cancel();
                        return;
                    }
                    
                    // 执行传送
                    player.teleport(safeLocation);
                    
                    // 设置冷却时间
                    storageManager.setLastTeleportTime(uuid);
                    
                    // 传送完成消息
                    String teleportCompleteMessage = configManager.getTeleportCompleteMessage().replace("&", "§");
                    player.sendMessage(teleportCompleteMessage);
                    
                    // 记录传送日志
                    plugin.getLogger().info(player.getName() + " 使用/back命令从 " + 
                        formatLocation(player.getLocation()) + " 传送到 " + 
                        formatLocation(safeLocation));
                    
                } catch (Exception e) {
                     String teleportFailedMessage = configManager.getTeleportFailedMessage()
                         .replace("{error}", e.getMessage())
                         .replace("&", "§");
                     player.sendMessage(teleportFailedMessage);
                     plugin.getLogger().warning("玩家 " + player.getName() + " 传送失败: " + e.getMessage());
                } finally {
                    storageManager.setUsingBackCommand(uuid, false);
                    cancel();
                }
            }
        }.runTask(plugin);
    }
    
    /**
     * 播放传送音效和粒子效果
     */
    private void playTeleportEffects(Player player) {
        Location location = player.getLocation();
        
        // 传送粒子效果
        player.getWorld().spawnParticle(Particle.PORTAL, location, 50, 1, 1, 1, 1);
        player.getWorld().spawnParticle(Particle.ENCHANT, location, 20, 0.5, 0.5, 0.5, 1);
        
        // 传送音效
        player.playSound(location, Sound.ITEM_CHORUS_FRUIT_TELEPORT, 1.0f, 1.0f);
        player.playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.0f);
    }
    
    /**
     * 格式化时间戳
     */
    private String formatTimestamp(long timestamp) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            .format(new java.util.Date(timestamp));
    }
    
    /**
     * 格式化位置信息
     */
    private String formatLocation(Location location) {
        return String.format("%s (%.0f, %.0f, %.0f)", 
            location.getWorld().getName(),
            location.getX(), location.getY(), location.getZ());
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return Arrays.asList();
        }
        
        Player player = (Player) sender;
        
        if (args.length == 1) {
            if (player.hasPermission("liteback.admin")) {
                return Arrays.asList("reload", "help", "info", "clean");
            } else {
                return Arrays.asList("help", "info");
            }
        }
        
        return Arrays.asList();
    }
}