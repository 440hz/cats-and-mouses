# PLAN.md — текущий план разработки

Живой план этапа: цели, решения, задачи и статусы. Обновляется по ходу работ.
Связанные документы: [GAME.md](GAME.md) (общее описание и учёт),
[MENU.md](MENU.md) (экраны/меню/переходы — источник истины навигации),
[AGENTS.md](AGENTS.md) (правила работы агента).

## Текущий этап
**Этап 17 — Анимации: 17.1 «Сыр» (в работе). Версия 1.0.63.**
Ранее: **Этап 16 — Стартовый отсчёт «3 → 2 → 1»** (завершён, 1.0.56);
**Этап 15 — Сердечки** (15.1 редактор, 15.2 рендер/подбор, 15.3 игровой эффект, версия 1.0.54).
Параллельно: **Этап 11 — Релиз в RuStore** (версия 1.0.49 на модерации, ручная публикация); выполнены **Этап 13 — Kotlin-тесты** (198 тестов, `core:test`) и **Этап 14 — Desktop-редактор карт** (`--editor`, вне APK).
Инфраструктура: навык `check-balances` — проверка балансов OpenCode Go/Zen, SpriteCook и DeepSeek с выводом таблицами (все ключи балансов — в `opencode.json`, gitignored).
Зарезервировано на будущее: **Этап 117 — Дополнительные Android-магазины**, **Этап 118 — Desktop-релиз (Steam с нуля + itch.io)** (детали — раздел «Этапы на будущее» в конце).
Предыдущее: Этап 12 — Применение тем + миры + карта 32×16 + превью карт (готово);
Этап 7 — Темы (9 тем: `grass`, `castle`, `ice`, `desert`, `moss`, `swamp`, `space`, `volcano`, `kitchen`).
Обновлено: 2026-09-21.
Карты переработаны в редакторе (`1/1`, `1/3`, `1/5`, `2/2`, `2/3`, `final`); в `1/5` и `2/2` восстановлен старт `+`; билды пересобраны (198 тестов, Desktop-дистрибутив, debug/release APK).

## Цель
- Системная тема (`grass`) для заставки, меню и всех оверлеев.
- Тема на каждый мир (карта ≠ тема): `maps/<мир>/theme.json`, финал — `maps/final.json`.
- Карты 32×16 (было 28×16), авто-подсчёт миров/карт.
- Отладочный `--theme <id>`: форс темы + старт без меню.
- Dev-превью каждой карты (PNG, 1:1, не коммитим).

## Решения (зафиксированы)
- `themes/themes.json`: `active` → `system` (= `grass`).
- Тема мира: `assets/maps/<мир>/theme.json` = `{ "theme": "<id>" }`; `assets/maps/final.json`.
- Нет/неизвестная тема → системная.
- Порядок: миры по номеру → карты по имени → `final` последним.
- Индекс уровней — генерируемый `assets/maps/maps.json` (Gradle: `list()` не читает jar/APK).
- `--theme` — только отладка (без меню, дефолт `1/1`); прямой старт также при `--start-map`.
- `CELL_SIZE=40`, `MAZE_COLS=32` → `MAZE_OFFSET_X=0`; dual-grid рендер не меняем.
- Финал: открытое поле 32×16, стены-слово **КОНЕЦ**, 4 преследователя, сыр по стартам котов.

## Матрица уровней (31; все 7 тем)
| Мир | Тема | Карты | Сыр |
|---|---|---|---|
| 1 | grass | 5 (генерир.) | 1,2,2,3,3 |
| 2 | castle | 5 | 3,3,4,4,4 |
| 3 | ice | 5 | 4,4,5,5,5 |
| 4 | desert | 5 | 5,5,6,6,6 |
| 5 | moss | 5 | 6,6,7,7,7 |
| 6 | swamp | 5 | 7,8,8,8,9 |
| final | space | 1 | 4 (по A–D) |

