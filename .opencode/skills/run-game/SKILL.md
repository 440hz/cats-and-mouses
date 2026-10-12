---
name: run-game
description: Запуск и перезапуск игры CATS AND MOUSES во всех режимах — Desktop dev (lwjgl3:run), быстрый из дистрибутива (installDist + desktop-run.bat), конфигурация IDEA, Android (installDebug/assembleDebug), AI/MCP-режим, редактор карт (`--editor`), флаги --start-map / --debug / --ai-mode / --theme / --assets-dir / --shot (dev-снимок кадра). Используй, когда нужно запустить или перезапустить игру, собрать дистрибутив, стартовать с конкретной карты, выбрать тему локации, включить логи, AI-режим, открыть редактор карт, установить на телефон.
---

# Запуск игры (CATS AND MOUSES)

Все команды — из корня проекта, PowerShell.

## 1. Какой режим выбрать

| Ситуация | Режим |
|---|---|
| Разработка, часто меняю код/карты/спрайты | **Desktop dev**: `.\gradlew lwjgl3:run` |
| Запустить и не блокироваться (агент) | **Фон**: `runBg` / `runEditor` / `runDistBg` |
| Нужен быстрый повторный запуск без пересборки | **Дистрибутив**: `installDist` → `.\desktop-run.bat` |
| Работа из IDE с отладчиком | **IDEA**: конфигурация «CATS AND MOUSES Desktop» |
| Проверка на телефоне | **Android**: `.\gradlew android:installDebug` |
| Нужен APK без устройства | `.\gradlew android:assembleDebug` |
| Автотест/управление извне (MCP, HTTP) | **AI-режим**: `--ai-mode` |
| Правка карт визуально | **Редактор карт** (Desktop): `--editor`; для агента — `runEditor` (фон) |
| Проверка, что всё собирается | `.\gradlew build` |

### Чем отличаются три способа запустить Desktop

Во всех случаях запускается один класс `com.catsandmouses.lwjgl3.Lwjgl3Launcher`.

| | `lwjgl3:run` | `installDist` + `desktop-run.bat` | IDEA-конфигурация |
|---|---|---|---|
| Что это | Gradle-задача `JavaExec` | автономный дистрибутив (`bin/`+`lib/`) | запуск `.main()` из IDE |
| Классы | свежая сборка Gradle | зафиксированы в `core.jar`/`lwjgl3.jar` | вывод сборки IDEA |
| Ассеты | копия `assets/` в `lwjgl3/build/resources/main/` | внутри `lwjgl3.jar` | `assets/` (working dir `$PROJECT_DIR$/assets`) |
| Пересборка | автоматически каждый запуск | только по ручному `installDist` | Make перед запуском |
| Скорость | медленнее | мгновенно | быстро |
| Правки ассетов | видны сразу | не видны до `installDist` | видны после Make |
| Аргументы | `--args="--start-map 1/2"` | `.\desktop-run.bat --start-map 1/2` | Program arguments |

Правило: при разработке (частые правки карт/спрайтов) — `lwjgl3:run`; для быстрых повторных
запусков готового билда — дистрибутив, но помни, что ассеты в нём «вморожены» в jar.

### Неблокирующие запуски (обязательно для агента)

`run` (JavaExec) и `runDist` (Exec) **держат терминал/агента**, пока окно открыто. Для
интерактивного тестирования и любых запусков при диалоге использовать фоновые задачи —
они стартуют отдельный java-процесс, сразу завершаются и пишут логи:

| Задача | Что запускает | Логи |
|---|---|---|
| `.\gradlew lwjgl3:runBg -PgameArgs="--start-map 1/2 --debug"` | игру (dev), аргументы через `-PgameArgs` | `lwjgl3/build/run-logs/game-{out,err}.log` |
| `.\gradlew lwjgl3:runEditor` | редактор карт (`--editor`) | `lwjgl3/build/editor-logs/editor-{out,err}.log` |
| `.\gradlew lwjgl3:runDistBg` | `installDist` + дистрибутив | `lwjgl3/build/run-logs/dist-{out,err}.log` |

`--args="..."` для фоновых задач **не работает** (это опция `JavaExec`) — аргументы игре
передаются через `-PgameArgs="..."`. Android-установка (`android:installDebug`) и
`lwjgl3:installDist` возвращают управление сами — они не блокирующие.

