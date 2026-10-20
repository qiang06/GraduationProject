# 为什么 Capptivo 在 Linux Mint 上“装了却不能用”，以及我是怎么把它救回来的

> 环境：Linux Mint 22.3 / Cinnamon 6.6 / X11 / 2560×1440 / RTX 3050 Laptop
> 软件：Capptivo 1.0.4（AppImage）
> 日期：2026-10-20

先说结论：**不是 Capptivo 坏了，是 Cinnamon 没有它依赖的 `ScreenCast` portal。** 后面又接连踩了 GStreamer 插件、720p 预览代理、导出收尾、悬浮条点击穿透四个坑。全部绕过去之后，它现在处于“能录、能编辑背景、能自动缩放、能导出，但录制按钮仍然不可用”的勉强可用状态。

---

## 1. 我为什么非要让它跑起来

原因很单纯：**我喜欢它的背景编辑。**

OBS 能录，但录出来就是一块屏幕；Capptivo 的 Look 面板可以给画面加渐变/图片/纯色背景、圆角、阴影、模糊、内边距，再配合自动缩放和跟随光标，演示视频看起来会“像产品”，而不是“像远程桌面”。

所以我装了 AppImage。打开之后，界面出来了，悬浮条也出来了，然后——

- 点 **Record**：没反应。
- 手动塞一个工程进去：能打开，但**播放不了**。
- 修好播放：**画面很糊**。
- 修好清晰度：导出**卡在 100%**。
- 想关掉没用的悬浮条：**X 点不动**。

这就是一整套连环坑。

---

## 2. 第一层：Record 按钮为什么是死的

Capptivo 在 Linux 上的采集链路是：

```
Capptivo (Rust/Tauri)
  → xdg-desktop-portal 的 org.freedesktop.portal.ScreenCast
  → PipeWire 视频流
```

它自己不直接抓 X11 屏幕，而是把“录哪个窗口/显示器”交给系统 portal。问题就出在这里。

直接查 D-Bus：

```bash
gdbus introspect --session \
  --dest org.freedesktop.portal.Desktop \
  --object-path /org/freedesktop/portal/desktop
```

列出来的接口里有 `Inhibit`、`Location`、`Notification`、`FileChooser`……**就是没有 `org.freedesktop.portal.ScreenCast`**。

直接调用会得到：

```text
org.freedesktop.DBus.Error.UnknownMethod:
在路径 org.freedesktop.portal.ScreenCast 的对象上没有对应接口
```

再查 Mint 的 portal 后端：

```bash
cat /usr/share/xdg-desktop-portal/portals/xapp.portal
```

`xdg-desktop-portal-xapp 1.1.3` 只实现了：

```text
Wallpaper / Inhibit / Screenshot / Lockdown / Settings / Background
```

没有 ScreenCast。`xdg-desktop-portal-gtk` 也没有，而且它的 `UseIn=gnome`，在 Cinnamon 下根本不是给这个会话用的。

这不是“设置没开”，也不是“换个 Flatpak 权限”能解决的。Linux Mint 的 xapp portal 至今没有实现 ScreenCast，上游 issue 还开着：

