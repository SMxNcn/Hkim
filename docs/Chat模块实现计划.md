# Chat 模块实现计划

> 状态: **草稿待确认**  
> 基于 Minecraft 26.2-snapshot-7

---

## 一、功能清单

### 阶段一（基础渲染）

| # | 功能 | 说明 |
|---|------|------|
| 1.1 | 取消原版聊天渲染 | Mixin 取消 `Hud.extractChat()` 和 `ChatScreen` 中的 `chat.extractRenderState()` |
| 1.2 | 自定义聊天面板背景 | PIP `drawRoundedRect` 绘制圆角背景，支持颜色/透明度配置 |
| 1.3 | 消息文本渲染 | 用 `font.drawInBatch` 绘制 `FormattedCharSequence`，保持原版样式（颜色、加粗等） |
| 1.4 | 消息淡入动画 | 每条消息从 alpha=0 渐变到 alpha=1，用 `GuiAnimation` + `Easing.CUBIC_OUT` |
| 1.5 | 左下角锚定布局 | 消息从底部向上排列，和原版行为一致 |

### 阶段二（交互增强）

| # | 功能 | 说明 |
|---|------|------|
| 2.1 | 重复消息压缩 | 连续相同消息合并显示为 "消息 §7(N)"（淡灰色括号计数）。不压缩分隔类文本（纯虚线/方块符号等装饰行）。可配置时间窗口 |
| 2.2 | 聊天搜索 | 聊天窗口右下角添加搜索按钮（尺寸 = 默认聊天栏输入框高度）。点击后按钮上方弹出输入框，实时筛选，只显示包含目标文本的行（不高亮匹配文字自身），↑↓ 切换匹配项 |
| 2.3 | 聊天复制 | Ctrl+左键: 复制无格式纯文本消息。Ctrl+Shift+左键: 复制带格式消息（用 `&` 代替 `§`）。不干扰原版点击链接行为 |
| 2.4 | 消息交互保留 | 普通左键点击链接/悬停提示等原版交互行为继续可用 |

### 阶段二（续）

| # | 功能 | 说明 |
|---|------|------|
| 2.5 | 半屏聊天输入 | ChatScreen 模式下输入框宽度=聊天面板宽度，随输入字符长度自动向右扩展（不换行），支持超长输入时横向滚动 |
| 2.6 | 移除消息来源竖条 & 滚动条 | 去除消息左侧的彩色来源指示竖条及其 tooltip，同时移除右侧滚动条（由于我们完全接管了渲染，原版不画即可） |
| 2.7 | 聊天记录上限扩展 | 将 `ChatComponent` 内部的消息存储上限从 100 提升至 32767 |

### 阶段三（可选优化）

| # | 功能 | 说明 |
|---|------|------|
| 3.1 | 消息气泡效果 | 每条消息独立背景气泡 |
| 3.2 | 时间戳显示 | 消息旁显示时间 |
| 3.3 | 聊天过滤器 | 按关键词/正则过滤消息 |

---

## 二、架构总览

```
┌──────────────────────────────────────────────────────────────────┐
│  ChatModule (Module)                                              │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │  模块设置                                                   │  │
│  │  ├ 启用/禁用                                                │  │
│  │  ├ 背景颜色 (ColorSetting)                                  │  │
│  │  ├ 背景透明度 (NumberSetting)                                │  │
│  │  ├ 圆角半径 (NumberSetting)                                  │  │
│  │  ├ 淡入时长 (NumberSetting)                                  │  │
│  │  ├ 重复压缩开关 (BooleanSetting)                              │  │
│  │  └ 合并时间窗口 (NumberSetting)                              │  │
│  ├────────────────────────────────────────────────────────────┤  │
│  │  消息管理                                                   │  │
│  │  ├ messageStates: Map<GuiMessage, MessageAnimState>         │  │
│  │  ├ duplicateTracker: {lastMsg, count, timestamp}            │  │
│  │  └ searchQuery: String + matchIndices                       │  │
│  ├────────────────────────────────────────────────────────────┤  │
│  │  渲染入口 (被两个 Mixin 调用)                                 │  │
│  │  ├ renderHudChat(graphics)         ← Hud.extractChat 取消后  │  │
│  │  └ renderScreenChat(graphics, ...) ← ChatScreen 替换        │  │
│  │      └ 统一调用 renderChatInternal()                          │  │
│  └────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
```

---

## 三、Mixin 清单

### Mixin 1: 取消 Hud 聊天