## Задачи
### 1. Генератор и контент
- [x] `tools/gen_levels.py` — конфиг миров, связные 32×16, `+`/`A`–`D`/сыр, валидация, seed, `theme.json`.
- [x] `tools/map_preview.py` — превью PNG 1:1 (dual-grid + спрайты), вызов из генератора.
- [x] `maps/1..6` (30 карт) + `final.txt` + `final.json`.
### 2. Тема-система
- [x] `themes/themes.json`: `active` → `system`.
- [x] `ThemeLoader`: `system()` / `forId(id)` с fallback.
- [x] `LevelCatalog` (чтение `maps.json`).
- [x] Gradle-генерация `maps.json` (+ зависимости processResources/mergeAssets).
### 3. Код
- [x] `MainGame`: `systemTheme`, прямой старт при `--theme`/`--start-map`; тема уровня.
- [x] `GameScreen`: `LevelCatalog`, перезагрузка темы мира, оверлеи на системной теме.
- [x] `ScreenRouter`: `mapName` через каталог.
- [x] `Constants`: убрать `MAP_FILES`, `MAZE_COLS=32`, путь `maps.json`, бамп версии.
- [x] `Lwjgl3Launcher`: явный флаг `--start-map`.
### 4. Сборка и документы
- [x] `android`/`lwjgl3`: исключить превью из APK/jar.
- [x] `.gitignore`: `assets/maps/**/*.png|jpg`.
- [x] `MENU.md`, `GAME.md`, `run-game`, `AGENTS.md`, `PLAN.md`.
- [x] `versionCode 47` / `versionName 1.0.47`.
### 5. Проверка
- [x] `.\gradlew build` — BUILD SUCCESSFUL.
- [x] AI-прогон: темы миров (swamp/space/castle), 31 уровень, сыр, без исключений.
- [x] `installDist`: `maps.json` в jar, превью отсутствуют.
- [x] Запуск без флагов (заставка→меню) без краша.
- [ ] Визуальная проверка тем/меню пользователем (по желанию).
### 6. Dev-проверка карт (Python, вне сборки)
- [x] `tests/check_map.py` — статика + симуляция игры + вердикт ДА/НЕТ + маршрут (`--map <полный путь>`, `--runs`).
- [x] Эталоны `tests/fixtures/{win_100,fail_100,random_spawn}.txt` (100 % побед / 0 % / мульти-`+`, микс 40–60 %).
- [x] `tests/README.md`; разделы `GAME.md`/`AGENTS.md` обновлены.

## Порядок реализации
1. Генератор + превью → контент.
2. Тема-система, `LevelCatalog`, Gradle-манифест.
3. Код (ThemeLoader → MainGame → GameScreen → ScreenRouter → Constants → Launcher).
4. Документы, версия, `.gitignore`, сборка, прогон.

## Риски
- `list()` не читает jar/APK → решается `maps.json`.
- Dual-grid полтайл за границей → выступание вне карты (без обрезки стен).
- Превью автогенерируемы → в git не хранить, из сборки исключить.
- Сохранённый индекс уровня в `ProgressStore` зависит от порядка `maps.json` (при вставке карты в середину индекс сдвигается).

## Этап 11 — Релиз в RuStore
- [x] Ключ подписи `android/catsandmouses-release.jks` (RSA 2048, alias `catsandmouses`), `keystore.properties` (gitignored), `signingConfigs.release`/`buildTypes.release` в `android/build.gradle.kts`.
- [x] Релизные сборки: `android-release.aab` (9,8 МБ), `android-release.apk` (11,8 МБ); `jar verified`; `.\gradlew build` — OK.
- [x] Иконка витрины `store/rustore/icon_512.png` (512×512).
- [x] Скриншоты `store/rustore/screenshots/` — 5 геймплейных 1920×1080 JPG (grass/castle/ice/desert/final) + заставка.
- [x] Листинг: название «Кошки-мышки», краткое 69/80, полное 1165/4000, категории **Аркады** + **Казуальные**, 5 тегов, возраст **0+**, «Что нового».
- [x] Загрузка APK в RuStore Console (физлицо), режим публикации — **ручной**; версия **на модерации**.
- [x] Проверка карточки версии 1.0.49 через API (`screens -VersionId 2064833696`): **6** скриншотов, все `SCREENSHOT/LANDSCAPE` 1920×1080 (≥3 — ок). Найдено: кадр **№2 — оверлей паузы** (`05_menu.jpg`), кадр **№0 — заставка** со старой надписью «Нажми пробел».
- [ ] После одобрения 1.0.49 — «Опубликовать» (Консоль или `pwsh tools/rustore-publish.ps1 publish`).
- [ ] **1.0.50** (фикс заставки, APK/AAB готовы): загрузить версию (`release` или Консоль) и **починить карточку** — удалить кадр-паузу (`screens -DeleteId 8356347`), порядок «геймплей → заставка», заменить заставку на `07_loading.jpg` (надпись «Нажми, чтобы начать»).
- [ ] Обновления: поднимать `versionCode`/`versionName`, пересобирать тем же ключом.
- [ ] Любые изменения в RuStore — только с явного одобрения (правило 10 `AGENTS.md`).

