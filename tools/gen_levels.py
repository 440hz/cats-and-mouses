# Генератор карт и тем миров для CATS AND MOUSES (dev-only).
#
# Каждый мир — каталог assets/maps/<N>/ с картами <k>.txt (ширина 32, высота 16)
# и theme.json с темой мира. Финал — assets/maps/final.txt + final.json.
# Карты строятся детерминированно (seed), проверяются на связность.
#
# Запуск: python tools/gen_levels.py
# Превью карт (PNG) генерируются отдельно: python tools/map_preview.py

import os
import random
import json
import sys

W = 32
H = 16
BORDER = "#"

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
MAPS = os.path.join(ROOT, "assets", "maps")

# Миры прохождения: тема, число карт, плотность лишних стен (сложность), сыр по картам.
WORLDS = [
    {"id": 1, "theme": "grass",  "density": 0.00, "cheese": [1, 2, 2, 3, 3]},
    {"id": 2, "theme": "castle", "density": 0.05, "cheese": [3, 3, 4, 4, 4]},
    {"id": 3, "theme": "ice",    "density": 0.10, "cheese": [4, 4, 5, 5, 5]},
    {"id": 4, "theme": "desert", "density": 0.15, "cheese": [5, 5, 6, 6, 6]},
    {"id": 5, "theme": "moss",   "density": 0.20, "cheese": [6, 6, 7, 7, 7]},
    {"id": 6, "theme": "swamp",  "density": 0.26, "cheese": [7, 8, 8, 8, 9]},
    {"id": 7, "theme": "volcano", "density": 0.32, "cheese": [8, 8, 9, 9, 9]},
]

FINAL_THEME = "space"

# Буквы КОНЕЦ: 5x7, '#' — стена, ' ' — проход.
LETTERS = {
    "К": ["#   #", "#  # ", "# #  ", "##   ", "# #  ", "#  # ", "#   #"],
    "О": [" ### ", "#   #", "#   #", "#   #", "#   #", "#   #", " ### "],
    "Н": ["#   #", "#   #", "#   #", "#####", "#   #", "#   #", "#   #"],
    "Е": ["#####", "#    ", "#    ", "#### ", "#    ", "#    ", "#####"],
    "Ц": ["#   #", "#   #", "#   #", "#   #", "#   #", "#####", "    #"],
}
WORD = "КОНЕЦ"


def blank_grid():
    return [[BORDER if (r in (0, H - 1) or c in (0, W - 1)) else " " for c in range(W)] for r in range(H)]


def in_bounds(r, c):
    return 0 <= r < H and 0 <= c < W


def is_wall(grid, r, c):
    if not in_bounds(r, c):
        return True
    return grid[r][c] == BORDER


def neighbors(r, c):
    yield r - 1, c
    yield r + 1, c
    yield r, c - 1
    yield r, c + 1


def connected_components(grid):
    seen = [[False] * W for _ in range(H)]
    comps = []
    for r in range(H):
        for c in range(W):
            if grid[r][c] == BORDER or seen[r][c]:
                continue
            stack = [(r, c)]
            seen[r][c] = True
            comp = []
            while stack:
                cr, cc = stack.pop()
                comp.append((cr, cc))
                for nr, nc in neighbors(cr, cc):
                    if in_bounds(nr, nc) and grid[nr][nc] != BORDER and not seen[nr][nc]:
                        seen[nr][nc] = True
                        stack.append((nr, nc))
            comps.append(comp)
    return comps


def keep_largest_component(grid):
    """Оставляет только самую большую связную область, остальные проходы замуровывает."""
    comps = connected_components(grid)
    if not comps:
        return []
    main = max(comps, key=len)
    main_set = set(main)
    for r in range(H):
        for c in range(W):
            if grid[r][c] != BORDER and (r, c) not in main_set:
                grid[r][c] = BORDER
    return main


def protected_corners():
    """Клетки вокруг углов, где не ставим лишние стены (чтобы старты котов были рядом)."""
    zones = set()
    for cr, cc in [(1, 1), (1, W - 2), (H - 2, 1), (H - 2, W - 2)]:
        for dr in range(-1, 3):
            for dc in range(-1, 3):
                if in_bounds(cr + dr, cc + dc):
                    zones.add((cr + dr, cc + dc))
    return zones


def carve_pillars(grid, rng):
    """Регулярная сетка столбов на чётных координатах — база лабиринта; часть убираем для разнообразия."""
    for r in range(2, H - 1, 2):
        for c in range(2, W - 1, 2):
            if rng.random() < 0.15:
                continue  # пропускаем столб — извилистость и разнообразие
            grid[r][c] = BORDER


def add_extra_walls(grid, density, rng):
    if density <= 0:
        return
    zones = protected_corners()
    candidates = []
    for r in range(1, H - 1):
        for c in range(1, W - 1):
            if grid[r][c] == " " and (r, c) not in zones:
                candidates.append((r, c))
    rng.shuffle(candidates)
    for r, c in candidates:
        if rng.random() < density:
            grid[r][c] = BORDER


def nearest_cell(main_set, tr, tc):
    return min(main_set, key=lambda rc: abs(rc[0] - tr) + abs(rc[1] - tc))


def farthest_spread(main_set, count, rng):
    """Раскидывает точки равномерно (farthest-point sampling)."""
    cells = list(main_set)
    first = rng.choice(cells)
    chosen = [first]
    while len(chosen) < count and len(chosen) < len(cells):
        best = None
        best_d = -1
        for cell in cells:
            d = min(abs(cell[0] - ch[0]) + abs(cell[1] - ch[1]) for ch in chosen)
            if d > best_d:
                best_d = d
                best = cell
        chosen.append(best)
    return chosen


