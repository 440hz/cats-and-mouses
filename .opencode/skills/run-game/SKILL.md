---
name: run-game
description: Запуск и перезапуск игры CATS AND MOUSES во всех режимах — Desktop dev (lwjgl3:run), быстрый из дистрибутива (installDist + desktop-run.bat), конфигурация IDEA, Android (installDebug/assembleDebug), AI/MCP-режим, флаги --start-map / --debug / --ai-mode / --theme. Используй, когда нужно запустить или перезапустить игру, собрать дистрибутив, стартовать с конкретной карты, выбрать тему локации, включить логи или AI-режим, установить на телефон.
---

# Запуск игры (CATS AND MOUSES)

Все команды — из корня проекта, PowerShell.

## 1. Какой режим выбрать

| Ситуация | Режим |
|---|---|
| Разработка, часто меняю код/карты/спрайты | **Desktop dev**: `.\gradlew lwjgl3:run` |
| Нужен быстрый повторный запуск без пересборки | **Дистрибутив**: `installDist` → `.\desktop-run.bat` |
| Работа из IDE с отладчиком | **IDEA**: конфигурация «CATS AND MOUSES Desktop» |
| Проверка на телефоне | **Android**: `.\gradlew android:installDebug` |
| Нужен APK без устройства | `.\gradlew android:assembleDebug` |
| Автотест/управление извне (MCP, HTTP) | **AI-режим**: `--ai-mode` |
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
| `--theme <id>` | тема локации, переопределяет `active` в `themes.json` | `--theme castle`, `--theme=castle` |

Примеры:

```powershell
.\gradlew lwjgl3:run --args="--start-map 1/2"
.\gradlew lwjgl3:run --args="--start-map final --debug"
.\gradlew lwjgl3:run --args="--ai-mode 8080 --start-map 1/1"
.\gradlew lwjgl3:run --args="--ai-mode"
.\gradlew lwjgl3:run --args="--theme castle --start-map 1/1"
```

`--theme <id>` (`grass`, `castle`, …) применяется к заставке, меню и лабиринту.
Если темы с таким id нет — откат на `active` из `themes/themes.json`
(`ThemeLoader.resolveTheme`). Доступные темы — папки в `assets/themes/`.

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
