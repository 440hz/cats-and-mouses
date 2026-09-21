# Превью карт для CATS AND MOUSES (dev-only).
#
# Рисует карту 32x16 «как в игре»: фон темы тайлами, стены через dual-grid
# (тот же маппинг масок, что в GameScreen.drawAutotileWalls), спрайты мыши,
# котов и сыра. Результат — PNG 1:1 (1280x640) рядом с картой:
#   assets/maps/<мир>/<карта>.png
# Превью автогенерируемы, в git не хранятся, в игровую сборку не попадают.
#
# Запуск: python tools/map_preview.py
# Зависимость: Pillow

import os
import json
from PIL import Image, ImageDraw

CELL = 40
W = 32
H = 16
BORDER = "#"

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
ASSETS = os.path.join(ROOT, "assets")
MAPS = os.path.join(ASSETS, "maps")
THEMES = os.path.join(ASSETS, "themes")
SPRITES = os.path.join(ASSETS, "sprites")

# Маппинг масок dual-grid — идентичен GameScreen.drawAutotileWalls()
FRAME_BY_MASK = [-1, 15, 8, 9, 0, 11, 14, 7, 13, 4, 1, 10, 3, 2, 5, 6]

CAT_SPRITE = {
    "A": "cat_chaser.png",
    "B": "cat_patrol.png",
    "C": "cat_random.png",
    "D": "cat_ambush.png",
}


def read_map(path):
    with open(path, encoding="utf-8") as f:
        lines = [ln.rstrip("\n").rstrip("\r") for ln in f if ln.strip("\r\n")]
    grid = [list(ln.ljust(W))[:W] for ln in lines]
    grid += [list("#" * W) for _ in range(H - len(grid))]
    return grid


def is_wall(grid, r, c):
    if r < 0 or r >= H or c < 0 or c >= W:
        return True
    return grid[r][c] == BORDER


def load_theme(theme_id):
    path = os.path.join(THEMES, theme_id, "theme.json")
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def tile_background(img, theme_id):
    bg_path = os.path.join(THEMES, theme_id, "background.png")
    bg = Image.open(bg_path).convert("RGB")
    for y in range(0, H * CELL, bg.height):
        for x in range(0, W * CELL, bg.width):
            img.paste(bg, (x, y))
    return img


