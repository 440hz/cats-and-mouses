import argparse
import os
from PIL import Image

# Нормализация одиночного спрайта-маркера (сыр, сердечко и т.п.): обрезка по видимым
# пикселям (alpha > threshold), масштаб контента до заданной высоты (LANCZOS, пропорции
# сохранены) и центрирование на прозрачном холсте. Тот же приём, что у сыра: контент
# уменьшается ВНУТРИ холста, а не кодом отрисовки. Dev-only, в игровую сборку не входит.
# Запуск:
#   python tools/pad_marker.py <raw.png> <out.png> --height 38 --canvas 64
#   python tools/pad_marker.py <in.png> <out.png> --color 42,42,42   (перекраска силуэта)

THRESHOLD_DEFAULT = 10


def content_bbox(img, threshold):
    alpha = img.split()[3].point(lambda a: 255 if a > threshold else 0)
    return alpha.getbbox()


def main():
    p = argparse.ArgumentParser()
    p.add_argument("src")
    p.add_argument("out")
    p.add_argument("--height", type=int, default=None, help="высота контента; без него — не масштабировать")
    p.add_argument("--canvas", type=int, default=64)
    p.add_argument("--threshold", type=int, default=THRESHOLD_DEFAULT)
    p.add_argument("--color", default=None, help="R,G,B — залить силуэт (alpha сохраняется)")
    args = p.parse_args()

    img = Image.open(args.src).convert("RGBA")
    bbox = content_bbox(img, args.threshold)
    if bbox is None:
        raise SystemExit(f"пустой спрайт (нет пикселей alpha > {args.threshold}): {args.src}")
    content = img.crop(bbox)

    if args.height:
        cw, ch = content.size
        new_w = max(1, round(cw * args.height / ch))
        content = content.resize((new_w, args.height), Image.LANCZOS)

    if args.color:
        r, g, b = (int(v) for v in args.color.split(","))
        flat = Image.new("RGBA", content.size, (r, g, b, 0))
        flat.putalpha(content.split()[3])
        content = flat

    canvas = Image.new("RGBA", (args.canvas, args.canvas), (0, 0, 0, 0))
    x = (args.canvas - content.width) // 2
    y = (args.canvas - content.height) // 2
    canvas.alpha_composite(content, (x, y))
    os.makedirs(os.path.dirname(os.path.abspath(args.out)), exist_ok=True)
    canvas.save(args.out)
    print(f"{args.src} -> {args.out}: контент {content.width}x{content.height} на холсте {args.canvas}x{args.canvas}")


if __name__ == "__main__":
    main()
