import os
from PIL import Image, ImageDraw

# Превью спрайтов: те же игровые спрайты в том же размере (1:1, без увеличения),
# но прозрачный фон заменён чёрно-белой шахматкой (клетка 4px),
# чтобы визуально видеть весь холст и как контент в нём лежит. Dev-only.
# Каталоги assets/**/view/ хранятся в git, но в игровую сборку не попадают.
# Запуск: python tools/make_view.py

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "assets"))
CELL = 4


def make_checker(w, h, cell):
    img = Image.new("RGB", (w, h))
    d = ImageDraw.Draw(img)
    for cy in range(0, h, cell):
        for cx in range(0, w, cell):
            fill = (0, 0, 0) if ((cx // cell) + (cy // cell)) % 2 == 0 else (255, 255, 255)
            d.rectangle([cx, cy, cx + cell - 1, cy + cell - 1], fill=fill)
    return img


def make_view(game_path, view_path):
    img = Image.open(game_path).convert("RGBA")
    w, h = img.size
    checker = make_checker(w, h, CELL).convert("RGBA")
    combined = Image.alpha_composite(checker, img)
    os.makedirs(os.path.dirname(view_path), exist_ok=True)
    combined.save(view_path)
    return w, h


found = []
for dirpath, dirnames, filenames in os.walk(ROOT):
    if os.path.basename(dirpath) == "raw":
        parent = os.path.dirname(dirpath)
        for f in sorted(filenames):
            if not f.lower().endswith(".png"):
                continue
            game = os.path.join(parent, f)
            if not os.path.exists(game):
                continue
            view = os.path.join(parent, "view", f)
            w, h = make_view(game, view)
            found.append((f, w, h))

for f, w, h in found:
    print(f"{f}: {w}x{h} -> view/{f} {w}x{h}")
print("\nTOTAL:", len(found))