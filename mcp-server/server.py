from fastmcp import FastMCP
import requests
import os

GAME_URL = os.environ.get("GAME_URL", "http://localhost:8080")

mcp = FastMCP("cats-and-mouses")

@mcp.tool()
def get_game_state() -> str:
    """Получить текущее состояние игры: позиции мыши, котов, сыр, счёт, жизни."""
    try:
        r = requests.get(f"{GAME_URL}/state", timeout=5)
        r.raise_for_status()
        return r.text
    except requests.RequestException as e:
        return f"ERROR: не удалось получить состояние игры: {e}"

@mcp.tool()
def send_move(direction: str) -> str:
    """Отправить команду движения мыши.
    Args:
        direction: одно из UP, DOWN, LEFT, RIGHT
    """
    direction = direction.upper()
    if direction not in ("UP", "DOWN", "LEFT", "RIGHT"):
        return f"ERROR: неверное направление '{direction}'. Используй UP, DOWN, LEFT, RIGHT."
    try:
        r = requests.post(
            f"{GAME_URL}/input",
            json={"action": "move", "direction": direction},
            timeout=5,
        )
        r.raise_for_status()
        return f"OK: движение {direction} отправлено"
    except requests.RequestException as e:
        return f"ERROR: не удалось отправить команду: {e}"

@mcp.tool()
def toggle_pause() -> str:
    """Поставить игру на паузу или снять с паузы."""
    try:
        r = requests.post(
            f"{GAME_URL}/input",
            json={"action": "pause"},
            timeout=5,
        )
        r.raise_for_status()
        return "OK: пауза переключена"
    except requests.RequestException as e:
        return f"ERROR: не удалось переключить паузу: {e}"

@mcp.tool()
def reset_level() -> str:
    """Перезапустить текущий уровень."""
    try:
        r = requests.post(
            f"{GAME_URL}/input",
            json={"action": "reset"},
            timeout=5,
        )
        r.raise_for_status()
        return "OK: уровень перезапущен"
    except requests.RequestException as e:
        return f"ERROR: не удалось перезапустить уровень: {e}"

if __name__ == "__main__":
    mcp.run()