import os
from PIL import Image, ImageDraw

# Превью спрайтов: те же игровые спрайты, но прозрачный фон заменён
# чёрно-белой шахматкой (1 клетка ~ 1 пиксель оригинала, масштаб ~до 512px),
# чтобы визуально видеть границы холста и контент. Dev-only, для глаз разработчика.
# Каталоги assets/**/view/ в .gitignore — в git и в игру не попадают.
# Запуск: python tools/make_view.py

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "assets"))


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
    scale = max(2, -(-512 // max(w, h)))
    outw, outh = w * scale, h * scale
    cell = max(4, scale)
    checker = make_checker(outw, outh, cell).convert("RGBA")
    content = img.resize((outw, outh), Image.NEAREST)
    combined = Image.alpha_composite(checker, content)
    os.makedirs(os.path.dirname(view_path), exist_ok=True)
    combined.save(view_path)
    return w, h, outw, outh


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
            w, h, outw, outh = make_view(game, view)
            found.append((f, w, h, outw, outh))

for f, w, h, outw, outh in found:
    print(f"{f}: {w}x{h} -> view/{f} {outw}x{outh}")
print("\nTOTAL:", len(found))