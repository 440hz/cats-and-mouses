---
name: generate-theme
description: Полная процедура генерации новой темы локации для CATS AND MOUSES — стены (15-piece dual-grid autotile через generate_tileset) и бесшовный фон (generate_game_art), скачивание RAW, масштабирование, view-превью, theme.json, регистрация в themes.json, asset-manifest и GAME.md. Используй, когда нужно сгенерировать/добавить тему (лёд, песок, замок и т.п.) или перегенерировать её стены/фон.
---

# Генерация темы локации (CATS AND MOUSES)

Тема = **папка `assets/themes/<id>/`** с двумя игровыми спрайтами, `theme.json`
и записью в индексе. Стены рисуются через **dual-grid 15-piece autotile**,
фон — бесшовным тайлом. Ниже — отработанная процедура.

## 0. Обязательное подтверждение (правила `AGENTS.md`)

Перед ЛЮБОЙ генерацией через SpriteCook:

1. **Прокси** — спроси пользователя, включён ли прокси; без явного «да» не начинай.
2. **Баланс** — `get_credit_balance`, покажи остаток.
3. **Модели** — `list_generation_models`, покажи таблицу (название, цена, назначение).
4. **Рекомендация + вопрос** — предложи модель и ЗАДАЙ вопрос «Какую модель использовать?».
   **СТОП. Дождись ответа.** Исключение: пользователь сам назвал модель.

## 1. Что генерируем (2 ассета)

| # | Ассет | Инструмент | Модель | Цена |
|---|---|---|---|---|
| 1 | Стены — 15-piece autotile | `generate_tileset` | `gemini-3.1-flash-image` | **12 кр.** |
| 2 | Фон — бесшовный тайл | `generate_game_art` | `gpt-image-2.5-flare` (1K medium) | **8 кр.** |

Итого ≈ **20 кредитов** на тему.

Проект SpriteCook: `CATS AND MOUSES` = `786e135f-52aa-47c4-a9c1-de9f41148d71` (найти через `list_projects`).

### 1.1 Стены (15-piece autotile)

```
generate_tileset(
  prompt = "<шаблон ниже>",
  style_mode = "pixel",
  perspective = "topdown",
  piece_set = "15-piece",
  tile_size = 32,
  edges = "transparent",
  elevation = "minimal",
  model = "gemini-3.1-flash-image",
  project_id = "786e135f-52aa-47c4-a9c1-de9f41148d71",
  wait_seconds = 90
)
```

**Шаблон промпта стен:**
> Top-down 15-piece autotile tileset of <материал стен>. <детали: слои, трещины, мох/иней, крошка>. Cozy pixel art 16-bit SNES JRPG style, thick dark outline only on outer edges, seamless inner edges, transparent background, strict grid, no gaps, static, single spritesheet 4x4, top-down view

Результат: `asset_kind=tileset`, **128×128**, атлас **4×4**, tile 32.

### 1.2 Фон (бесшовный тайл)

```
generate_game_art(
  prompt = "<шаблон ниже>",
  model = "gpt-image-2.5-flare",
  resolution = "1K", quality = "medium",
  mode = "texture", pixel = true,
  bg_mode = "include", smart_crop = false,
  aspect_ratio = "1:1", width = 512, height = 512,
  project_id = "786e135f-52aa-47c4-a9c1-de9f41148d71",
  wait_seconds = 90
)
```

**Шаблон промпта фона:**
> Seamless tileable top-down ground texture: <поверхность> for a <тема> theme, <детали: рябь, камешки, трещины>. 16-bit SNES JRPG pixel art style, opaque, no walls, no objects, no shadows, uniform lighting, repeating pattern, top-down view, strict grid alignment

`smart_crop=false` — иначе кроп может сломать бесшовность. Результат может быть
512×512 или 256×256; RAW обычно 1024×1024 (RGB).

### 1.3 Если MCP-вызов вернул timeout

`wait_seconds=90` часто заканчивается `MCP error -32001: Request timed out`,
но **задача уже создана и идёт асинхронно**. Не перезапускай генерацию (это
спишет кредиты повторно)! Найди результат:

```
list_recent_assets(limit = 3)   # asset с нужным prompt
list_active_jobs()               # если ещё не готово
```

## 2. Получить RAW и скачать

`get_asset_metadata(asset_id)` → взять `raw_url` из ответа. Скачать (PowerShell):

```powershell
Invoke-WebRequest -Uri "<raw_url>" -OutFile "assets/themes/<id>/raw/wall_autotile.png"
Invoke-WebRequest -Uri "<raw_url>" -OutFile "assets/themes/<id>/raw/background.png"
```

**Проверить формат по магическим байтам** (НЕ по Content-Type):

