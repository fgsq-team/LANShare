#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
用 icon/lanshare.svg 生成 Android APP 图标（多密度 mipmap PNG）。

生成内容:
  1. ic_launcher.png       方形圆角版（红底由圆形替换为圆角矩形）
  2. ic_launcher_round.png 圆形版（直接使用原始 SVG）
  分别输出到 mipmap-mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi（48/72/96/144/192 px）。

依赖（任选其一即可，优先顺序: cairosvg > resvg-py > svglib）:
  pip install pillow cairosvg
  pip install pillow resvg-py        # 无需 Cairo，Windows 下最省事
  pip install pillow svglib reportlab

用法:
  python icon/generate_icons.py
"""

import io
import os
import re
import sys

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(BASE_DIR)
SVG_PATH = os.path.join(BASE_DIR, "lanshare.svg")
RES_DIR = os.path.join(PROJECT_ROOT, "app", "src", "main", "res")

# 各密度图标尺寸（dp 基准 48）
DENSITIES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}

# SVG 中的背景圆 -> 圆角矩形（方形图标用），取圆的包围盒，圆角约为宽度的 22%
BG_CIRCLE_RE = re.compile(r'<circle\s+cx="([\d.]+)"\s+cy="([\d.]+)"\s+r="([\d.]+)"\s+fill="(#[0-9A-Fa-f]{3,8})"\s*/>')


def render_png(svg_str: str, size: int) -> bytes:
    """把 SVG 字符串渲染为 size x size 的 PNG 字节，依次尝试可用的渲染后端。"""
    try:
        import cairosvg
        return cairosvg.svg2png(
            bytestring=svg_str.encode("utf-8"),
            output_width=size, output_height=size,
        )
    except ImportError:
        pass

    try:
        from resvg_py import svg_to_bytes
        return svg_to_bytes(svg_string=svg_str, width=size, height=size)
    except ImportError:
        pass

    try:
        from svglib.svglib import svg2rlg
        from reportlab.graphics import renderPM
        drawing = svg2rlg(io.StringIO(svg_str))
        scale = size / max(drawing.width, drawing.height)
        return renderPM.drawToString(drawing, fmt="PNG", scale=scale, dpi=72)
    except ImportError:
        pass

    sys.exit("未找到 SVG 渲染后端，请先执行: pip install pillow resvg-py（或 cairosvg / svglib+reportlab）")


def make_square_svg(svg: str) -> str:
    """把背景圆替换为同色圆角矩形，得到方形圆角图标版 SVG。"""
    def repl(m):
        cx, cy, r, fill = (float(m.group(1)), float(m.group(2)),
                           float(m.group(3)), m.group(4))
        x, y, d = cx - r, cy - r, 2 * r
        rx = ry = d * 0.22
        return f'<rect x="{x:.1f}" y="{y:.1f}" width="{d:.1f}" height="{d:.1f}" rx="{rx:.1f}" ry="{ry:.1f}" fill="{fill}"/>'

    new_svg, n = BG_CIRCLE_RE.subn(repl, svg, count=1)
    if n == 0:
        print("警告: 未在 SVG 中找到背景圆，方形图标将保留原始构图。")
    return new_svg


def make_round_svg(svg: str) -> str:
    """把背景圆放大到铺满整个画布并居中，渲染结果天然就是干净的圆形（带抗锯齿）。"""
    m = re.search(r'<svg[^>]*\bwidth="([\d.]+)"[^>]*\bheight="([\d.]+)"', svg)
    if not m:
        print("警告: 无法解析 SVG 画布尺寸，圆形图标将保留原始构图。")
        return svg
    w, h = float(m.group(1)), float(m.group(2))
    repl = f'<circle cx="{w / 2:.1f}" cy="{h / 2:.1f}" r="{min(w, h) / 2:.1f}" fill="\\4"/>'
    new_svg, n = BG_CIRCLE_RE.subn(repl, svg, count=1)
    if n == 0:
        print("警告: 未在 SVG 中找到背景圆，圆形图标将保留原始构图。")
    return new_svg


def save_png(png_bytes: bytes, out_path: str):
    """直接保存渲染结果，保留透明通道。"""
    from PIL import Image
    Image.open(io.BytesIO(png_bytes)).convert("RGBA").save(out_path, "PNG")


def main():
    if not os.path.isfile(SVG_PATH):
        sys.exit(f"找不到 SVG 源文件: {SVG_PATH}")
    with open(SVG_PATH, "r", encoding="utf-8") as f:
        svg = f.read()
    square_svg = make_square_svg(svg)
    round_svg = make_round_svg(svg)

    for density, size in DENSITIES.items():
        mipmap = os.path.join(RES_DIR, f"mipmap-{density}")
        os.makedirs(mipmap, exist_ok=True)

        save_png(render_png(square_svg, size),
                 os.path.join(mipmap, "ic_launcher.png"))
        save_png(render_png(round_svg, size),
                 os.path.join(mipmap, "ic_launcher_round.png"))
        print(f"[{density}] {size}px -> mipmap-{density}/ic_launcher.png, ic_launcher_round.png")

    print("\n完成。SVG 源:", SVG_PATH)


if __name__ == "__main__":
    main()
