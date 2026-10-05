# CommandBlockUIOverhaul - 命令方块界面重构

<p align="center">
  <strong>为Minecraft命令方块带来现代化多行编辑体验</strong><br>
  <span>告别单行文本框，拥抱智能多行命令编辑</span>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.20.1-blue?style=flat-square&logo=minecraft" alt="Minecraft 1.20.1">
  <img src="https://img.shields.io/badge/Loader-Forge-orange?style=flat-square&logo=curseforge" alt="Forge">
  <img src="https://img.shields.io/badge/License-MIT-green?style=flat-square" alt="MIT License">
</p>

## 📖 模组简介

**命令方块界面重构（CommandBlockUIOverhaul）** 是一款革命性的Minecraft Forge模组，旨在彻底改变原版命令方块的编辑体验。告别在狭窄的单行文本框中左右横移的痛苦，享受宽敞的多行编辑界面带来的便利！

### 🎯 设计理念
- **简洁直观** - 保持原版风格，但功能更强大
- **智能高效** - 自动换行，提升编辑效率

## ✨ 核心特性

### ✅ 已实现功能
| 功能         | 描述                 |
|------------|--------------------|
| **智能换行**   | 自动检测括号换行，保持代码结构清晰  |
| **宽敞编辑区域** | 大幅扩展的命令编辑区域，告别视觉拥挤 |
| **文本渲染**   | 多行文本渲染，提供原版命令的颜色渲染 |
| **原版兼容性**  | 所有原版命令方块功能保持完整     |
| **智能命令联想** | 输入时提供命令建议和补全       |
| **撤销与重做** | Ctrl+Z 撤销；Ctrl+Y / Ctrl+Shift+Z 重做，保留最近 100 次文本修改 |
| **鼠标多行拖选** | 左键拖选文字，拖到编辑区上下边缘时自动滚动 |
| **编辑状态保留** | 缩放窗口保留草稿、光标、选区、输出开关和撤销历史 |
| **补全弹窗优化** | 不透明背景、独立前景层、底部向上弹出及屏幕边界限制 |
| **草稿同步保护** | 等待首次服务器数据后允许保存，后续输出更新不会覆盖草稿 |

撤销历史仅保留在当前编辑界面中，关闭界面后不保存。详细的 bug 成因、修复方式和验证范围见 [1.20.1 回迁说明](docs/BACKPORT-1.20.1.md)。

### 🔄 开发中功能
| 功能        | 描述            |
|-----------|---------------|
| **色彩扩展**  | 渲染括号与括号内文本的颜色 |
| **高度可配置** | 自定义换行规则、缩进风格等 |
| **设置界面**  | 直观的模组配置选项     |
| **代码优化**  | 优化代码结构，提升运行效率 |

## 📥 安装指南

### 系统要求
- **Minecraft版本**: 1.20.1
- **模组加载器**: Forge 47.4.13
- **Java版本**: Java 17

## 📝 许可证
完整许可证请查看 [LICENSE](LICENSE) 文件。

### 常见问题
<details>
<summary><b>Q: 支持其他Minecraft版本吗？</b></summary>

A: 本分支对应 Minecraft 1.20.1 Forge。Minecraft 1.21.1 NeoForge 版本位于仓库的 `1.21.1-neoforge` 分支。
</details>

<details>
<summary><b>Q: 是客户端模组吗？</b></summary>

A: 界面替换和编辑功能仅在客户端启用，服务端沿用原版数据包，不需要安装本模组。编辑命令方块仍需服务器授予相应权限。真实服务器保存往返和第三方模组兼容性仍需游戏内验收。
</details>

<details>
<summary><b>Q: 性能如何？</b></summary>

A: 目前模组处于开发中，不保证性能有多好，但是就渲染个文本估计性能影响也不大。
</details>

**最后更新**: 2026年10月6日

**版本**: v1.0.1（1.20.1 功能回迁修复版）

**开发者**: 青花(Cyanhana_Neko)

---
> **注意**: 本模组仍在开发中，某些功能可能还不完善。欢迎反馈和建议！