def generate_map(density, cheese_count, seed):
    rng = random.Random(seed)
    grid = blank_grid()
    carve_pillars(grid, rng)
    add_extra_walls(grid, density, rng)
    main = keep_largest_component(grid)
    main_set = set(main)

    # Старты котов по углам (A/B/C/D), мышь — ближе к центру.
    a = nearest_cell(main_set, 1, 1)
    b = nearest_cell(main_set, 1, W - 2)
    c = nearest_cell(main_set, H - 2, 1)
    d = nearest_cell(main_set, H - 2, W - 2)
    cat_cells = [a, b, c, d]
    occupied = set(cat_cells)

    mouse = nearest_cell(main_set, H // 2, W // 2)
    if mouse in occupied:
        mouse = next((cell for cell in main if cell not in occupied), mouse)
    occupied.add(mouse)

    free = [cell for cell in main if cell not in occupied]
    cheese_cells = farthest_spread(set(free), min(cheese_count, len(free)), rng)

    for (r, c), ch in zip(cat_cells, "ABCD"):
        grid[r][c] = ch
    grid[mouse[0]][mouse[1]] = "+"
    for i, (r, c) in enumerate(cheese_cells):
        grid[r][c] = str(i % 9 + 1)

    return ["".join(row) for row in grid]


def build_final():
    """Финал: открытое поле, стены-слово КОНЕЦ, старты по углам, мышь в центре сверху."""
    grid = blank_grid()
    start_row = 4
    start_col = 1
    for i, ch in enumerate(WORD):
        glyph = LETTERS[ch]
        col0 = start_col + i * 6
        for dr, line in enumerate(glyph):
            for dc, sym in enumerate(line):
                if sym == "#":
                    grid[start_row + dr][col0 + dc] = BORDER
    for (r, c), ch in zip([(1, 1), (1, W - 2), (H - 2, 1), (H - 2, W - 2)], "ABCD"):
        grid[r][c] = ch
    grid[2][W // 2 - 1] = "+"
    grid[2][W // 2] = "+"
    return ["".join(row) for row in grid]


def validate(lines, cheese_expected):
    assert len(lines) == H, f"высота {len(lines)} != {H}"
    for line in lines:
        assert len(line) == W, f"ширина {len(line)} != {W}"
    grid = [list(line) for line in lines]
    comps = connected_components(grid)
    main = max(comps, key=len) if comps else []
    main_set = set(main)
    cats = [(r, c) for r in range(H) for c in range(W) if grid[r][c] in "ABCD"]
    mice = [(r, c) for r in range(H) for c in range(W) if grid[r][c] == "+"]
    cheese = [(r, c) for r in range(H) for c in range(W) if grid[r][c] in "123456789"]
    assert len(cats) == 4, f"котов {len(cats)}"
    assert len(mice) >= 1, "нет старта мыши"
    start = mice[0] if mice[0] in main_set else (main[0] if main else (1, 1))
    reach = set()
    stack = [start]
    reach.add(start)
    while stack:
        cr, cc = stack.pop()
        for nr, nc in neighbors(cr, cc):
            if in_bounds(nr, nc) and grid[nr][nc] != BORDER and (nr, nc) not in reach:
                reach.add((nr, nc))
                stack.append((nr, nc))
    for cell in cats + mice + cheese:
        assert cell in reach, f"недостижимая клетка {cell}"
    if cheese_expected is not None:
        assert len(cheese) == cheese_expected, f"сыра {len(cheese)} != {cheese_expected}"
    return len(reach)


def write_lines(path, lines):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(lines) + "\n")


def write_theme(level_dir, theme_id):
    os.makedirs(level_dir, exist_ok=True)
    path = os.path.join(level_dir, "theme.json")
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump({"theme": theme_id}, f, ensure_ascii=False)
        f.write("\n")


def main():
    # Фильтр миров: `python tools/gen_levels.py 7` сгенерирует только мир 7
    # (без аргументов — все миры). Фильтр также отключает перегенерацию final,
    # чтобы случайно не затереть правки, сделанные вручную в редакторе.
    selected = {int(arg) for arg in sys.argv[1:] if arg.isdigit()}
    worlds = [w for w in WORLDS if not selected or w["id"] in selected]
    generate_final = not selected

    total = 0
    for world in worlds:
        level_dir = os.path.join(MAPS, str(world["id"]))
        write_theme(level_dir, world["theme"])
        for i, cheese in enumerate(world["cheese"], start=1):
            seed = world["id"] * 1000 + i
            lines = generate_map(world["density"], cheese, seed)
            cells = validate(lines, cheese)
            write_lines(os.path.join(level_dir, f"{i}.txt"), lines)
            total += 1
            print(f"world {world['id']} card {i}: {world['theme']}, cheese={cheese}, reach={cells}")

    if generate_final:
        final_lines = build_final()
        cells = validate(final_lines, None)
        write_lines(os.path.join(MAPS, "final.txt"), final_lines)
        with open(os.path.join(MAPS, "final.json"), "w", encoding="utf-8", newline="\n") as f:
            json.dump({"theme": FINAL_THEME}, f, ensure_ascii=False)
            f.write("\n")
        total += 1
        print(f"final: {FINAL_THEME}, reach={cells}")

    print(f"\nTOTAL maps: {total}")

    # Превью карт (PNG) — если доступен Pillow. Не критично для генерации.
    try:
        import map_preview
        print()
        map_preview.render_all()
    except Exception as error:  # noqa: BLE001
        print(f"\npreviews skipped: {error}")


if __name__ == "__main__":
    main()