def draw_autotile(img, grid, theme_id, theme):
    atlas = Image.open(os.path.join(THEMES, theme_id, "wall_autotile.png")).convert("RGBA")
    cols = int(theme.get("atlasColumns", 4))
    rows = int(theme.get("atlasRows", 4))
    tw = atlas.width // cols
    th = atlas.height // rows
    for r in range(H + 1):
        for c in range(W + 1):
            mask = (1 if is_wall(grid, r - 1, c - 1) else 0) | \
                   (2 if is_wall(grid, r - 1, c) else 0) | \
                   (4 if is_wall(grid, r, c - 1) else 0) | \
                   (8 if is_wall(grid, r, c) else 0)
            frame = FRAME_BY_MASK[mask]
            if frame < 0:
                continue
            fc, fr = frame % cols, frame // cols
            tile = atlas.crop((fc * tw, fr * th, (fc + 1) * tw, (fr + 1) * th))
            img.paste(tile, (c * CELL - CELL // 2, r * CELL - CELL // 2), tile)


def draw_classic_walls(img, grid, theme_id, theme):
    files = theme.get("wallFiles") or []
    if not files:
        return
    tex = Image.open(os.path.join(THEMES, theme_id, files[0])).convert("RGBA")
    tex = tex.resize((CELL, CELL), Image.LANCZOS)
    for r in range(H):
        for c in range(W):
            if grid[r][c] == BORDER:
                img.paste(tex, (c * CELL, r * CELL), tex)


def paste_sprite(img, sprite_name, col, row, size):
    path = os.path.join(SPRITES, sprite_name)
    spr = Image.open(path).convert("RGBA")
    spr = spr.resize((size, size), Image.LANCZOS)
    cx = col * CELL + CELL // 2
    cy = row * CELL + CELL // 2
    img.paste(spr, (cx - size // 2, cy - size // 2), spr)


def draw_entities(img, grid):
    for r in range(H):
        for c in range(W):
            ch = grid[r][c]
            if ch == "+":
                paste_sprite(img, "mouse.png", c, r, 48)
            elif ch in CAT_SPRITE:
                paste_sprite(img, CAT_SPRITE[ch], c, r, 48)
            elif ch in "123456789":
                paste_sprite(img, "cheese.png", c, r, 48)


def render_map(map_path, theme_id, out_path, label):
    grid = read_map(map_path)
    theme = load_theme(theme_id)
    img = Image.new("RGBA", (W * CELL, H * CELL), (20, 20, 28, 255))
    tile_background(img, theme_id)
    if theme.get("wallAutotile"):
        draw_autotile(img, grid, theme_id, theme)
    else:
        draw_classic_walls(img, grid, theme_id, theme)
    draw_entities(img, grid)

    draw = ImageDraw.Draw(img)
    draw.rectangle([0, 0, 180, 20], fill=(0, 0, 0, 180))
    draw.text((6, 4), label, fill=(255, 255, 255, 255))

    img.convert("RGB").save(out_path)
    return img.size


def worlds():
    result = []
    for name in sorted(os.listdir(MAPS)):
        d = os.path.join(MAPS, name)
        if os.path.isdir(d) and name.isdigit():
            result.append((name, d))
    return result


def render_all():
    count = 0
    for name, d in worlds():
        theme_meta = os.path.join(d, "theme.json")
        theme_id = "grass"
        if os.path.exists(theme_meta):
            with open(theme_meta, encoding="utf-8") as f:
                theme_id = json.load(f).get("theme", "grass")
        for f in sorted(os.listdir(d)):
            if not f.endswith(".txt"):
                continue
            card = f[:-4]
            w, h = render_map(os.path.join(d, f), theme_id, os.path.join(d, card + ".png"),
                              f"{name}/{card}  {theme_id}")
            count += 1
            print(f"maps/{name}/{card}.txt -> maps/{name}/{card}.png {w}x{h} ({theme_id})")

    final_txt = os.path.join(MAPS, "final.txt")
    if os.path.exists(final_txt):
        theme_id = "grass"
        final_json = os.path.join(MAPS, "final.json")
        if os.path.exists(final_json):
            with open(final_json, encoding="utf-8") as f:
                theme_id = json.load(f).get("theme", "grass")
        w, h = render_map(final_txt, theme_id, os.path.join(MAPS, "final.png"),
                          f"final  {theme_id}")
        count += 1
        print(f"maps/final.txt -> maps/final.png {w}x{h} ({theme_id})")

    print("\nTOTAL previews:", count)


def theme_showcase(map_rel="1/1", out_name="map.png"):
    """Рендерит одну карту во всех темах: themes/<id>/view/<out_name>."""
    with open(os.path.join(THEMES, "themes.json"), encoding="utf-8") as f:
        ids = [t["id"] for t in json.load(f)["themes"]]
    map_path = os.path.join(MAPS, *map_rel.split("/")) + ".txt"
    count = 0
    for theme_id in ids:
        out = os.path.join(THEMES, theme_id, "view", out_name)
        os.makedirs(os.path.dirname(out), exist_ok=True)
        w, h = render_map(map_path, theme_id, out, f"{map_rel}  {theme_id}")
        count += 1
        print(f"themes/{theme_id}/view/{out_name} {w}x{h} ({theme_id})")
    print("\nTOTAL:", count)


if __name__ == "__main__":
    import sys
    if "--theme-map" in sys.argv:
        i = sys.argv.index("--theme-map")
        rel = sys.argv[i + 1] if i + 1 < len(sys.argv) else "1/1"
        theme_showcase(rel)
    else:
        render_all()
