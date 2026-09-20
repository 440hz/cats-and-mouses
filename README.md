# CATS AND MOUSES

2D-игра в стиле **Pac-Man** с инверсией ролей. Игрок управляет **мышью**, а **четыре кота** охотятся за ней. Цель — собрать весь сыр в лабиринте, не попавшись котам.

![Статус сборки](https://img.shields.io/badge/build-passing-brightgreen) ![Платформы](https://img.shields.io/badge/platforms-Android%20%7C%20Desktop-blue) ![Версия](https://img.shields.io/badge/version-1.0.24-blue) ![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-orange) ![LibGDX](https://img.shields.io/badge/LibGDX-1.12.1-purple)

## ⚙️ Технический стек

- **Язык**: Kotlin 1.9.24
- **Движок**: LibGDX 1.12.1
- **Платформы**: Android (minSdk 24, targetSdk 35) + Desktop (LWJGL3)
- **Сборка**: Gradle 8.9, Kotlin DSL, JDK 21

## 🐭 Как играть

- **Мышь** двигается строго по сетке, поворот — только в центре клетки.
- **Цель** — съесть весь сыр на карте, собрав который открываешь переход к следующей карте (прогрессия 1 → 2 → 3 → 4 → финальная).
- **Сыр** расставляется вручную цифрами `1`–`9` на карте; если цифр нет, сыр спавнится на стартовых клетках котов.
- **Увернуться** от четырёх котов с разными стратегиями:

| Кот | Стратегия |
|-----|-----------|
| **Chaser** | Идёт к мыши кратчайшим путём (BFS) |
| **Patrol** | Ходит по периметру; атакует, если мышь рядом |
| **Random** | Блуждает случайно, с шансом 30% — за мышью |
| **Ambush** | Перехватывает по направлению движения мыши |

Смерть мыши пока не реализована — этап «Жизни и рестарт» в планах.

### Управление

| Платформа | Управление |
|-----------|------------|
| **Desktop** | `W`/`S`/`A`/`D` или стрелки; **Space** — пауза |
| **Android** | свайпы в нужную сторону (порог 40 px) |

### Отладка (Desktop)

- `.\gradlew lwjgl3:run --args="--debug"` — включить логи LibGDX.
- `.\gradlew lwjgl3:run --args="--ai-mode [PORT]"` — AI-режим: встроенный HTTP-сервер с эндпоинтами `GET /state` и `POST /input` (команды `move`, `pause`, `reset`).
- MCP-сервер для управления игрой из OpenCode — в `mcp-server/` (FastMCP): `get_game_state`, `send_move`, `toggle_pause`, `reset_level`.

## 🚀 Запуск

### Desktop
```powershell
.\gradlew lwjgl3:run
```

### Android
```powershell
.\gradlew android:installDebug
```
Требуется подключённое устройство или эмулятор.

### Полная сборка
```powershell
.\gradlew build
```

> При ошибке кэша Gradle (`Could not read workspace metadata`) — удали содержимое `%USERPROFILE%\.gradle\caches\8.9\transforms` и собери с `.\gradlew build --no-daemon`.

## 📁 Структура

```
core/        Основная игровая логика (MainGame.kt, GameScreen.kt, MazeLoader.kt, ThemeLoader.kt, entities/)
android/     Android-модуль (иконка, нарезка mipmap)
lwjgl3/      Desktop-модуль
mcp-server/  MCP-сервер для управления игрой из ИИ-агента (Python/FastMCP)
assets/      Ресурсы:
  maps/       Карты лабиринта (прогрессия 1/1..5.txt, final.txt)
  themes/     Темы локаций (grass — активная: фон, стены, raw-оригиналы)
  sprites/    Глобальные спрайты персонажей (мышь, сыр, 4 кота)
  icon/       Иконка приложения
  asset-manifest.json  Соответствие asset_id → файлы (SpriteCook)
```

## 🗺️ Изменение карты

Карты лежат в `assets/maps/`: **28 колонок × 16 строк**.

| Символ | Значение |
|--------|----------|
| `#` | стена |
| пробел | проход |
| `1`–`9` | ручная расстановка сыра (сколько цифр — столько сыров) |
| `H` | сердечко (латинская H, до 3 на карту) |
| `+` | стартовая клетка мыши (если несколько — случайная) |
| `A`–`D` | явные стартовые клетки котов (иначе — рандом около углов) |
| `*` | убежище (только мышь) — этап в планах |

## 🎨 Темы локаций

Тема определяет фон и спрайты стен (`assets/themes/<тема>/theme.json`). Активная тема задаётся в `assets/themes/themes.json` (поле `active`). Сейчас доступна одна тема — **grass** (трава и кусты).

Подробное описание проекта, правил и планов — в [GAME.md](GAME.md). История изменений — в [CHANGELOG.md](CHANGELOG.md). Инструкции для ИИ-агента — в [AGENTS.md](AGENTS.md).