```powershell
$path = "assets/themes/<id>/raw/wall_autotile.png"
$b = [System.IO.File]::ReadAllBytes($path)
if ($b[0] -eq 0x89 -and $b[1] -eq 0x50) { "PNG" }
elseif ($b[0] -eq 0xFF -and $b[1] -eq 0xD8) { "JPEG" }
```

Если пришёл JPEG — сконвертировать в PNG (для JPEG — ключевать белый фон в
прозрачность с мягкой кромкой) и **сообщить пользователю** («Файл X был JPEG →
конвертирован в PNG»). Разрешение спрашивать не нужно.

## 3. Масштабирование (dev-скрипт, Pillow)

Игра использует `CELL_SIZE = 40`, поэтому тайл атласа 32 → **40**:

```powershell
python -c "from PIL import Image; t=Image.open(r'assets/themes/<id>/raw/wall_autotile.png').convert('RGBA').resize((160,160), Image.LANCZOS); t.save(r'assets/themes/<id>/wall_autotile.png')"
python -c "from PIL import Image; b=Image.open(r'assets/themes/<id>/raw/background.png').convert('RGB').resize((200,200), Image.LANCZOS); b.save(r'assets/themes/<id>/background.png')"
```

- Тайл: **128×128 → 160×160** (4×4 по 40×40), LANCZOS, RGBA.
- Фон: RAW → **200×200**, LANCZOS, RGB (непрозрачный), как у `grass`.

## 4. Превью `view/`

```powershell
python tools/make_view.py
```

Скрипт сам найдёт пары `raw/…` + игровой файл в `themes/<id>/` и положит
превью на чёрно-белую шахматку в `themes/<id>/view/`. Полезно визуально
проверить бесшовность фона и раскладку тайлов.

## 5. `theme.json` темы

```json
{
  "name": "<Название темы>",
  "wallFiles": [],
  "wallAutotile": "wall_autotile.png",
  "tileSize": 40,
  "atlasColumns": 4,
  "atlasRows": 4,
  "extrasCountMin": 0,
  "extrasCountMax": 0,
  "background": "background.png"
}
```

`wallAutotile` включает dual-grid рендер; `wallFiles`/`extras*` не используются
(оставлены как fallback для тем без autotile).

## 6. Регистрация в `assets/themes/themes.json`

Добавить в `themes` (активной обычно оставляем `grass`):

```json
{ "id": "<id>", "name": "<Название темы>" }
```

## 7. Манифесты и документация

- **`assets/asset-manifest.json`** — запись `asset_id → { label, game, raw, dims, content, rawDims }`:
  - тайл: `dims: "160x160"`, `rawDims: "128x128"`;
  - фон: `dims: "200x200"`, `rawDims: "1024x1024"`.
- **`assets/asset-manifest.md`** — строка в таблицу (обновлять вместе с JSON).
- **`GAME.md`** — «Текущий статус» (новый пункт) и «Этап 7 — Темы».

## 8. Проверка

```powershell
.\gradlew build                 # BUILD SUCCESSFUL
.\gradlew lwjgl3:installDist    # для desktop-run.bat
.\gradlew lwjgl3:run --args="--theme <id> --start-map 1/1"
```

В логе должно быть `[MainGame] create() ... theme=<id>`. Заставка, меню и
лабиринт берут фон/стены выбранной темы (единый `ThemeLoader`).

## 9. Технические детали (не ломать)

- Dual-grid: `GameScreen.drawAutotileWalls()` — маска 4 соседей, маппинг
  `frameByMask = [-1, 15, 8, 9, 0, 11, 14, 7, 13, 4, 1, 10, 3, 2, 5, 6]`,
  тайл рисуется со сдвигом **−`CELL_SIZE/2`** по X и Y (центрирование на углу).
- Сетка карты неизменна: 28×16, клетка 40.
- RAW (`themes/<id>/raw/`) и `view/` хранятся в git, но исключены из APK/jar.
- Не выбирай модель сам и не перезапускай таймаутнувшую генерацию (двойное списание).

## 10. Уже сделанные темы (образцы)

| id | Название | Стены (asset_id) | Фон (asset_id) |
|---|---|---|---|
| `grass` | Трава и кусты | `cee551ee-…` (куст), модель `gemini-3.1-flash-image` | `9b23ff34-…` (трава) |
| `castle` | Старый замок | `1b3e9538-…` (песчаник замка) | `ba7814d1-…` (каменные плиты) |
| `ice` | Ледяная пещера | `54361830-…` (лёд/снег) | `d9afd3c7-…` (снег + голубой лёд) |
| `desert` | Песчаный каньон | `73ec559d-…` (слоистый песчаник) | `bef5b628-…` (золотой песок) |

Переключение темы: `--theme <id>` (приоритет) → `active` в `themes.json` → откат
на `active`, если темы нет.