```java
@Mixin(Hud.class)
public class HudMixin {
    @Inject(method = "extractChat", at = @At("HEAD"), cancellable = true)
    private void onExtractChat(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        if (ChatModule.INSTANCE.getEnabled()) ci.cancel();
    }
}
```

### Mixin 2: 替换 ChatScreen 聊天渲染

```java
@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    @Redirect(method = "extractRenderState", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/gui/components/ChatComponent;extractRenderState(" +
                 "Lnet/minecraft/client/gui/GuiGraphicsExtractor;" +
                 "Lnet/minecraft/client/gui/Font;IIII" +
                 "Lnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V"
    ))
    private void redirectChatRender(ChatComponent chat, GuiGraphicsExtractor graphics,
                                     Font font, int x, int y, int w, int h,
                                     ChatComponent.DisplayMode mode, boolean insertionClick) {
        if (!ChatModule.INSTANCE.getEnabled()) {
            chat.extractRenderState(graphics, font, x, y, w, h, mode, insertionClick);
            return;
        }
        ChatModule.INSTANCE.renderInChatScreen(graphics, font, x, y, w, h, mode);
    }
}
```

### Mixin 3: 聊天记录上限扩展

```java
@Mixin(ChatComponent.class)
public class ChatComponentMixin {
    @ModifyConstant(method = "addMessageToQueue", constant = @Constant(intValue = 100))
    private int increaseMaxChatHistory(int original) {
        return ChatModule.INSTANCE.getEnabled() ? 32767 : original;
    }
}
```

### Mixin 4: 读取 ChatComponent 内部数据 (Accessor)

```java
@Mixin(ChatComponent.class)
public interface ChatComponentAccessor {
    @Accessor("allMessages") List<GuiMessage> getAllMessages();
    @Accessor("trimmedMessages") List<GuiMessage.Line> getTrimmedMessages();
    @Accessor("chatScrollbarPos") int getChatScrollbarPos();
}
```

---

## 四、渲染坐标系统

```
Hud 模式（未打开聊天输入框）:
┌──────────────────────────────────────┐
│                                      │  screenHeight
│                                      │
│   ┌─ 自定义聊天面板 ────────────┐    │
│   │  最旧消息                     │    │  ← scrollPos 控制
│   │  ...                          │    │
│   │  ...                          │    │
│   │  [最新消息]                   │    │  ← chatBottom - 1*lineHeight
│   └───────────────────────────────┘    │
│                                 ◄19px │  ← chatBottom = screenHeight - 19
│   ⚔  ♥♥♥♥♥   ▉▉▉▉▉   快捷栏       │
└──────────────────────────────────────┘

ChatScreen 模式（按 T 后）:
┌──────────────────────────────────────┐
│   ┌─ 自定义聊天面板 ────────────┐    │
│   │  最旧消息                     │    │
│   │  ...                          │    │
│   │  [最新消息]                   │    │
│   └───────────────────────────────┘    │
│                                 ◄14px │  ← chatBottom = screenHeight - 14
│   ┌──────────────────────────────┐    │
│   │ ▷ 输入框 (原版 EditBox)       │    │
│   └──────────────────────────────┘    │
│   ┌─ /command 补全 ──────────────┐    │
│   └──────────────────────────────┘    │
└──────────────────────────────────────┘

消息定位公式:
  lineY (底边)  = chatBottom - (lineFromBottom + 1) * lineHeight
  lineTop (顶边) = lineY - lineHeight
  lineHeight    = 9 + chatLineSpacing (默认 9px)
```

---

## 五、渲染流程伪代码

