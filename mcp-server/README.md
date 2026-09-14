# MCP-сервер для игры CATS AND MOUSES

Мост между HTTP-сервером игры и AI-агентом в OpenCode.

## Установка

1. Установить Python 3.10+.
2. Установить зависимости:
   pip install -r requirements.txt

## Запуск

1. Запустить игру с AI-режимом:
   ./gradlew lwjgl3:run --args="--ai-mode 8080"

2. Запустить MCP-сервер:
   python server.py

## Инструменты

- get_game_state() — состояние игры
- send_move(direction) — движение мыши (UP/DOWN/LEFT/RIGHT)
- toggle_pause() — пауза
- reset_level() — рестарт уровня

## Переменные окружения

- GAME_URL — адрес HTTP-сервера игры (по умолчанию http://localhost:8080)