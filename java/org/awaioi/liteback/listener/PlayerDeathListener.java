package org.awaioi.liteback.listener;

import org.awaioi.liteback.LiteBack;
import org.awaioi.liteback.manager.ConfigManager;
import org.awaioi.liteback.manager.DeathStorageManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 玩家死亡事件监听器
 * 监听玩家死亡事件并记录死亡点信息
 */
public class PlayerDeathListener implements Listener {
    private final LiteBack plugin;
    private final ConfigManager configManager;
    private final DeathStorageManager storageManager;
    
    // 危险的方块类型列表
    private static final Pattern DANGEROUS_BLOCK_PATTERN = Pattern.compile(
        "LAVA|MAGMA|CACTUS|WITHER_ROSE|FIRE|SOUL_FIRE"
    );
    
    public PlayerDeathListener(LiteBack plugin, ConfigManager configManager, DeathStorageManager storageManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.storageManager = storageManager;
    }
    
    /**
     * 监听玩家死亡事件
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        try {
            Player player = event.getEntity();
            
            // 获取玩家死亡位置
            Location deathLocation = player.getLocation();
            
            // 清理死亡消息，移除颜色代码和特殊字符
            String cleanDeathMessage = cleanDeathMessage(event.getDeathMessage());
            
            // 记录死亡点
            storageManager.saveDeathRecord(player, deathLocation, cleanDeathMessage);
            
            // 发送死亡点记录成功的消息
            sendDeathRecordedMessage(player, deathLocation);
            
            plugin.getLogger().info(player.getName() + " 在 " + 
                deathLocation.getWorld().getName() + " (" + 
                String.format("%.0f", deathLocation.getX()) + ", " + 
                String.format("%.0f", deathLocation.getY()) + ", " + 
                String.format("%.0f", deathLocation.getZ()) + ") 死亡");
                
        } catch (Exception e) {
            plugin.getLogger().warning("处理玩家死亡事件时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 清理死亡消息
     * 移除颜色代码、Minecraft格式代码和过长的内容
     */
    private String cleanDeathMessage(String deathMessage) {
        if (deathMessage == null) {
            return "";
        }
        
        // 移除Minecraft格式代码 (§)
        String cleaned = deathMessage.replaceAll("§[0-9a-fk-or]", "");
        // 移除JSON格式
        cleaned = cleaned.replaceAll("[\"{}]", "");
        // 限制长度
        if (cleaned.length() > 100) {
            cleaned = cleaned.substring(0, 97) + "...";
        }
        
        return cleaned.trim();
    }
    
    /**
     * 发送死亡点记录成功消息
     */
    private void sendDeathRecordedMessage(Player player, Location location) {
        String message = configManager.getDeathRecordedMessage();
        
        // 替换消息中的变量
        message = message
            .replace("{x}", String.format("%.0f", location.getX()))
            .replace("{y}", String.format("%.0f", location.getY()))
            .replace("{z}", String.format("%.0f", location.getZ()))
            .replace("{dim}", getDimensionName(location.getWorld().getEnvironment()))
            .replace("&", "§");
        
        player.sendMessage(message);
    }
    
    /**
     * 获取维度中文名称
     */
    private String getDimensionName(org.bukkit.World.Environment environment) {
        switch (environment) {
            case NORMAL:
                return "主世界";
            case NETHER:
                return "下界";
            case THE_END:
                return "末地";
            default:
                return "未知维度";
        }
    }
    
    /**
     * 检查位置是否安全
     * 检查目标位置是否为可站立的方块且无危险方块
     */
    public boolean isLocationSafe(Location location) {
        if (!configManager.isSafeTeleportCheckEnabled()) {
            return true;
        }
        
        if (location == null || location.getWorld() == null) {
            return false;
        }
        
        int minY = configManager.getMinY();
        int maxY = configManager.getMaxY();
        
        // 检查Y坐标范围
        if (location.getY() < minY || location.getY() > maxY) {
            return false;
        }
        
        // 检查目标位置下方是否为固体方块
        Location belowLocation = location.clone().subtract(0, 1, 0);
        if (belowLocation.getBlock().getType().isAir()) {
            return false;
        }
        
        // 检查目标位置和周围是否有危险方块
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = -1; z <= 1; z++) {
                    Location checkLocation = location.clone().add(x, y, z);
                    if (isDangerousBlock(checkLocation.getBlock().getType().name())) {
                        return false;
                    }
                }
            }
        }
        
        return true;
    }
    
    /**
     * 检查方块类型是否为危险方块
     */
    private boolean isDangerousBlock(String blockType) {
        List<String> disabledBlocks = configManager.getDisabledBlocks();
        for (String disabledBlock : disabledBlocks) {
            if (blockType.equals(disabledBlock)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 寻找附近的安全位置
     * 如果当前位置不安全，在附近寻找安全的传送位置
     */
    public Location findSafeLocation(Location originalLocation) {
        if (isLocationSafe(originalLocation)) {
            return originalLocation;
        }
        
        // 在周围10格范围内寻找安全位置
        for (int radius = 1; radius <= 10; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    for (int y = -2; y <= 2; y++) {
                        Location testLocation = originalLocation.clone().add(x, y, z);
                        if (isLocationSafe(testLocation)) {
                            return testLocation;
                        }
                    }
                }
            }
        }
        
        // 如果找不到安全位置，返回原位置
        return originalLocation;
    }
}