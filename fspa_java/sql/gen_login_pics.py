# -*- coding: utf-8 -*-
"""生成登录页内置素材：左侧宣传图 + 全屏背景图

原来登录页的 banner / 背景图指向 crmebimage 下的老图片，磁盘上早已不存在，
导致登录页左侧是一块白框、背景只剩 canvas 星空。这里改为程序内置素材，
随 jar 一起发布，不依赖外部上传目录。
"""
import os
import random
from PIL import Image, ImageDraw, ImageFilter, ImageFont

OUT_DIR = r"D:/working/fspa_pro/fspa_java/crmeb-admin/src/main/resources/static/images"
FONT_BD = "C:/Windows/Fonts/msyhbd.ttc"
FONT_RG = "C:/Windows/Fonts/msyh.ttc"

GREEN_DEEP = (36, 70, 58)
GREEN_DARK = (18, 40, 32)
WHITE = (255, 255, 255, 255)


def radial(size, inner, outer, center=(0.5, 0.35), radius=1.35):
    """径向渐变底图"""
    w, h = size
    base = Image.new("RGB", (w, h), outer)
    grad = Image.new("L", (w, h), 0)
    d = ImageDraw.Draw(grad)
    cx, cy = int(w * center[0]), int(h * center[1])
    maxr = int(max(w, h) * radius)
    steps = 220
    for i in range(steps, 0, -1):
        r = int(maxr * i / steps)
        v = int(255 * (1 - i / steps))
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=v)
    grad = grad.filter(ImageFilter.GaussianBlur(12))
    top = Image.new("RGB", (w, h), inner)
    return Image.composite(top, base, grad)


def draw_mark(d, cx, cy, r, lw, color):
    """圆环 + 双向箭头：物物相换"""
    d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=color, width=lw)
    half = int(r * 0.52)
    head = max(5, int(r * 0.24))
    gap = max(6, int(r * 0.21))
    y1 = cy - gap
    d.line([cx + half, y1, cx - half + head, y1], fill=color, width=lw)
    d.polygon([(cx - half, y1), (cx - half + head + 1, y1 - head + 1),
               (cx - half + head + 1, y1 + head - 1)], fill=color)
    y2 = cy + gap
    d.line([cx - half, y2, cx + half - head, y2], fill=color, width=lw)
    d.polygon([(cx + half, y2), (cx + half - head - 1, y2 - head + 1),
               (cx + half - head - 1, y2 + head - 1)], fill=color)


def center_text(d, y, text, font, fill, width):
    bbox = d.textbbox((0, 0), text, font=font)
    d.text(((width - (bbox[2] - bbox[0])) / 2 - bbox[0], y), text, font=font, fill=fill)


def gen_banner(path):
    """左侧宣传图 572x800（页面按 286x400 显示，2 倍图）"""
    W, H = 572, 800
    im = radial((W, H), (44, 84, 70), (16, 34, 27), center=(0.5, 0.28), radius=1.1)
    d = ImageDraw.Draw(im, "RGBA")

    # 几道斜向光带
    for i, (off, alpha) in enumerate([(-260, 16), (60, 12), (320, 9)]):
        d.polygon([(off, 0), (off + 210, 0), (off + 210 - 520, H), (off - 520, H)],
                  fill=(255, 255, 255, alpha))

    # 圆环装饰
    d.ellipse([W - 150, -90, W + 90, 150], outline=(255, 255, 255, 34), width=3)
    d.ellipse([-110, H - 170, 150, H + 90], outline=(255, 255, 255, 28), width=3)

    # 主图形
    draw_mark(d, W // 2, 250, 86, 9, (255, 255, 255, 236))

    f_title = ImageFont.truetype(FONT_BD, 62)
    f_sub = ImageFont.truetype(FONT_RG, 25)
    f_item = ImageFont.truetype(FONT_RG, 23)

    center_text(d, 388, "良物换物", f_title, (255, 255, 255, 255), W)
    center_text(d, 478, "闲置互换 · 智能匹配", f_sub, (176, 214, 197, 255), W)

    # 分隔线
    d.line([W / 2 - 70, 534, W / 2 + 70, 534], fill=(255, 255, 255, 70), width=2)

    items = ["多人物品成链匹配", "信用分保障履约", "全流程留痕可追溯"]
    y = 572
    for it in items:
        d.ellipse([W / 2 - 108, y + 9, W / 2 - 98, y + 19], fill=(120, 200, 165, 255))
        d.text((W / 2 - 84, y), it, font=f_item, fill=(223, 238, 230, 255))
        y += 52

    im.save(path, quality=95)
    print("banner ->", path, im.size)


def gen_bg(path):
    """全屏背景 1920x1080"""
    W, H = 1920, 1080
    im = radial((W, H), (38, 74, 62), (11, 22, 19), center=(0.5, 0.3), radius=1.5)
    d = ImageDraw.Draw(im, "RGBA")
    random.seed(20261029)
    for _ in range(420):
        x = random.randint(0, W - 1)
        y = random.randint(0, H - 1)
        r = random.choice([1, 1, 1, 2])
        a = random.randint(28, 96)
        d.ellipse([x, y, x + r, y + r], fill=(255, 255, 255, a))
    # 大体积淡圆
    d.ellipse([-320, H - 620, 420, H + 120], outline=(255, 255, 255, 16), width=3)
    d.ellipse([W - 520, -280, W + 260, 500], outline=(255, 255, 255, 14), width=3)
    im = im.filter(ImageFilter.GaussianBlur(0.4))
    im.save(path, quality=90)
    print("bg ->", path, im.size)


if __name__ == "__main__":
    os.makedirs(OUT_DIR, exist_ok=True)
    gen_banner(os.path.join(OUT_DIR, "login-banner.png"))
    gen_bg(os.path.join(OUT_DIR, "login-bg.jpg"))