**Не ждать в фоне.** После запуска сразу вернуть управление и ждать сообщения пользователя.
Никаких `Start-Sleep`/ожиданий процесса. Детект «закрыл — проверь» — по факту сообщения:
`Get-CimInstance Win32_Process -Filter "Name='java.exe'"` (+ хвост лога).

**Неблокирующие пересборки и тесты.** Долгие пересборки и тесты (`build`, `core:test`,
`lwjgl3:installDist`, `android:assembleDebug`/`assembleRelease`/`bundleRelease`) тоже нельзя запускать
блокирующим вызовом — агент «зависает». Запускать через хелпер `pwsh tools/build-bg.ps1 [задачи…]`: он стартует
`gradlew` отдельным процессом и сразу возвращает управление, логи — в
`lwjgl3/build/build-logs/build-{out,err}.log` (результат `BUILD SUCCESSFUL` / `BUILD FAILED` —
смотреть по логу при следующем обращении). Примеры: `pwsh tools/build-bg.ps1` (всё),
`pwsh tools/build-bg.ps1 build` (только сборка/тесты), `pwsh tools/build-bg.ps1 core:test`
(Kotlin-тесты неблокирующе), `pwsh tools/build-bg.ps1 android:assembleDebug`.

### Три именованных запуска (`.run/` в IDEA)

В проекте есть три готовые run-конфигурации IntelliJ (хранятся в `.run/*.run.xml`, **коммитятся** в git):

| Конфигурация | Что делает | CLI-эквивалент |
|---|---|---|
| **Run Dev** | запуск через Gradle, ассеты из `build/resources` | `.\gradlew :lwjgl3:run` |
| **Run Idea** | Application-запуск `Lwjgl3Launcher`, working dir `$PROJECT_DIR$/assets` | — (запуск из IDE) |
| **Run Dist** | `installDist` + запуск собранного дистрибутива | `.\gradlew :lwjgl3:runDist` |

`runDist` — Gradle-таска (`lwjgl3/build.gradle.kts`): пересобирает дистрибутив и запускает
`build/install/lwjgl3/bin/lwjgl3`. После правок `assets/**` она сама обновит jar.
Конфигурации появляются в выпадающем списке IDEA сразу; если нет — `File → Reload from Disk`.

### Индикатор режима запуска в игре

В правом нижнем углу версия показывается с режимом: `v<версия>(d|r|i|a)`.
`d` — dev, `r` — дистрибутив, `i` — IDEA, `a` — Android. Логика — `LaunchMode.kt`:
`d` задаётся системным свойством в таске `:lwjgl3:run` (`-Dlaunch.mode=d`, иначе dev-запуск
неотличим от дистрибутива — core тоже в jar), `r` — по классам в jar (dist), `i` — по
`-Dlaunch.mode=i` в `.run/Run Idea.run.xml`. Это удобно для быстрой проверки, что запущен
именно нужный вариант.

## 2. Desktop — dev-запуск (пересборка при каждом запуске)

```powershell
.\gradlew lwjgl3:run
```

- Читает ассеты **напрямую из `assets/`** — правки карт/спрайтов подхватываются сразу (через `processResources`).
- Окно 1280×720, поверх всех, забирает фокус клавиатуры.
- Логи по умолчанию отключены.

## 3. Desktop — быстрый запуск из дистрибутива

Собрать один раз (после изменений кода/ассетов):

```powershell
.\gradlew lwjgl3:installDist
```

Затем запускать сколько угодно раз (мгновенно, без Gradle):

```powershell
.\desktop-run.bat
```

Дистрибутив: `lwjgl3/build/install/lwjgl3/` (launcher `bin/lwjgl3.bat`).

**ВАЖНО:** в дистрибутиве ассеты упакованы в `lwjgl3/lib/lwjgl3.jar`. Если после сборки поменять
`assets/**` — изменения не появятся, пока не пересобрать `installDist`. Для итерации по ассетам
используй dev-запуск (`lwjgl3:run`).

## 4. IDEA

Конфигурация **«CATS AND MOUSES Desktop»**: main class `com.catsandmouses.lwjgl3.Lwjgl3Launcher`,
module `CatsAndMouses.lwjgl3.main`, working dir `$PROJECT_DIR$/assets`.

## 5. Флаги запуска

Передаются через `--args` (только для `lwjgl3:run`) или в поле Program arguments конфигурации IDEA.

