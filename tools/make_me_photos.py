# Dev-скрипт: оригиналы фото пасхалки assets/me/raw/ -> игровые assets/me/*.jpg (dev-only).
#
# Рамка строится КАК СТЕНЫ ЛАБИРИНТА — dual-grid 15-piece автотайл темы (см.
# MazeRenderer.drawAutotileWalls в игре: маска из 4 клеток вокруг угла, тайл 40×40).
# Фото: кроп под внутреннюю область → уменьшение до PIXEL_WIDTH → NEAREST-увеличение.
# Оригиналы в raw/ в git, в сборку/jar/APK не попадают. Запуск: python tools/make_me_photos.py

import os
from PIL import Image, ImageOps

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
RAW = os.path.join(ROOT, "assets", "me", "raw")
OUT = os.path.join(ROOT, "assets", "me")

MAX_WIDTH = 1280              # ширина готового игрового файла
MAX_HEIGHT = round(MAX_WIDTH * 9 / 16)   # 720 (16:9)
PIXEL_WIDTH = 80              # ширина «низкого» изображения (меньше — крупнее пиксели)
BORDER_TILES = 1              # толщина рамки-стены в клетках (1 = как стены лабиринта)
COUNT = None                  # сколько первых оригиналов выводить (None — все); если задан SELECT — игнорируется
# Какие оригиналы брать и в каком порядке (имена из raw/). None — по COUNT/все.
SELECT = ["08.jpg"]
EXTS = (".jpg", ".jpeg", ".png")

# Автотайл стен темы (dual-grid 15-piece), как в игре
AUTOTILE = os.path.join(ROOT, "assets", "themes", "grass", "wall_autotile.png")
ATLAS_COLUMNS = 4
ATLAS_ROWS = 4
TILE = 40                     # размер клетки/тайла (Constants.CELL_SIZE)

# Таблица соответствия маски угла и кадра атласа — 1:1 с MazeRenderer.drawAutotileWalls.
# Бит 1 — top-left, 2 — top-right, 4 — bottom-left, 8 — bottom-right (стена вокруг угла).
FRAME_BY_MASK = [-1, 15, 8, 9, 0, 11, 14, 7, 13, 4, 1, 10, 3, 2, 5, 6]

FRAME = BORDER_TILES * TILE
INNER_W = MAX_WIDTH - 2 * FRAME
INNER_H = MAX_HEIGHT - 2 * FRAME


def crop_to_aspect(img, aspect):
    """Центральный кроп изображения под заданное соотношение сторон."""
    w, h = img.size
    if w / h > aspect:
        new_w = max(1, round(h * aspect))
        left = (w - new_w) // 2
        return img.crop((left, 0, left + new_w, h))
    new_h = max(1, round(w / aspect))
    top = (h - new_h) // 2
    return img.crop((0, top, w, top + new_h))


def build_wall_frame():
    """Рамка из стен темы через dual-grid автотайл: канва с проёмом под фото."""
    cols = MAX_WIDTH // TILE
    rows = MAX_HEIGHT // TILE
    b = BORDER_TILES

    def is_wall(c, r):
        if c < 0 or c >= cols or r < 0 or r >= rows:
            return True  # вне поля — стена (как isWall в игре)
        return c < b or c >= cols - b or r < b or r >= rows - b

    canvas = Image.new("RGBA", (MAX_WIDTH, MAX_HEIGHT), (0, 0, 0, 0))
    atlas = Image.open(AUTOTILE).convert("RGBA")
    tile_w = atlas.width // ATLAS_COLUMNS
    tile_h = atlas.height // ATLAS_ROWS

    for row in range(rows + 1):
        for col in range(cols + 1):
            mask = (
                (1 if is_wall(col - 1, row - 1) else 0)
                | (2 if is_wall(col, row - 1) else 0)
                | (4 if is_wall(col - 1, row) else 0)
                | (8 if is_wall(col, row) else 0)
            )
            frame = FRAME_BY_MASK[mask]
            if frame < 0:
                continue
            tx = frame % ATLAS_COLUMNS
            ty = frame // ATLAS_COLUMNS
            tile = atlas.crop((tx * tile_w, ty * tile_h, tx * tile_w + tile_w, ty * tile_h + tile_h))
            if tile.size != (TILE, TILE):
                tile = tile.resize((TILE, TILE), Image.LANCZOS)
            canvas.paste(tile, (col * TILE - TILE // 2, row * TILE - TILE // 2), tile)

    return canvas


def main():
    os.makedirs(OUT, exist_ok=True)
    if not os.path.isdir(RAW):
        print(f"нет каталога {RAW}")
        return

    # Удаляем предыдущие игровые фото (не трогая raw/ и photos.json)
    for old in os.listdir(OUT):
        path = os.path.join(OUT, old)
        if os.path.isfile(path) and old.lower().endswith(EXTS):
            os.remove(path)

    all_sources = sorted(n for n in os.listdir(RAW) if n.lower().endswith(EXTS))
    if SELECT is not None:
        available = set(all_sources)
        sources = [n for n in SELECT if n in available]  # порядок — как в SELECT
    elif COUNT is not None:
        sources = all_sources[:COUNT]
    else:
        sources = all_sources

    frame_canvas = build_wall_frame()
    made = 0
    for index, name in enumerate(sources, start=1):
        src = ImageOps.exif_transpose(Image.open(os.path.join(RAW, name))).convert("RGB")
        src = crop_to_aspect(src, INNER_W / INNER_H)

        # 1) уменьшить → 2) увеличить NEAREST (пикселизация запекается)
        pixel_h = max(1, round(PIXEL_WIDTH * INNER_H / INNER_W))
        small = src.resize((PIXEL_WIDTH, pixel_h), Image.LANCZOS)
        photo = small.resize((INNER_W, INNER_H), Image.NEAREST)

        canvas = frame_canvas.copy()
        canvas.paste(photo, (FRAME, FRAME))

        out_name = f"{index}.jpg"
        canvas.convert("RGB").save(os.path.join(OUT, out_name), "JPEG", quality=90)
        made += 1
        print(f"{name} -> {out_name} {MAX_WIDTH}x{MAX_HEIGHT} (pixel {PIXEL_WIDTH}px, walls {BORDER_TILES} tile)")

    print(f"\nготово: {made} фото -> assets/me/ (рамка из стен темы, dual-grid)")


if __name__ == "__main__":
    main()
