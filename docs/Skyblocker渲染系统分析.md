# Skyblocker 渲染系统分析

> 生成日期: 2026-06-05  
> 基于 Minecraft 26.2-snapshot-7 (Fabric) 的渲染管线 API

---

## 目录

1. [架构总览](#1-架构总览)
2. [生命周期与事件处理](#2-生命周期与事件处理)
3. [核心类 Renderer (批处理系统)](#3-核心类-renderer-批处理系统)
4. [自定义管线定义](#4-自定义管线定义)
5. [自定义顶点格式与绑定组布局](#5-自定义顶点格式与绑定组布局)
6. [Primitive 收集器 (PrimitiveCollectorImpl)](#6-primitive-收集器-primitivecollectorimpl)
7. [Primitive 渲染器详解](#7-primitive-渲染器详解)
   - [7.1 FilledBoxRenderer](#71-filledboxrenderer)
   - [7.2 FilledBoxInstancedRenderer](#72-filledboxinstancedrenderer)
   - [7.3 OutlinedBoxRenderer](#73-outlinedboxrenderer)
   - [7.4 OutlinedBoxInstancedRenderer](#74-outlinedboxinstancedrenderer)
   - [7.5 LinesRenderer](#75-linesrenderer)
   - [7.6 QuadRenderer](#76-quadrenderer)
   - [7.7 TexturedQuadRenderer](#77-texturedquadrenderer)
   - [7.8 TextPrimitiveRenderer](#78-textprimitiverenderer)
   - [7.9 CylinderRenderer](#79-cylinderrenderer)
   - [7.10 FilledCircleRenderer](#710-filledcirclerenderer)
   - [7.11 OutlinedCircleRenderer](#711-outlinedcirclerenderer)
   - [7.12 SphereRenderer](#712-sphererenderer)
   - [7.13 CursorLineRenderer](#713-cursorlinerenderer)
   - [7.14 BlockHologramRenderer](#714-blockhologramrenderer)
8. [Instanced 渲染实现](#8-instanced-渲染实现)
   - [8.1 AbstractUniformTexelBuffer](#81-abstractuniformtexelbuffer)
   - [8.2 BoxDataUniform](#82-boxdatauniform)
   - [8.3 Instanced Shader 分析](#83-instanced-shader-分析)
9. [着色器分析](#9-着色器分析)
   - [9.1 filled_box.vsh](#91-filledboxvsh)
   - [9.2 outlined_box.vsh](#92-outlinedboxvsh)
   - [9.3 box_blur.fsh](#93-boxblurfsh)
10. [Glow 系统](#10-glow-系统)
11. [矩阵变换策略](#11-矩阵变换策略)
12. [与 HkimV 的对比改进建议](#12-与-hkimv-的对比改进建议)

---

## 1. 架构总览

Skyblocker 的渲染系统分为三层:

```
┌─────────────────────────────────────────────────────────────┐
│  第一层: API 层 (LevelRenderExtractionCallback)               │
│  各功能模块通过实现 Renderable 接口/注册回调提交原始数据        │
├─────────────────────────────────────────────────────────────┤
│  第二层: 收集层 (PrimitiveCollectorImpl)                      │
│  将原始数据转为 RenderState, 进行视锥体裁剪, 按类型分桶存储   │
├─────────────────────────────────────────────────────────────┤
│  第三层: 渲染层 (Renderer + PrimitiveRenderer)                │
│  PrimitiveRenderer 将 State → 顶点数据写入 BufferBuilder      │
│  Renderer 管理批处理、分配 GPU 缓冲、执行 drawIndexed          │
└─────────────────────────────────────────────────────────────┘
```

**文件位置**: `src/main/java/de/hysky/skyblocker/utils/render/`

### 包结构

| 包 | 内容 |
|---|---|
| `utils/render/` | 核心基础设施: Renderer, RenderHelper, SkyblockerRenderPipelines, 自定义格式 |
| `utils/render/primitive/` | 各种 PrimitiveRenderer 实现 |
| `utils/render/state/` | 各种 RenderState 数据类 |
| `utils/render/texture/` | 纹理管理工具 |
| `utils/render/title/` | Title 渲染系统(用于标题显示) |

---

## 2. 生命周期与事件处理

### 2.1 事件注册 (`RenderHelper.java`)

```java
@Init
public static void init() {
    // Phase 1: 收集阶段 (END_EXTRACTION)
    LevelExtractionEvents.END_EXTRACTION.register(RenderHelper::startExtraction);
    
    // Phase 2: 提交 Vanilla 渲染 (COLLECT_SUBMITS)
    LevelRenderEvents.COLLECT_SUBMITS.register(RenderHelper::submitVanillaSubmittables);
    
    // Phase 3: 执行绘制 (END_MAIN)
    LevelRenderEvents.END_MAIN.register(RenderHelper::executeDraws);
}
```

### 2.2 三阶段流程

```
时间线: EXTRACTION → COLLECT_SUBMITS → END_MAIN

Phase 1 (END_EXTRACTION):
  ├─ 创建 PrimitiveCollectorImpl(levelState, frustum)
  ├─ 调用 LevelRenderExtractionCallback.EVENT.invoker().onExtract(collector)
  │   └─ 各功能模块通过 collector.submitXxx() 提交原始数据
  ├─ collector.endCollection() → 冻结收集器
  └─ 此时 Primitives 按类型存储在 collector 的 List 中

Phase 2 (COLLECT_SUBMITS):
  └─ collector.dispatchVanillaSubmittables()
      └─ 提交 Vanilla 风格的渲染(如信标光束)到 Minecraft 主渲染队列

Phase 3 (END_MAIN):
  ├─ collector.dispatchPrimitivesToRenderers(cameraState)
  │   └─ 将 state 列表分发给对应的 PrimitiveRenderer
  │       └─ PrimitiveRenderer.submitPrimitives() 写入 vertex 数据到 BufferBuilder
  ├─ Renderer.executeDraws()
  │   ├─ endBatches() → 结束所有批次, buildOrThrow() 生成 MeshData
  │   ├─ setupDraws() → 分配/更新 GPU 缓冲区, 记录 Draw 命令
  │   └─ dispatchDraws() → 创建 RenderPass, 执行所有 drawIndexed
  └─ 清理
```

### 2.3 LevelRenderExtractionCallback

用于各功能模块向渲染系统提交数据的 Fabric Event:

```java
public interface LevelRenderExtractionCallback {
    Event<LevelRenderExtractionCallback> EVENT = EventFactory.createArrayBacked(
        LevelRenderExtractionCallback.class,
        callbacks -> collector -> {
            for (LevelRenderExtractionCallback callback : callbacks) {
                callback.onExtract(collector);
            }
        }
    );
    void onExtract(PrimitiveCollector collector);
}
```

使用方法:
```java
LevelRenderExtractionCallback.EVENT.register(collector -> {
    collector.submitFilledBox(box, colour, alpha, throughWalls);
    collector.submitText(text, pos, throughWalls);
});
```

---

## 3. 核心类 Renderer (批处理系统)

`Renderer.java` 是整个渲染系统的核心, 管理缓冲区分配、批处理和 GPU 提交。

### 3.1 关键数据结构

```java
// 每个 (pipeline, textureSetup, alpha) 三元组对应一个 BatchedDraw
private static final Int2ObjectMap<BatchedDraw> BATCHED_DRAWS = new Int2ObjectArrayMap<>(5);

// 批量分配的 allocators, 避免每帧新建 ByteBufferBuilder
private static final Int2ObjectMap<ByteBufferBuilder> ALLOCATORS = new Int2ObjectArrayMap<>(5);

// 所有 VertexFormat 对应的共享环形缓冲区 (MappableRingBuffer)
private static final Map<VertexFormat, MappableRingBuffer> VERTEX_BUFFERS = new Object2ObjectOpenHashMap<>();

// 准备执行的绘制命令
private static final List<PreparedDraw> PREPARED_DRAWS = new ArrayList<>();

// 最终提交到 GPU 的 Draw 列表
private static final List<Draw> DRAWS = new ArrayList<>();
```

### 3.2 批处理机制

#### BatchedDraw — 同一管线/纹理/alpha 的缓存

```java
private record BatchedDraw(
    BufferBuilder bufferBuilder,
    int instanceCount,
    RenderPipeline pipeline,
    TextureSetup textureSetup,
    float alphaMultiplier,
    @Nullable UniformBinding uniform
) {}
```

- key = `hash(pipeline, textureSetup, alphaMultiplier)` → 一个 32 位 int
- 同 key 的所有顶点追加到同一个 BufferBuilder
- 这样不同模块提交的同管线/纹理/alpha 数据会自动合并为一次 drawcall

#### 排除批处理

某些管线(如 TRIANGLE_STRIP/TRIANGLE_FAN 拓扑)需要排除批处理:

```java
Renderer.excludePipelineFromBatching(CYLINDER);   // TRIANGLE_STRIP
Renderer.excludePipelineFromBatching(CIRCLE);      // TRIANGLE_FAN
Renderer.excludePipelineFromBatching(LINES_THROUGH_WALLS);
Renderer.excludePipelineFromBatching(RenderPipelines.LINES);
```

#### getBuffer() 分发逻辑

```java
public static BufferBuilder getBuffer(RenderPipeline pipeline, TextureSetup textureSetup, float alphaMultiplier) {
    if (!EXCLUDED_FROM_BATCHING.contains(pipeline)) {
        return setupBatched(pipeline, textureSetup, alphaMultiplier);
    } else {
        return setupUnbatched(pipeline, textureSetup, alphaMultiplier, 1, null);
    }
}
```

额外重载版本用于 Instance 渲染:
```java
public static BufferBuilder getBuffer(RenderPipeline pipeline, TextureSetup textureSetup,
    float alphaMultiplier, int instanceCount, @Nullable UniformBinding uniform) {
    return setupUnbatched(pipeline, textureSetup, alphaMultiplier, instanceCount, uniform);
}
```

### 3.3 executeDraws() 执行流程

```
executeDraws()
├── endBatches()
│   ├── 遍历 BATCHED_DRAWS, 对每个调用 prepareBatchedDraw()
│   └── prepareBatchedDraw(): 调用 bufferBuilder.buildOrThrow() 生成 MeshData
│                            → 创建 PreparedDraw(builtBuffer, instanceCount, ...)
│
├── setupDraws()
│   ├── setupVertexBuffers()
│   │   └── 按 VertexFormat 统计总顶点数, 分配/调整 MappableRingBuffer 大小
│   ├── 遍历 PreparedDraws:
│   │   ├── 将 vertexData copy 到共享 VERTEX_BUFFERS 中对应的 GpuBuffer
│   │   ├── 记录 buffer 偏移量(baseVertex)
│   │   └── 创建 Draw(vertexBuffer, baseVertex, indexCount, ...)
│   └── builtBuffer.close() 释放 MeshData
│
├── dispatchDraws()
│   ├── applyViewOffsetZLayering()  -- 投影分层偏移
│   ├── 遍历 DRAWS, 对每个调用 draw()
│   └── unapplyViewOffsetZLayering()
│
└── buffer rotation → 避免 GPU 同步等待
```

### 3.4 draw() 方法 — 最终 GPU 提交

```java
private static void draw(Draw draw) {
    // 获取线段的自动索引缓冲区 (LINES/LINE_STRIP 拓扑的索引是自动生成的)
    AutoStorageIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(draw.pipeline().getPrimitiveTopology());
    GpuBuffer indices = shapeIndexBuffer.getBuffer(draw.indexCount());
    IndexType indexType = shapeIndexBuffer.type();

    try (RenderPass renderPass = RenderSystem.getDevice()
            .createCommandEncoder()
            .createRenderPass(() -> "skyblocker world rendering",
                getMainColorTexture(), Optional.empty(),
                getMainDepthTexture(), OptionalDouble.empty())) {
        
        renderPass.setPipeline(draw.pipeline);
        RenderSystem.bindDefaultUniforms(renderPass);
        
        // 写入 DynamicTransforms (ModelView, ColorModulator, ModelOffset, TextureMatrix)
        renderPass.setUniform("DynamicTransforms", setupDynamicTransforms(draw.alphaMultiplier));
        
        // 可选的自定义 Uniform
        if (draw.uniform != null) {
            renderPass.setUniform(draw.uniform.name, draw.uniform.buffer);
        }
        
        // 纹理绑定
        if (draw.textureSetup.texure0() != null)
            renderPass.bindTexture("Sampler0", ...);
        if (draw.textureSetup.texure2() != null)
            renderPass.bindTexture("Sampler2", ...);
        
        renderPass.setVertexBuffer(0, draw.vertices.slice());
        renderPass.setIndexBuffer(indices, indexType);
        
        // 最终绘制调用
        renderPass.drawIndexed(draw.indexCount, draw.instanceCount, 0, draw.baseVertex, 0);
    }
}
```

**关键点**:
- 使用 `RenderSystem.getSequentialBuffer()` 获取拓扑对应的自动索引(适用于 LINES/QUADS 等非显式索引拓扑)
- `drawIndexed` 参数顺序: `(indexCount, instanceCount, firstIndex, baseVertex, firstInstance)`
- 使用 `RenderSystem.getModelViewMatrixCopy()` 获取视图矩阵
- DynamicTransforms 写入: `.writeTransform(modelView, colorModulator)`

### 3.5 缓冲区复用与旋转

使用 `MappableRingBuffer` 实现三重缓冲:

```java
// Renderer.executeDraws() 末尾:
for (MappableRingBuffer buffer : VERTEX_BUFFERS.values()) {
    buffer.rotate();
}
```

- 大小不足时重新分配, 足够时仅旋转到下一块
- 避免每帧创建/销毁 GPU 缓冲区
- 防止 GPU 仍在读取时 CPU 写入同一块内存

---

## 4. 自定义管线定义

`SkyblockerRenderPipelines.java` 使用 `RenderPipelines.register()` 注册自定义管线。

### 4.1 管线构建模式

Skyblocker 大量使用"片段构建"模式:

```java
// 1. 从现有 snippet 克隆并覆盖部分属性
RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
    .withLocation(SkyblockerMod.id("pipeline/..."))
    .withVertexShader(SkyblockerMod.id("core/filled_box"))
    .withBindGroupLayout(SkyblockerBindGroupLayouts.BOX_DATA)
    .withVertexBinding(0, DefaultVertexFormat.POSITION)
    .withPrimitiveTopology(PrimitiveTopology.QUADS)
    .withCull(false)
    .build();

// 2. 仅覆盖深度状态 (快速创建 through-walls 变体)
RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
    .withLocation(SkyblockerMod.id("pipeline/lines_through_walls"))
    .withDepthStencilState(Optional.empty())  // ← 不写入深度, 不测试 = 穿墙
    .build();
```

### 4.2 完整管线清单

| 管线名称 | 拓扑 | 顶点格式 | 深度测试 | 用途 |
|---|---|---|---|---|
| `FILLED_INSTANCED` | QUADS | POSITION | 默认(>=) | 实例化填充盒 |
| `FILLED_THROUGH_WALLS_INSTANCED` | QUADS | POSITION | 不测试 | 实例化穿墙填充盒 |
| `FILLED_THROUGH_WALLS` | QUADS | POSITION_COLOR | 不测试 | 非实例化穿墙填充盒 |
| `OUTLINED_BOX_INSTANCED` | LINES | POSITION_NORMAL | 默认(>=) | 实例化线框盒 |
| `OUTLINED_BOX_THROUGH_WALLS_INSTANCED` | LINES | POSITION_NORMAL | 不测试 | 实例化穿墙线框盒 |
| `LINES_THROUGH_WALLS` | LINES | POSITION_COLOR_NORMAL | 不测试 | 穿墙线条 |
| `QUADS_THROUGH_WALLS` | QUADS | POSITION_COLOR | 不测试 | 穿墙四边形 |
| `TEXTURE` | QUADS | POSITION_COLOR_TEX_LIGHTMAP | >= | 3D 纹理 |
| `TEXTURE_THROUGH_WALLS` | QUADS | POSITION_COLOR_TEX_LIGHTMAP | 不测试 | 穿墙 3D 纹理 |
| `CYLINDER` | TRIANGLE_STRIP | POSITION_COLOR | 默认(>=) | 圆柱体 |
| `CIRCLE` | TRIANGLE_FAN | POSITION_COLOR | 默认(>=) | 圆形填充 |
| `CIRCLE_LINES` | QUADS | POSITION_COLOR | 默认(>=) | 环形线框 |
| `BLURRED_RECTANGLE` | QUADS | POSITION_COLOR | 默认(>=) | 模糊矩形(UI) |
| `OUTLINE_DEPTH_CULL` | LINES | (继承 OUTLINE) | >=, 背面剔除 | 实体轮廓线 |
| `OUTLINE_DEPTH_NO_CULL` | LINES | (继承 OUTLINE) | >=, 无双面 | 实体轮廓线 |

### 4.3 深度状态配置

```java
// 默认深度测试 (GREATER_THAN_OR_EQUAL, 可写)
new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false)
// 等价于默认 — 管线不显式设置时自动使用

// 穿透深度 (不测试, 不写入) — 用于穿墙效果
Optional.empty()

// 开启深度写入
new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true)
```

### 4.4 预编译

```java
@Init
public static void init() {
    // 排除批处理
    Renderer.excludePipelineFromBatching(CYLINDER);
    Renderer.excludePipelineFromBatching(CIRCLE);
    Renderer.excludePipelineFromBatching(LINES_THROUGH_WALLS);
    Renderer.excludePipelineFromBatching(RenderPipelines.LINES);
    
    // Iris 兼容
    IrisCompatibility.assignPipelines();
}
```

管线在首次使用时自动编译, `init()` 仅确保在 Mod 初始化时触发。

---

## 5. 自定义顶点格式与绑定组布局

### 5.1 顶点格式 (SkyblockerVertexFormats)

```java
public static final VertexFormat POSITION_NORMAL = VertexFormat.builder(0)
    .addAttribute(DefaultVertexFormat.POSITION_SEMANTIC_NAME, DefaultVertexFormat.POSITION_FORMAT)
    .addAttribute(DefaultVertexFormat.NORMAL_SEMANTIC_NAME, DefaultVertexFormat.NORMAL_FORMAT)
    .build();
```

用于 Instanced 线框盒: 顶点只需 Position + Normal (颜色和线宽传递到 uniform 中)。

### 5.2 绑定组布局 (SkyblockerBindGroupLayouts)

```java
public static final BindGroupLayout BOX_DATA = BindGroupLayout.builder()
    .withUniform("BoxData", UniformType.TEXEL_BUFFER, GpuFormat.RGBA32_FLOAT)
    .build();

public static final BindGroupLayout OUTLINED_BOX_DATA = BindGroupLayout.builder()
    .withUniform("OutlinedBoxData", UniformType.TEXEL_BUFFER, GpuFormat.RGBA32_FLOAT)
    .build();
```

- 使用 `TEXEL_BUFFER` (纹理缓冲) 类型, 相当于 GPU 可以随机访问的 buffer
- 每个 Texel 4 × 32 位浮点 (RGBA32_FLOAT) = 16 字节
- 每个 box 实例占用 2 个 texel (32 字节)

---

## 6. Primitive 收集器 (PrimitiveCollectorImpl)

### 6.1 收集阶段

`PrimitiveCollectorImpl` 在 `END_EXTRACTION` 阶段创建, 接收所有模块的提交请求:

```java
public PrimitiveCollectorImpl(LevelRenderState worldState, Frustum frustum) {
    // 根据后端类型决定是否启用 Instanced 渲染
    boolean isVulkan = ((GpuDeviceAccessor) RenderSystem.getDevice())
        .getBackend() instanceof VulkanDevice;
    this.useInstancing = isVulkan || Debug.debugEnabled();
    ...
}
```

**注意**: Instanced 渲染仅在 Vulkan 后端或 Debug 模式下启用。

### 6.2 提交方法

每个 submit 方法执行:
1. `ensureNotFrozen()` — 确保收集阶段未结束
2. 视锥体裁剪 (部分方法)
3. 创建对应的 RenderState 对象, 填充字段
4. 添加到对应的 List 中

### 6.3 RenderState 列表

| 字段 | 类型 | 对应 submit 方法 |
|---|---|---|
| `vanillaSubmittables` | `List<VanillaSubmittable>` | `submitVanilla()` |
| `filledBoxStates` | `List<FilledBoxRenderState>` | `submitFilledBox()` |
| `outlinedBoxStates` | `List<OutlinedBoxRenderState>` | `submitOutlinedBox()` |
| `linesStates` | `List<LinesRenderState>` | `submitLinesFromPoints()` |
| `cursorLineStates` | `List<CursorLineRenderState>` | `submitLineFromCursor()` |
| `quadStates` | `List<QuadRenderState>` | `submitQuad()` |
| `texturedQuadStates` | `List<TexturedQuadRenderState>` | `submitTexturedQuad()` |
| `blockHologramStates` | `List<BlockHologramRenderState>` | `submitBlockHologram()` (已禁用) |
| `textStates` | `List<TextRenderState>` | `submitText()` |
| `cylinderStates` | `List<CylinderRenderState>` | `submitCylinder()` |
| `filledCircleStates` | `List<FilledCircleRenderState>` | `submitFilledCircle()` |
| `sphereStates` | `List<SphereRenderState>` | `submitSphere()` |
| `outlinedCircleStates` | `List<OutlinedCircleRenderState>` | `submitOutlinedCircle()` |

### 6.4 dispatchPrimitivesToRenderers() 分发

在 `END_MAIN` 阶段执行:

```java
public void dispatchPrimitivesToRenderers(CameraRenderState cameraState) {
    // 非实例化: 逐个 state 调用 submitPrimitives(state, cameraState)
    for (FilledBoxRenderState state : this.filledBoxStates) {
        FilledBoxRenderer.INSTANCE.submitPrimitives(state, cameraState);
    }
    
    // 实例化: 传入整个 List
    if (this.useInstancing) {
        FilledBoxInstancedRenderer.INSTANCE.submitPrimitives(this.filledBoxStates, cameraState);
    }
    
    // ... 其他类型类似
}
```

---

## 7. Primitive 渲染器详解

所有 PrimitiveRenderer 遵循同一模式:
1. 从 `Renderer.getBuffer(pipeline, ...)` 获取 BufferBuilder
2. 构建位置矩阵 `positionMatrix = translate(-cameraPos)`
3. 写入顶点数据
4. 设置颜色/法线/UV 等属性

### 7.1 FilledBoxRenderer

**文件**: `primitive/FilledBoxRenderer.java`  
**管线**: `FILLED_THROUGH_WALLS` (穿墙) 或 `DEBUG_FILLED_BOX` (vanilla)  
**拓扑**: TRIANGLES (由 vanilla DEBUG_FILLED_SNIPPET 定义)

位置矩阵:
```java
Matrix4f positionMatrix = new Matrix4f()
    .translate((float) -cameraState.pos.x, (float) -cameraState.pos.y, (float) -cameraState.pos.z);
```

Box 绘制: 6 个面 × 4 顶点 (QUADS 拓扑自动拆三角):
```java
// Front face (Z+)
buffer.addVertex(positionMatrix, minX, minY, maxZ).setColor(r, g, b, a);
buffer.addVertex(positionMatrix, maxX, minY, maxZ).setColor(r, g, b, a);
buffer.addVertex(positionMatrix, maxX, maxY, maxZ).setColor(r, g, b, a);
buffer.addVertex(positionMatrix, minX, maxY, maxZ).setColor(r, g, b, a);
// ... 其余 5 个面
```

**关键区别与 HkimV**: Skyblocker 使用 `addVertex(Matrix4f, ...)` 带位置矩阵的重载, 而 HkimV 使用 `addVertex(float, float, float)` 然后通过 DynamicTransforms 的 modelView 矩阵变换。两种方式各有利弊:
- 带矩阵: 在 CPU 端完成坐标变换, 减少 GPU 工作量
- 用 DynamicTransforms: GPU 端统一变换, 更灵活

### 7.2 FilledBoxInstancedRenderer

**文件**: `primitive/FilledBoxInstancedRenderer.java`  
**管线**: `FILLED_INSTANCED` / `FILLED_THROUGH_WALLS_INSTANCED`  
**拓扑**: QUADS

核心思路:
1. 按 `throughWalls` 分组
2. 为每组构建 `BoxDataUniform` (存储所有 box 的世界坐标和颜色)
3. 作为 UniformBinding 传递给管线
4. 提交一个单位立方体 (0-1 范围的 6 个面)
5. 着色器中使用 `gl_InstanceID` 从 BoxData 中读取实际变换

```java
public void submitPrimitives(List<FilledBoxRenderState> states, CameraRenderState cameraState) {
    // 分组
    List<FilledBoxRenderState> normalStates = ...;
    List<FilledBoxRenderState> throughWallsStates = ...;
    
    // 构建 Uniform + 单位立方体
    Renderer.UniformBinding uniform = new Renderer.UniformBinding(
        "BoxData", this.normalBoxData.update(normalStates, cameraState));
    BufferBuilder buffer = Renderer.getBuffer(
        FILLED_INSTANCED, TextureSetup.noTexture(), 1f, 
        normalStates.size(),  // ← instanceCount
        uniform);
    
    buildUnitBox(0, 0, 0, 1, 1, 1, buffer);  // 只提交一次
}
```

单位立方体的顶点坐标是 0-1, 着色器用 `mix(boxMin, boxMax, Position)` 映射到实际位置。

### 7.3 OutlinedBoxRenderer

**文件**: `primitive/OutlinedBoxRenderer.java`  
**管线**: `LINES_THROUGH_WALLS` (穿墙) 或 `LINES` (vanilla)  
**拓扑**: LINES

绘制 12 条边 × 2 端点 = 24 个顶点。每条边需要正确设置法线方向以控制线宽在屏幕空间的扩展方向:

```java
buffer.addVertex(positionMatrix, minX, minY, minZ).setColor(r, g, b, a).setNormal(1, 0, 0).setLineWidth(w);
buffer.addVertex(positionMatrix, maxX, minY, minZ).setColor(r, g, b, a).setNormal(1, 0, 0).setLineWidth(w);
// ... 每条边用不同的法线方向
```

### 7.4 OutlinedBoxInstancedRenderer

**文件**: `primitive/OutlinedBoxInstancedRenderer.java`  
**管线**: `OUTLINED_BOX_INSTANCED` / `OUTLINED_BOX_THROUGH_WALLS_INSTANCED`  
**拓扑**: LINES

与 FilledBoxInstancedRenderer 类似, 使用 `OutlinedBoxDataUniform` 传递 box 数据 + 单位立方体 + Instance 着色器。

### 7.5 LinesRenderer

**文件**: `primitive/LinesRenderer.java`  
**管线**: `LINES_THROUGH_WALLS` (穿墙) 或 `LINES` (vanilla)

连续线段绘制, 需要为每个顶点计算法线来控制线宽:

```java
for (int i = 0; i < points.length; i++) {
    Vec3 nextPoint = points[i + 1 == points.length ? i - 1 : i + 1];
    Vector3f normalVec = nextPoint.toVector3f()
        .sub((float) points[i].x(), (float) points[i].y(), (float) points[i].z())
        .normalize();
    if (i + 1 == points.length) normalVec.negate(); // 最后一个点法线取反
    
    buffer.addVertex(positionMatrix, ...)
        .setColor(...)
        .setNormal(normalVec.x(), normalVec.y(), normalVec.z())
        .setLineWidth(state.lineWidth);
}
```

### 7.6 QuadRenderer

**文件**: `primitive/QuadRenderer.java`  
**管线**: `QUADS_THROUGH_WALLS` (穿墙) 或 `DEBUG_QUADS`

简单的 4 顶点四边形:
```java
for (int i = 0; i < 4; i++) {
    buffer.addVertex(positionMatrix, ...).setColor(...);
}
```

### 7.7 TexturedQuadRenderer

**文件**: `primitive/TexturedQuadRenderer.java`  
**管线**: `TEXTURE` / `TEXTURE_THROUGH_WALLS`  
**纹理**: 绑定到 Sampler0 和 Sampler2 (光照贴图)

带纹理的面向玩家四边形 (类似命名牌效果):

```java
TextureSetup textureSetup = TextureSetup.singleTexture(textureView, sampler);
BufferBuilder buffer = Renderer.getBuffer(pipeline, textureSetup);

Matrix4f positionMatrix = new Matrix4f()
    .translate(pos.x - cameraPos.x, pos.y - cameraPos.y, pos.z - cameraPos.z)
    .rotate(cameraState.orientation);  // ← 面向玩家

buffer.addVertex(positionMatrix, renderOffset.x, renderOffset.y, renderOffset.z)
    .setUv(1, 1 - textureHeight).setColor(r, g, b, a);
// ... 3 more vertices with UV coords
```

### 7.8 TextPrimitiveRenderer

**文件**: `primitive/TextPrimitiveRenderer.java`  
**管线**: `TEXT_SEE_THROUGH` / `TEXT` / `TEXT_GRAYSCALE_SEE_THROUGH` / `TEXT_GRAYSCALE`

3D 文字渲染的核心, 使用 `Font.PreparedText` (预格式化的字形数据):

```java
public void submitPrimitives(TextRenderState state, CameraRenderState cameraState) {
    Matrix4f positionMatrix = new Matrix4f()
        .translate(pos.x - cameraPos.x, pos.y - cameraPos.y, pos.z - cameraPos.z)
        .rotate(cameraState.orientation)    // 面向相机
        .scale(scale, -scale, scale);       // 自适应缩放
    
    state.glyphs.visit(new Font.GlyphVisitor() {
        public void acceptGlyph(TextRenderable.Styled glyph) {
            TextureSetup setup = TextureSetup.singleTextureWithLightmap(
                glyph.textureView(), sampler);
            BufferBuilder buffer = Renderer.getBuffer(pipeline, setup);
            glyph.render(positionMatrix, buffer, FULL_BRIGHT, false);
        }
    });
}
```

**关键点**:
- 每个字形 (glyph) 调用一次 `Renderer.getBuffer()` − 因为有不同纹理
- 需要 `TextureSetup.singleTextureWithLightmap()` 绑定字形纹理 + 光照贴图
- `FULL_BRIGHT` 使用 `LightCoordsUtil.FULL_BRIGHT` 让文字始终全亮

### 7.9 CylinderRenderer

**文件**: `primitive/CylinderRenderer.java`  
**管线**: `CYLINDER` (TRIANGLE_STRIP, POSITION_COLOR)

```java
for (int i = 0; i <= state.segments; i++) {
    double angle = TAU * i / segments;
    float dx = cos(angle) * radius;
    float dz = sin(angle) * radius;
    
    // TRIANGLE_STRIP 交替写入顶和底
    buffer.addVertex(positionMatrix, cx + dx, cy + halfHeight, cz + dz).setColor(colour);
    buffer.addVertex(positionMatrix, cx + dx, cy - halfHeight, cz + dz).setColor(colour);
}
```

### 7.10 FilledCircleRenderer

**文件**: `primitive/FilledCircleRenderer.java`  
**管线**: `CIRCLE` (TRIANGLE_FAN, POSITION_COLOR)

```java
for (int i = 0; i <= state.segments; i++) {
    double angle = TAU * i / segments;
    float dx = cos(angle) * radius;
    float dz = sin(angle) * radius;
    buffer.addVertex(positionMatrix, cx + dx, cy, cz + dz).setColor(colour);
}
```

TRIANGLE_FAN 自动以第一个顶点为圆心, 每个后续顶点与前一个及圆心构成三角形。

### 7.11 OutlinedCircleRenderer

**文件**: `primitive/OutlinedCircleRenderer.java`  
**管线**: `CIRCLE_LINES` (QUADS, POSITION_COLOR)

环形填充: 每个 segment 生成一个四边形(由内外半径构成):

```java
for (int i = 0; i < state.segments; i++) {
    // 四个顶点构成一个梯形 × 2 三角形
    buffer.addVertex(positionMatrix, cx + x1Inner, cy, cz + z1Inner).setColor(colour);
    buffer.addVertex(positionMatrix, cx + x1Outer, cy, cz + z1Outer).setColor(colour);
    buffer.addVertex(positionMatrix, cx + x2Outer, cy, cz + z2Outer).setColor(colour);
    buffer.addVertex(positionMatrix, cx + x2Inner, cy, cz + z2Inner).setColor(colour);
}
```

### 7.12 SphereRenderer

**文件**: `primitive/SphereRenderer.java`  
**管线**: `CYLINDER` (TRIANGLE_STRIP) — 复用圆柱体管线

球体三角网格: 纬度环 × 经度段:

```java
for (int lat = 0; lat < rings; lat++) {
    double lat0 = PI * lat / rings, lat1 = PI * (lat+1) / rings;
    float y0 = cos(lat0) * radius, y1 = cos(lat1) * radius;
    float r0 = sin(lat0) * radius, r1 = sin(lat1) * radius;
    
    for (int lon = 0; lon <= segments; lon++) {
        double angle = TAU * lon / segments;
        float x0 = cos(angle), z0 = sin(angle);
        
        // TRIANGLE_STRIP 格式: 交替写入上下纬圈
        buffer.addVertex(positionMatrix, cx + x0*r0, cy + y0, cz + z0*r0).setColor(colour);
        buffer.addVertex(positionMatrix, cx + x0*r1, cy + y1, cz + z0*r1).setColor(colour);
    }
}
```

### 7.13 CursorLineRenderer

**文件**: `primitive/CursorLineRenderer.java`  
**管线**: `LINES_THROUGH_WALLS`

从相机到目标点的射线:
```java
Vec3 cameraPoint = cameraState.pos.add(
    new Vec3(cameraState.orientation.transform(new Vector3f(0, 0, -1))));
// 从相机前一点到目标点画线
```

### 7.14 BlockHologramRenderer

**文件**: `primitive/BlockHologramRenderer.java`  
**状态**: 已禁用 (整个方法体被注释)

原本用于在 3D 世界中渲染方块全息预览, 使用 Fabric Renderer API 的 QuadEmitter。目前等待 API 稳定。

---

## 8. Instanced 渲染实现

### 8.1 AbstractUniformTexelBuffer

**文件**: `AbstractUniformTexelBuffer.java`

用于高效传递 instanced 数据的抽象基类:

```java
public abstract class AbstractUniformTexelBuffer<T> implements AutoCloseable {
    private final int instanceBytes;          // 每个实例的字节数
    private MappableRingBuffer buffer;        // 环形 GPU 缓冲
    
    // 写数据到 GPU buffer
    public final GpuBuffer update(List<T> states, CameraRenderState cameraRenderState) {
        prepareBuffer(states.size() * instanceBytes);
        GpuBuffer texelBuffer = this.buffer.currentBuffer();
        
        try (GpuBufferSlice.MappedView mappedView = texelBuffer.map(false, true)) {
            long address = MemoryUtil.memAddress(mappedView.data());
            MemorySegment buffer = MemorySegment.ofAddress(address).reinterpret(this.buffer.size());
            this.updateBuffer(states, cameraRenderState, buffer);
        }
        
        return texelBuffer;
    }
    
    protected abstract void updateBuffer(List<T> states, CameraRenderState cameraRenderState, MemorySegment buffer);
}
```

特征:
- 使用 `Foreign Memory API (MemorySegment)` 进行 native 内存写入
- 使用 `MappableRingBuffer` 实现环形缓冲区旋转
- `toNativeRgba()` 处理端序问题

### 8.2 BoxDataUniform

每个填充盒实例占用 2 个 texel (32 字节):

```
Texel 0: (minX-camX, minY-camY, minZ-camZ, maxX-camX)
Texel 1: (maxY-camY, maxZ-camZ, packedColour, unused)
```

```java
public class BoxDataUniform extends AbstractUniformTexelBuffer<FilledBoxRenderState> {
    private static final int TEXELS_PER_INSTANCE = 2;
    private static final int BYTES_PER_BOX = (Float.BYTES * 4) * TEXELS_PER_INSTANCE;  // = 32

    protected void updateBuffer(List<FilledBoxRenderState> states, CameraRenderState cameraRenderState, MemorySegment buffer) {
        for (int i = 0; i < states.size(); i++) {
            long offset = i * BYTES_PER_BOX;
            FilledBoxRenderState state = states.get(i);
            int colour = toNativeRgba(ARGB.colorFromFloat(state.alpha, ...));
            
            // 坐标偏移相机位置 (double → float 转换在此处完成)
            buffer.set(JAVA_FLOAT, offset + 0,  (float)(state.minX - cameraRenderState.pos.x));
            buffer.set(JAVA_FLOAT, offset + 4,  (float)(state.minY - cameraRenderState.pos.y));
            buffer.set(JAVA_FLOAT, offset + 8,  (float)(state.minZ - cameraRenderState.pos.z));
            buffer.set(JAVA_FLOAT, offset + 12, (float)(state.maxX - cameraRenderState.pos.x));
            
            buffer.set(JAVA_FLOAT, offset + 16, (float)(state.maxY - cameraRenderState.pos.y));
            buffer.set(JAVA_FLOAT, offset + 20, (float)(state.maxZ - cameraRenderState.pos.z));
            buffer.set(JAVA_INT,   offset + 24, colour);
            buffer.set(JAVA_FLOAT, offset + 28, 0f);
        }
    }
}
```

OutlinedBoxDataUniform 额外在第 4 个 float 存储 `lineWidth`。

### 8.3 Instanced Shader 分析

见下方 [9.1 filled_box.vsh](#91-filledboxvsh)。

---

## 9. 着色器分析

### 9.1 filled_box.vsh

```glsl
#version 330
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

uniform samplerBuffer BoxData;

in vec3 Position;
out vec4 vertexColor;

vec4 unpackColour(uint colour) {
    return vec4(
        float(colour & 0xFFu),
        float((colour >> 8u) & 0xFFu),
        float((colour >> 16u) & 0xFFu),
        float((colour >> 24u) & 0xFFu)
    ) / 255.0;
}

void main() {
    int base = gl_InstanceID * 2;
    
    vec4 data1 = texelFetch(BoxData, base);
    vec4 data2 = texelFetch(BoxData, base + 1);
    
    vec3 boxMin = data1.xyz;
    vec3 boxMax = vec3(data1.w, data2.xy);
    uint colour = floatBitsToUint(data2.z);
    
    // Position 是 0-1 范围的单位立方体
    vec3 worldPos = mix(boxMin, boxMax, Position);
    
    gl_Position = ProjMat * ModelViewMat * vec4(worldPos, 1.0);
    vertexColor = unpackColour(colour);
}
```

**关键设计**:
- `samplerBuffer` 类型 = 可以随机读取的 GPU 纹理缓冲区
- `gl_InstanceID` 索引到每个实例的 2 个 texel
- `mix(boxMin, boxMax, Position)` 将单位立方体映射到实际 AABB
- 颜色打包在 RGBA 32 位整数中, 用 `floatBitsToUint` 恢复
- 使用 `ProjMat` (来自 projection.glsl) 和 `ModelViewMat` (来自 dynamictransforms.glsl)

### 9.2 outlined_box.vsh

```glsl
// ... 同上获取 BoxData ...

// 核心线宽技术: 屏幕空间扩展
// 1. 计算线段端点在屏幕空间的位置
vec4 linePosStart = ProjMat * VIEW_SCALE * ModelViewMat * vec4(worldPos, 1.0);
vec4 linePosEnd = ProjMat * VIEW_SCALE * ModelViewMat * vec4(worldPos + Normal, 1.0);

// 2. 归一化设备坐标
vec3 ndc1 = linePosStart.xyz / linePosStart.w;
vec3 ndc2 = linePosEnd.xyz / linePosEnd.w;

// 3. 计算线段方向在屏幕空间的垂直向量
vec2 lineScreenDirection = normalize((ndc2.xy - ndc1.xy) * ScreenSize);
vec2 lineOffset = vec2(-lineScreenDirection.y, lineScreenDirection.x) * lineWidth / ScreenSize;

// 4. 根据顶点 ID 奇偶偏移
if (gl_VertexID % 2 == 0) {
    gl_Position = vec4((ndc1 + vec3(lineOffset, 0.0)) * linePosStart.w, linePosStart.w);
} else {
    gl_Position = vec4((ndc1 - vec3(lineOffset, 0.0)) * linePosStart.w, linePosStart.w);
}
```

**线宽实现原理**:
1. 法线方向被编码为"厚度方向" — 每个顶点的 Normal 指向线段外
2. `position + Normal` 给出一个虚拟参考点, 用于计算线段在屏幕空间的方向
3. 计算屏幕空间垂直向量, 乘以线宽
4. `gl_VertexID % 2` 决定向哪侧偏移 (形成线段的宽度)

这是 Minecraft 1.20.5+ 后移除了硬件线宽后, 在着色器中模拟线宽的标准做法。

### 9.3 box_blur.fsh

```glsl
uniform sampler2D Sampler0;

void main() {
    vec2 screenUV = gl_FragCoord.xy / ScreenSize;
    vec2 texelSize = 1.0 / ScreenSize;
    
    int blurRadius = int(vertexColor.r * 255.0);  // 模糊半径编码在红色通道中
    
    vec4 colourSum = vec4(0.0);
    float totalWeights = 0.0;
    
    // 步长 2, 利用线性过滤混合跳过像素
    for (float x = -r; x < r; x += 2.0) {
        for (float y = -r; y < r; y += 2.0) {
            vec2 offset = vec2(x + 0.5, y + 0.5) * texelSize;
            colourSum += texture(Sampler0, screenUV + offset);
            totalWeights += 1.0;
        }
    }
    
    fragColor = vec4((colourSum / totalWeights).rgb, 1.0);
}
```

**优化技巧**: 步长 2 配合线性过滤, 每一次采样实际上覆盖了 2 个像素的范围, 采样次数减少 75%。

---

## 10. Glow 系统

### 10.1 实体发光

通过 Mixin 注入到实体验渲染中:
- `EntityRenderDispatcherMixin` — 修改发光渲染行为
- `ItemFeatureRendererMixin` — 物品轮廓发光
- `CustomGlowState` — 接口注入, 存储自定义发光颜色

### 10.2 GlowRenderer

```java
public class GlowRenderer implements AutoCloseable {
    public static final GlowRenderer INSTANCE = new GlowRenderer();
    private @Nullable GpuTexture glowDepthTexture;
    
    // 复制主深度缓冲到发光深度纹理
    public void updateGlowDepthTexDepth() {
        tryUpdateDepthTexture();
        RenderSystem.getDevice().createCommandEncoder()
            .copyTextureToTexture(mainDepth, glowDepthTexture, ...);
    }
}
```

---

## 11. 矩阵变换策略

### 11.1 位置矩阵 vs DynamicTransforms

Skyblocker 使用了**混合策略**:

**非实例化渲染** — CPU 端预转换:
```java
Matrix4f positionMatrix = new Matrix4f()
    .translate((float) -cameraState.pos.x, (float) -cameraState.pos.y, (float) -cameraState.pos.z);

buffer.addVertex(positionMatrix, x, y, z).setColor(r, g, b, a);
```

**实例化渲染** — GPU 端转换:
```glsl
// uniform BoxData 包含相机偏移后的坐标
vec3 worldPos = mix(boxMin, boxMax, Position);
gl_Position = ProjMat * ModelViewMat * vec4(worldPos, 1.0);
```

### 11.2 DynamicTransforms 设置

```java
// 非纹理管线: 仅 modelView + colorModulator
private static GpuBufferSlice setupDynamicTransforms(float alphaMultiplier) {
    return RenderSystem.getDynamicUniforms()
        .writeTransform(RenderSystem.getModelViewMatrixCopy(),
            new Vector4f(1f, 1f, 1f, alphaMultiplier));
}
```

注意: 这里使用了 `writeTransform(Matrix4f modelView, Vector4f colorModulator)` — 只有 2 个参数的版本, 省略了 modelOffset 和 textureMatrix (使用默认值)。

### 11.3 DDK 中的矩阵

在 non-instanced 模式下, modelView 矩阵是单位矩阵(因为坐标已经由 positionMatrix 转换到了相机空间), 而 ProjMat 保持不变。

在 instanced 模式下, modelView 矩阵是 `RenderSystem.getModelViewMatrixCopy()` (完整的视图矩阵), 因为 BoxData 中的坐标是相机偏移后的世界坐标。

---

## 12. 与 HkimV 的对比改进建议

### 12.1 架构差异

| 方面 | Skyblocker | HkimV |
|---|---|---|
| 批处理 | 自动按 (pipeline, texture, alpha) 合并 | 简单逐个提交 |
| 缓冲区管理 | MappableRingBuffer 共享缓冲 | 每帧新建 buffer |
| Instanced | 支持 (需 Vulkan) | 不支持 |
| 视锥体裁剪 | 收集阶段执行 | 未实现 |
| 颜色打包 | GPU 端 unpack (shader) | CPU 端直接写入 |
| 位置矩阵 | 非 instanced 用 CPU 预转换 | 用 DynamicTransforms modelView |
| 文字渲染 | Font.PreparedText + GlyphVisitor | 未实现 |
| 圆/圆柱/球 | Triangle Fan/Strip | 未实现 |
| 模糊矩形 | Box Blur Shader | 未实现 |

### 12.2 可参考优化方向

1. **视锥体裁剪**: 引入 `FrustumUtils.isVisible()` 减少提交
2. **环形缓冲区**: 使用 `MappableRingBuffer` 替代每帧重新创建 GpuBuffer
3. **实例化渲染**: 对于大量同类型盒子, 改用 Instanced 方式
4. **文字管线**: 参考 `TextPrimitiveRenderer` 实现完整的 3D 文字
5. **着色器线宽**: 参考 `outlined_box.vsh` 的屏幕空间线宽算法
6. **排除批处理**: 有些管线需要单独提交(如 TRIANGLE_STRIP)
7. **Uniform 纹理**: 使用 texel buffer 传递 instanced 数据
