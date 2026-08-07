# Mindustry Mod Generator

面向 IntelliJ IDEA 的 Mindustry Java 模组开发插件：工程向导、一键构建 / 安装 / 运行 / 调试。

---

## 功能特性

### 1. 模组工程向导

创建 Gradle（Groovy DSL）Mindustry Java 模组工程，自动生成 `gradlew`、`build.gradle`、`mod.hjson`、示例主类、`assets` 资源与 GitHub Actions 工作流。

向导采用双页签布局：

- **模组信息**：显示名称、模组名称、作者、描述、主类路径、模组版本（可编辑）
- **游戏版本**：
  - 版本类型：稳定版 / 测试版
  - 游戏版本：组合框 + `[−][页码][＋]` 翻页选择（每页 100 个版本）
  - 最低游戏版本：可与游戏版本联动同步

生成的 `build.gradle` 使用所选的游戏版本作为编译依赖版本（`mindustryVersion`），`mod.hjson` 使用最低游戏版本（`minGameVersion`）。

### 2. 运行 / 调试配置

新增 "Mindustry Game" 运行配置，一键执行完整链路：

1. **构建**：调用外部 `gradlew`（wrapper）执行 Gradle 任务（默认 `jar`），输出流式显示在控制台
2. **安装**：按模块名精确定位 `build/libs/<模块名>Desktop.jar`，先删除模组目录中的旧文件再复制
3. **启动**：用模块/项目 JDK 执行 `java -jar <游戏jar>`，Debug 模式附加游戏 `-debug` 参数

**Debug 调试**：采用 IDE 框架原生的本地调试路径（`JavaCommandLineState`）——调试器先监听端口，游戏启动后主动连接，附加零竞态；支持断点、变量、调用栈。

可配置项：Gradle 任务、游戏 JAR 路径、模组目录、启动前构建、复制到模组目录。

### 3. 设置（Settings → Mindustry）

- **游戏 JAR 路径**：默认 `~/.mindustry/Mindustry.jar`，可浏览选择
- **版本管理表格**：稳定版 / 测试版列表（分页 100/页）、可点击的下载链接、一键下载按钮、已下载状态标记（`Mindustry-<版本>.jar` / `Mindustry-BE-<版本>.jar` 存在即已下载）；下载成功自动更新游戏路径
- **镜像**：前缀模式列表（默认 `https://gh.noki.icu/`），可添加 / 删除；勾选"前缀模式"= 镜像 URL + 原始 URL，取消勾选 = 直连；版本列表获取与下载均走镜像
- **模组目录**：Windows 默认 `%USERPROFILE%\AppData\Roaming\Mindustry\mods`，其他平台默认 `~/.local/share/Mindustry/mods`

---

## 使用说明

### 创建模组项目

`File → New → Project`，选择 **Mindustry Mod Project**，填写模组信息并在"游戏版本"页签中选择版本，点击完成即可。

### 运行与调试

1. 打开运行配置对话框，新建 **Mindustry Game** 配置，选择目标模块
2. 点击 **Run**：构建 → 安装 → 启动游戏
3. 点击 **Debug**：游戏以 `-debug` 模式启动并等待调试器附加；附加成功后按 **F9（Resume）** 游戏窗口出现，可打断点调试

### 配置游戏与镜像

在 `Settings → Mindustry` 中选择游戏版本并点击下载（或手动指定游戏 JAR 路径）；必要时添加 / 切换镜像。

---

## 技术实现

- **工程向导**：基于 IDE Starter 框架（`StarterModuleBuilder` + `StarterInitialStep`），页签布局，模板属性通过 `MINDUSTRY_*` 注入文件模板
- **运行配置**：`ModuleBasedConfiguration` + `JavaCommandLineState`；构建经 `GeneralCommandLine` 调用外部 wrapper（30 分钟超时）
- **调试**：框架本地调试路径 —— `RemoteConnectionBuilder` 注入 jdwp（默认 `server=n`，游戏主动连接），调试器先 `SocketListen` 监听端口，应用启动后连接，无需自定义附加逻辑
- **版本获取**：GitHub tags API（`?per_page=100&page=N`）；Apache HttpClient（10s 连接 / 15s 读取超时），镜像失败自动回退直连，空响应视为失败
- **下载**：`HttpRequests`（10s 连接 / 60s 读取超时，跟随重定向）
- **持久化**：应用级 `PersistentStateComponent`（`mindustry-settings.xml`）
- **国际化**：中英文消息包（`MindustryProjectWizardBundle` / `MindustryRunBundle`）

---

## 项目目录结构

```
src/main/java/chire/idea/mindustry/
├── generators/            # 工程向导（模块类型、构建器、向导步骤、版本枚举）
├── run/                   # 运行配置（配置类型、运行配置、参数状态、Gradle 构建器）
├── settings/              # 设置（状态持久化、设置页、游戏下载器）
└── ui/                    # 共享 UI（版本表格面板）

src/main/resources/
├── META-INF/plugin.xml    # 插件描述符
├── fileTemplates/         # 文件模板（build.gradle / mod.hjson / 主类）
├── messages/              # 中英文消息包
└── template/              # 工程模板资源（gradlew、assets 等）
```

---

## 常见问题（FAQ）

**Q: 版本列表一直"加载中"或加载失败？**
A: 请检查网络与镜像可达性。插件带 10s 超时，镜像失败会自动回退直连；失败时控制台 / 状态栏会显示原因。

**Q: Debug 后游戏窗口不出现？**
A: 调试模式默认 `suspend=y`（游戏暂停在入口等待调试器）。按 **F9（Resume）** 后游戏才会运行。

**Q: 下载的游戏 jar 存放在哪里？**
A: 存放在"游戏 JAR 路径"所在目录，文件名为 `Mindustry-<版本>.jar` / `Mindustry-BE-<版本>.jar`，可多版本共存。

**Q: 镜像的"前缀模式"是什么？**
A: 勾选时请求地址为 `镜像URL + 原始URL`（如 `https://gh.noki.icu/https://api.github.com/...`）；不勾选则直接使用原始 URL。

**Q: 断点不生效？**
A: 请确认游戏版本不低于模组的 `minGameVersion`，并确保已按 F9 恢复运行。

---

## 构建与开发

- 环境要求：JDK 21+，IntelliJ Platform Gradle Plugin 2.11（Gradle 9.x）
- 构建插件：`./gradlew buildPlugin`
- 启动沙箱 IDE：`./gradlew runIde`

---

## 兼容性

- 目标 IDE：IntelliJ IDEA 2025.2（`since-build 252`，`until-build 252.*`）
- 语言：中文 / English
- 依赖 IDE 插件：`com.intellij.java`

---

## 许可证

本项目目前未附带 LICENSE 文件，所有权利归作者（ChiReS）所有。如需使用或分发，请先联系作者。
