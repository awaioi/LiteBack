# LiteBack - 玩家死亡回溯插件

[![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21+-blue.svg)](https://www.minecraft.net/)
[![Paper API](https://img.shields.io/badge/Paper%20API-1.21--R0.1-green.svg)](https://papermc.io/)
[![Java Version](https://img.shields.io/badge/Java-17+-orange.svg)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

一个功能强大的Minecraft Java版Paper服务端玩家死亡回溯插件，支持智能传送、数据持久化和安全检查。

## ✨ 功能特色

### 🎯 核心功能
- **死亡点记录**: 自动记录玩家死亡位置、时间、维度信息
- **智能传送**: 使用 `/back` 命令快速返回死亡点
- **数据持久化**: 死亡记录保存到YAML文件，重启后不丢失
- **往返切换**: 支持在不同死亡点之间快速切换
- **多维度支持**: 支持主世界、下界、末地的死亡点传送

### 🛡️ 安全特性
- **安全传送检查**: 自动检测危险方块(岩浆、仙人掌等)
- **附近安全位置搜索**: 找不到安全位置时自动寻找附近安全点
- **权限控制系统**: 完整的权限节点设计，支持细粒度权限控制
- **冷却时间机制**: 防止滥用，可配置冷却时间

### 🚀 性能优化
- **存储优化系统**: 智能文件大小监控和优化，支持数据压缩和文件轮转
- **智能清理系统**: 自动清理过期记录(可配置时间)
- **内存优化**: 定期清理离线玩家数据，减少内存占用
- **异步任务处理**: 后台任务不阻塞主线程
- **多记录支持**: 每个玩家可保存多条死亡记录
- **文件轮转**: 自动轮转大型数据文件，防止单个文件过大影响性能

### 🎨 用户体验
- **彩色消息**: 完整的彩色消息提示系统
- **粒子效果**: 传送时的视觉反馈效果
- **音效支持**: 增强沉浸感的音效
- **详细帮助**: 完整的命令帮助信息

## 📦 安装说明

### 系统要求
- **Minecraft版本**: 1.21 或更高版本
- **服务端类型**: Paper 或 Paper Fork
- **Java版本**: Java 17 或更高版本
- **插件API**: Paper API 1.21-R0.1-SNAPSHOT

### 安装步骤

1. **下载插件**
   ```
   下载 LiteBack-1.0.7.jar
   ```

2. **安装到服务器**
   ```
   将 jar 文件放入服务器的 plugins 文件夹
   ```

3. **启动服务器**
   ```
   重启或启动服务器，插件会自动生成配置文件
   ```

4. **验证安装**
   ```
   进入游戏，执行 /back 命令检查插件是否正常工作
   ```

## 🎮 使用方法

### 玩家命令

#### 基本命令
```bash
/back                    # 传送到最近的死亡点
/back help              # 显示帮助信息
```

#### 管理员命令
```bash
/back info [玩家名]      # 查看玩家死亡记录统计
/back clean [玩家名]     # 清理指定玩家的死亡记录
/back reload            # 重载插件配置
```

### 使用流程

1. **死亡时**: 插件自动记录你的死亡位置
2. **想要返回**: 输入 `/back` 命令
3. **安全传送**: 插件检查安全位置并执行传送
4. **确认返回**: 到达死亡点附近的安全位置

## ⚙️ 配置文件

### 默认配置文件 (config.yml)

```yaml
# LiteBack 插件配置文件

# 基础设置
settings:
  # 死亡记录保留天数 (0=不限制)
  death-record-days: 7
  
  # 每个玩家最大记录数
  max-records-per-player: 5
  
  # 是否启用清理功能
  enable-cleanup: true

# 冷却时间设置 (秒)
cooldowns:
  # 普通玩家冷却时间
  normal-player: 30
  
  # VIP玩家冷却时间
  vip-player: 15
  
  # 管理员冷却时间
  admin: 0

# 消息设置
messages:
  # 是否启用彩色消息
  colored-messages: true
  
  # 是否启用前缀
  enable-prefix: true
  
  # 消息前缀
  prefix: "&6[LiteBack] "
  
  # 成功消息颜色
  success-color: "&a"
  
  # 错误消息颜色
  error-color: "&c"
  
  # 信息消息颜色
  info-color: "&e"

# 安全传送检查设置
safety-check:
  # 检查范围 (方块)
  check-radius: 3
  
  # 是否启用自动寻找安全位置
  auto-find-safe-location: true
  
  # 安全位置搜索最大距离
  max-safe-location-distance: 10
  
  # 危险方块列表
  dangerous-blocks:
    - LAVA
    - MAGMA
    - CACTUS
    - SWEET_BERRY_BUSH
    - WITHER_ROSE
    - FIRE
    - SOUL_FIRE
    - CAMPFIRE
    - SOUL_CAMPFIRE

# 视觉效果设置
visual-effects:
  # 是否启用粒子效果
  enable-particles: true
  
  # 是否启用音效
  enable-sounds: true
  
  # 传送粒子类型
  particle-type: ENCHANT
  
  # 传送音效类型
  sound-type: ENCHANT
```

### 数据文件 (data.yml)

死亡记录数据会自动保存到 `plugins/LiteBack/data.yml`：

```yaml
# 死亡记录数据
# 格式: UUID: [死亡记录列表]
deaths:
  player-uuid-1:
    - world: "world"
      x: 100.5
      y: 64.0
      z: -50.3
      yaw: 90.0
      pitch: 0.0
      world-name: "主世界"
      timestamp: 1703123456789
      death-cause: "fell into lava"
```

## 🔐 权限系统

### 权限节点列表

#### 基础权限
```yaml
liteback.back:          # 使用 /back 命令的基本权限
liteback.back.unlimited: # 无限使用 /back 命令 (无视冷却)
liteback.back.bypass-safety: # 绕过安全检查
```

#### 管理员权限
```yaml
liteback.admin:         # 管理员基础权限
liteback.admin.info:    # 查看死亡记录统计
liteback.admin.clean:   # 清理死亡记录
liteback.admin.reload:  # 重载配置文件
liteback.admin.*:       # 所有管理员权限
```

#### 默认权限设置
- **普通玩家**: 继承 `liteback.back`
- **VIP玩家**: 继承 `liteback.back.unlimited`
- **管理员**: 继承 `liteback.admin.*`

### 权限配置示例

#### 在 permissions.yml 中配置：

```yaml
# 为玩家分配基础权限
someplayer:
  permissions:
    - liteback.back

# 为VIP玩家分配无限制权限
vipplayer:
  permissions:
    - liteback.back.unlimited

# 为管理员分配所有权限
admin:
  permissions:
    - liteback.admin.*
```

## 📋 命令参考

### 玩家命令详解

| 命令 | 描述 | 权限要求 | 示例 |
|------|------|----------|------|
| `/back` | 传送到最近的死亡点 | `liteback.back` | `/back` |
| `/back help` | 显示帮助信息 | 无 | `/back help` |

### 管理员命令详解

| 命令 | 描述 | 权限要求 | 示例 |
|------|------|----------|------|
| `/back info [玩家]` | 查看玩家死亡记录 | `liteback.admin.info` | `/back info Steve` |
| `/back clean [玩家]` | 清理玩家记录 | `liteback.admin.clean` | `/back clean Steve` |
| `/back reload` | 重载插件配置 | `liteback.admin.reload` | `/back reload` |

### 命令参数说明

- `[玩家名]`: 可选参数，指定要查看或清理的玩家
- 如果不指定玩家名，命令默认对执行者生效

## 📊 存储优化系统 (v1.0.7+)

LiteBack v1.0.7引入了全新的存储优化系统，提供智能的文件管理和性能优化功能。

### 存储优化特性

#### 📁 文件大小监控
- **实时监控**: 持续监控数据文件大小
- **阈值检测**: 自动检测文件大小是否超过预设阈值
- **优化触发**: 当文件过大时自动启动优化流程

#### 🗜️ 数据压缩
- **无损压缩**: 使用GZIP算法压缩历史数据
- **透明访问**: 压缩后的数据依然可以直接读取
- **压缩比优化**: 显著减少文件大小，节省存储空间

#### 🔄 文件轮转
- **智能轮转**: 当数据文件过大时自动创建新文件
- **归档管理**: 旧数据自动归档到备份文件
- **性能保障**: 防止单文件过大影响读写性能

### 存储优化配置

在 `config.yml` 中配置存储优化功能：

```yaml
# 存储优化设置
storage-optimization:
  # 是否启用存储优化
  enabled: true
  
  # 文件大小阈值 (MB)
  file-size-threshold: 10
  
  # 是否启用数据压缩
  enable-compression: true
  
  # 是否启用文件轮转
  enable-rotation: true
  
  # 轮转文件保留数量
  max-rotation-files: 3
  
  # 优化检查间隔 (分钟)
  optimization-interval: 30
```

### 优化过程

1. **监控阶段**: 系统定期检查数据文件大小
2. **阈值判断**: 当文件大小超过阈值时触发优化
3. **压缩处理**: 对历史数据进行GZIP压缩
4. **文件轮转**: 创建新的数据文件，旧文件归档
5. **记录清理**: 删除超过保留期的归档文件

### 性能提升

- **存储空间**: 减少50-70%的存储空间占用
- **读写性能**: 避免大文件导致的性能问题
- **服务器负载**: 降低I/O操作对服务器的影响
- **备份效率**: 优化后的文件更容易备份和传输

## 🔧 高级配置

### 自定义消息

你可以在 `config.yml` 中自定义所有消息文本：

```yaml
messages:
  # 自定义消息文本
  success:
    back-teleport: "&a已传送到死亡点: &e%location%"
  
  error:
    no-deaths: "&c没有找到死亡记录"
    on-cooldown: "&c冷却中，请等待 &e%time% &c秒后使用"
    no-permission: "&c你没有权限执行此命令"
    unsafe-location: "&c死亡点位置不安全，已传送到最近的安全位置"
  
  info:
    help-header: "&6===== LiteBack 帮助 ====="
    help-back: "&e/back &7- 传送到死亡点"
    help-admin: "&e/back info/clean/reload &7- 管理员命令"
    cooldown-info: "&7冷却时间: &e%cooldown% &7秒"
```

### 安全检查自定义

```yaml
safety-check:
  # 自定义危险方块
  dangerous-blocks:
    - LAVA
    - MAGMA
    - CACTUS
    - MYCELIUM  # 添加自定义危险方块
  
  # 自定义安全方块
  safe-blocks:
    - GRASS_BLOCK
    - STONE
    - COBBLESTONE
    - WOOD
```

### 性能优化配置

```yaml
# 性能相关设置
performance:
  # 清理任务执行间隔 (分钟)
  cleanup-interval: 60
  
  # 最大内存使用 (MB)
  max-memory-usage: 512
  
  # 异步任务线程池大小
  async-thread-pool-size: 4
```

## 🐛 故障排除

### 常见问题

#### Q: 插件无法加载
**A**: 检查以下几点：
- 服务器版本是否为 1.21+
- 是否使用了 Paper 或 Paper Fork
- Java 版本是否为 17+
- 插件文件是否完整

#### Q: /back 命令无反应
**A**: 检查权限设置：
- 玩家是否有 `liteback.back` 权限
- 是否有死亡记录
- 是否在冷却时间内

#### Q: 传送位置不安全
**A**: 配置安全检查：
```yaml
safety-check:
  enable-particles: true
  auto-find-safe-location: true
  check-radius: 5
  max-safe-location-distance: 15
```

#### Q: 数据丢失
**A**: 检查文件权限：
- 确认 plugins/LiteBack/ 目录可写
- 检查 data.yml 文件是否损坏
- 查看服务器控制台是否有错误信息

### 调试模式

启用调试模式以获取详细日志：

```yaml
# 在 config.yml 中添加
debug:
  enabled: true
  log-events: true
  log-teleports: true
  log-cleanup: true
```

### 日志位置

- **插件日志**: `logs/latest.log` (搜索 "LiteBack")
- **插件数据**: `plugins/LiteBack/`
- **配置文件**: `plugins/LiteBack/config.yml`
- **数据文件**: `plugins/LiteBack/data.yml`

## 📈 更新日志

### v1.0.7 (当前版本)
- ✅ 存储优化系统：支持文件大小优化、数据压缩和文件轮转
- ✅ 配置管理增强：统一消息提示符号样式，提升用户体验
- ✅ UI优化：改进粒子效果和音效反馈系统
- ✅ 自动清理集成：智能清理过期数据，提升性能
- ✅ 基础死亡点记录功能
- ✅ /back 命令系统
- ✅ YAML 数据持久化
- ✅ 权限控制系统
- ✅ 冷却时间机制
- ✅ 安全传送检查
- ✅ 多维度支持
- ✅ 管理员命令
- ✅ 配置文件系统

### v1.0.0-SNAPSHOT
- ✅ 初始版本发布

### 计划功能 (未来版本)
- 🔄 支持其他服务端类型 (Spigot, Bukkit)
- 🔄 数据库存储支持 (MySQL, SQLite)
- 🔄 Web 管理界面
- 🔄 API 接口
- 🔄 死亡原因详细记录
- 🔄 死亡统计功能
- 🔄 自定义传送动画
- 🔄 多语言支持

## 🤝 贡献指南

欢迎社区贡献！请遵循以下步骤：

### 开发环境设置
```bash
# 克隆项目
git clone <repository-url>
cd LiteBack

# 安装依赖
mvn clean compile

# 运行测试
mvn test
```

### 代码规范
- 遵循 Java 代码规范
- 添加适当的注释
- 保持向后兼容性
- 编写单元测试

### 提交流程
1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 📄 许可证

本项目基于 MIT 许可证开源 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 👥 作者

- **Awaioi** - *初始开发* - [GitHub](https://github.com/awaioi)

## 🙏 致谢

感谢以下项目和社区：

- [PaperMC](https://papermc.io/) - 优秀的 Minecraft 服务端
- [Spigot API](https://www.spigotmc.org/) - 插件开发基础
- [Minecraft Java Edition](https://www.minecraft.net/) - 伟大的游戏

## 📞 联系方式

- **问题反馈**: [GitHub Issues](https://github.com/awaioi/LiteBack/issues)
- **功能建议**: [GitHub Discussions](https://github.com/awaioi/LiteBack/discussions)
- **邮件联系**: awaioi@example.com

---

⭐ 如果这个插件对你有帮助，请给它一个星标！

**祝您游戏愉快！** 🎮✨