```kotlin
fun renderChatInternal(graphics: GuiGraphicsExtractor, focused: Boolean) {
    val chat = mc.gui.hud.chat
    val font = mc.font

    // ── 1. 计算尺寸 ──
    val scale = mc.options.chatScale()
    val chatWidth = floor(40 + 280 * scale)
    val chatHeight = floor(20 + 160 * scale)
    val lineHeight = 9 + mc.options.chatLineSpacing().toFloat()
    val linesPerPage = (chatHeight / lineHeight).toInt()

    val baseX = 2f
    val chatBottom = mc.window.guiScaledHeight - if (focused) 14 else 19

    // ── 2. 用 PIP 绘制面板背景 ──
    if (showBackground) {
        graphics.drawRoundedRect(
            baseX, chatBottom - chatHeight,
            chatWidth, chatHeight,
            bgColor, cornerRadius
        )
    }

    // ── 3. 遍历消息行 ──
    var lineFromBottom = 0
    chat.forEachLine(AlphaCalculator.timeBased(chat.chatScrollbarPos)) { line, i, alpha ->

        val state = getOrCreateAnimState(line.parent)
        state.fadeAnim.update(deltaTime)
        val msgAlpha = alpha * state.fadeAnim.getValue()
        if (msgAlpha < 0.01f) return@forEachLine

        val lineY = chatBottom - (lineFromBottom + 1) * lineHeight
        val textX = baseX + 4
        val textY = lineY - lineHeight + 2  // baseline 对齐

        // ── 4. 每行背景（可选气泡） ──
        if (showMessageBg) {
            graphics.drawRoundedRect(
                textX - 2, lineY - lineHeight,
                chatWidth - 8, lineHeight,
                msgBgColor, msgCornerRadius
            )
        }

        // ── 5. 绘制文本 ──
        val finalColor = withAlpha(textColor, msgAlpha)
        val displayText = if (searchQuery.isNotEmpty())
            highlightMatches(line.content, searchQuery)
        else line.content
        font.drawInBatch(displayText, textX, textY, finalColor, ...)

        // ── 6. 重复计数标记 ──
        if (state.duplicateCount > 1) {
            font.drawInBatch(" ×${state.duplicateCount}",
                textX + font.width(displayText) + 2, textY,
                duplicateColor, ...)
        }

        lineFromBottom++
    }

    // ── 7. 搜索 UI（在聊天面板上方或内部） ──
    if (searchActive) {
        graphics.drawRoundedRect(baseX, chatBottom - chatHeight - 24,
            searchBarWidth, 20, searchBgColor, 4)
        font.drawInBatch("Search: $searchQuery ($currentMatch/${totalMatches})",
            baseX + 4, chatBottom - chatHeight - 18, 0xFFFFFFFF, ...)
    }
}
```

---

## 六、重复消息压缩逻辑

```kotlin
data class DuplicateState(
    val lastContent: String = "",
    var count: Int = 1,
    var lastTime: Long = 0L,
    var lastGuiMessage: GuiMessage? = null
)

// 分隔类文本检测 — 不压缩这些
// 规则: 连续 20 个以上非字母/数字/中文的相同字符视为分隔线
private fun isSeparator(text: String): Boolean {
    val clean = text.replace(Regex("§."), "")  // 先去掉格式化码
    if (clean.length < 20) return false

    // 取第一个非字母/数字/中文字符
    val firstChar = clean.firstOrNull { c ->
        !c.isLetterOrDigit() && !isChinese(c)
    } ?: return false

    // 检查是否全部由这个字符组成
    return clean.all { c ->
        c == firstChar || c == ' ' || isFormattingCode(c)
    }
}

private fun isChinese(c: Char): Boolean {
    return c in '一'..'鿿' || c in '㐀'..'䶿'
           || c in '豈'..'﫿' || c in '⾀0'..'⾡F'
}

// 精简版（如果确定只有单字符重复）:
private fun isSeparatorSimple(text: String): Boolean {
    val clean = text.replace(Regex("§."), "")
    if (clean.length < 20) return false
    val chars = clean.toSet()
    return chars.size <= 2  // 字符 + 空格
}

// 消息到达时:
@EventHandler
fun onChat(event: ChatReceiveEvent) {
    if (!enabled || !compressDuplicates) return
    if (isSeparator(event.message)) return      // ← 分隔文本跳过压缩

    val now = System.currentTimeMillis()
    val currentText = event.message

    if (currentText == dupState.lastContent
        && now - dupState.lastTime < mergeWindowMillis
        && dupState.lastGuiMessage != null) {

        dupState.count++
        dupState.lastTime = now
        event.cancel()

        // 更新已发送消息的显示
        updateMergedMessage(dupState.lastGuiMessage, dupState.count)
    } else {
        dupState.lastContent = currentText
        dupState.count = 1
        dupState.lastTime = now
        dupState.lastGuiMessage = null
    }
}

// 显示格式: "消息 §7(N)"  淡灰色括号 + 数字
fun formatDuplicateCount(count: Int): String {
    return "§7($count)"
}
```

---

## 七、搜索逻辑