## Этап 13 — Kotlin-тесты игровой логики
- [x] Инфраструктура: `core` — `junit-jupiter`, `kotlin("test")`, `gdx-backend-headless`; `test { useJUnitPlatform(); workingDir=assets; dependsOn(generateMapsManifest) }`.
- [x] Хелперы: `GdxTestBase`, `FakeApplication`, `FakeInput`, `FakePreferences`, `TestFakes`; фикстуры `core/src/test/resources` (+ реальные `assets`).
- [x] Tier 1: `Direction`, `MenuDefinitions`, `Constants`, `LaunchMode`.
- [x] Tier 2: `ProgressStore`, `UiButton`, `UiMenuOverlay`, `InputHandler`.
- [x] Tier 3: `MazeLoader`, `LevelCatalog`, `ThemeLoader`.
- [x] Tier 4: `Pellet`, `Mouse`, `Cat` (стратегии, BFS, тупик, резервация, форс-мув).
- [x] Tier 4b: вынос симуляции в `GameWorld` (без рендера) + `MazeLoader.fromText`; `GameWorldSimulationTest` (порядок кадра, сбор сыра, победа/поимка, `keepScore`).
- [x] `.\gradlew core:test` — 171 PASSED; `.\gradlew build` — `BUILD SUCCESSFUL`.
- [x] Документы: `AGENTS.md`, `GAME.md`, `PLAN.md`, `tests/README.md`.

## Этап 14 — Desktop-редактор карт (`--editor`)
- [x] `MapEditor.kt` (core): сетка 32×16, `setCell`/`fillRect`, `undo`/`redo` (лимит 200), `load`/`toText`, `validate()` (старты, достижимость сыра).
- [x] `EditorFiles.kt` (core): поиск папки `assets` (`--assets-dir` → `assets.dir` → `../assets`/`assets`), атомарное сохранение.
- [x] `MazeRenderer.kt` (core): общий рендер фона/стен (dual-grid + классические, доп-стены), вынос из `GameScreen`.
- [x] `EditorScreen.kt` (lwjgl3): WYSIWYG, палитра, кисть/заливка, листание, диалоги, playtest (F5), сохранение (`Ctrl+S`).
- [x] `GameScreen`: playtest-параметры `overrideMazeText`, `saveProgress`, `onExitOverride`; `MainGame.editorFactory`.
- [x] `Lwjgl3Launcher`: `--editor`, `--assets-dir`; `lwjgl3` — зависимость `gdx-freetype` + `assets.dir` в dev-запуске.
- [x] Тесты: `MapEditorTest`, `EditorAssetsTest`, `MainGameEditorTest` (29; всего 171).
- [x] Версия **1.0.51**; `RUN.md`, `run-game`, `GAME.md`, `AGENTS.md`.
- [x] Проверка: `.\gradlew core:test` — 171 PASSED; `.\gradlew build` — `BUILD SUCCESSFUL`.
- [x] Фикс выхода из playtest: ESC теперь возвращает в редактор (dispose+`setScreen` через `Gdx.app.postRunnable`, иначе рендер работал с освобождённым `SpriteBatch` и приложение падало). Подтверждено пользователем.
- [x] Сетка лабиринта переключается клавишей **G** (`showGrid`), подсказка «Сетка: вкл/выкл».
- [x] Размер маркеров в редакторе = игровому (48 = `Mouse`/`Cat`/`Pellet.SPRITE_SIZE`), WYSIWYG.
- [x] Экспорт превью PNG `<карта>.png` рядом с `.txt` при сохранении (offscreen `FrameBuffer` + `PixmapIO`, аналог `tools/map_preview.py`); PNG gitignored.
- [x] Сыр: один инструмент + счётчик (`×N`, лимит 9), ЛКМ только на `' '`, ПКМ стереть, заливка с лимитом; `MapEditor.normalizeCheese()` нумерует `1..N` при сохранении; окно «Карта сохранена» с «ОК»; +4 теста (всего 175).
- [x] Визуальная проверка редактора пользователем (по желанию).
- [x] Неблокирующие запуски Desktop: хелпер `registerDetachedRun` + задачи `runBg` (игра, `-PgameArgs`), `runEditor` (`--editor`), `runDistBg` (дистрибутив) — отдельный процесс, задача завершается сразу, логи в `lwjgl3/build/{run-logs,editor-logs}/`; правило «все запуски неблокирующие, не ждать в фоне» в `AGENTS.md`, `RUN.md`, навыке `run-game`, `GAME.md`; проверено: закрытие окна корректно гасит процесс.

