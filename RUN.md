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

# Неблокирующие (фоновые) запуски: команда возвращается сразу, окно живёт само.
# Логи — в lwjgl3/build/run-logs/ (и editor-logs/ для редактора).
.\gradlew lwjgl3:runBg -PgameArgs="--start-map 1/2 --debug"
.\gradlew lwjgl3:runEditor
.\gradlew lwjgl3:runDistBg

# Редактор карт (Desktop, dev): блокирует терминал, пока окно открыто
.\gradlew lwjgl3:run --args="--editor"

# Редактор карт в фоне: запускает отдельный процесс и сразу возвращает управление
.\gradlew lwjgl3:runEditor

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
| Запустить и не блокироваться | **Фон**: `.\gradlew lwjgl3:runBg` / `runEditor` / `runDistBg` |
| Рисовать/править карты | **Редактор карт**: `.\gradlew lwjgl3:run --args="--editor"` (или `runEditor` в фоне) |
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

### Неблокирующие запуски (для агента и ручного теста)

`lwjgl3:run` и `lwjgl3:runDist` **блокируют** терминал, пока окно открыто. Чтобы запустить и
сразу вернуть управление, есть фоновые задачи — они стартуют отдельный процесс и завершаются:

```powershell
.\gradlew lwjgl3:runBg -PgameArgs="--start-map 1/2 --debug"   # игра (dev), аргументы через -PgameArgs
.\gradlew lwjgl3:runEditor                                    # редактор карт (--editor)
.\gradlew lwjgl3:runDistBg                                    # installDist + запуск дистрибутива
```

Логи фоновых запусков: `lwjgl3/build/run-logs/{game-out,game-err,dist-out,dist-err}.log`,
`lwjgl3/build/editor-logs/editor-{out,err}.log`. Закрытие окна (крестик или `ESC` → «Да» в
редакторе) корректно завершает процесс — «зависших» java-процессов не остаётся. Проверить
живой процесс: `Get-CimInstance Win32_Process -Filter "Name='java.exe'"`.
Не ждать в фоне (`Start-Sleep` и т.п.): запустил → сразу вернул управление.

## 3. Флаги запуска

Передаются через `--args` (для `lwjgl3:run`) или в Program arguments конфигурации IDEA.

| Флаг | Значение | Пример |
|---|---|---|
| `--start-map <карта>` | старт без меню с карты (алиас `--start-level`) | `--start-map 1/2`, `--start-map final` |
| `--debug` | включить логи LibGDX (алиас `--debug-logs`) | `--debug` |
| `--ai-mode [порт]` | HTTP-сервер управления; без порта — 8080 | `--ai-mode 8080`, `--ai-mode` |
| `--theme <id>` | **отладка темы**: форсирует тему всех карт + старт без меню | `--theme space --start-map final` |
| `--editor` | **редактор карт** (Desktop, dev): экран правки карт вместо меню | `--editor` |
| `--assets-dir <путь>` | папка исходных `assets` для сохранения карт редактором | `--editor --assets-dir C:\...\assets` |

```powershell
.\gradlew lwjgl3:run --args="--start-map 1/2"
.\gradlew lwjgl3:run --args="--theme space --start-map final --debug"
.\gradlew lwjgl3:run --args="--ai-mode 8080 --start-map 1/1"
```

Темы — папки `assets/themes/<id>/` (`grass`, `castle`, `ice`, `desert`, `moss`, `swamp`, `space`).
В обычной игре тема берётся из `assets/maps/<мир>/theme.json` (финал — `final.json`),
заставка/меню/оверлеи — системная тема (`system` в `assets/themes/themes.json`).

### Редактор карт (`--editor`)

```powershell
.\gradlew lwjgl3:run --args="--editor"   # блокирует терминал, пока окно открыто
.\gradlew lwjgl3:runEditor               # запуск в фоне, управление возвращается сразу
```

Desktop-приложение для правки существующих карт: WYSIWYG-рендер (та же тема/dual-grid,
что в игре), палитра символов, сохранение в исходные `assets/maps`, playtest по **F5**.
Список карт листается `<` / `>` (или `,` / `.`), тема карты подхватывается автоматически.
Не входит в APK (код только в модуле `lwjgl3`).

**Два способа запуска.** `run --args="--editor"` (Gradle `JavaExec`) держит терминал/агента,
пока редактор открыт, — это ожидаемо. `runEditor` (`lwjgl3/build.gradle.kts`) стартует
редактор отдельным процессом и сразу завершает задачу: терминал свободен, окно живёт само.
Логи фонового запуска — `lwjgl3/build/editor-logs/editor-out.log` и `editor-err.log`
(появляются при старте; без `--debug` почти пустые). Закрытие окна (крестик, либо `ESC` →
«Да») корректно завершает процесс — LibGDX `Lwjgl3Application.loop()` сам выходит при
`window.shouldClose()` (`windows.size == 0`), так что «зависшего» процесса не остаётся.

| Действие | Управление |
|---|---|
| Рисовать | ЛКМ (кисть); ПКМ — стереть в проход |
| Заливка прямоугольником | `R` — переключить инструмент, растянуть ЛКМ |
| Отменить / вернуть | `Ctrl+Z` / `Ctrl+Y` |
| Сохранить | `Ctrl+S` |
| Playtest (играть карту) | `F5` (выход — `ESC`) |
| Карта назад / вперёд | `,` / `.` (листать; при несохранённом — диалог) |
| Выход | `ESC` (при несохранённом — диалог) |

Папка сохранения: `--assets-dir` → системное свойство `assets.dir` → `../assets` / `assets`
относительно рабочей папки. Если не найдена — редактор работает в режиме только чтение.

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
