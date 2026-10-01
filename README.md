# CommandBlockUIOverhaul - 命令方块界面重构

<p align="center">
  <strong>为 Minecraft 1.21.1 的命令方块带来现代化多行编辑体验</strong><br>
  <span>告别单行文本框，使用结构清晰、可补全的命令编辑器</span>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.1-blue?style=flat-square&logo=minecraft" alt="Minecraft 1.21.1">
  <img src="https://img.shields.io/badge/Loader-NeoForge-orange?style=flat-square&logo=curseforge" alt="NeoForge">
  <img src="https://img.shields.io/badge/License-MIT-green?style=flat-square" alt="MIT License">
</p>

## 模组简介

**命令方块界面重构（CommandBlockUIOverhaul）** 是一个面向 NeoForge 1.21.1 的轻量客户端模组。它将原版命令方块和命令方块矿车的单行输入框改造成宽敞的多行编辑器，同时保留原版命令方块的数据包和交互方式。

模组只改变客户端的编辑界面与输入体验，不修改命令语法，也不要求服务端安装同一个模组。命令仍通过 Minecraft 原版协议发送和同步。

## 核心特性

| 功能 | 描述 |
| --- | --- |
| **智能多行排版** | 根据命令结构自动换行，并对括号层级进行缩进 |
| **命令语法着色** | 使用 Brigadier 解析结果区分字面量、参数和错误文本 |
| **命令补全** | 支持 Tab 补全、上下键选择和鼠标选择；补全窗口会自动避开屏幕边缘 |
| **撤销与重做** | Ctrl+Z 撤销，Ctrl+Y 或 Ctrl+Shift+Z 重做，保留最近 100 次编辑 |
| **鼠标多行选择** | 左键拖选文本，拖到编辑区边缘时自动滚动 |
| **命令方块支持** | 支持普通命令方块、连锁/循环/脉冲设置和命令方块矿车 |
| **输出显示** | 保留原版上一条输出和跟踪输出开关 |
| **安全同步** | 首次收到服务器数据后初始化编辑内容，后续更新不会覆盖未保存草稿 |
| **窗口缩放保留状态** | 保留草稿、光标、选区、输出开关和撤销历史 |

输入框支持 Unicode 文本的安全截断和删除，不会在补充平面字符的代理对中间截断。补全请求也会忽略过期结果，避免网络延迟导致提示回退到旧命令。

## 常见问题

<details>
<summary><b>Q：支持 Minecraft 1.20.1 吗？</b></summary>

A：1.20.1 Forge 版本请使用仓库的 `master` 分支。本页面对应的是 `1.21.1-neoforge` 分支。
</details>

<details>
<summary><b>Q：这是服务端模组吗？</b></summary>

A：它主要是客户端界面模组。服务端不需要安装，但玩家仍需拥有原版命令方块编辑权限。
</details>

<details>
<summary><b>Q：为什么补全内容和其他版本不一样？</b></summary>

A：补全来自当前连接的 Minecraft 1.21.1 服务器命令树，因此会受到游戏版本、服务器模组和玩家权限影响。
</details>

## 许可证

本项目使用 MIT 许可证，完整文本见 [LICENSE](LICENSE)。

开发者：青花（Cyanhana_Neko）；最后更新：2026 年 10 月 1 日

欢迎提交 Issue 反馈界面问题、补全异常或兼容性信息。