## Этап 15 — Сердечки (heart pickups)
Зафиксированные решения:
- Символ на карте — латинская **`H`** (от HEART).
- До **3 сердечек на карте** (лимит редактора); при переборе лишнее игнорируется.
- **`MAX_LIVES = 7`** — максимум жизней-сердечек, которые мышь накапливает (реализуется на этапе 15.3).
- Рисование сердечка — как сыра (спрайт на клетке); в редакторе — иконка + счётчик.
- Игровой эффект съедания — реализован (этап 15.3): +1 жизнь (кап 7), поимка с запасом — телепорт + неуязвимость.
- Спрайты уже есть: `sprites/heart_full.png` (`52e2e7e3`), `heart_empty.png` (`c0225f37`).

### 15.1 — Редактор (готово)
- [x] `MazeLoader.findHeartCells()` (символ `H`), `Constants.MAX_HEARTS_PER_MAP = 3`.
- [x] `MapEditor`: `ALLOWED += 'H'`, `heartCount()`, `fillHeartRect()` (кап 3, лишнее игнорируется), `H` не считается сыром.
- [x] `EditorScreen`: инструмент-сердечко (`heart_full.png`) + счётчик `×N/3`, хоткей `H`, кисть/ПКМ/заливка.
- [x] `tests/check_map.py`: `allowed += 'H'`.
- [x] Тесты: `MapEditorTest` (кап/игнор/не путается с сыром), `MazeLoaderTest.findHeartCells`.
- [x] Документы (`GAME.md`, `AGENTS.md`, `README.md`), версия `1.0.52`.
- [x] Проверка: `.\gradlew core:test` (182 PASSED), `.\gradlew build` — `BUILD SUCCESSFUL`.

### 15.2 — Рисование сердец в игре + съедание без эффектов (готово)
- [x] Сущность `Heart(cell, maze)` по образцу `Pellet` (флаг `collected`); `HeartTest` (+5).
- [x] `GameWorld`: список `hearts`, спавн `spawnHearts()` из `findHeartCells()`; съедание помечает сердце собранным (без очков/жизней/эффектов).
- [x] `GameScreen`: `drawHearts()` — спрайт на клетке как сыра (между сыром и сущностями).
- [x] Тесты `GameWorldSimulationTest` (+2: спавн по `H`, подбор без эффектов). `.\gradlew core:test` — 192 PASSED, `.\gradlew build` — `BUILD SUCCESSFUL`.

### 15.3 — Эффект при съедании (готово)
Зафиксированные решения:
- Стартовый запас **`START_LIVES = 1`**; сердечко даёт **+1 жизнь** до **`MAX_LIVES = 7`**.
- Жизни **копятся между картами** и **хранятся в `ProgressStore`** (`KEY_LIVES`): переносятся на рестарт-по-продвижению, «Следующая карта» и «Продолжить».
- При смерти (жизни = 0) новая попытка начинается с **1** (`START_LIVES`); `saveOnDeath` сбрасывает жизни, `clear()` тоже.
- Поимка с запасом: жизнь −1, мышь **телепортируется** в достижимую клетку, **максимально далёкую от всех котов** (мульти-BFS от котов), и получает **неуязвимость `INVULN_TIME = 1.5 с`** (мигает ~10 Гц); экран «Мышка попалась!» не показывается.
- HUD: 7 слотов по **`HUD_HEART_SIZE = 36`** (карта — 48); неактивные — **серый силуэт** `sprites/heart_grey.png` (`#5A5A5A`, производный от `heart_full`, изготовлен локально без SpriteCook); `heart_empty.png` из HUD убран.
- `tests/check_map.py` **не менялся** (одна попытка без жизней) — расхождение задокументировано в `tests/README.md`.

Реализация:
- [x] `Constants`: `MAX_LIVES 3→7`, `START_LIVES`, `HUD_HEART_SIZE`, `INVULN_TIME`, `KEY_LIVES`, `VERSION 1.0.54`.
- [x] `entities/Mouse.teleportTo(col,row)`.
- [x] `GameWorld`: `lives`/`invulnTimer`, `startLevel(startLives)`, подбор `+1` (кап), `checkCatCollision` (жизнь/телепорт/неуязвимость), `farthestCellFromCats()`.
- [x] `ProgressStore`: `savedLives()`, `saveOnDeath`/`saveOnAdvance(..., lives)`, `clear()`.
- [x] `MainGame.startGameDirect(..., startLives)`, `ScreenRouter.continueGame()`.
- [x] `GameScreen`: HUD 7×36, `heart_grey`, мигание, `getLives()`.
- [x] Спрайт `sprites/heart_grey.png` + `view/`, `asset-manifest.json`/`.md`.
- [x] Тесты: `GameWorldSimulationTest` (+6), `ProgressStoreTest` (+3), `ConstantsTest` (+проверки). `.\gradlew core:test` — **198 PASSED**; `.\gradlew build` — `BUILD SUCCESSFUL`.
- [x] Документы: `MENU.md`, `GAME.md`, `PLAN.md`, `CHANGELOG.md`, `tests/README.md`.
- [ ] Ручная проверка (запуск Desktop, чеклист) — пользователем.