| Флаг | Значение | Пример |
|---|---|---|
| `--start-map <карта>` | старт с карты (алиас `--start-level`) | `--start-map 1/2`, `--start-map final` |
| `--debug` | включить логи LibGDX (алиас `--debug-logs`) | `--debug` |
| `--ai-mode [порт]` | HTTP-сервер управления; без порта — 8080 | `--ai-mode 8080`, `--ai-mode=9999`, `--ai-mode` |
| `--theme <id>` | отладка темы: форсирует тему всех карт + старт без меню | `--theme castle`, `--theme=castle` |
| `--no-countdown` | отключить стартовый отсчёт «3 → 2 → 1» (старт сразу) | `--no-countdown` |
| `--credits` | dev: сразу открыть финальные титры (пасхалку) | `--credits` |
| `--lang <ru\|en>` | dev: форсировать язык интерфейса | `--lang en`, `--credits --lang en` |
| `--editor` | редактор карт (Desktop, dev): экран правки вместо меню | `--args="--editor"` |
| `--assets-dir <путь>` | папка исходных `assets` для сохранения карт редактором | `--editor --assets-dir C:\...\assets` |
| `--window <WxH>` | dev: размер окна Desktop | `--window 1920x1080` |
| `--shot <файл>` | dev: сохранить кадр экрана в PNG и выйти | `--shot C:\tmp\menu.png` |
| `--shot-screen <id>` | dev: экран для снимка (`loading`/`menu`/`settings`/`credits`/`game`) | `--shot-screen menu` |
| `--shot-delay <сек>` | dev: пауза перед кадром (дефолт 1 с) | `--shot-delay 2` |

Примеры:

```powershell
.\gradlew lwjgl3:run --args="--start-map 1/2"
.\gradlew lwjgl3:run --args="--start-map final --debug"
.\gradlew lwjgl3:run --args="--ai-mode 8080 --start-map 1/1"
.\gradlew lwjgl3:run --args="--ai-mode"
.\gradlew lwjgl3:run --args="--theme castle --start-map 1/1"
# dev-снимок экрана (меню 1920×1080 → PNG); в фоне, после кадра игра закрывается сама
.\gradlew lwjgl3:runBg -PgameArgs="--window 1920x1080 --shot-screen menu --shot C:\tmp\menu.png"
```

`--theme <id>` — **только отладка темы**: форсирует тему (`grass`, `castle`, …) для всех карт
мира и запускает игру **без меню** прямо на карте (`--start-map`, иначе `1/1`) — удобно смотреть,
как карта выглядит с темой. В обычной игре тема берётся из `theme.json` мира
(`assets/maps/<мир>/theme.json`, финал — `maps/final.json`), а заставка/меню/оверлеи — из
**системной** темы (`system` в `assets/themes/themes.json`, сейчас `grass`). Неизвестная тема →
откат на системную. Запуск без флагов показывает заставку и меню на системной теме.

Уровни и порядок прохождения вычисляются из манифеста `assets/maps/maps.json` (генерируется
Gradle-задачей `generateMapsManifest`). Карты и темы миров генерируются `tools/gen_levels.py`,
dev-превью 1:1 — `tools/map_preview.py` (PNG рядом с `.txt`, в git не хранятся).

**Стартовый отсчёт.** В обычной игре при старте карты показывается отсчёт **«3 → 2 → 1»**
(≈3 c, `Constants.COUNTDOWN_TIME`), затем игра начинается. В AI-режиме (`--ai-mode`) и в playtest
редактора отсчёт выключен автоматически; для ручных dev-запусков его отключает флаг
`--no-countdown`.

### Редактор карт (`--editor`)

```powershell
.\gradlew lwjgl3:run --args="--editor"   # блокирует терминал, пока окно открыто
.\gradlew lwjgl3:runEditor               # запуск в фоне: задача завершается сразу, окно живёт само
```

Desktop-редактор существующих карт: WYSIWYG (та же тема и dual-grid, что в игре), палитра
символов, заливка прямоугольником, undo/redo, сохранение в `assets/maps`, playtest по **F5**.
Листание карт — `<` / `>` (или `,` / `.`), тема карты подхватывается автоматически.

