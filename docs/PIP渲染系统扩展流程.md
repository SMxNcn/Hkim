# PIP 渲染系统扩展流程

> 生成日期: 2026-06-10  
> 基于 Minecraft 26.2-snapshot-7 (Fabric) 的 Picture-in-Picture 渲染 API  
> 项目: HkimV

---

## 目录

1. [系统架构总览](#1-系统架构总览)
2. [数据流全链路](#2-数据流全链路)
3. [扩展流程：添加新形状](#3-扩展流程添加新形状)
   - [3.1 编写着色器](#31-编写着色器)
   - [3.2 注册管线](#32-注册管线)
   - [3.3 定义 ShapeCommand](#33-定义-shapecommand)
   - [3.4 实现 buildDraw](#34-实现-builddraw)
   - [3.5 在 renderToTexture 中处理](#35-在-renderToTexture-中处理)
   - [3.6 暴露 ShapeRenderer API](#36-暴露-shapeRenderer-api)
4. [完整示例：添加一个 Triangle](#4-完整示例添加一个-triangle)
5. [着色器参数传递约定](#5-着色器参数传递约定)
6. [PIP 注册与生命周期](#6-pip-注册与生命周期)
7. [各文件位置索引](#7-各文件位置索引)

---

## 1. 系统架构总览

```
┌─────────────────────────────────────────────────────────────────┐
│  API 层 (ShapeRenderer.kt)                                      │
│  graphics.drawRoundedRect(x, y, w, h, color, radius)            │
│  graphics.drawCircle(cx, cy, color, radius)                     │
├─────────────────────────────────────────────────────────────────┤
│  PIPState 构建层 (ShapeRenderer.kt → buildAndSubmitPipState())   │
│  创建 PIPState → 填充 ShapeCommand 列表 → 提交给 GuiRenderState   │
├─────────────────────────────────────────────────────────────────┤
│  Minecraft Pip 调度层                                            │
│  GuiRenderState → 遍历 PIP states → PIPRenderer.renderToTexture │
├─────────────────────────────────────────────────────────────────┤
│  离屏渲染层 (PIPRenderer.kt)                                     │
│  创建 offscreen texture → 遍历 commands → buildDraw → submit    │
├─────────────────────────────────────────────────────────────────┤
│  Blit 合成层 (PIPRenderer.kt → blitTexture())                   │
│  将离屏纹理合成回 GUI 层                                         │
└─────────────────────────────────────────────────────────────────┘
```

### 核心文件

| 文件 | 路径 | 职责 |
|------|------|------|
| `PipState.kt` | `utils/render/pip/PipState.kt` | `PIPState` 渲染状态 + `ShapeCommand` 密封接口 |
| `PipRenderer.kt` | `utils/render/pip/PipRenderer.kt` | PIP 离屏渲染器 + 顶点组装 + 绘制提交 |
| `ShapeRenderer.kt` | `utils/render/pip/ShapeRenderer.kt` | 对外 API 扩展函数 + PIPState 构建 |
| `CustomRenderPipelines.kt` | `utils/render/CustomRenderPipelines.kt` | 自定义 RenderPipeline 注册 |
| `CustomRenderType.kt` | `utils/render/CustomRenderType.kt` | 3D 世界 RenderType (非 PIP 相关) |
| `RenderUtils.kt` | `utils/render/RenderUtils.kt` | 3D 世界渲染批处理 (非 PIP 相关) |

---

## 2. 数据流全链路

```
调用方 (ClickGUIScreen / Setting)
  │
  ▼
graphics.drawRoundedRect(x, y, w, h, color, radius)
  │  [ShapeRenderer.kt 扩展函数]
  ▼
buildAndSubmitPipState(graphics, x, y, w, h, commands)
  │  ├─ 计算 bounds (考虑 padding)
  │  ├─ 计算 scissor (与父 scissor 求交)
  │  ├─ 创建 PIPState(x0, y0, x1, y1, scale, scissor, pose, commands, modelView)
  │  └─ graphics.guiRenderState.addPicturesInPictureState(state)
  ▼
Minecraft 帧渲染循环 → 遍历所有 PIP states
  │
  ▼
PIPRenderer.renderToTexture(state, poseStack, collector)
  │  ├─ obtainStateTex() → 从池中获取/创建 offscreen texture
  │  ├─ 遍历 state.commands:
  │  │   ├─ ShapeCommand.RoundedRect → buildDraw() → DrawCmd
  │  │   └─ ShapeCommand.Circle     → buildDraw() → DrawCmd
  │  ├─ createRenderPass → 设置 pipeline、uniforms、vertex/index buffer
  │  └─ drawIndexed
  ▼
PIPRenderer.blitTexture(state, guiRenderState)
  │  └─ guiRenderState.addBlitToCurrentLayer(BlitRenderState)
  ▼
合成到屏幕
```

---

## 3. 扩展流程：添加新形状

要添加一个新形状（如 Triangle），需要完成以下 **6 步**：

### 3.1 编写着色器

**位置**: `src/main/resources/assets/hkim/shaders/core/<name>.vsh` 和 `.fsh`

**Vertex Shader** (模板):
```glsl
#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec4 Color;

out vec4 vertexColor;
out vec2 fragCoord;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color;
    fragCoord = Position.xy;
}
```

**Fragment Shader** — 使用 SDF (Signed Distance Field) 定义形状和抗锯齿：
```glsl
#version 330

#moj_import <minecraft:dynamictransforms.glsl>

in vec4 vertexColor;
in vec2 fragCoord;

out vec4 fragColor;

void main() {
    // 从 TextureMat 解包参数 (参见 §5 参数约定)
    vec2 p0 = TextureMat[0].xy;
    vec2 p1 = TextureMat[0].zw;
    // ...

    // SDF 计算 + 抗锯齿
    float dist = ...;
    float alpha = 1.0 - smoothstep(0.0, 1.0, dist);

    // Over 合成 (边框 + 填充)
    vec4 fillColor = vertexColor;
    // ...
    fragColor = vec4(fillColor.rgb, fillColor.a * alpha);
}
```

### 3.2 注册管线

**位置**: `CustomRenderPipelines.kt`

```kotlin
val MY_SHAPE: RenderPipeline = RenderPipelines.register(
    RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath("hkim", "my_shape"))
        .withVertexShader(Identifier.fromNamespaceAndPath("hkim", "core/my_shape"))
        .withFragmentShader(Identifier.fromNamespaceAndPath("hkim", "core/my_shape"))
        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
        .withCull(false)
        .withDepthStencilState(DepthStencilState(CompareOp.ALWAYS_PASS, false))
        .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .build()
)
```

### 3.3 定义 ShapeCommand

**位置**: `PipState.kt`

```kotlin
sealed interface ShapeCommand {
    // 已有
    data class RoundedRect(...) : ShapeCommand
    data class Circle(...) : ShapeCommand

    // 新增
    data class MyShape(
        val param1: Float,
        val param2: Float,
        // ...
        val fillColor: Int,
        val borderColor: Int?,
        val borderWidth: Float
    ) : ShapeCommand
}
```

### 3.4 实现 buildDraw

**位置**: `PipRenderer.kt` — 新增一个 `buildDraw` 重载

```kotlin
private fun buildDraw(
    cmd: ShapeCommand.MyShape,
    modelView: Matrix4f,
    device: GpuDevice
): DrawCmd? {
    val pipeline = CustomRenderPipelines.MY_SHAPE
    val c = unpackColor(cmd.fillColor)
    val bc = cmd.borderColor ?: 0

    // 将参数打包到 TextureMat 矩阵 (参见 §5 约定)
    val texMat = Matrix4f()
    texMat.m00(cmd.param1); texMat.m01(cmd.param2)
    texMat.m02(...); texMat.m03(...)
    texMat.m10(...)
    texMat.m11(if (cmd.borderColor != null) cmd.borderWidth else 0f)
    texMat.m12(((bc shr 16) and 0xFF) / 255f)
    texMat.m13(((bc shr 8) and 0xFF) / 255f)
    texMat.m20(((bc and 0xFF) / 255f))
    texMat.m21(((bc shr 24) and 0xFF) / 255f)

    val dynTrans = obtainDynTrans(device).also { writeDynTrans(it, modelView, texMat) }

    // 构建四边形顶点 (带 2px padding 防止抗锯齿裁切)
    val alloc = ByteBufferBuilder(128)
    val buf = BufferBuilder(alloc, PrimitiveTopology.QUADS,
        pipeline.getVertexFormatBinding(0) ?: DefaultVertexFormat.POSITION_COLOR)
    val pad = 2f
    // 4 个顶点 ...
    buf.addVertex(x0 - pad, y0 - pad, 0f).setColor(c[0], c[1], c[2], c[3])
    buf.addVertex(x1 + pad, y1 - pad, 0f).setColor(c[0], c[1], c[2], c[3])
    buf.addVertex(x2 + pad, y2 + pad, 0f).setColor(c[0], c[1], c[2], c[3])
    buf.addVertex(x3 - pad, y3 + pad, 0f).setColor(c[0], c[1], c[2], c[3])

    val mesh = buf.buildOrThrow()
    val vData = mesh.vertexBuffer()
    if (vData.remaining() == 0) { mesh.close(); alloc.close(); return null }
    val vSize = vData.remaining().toLong()

    val vtx = device.createBuffer({ "hkim_ms_v" },
        GpuBuffer.USAGE_VERTEX or GpuBuffer.USAGE_MAP_WRITE, vSize)
    vtx.map(0, vSize, false, true).use { it.data().put(vData) }

    mesh.close(); alloc.close()
    return DrawCmd(pipeline, vtx, dynTrans, quadIndexBuffer, IndexType.SHORT, 6)
}
```

### 3.5 在 renderToTexture 中处理

**位置**: `PipRenderer.kt` — `renderToTexture()` 方法的 `when` 分支

```kotlin
for (cmd in state.commands) {
    when (cmd) {
        is ShapeCommand.RoundedRect -> { draws.add(buildDraw(cmd, mv, device) ?: continue) }
        is ShapeCommand.Circle      -> { draws.add(buildDraw(cmd, mv, device) ?: continue) }
        is ShapeCommand.MyShape     -> { draws.add(buildDraw(cmd, mv, device) ?: continue) }  // ← 新增
    }
}
```

### 3.6 暴露 ShapeRenderer API

**位置**: `ShapeRenderer.kt`

```kotlin
fun GuiGraphicsExtractor.drawMyShape(
    param1: Float, param2: Float,
    color: Int,
    // ...
) {
    // 计算包围盒
    val bounds = ...
    buildAndSubmitPipState(this, bounds.x, bounds.y, bounds.w, bounds.h, listOf(
        ShapeCommand.MyShape(param1, param2, ..., color, null, 0f)
    ))
}
```

---

## 4. 完整示例：添加一个 Triangle

### 4.1 着色器

**`core/triangle.fsh`**:
```glsl
#version 330

#moj_import <minecraft:dynamictransforms.glsl>

in vec4 vertexColor;
in vec2 fragCoord;

out vec4 fragColor;

float triangleSDF(vec2 p, vec2 p0, vec2 p1, vec2 p2) {
    vec2 e0 = p1 - p0; vec2 e1 = p2 - p1; vec2 e2 = p0 - p2;
    vec2 v0 = p - p0;  vec2 v1 = p - p1;  vec2 v2 = p - p2;

    float d0 = (e0.y*v0.x - e0.x*v0.y);
    float d1 = (e1.y*v1.x - e1.x*v1.y);
    float d2 = (e2.y*v2.x - e2.x*v2.y);

    bool hasNeg = (d0 < 0) || (d1 < 0) || (d2 < 0);
    bool hasPos = (d0 > 0) || (d1 > 0) || (d2 > 0);

    float dist = 0.0;
    if (!(hasNeg && hasPos)) {
        // 在三角形内部
        dist = -min(abs(d0), min(abs(d1), abs(d2)));
    } else {
        // 在三角形外部 → 到最近边的距离
        float c0 = dot(v0, e0) / dot(e0, e0);
        c0 = clamp(c0, 0.0, 1.0);
        vec2 proj0 = p0 + c0 * e0;
        dist = length(p - proj0);

        float c1 = dot(v1, e1) / dot(e1, e1);
        c1 = clamp(c1, 0.0, 1.0);
        vec2 proj1 = p1 + c1 * e1;
        dist = min(dist, length(p - proj1));

        float c2 = dot(v2, e2) / dot(e2, e2);
        c2 = clamp(c2, 0.0, 1.0);
        vec2 proj2 = p2 + c2 * e2;
        dist = min(dist, length(p - proj2));
    }

    return dist;
}

void main() {
    vec2 p0 = TextureMat[0].xy;
    vec2 p1 = TextureMat[0].zw;
    vec2 p2 = TextureMat[1].xy;

    float dist = triangleSDF(fragCoord, p0, p1, p2);
    float outerAlpha = 1.0 - smoothstep(0.0, 1.0, dist);

    vec4 fillColor = vertexColor;
    float borderWidth = TextureMat[1][2];

    if (borderWidth > 0.0) {
        float innerDist = dist + borderWidth;
        float innerAlpha = 1.0 - smoothstep(0.0, 1.0, innerDist);
        float borderMask = outerAlpha - innerAlpha;

        vec4 borderColor = vec4(TextureMat[1][3], TextureMat[2][0], TextureMat[2][1], TextureMat[2][2]);

        float fillA = fillColor.a * innerAlpha;
        vec3 fillPm = fillColor.rgb * fillA;
        float borderA = borderColor.a * borderMask;
        vec3 borderPm = borderColor.rgb * borderA;

        float outA = borderA + fillA * (1.0 - borderA);
        vec3 outPm = borderPm + fillPm * (1.0 - borderA);
        vec3 outRgb = (outA > 0.001) ? outPm / outA : vec3(0.0);

        if (outA < 0.01) discard;
        fragColor = vec4(outRgb, outA);
    } else {
        if (outerAlpha < 0.01) discard;
        fragColor = vec4(fillColor.rgb, fillColor.a * outerAlpha);
    }
}
```

### 4.2 注册管线

```kotlin
// CustomRenderPipelines.kt
val TRIANGLE: RenderPipeline = RenderPipelines.register(
    RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath("hkim", "triangle"))
        .withVertexShader(Identifier.fromNamespaceAndPath("hkim", "core/triangle"))
        .withFragmentShader(Identifier.fromNamespaceAndPath("hkim", "core/triangle"))
        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
        .withCull(false)
        .withDepthStencilState(DepthStencilState(CompareOp.ALWAYS_PASS, false))
        .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .build()
)
```

### 4.3 定义 ShapeCommand

```kotlin
// PipState.kt
data class Triangle(
    val x1: Float, val y1: Float,
    val x2: Float, val y2: Float,
    val x3: Float, val y3: Float,
    val fillColor: Int,
    val borderColor: Int?,
    val borderWidth: Float
) : ShapeCommand
```

### 4.4 实现 buildDraw

```kotlin
// PipRenderer.kt
private fun buildDraw(
    cmd: ShapeCommand.Triangle,
    modelView: Matrix4f,
    device: GpuDevice
): DrawCmd? {
    val pipeline = CustomRenderPipelines.TRIANGLE
    val c = unpackColor(cmd.fillColor)
    val bc = cmd.borderColor ?: 0

    val minX = minOf(cmd.x1, cmd.x2, cmd.x3)
    val minY = minOf(cmd.y1, cmd.y2, cmd.y3)
    val maxX = maxOf(cmd.x1, cmd.x2, cmd.x3)
    val maxY = maxOf(cmd.y1, cmd.y2, cmd.y3)

    val texMat = Matrix4f()
    texMat.m00(cmd.x1); texMat.m01(cmd.y1)      // P0
    texMat.m02(cmd.x2); texMat.m03(cmd.y2)      // P1
    texMat.m10(cmd.x3); texMat.m11(cmd.y3)      // P2
    texMat.m12(if (cmd.borderColor != null) cmd.borderWidth else 0f)
    texMat.m13(((bc shr 16) and 0xFF) / 255f)
    texMat.m20(((bc shr 8) and 0xFF) / 255f)
    texMat.m21(((bc and 0xFF) / 255f))
    texMat.m22(((bc shr 24) and 0xFF) / 255f)

    val dynTrans = obtainDynTrans(device).also { writeDynTrans(it, modelView, texMat) }

    val alloc = ByteBufferBuilder(128)
    val buf = BufferBuilder(alloc, PrimitiveTopology.QUADS,
        pipeline.getVertexFormatBinding(0) ?: DefaultVertexFormat.POSITION_COLOR)
    val pad = 2f
    buf.addVertex(minX - pad, minY - pad, 0f).setColor(c[0], c[1], c[2], c[3])
    buf.addVertex(maxX + pad, minY - pad, 0f).setColor(c[0], c[1], c[2], c[3])
    buf.addVertex(maxX + pad, maxY + pad, 0f).setColor(c[0], c[1], c[2], c[3])
    buf.addVertex(minX - pad, maxY + pad, 0f).setColor(c[0], c[1], c[2], c[3])

    val mesh = buf.buildOrThrow()
    val vData = mesh.vertexBuffer()
    if (vData.remaining() == 0) { mesh.close(); alloc.close(); return null }
    val vSize = vData.remaining().toLong()

    val vtx = device.createBuffer({ "hkim_tri_v" },
        GpuBuffer.USAGE_VERTEX or GpuBuffer.USAGE_MAP_WRITE, vSize)
    vtx.map(0, vSize, false, true).use { it.data().put(vData) }

    mesh.close(); alloc.close()
    return DrawCmd(pipeline, vtx, dynTrans, quadIndexBuffer, IndexType.SHORT, 6)
}
```

### 4.5 renderToTexture 处理

```kotlin
// PipRenderer.kt → renderToTexture() 的 when 分支
is ShapeCommand.Triangle -> {
    val d = buildDraw(cmd, mv, device) ?: continue
    draws.add(d)
}
```

### 4.6 ShapeRenderer API

```kotlin
// ShapeRenderer.kt
fun GuiGraphicsExtractor.drawTriangle(
    x1: Float, y1: Float,
    x2: Float, y2: Float,
    x3: Float, y3: Float,
    color: Int
) {
    val minX = minOf(x1, x2, x3)
    val minY = minOf(y1, y2, y3)
    val maxX = maxOf(x1, x2, x3)
    val maxY = maxOf(y1, y2, y3)
    val w = maxX - minX
    val h = maxY - minY
    buildAndSubmitPipState(this, minX, minY, w, h, listOf(
        ShapeCommand.Triangle(x1, y1, x2, y2, x3, y3, color, null, 0f)
    ))
}

fun GuiGraphicsExtractor.drawTriangleWithBorder(
    x1: Float, y1: Float,
    x2: Float, y2: Float,
    x3: Float, y3: Float,
    fillColor: Int, borderColor: Int, borderWidth: Float
) {
    val minX = minOf(x1, x2, x3)
    val minY = minOf(y1, y2, y3)
    val maxX = maxOf(x1, x2, x3)
    val maxY = maxOf(y1, y2, y3)
    val w = maxX - minX
    val h = maxY - minY
    buildAndSubmitPipState(this, minX, minY, w, h, listOf(
        ShapeCommand.Triangle(x1, y1, x2, y2, x3, y3, fillColor, borderColor, borderWidth)
    ))
}
```

### 4.7 调用示例

```kotlin
// 在 ClickGUIScreen 或任意 Setting 中:
graphics.drawTriangle(
    100f, 100f,   // P1
    200f, 100f,   // P2
    150f, 180f,   // P3
    0xFFFF4444.toInt()  // 红色填充
)

// 带边框:
graphics.drawTriangleWithBorder(
    100f, 100f, 200f, 100f, 150f, 180f,
    0x80FF4444.toInt(),  // 半透明红色填充
    0xFFFFFFFF.toInt(),  // 白色边框
    2f                    // 边框宽度 2px
)
```

---

## 5. 着色器参数传递约定

因为 Minecraft 的 GUI 管线预留了 `DynamicTransforms` uniform block（参考 `dynamictransforms.glsl`），其中包含 `TextureMat` 矩阵，我们用它来传递形状参数。

### TextureMat 矩阵布局

| 槽位 | 表达式 | 用途 |
|------|--------|------|
| `TextureMat[0]` | `texMat[0]` | `xy` = 参数 A, `zw` = 参数 B |
| `TextureMat[1]` | `texMat[1]` | `x` = 参数 C, `y` = borderWidth, `z` = borderR/255, `w` = borderG/255 |
| `TextureMat[2]` | `texMat[2]` | `x` = borderB/255, `y` = borderA/255, 其余可用 |

### 边界颜色打包

边框颜色使用 `ARGB` 格式的 `Int`，在 CPU 端拆解为 `[0,1]` float，写入 `TextureMat`：

```kotlin
val bc = cmd.borderColor ?: 0
texMat.m12(((bc shr 16) and 0xFF) / 255f)  // R
texMat.m13(((bc shr 8) and 0xFF) / 255f)   // G
texMat.m20(((bc and 0xFF) / 255f))          // B
texMat.m21(((bc shr 24) and 0xFF) / 255f)  // A
```

### writeDynTrans 函数

```kotlin
private fun writeDynTrans(buf: GpuBuffer, modelView: Matrix4f, texMat: Matrix4f) {
    // 布局: ModelViewMat(64) + ColorModulator(16) + ModelOffset(12+4填充) + TextureMat(64) = 160 bytes
    val bb = java.nio.ByteBuffer.allocateDirect(160).order(ByteOrder.nativeOrder())
    val fb = bb.asFloatBuffer()
    fun putM(m: Matrix4f) = fb.put(floatArrayOf(
        m.m00(), m.m01(), m.m02(), m.m03(),
        m.m10(), m.m11(), m.m12(), m.m13(),
        m.m20(), m.m21(), m.m22(), m.m23(),
        m.m30(), m.m31(), m.m32(), m.m33()
    ))
    putM(modelView)
    fb.put(floatArrayOf(1f, 1f, 1f, 1f))     // ColorModulator
    fb.put(floatArrayOf(0f, 0f, 0f, 0f))     // ModelOffset (vec3 + padding)
    putM(texMat)                               // TextureMat
    bb.position(160).flip()
    buf.map(0, 160, false, true).use { it.data().put(bb) }
}
```

---

## 6. PIP 注册与生命周期

### 注册方式

通过 Mixin 注入到 `GameRenderer` 构造函数中：

**`GameRendererMixin.java`**:
```java
@Redirect(method = "<init>", at = @At(value = "INVOKE",
    target = "Ljava/util/List;of(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/List;"))
private List<PictureInPictureRenderer<?>> onListOf(Object e1, Object e2, Object e3, Object e4, Object e5) {
    List<PictureInPictureRenderer<?>> list = new ArrayList<>();
    list.add((PictureInPictureRenderer<?>) e1);
    list.add((PictureInPictureRenderer<?>) e2);
    list.add((PictureInPictureRenderer<?>) e3);
    list.add((PictureInPictureRenderer<?>) e4);
    list.add((PictureInPictureRenderer<?>) e5);
    list.add(new PIPRenderer());  // ← 将自己的 PIPRenderer 添加到列表末尾
    return list;
}
```

### 生命周期

```
GameRenderer 初始化
  │
  ▼
Mixin 注入 → PIPRenderer 添加到 PIP 渲染器列表
  │
  ▼ (每帧)
GuiRenderState 收集 PIP states
  │
  ▼
Minecraft 遍历所有 PIPRenderer:
  ├─ renderToTexture(state) → 离屏渲染
  └─ blitTexture(state, guiRenderState) → 合成回屏幕
  │
  ▼ (游戏关闭)
PIPRenderer.close() → 清理所有 GPU 资源
```

### 资源管理

- **纹理池** (`texPool`): 按 `(w, h)` 键存储 `PerStateTex`，帧间复用
- **投影缓冲池** (`projPool`): 环形缓冲，永不释放
- **动态变换池** (`dynTransPool`): 环形缓冲，永不释放
- **顶点缓冲**: 每帧创建，帧末清理 (`pendingVtxCleanup`)

---

## 7. 各文件位置索引

```
src/main/
├── kotlin/cn/hkim/addon/
│   ├── Hkim.kt                                     # Mod 入口
│   ├── config/clickgui/ClickGUIScreen.kt            # 使用示例 (PIP 调用方)
│   ├── config/settings/
│   │   ├── BooleanSetting.kt                        # 使用示例 (drawCircle, drawRoundedRect)
│   │   └── NumberSetting.kt                         # 使用示例 (drawCircleWithBorder, drawRoundedRect)
│   └── utils/render/
│       ├── CustomRenderPipelines.kt                 # [注册管线]
│       ├── CustomRenderType.kt                      # 3D 世界 RenderType
│       ├── RenderUtils.kt                           # 3D 世界批处理
│       └── pip/
│           ├── PipState.kt                          # [定义 ShapeCommand]
│           ├── PipRenderer.kt                       # [buildDraw + renderToTexture]
│           └── ShapeRenderer.kt                     # [对外 API]
│
├── java/cn/hkim/addon/mixins/
│   └── GameRendererMixin.java                       # [注册 PIPRenderer]
│
└── resources/assets/hkim/shaders/core/
    ├── rounded_rect.vsh                             # [着色器]
    ├── rounded_rect.fsh                             # [着色器]
    ├── circle.vsh                                   # [着色器]
    └── circle.fsh                                   # [着色器]
```

---

> **核心原则**: PIP 系统通过 `ShapeCommand` 密封接口实现扩展开放。新增形状只需增加一个 `data class` 变体、一个着色器对、一个管线、一个 `buildDraw` 重载、一个 `when` 分支和一个 `ShapeRenderer` 扩展函数。其余（离屏纹理管理、blit 合成、资源池）均由框架自动处理。