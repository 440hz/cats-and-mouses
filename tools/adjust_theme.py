# Подгонка яркости темы CATS AND MOUSES (dev-only).
#
# Читает RAW темы (themes/<id>/raw/background.png, raw/wall_autotile.png),
# применяет «мягкую» коррекцию яркости и пишет игровые файлы в themes/<id>/:
#   background.png      — RGB × backgroundShade (затемнение), 200x200, LANCZOS;
#   wall_autotile.png   — c' = c + (255 - c) × wallLighten (осветление),
#                         160x160, LANCZOS, alpha без изменений.
#
# Значения берутся из theme.json (backgroundShade, wallLighten). Если поля нет —
# используется аргумент CLI, иначе дефолт (0.85 / 0.15, т.е. 15%).
# Оригиналы raw/ не трогаются. Превью view/ строится отдельно: tools/make_view.py.
#
# Запуск: python tools/adjust_theme.py <theme_id> [--shade 0.85] [--lighten 0.15]
# Зависимость: Pillow

import argparse
import json
import os

from PIL import Image

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
THEMES = os.path.join(ROOT, "assets", "themes")

BG_OUT = (200, 200)
WALL_OUT = (160, 160)
DEFAULT_SHADE = 0.85
DEFAULT_LIGHTEN = 0.15


def shade_background(img, factor):
    rgb = img.convert("RGB")
    r, g, b = rgb.split()
    mul = lambda v: int(round(v * factor))
    return Image.merge("RGB", (r.point(mul), g.point(mul), b.point(mul)))


def lighten_wall(img, amount):
    rgba = img.convert("RGBA")
    r, g, b, a = rgba.split()
    boost = lambda v: min(255, int(round(v + (255 - v) * amount)))
    return Image.merge("RGBA", (r.point(boost), g.point(boost), b.point(boost), a))


def theme_values(theme_dir, args):
    path = os.path.join(theme_dir, "theme.json")
    data = {}
    if os.path.exists(path):
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
    shade = args.shade if args.shade is not None else data.get("backgroundShade", DEFAULT_SHADE)
    lighten = args.lighten if args.lighten is not None else data.get("wallLighten", DEFAULT_LIGHTEN)
    return float(shade), float(lighten)


def main():
    ap = argparse.ArgumentParser(description="Яркость темы: raw -> игровые файлы")
    ap.add_argument("theme", help="id темы (папка assets/themes/<id>)")
    ap.add_argument("--shade", type=float, default=None, help="множитель RGB фона (напр. 0.85)")
    ap.add_argument("--lighten", type=float, default=None, help="доля осветления стен (напр. 0.15)")
    args = ap.parse_args()

    theme_dir = os.path.join(THEMES, args.theme)
    raw_dir = os.path.join(theme_dir, "raw")
    if not os.path.isdir(raw_dir):
        raise SystemExit(f"нет raw/: {raw_dir}")

    shade, lighten = theme_values(theme_dir, args)

    bg_raw = os.path.join(raw_dir, "background.png")
    wall_raw = os.path.join(raw_dir, "wall_autotile.png")

    bg = Image.open(bg_raw)
    bg = shade_background(bg, shade).resize(BG_OUT, Image.LANCZOS)
    bg.save(os.path.join(theme_dir, "background.png"))
    print(f"background.png: RGB x{shade} -> {BG_OUT[0]}x{BG_OUT[1]}")

    wall = Image.open(wall_raw)
    wall = lighten_wall(wall, lighten).resize(WALL_OUT, Image.LANCZOS)
    wall.save(os.path.join(theme_dir, "wall_autotile.png"))
    print(f"wall_autotile.png: lighten {lighten} -> {WALL_OUT[0]}x{WALL_OUT[1]}")

    print(f"[{args.theme}] backgroundShade={shade}, wallLighten={lighten}")
    print("Дальше: python tools/make_view.py")


if __name__ == "__main__":
    main()
