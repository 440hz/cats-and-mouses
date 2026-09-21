import argparse
import os

from PIL import Image

# Нормализация анимационного спрайт-листа в игровой вид.
# Лист от SpriteCook (горизонтальный стрип кадров) режется на кадры, каждый кадр
# обрезается по видимому контенту (alpha > порога), масштабируется до заданной
# высоты контента (LANCZOS) и центрируется на квадратном прозрачном холсте.
#
# Важно: масштаб и опорная точка общие для всех кадров (по объединённому bbox),
# чтобы покачивание/смещение внутри кадра сохранилось, а не «схлопнулось»
# в статичную позу.
#
# Пример (сыр): python tools/normalize_sheet.py \
#   --in assets/sprites/raw/cheese_sheet.png --frames 6 \
#   --frame-size 64 --content-height 38 --out assets/sprites/cheese_sheet.png


def parse_args():
    p = argparse.ArgumentParser(description="Нормализовать анимационный спрайт-лист")
    p.add_argument("--in", dest="src", required=True, help="Исходный RAW-лист (PNG)")
    p.add_argument("--frames", type=int, required=True, help="Число кадров в листе")
    p.add_argument("--out", dest="dst", required=True, help="Куда сохранить игровой лист")
    p.add_argument("--frame-size", type=int, default=64, help="Сторона квадратного холста кадра")
    p.add_argument("--content-height", type=int, required=True, help="Целевая высота видимого контента")
    p.add_argument("--alpha-threshold", type=int, default=10, help="Порог alpha для обрезки")
    p.add_argument("--anchor", choices=["bottom", "center"], default="bottom",
                   help="Привязка контента по вертикали (bottom — стоит на земле)")
    return p.parse_args()


def visible_bbox(frame, threshold):
    alpha = frame.getchannel("A").point(lambda v: 255 if v > threshold else 0)
    return alpha.getbbox()


def main():
    args = parse_args()

    sheet = Image.open(args.src).convert("RGBA")
    total_w, frame_h = sheet.size
    if total_w % args.frames != 0:
        raise SystemExit(f"Ширина листа {total_w} не делится на {args.frames} кадров")
    frame_w = total_w // args.frames

    frames = [sheet.crop((i * frame_w, 0, (i + 1) * frame_w, frame_h)) for i in range(args.frames)]

    boxes = [visible_bbox(f, args.alpha_threshold) for f in frames]
    if any(b is None for b in boxes):
        raise SystemExit("Найден пустой кадр (нет видимого контента)")

    ux0 = min(b[0] for b in boxes)
    uy0 = min(b[1] for b in boxes)
    ux1 = max(b[2] for b in boxes)
    uy1 = max(b[3] for b in boxes)
    u_w, u_h = ux1 - ux0, uy1 - uy0

    scale = args.content_height / u_h
    out_w = max(1, round(u_w * scale))
    out_h = args.content_height
    if out_w > args.frame_size or out_h > args.frame_size:
        raise SystemExit(f"Контент {out_w}x{out_h} не влезает в холст {args.frame_size}")

    if args.anchor == "bottom":
        paste_y = (args.frame_size + out_h) // 2 - out_h
    else:
        paste_y = (args.frame_size - out_h) // 2
    paste_x = (args.frame_size - out_w) // 2

    strip = Image.new("RGBA", (args.frame_size * args.frames, args.frame_size), (0, 0, 0, 0))
    for i, frame in enumerate(frames):
        content = frame.crop((ux0, uy0, ux1, uy1)).resize((out_w, out_h), Image.LANCZOS)
        strip.paste(content, (i * args.frame_size + paste_x, paste_y), content)

    os.makedirs(os.path.dirname(os.path.abspath(args.dst)), exist_ok=True)
    strip.save(args.dst)
    content_bbox = strip.getchannel("A").point(lambda v: 255 if v > args.alpha_threshold else 0).getbbox()
    print(f"{args.src}: {total_w}x{frame_h} ({args.frames} кадров {frame_w}x{frame_h})")
    print(f"  union bbox: ({ux0},{uy0})-({ux1},{uy1}) {u_w}x{u_h}, scale {scale:.3f}")
    print(f"  контент: {out_w}x{out_h}, холст {args.frame_size}x{args.frame_size}")
    print(f"  -> {args.dst} {strip.size[0]}x{strip.size[1]} (bbox {content_bbox})")


if __name__ == "__main__":
    main()