## Этап 16 — Стартовый отсчёт «3 → 2 → 1»
Цель: при старте карты дать игроку ~3 секунды на подготовку — крупные цифры **3 → 2 → 1** по центру, затем игра начинается.

Зафиксированные решения:
- Порядок **3 → 2 → 1** (по 1 c, итого 3 c), без «GO/СТАРТ». `Constants.COUNTDOWN_TIME = 3f`.
- Отсчёт в `GameWorld` (симуляция без рендера): `countdownTimer`, `isCountdown`, `countdownNumber`; параметр `startLevel(..., countdown)`; `update()` во время отсчёта уменьшает таймер и выходит (мышь/коты/сыр не двигаются, коллизий нет).
- Ввод во время отсчёта буферизуется (`InputHandler` хранит направление) и срабатывает сразу после отсчёта; Space/ESC/BACK/тап игнорируются.
- Визуал: крупная цифра по центру (`Constants.FONT_SIZE_COUNTDOWN = 192`), **наплыв и затухание** (alpha + масштаб) и синхронное затемнение фона.
- Отключение: `--ai-mode`, playtest редактора, dev-флаг `--no-countdown`. AI-состояние — `COUNTDOWN`.
- Отсчёт применяется при всех реальных стартах: новая игра, «Попробуй ещё!», «Следующая карта», AI `reset`.

Задачи:
- [x] `Constants`: `COUNTDOWN_TIME`, `FONT_SIZE_COUNTDOWN`, версия `1.0.56`.
- [x] `GameWorld`: поля отсчёта + блокировка `update`; `startLevel(..., countdown = COUNTDOWN_TIME)`.
- [x] `GameScreen`: `drawCountdown()` с анимацией, отдельный шрифт `1/2/3`, ветка `COUNTDOWN` в `getGameState()`, передача `countdownEnabled`.
- [x] `MainGame`/`Lwjgl3Launcher`/`EditorScreen`: флаг `--no-countdown`, авто-отключение в AI и playtest.
- [x] Тесты: `GameWorldSimulationTest` (+3), `ConstantsTest` (+1); 4 теста переведены на `countdown = 0f`. `.\gradlew core:test` — **202 PASSED**; `.\gradlew build` — `BUILD SUCCESSFUL`.
- [x] Документы: `MENU.md`, `GAME.md`, `CHANGELOG.md`, `tests/README.md`, `PLAN.md`.
- [ ] Ручная проверка (запуск Desktop: старт/рестарт/следующая карта, `--no-countdown`) — за пользователем.

## Этап 17 — Анимации
Цель: оживить объекты. Анимации — **по типам**, у каждого свои. Сейчас реализован сыр; мышь, коты и сердца — отдельные этапы.

Зафиксированные решения:
- Два механизма (`core/.../graphics/`):
    - **кадровый** `AnimatedSprite` — горизонтальный спрайт-лист `sprites/<name>_sheet.png` (кадры 64×64) для нарисованных кадров (шаг мыши/котов);
    - **процедурный idle-акцент** `OccasionalAnimator` — объект статичен, по случайному интервалу проигрывает один случайный акт и возвращается; наборы актов — на тип.
- Наборы актов и интервалы — по типу; логика чистая (без GL), детерминирована по seed.
- Время ведёт рендер (`GameScreen`) и тикает **только** в живой игре (не на отсчёте/паузе/оверлеях) — `GameWorld` остаётся render-free.
- RAW/`view/` листов хранятся в git, в игровую сборку/APK не попадают.

