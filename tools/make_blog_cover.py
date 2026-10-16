"""Сборка обложки статьи в Блог RuStore (dev-only, вне игры/APK).

Берёт сгенерированный RAW-арт (store/blog/raw/*), приводит к 16:9 canvas,
накладывает заголовок и подпись. Запуск:
    python tools/make_blog_cover.py <in.png> <out.png> <hd|pixel>
"""

import sys
from PIL import Image, ImageDraw, ImageFont

W, H = 1568, 880
FONT_BOLD = r"C:\Windows\Fonts\arialbd.ttf"
FONT_REG = r"C:\Windows\Fonts\arial.ttf"

TITLE = "Кошки-мышки"
SUBTITLE = "собирай сыр — убегай от четырёх котов"


def cover(img, w, h, resample, bias_y=0.5):
    """Масштаб «по заполнению» + центральный кроп с учётом вертикального смещения."""
    ratio = max(w / img.width, h / img.height)
    nw, nh = round(img.width * ratio), round(img.height * ratio)
    img = img.resize((nw, nh), resample)
    x = (nw - w) // 2
    y = int((nh - h) * bias_y)
    return img.crop((x, y, x + w, y + h))


def bottom_scrim(base, height):
    """Наложить мягкое затемнение снизу для читаемости заголовка."""
    scrim = Image.new("RGBA", base.size, (0, 0, 0, 0))
    px = scrim.load()
    top = base.height - height
    for y in range(top, base.height):
        t = (y - top) / height
        a = int(200 * (t ** 1.4))
        for x in range(base.width):
            px[x, y] = (0, 0, 0, a)
    return Image.alpha_composite(base.convert("RGBA"), scrim)


def draw_text(draw, xy, text, font, fill, anchor, shadow=(0, 0, 0, 230), off=4):
    x, y = xy
    draw.text((x + off, y + off), text, font=font, fill=shadow, anchor=anchor)
    draw.text((x, y), text, font=font, fill=fill, anchor=anchor)


def main():
    src, out, mode = sys.argv[1], sys.argv[2], sys.argv[3]
    resample = Image.NEAREST if mode == "pixel" else Image.LANCZOS
    img = Image.open(src).convert("RGB")
    img = cover(img, W, H, resample)

    base = bottom_scrim(img, 360)
    draw = ImageDraw.Draw(base)
    f_title = ImageFont.truetype(FONT_BOLD, 126)
    f_sub = ImageFont.truetype(FONT_REG, 42)

    draw_text(draw, (W // 2, H - 168), TITLE, f_title, (255, 255, 255, 255), "mm")
    draw_text(draw, (W // 2, H - 66), SUBTITLE, f_sub, (240, 240, 240, 255), "mm", off=3)

    base.convert("RGB").save(out, "PNG")
    print(f"saved {out} {base.size}")


if __name__ == "__main__":
    main()