```
聊天面板右下角按钮布局:

┌─ 聊天面板 ──────────────────────────┐
│                                      │
│  消息1                               │
│  消息2  ← 匹配高亮                    │
│  ┌─ 搜索输入框 ──────────── [✕] ─┐   │
│  │  Search: xxx         3/5 匹配  │   │
│  └───────────────────────────────┘   │
│                          ┌────┐      │
│                          │ 🔍 │      │  ← 搜索按钮, 高度=输入框高度
│                          └────┘      │
└──────────────────────────────────────┘

搜索按钮:
  - 位置: 聊天面板右下角 (chatRight - buttonSize, chatBottom - buttonSize - margin)
  - 尺寸: buttonSize = 默认输入框高度 = 20px (ChatScreen 的 EditBox 高度)
  - 点击: 在按钮上方弹出搜索框
  - 图标: "🔍" 或放大镜文本
  - 样式: PIP drawRoundedRect 圆角按钮

搜索框 (点击按钮后出现):
  - 位置: 按钮正上方
  - 内容: 输入查询文字 + 匹配数 "3/5" + 关闭按钮 ✕
  - 操作: 实时筛选 (输入即查), 只显示包含目标文本的行（匹配行正常显示，不加背景/高亮）, ↑↓ 切换匹配项
```

```kotlin
// 搜索状态
private var searchActive = false
private var searchQuery = ""
private val searchInput = EditBox(font, x, y, w, h, Component.empty())
private var matchResults = MutableList<Int>()    // 匹配的 allMessages index
private var currentMatchIndex = 0

// 搜索按钮尺寸 (与 ChatScreen 输入框等高 ≈ 20px)
private val searchButtonSize = 20f

// ─── 渲染搜索按钮 ───
private fun renderSearchButton(graphics: GuiGraphicsExtractor) {
    val btnX = chatRight - searchButtonSize - 2
    val btnY = chatBottom - searchButtonSize - 2
    graphics.drawRoundedRect(btnX, btnY, searchButtonSize, searchButtonSize,
        buttonBgColor, 4f)
    // 放大镜图标
    graphics.centeredText(mc.font, "🔍",
        (btnX + searchButtonSize / 2).toInt(),
        (btnY + (searchButtonSize - mc.font.lineHeight) / 2).toInt(),
        0xFFFFFFFF)
}

// ─── 渲染搜索框 ───
private fun renderSearchBox(graphics: GuiGraphicsExtractor) {
    val boxW = chatWidth * 0.6f
    val boxH = 20f
    val boxX = chatRight - boxW - 2
    val boxY = chatBottom - searchButtonSize - boxH - 4

    graphics.drawRoundedRect(boxX, boxY, boxW, boxH, searchBgColor, 4f)
    searchInput.extractWidgetRenderState(graphics, mouseX, mouseY, delta)

    // 匹配计数
    if (searchQuery.isNotEmpty()) {
        graphics.text(mc.font, "${currentMatchIndex + 1}/${matchResults.size}",
            (boxX + boxW - 40).toInt(), (boxY + 4).toInt(), 0xFF888888, false)
    }
}

// ─── 执行搜索 ───
fun executeSearch(query: String) {
    searchQuery = query
    matchResults.clear()
    currentMatchIndex = 0
    if (query.isEmpty()) return

    val chat = mc.gui.hud.chat
    for ((index, msg) in chat.allMessages.withIndex()) {
        if (msg.content.string.contains(query, ignoreCase = true)) {
            matchResults.add(index)
        }
    }
}

// ─── 搜索模式下的 forEachLine 包装 ───
// 在 renderChatInternal 中:
//   如果 searchActive，跳过不匹配的行；匹配行正常渲染（不加背景/高亮）
chat.forEachLine(alphaCalc) { line, i, alpha ->
    if (searchActive && !isMatch(line.parent)) return@forEachLine  // ← 直接跳过不匹配行，不显示
    // ... 正常渲染（和未搜索时一样）
}
```

---

## 八、复制逻辑