### 17.1 — Сыр (готово)
Решение: сыр **обычно статичен**; иногда проигрывает один случайный акцент. Подход — процедурный.
- [x] `OccasionalAnimator.kt` — расписание (случайный интервал), случайный акт из набора, точный возврат в identity; чистые `Act`/`ActTransform`/`ActCurves`.
- [x] Разнообразие: соль на запуск уровня (`Random.nextLong() xor cellSeed`, `GameScreen`) — открытие меняется между перезапусками; один и тот же акт не выпадает дважды подряд.
- [x] `CheeseAnimation.kt` — акты: покачивание ±15° (0.5 с), разбухание 1.0→1.30→1.0 (0.7 с), сдувание 1.0→0.775→1.0 (0.7 с); `create(seed)`, `cellSeed(col,row)`.
- [x] `Constants`: интервал 2–3 с, длительности/амплитуды; убраны `CHEESE_FRAME_*`.
- [x] `Pellet.draw(...)` — необязательные `scale`/`rotation`/`offsetY` (дефолты = прежнее поведение).
- [x] `GameScreen`: статичный `cheese.png`, аниматор на каждый сыр, тик только в живой игре, `dispose()`.
- [x] Откат кадрового варианта: `sprites/cheese_sheet.png` + `view/` убраны из игры; RAW `sprites/raw/cheese_sheet.png` оставлен архивом (запись в манифесте).
- [x] Тесты: `OccasionalAnimatorTest` (+13), `CheeseAnimationTest` (+9), правка `ConstantsTest`; `AnimatedSpriteTest` (+6, механизм для будущих). `.\gradlew core:test` — **231 PASSED**; `.\gradlew build` — `BUILD SUCCESSFUL`.
- [ ] Ручная проверка (запуск Desktop: сыр статичен, изредка акценты; заморозка на паузе/отсчёте/оверлеях) — за пользователем.

### 17.2+ — Дальше (не начато, отдельные этапы)
- Мышь: шаг/покой (кадровый лист).
- Коты: шаг для каждого (4 листа).
- Сердечки: пульс/мерцание (процедурный акцент).
- Опционально: анимация UI (кнопки, логотип загрузки).

## Этапы на будущее (зарезервированы)

### Этап 117 — Дополнительные Android-магазины
Статус: не начат. Цель — разместить подписанный APK/AAB помимо RuStore.
- Магазины: Huawei AppGallery, Xiaomi GetApps, NashStore.
- Переиспользуем: keystore `android/catsandmouses-release.jks`, пакет `com.catsandmouses`,
  иконку `store/rustore/icon_512.png`, скриншоты 1920×1080 (`store/rustore/screenshots/`).
- Фазы: структура `store/<магазин>/` + `store/README.md`; общий листинг `store/common/listing.ru.json`;
  `store/common/privacy.ru.md` (+EN); `tools/store-assets.py` (производные иконки/баннеры);
  затем по магазинам; навык `store-publish`; документы.
- Открыто: список магазинов (добавлять Aptoide/APKPure/Amazon? Google Play — ограничен для РФ);
  аккаунты/верификация (ИП/юрлицо?); privacy-URL (GitHub Pages зеркала или свой сайт);
  языки (RU/EN); формат (навык + `store/README.md` или только чек-листы).

### Этап 118 — Desktop-релиз: Steam с нуля + itch.io
Статус: не начат. Steam-аккаунта/приложения/страницы нет — всё с нуля.
- Ф0 — Steamworks: аккаунт + Steam Guard, partner-регистрация, Steam Direct **$100**, W-8BEN,
  банковские реквизиты (риск выплат из РФ — проверить до оплаты), AppID/DepotID;
  сроки: **+30 дней** после оплаты, Coming Soon **≥2 недели** до релиза (первый релиз ~6–8 недель).
  Обязательная **декларация AI-контента** (часть спрайтов/тем сгенерированы SpriteCook).
- Ф1 — self-contained Windows-билд: Gradle-задача на **`jpackage`** (`--type app-image`, вложенная JRE),
  `tools/make_icon.py` → `app.ico`, брендинг/заголовок окна, fullscreen/разрешения.
- Ф2 — депоты + SteamPipe: `steam/app_build_<appid>.vdf`, `steam/depot_build_<depotid>.vdf`,
  обёртка `tools/steam-build.ps1`; ветки `default` + `beta`.
- Ф3 — витрина: капсулы (header 460×215, small 231×87, main 1232×706, vertical 374×448,
  page bg 1438×810, library 600×900, library hero 3840×1240, лого ≤1280×720), скриншоты (есть),
  трейлер, описания RU+EN, теги, возрастной опрос Valve.
- Ф4 — тестовый билд в `beta`. Ф5 — релиз (только с явного подтверждения пользователя).
- Ф6 — itch.io (тот же self-contained ZIP). Ф7 — (опц.) `steamworks4j`: достижения/Cloud/overlay.
- Открыто: тип аккаунта (физлицо/компания); платформы (Windows-only или +Linux/macOS, нужен CI);
  цена (бесплатно/платно); бренд («Кошки-мышки» / «Cats and Mouses»); SDK-фичи (сразу/позже);
  трейлер; капсулы (локально или SpriteCook).

