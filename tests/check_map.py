#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""check_map.py - комплексный анализ проходимости одной карты CATS AND MOUSES.

Dev-only: не входит в игровую сборку (APK / jar / dist), см. AGENTS.md.

Запуск:
    python tests/check_map.py --map "C:\\...\\tests\\fixtures\\win_100.txt"
    python tests/check_map.py --map "...\\assets\\maps\\1\\1.txt" --runs 100
    python tests/check_map.py --map "...\\assets\\maps\\final.txt" --json report.json

Что делает:
    1) Статический анализ: формат 16x32, периметр, символы, ровно 4 кота A-D,
       старт(ы) мыши '+', BFS-достижимость сыра/котов, изолированные области,
       тупики и мосты (артикуляционные точки), дистанции.
    2) Симуляция игрового цикла: точный порт GameScreen.updatePlaying + Cat/Mouse
       (стратегии Chaser/Patrol/Random/Ambush, общая резервация клеток, скорости
       150 vs 120, в final - 4 Chaser по 150).
    3) Monte-Carlo на N прогонов (учитывает RNG котов и случайный выбор '+').
    4) Вердикт ДА/НЕТ. Если ДА - маршрут победного прогона.

Зависимости: только стандартная библиотека Python 3.
"""

import argparse
import json
import math
import os
import random
import sys
from collections import deque

try:
    sys.stdout.reconfigure(encoding="utf-8")
except Exception:  # noqa: BLE001
    pass

# --- Константы движка (core/.../Constants.kt) --------------------------------
MAZE_COLS = 32
MAZE_ROWS = 16
CELL_SIZE = 40
MOUSE_SPEED = 150.0
CAT_SPEED = 120.0
CAT_SPEED_FINAL = 150.0
MAX_TIME_DEFAULT = 180.0
INF = 10 ** 9

DIRS = [("UP", -1, 0), ("DOWN", 1, 0), ("LEFT", 0, -1), ("RIGHT", 0, 1)]
DELTA = {name: (dr, dc) for name, dr, dc in DIRS}
OPPOSITE = {"UP": "DOWN", "DOWN": "UP", "LEFT": "RIGHT", "RIGHT": "LEFT", "NONE": "NONE"}
CAT_NAMES = ["Барсик", "Матроскин", "Леопольд", "Черныш"]
CAT_LETTERS = ["A", "B", "C", "D"]


# --- Утилиты сетки -----------------------------------------------------------
def is_wall(grid, r, c):
    if r < 0 or r >= len(grid) or c < 0 or c >= len(grid[0]):
        return True
    return grid[r][c] == "#"


def step(cell, direction):
    dr, dc = DELTA[direction]
    return (cell[0] + dr, cell[1] + dc)


def cell_to_world(cell):
    return (cell[1] * CELL_SIZE, (MAZE_ROWS - 1 - cell[0]) * CELL_SIZE)


def bfs_reachable(grid, start):
    seen = {start}
    dq = deque([start])
    while dq:
        r, c = dq.popleft()
        for _, dr, dc in DIRS:
            n = (r + dr, c + dc)
            if not is_wall(grid, n[0], n[1]) and n not in seen:
                seen.add(n)
                dq.append(n)
    return seen


def bfs_dist_map(grid, sources):
    dist = {}
    dq = deque()
    for s in sources:
        if is_wall(grid, s[0], s[1]):
            continue
        dist[s] = 0
        dq.append(s)
    while dq:
        r, c = dq.popleft()
        d = dist[(r, c)]
        for _, dr, dc in DIRS:
            n = (r + dr, c + dc)
            if not is_wall(grid, n[0], n[1]) and n not in dist:
                dist[n] = d + 1
                dq.append(n)
    return dist


def find_path(grid, start, target):
    """BFS по проходимым клеткам (коты игнорируются, как в Cat.findPath)."""
    if start == target:
        return []
    came = {start: None}
    dq = deque([start])
    while dq:
        cur = dq.popleft()
        if cur == target:
            break
        for _, dr, dc in DIRS:
            n = (cur[0] + dr, cur[1] + dc)
            if not is_wall(grid, n[0], n[1]) and n not in came:
                came[n] = cur
                dq.append(n)
    if target not in came:
        return None
    path = []
    cur = target
    while cur != start:
        path.append(cur)
        cur = came[cur]
    path.reverse()
    return path


# --- Загрузка и разбор карты -------------------------------------------------
class MapData:
    def __init__(self, grid, path):
        self.grid = grid
        self.path = path
        self.plus = [(r, c) for r in range(len(grid)) for c in range(len(grid[0])) if grid[r][c] == "+"]
        self.cats = {}
        for r in range(len(grid)):
            for c in range(len(grid[0])):
                ch = grid[r][c]
                if ch in CAT_LETTERS:
                    self.cats[ch] = (r, c)
        self.cheese = [
            (r, c) for r in range(len(grid)) for c in range(len(grid[0])) if grid[r][c] in "123456789"
        ]
        stem = os.path.splitext(os.path.basename(path))[0].lower()
        self.is_final = stem == "final"
        # Эффективный сыр как в spawnPellets: цифры, иначе старты котов A-D.
        if self.cheese:
            self.cheese_effective = list(self.cheese)
        elif self.cats:
            self.cheese_effective = [self.cats[ch] for ch in CAT_LETTERS if ch in self.cats]
        else:
            self.cheese_effective = []

    @property
    def cat_list(self):
        """Стартовые клетки котов в порядке A,B,C,D (движок требует ровно 4)."""
        return [self.cats[ch] for ch in CAT_LETTERS]


def load_map(path):
    """Читает файл как MazeLoader: строки по \\n, обрезка \\r, паддинг до MAZE_COLS."""
    with open(path, encoding="utf-8") as f:
        raw = f.read()
    lines = [ln.rstrip("\r") for ln in raw.split("\n")]
    lines = [ln for ln in lines if ln != ""]
    width = max((len(ln) for ln in lines), default=0)
    width = max(width, MAZE_COLS)
    grid = []
    for ln in lines:
        row = list(ln.ljust(width, "#"))
        grid.append(row)
    return grid, lines


# --- Сущности: точный порт Mouse / Cat ---------------------------------------
class Mouse:
    def __init__(self, grid, start, speed=MOUSE_SPEED):
        self.grid = grid
        self.cell = tuple(start)
        self.position = list(cell_to_world(self.cell))
        self.direction = "NONE"
        self._next = "NONE"
        self.target = tuple(start)
        self.is_moving = False
        self.speed = speed

    def set_next(self, direction):
        if direction and direction != "NONE":
            self._next = direction

    def _passable(self, direction):
        return not is_wall(self.grid, *step(self.cell, direction))

    def _choose(self):
        if self._next != "NONE" and self._passable(self._next):
            return self._next
        if self.direction != "NONE" and self._passable(self.direction):
            return self.direction
        return "NONE"

    def _try_start(self):
        if self._next != "NONE" and self._passable(self._next):
            self.direction = self._next
            self.target = step(self.cell, self.direction)
            self.is_moving = True

    def update(self, dt):
        if not self.is_moving:
            self._try_start()
            return
        target = cell_to_world(self.target)
        dx = target[0] - self.position[0]
        dy = target[1] - self.position[1]
        dist = math.hypot(dx, dy)
        move = self.speed * dt
        if move >= dist:
            self.position = list(target)
            self.cell = self.target
            chosen = self._choose()
            if chosen != "NONE" and self._passable(chosen):
                self.direction = chosen
                self.target = step(self.cell, chosen)
                self.is_moving = True
            else:
                self.direction = "NONE"
                self.is_moving = False
        else:
            self.position[0] += dx * (move / dist)
            self.position[1] += dy * (move / dist)


class Cat:
    def __init__(self, grid, start, strategy, name, speed=CAT_SPEED):
        self.grid = grid
        self.cell = tuple(start)
        self.position = list(cell_to_world(self.cell))
        self.direction = "NONE"
        self.target = tuple(start)
        self.is_moving = False
        self.time_since_last_move = 0.0
        self.strategy = strategy
        self.name = name
        self.speed = speed
        self.rng = random

    def _passable(self, direction):
        return not is_wall(self.grid, *step(self.cell, direction))

    def update(self, dt, mouse_cell, mouse_direction, reserved):
        if not self.is_moving:
            self._try_start(mouse_cell, mouse_direction, reserved)
            if self.is_moving:
                self.time_since_last_move = 0.0
                reserved.add(self.target)
            else:
                self.time_since_last_move += dt
            return

        target = cell_to_world(self.target)
        dx = target[0] - self.position[0]
        dy = target[1] - self.position[1]
        dist = math.hypot(dx, dy)
        move = self.speed * dt
        if move >= dist:
            self.position = list(target)
            self.cell = self.target
            self.time_since_last_move = 0.0
            chosen = self._choose(mouse_cell, mouse_direction, reserved)
            if chosen != "NONE" and self._passable(chosen):
                self.direction = chosen
                self.target = step(self.cell, chosen)
                self.is_moving = True
                reserved.add(self.target)
            else:
                self.direction = "NONE"
                self.is_moving = False
        else:
            self.position[0] += dx * (move / dist)
            self.position[1] += dy * (move / dist)
            self.time_since_last_move = 0.0

    def _try_start(self, mouse_cell, mouse_direction, reserved):
        direction = self._choose(mouse_cell, mouse_direction, reserved)
        if direction != "NONE" and self._passable(direction):
            self.direction = direction
            self.target = step(self.cell, direction)
            self.is_moving = True

    def _choose(self, mouse_cell, mouse_direction, reserved):
        all_options = []
        free_options = []
        for name, _, _ in DIRS:
            if name == OPPOSITE[self.direction]:
                continue
            n = step(self.cell, name)
            if not is_wall(self.grid, n[0], n[1]):
                all_options.append(name)
                if n not in reserved:
                    free_options.append(name)

        if free_options:
            options = free_options
        elif self.time_since_last_move > 1.0:
            options = all_options
        else:
            options = []

        if not options:
            reverse = OPPOSITE[self.direction]
            if reverse != "NONE" and self._passable(reverse):
                return reverse
            return "NONE"

        if self.strategy == "Chaser":
            return self._bfs_direction(mouse_cell, options) or self.rng.choice(options)
        if self.strategy == "Patrol":
            distance = self._bfs_distance(self.cell, mouse_cell)
            if distance is not None and distance <= 5:
                return self._bfs_direction(mouse_cell, options) or self.rng.choice(options)
            scores = []
            for name in options:
                n = step(self.cell, name)
                walls = sum(
                    1 for d, _, _ in DIRS if is_wall(self.grid, *step(n, d))
                )
                scores.append((name, walls))
            best = max(w for _, w in scores)
            return self.rng.choice([name for name, w in scores if w == best])
        if self.strategy == "Random":
            if self.rng.random() < 0.3:
                return self._bfs_direction(mouse_cell, options) or self.rng.choice(options)
            return self.rng.choice(options)
        if self.strategy == "Ambush":
            lookahead = 2 if mouse_direction != "NONE" else 0
            dr, dc = DELTA.get(mouse_direction, (0, 0))
            target = (mouse_cell[0] + dr * lookahead, mouse_cell[1] + dc * lookahead)
            return self._bfs_direction(target, options) or self.rng.choice(options)
        return self.rng.choice(options)

    def _bfs_direction(self, target, options):
        path = find_path(self.grid, self.cell, target)
        if not path:
            return None
        nxt = path[0]
        delta = (nxt[0] - self.cell[0], nxt[1] - self.cell[1])
        for name in options:
            if DELTA[name] == delta:
                return name
        return None

    def _bfs_distance(self, start, target):
        path = find_path(self.grid, start, target)
        if path is None:
            return None
        return len(path)


# --- Спавн сыра (порт GameScreen.spawnPellets) -------------------------------
def spawn_pellets(grid, mouse_start, cat_starts, manual_cells, rng):
    reachable = bfs_reachable(grid, mouse_start)
    occupied = {mouse_start}
    occupied.update(cat_starts)

    pellets = []
    if manual_cells:
        replaced = 0
        for cell in manual_cells:
            if cell not in reachable or cell in occupied:
                replaced += 1
            else:
                pellets.append(cell)
        used = set(occupied) | set(pellets)
        available = [c for c in reachable if c not in used]
        rng.shuffle(available)
        pellets.extend(available[: min(replaced, len(available))])
    elif cat_starts:
        for cell in cat_starts:
            if cell in reachable:
                pellets.append(cell)
    else:
        available = [c for c in reachable if c not in occupied]
        rng.shuffle(available)
        pellets.extend(available[:3])
    return pellets


# --- Политика мыши (бот) -----------------------------------------------------
def ai_decide(grid, cell, cat_cells, cheese_sources, radius, current_dir):
    """Возвращает (direction, dist_to_nearest_cat). Идёт к ближайшему сыру,
    при коте ближе radius - уходит по максимальной BFS-дистанции от котов."""
    dcat = bfs_dist_map(grid, cat_cells)
    sources = [c for c in cheese_sources if c != cell] or list(cheese_sources)
    dcheese = bfs_dist_map(grid, sources) if sources else {}

    candidates = []
    for name, _, _ in DIRS:
        n = step(cell, name)
        if not is_wall(grid, n[0], n[1]):
            candidates.append((name, n))

    nearest_cat = dcat.get(cell, INF)
    if not candidates:
        return "NONE", nearest_cat

    if nearest_cat <= radius:
        best = max(
            candidates,
            key=lambda t: (
                dcat.get(t[1], INF),
                -dcheese.get(t[1], INF),
                1 if t[0] == current_dir else 0,
            ),
        )
        return best[0], nearest_cat

    target_d = dcheese.get(cell, INF)
    goal = target_d - 1
    options = [(name, n) for name, n in candidates if dcheese.get(n, INF) == goal]
    if not options:
        options = [min(candidates, key=lambda t: dcheese.get(t[1], INF))]
    best = max(
        options,
        key=lambda t: (dcat.get(t[1], INF), 1 if t[0] == current_dir else 0),
    )
    return best[0], nearest_cat


# --- Один прогон симуляции ---------------------------------------------------
def simulate(grid, data, rng, dt, radius, max_time):
    start = rng.choice(data.plus)
    cat_starts = data.cat_list
    if data.is_final:
        strategies = ["Chaser"] * 4
        speed = CAT_SPEED_FINAL
    else:
        strategies = ["Chaser", "Patrol", "Random", "Ambush"]
        speed = CAT_SPEED

    mouse = Mouse(grid, start)
    cats = [Cat(grid, cat_starts[i], strategies[i], CAT_NAMES[i], speed) for i in range(4)]
    pellets = set(spawn_pellets(grid, start, cat_starts, data.cheese, rng))
    uneaten = set(pellets)

    t = 0.0
    margin = INF
    path = [tuple(mouse.cell)]
    cheese_order = []
    last_decision = None
    last_dir = "NONE"
    outcome = "TIMEOUT"
    catcher = None

    while t < max_time:
        decision_cell = tuple(mouse.target) if mouse.is_moving else tuple(mouse.cell)
        if decision_cell != last_decision:
            sources = set(uneaten)
            sources.discard(decision_cell)
            if not sources:
                sources = set(uneaten)
            last_dir, nearest = ai_decide(
                grid, decision_cell, [tuple(c.cell) for c in cats], sources, radius, mouse.direction
            )
            last_decision = decision_cell
            margin = min(margin, nearest)

        mouse.set_next(last_dir)
        mouse.update(dt)
        if tuple(mouse.cell) != path[-1]:
            path.append(tuple(mouse.cell))

        hit = next((c for c in cats if tuple(c.cell) == tuple(mouse.cell)), None)
        if hit is not None:
            outcome = "CAUGHT"
            catcher = hit.name
            break

        if tuple(mouse.cell) in uneaten:
            uneaten.discard(tuple(mouse.cell))
            cheese_order.append(tuple(mouse.cell))

        reserved = {tuple(c.cell) for c in cats}
        for cat in cats:
            cat.update(dt, tuple(mouse.cell), mouse.direction, reserved)
            reserved.add(tuple(cat.cell))

        hit = next((c for c in cats if tuple(c.cell) == tuple(mouse.cell)), None)
        if hit is not None:
            outcome = "CAUGHT"
            catcher = hit.name
            break

        if not uneaten:
            outcome = "WIN"
            break

        t += dt

    return {
        "outcome": outcome,
        "time": round(t, 2),
        "catcher": catcher,
        "start": start,
        "margin": margin,
        "path": path,
        "cheese_order": cheese_order,
        "cheese_total": len(pellets),
        "cheese_eaten": len(cheese_order),
    }


# --- Статический анализ ------------------------------------------------------
def analyze_static(grid, lines, data):
    problems = []
    warnings = []

    if len(lines) != MAZE_ROWS:
        problems.append(f"высота {len(lines)}, ожидается {MAZE_ROWS}")
    bad_rows = [i for i, ln in enumerate(lines) if len(ln) != MAZE_COLS]
    if bad_rows:
        problems.append(f"ширина != {MAZE_COLS} в строках {bad_rows[:8]}")

    allowed = set("# 123456789+ABCDH")
    unknown = sorted({ch for row in grid for ch in row} - allowed)
    if unknown:
        problems.append(f"неизвестные символы: {unknown}")

    for c in range(MAZE_COLS):
        if grid[0][c] != "#" or grid[MAZE_ROWS - 1][c] != "#":
            problems.append("верхний/нижний периметр не замкнут")
            break
    for r in range(MAZE_ROWS):
        if grid[r][0] != "#" or grid[r][MAZE_COLS - 1] != "#":
            problems.append("левый/правый периметр не замкнут")
            break

    if not data.plus:
        problems.append("нет старта мыши '+'")
    if len(data.cats) != 4:
        problems.append(f"стартов котов {len(data.cats)}, ожидается 4 (A-D)")
    if not data.cheese_effective:
        warnings.append("сыр задан неявно: движок поставит 3 случайные клетки (статически не проверить)")

    start_reach = {}
    for p in data.plus:
        start_reach[p] = bfs_reachable(grid, p)

    if data.plus:
        good = [p for p in data.plus if all(c in start_reach[p] for c in data.cheese_effective)]
        if not good:
            problems.append("ни один старт '+' не достигает всего сыра")
        elif len(good) < len(data.plus):
            warnings.append(f"не все '+' достигают всего сыра ({len(good)}/{len(data.plus)})")

        for p, reach in start_reach.items():
            for cell in data.cat_list:
                if cell not in reach:
                    warnings.append(f"кот {cell} недостижим от старта {p}")

    all_passable = [(r, c) for r in range(MAZE_ROWS) for c in range(MAZE_COLS) if grid[r][c] != "#"]
    comps = []
    seen = set()
    for cell in all_passable:
        if cell in seen:
            continue
        comp = bfs_reachable(grid, cell)
        seen |= comp
        comps.append(comp)
    isolated = []
    if data.plus:
        isolated = [comp for comp in comps if not (comp & set(data.plus))]

    ref_start = data.plus[0] if data.plus else (1, 1)
    reach = start_reach.get(ref_start, bfs_reachable(grid, ref_start))
    dead_ends = [
        cell
        for cell in reach
        if sum(1 for name, _, _ in DIRS if not is_wall(grid, *step(cell, name))) == 1
    ]
    arts = articulation_points(grid, reach)

    dstart = bfs_dist_map(grid, [ref_start]) if data.plus else {}
    return {
        "problems": problems,
        "warnings": warnings,
        "plus": data.plus,
        "cats": data.cat_list,
        "cheese": data.cheese_effective,
        "reachable": len(reach),
        "passable": len(all_passable),
        "isolated_components": len(isolated),
        "isolated_sizes": sorted((len(comp) for comp in isolated), reverse=True)[:8],
        "dead_ends": dead_ends,
        "articulation": arts,
        "dist_start_cheese": {str(c): dstart.get(c) for c in data.cheese_effective},
        "dist_start_cats": {str(c): dstart.get(c) for c in data.cat_list},
    }


def articulation_points(grid, reach):
    base = len(reach)
    arts = []
    cells = list(reach)
    for cell in cells:
        rest = [c for c in cells if c != cell]
        if not rest:
            continue
        start = rest[0]
        seen = {start}
        dq = deque([start])
        while dq:
            cur = dq.popleft()
            for _, dr, dc in DIRS:
                n = (cur[0] + dr, cur[1] + dc)
                if n == cell or is_wall(grid, n[0], n[1]) or n in seen:
                    continue
                seen.add(n)
                dq.append(n)
        if len(seen) < base - 1:
            arts.append(cell)
    return arts


# --- Печать ------------------------------------------------------------------
def compress_path(path):
    commands = []
    for i in range(1, len(path)):
        prev, cur = path[i - 1], path[i]
        dr, dc = cur[0] - prev[0], cur[1] - prev[1]
        name = next((n for n, d in DELTA.items() if d == (dr, dc)), "?")
        if commands and commands[-1][0] == name:
            commands[-1][1] += 1
        else:
            commands.append([name, 1])
    return [f"{name}x{count}" for name, count in commands]


def render_route(grid, path, start, cheese_order):
    cells = {cell: "*" for cell in path}
    for cell in cheese_order:
        cells[cell] = "x"
    cells[start] = "+"
    out = []
    for r in range(MAZE_ROWS):
        row = []
        for c in range(MAZE_COLS):
            cell = (r, c)
            if cell in cells:
                row.append(cells[cell])
            else:
                row.append(grid[r][c])
        out.append("".join(row))
    return out


def fmt(value):
    return "-" if value is None else str(value)


def main():
    parser = argparse.ArgumentParser(description="Комплексный анализ проходимости одной карты")
    parser.add_argument("--map", required=True, help="полный путь к .txt карты")
    parser.add_argument("--runs", type=int, default=100, help="число прогонов симуляции")
    parser.add_argument("--seed", type=int, default=0, help="базовый seed")
    parser.add_argument("--dt", type=float, default=1.0 / 60.0, help="шаг симуляции, c")
    parser.add_argument("--R", dest="radius", type=int, default=3, help="радиус реакции мыши на кота")
    parser.add_argument("--max-time", type=float, default=MAX_TIME_DEFAULT, help="лимит партии, c")
    parser.add_argument("--json", dest="json_path", default=None, help="сохранить отчёт в JSON")
    parser.add_argument("--no-progress", action="store_true", help="не показывать прогрессбар симуляции")
    args = parser.parse_args()

    if not os.path.isfile(args.map):
        print(f"Файл не найден: {args.map}")
        return 1

    grid, lines = load_map(args.map)
    data = MapData(grid, args.map)

    st = analyze_static(grid, lines, data)
    report = {
        "map": args.map,
        "static": {
            "ok": not st["problems"],
            "problems": st["problems"],
            "warnings": st["warnings"],
        },
    }

    # Симуляция считается до печати, чтобы карту (с маршрутом, если есть победа)
    # можно было показать сразу после строки «режим», а не в конце.
    sim_ok = not st["problems"]
    results = []
    wins = []
    caught = []
    timeouts = []
    catcher_stats = {}
    win_rate = 0.0
    best = None
    if sim_ok:
        show_progress = not args.no_progress and args.runs > 0
        if show_progress:
            print(f"Симуляция: {args.runs} прогонов...", file=sys.stderr)
        for i in range(args.runs):
            rng = random.Random(args.seed + i)
            results.append(simulate(grid, data, rng, args.dt, args.radius, args.max_time))
            if show_progress:
                done = i + 1
                pct = done * 100 // args.runs
                filled = pct * 20 // 100
                bar = "#" * filled + "-" * (20 - filled)
                print(
                    f"\rСимуляция [{bar}] {pct:3d}% ({done}/{args.runs})",
                    end="",
                    file=sys.stderr,
                    flush=True,
                )
        if show_progress:
            print(file=sys.stderr, flush=True)
        wins = [r for r in results if r["outcome"] == "WIN"]
        caught = [r for r in results if r["outcome"] == "CAUGHT"]
        timeouts = [r for r in results if r["outcome"] == "TIMEOUT"]
        win_rate = len(wins) / len(results) * 100 if results else 0.0
        for r in caught:
            catcher_stats[r["catcher"]] = catcher_stats.get(r["catcher"], 0) + 1
        if wins:
            best = max(wins, key=lambda r: (r["margin"], -r["time"]))

    def save_json():
        if args.json_path:
            with open(args.json_path, "w", encoding="utf-8") as f:
                json.dump(report, f, ensure_ascii=False, indent=2)

    # Шапка
    print("=" * 64)
    print(f"КАРТА: {args.map}")
    print(f"режим: {'final (4 Chaser, speed 150)' if data.is_final else 'обычный (speed 120)'}")
    print("=" * 64)

    # Карта — сразу после строки «режим»
    if best is not None:
        print("\nКАРТА (* путь мыши, + старт, x съеденный сыр):")
        for row in render_route(grid, best["path"], best["start"], best["cheese_order"]):
            print("  " + row)
    else:
        print("\nКАРТА (# стена, пробел проход, 1-9 сыр, + старт, A-D коты):")
        for row in grid:
            print("  " + "".join(row))

    # 1) статика
    print("\n[1] СТАТИЧЕСКИЙ АНАЛИЗ")
    print(f"  размер: {len(lines)}x{MAZE_COLS}")
    print(f"  старты мыши '+': {st['plus']}")
    print(f"  коты A-D: {st['cats']}")
    cheese_src = "цифры 1-9" if data.cheese else ("старты котов A-D" if data.cats else "случайные 3 клетки")
    print(f"  сыр: {st['cheese']} (источник: {cheese_src})")
    print(f"  достижимо: {st['reachable']}/{st['passable']} проходных клеток")
    print(f"  тупиков: {len(st['dead_ends'])}; мостов (артик. точек): {len(st['articulation'])}")
    if st["isolated_components"]:
        print(f"  изолированных областей: {st['isolated_components']} (размеры {st['isolated_sizes']})")
    for p, dist in st["dist_start_cheese"].items():
        print(f"    дистанция {st['plus'][0] if st['plus'] else '?'} -> сыр {p}: {fmt(dist)}")
    if st["problems"]:
        for p in st["problems"]:
            print(f"  FAIL: {p}")
    if st["warnings"]:
        for w in st["warnings"]:
            print(f"  WARN: {w}")
    if not st["problems"] and not st["warnings"]:
        print("  статика: OK")

    if not sim_ok:
        print("\n[2] СИМУЛЯЦИЯ: пропущена (статическая ошибка)")
        print("\nВЕРДИКТ: НЕТ (карта невалидна)")
        report["verdict"] = "NO"
        save_json()
        return 1

    # 2) Monte-Carlo
    print(f"\n[2] СИМУЛЯЦИЯ: {args.runs} прогонов (dt={args.dt:.5f}, R={args.radius})")
    print(f"  победы: {len(wins)}/{len(results)} ({win_rate:.1f}%)")
    print(f"  поимки: {len(caught)}; таймауты: {len(timeouts)}")
    for name, count in sorted(catcher_stats.items(), key=lambda kv: -kv[1]):
        print(f"    {name}: {count}")
    if wins:
        times = sorted(r["time"] for r in wins)
        print(f"  время победы: min={times[0]:.2f} c, median={times[len(times) // 2]:.2f} c, max={times[-1]:.2f} c")
    report["simulation"] = {
        "runs": len(results),
        "wins": len(wins),
        "caught": len(caught),
        "timeouts": len(timeouts),
        "win_rate": round(win_rate, 2),
        "catchers": catcher_stats,
    }

    # 3) вердикт и маршрут
    print("\n[3] ВЕРДИКТ")
    if not wins:
        print("  НЕТ - победных прогонов не найдено")
        print("  (это результат бота, а не доказательство непроходимости)")
        report["verdict"] = "NO"
        save_json()
        return 1

    print("  ДА - карта проходима (есть победные прогоны)")
    print(f"\n[4] МАРШРУТ (лучший победный прогон: seed={args.seed}..{args.seed + args.runs - 1})")
    print(f"  старт мыши: {best['start']}")
    print(f"  порядок сыра: {best['cheese_order']}")
    print(f"  время: {best['time']:.2f} c; запас до котов: {best['margin']} клеток")
    print(f"  команд: {' '.join(compress_path(best['path']))}")

    report["verdict"] = "YES"
    report["route"] = {
        "start": list(best["start"]),
        "cheese_order": [list(c) for c in best["cheese_order"]],
        "time": best["time"],
        "margin": best["margin"],
        "commands": compress_path(best["path"]),
        "path": [list(c) for c in best["path"]],
        "reliability": round(win_rate, 2),
    }

    save_json()
    if args.json_path:
        print(f"\nJSON: {args.json_path}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