```kotlin
// 点击处理: 在 ChatScreen 的 mouseClicked 或 Hud 的鼠标事件中注入
// 判断条件:
//   Ctrl+左键         → 复制纯文本 (clean string)
//   Ctrl+Shift+左键   → 复制格式化文本 (& 代替 §)
//   普通左键           → 原版行为 (点击链接等)

fun handleChatClick(mouseX: Float, mouseY: Float, button: Int,
                    ctrl: Boolean, shift: Boolean): Boolean {
    if (button != 0) return false           // 仅左键
    if (!ctrl && !shift) return false       // 无修饰键 → 原版行为

    val hoveredLine = findHoveredLine(mouseX, mouseY) ?: return false
    val text = hoveredLine.parent.content.string  // 原始 Component 文本

    val clipboardText = when {
        ctrl && shift -> formatWithAmpersand(text)  // "&eHello" 格式
        ctrl -> text.clean                           // 去 § 码
        else -> return false
    }

    mc.keyboardHandler.setClipboard(clipboardText)
    modMessage("§7已复制到剪贴板")
    return true
}

// 将 § 格式码转为 & 格式
private fun formatWithAmpersand(text: String): String {
    return text.replace("§", "&")
}

// 查找鼠标所在的消息行
private fun findHoveredLine(mx: Float, my: Float): GuiMessage.Line? {
    // 利用 ChatComponent.forEachLine 获取每行位置进行命中检测
    val chat = mc.gui.hud.chat
    var result: GuiMessage.Line? = null
    var lineFromBottom = 0
    chat.forEachLine(AlphaCalculator.timeBased(chat.chatScrollbarPos)) { line, i, _ ->
        val lineY = chatBottom - (lineFromBottom + 1) * lineHeight
        if (my in lineY - lineHeight..lineY && mx in baseX..baseX + chatWidth) {
            result = line
        }
        lineFromBottom++
    }
    return result
}

// 注入方式:
//   Hud 模式 → 在 HudMixin 或 MouseHandlerMixin 中检测点击
//   ChatScreen 模式 → 在 ChatScreenMixin 中 hook mouseClicked
```

---

## 九、额外说明

### 9.1 2.6 移除消息来源竖条 & 滚动条（零成本）

由于我们通过 Mixin 完全取消了原版 `chat.extractRenderState()` 的调用，消息左侧的彩色来源指示竖条（由 `ChatGraphicsAccess.handleTag()` 绘制）和右侧任何滚动条自然**不再出现**。无需额外代码。

### 9.2 2.7 聊天记录上限扩展

通过 `@ModifyConstant` 将 `addMessageToQueue` 中的硬编码常量 `100` 替换为 `32767`：

```java
@Mixin(ChatComponent.class)
public class ChatComponentMixin {
    @ModifyConstant(method = "addMessageToQueue", constant = @Constant(intValue = 100))
    private int increaseMaxChatHistory(int original) {
        return ChatModule.INSTANCE.getEnabled() ? 32767 : original;
    }
}
```

`allMessages` 和 `trimmedMessages` 本身是 `ArrayList` 无上限，唯一限制就是 `addMessageToQueue` 中 `if (size > 100) removeLast()`。

---

## 十一、半屏聊天输入（2.5）详细设计

### 11.1 行为定义

```
Hud 模式 (未打开聊天):
  ┌─ 聊天面板 (chatWidth) ──────────┐
  │  消息                            │
  │  消息                            │
  └──────────────────────────────────┘
  聊天面板宽度 = 原版宽度 (40 + 280*scale)

ChatScreen 模式 (按 T):
  ┌─ 聊天面板 (半屏宽度) ───────────────────────┐
  │  消息                                       │
  │  消息                                       │
  ├────────────────────────────────────────────┤
  │  ▷ 输入内容  (同宽，随输入向右延伸)          │
  │  ▷ 输入内容 很长时→→→→→→→→→→→→→→→→→→→→→→ │
  └────────────────────────────────────────────┘
  面板宽度 = max(原版宽度, screenWidth / 2)
  输入框宽度 = 面板宽度，随文本增长向右扩展
```

### 11.2 面板宽度计算

```kotlin
fun getChatPanelWidth(focused: Boolean): Int {
    val baseWidth = floor(40 + 280 * mc.options.chatScale())
    return if (focused) {
        maxOf(baseWidth, mc.window.guiScaledWidth / 2)   // 至少半屏
    } else {
        baseWidth                                           // Hud 模式用原版宽度
    }
}

fun getChatPanelHeight(focused: Boolean): Int {
    return if (focused) {
        floor(20 + 160 * mc.options.chatHeightFocused())
    } else {
        floor(20 + 160 * mc.options.chatHeightUnfocused())
    }
}
```

### 11.3 输入框渲染（替换原版 EditBox）

原版 ChatScreen 中，`input` 是一个 `EditBox(font, 2, height-14, width-4, 14, ...)`，宽度固定为 `screenWidth - 4`。EditBox 是**单行输入**，文本超出时横向滚动而非换行。

我们要做的：