### Этап 119 — Звук и музыка (SFX + фоновая музыка)
Статус: не начат (зарезервирован). Цель — музыкальное сопровождение и звуковые эффекты.
- **Источник ассетов (блокер)**: инструмента генерации музыки в проекте нет (SpriteCook — только изображения). Варианты: CC0-паки Kenney.nl (SFX/джинглы, безопасно для RuStore); AI-сервис (Suno/Udio — проверить коммерческую лицензию); готовые файлы пользователя; комбинация. Формат — **OGG Vorbis**.
- **`AudioManager` (core)**: `Music` + пул `Sound`, громкости/тумблеры из `ProgressStore` (`KEY_MUSIC_ENABLED`/`KEY_SFX_ENABLED`); API `playMusic`/`stopMusic`/`pauseMusic`/`resumeMusic`/`playSfx`/`setMusicEnabled`/`setSfxEnabled`; **no-op при `Gdx.audio == null`** (headless-тесты); создаётся и `dispose()` в `MainGame`, прокидывается в экраны; папки `assets/audio/{music,sfx}/` + `raw/` (в git, исключить из jar/APK, как `sprites/raw`).
- **SFX — по дельтам кадра в `GameScreen`** (симуляция `GameWorld` остаётся чистой): сыр, сердечко, потеря жизни/телепорт, game over, win, тик отсчёта, клик кнопки.
- **Музыка**: заставка/меню — трек меню; геймплей — трек(и) по темам/настроению; на паузе — приглушение; Android — `LifecycleListener` (pause/resume при сворачивании).
- **Настройки**: экран «Настройки» (сначала спецификация в `MENU.md`), тумблеры «Музыка»/«Звуки», `MenuDefinitions`/`ProgressStore`.
- **Тесты**: `AudioManager` (no-op), `ProgressStore` (дефолты/персист тумблеров), `MenuDefinitions` (спека настроек); `MainGameEditorTest` остаётся зелёным.
- Открыто: объём (только музыка или + SFX); источник ассетов и кто конвертирует в OGG (нужен ли `ffmpeg`); музыка на каждую из 9 тем или 1 трек меню + 1–3 геймплейных; отдельный экран настроек или тумблеры в меню; вкл/выкл или ползунки громкости.

