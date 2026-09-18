# RUN.md — запуск игры (CATS AND MOUSES)

Шпаргалка по запуску. Все команды — из корня проекта в PowerShell.
Полные детали режимов, флагов и AI/MCP — в навыке `.opencode/skills/run-game/SKILL.md`.

## 0. Команды (кратко)

```powershell
# Desktop, dev-запуск (ассеты из assets/)
.\gradlew lwjgl3:run

# Быстрый запуск из дистрибутива
.\gradlew lwjgl3:installDist
.\desktop-run.bat

# С флагами
.\gradlew lwjgl3:run --args="--start-map 1/1"
.\gradlew lwjgl3:run --args="--theme space --start-map final --debug"

# Android на подключённом телефоне
.\gradlew android:installDebug

# Сборка
.\gradlew build

# Остановить запущенные экземпляры
Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
  Where-Object { $_.CommandLine -like '*lwjgl3*' } |
  ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
```

## 1. Какой режим выбрать

| Ситуация | Режим |
|---|---|
| Разработка, часто правлю код/карты/спрайты | **Desktop dev**: `.\gradlew lwjgl3:run` |
| Быстрый повторный запуск без пересборки | **Дистрибутив**: `installDist` → `.\desktop-run.bat` |
| Отладка из IDE | IDEA: «Run Dev» / «Run Idea» / «Run Dist» (`.run/`) |
| Проверка на телефоне | **Android**: `.\gradlew android:installDebug` |
| APK без устройства | `.\gradlew android:assembleDebug` |
| Автотест/управление извне | **AI-режим**: `--ai-mode` |
| Проверка сборки | `.\gradlew build` |

## 2. Desktop

```powershell
# dev-запуск: ассеты читаются из assets/ (правки видны сразу)
.\gradlew lwjgl3:run

# дистрибутив: собрать один раз, потом запускать мгновенно
.\gradlew lwjgl3:installDist
.\desktop-run.bat
```

Важно: в дистрибутиве ассеты «вморожены» в `lwjgl3/lib/lwjgl3.jar` — после правок `assets/**`
нужен повторный `installDist` (или `runDist`). Для итерации по ассетам используй `lwjgl3:run`.

Запуск из IDEA: конфигурации **Run Dev** (`:lwjgl3:run`), **Run Idea** (working dir `assets`),
**Run Dist** (`:lwjgl3:runDist` = installDist + запуск).

## 3. Флаги запуска

Передаются через `--args` (для `lwjgl3:run`) или в Program arguments конфигурации IDEA.

| Флаг | Значение | Пример |
|---|---|---|
| `--start-map <карта>` | старт без меню с карты (алиас `--start-level`) | `--start-map 1/2`, `--start-map final` |
| `--debug` | включить логи LibGDX (алиас `--debug-logs`) | `--debug` |
| `--ai-mode [порт]` | HTTP-сервер управления; без порта — 8080 | `--ai-mode 8080`, `--ai-mode` |
| `--theme <id>` | **отладка темы**: форсирует тему всех карт + старт без меню | `--theme space --start-map final` |

```powershell
.\gradlew lwjgl3:run --args="--start-map 1/2"
.\gradlew lwjgl3:run --args="--theme space --start-map final --debug"
.\gradlew lwjgl3:run --args="--ai-mode 8080 --start-map 1/1"
```

Темы — папки `assets/themes/<id>/` (`grass`, `castle`, `ice`, `desert`, `moss`, `swamp`, `space`).
В обычной игре тема берётся из `assets/maps/<мир>/theme.json` (финал — `final.json`),
заставка/меню/оверлеи — системная тема (`system` в `assets/themes/themes.json`).

## 4. AI/MCP-режим

`--ai-mode [порт]` стартует сразу в `GameScreen`, минуя меню.

| Метод | Endpoint | Действие |
|---|---|---|
| GET | `/state` | JSON с состоянием игры |
| POST | `/input` | `{"action":"move","direction":"UP"}`, `{"action":"pause"}`, `{"action":"reset"}` |

```powershell
Invoke-RestMethod -Uri http://localhost:8080/state
Invoke-RestMethod -Uri http://localhost:8080/input -Method POST -ContentType "application/json" -Body '{"action":"move","direction":"UP"}'
```

## 5. Android

```powershell
.\gradlew android:installDebug     # установить на подключённое устройство
.\gradlew android:assembleDebug    # собрать APK без устройства
```

APK: `android/build/outputs/apk/debug/android-debug.apk`.
На телефоне нужны «Отладка по USB» и «Установка через USB»; проверить подключение — `adb devices`.
Установить готовый APK: `adb install -r android\build\outputs\apk\debug\android-debug.apk`.

## 6. Сборка и проблемные кэши

```powershell
.\gradlew build
.\gradlew build --no-daemon      # при битых кэшах Gradle
```

## 7. Остановка и перезапуск (Desktop)

```powershell
# остановить все запущенные экземпляры
Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
  Where-Object { $_.CommandLine -like '*lwjgl3*' } |
  ForEach-Object { Stop-Process -Id $_.ProcessId -Force }

# перезапуск из дистрибутива с пересборкой
.\gradlew lwjgl3:installDist
Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -like '*lwjgl3*' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
Start-Process -FilePath "cmd.exe" -ArgumentList "/c","desktop-run.bat" -WorkingDirectory "$PWD" -WindowStyle Minimized
```

## 8. Dev-утилиты (не для игры)

```powershell
python tools/gen_levels.py     # перегенерировать карты миров + theme.json + превью
python tools/map_preview.py    # только превью карт (PNG 1:1 рядом с .txt)
python tools/make_view.py      # превью спрайтов на шахматке (view/)
```

Правило тестового запуска (цель → план → роль пользователя → отчёт) — в `AGENTS.md`.