```kotlin
// 在 ChatScreen 初始化时，设置 input 宽度 = 面板宽度
input.width = getChatPanelWidth(focused = true) - 4

// 渲染时：根据当前文本宽度动态扩展 input.width
fun updateInputWidth() {
    val textWidth = mc.font.width(input.value)  // 当前文本像素宽度
    val minWidth = getChatPanelWidth(focused = true) - 4
    val maxWidth = mc.window.guiScaledWidth - 4   // 不超过屏幕宽度

    input.width = (minWidth + textWidth).coerceIn(minWidth, maxWidth)
}

// 每次 input 内容变化时调用:
input.setResponder { updateInputWidth() }
```

### 11.4 聊天面板宽度联动

聊天面板宽度要和输入框宽度同步：

```kotlin
// 在 renderChatInternal 中:
val panelWidth = if (focused) {
    maxOf(
        floor(40 + 280 * mc.options.chatScale()),
        mc.window.guiScaledWidth / 2,
        input.width + 4   // 确保面板不比输入框窄
    )
} else {
    floor(40 + 280 * mc.options.chatScale())
}
```

### 11.5 消息文本折行宽度

面板变宽后，每条消息的折行宽度也要相应变大，否则文字只占面板左半部分：

```kotlin
// 在渲染每条消息时，如果面板宽度 > 原版宽度，需要重新 splitLines
val splitWidth = panelWidth - 8  // 减去内边距
val displayLines = if (focused && panelWidth > originalChatWidth) {
    line.parent.splitLines(font, splitWidth)  // 用更宽的宽度重新折行
} else {
    listOf(line)  // 用原版 trimmedMessages
}
```

### 11.6 实现要点

| 方面 | 方案 |
|------|------|
| 面板宽度 | `max(原版宽度, 半屏宽度, 输入框宽度)` |
| 输入框宽度 | 初始 = 面板宽度，随文本增长 `minWidth + textWidth` |
| 输入框位置 | 左下角锚定，左侧与面板对齐 |
| 输入框最大宽度 | `screenWidth - 4`（不超出屏幕） |
| 消息折行 | 面板变宽时用 `GuiMessage.splitLines(font, newWidth)` 重新折行 |
| Hud 模式 | 不受影响，保持原版宽度 |

---

## 十三、文件清单

| 文件 | 类型 | 内容 |
|------|------|------|
| `features/impl/Chat.kt` | 新建 | ChatModule 主逻辑 |
| `mixins/HudChatMixin.java` | 修改 | 添加 `extractChat` 取消 |
| `mixins/ChatScreenMixin.java` | 新建 | 替换 ChatScreen 聊天渲染 |
| `mixins/ChatComponentMixin.java` | 新建 | 扩展聊天记录上限 (100 → 32767) |
| `mixins/accessors/ChatComponentAccessor.java` | 新建 | 读取 `allMessages`, `trimmedMessages`, `chatScrollbarPos` |

---

## 十四、实施顺序

```
Step 1: Mixin + Accessor
  ├─ HudChatMixin: 取消 extractChat
  ├─ ChatScreenMixin: @Redirect chat.extractRenderState
  ├─ ChatComponentMixin: @ModifyConstant 100 → 32767
  └─ ChatComponentAccessor: @Accessor

Step 2: ChatModule 骨架
  ├─ Module 定义 + 设置项
  ├─ renderHudChat() + renderScreenChat()
  └─ forEachLine 遍历 + 左下角定位

Step 3: 自定义背景 + 文本渲染
  ├─ PIP drawRoundedRect 面板背景
  └─ font.drawInBatch 文本

Step 4: 淡入动画
  ├─ MessageAnimState + GuiAnimation
  └─ alpha 叠加到 forEachLine 的 alpha

Step 5: 重复消息压缩
  ├─ ChatReceiveEvent 拦截
  ├─ DuplicateState 跟踪
  └─ "消息 ×N" 显示

Step 6: 聊天搜索
  ├─ Ctrl+F 切换
  ├─ allMessages 遍历匹配
  └─ 高亮 + ↑↓ 导航

Step 7: 聊天复制
  └─ 悬停检测 + clipboard 写入

Step 8: 半屏聊天输入
  ├─ 面板宽度计算 (Hud窄/ChatScreen半屏)
  ├─ 输入框宽度动态扩展
  └─ 消息折行宽度联动

注: 2.6（移除竖条/滚动条）已在 Step 1 中随取消原版渲染自动完成，无需额外步骤。2.7（记录上限）已在 Step 1 的 ChatComponentMixin 中完成。
```

---

> ⏳ 请确认以上功能范围和实施方案是否符合预期，确认后开始编码。