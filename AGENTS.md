# Repository Guidelines

## 项目结构与模块组织
本仓库是一个基于 Gradle 的单模块 IntelliJ Platform 插件项目。

- `src/main/java/indi/bookmarkx/`：核心源码，按职责拆分为 `action/`、`ui/`、`listener/`、`persistence/`、`model/`、`utils/` 等目录。
- `src/main/resources/`：插件资源与元数据，包括 `META-INF/plugin.xml`、图标资源和 `i18n/` 国际化文件。
- `src/test/java/indi/bookmarkx/`：测试代码，当前主要覆盖模型和树结构相关逻辑。
- `gradle/` 与 `gradlew*`：Gradle Wrapper，统一本地构建环境。
- `build/`：构建产物目录，不要手动修改或提交无关内容。

## 构建、测试与开发命令
请在仓库根目录使用 Gradle Wrapper。

- `./gradlew.bat build`：编译项目、执行测试并打包插件。
- `./gradlew.bat test`：仅运行 JUnit 5 测试。
- `./gradlew.bat runIde`：启动沙箱 IntelliJ，用于本地手动调试插件。
- `./gradlew.bat clean`：清理构建产物。

项目使用 `org.jetbrains.intellij` 插件，当前开发基线是 IntelliJ `2021.2.2`。兼容范围在 `build.gradle` 和 `plugin.xml` 中维护。

## 代码风格与命名约定
Java 代码统一使用 4 空格缩进，并遵循 IntelliJ 默认格式化规则。包名保持在 `indi.bookmarkx` 下。

- 类名使用 `UpperCamelCase`，如 `BookmarksManager`、`RootWindowFactory`
- 方法和字段使用 `lowerCamelCase`
- 常量使用 `UPPER_SNAKE_CASE`
- 动作类以 `Action` 结尾，UI 面板类以 `Panel` 结尾，持久化或配置类优先使用 `Persistent`、`Settings` 等后缀

尽量保持方法短小、职责单一，不要把 UI、持久化和动作逻辑混写在同一个类中。

## 测试规范
测试框架为 JUnit 5，仓库已引入 Mockito，可用于模拟 IntelliJ 相关依赖。

- 测试文件放在 `src/test/java/` 对应包路径下
- 测试类命名使用 `*Test` 后缀
- 提交 PR 前至少执行一次 `./gradlew.bat test`

当前测试覆盖率不高。修改书签创建、持久化、导入导出、树结构导航等行为时，应补充对应测试。

## 提交与 Pull Request 规范
现有提交历史同时存在中文简述和带 scope 的 conventional 风格，如 `refactor(bookmarks): ...`。建议继续使用简洁、明确、偏祈使句的提交标题；涉及明确模块时带上 scope。

示例：

- `fix(listener): 修复行号变化后图标未更新的问题`
- `refactor(ui): 简化书签编辑面板状态切换逻辑`

PR 应包含变更摘要、关联 issue（如有）、测试说明；如果改动涉及 Tool Window、对话框、Gutter 图标或交互流程，建议附上截图或 GIF。
