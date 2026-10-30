# -*- coding: utf-8 -*-
"""生成「良物换物」后台 logo。

两套配色，因为承载背景不同：
  - 白图（logo.png / logo-square.png）：深色侧边栏用
  - 深图（logo-login.png）：登录页白色卡片用，白字白图在白底上等于看不见
"""
from PIL import Image, ImageDraw, ImageFont
import os

FONT_BD = "C:/Windows/Fonts/msyhbd.ttc"
OUT_DIR = r"D:/working/fspa_pro/fspa_java/crmeb-admin/src/main/resources/static/images"
WHITE = (255, 255, 255, 255)
# 品牌墨绿，与「良物」生态气质一致，白卡上对比度足够
DARK = (47, 79, 63, 255)


def draw_mark(d, cx, cy, r, lw, color=WHITE):
    """圆形环 + 环内双向箭头，寓意「物与物相换」"""
    d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=color, width=lw)
    half = int(r * 0.55)
    head = max(4, int(r * 0.26))
    gap = max(4, int(r * 0.23))
    # 上：向左
    y1 = cy - gap
    d.line([cx + half, y1, cx - half + head, y1], fill=color, width=lw)
    d.polygon([(cx - half, y1), (cx - half + head + 1, y1 - head + 1),
               (cx - half + head + 1, y1 + head - 1)], fill=color)
    # 下：向右
    y2 = cy + gap
    d.line([cx - half, y2, cx + half - head, y2], fill=color, width=lw)
    d.polygon([(cx + half, y2), (cx + half - head - 1, y2 - head + 1),
               (cx + half - head - 1, y2 + head - 1)], fill=color)


def gen_wide(path, text="良物换物", font_size=34, color=WHITE):
    W, H = 320, 80
    im = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    draw_mark(d, 42, 40, 31, 5, color)
    font = ImageFont.truetype(FONT_BD, font_size)
    bbox = d.textbbox((0, 0), text, font=font)
    tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
    d.text((86, (H - th) / 2 - bbox[1]), text, font=font, fill=color)
    im.save(path)
    print("wide ->", path, im.size, "text_w=", tw)


def gen_square(path, color=WHITE):
    S = 96
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    draw_mark(d, S // 2, S // 2, 32, 5, color)
    im.save(path)
    print("square ->", path, im.size)


if __name__ == "__main__":
    os.makedirs(OUT_DIR, exist_ok=True)
    # 深色侧栏：白图
    gen_wide(os.path.join(OUT_DIR, "logo.png"))
    gen_square(os.path.join(OUT_DIR, "logo-square.png"))
    # 登录页白卡：深色图（白图在白卡上不可见）
    gen_wide(os.path.join(OUT_DIR, "logo-login.png"), font_size=40, color=DARK)
