#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""把 app / player / web 中在用的尖锐图标统一重绘为圆润风格。

矢量图标: 重写 res/drawable 下的 vector XML（保留原尺寸/viewport/颜色引用）。
位图图标: 以原尺寸、原配色渲染 SVG 回写 PNG（resvg-py / cairosvg）。
用法: python icon/rounded_icons.py
"""
import math
import os
import re
import sys

BASE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(BASE)
APP_RES = os.path.join(ROOT, "app", "src", "main", "res")
PLAYER_RES = os.path.join(ROOT, "player", "src", "main", "res")
WEB_IMG = os.path.join(ROOT, "app", "src", "main", "assets", "web", "images")


def P(d, fill=None, stroke=None, sw=0, opacity=None, text=None, size=0, tx=12, ty=16.5):
    return dict(d=d, fill=fill, stroke=stroke, sw=sw, opacity=opacity, text=text, size=size, tx=tx, ty=ty)


def rpoly(pts, r):
    """多边形 -> 圆角路径：每个顶点用二次贝塞尔切圆。"""
    n = len(pts)
    d = []
    for i in range(n):
        p0, p1, p2 = pts[(i - 1) % n], pts[i], pts[(i + 1) % n]
        v0 = (p0[0] - p1[0], p0[1] - p1[1])
        v2 = (p2[0] - p1[0], p2[1] - p1[1])
        l0, l2 = math.hypot(*v0), math.hypot(*v2)
        rr = min(r, l0 / 2, l2 / 2)
        a = (p1[0] + v0[0] / l0 * rr, p1[1] + v0[1] / l0 * rr)
        b = (p1[0] + v2[0] / l2 * rr, p1[1] + v2[1] / l2 * rr)
        d.append(("M" if i == 0 else "L") + f"{a[0]:.2f},{a[1]:.2f}")
        d.append(f"Q{p1[0]:.2f},{p1[1]:.2f} {b[0]:.2f},{b[1]:.2f}")
    return " ".join(d) + " Z"


def rrect(x, y, w, h, r):
    return (f"M{x + r},{y} h{w - 2 * r} a{r},{r} 0 0 1 {r},{r} v{h - 2 * r} "
            f"a{r},{r} 0 0 1 -{r},{r} h-{w - 2 * r} a{r},{r} 0 0 1 -{r},-{r} "
            f"v-{h - 2 * r} a{r},{r} 0 0 1 {r},-{r} z")


def circ(cx, cy, r):
    return f"M{cx},{cy} m-{r},0 a{r},{r} 0 1 0 {2 * r},0 a{r},{r} 0 1 0 -{2 * r},0"


def gear(cx, cy, ro, ri, hole, teeth=8):
    pts = []
    step = 2 * math.pi / teeth
    for i in range(teeth):
        a = i * step
        for frac, rad in ((0.0, ri), (0.2, ri), (0.3, ro), (0.7, ro), (0.8, ri)):
            pts.append((cx + rad * math.cos(a + frac * step), cy + rad * math.sin(a + frac * step)))
    return rpoly(pts, 0.7) + " " + circ(cx, cy, hole)


WHITE = "#FFFFFF"
BLACK = "#000000"

# ---------------- 矢量图标（app / player） ----------------
VECTORS = {
    "app/drawable/play.xml": [P(rpoly([(8, 5), (8, 19), (19, 12)], 2), fill=BLACK)],
    "app/drawable/pause.xml": [P(rrect(6, 5, 4, 14, 2), fill=BLACK), P(rrect(14, 5, 4, 14, 2), fill=BLACK)],
    "app/drawable/next.xml": [P(rpoly([(6, 6), (6, 18), (14.5, 12)], 1.8), fill=BLACK), P(rrect(16, 6, 2.5, 12, 1.2), fill=BLACK)],
    "app/drawable/previous.xml": [P(rrect(5.5, 6, 2.5, 12, 1.2), fill=BLACK), P(rpoly([(18, 6), (18, 18), (9.5, 12)], 1.8), fill=BLACK)],
    "app/drawable/ic_send.xml": [P(rpoly([(12, 2), (20.5, 20.5), (12, 16.5), (3.5, 20.5)], 1.5), fill=BLACK)],
    "app/drawable/ic_send_white.xml": [P(rpoly([(3, 3.5), (21, 12), (3, 20.5), (3, 14), (13, 12), (3, 10)], 1.2), fill=WHITE)],
    "app/drawable/ic_file.xml": [P(rpoly([(3, 5), (10, 5), (12, 7), (21, 7), (21, 19), (3, 19)], 2), fill=BLACK)],
    "app/drawable/ic_record.xml": [P(rpoly([(5, 2), (14, 2), (19, 7), (19, 22), (5, 22)], 2) + " M14,2 L14,7 L19,7 M8,12 L16,12 M8,16 L14,16", stroke=BLACK, sw=2)],
    "app/drawable/ic_select.xml": [P(circ(12, 12, 10), fill="@color/green"), P("M7.5,12.2 L10.5,15.2 L16.5,8.8", stroke=WHITE, sw=2.4)],
    "app/drawable/ic_image_select.xml": [P(circ(12, 12, 10), fill="@color/green"), P("M7.5,12.2 L10.5,15.2 L16.5,8.8", stroke=WHITE, sw=2.4)],
    "app/drawable/ic_status_error.xml": [P(circ(12, 12, 10), fill="#F44336"), P("M12,7 L12,13.5", stroke=WHITE, sw=2.2), P(circ(12, 17, 1.3), fill=WHITE)],
    "app/drawable/ic_status_success.xml": [P(circ(12, 12, 10), fill="#4CAF50"), P("M7.5,12.2 L10.5,15.2 L16.5,8.8", stroke=WHITE, sw=2.4)],
    "app/drawable/ic_music.xml": [P(circ(12, 12, 10), fill="@color/colorPrimary"), P(circ(10.2, 16.2, 2.6), fill=WHITE), P("M12.8,16.2 L12.8,7.5 L16.5,7.5 L16.5,9.5", stroke=WHITE, sw=2)],
    "player/drawable/lp_ic_play.xml": [P(rpoly([(8, 5), (8, 19), (19, 12)], 2), fill=WHITE)],
    "player/drawable/lp_ic_pause.xml": [P(rrect(6, 5, 4, 14, 2), fill=WHITE), P(rrect(14, 5, 4, 14, 2), fill=WHITE)],
    "player/drawable/lp_ic_forward.xml": [P(rpoly([(4, 6), (4, 18), (12.5, 12)], 1.8), fill=WHITE), P(rpoly([(13, 6), (13, 18), (21.5, 12)], 1.8), fill=WHITE)],
    "player/drawable/lp_ic_backward.xml": [P(rpoly([(11, 6), (11, 18), (2.5, 12)], 1.8), fill=WHITE), P(rpoly([(20, 6), (20, 18), (11.5, 12)], 1.8), fill=WHITE)],
    "player/drawable/lp_ic_back.xml": [P("M19,12 L6,12 M11,6.5 L5.5,12 L11,17.5", stroke=WHITE, sw=2.2)],
    "player/drawable/lp_ic_brightness.xml": [P(circ(12, 12, 4), stroke=WHITE, sw=2), P(" ".join(
        f"M{12 + 6.5 * math.cos(a):.2f},{12 + 6.5 * math.sin(a):.2f} L{12 + 9.5 * math.cos(a):.2f},{12 + 9.5 * math.sin(a):.2f}"
        for a in [i * math.pi / 4 for i in range(8)]), stroke=WHITE, sw=2)],
    "player/drawable/lp_ic_center_play.xml": [P(circ(12, 12, 9.5), stroke=WHITE, sw=2), P(rpoly([(10, 8.5), (10, 15.5), (16, 12)], 1.5), fill=WHITE)],
    "player/drawable/lp_ic_error.xml": [P(circ(12, 12, 9.5), stroke=WHITE, sw=2), P("M12,7.5 L12,13", stroke=WHITE, sw=2), P(circ(12, 16.5, 1.2), fill=WHITE)],
    "player/drawable/lp_ic_fullscreen.xml": [P("M9,4 L6.5,4 A2.5,2.5 0 0 0 4,6.5 L4,9 M15,4 L17.5,4 A2.5,2.5 0 0 1 20,6.5 L20,9 M20,15 L20,17.5 A2.5,2.5 0 0 1 17.5,20 L15,20 M9,20 L6.5,20 A2.5,2.5 0 0 1 4,17.5 L4,15", stroke=WHITE, sw=2.2)],
    "player/drawable/lp_ic_fullscreen_exit.xml": [P("M4,9 L6.5,9 A2.5,2.5 0 0 0 9,6.5 L9,4 M20,9 L17.5,9 A2.5,2.5 0 0 1 15,6.5 L15,4 M15,20 L15,17.5 A2.5,2.5 0 0 1 17.5,15 L20,15 M9,20 L9,17.5 A2.5,2.5 0 0 0 6.5,15 L4,15", stroke=WHITE, sw=2.2)],
    "player/drawable/lp_ic_lock_closed.xml": [P(rrect(5, 10, 14, 11, 2.5), stroke=WHITE, sw=2), P("M8.5,10 L8.5,7 a3.5,3.5 0 0 1 7,0 L15.5,10", stroke=WHITE, sw=2), P(circ(12, 15.5, 1.6), fill=WHITE)],
    "player/drawable/lp_ic_lock_open.xml": [P(rrect(5, 10, 14, 11, 2.5), stroke=WHITE, sw=2), P("M8.5,10 L8.5,6.5 a3.5,3.5 0 0 1 7,0 L15.5,8", stroke=WHITE, sw=2), P(circ(12, 15.5, 1.6), fill=WHITE)],
    "player/drawable/lp_ic_volume_up.xml": [P(rpoly([(3, 9.5), (7, 9.5), (12, 5), (12, 19), (7, 14.5), (3, 14.5)], 1.5), fill=WHITE), P("M15.5,9.5 a3.5,3.5 0 0 1 0,5 M18,7 a7,7 0 0 1 0,10", stroke=WHITE, sw=2)],
    "player/drawable/lp_ic_volume_off.xml": [P(rpoly([(3, 9.5), (7, 9.5), (12, 5), (12, 19), (7, 14.5), (3, 14.5)], 1.5), fill=WHITE), P("M16,9.5 L21,14.5 M21,9.5 L16,14.5", stroke=WHITE, sw=2)],
    "player/drawable/lp_ic_no_net.xml": [P("M3.5,9 Q12,1.5 20.5,9 M6.5,12.5 Q12,7 17.5,12.5", stroke=WHITE, sw=1.8), P(circ(12, 17.5, 1.4), fill=WHITE), P("M3,3 L21,21", stroke=WHITE, sw=2)],
}

# ---------------- 位图图标设计（颜色参数化） ----------------
def d_setting(c): return [P(gear(12, 12, 10, 7.6, 3.6), fill=c)]
def d_scan(c): return [P("M8,3.5 L6,3.5 A2.5,2.5 0 0 0 3.5,6 L3.5,8 M16,3.5 L18,3.5 A2.5,2.5 0 0 1 20.5,6 L20.5,8 M20.5,16 L20.5,18 A2.5,2.5 0 0 1 18,20.5 L16,20.5 M8,20.5 L6,20.5 A2.5,2.5 0 0 1 3.5,18 L3.5,16", stroke=c, sw=2.6)]
def d_add(c): return [P(circ(12, 12, 9), stroke=c, sw=1.8), P("M12,8 L12,16 M8,12 L16,12", stroke=c, sw=1.8)]
def d_audio(c): return [P(circ(12, 12, 9), stroke=c, sw=1.8), P(rpoly([(8, 10.2), (10, 10.2), (12.5, 8), (12.5, 16), (10, 13.8), (8, 13.8)], 0.8), fill=c), P("M14.5,10 a3,3 0 0 1 0,4", stroke=c, sw=1.6)]
def d_emoji(c): return [P(circ(12, 12, 9), stroke=c, sw=1.8), P(circ(9, 10, 1.1), fill=c), P(circ(15, 10, 1.1), fill=c), P("M8.5,14 Q12,17 15.5,14", stroke=c, sw=1.8)]
def d_pc(c): return [P(rrect(4.5, 5, 15, 10, 2), fill="#D1D3D4", stroke=c, sw=1.8), P("M3,18.5 L21,18.5", stroke=c, sw=1.8)]
def d_phone(c): return [P(rrect(12.5, 3.5, 8, 17, 2), fill="#D1D3D4", stroke=c, sw=1.8), P(rrect(3.5, 6.5, 8, 14, 2), fill="#D1D3D4", stroke=c, sw=1.8), P("M6,18 L9,18", stroke=c, sw=1.2)]
def d_delete(c): return [P(rrect(5, 6, 14, 2, 1), fill=c), P(rpoly([(6.5, 8), (17.5, 8), (16.8, 20.5), (7.2, 20.5)], 1.8), fill=c), P("M9.5,6 L9.5,4.8 A1.3,1.3 0 0 1 10.8,3.5 L13.2,3.5 A1.3,1.3 0 0 1 14.5,4.8 L14.5,6", stroke=c, sw=1.8)]
def d_back(c): return [P("M13.5,5 L7,12 L13.5,19 M19,5 L12.5,12 L19,19", stroke=c, sw=2.4)]
def d_next(c): return [P("M9,5 L16,12 L9,19", stroke=c, sw=2.4)]
def d_exit(c): return [P("M20,12 L5,12 M10.5,6.5 L5,12 L10.5,17.5", stroke=c, sw=2.2)]
def d_folder(c): return [P(rpoly([(3, 5), (10, 5), (12, 7), (21, 7), (21, 19), (3, 19)], 2), fill=c)]
def d_folder_outline(c): return [P(rpoly([(3, 5), (10, 5), (12, 7), (21, 7), (21, 19), (3, 19)], 2), stroke=c, sw=2)]
def d_folder_upload(c): return d_folder(c) + [P("M12,16.5 L12,10.5 M9,13 L12,10 L15,13", stroke=WHITE, sw=2)]
def doc_fill(c, fold=WHITE): return [P(rpoly([(5, 2), (14, 2), (19, 7), (19, 22), (5, 22)], 2), fill=c), P("M14,2 L19,7 L14,7 Z", fill=fold)]
def doc_outline(c): return [P(rpoly([(5, 2), (14, 2), (19, 7), (19, 22), (5, 22)], 2) + " M14,2 L14,7 L19,7", stroke=c, sw=2)]
def d_coder(c): return doc_fill(c) + [P("M10,12 L7.5,14.5 L10,17 M14,12 L16.5,14.5 L14,17", stroke=WHITE, sw=2)]
def d_txts(c): return doc_fill(c) + [P("M8.5,12 L15.5,12 M8.5,15.5 L15.5,15.5 M8.5,19 L12.5,19", stroke=WHITE, sw=1.8)]
def d_zip(c): return doc_fill(c) + [P(rrect(10.6, 7.5, 2, 2, 0.6), fill=WHITE), P(rrect(11.4, 10.5, 2, 2, 0.6), fill=WHITE), P(rrect(10.6, 13.5, 2, 2, 0.6), fill=WHITE), P(circ(12, 18, 1.6), stroke=WHITE, sw=1.5)]
def d_pdf(c): return doc_outline(c) + [P(rrect(3, 11, 18, 7.5, 2), fill=c), P(None, fill=WHITE, text="PDF", size=5)]
def d_txt(c): return doc_outline(c) + [P(None, fill=c, text="T", size=9)]
def d_word(c): return [P(rrect(3, 4, 10, 16, 2), fill=c), P(None, fill=WHITE, text="W", size=7.5, tx=8, ty=14.8), P("M15.5,7 L21,7 M15.5,11 L21,11 M15.5,15 L21,15 M15.5,19 L18,19", stroke=c, sw=1.8)]
def d_gif(c): return [P(rrect(1, 1, 30, 17, 4), fill=WHITE, stroke=BLACK, sw=1.2, opacity=0.55), P(None, fill=BLACK, text="GIF", size=8, opacity=0.45, tx=16, ty=12.5)]
def d_android(c): return [P(rrect(2, 2, 20, 20, 5), fill=c), P("M8,10.5 a4,4 0 0 1 8,0 Z", fill=WHITE), P("M9,7 L8,5.5 M15,7 L16,5.5", stroke=WHITE, sw=1.2), P(circ(10.3, 9, 0.7), fill=c), P(circ(13.7, 9, 0.7), fill=c), P(rrect(8, 11.5, 8, 6.5, 1.5), fill=WHITE), P(rrect(5.6, 11.5, 1.6, 5, 0.8), fill=WHITE), P(rrect(16.8, 11.5, 1.6, 5, 0.8), fill=WHITE), P(rrect(9.5, 18, 1.6, 2.5, 0.8), fill=WHITE), P(rrect(12.9, 18, 1.6, 2.5, 0.8), fill=WHITE)]
def d_apps(c): return [P(circ(x, y, 1.7), fill=c) for x in (6, 12, 18) for y in (6, 12, 18)]
def d_img(c): return [P(rrect(3, 4.5, 18, 15, 3), stroke=c, sw=2), P(circ(8.5, 9.5, 1.7), fill=c), P("M6,16.5 L10.5,12 L13.5,15 L16,12.5 L18.5,15.5", stroke=c, sw=2)]
def d_record(c): return [P(rpoly([(5, 2), (14, 2), (19, 7), (19, 22), (5, 22)], 2) + " M14,2 L14,7 L19,7 M8.5,13 L12,13 M8.5,17 L11,17 M13.5,18.5 L18,14", stroke=c, sw=2)]
def d_no_net(c): return [P("M3.5,9 Q12,1.5 20.5,9 M6.5,12.5 Q12,7 17.5,12.5", stroke=c, sw=2.2), P(circ(12, 17.5, 1.4), fill=c), P("M3,3 L21,21", stroke=c, sw=2.4)]
def d_play(c): return [P(rpoly([(8, 5), (8, 19), (19, 12)], 2.5), fill=c)]
def d_wendang(c): return doc_fill(c, fold="#E5F2FF") + [P("M8.5,12 L15.5,12 M8.5,15.5 L15.5,15.5 M8.5,19 L12.5,19", stroke="#E5F2FF", sw=1.6)]
def d_wenjianjia(c): return [P(rpoly([(2, 3), (9, 3), (11, 5.5), (2, 5.5)], 1.2), fill="#EB9B00"), P(rrect(2, 5.5, 20, 12, 2), fill=c)]

# (设计, 颜色, 输出路径, 像素宽, 像素高, viewBox宽, viewBox高)
PNGS = [
    (d_setting, WHITE, "app/res/drawable-hdpi/ic_setting.png", 72, 72, 24, 24),
    (d_setting, WHITE, "app/res/drawable-xhdpi/ic_setting.png", 96, 96, 24, 24),
    (d_setting, WHITE, "app/res/drawable-xxhdpi/ic_setting.png", 144, 144, 24, 24),
    (d_scan, "#424242", "app/res/drawable-xxhdpi/ic_scan.png", 128, 128, 24, 24),
    (d_scan, "#E0E0E0", "app/res/drawable-night/ic_scan.png", 128, 128, 24, 24),
    (d_add, "#7F8389", "app/res/drawable-xhdpi/ic_add.png", 70, 70, 24, 24),
    (d_audio, "#7F8389", "app/res/drawable-xhdpi/ic_audio.png", 70, 70, 24, 24),
    (d_emoji, "#7F8389", "app/res/drawable-xhdpi/ic_emoji.png", 70, 70, 24, 24),
    (d_pc, "#333333", "app/res/drawable-xhdpi/ic_pc.png", 72, 72, 24, 24),
    (d_phone, "#333333", "app/res/drawable-xhdpi/ic_phone.png", 72, 72, 24, 24),
    (d_delete, BLACK, "app/res/drawable-xxhdpi/baseline_delete_black_48.png", 96, 96, 24, 24),
    (d_back, BLACK, "app/res/drawable-xxhdpi/ic_back.png", 50, 50, 24, 24),
    (d_next, "#444444", "app/res/drawable-xxhdpi/ic_next.png", 96, 96, 24, 24),
    (d_exit, WHITE, "app/res/drawable-xxhdpi/ic_exit.png", 96, 96, 24, 24),
    (d_folder, "#444444", "app/res/drawable-xxhdpi/ic_folder.png", 96, 96, 24, 24),
    (d_folder_upload, "#444444", "app/res/drawable-xxhdpi/ic_folder_upload.png", 96, 96, 24, 24),
    (d_coder, "#515151", "app/res/drawable-xxhdpi/ic_coder_file.png", 96, 96, 24, 24),
    (lambda c: doc_fill(c), "#515151", "app/res/drawable-xxhdpi/ic_file_file.png", 96, 96, 24, 24),
    (d_pdf, "#515151", "app/res/drawable-xxhdpi/ic_pdf_file.png", 96, 96, 24, 24),
    (d_txt, "#515151", "app/res/drawable-xxhdpi/ic_txt_file.png", 96, 96, 24, 24),
    (d_txts, "#515151", "app/res/drawable-xxhdpi/ic_txts_file.png", 96, 96, 24, 24),
    (d_word, "#515151", "app/res/drawable-xxhdpi/ic_word_file.png", 96, 96, 24, 24),
    (d_zip, "#515151", "app/res/drawable-xxhdpi/ic_zip_file.png", 96, 96, 24, 24),
    (d_gif, None, "app/res/drawable-xxhdpi/ic_gif.png", 64, 38, 32, 19),
    (d_android, "#6A737B", "app/res/drawable-xxhdpi/ic_null_android_file.png", 96, 96, 24, 24),
    (d_apps, "#4D5259", "web/apps.png", 32, 32, 24, 24),
    (d_exit, "#424040", "web/exit.png", 40, 40, 24, 24),
    (d_folder_outline, "#4D5259", "web/folder.png", 32, 32, 24, 24),
    (d_img, "#4D5259", "web/img.png", 32, 32, 24, 24),
    (d_record, "#4D5259", "web/record.png", 32, 32, 24, 24),
    (d_no_net, WHITE, "web/no_network.png", 48, 48, 24, 24),
    (d_play, WHITE, "web/player_play.png", 128, 128, 24, 24),
    (d_wendang, "#1E89FF", "web/wendang.png", 20, 20, 24, 24),
    (d_wenjianjia, "#F8B700", "web/wenjianjia_20.png", 20, 17, 24, 20.4),
    (d_wenjianjia, "#F8B700", "web/wenjianjia_60.png", 40, 35, 24, 20.4),
]


def svg_path(p):
    if p["text"]:
        op = f' fill-opacity="{p["opacity"]}"' if p["opacity"] else ""
        return (f'<text x="{p["tx"]}" y="{p["ty"]}" font-family="Arial, DejaVu Sans, sans-serif" font-weight="700" '
                f'font-size="{p["size"]}" text-anchor="middle" fill="{p["fill"]}"{op}>{p["text"]}</text>')
    attrs = f' d="{p["d"]}"'
    attrs += f' fill="{p["fill"] or "none"}"'
    if p["fill"] and p["opacity"]:
        attrs += f' fill-opacity="{p["opacity"]}"'
    if p["stroke"]:
        attrs += f' stroke="{p["stroke"]}" stroke-width="{p["sw"]}" stroke-linecap="round" stroke-linejoin="round"'
        if p["opacity"]:
            attrs += f' stroke-opacity="{p["opacity"]}"'
    if p["fill"] and p["d"].count("M") > 1:
        attrs += ' fill-rule="evenodd"'
    return "<path" + attrs + "/>"


def render(svg, w, h):
    try:
        import cairosvg
        return cairosvg.svg2png(bytestring=svg.encode(), output_width=w, output_height=h)
    except ImportError:
        from resvg_py import svg_to_bytes
        return svg_to_bytes(svg_string=svg, width=w, height=h)


def write_vector(rel, paths):
    fp = os.path.join(ROOT, rel.replace("app/", "app/src/main/res/", 1).replace("player/", "player/src/main/res/", 1))
    old = open(fp, encoding="utf-8").read()
    w = re.search(r'android:width="([^"]+)"', old).group(1)
    h = re.search(r'android:height="([^"]+)"', old).group(1)
    vw = re.search(r'viewportWidth="([\d.]+)"', old).group(1)
    vh = re.search(r'viewportHeight="([\d.]+)"', old).group(1)
    out = ['<?xml version="1.0" encoding="utf-8"?>',
           '<!-- 圆润版图标，由 icon/rounded_icons.py 生成 -->',
           '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
           f'    android:width="{w}"', f'    android:height="{h}"',
           f'    android:viewportWidth="{vw}"', f'    android:viewportHeight="{vh}">']
    for p in paths:
        out.append("    <path")
        if p["fill"]:
            out.append(f'        android:fillColor="{p["fill"]}"')
            if p["d"].count("M") > 1:
                out.append('        android:fillType="evenOdd"')
        if p["stroke"]:
            out.append(f'        android:strokeColor="{p["stroke"]}"')
            out.append(f'        android:strokeWidth="{p["sw"]}"')
            out.append('        android:strokeLineCap="round"')
            out.append('        android:strokeLineJoin="round"')
        out.append(f'        android:pathData="{p["d"]}" />')
    out.append("</vector>")
    open(fp, "w", encoding="utf-8").write("\n".join(out) + "\n")


def main():
    for rel, paths in VECTORS.items():
        write_vector(rel, paths)
    print(f"vector: {len(VECTORS)} 个已重写")
    fp = os.path.join(APP_RES, "drawable", "ic_charging.xml")
    s = open(fp, encoding="utf-8").read()
    if "strokeLineCap" not in s:
        s = s.replace('android:strokeWidth="2"', 'android:strokeWidth="2"\n      android:strokeLineCap="round"\n      android:strokeLineJoin="round"')
        open(fp, "w", encoding="utf-8").write(s)
    from PIL import Image
    import io
    for design, color, rel, w, h, vw, vh in PNGS:
        paths = design(color)
        body = "".join(svg_path(p) for p in paths)
        svg = f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {vw} {vh}">{body}</svg>'
        fp = os.path.join(WEB_IMG, rel[4:]) if rel.startswith("web/") else os.path.join(ROOT, rel.replace("app/res/", "app/src/main/res/", 1))
        Image.open(io.BytesIO(render(svg, w, h))).convert("RGBA").save(fp, "PNG")
    print(f"png: {len(PNGS)} 个已重绘")
    sys.path.insert(0, BASE)
    import generate_icons as gi
    svg = gi.make_round_svg(open(os.path.join(BASE, "lanshare.svg"), encoding="utf-8").read())
    Image.open(io.BytesIO(gi.render_png(svg, 96))).convert("RGBA").save(os.path.join(WEB_IMG, "lanshare.png"), "PNG")
    print("web logo 已同步")


if __name__ == "__main__":
    main()