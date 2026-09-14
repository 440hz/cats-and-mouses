r"""Тестовый скрипт для проверки AI-режима игры.

Схема теста:
    RESET                -> мышь в стартовой точке
    LEFT  до стены
    DOWN  до стены
    RIGHT до стены
    UP    до стартовой точки

Мышь движется по клеткам и останавливается только упёршись в стену
(direction == NONE). Тест после каждой команды поллит состояние и ждёт,
пока мышь остановится на стене, после чего фиксирует позицию.

Запуск (из папки mcp-server):
    py -3.12 test_ai_mode.py

Требует запущенной игры: .\gradlew lwjgl3:run --args="--ai-mode 8080"
"""

import time
import requests

GAME_URL = "http://localhost:8080"
DELTA = {
    "UP": (0, -1),
    "LEFT": (-1, 0),
    "DOWN": (0, 1),
    "RIGHT": (1, 0),
}


def log(message: str) -> None:
    print(f"[{time.time():.2f}] {message}", flush=True)


def get_state() -> dict:
    r = requests.get(f"{GAME_URL}/state", timeout=5)
    r.raise_for_status()
    return r.json()


def send(action: str, **extra) -> None:
    body = {"action": action, **extra}
    r = requests.post(f"{GAME_URL}/input", json=body, timeout=5)
    r.raise_for_status()


def dump(label: str, st: dict) -> None:
    m = st["mouse"]
    log(
        f"{label} state={st['game_state']} mouse=({m['x']},{m['y']}) dir={m['direction']} "
        f"cheese={st['cheese_collected']}/{st['cheese_total']} lives={st['lives']} score={st['score']}"
    )


def reset_to_playing(timeout: float = 5.0) -> dict | None:
    """ОДИН RESET в начале теста, ждём PLAYING на чистой карте."""
    deadline = time.time() + timeout
    while time.time() < deadline:
        try:
            send("reset")
            log("SENT reset")
        except requests.RequestException as e:
            log(f"WARN reset failed ({e}), retrying")
        time.sleep(0.25)
        st = get_state()
        if st["game_state"] == "PLAYING" and st["cheese_collected"] == 0:
            return st
        log(f"WARN reset: state={st['game_state']} cheese={st['cheese_collected']}, waiting")
    return None


def move_to_wall(direction: str, start_cell: tuple, timeout: float = 8.0):
    """Отправляет move в направлении и ждёт остановки мыши на стене.

    Возвращает (final_cell, st). final_cell — тупл (x, y).
    """
    send("move", direction=direction)
    log(f"SENT move {direction}")

    deadline = time.time() + timeout
    last_cell = start_cell
    moved = False
    stopped_since = None

    while time.time() < deadline:
        st = get_state()
        if st["game_state"] != "PLAYING":
            log(f"WARN {direction}: игра в состоянии {st['game_state']}")
            return None, st

        m = st["mouse"]
        cell = (m["x"], m["y"])
        if cell != last_cell:
            moved = True
            last_cell = cell

        if m["direction"] == "NONE":
            if stopped_since is None:
                stopped_since = time.time()
            elif time.time() - stopped_since >= 0.12:
                return last_cell, st
        else:
            stopped_since = None

        time.sleep(0.05)

    log(f"WARN {direction}: таймаут, мышь не остановилась (moved={moved})")
    return last_cell, st


def main() -> None:
    before = reset_to_playing()
    if before is None:
        log("ERROR: не удалось сбросить уровень в PLAYING")
        return
    dump("BEFORE", before)
    start_cell = (before["mouse"]["x"], before["mouse"]["y"])

    cell = start_cell
    previous_cell = (None, None)
    ok = True
    for direction in ("LEFT", "DOWN", "RIGHT", "UP"):
        prev_x, prev_y = previous_cell if previous_cell != (None, None) else start_cell
        cell, st = move_to_wall(direction, cell)
        if cell is None:
            log(f"{direction}: НЕ ПОДТВЕРЖДЁН (состояние {st['game_state']})")
            ok = False
            break

        dx, dy = DELTA[direction]
        moved_in_direction = (cell[0] - prev_x) * dx >= 1 or (cell[1] - prev_y) * dy >= 1
        dump(f"STOP {direction}", st)
        if moved_in_direction:
            log(f"{direction}: OK, мышь дошла до стены ({cell[0]},{cell[1]})")
        else:
            log(
                f"{direction}: FAIL, мышь не сдвинулась в направлении "
                f"(была ({prev_x},{prev_y}), теперь ({cell[0]},{cell[1]}))"
            )
            ok = False

        previous_cell = cell

    print("\n=== ИТОГ ===")
    if ok:
        print(f"Все направления работают, финальная позиция: ({cell[0]},{cell[1]})")
        print("UP вернул мышь к стартовой точке" if cell == start_cell else f"НО: старт был ({start_cell[0]},{start_cell[1]})")
    else:
        print("ЕСТЬ ПРОБЛЕМЫ — смотри лог")

    st = get_state()
    dump("FINAL", st)


if __name__ == "__main__":
    main()