**`run` или `runEditor`?** Оба запускают один класс `Lwjgl3Launcher` с `--editor`.
`run --args="--editor"` (Gradle `JavaExec`) держит терминал/агента, пока редактор открыт, —
это нормально, но мешает диалогу. `runEditor` (`lwjgl3/build.gradle.kts:63`) стартует редактор
отдельным процессом (`java -cp ... Lwjgl3Launcher --editor`) и сразу завершает Gradle-задачу:
терминал свободен, редактор продолжает работать. Для интерактивной проверки/ручного
тестирования агенту использовать **`runEditor`**.

Логи фонового запуска: `lwjgl3/build/editor-logs/editor-out.log`, `editor-err.log`
(пишутся всегда, при отсутствии `--debug` почти пустые). Закрытие окна (крестик или `ESC` →
«Да») корректно завершает процесс — LibGDX `Lwjgl3Application.loop()` выходит по
`window.shouldClose()`, когда окон не осталось, так что «зависших» java-процессов не остаётся.

Проверить, что редактор ещё жив/уже закрыт:

```powershell
Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
  Where-Object { $_.CommandLine -like '*--editor*' } |
  Select-Object ProcessId
```

| Действие | Управление |
|---|---|
| Рисовать / стереть | ЛКМ (выбранный символ) / ПКМ (проход) |
| Инструмент заливки | `R`, затем растянуть ЛКМ |
| Undo / redo | `Ctrl+Z` / `Ctrl+Y` |
| Сохранить | `Ctrl+S` |
| Playtest | `F5` (выход — `ESC`) |
| Выход | `ESC` |

Папка сохранения: `--assets-dir` → свойство `assets.dir` → `../assets` / `assets` от рабочей
папки. Не найдена — режим только чтение. В APK редактор не входит (только модуль `lwjgl3`).

## 6. AI/MCP-режим

`--ai-mode [порт]` поднимает встроенный HTTP-сервер (`AiDebugServer`), игра стартует сразу в
`GameScreen`, минуя меню и заставку.

| Метод | Endpoint | Тело / действие |
|---|---|---|
| GET | `/state` | JSON с состоянием игры |
| POST | `/input` | `{"action":"move","direction":"UP"}` — задать направление |
| POST | `/input` | `{"action":"pause"}` — пауза/продолжить |
| POST | `/input` | `{"action":"reset"}` — рестарт уровня |

```powershell
Invoke-RestMethod -Uri http://localhost:8080/state
Invoke-RestMethod -Uri http://localhost:8080/input -Method POST -ContentType "application/json" -Body '{"action":"move","direction":"UP"}'
```

MCP-сервер `mcp-server/` (подключён в `opencode.json`) даёт инструменты
`get_game_state`, `send_move`, `toggle_pause`, `reset_level`.

## 7. Android

```powershell
# Установить на подключённое устройство/эмулятор
.\gradlew android:installDebug

# Собрать debug APK без устройства
.\gradlew android:assembleDebug
```

## 8. Полная сборка и проблемные кэши

```powershell
.\gradlew build
.\gradlew build --no-daemon   # при битых кэшах Gradle
```

## 9. Остановка и перезапуск (Desktop)

Остановить все запущенные экземпляры игры:

```powershell
Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
  Where-Object { $_.CommandLine -like '*lwjgl3*' } |
  ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
```

Перезапуск из дистрибутива (с пересборкой) одной строкой:

```powershell
.\gradlew lwjgl3:installDist; Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -like '*lwjgl3*' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }; Start-Process -FilePath "cmd.exe" -ArgumentList "/c","desktop-run.bat" -WorkingDirectory "$PWD" -WindowStyle Minimized
```

## 10. Правила тестового запуска

Перед интерактивным запуском (не только для проверки сборки) — по правилам `AGENTS.md`:
сообщи **цель** запуска, дай **план тестирования**, уточни **роль пользователя**
(сам жмёт клавиши или ждёт автоматической проверки через MCP/AI). После — **отчёт**:
что проверено, что сработало, что нет.

- **Все запуски — неблокирующие** (`runBg` / `runEditor` / `runDistBg`), не ждать в фоне
  (`Start-Sleep` и т.п.). Запустил → сразу вернул управление.
- **Перед долгим шагом предупреди.** Если запуск собирает проект (первый `runBg`/`runEditor`,
  `installDist`, `android:installDebug`, `build`, `runDistBg`) — сначала короткое сообщение
  («Идёт сборка, это займёт ~30–60 с…»), затем команда. То же для любой долгой команды.