## История
- 2026-09-21 — **этап 17.1-бис (сыр: процедурные акценты)**: сыр обычно статичен, изредка проигрывает один случайный акт — покачивание ±15°, разбухание (+30%), сдувание (−22.5%) — через случайный интервал 2–3 с; механизм `OccasionalAnimator` (чистая логика, seed по клетке + соль на запуск, без повторов подряд) + `CheeseAnimation`; время тикает только в живой игре; кадровый вариант (лист `cheese_sheet`) откачен, RAW оставлен архивом; тесты 231, `BUILD SUCCESSFUL`; версия **1.0.63**.
- 2026-09-21 — **этап 17.1 (анимация сыра, кадровый вариант — откачен)**: общий механизм `AnimatedSprite` (покадровый лист, `regionAt(elapsed, phase)`, чистая `frameIndexAt`), время анимации в `GameScreen`, сдвиг фазы по клетке; анимация сыра от SpriteCook (6 кадров, блик + покачивание, 20 кредитов) → `tools/normalize_sheet.py` → `sprites/cheese_sheet.png`; тесты 209; версия 1.0.62. Механизм `AnimatedSprite` сохранён для будущих мыши/котов.
- 2026-09-21 — **этап 17.1 (анимация сыра)**: общий механизм `AnimatedSprite` (покадровый лист, `regionAt(elapsed, phase)`, чистая `frameIndexAt`), время анимации в `GameScreen` (`animationClock`, только в живой игре), сдвиг фазы по клетке; анимация сыра от SpriteCook (6 кадров, блик + покачивание, 20 кредитов) → `tools/normalize_sheet.py` → `sprites/cheese_sheet.png`; тесты 209, `BUILD SUCCESSFUL`; версия **1.0.62**; ручная проверка — за пользователем.
- 2026-09-21 — **этап 16 (стартовый отсчёт)**: 3 секунды «3 → 2 → 1» перед началом карты; мышь/коты заморожены, ввод буферизуется, пауза/меню недоступны; визуал с наплывом и затуханием; отсчёт в `GameWorld` (+AI-состояние `COUNTDOWN`); отключение в `--ai-mode`/playtest/`--no-countdown`; тесты 202, `BUILD SUCCESSFUL`; версия **1.0.56**; ручная проверка — за пользователем.
- 2026-09-21 — **этап 15.3 (сердечки: эффект)**: `START_LIVES=1`, `MAX_LIVES 3→7`, подбор +1; поимка с запасом — жизнь −1, телепорт в самую далёкую от котов клетку (BFS), неуязвимость 1.5 с + мигание; жизни копятся и хранятся в `ProgressStore` (`KEY_LIVES`), «Продолжить» восстанавливает; HUD 7×36, неактивные — `heart_grey` (`#5A5A5A`, локально); тесты 198, `BUILD SUCCESSFUL`; версия **1.0.54**; ручная проверка — за пользователем.
- 2026-09-21 — яркость тем: `tools/adjust_theme.py` (RAW → фон RGB × `backgroundShade`, стены `c + (255−c)×wallLighten`, дефолт 15%, затем LANCZOS-масштаб → игровые файлы); значения хранятся в `theme.json` (`backgroundShade`/`wallLighten`) для воспроизводимой перегенерации из `raw/`. Тема `castle` перегенерирована на 15%, `view/` обновлён; навык `generate-theme` дополнен.
- 2026-09-21 — все запуски Desktop сделаны неблокирующими: хелпер `registerDetachedRun` + задачи `lwjgl3:runBg` (игра, аргументы `-PgameArgs="..."`), `runEditor` (`--editor`), `runDistBg` (дистрибутив). Стартуют отдельный процесс, задача завершается сразу; логи в `lwjgl3/build/{run-logs,editor-logs}/`. Проверено: закрытие окна корректно завершает процесс. Правило «не ждать в фоне» закреплено в `AGENTS.md`, `RUN.md`, навыке `run-game`, `GAME.md (п. 85)`.
- 2026-09-20 — правки редактора: выход из playtest по ESC возвращает в редактор (dispose+`setScreen` отложены через `Gdx.app.postRunnable`), сетка вкл/выкл по клавише **G**.
- 2026-09-20 — Desktop-редактор карт (`--editor`): `MapEditor`/`EditorFiles`/`MazeRenderer` (core), `EditorScreen` (lwjgl3), playtest через `GameScreen`, сохранение в `assets/maps`; +29 тестов (171); версия 1.0.51; документы (`RUN.md`, `run-game`, `AGENTS.md`).
- 2026-09-19 — Kotlin-тесты (142, JUnit5 + headless): Tier 1–3 (логика/UI/файлы) и Tier 4 (симуляция `Mouse`/`Cat`); рефакторинг: игровой цикл вынесен из `GameScreen` в `GameWorld`, добавлен `MazeLoader.fromText`; команда `.\gradlew core:test`, тесты входят в `.\gradlew build`.
- 2026-09-19 — проверка карточки RuStore через API (`screens`): версия 1.0.49, 6 скриншотов 1920×1080 LANDSCAPE; выявлены дефекты (кадр №2 — оверлей паузы, №0 — заставка первой) — исправить в 1.0.50. Исправлен баг в `tools/rustore-publish.ps1` (URL списка скриншотов: `?` после переменной).
- 2026-09-19 — навык `rustore-publish` (SKILL.md + reference/api.md + reference/ids.md), полная API-автоматизация `tools/rustore-publish.ps1` (версии, медиа, отзывы, рейтинг), Gradle-задача `rustoreRelease`, ключ в gitignored `rustore-api.properties`; проверено на реальном ключе (`apps`, `status` — 1.0.49 в модерации).
- 2026-09-19 — автоматизация RuStore: `tools/rustore-publish.ps1` (token/apps/status/release/publish), `store/rustore/version.json`, секреты в gitignored `rustore-api.properties`; проверена подпись+HTTP.
- 2026-09-19 — заставка: подсказка «Нажми, чтобы начать» (нейтрально для устройств); версия **1.0.50** (versionCode 50); пересобраны `.apk`/`.aab`, переснят скриншот заставки; нужна повторная загрузка в RuStore.
- 2026-09-19 — релиз в RuStore: keystore/подпись, релизные `.aab`/`.apk`, иконка 512×512, скриншоты 1920×1080, ASO-листинг; APK загружен, версия на модерации. Установлен навык `.opencode/skills/rustore-aso/`.
- 2026-09-18 — dev-чекер карт `tests/check_map.py` (статика + симуляция + вердикт + маршрут), эталоны `tests/fixtures/`.
- 2026-09-18 — отжим активной кнопки (влево-вверх на 10 px, при нажатии — на место); `UiButton.isFocused`/`isPressed`; версия 1.0.48.
- 2026-09-18 — создан документ; зафиксирован этап 12 (темы/миры/32×16/превью), реализовано.