- [linuxmint/xdg-desktop-portal-xapp#13](https://github.com/linuxmint/xdg-desktop-portal-xapp/issues/13)

所以 Kooha、浏览器“共享屏幕”、Capptivo 这类走 portal 的录屏工具，在这台机器上是**同一个死因**。

**结论：Record 按钮不可能靠配置修好。** 只有三条路：补 portal 后端（要改 Muffin/portal，root 构建）、换 GNOME 会话、或者绕开它的录制。

我选了第三条。

---

## 3. 第二层：为什么“能打开工程，但播放不了”

既然不能让它录，那就反过来：**我按它的工程格式生成录制结果。**

Capptivo 的项目目录长这样（源码 `ProjectStore` 定义）：

```
<project>/
├── project.json     # 清单：id / title / capture / files / editorState
├── meta.json        # 宽高 / fps / 时长 / 帧数
├── cursor.json      # 光标轨迹（60Hz，归一化坐标）
├── screen.mp4       # 原始录屏
├── thumbnail.jpg    # 封面
└── proxy.mp4        # 预览代理（可选）
```

项目库会扫描这个目录，所以只要文件对，Capptivo 就认。

我用 Python + Xlib + ffmpeg 录了一份，塞进去，编辑器确实打开了。但视频区域是黑的，控制台报：

```text
GStreamer element appsink not found. Please install it.
GStreamer element autoaudiosink not found. Please install it
```

奇怪的是，系统里明明有这些插件：

```bash
gst-inspect-1.0 appsink        # OK
gst-inspect-1.0 autoaudiosink  # OK
```

问题在 AppImage 的打包方式：它带了 `libgstreamer-1.0.so.0`，但**没有带 `gstreamer-1.0` 插件目录**，启动脚本也没有把系统插件路径加进去。结果 GStreamer 插件加载失败，H.264 视频无法解码，自然播不了。

修法很简单——启动时强制指向系统插件：

```bash
export GST_PLUGIN_SYSTEM_PATH=/usr/lib/x86_64-linux-gnu/gstreamer-1.0
export GST_PLUGIN_PATH=/usr/lib/x86_64-linux-gnu/gstreamer-1.0
exec ~/.local/opt/capptivo/Capptivo_1.0.4_amd64.AppImage "$@"
```

把这个包成 `run-capptivo.sh`，桌面入口指向它。播放恢复正常。

---

## 4. 第三层：为什么录出来“好模糊”

能播之后，第一反应是：这也太糊了。

查源码才发现，Capptivo 的编辑器**不是直接播原片**，而是先生成一个预览代理：

```rust
// src-tauri/src/project/proxy.rs
const PROXY_LONG_EDGE: u32 = 1280;
```

也就是说，2560×1440 的录制会被压成 1280 宽的 `proxy.mp4`，只为了让拖动时间线更流畅。导出时才读原始 `screen.mp4`。

编辑器里看到的糊，是 720p 代理被放大到画布上的结果，不是录制糊。

我的修法有点“脏”但有效：**让 `proxy.mp4` 变成指向 `screen.mp4` 的符号链接**。

```bash
ln -sf screen.mp4 proxy.mp4
```

因为 `ensure_proxy` 只检查 `Path::is_file()`，看到链接就直接返回，不会重新转码；编辑器于是拿 1440p 原片当预览，立刻清晰。

同时把录制端参数提了一档：

```bash
-c:v libx264 -preset veryfast -tune stillimage -crf 16
```

`stillimage` 对文字/UI 更友好，CRF 16 比 18 保留更多边缘细节。

> 注意：这是 hack。如果 Capptivo 重新生成 proxy，或者工程被拷贝时丢了链接，预览会回到 720p。

---

## 5. 第四层：为什么导出卡在 100%

这是目前唯一没彻底解决的坑。

导出的文件其实**已经完整写出来了**：

```text
~/桌面/-2026-10-20-19-48.mp4
1.24 MB · 1920×1072 · 3.03s · H.264 · 解码正常
```

但进度层一直停在 `Exporting… Saving… 100%`。

查它的诊断日志：

```text
~/.local/share/com.capptivo.desktop/diagnostics/diagnostics-v1.log
{"code":"native_panic"}
{"code":"native_panic"}
```

原生导出引擎在收尾阶段 panic 了，前端的完成回调没回来，所以 UI 卡住；文件本身是好的。

现在的操作约定：**看到 100% 后等 2–3 秒，确认文件出现在保存目录，直接点 Cancel。** 文件不会删，可以正常用。

另外两个导出限制是源码写死的：

```ts
// src/windows/editor/export/exportSettings.ts
const VIDEO_LONG_EDGE = 1920;   // 导出长边上限 1080p 级
```

- 不能导出 2560×1440 原生；要原生只能用 **Passthrough**（直接拷 `screen.mp4`，但丢背景/缩放/光标）。
- 编码档位 `fast / balanced / quality` 对应 4 / 8 / 16 Mbps；文字演示建议选 **Quality**。

1920×1072 而不是 1080，是因为编码器要求 16 对齐，1080 向下取整到 1072。

---

## 6. 第五层（小坑）：悬浮条点不了

Capptivo 启动会弹一个录制悬浮条。它的 X 按钮点不动——源码里有 `set_ignore_cursor_events` 的逻辑，在这套 X11 环境下基本是点击穿透，鼠标事件直接穿到下面的窗口。

试过直接 `windowunmap`：条是没了，但 Tauri 把“最后一个窗口消失”当成退出，进程也走了。

最后改成 `windowminimize`：

```bash
xdotool windowminimize <Capptivo窗口id>
```

条收起，进程和托盘保留。再做一个 `capptivo-edit.sh`，从菜单启动时等窗口出现后自动最小化，眼不见心不烦。

> 系统本身没装 `xdotool`，这里用的是从 Ubuntu 包解出来的二进制；不想折腾的话，用 `wmctrl -r Capptivo -b add,hidden` 也能达到类似效果。

---

## 7. 我最终搭出来的东西

```
capptivo-record (Python)
├── X11 指针采样 60Hz ──────────► cursor.json
└── ffmpeg x11grab 2560×1440@30 ─► screen.mp4
                                      │
                                      ▼
        ~/.local/share/com.capptivo.desktop/projects/<id>/
        ├── project.json  meta.json  cursor.json  thumbnail.jpg
        └── proxy.mp4 -> screen.mp4   （全分辨率预览）
                                      │
                                      ▼
                            Capptivo 编辑器
                     背景 / 圆角 / 阴影 / 自动缩放 / 导出
```

录制的核心命令：

```bash
ffmpeg -f x11grab -draw_mouse 0 -framerate 30 \
  -video_size 2560x1440 -i :0.0+0,0 \
  -c:v libx264 -preset veryfast -tune stillimage -crf 16 \
  -pix_fmt yuv420p -movflags +faststart screen.mp4
```

光标轨迹按 Capptivo 的格式写，只记录“移动/按下/抬起”三种事件，坐标归一化到 0–1：

```json
{
  "captureRect": { "x": 0, "y": 0, "width": 2560, "height": 1440 },
  "scaleFactor": 1.0,
  "samples": [
    { "t": 0.0004, "x": 0.37812, "y": 0.5875, "kind": "move" },
    { "t": 1.8504, "x": 0.46875, "y": 0.48611, "kind": "down" },
    { "t": 2.0004, "x": 0.46875, "y": 0.48611, "kind": "up" }
  ]
}
```

日常用法：

```bash
capptivo-record                 # 3 秒倒计时，回车停止
capptivo-record --seconds 30    # 录 30 秒自动停
capptivo-record --fps 60        # 60 帧
capptivo-record --open          # 录完直接开 Capptivo
```

以及两个启动器：

- `run-capptivo.sh`：GStreamer 插件路径修复，保证能播放。
- `capptivo-edit.sh`：启动后自动最小化悬浮条，只留托盘。

---

## 8. 现在的边界（诚实版）

| 能力 | 状态 |
|---|---|
| Capptivo 自带 Record | ❌ 用不了（缺 ScreenCast portal） |
| 外部录制 → 原生工程 | ✅ `capptivo-record` |
| 编辑器 / 播放 | ✅（GStreamer 修复后） |
| **背景编辑**（渐变/图片/纯色/圆角/阴影/模糊/padding） | ✅ 正常 |
| 自动缩放 / 跟随光标 | ✅ 基于点击与光标轨迹 |
| 预览清晰度 | ✅ 全分辨率（proxy 符号链接 hack） |
| 导出文件 | ✅ 能生成（默认上限 1920 长边） |
| 导出 UI | ⚠️ 卡 100%，点 Cancel 拿文件 |
| 音频 | ❌ 采集器不录麦克风/系统声 |
| 悬浮条 | ✅ 已自动最小化 |
| 彻底修好 Record | 需要补 portal / 改 Muffin，root 级别 |

---

## 9. 如果真要把 Record 按钮修好

- **A. 补 Cinnamon 的 ScreenCast portal**：参考 xdg-desktop-portal-xapp 的 PR #31 思路，用 `org.cinnamon.Muffin.ScreenCast` 做后端。问题是要配套改 Muffin/portal，必须 root 构建，且 Mint 上游没合并。
- **B. 换 GNOME on Xorg 会话**：装 `xdg-desktop-portal-gnome` + `gnome-shell`，Capptivo 直接可用；代价是整个桌面环境换掉。
- **C. 去 Windows/macOS 用 Capptivo**：原生支持，没有 portal 这层。
- **D. 继续用现在的桥接方案**：录制归 ffmpeg，编辑归 Capptivo。

---

## 10. 这次踩坑学到的

1. **AppImage ≠ 自带一切。** 它可能带了库，却没带插件目录；系统集成（GStreamer/WebKit）最容易在这里断。
2. **portal 是 Linux 桌面的隐形地基。** 一个 `ScreenCast` 接口缺失，会同时打死 Kooha、浏览器共享、Capptivo。
3. **原生工程格式是突破口。** 只要能让它认目录，录制器完全可以换掉；`screen.mp4 + cursor.json + meta.json + project.json` 就是事实标准。
4. **诊断顺序很重要**：D-Bus 接口 → 进程日志 → 产物 `ffprobe` → 源码常量，比反复点按钮快得多。
5. **“能用”和“好用”之间，往往只差一个 720p 代理和一个 100% 卡住。**

---

## 附：文件与命令速查

```bash
# 录制
~/.local/bin/capptivo-record
~/.local/bin/capptivo-record --seconds 30

# 编辑（自动收起悬浮条）
~/.local/opt/capptivo/capptivo-edit.sh

# 带 GStreamer 修复的启动器
~/.local/opt/capptivo/run-capptivo.sh

# 工程目录
~/.local/share/com.capptivo.desktop/projects/

# 静态 ffmpeg（带 x11grab）
~/.local/opt/ffmpeg/ffmpeg-master-latest-linux64-gpl/bin/ffmpeg
```

相关链接：

- [Capptivo（GitHub）](https://github.com/SECHAK-AG/capptivo)
- [Linux Mint xdg-desktop-portal-xapp ScreenCast issue](https://github.com/linuxmint/xdg-desktop-portal-xapp/issues/13)
- [xdg-desktop-portal ScreenCast 文档](https://flatpak.github.io/xdg-desktop-portal/docs/doc-org.freedesktop.portal.ScreenCast.html)
