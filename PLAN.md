# PLAN.md — текущий план разработки

Живой план этапа: цели, решения, задачи и статусы. Обновляется по ходу работ.
Связанные документы: [GAME.md](GAME.md) (общее описание и учёт),
[MENU.md](MENU.md) (экраны/меню/переходы — источник истины навигации),
[AGENTS.md](AGENTS.md) (правила работы агента).

## Текущий этап
**Этап 17 — Анимации: 17.1 «Сыр» + 17.3 «Анимация в редакторе» + 17.4 «Сердечко: постоянный пульс» (в работе; ручные проверки закрыты). Версия 1.0.65.**
Ранее: **Этап 16 — Стартовый отсчёт «3 → 2 → 1»** (завершён, 1.0.56);
**Этап 15 — Сердечки** (15.1 редактор, 15.2 рендер/подбор, 15.3 игровой эффект, версия 1.0.54).
Параллельно: **Этап 11 — Релиз в RuStore** (версия 1.0.49 модерацию прошла — `READY_FOR_PUBLICATION`, ручная публикация; решено не публиковать, первой версией выпустить **1.1.1** после доработки карт и ручных тестов на телефоне); выполнены **Этап 13 — Kotlin-тесты** (198 тестов, `core:test`) и **Этап 14 — Desktop-редактор карт** (`--editor`, вне APK).
Инфраструктура: навык `check-balances` — проверка балансов OpenCode Go/Zen, SpriteCook и DeepSeek с выводом таблицами (все ключи балансов — в `opencode.json`, gitignored).
Зарезервировано на будущее: **Этап 117 — Дополнительные Android-магазины**, **Этап 118 — Desktop-релиз (Steam с нуля + itch.io)** (детали — раздел «Этапы на будущее» в конце).
Предыдущее: Этап 12 — Применение тем + миры + карта 32×16 + превью карт (готово);
Этап 7 — Темы (9 тем: `grass`, `castle`, `ice`, `desert`, `moss`, `swamp`, `space`, `volcano`, `kitchen`).
Обновлено: 2026-10-16.
Карты переработаны в редакторе (`1/1`, `1/3`, `1/5`, `2/2`, `2/3`, `final`); в `1/5` и `2/2` восстановлен старт `+`; билды пересобраны (198 тестов, Desktop-дистрибутив, debug/release APK).
Добавлены **мир 7 «Вулкан»** и **мир 8 «Гигантская кухня»** (по 5 карт, темы `volcano`/`kitchen`) — всего **41 уровень**; генератор `gen_levels.py` получил фильтр миров и не перезаписывает `final`.
Готово: **фильтр уровней для мобильной сборки** — флаг `"use"` в `maps.json` (сохраняется `generateMapsManifest` при перегенерации, дефолт `true`); в APK попадают только уровни с `use:true` (чистка карт/превью/`theme.json` + перезапись манифеста), Desktop/dev не фильтруются.

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

## Матрица уровней (41; 8 миров + финал)
| Мир | Тема | Карты | Сыр |
|---|---|---|---|
| 1 | grass | 5 (генерир.) | 1,2,2,3,3 |
| 2 | castle | 5 | 3,3,4,4,4 |
| 3 | ice | 5 | 4,4,5,5,5 |
| 4 | desert | 5 | 5,5,6,6,6 |
| 5 | moss | 5 | 6,6,7,7,7 |
| 6 | swamp | 5 | 7,8,8,8,9 |
| 7 | volcano | 5 | 8,8,9,9,9 |
| 8 | kitchen | 5 | 9,9,9,9,9 |
| final | space | 1 | 4 (по A–D) |

## Мир 7 — Вулкан (готово)
Цель: завести новый мир прохождения на уже готовой теме `volcano` (п. 90 `GAME.md`), не трогая существующие карты.
- [x] `tools/gen_levels.py`: запись мира `{"id": 7, "theme": "volcano", "density": 0.32, "cheese": [8,8,9,9,9]}` + фильтр запуска по номерам миров (`python tools/gen_levels.py 7`); при фильтре `final` не перегенерируется (защита ручных правок).
- [x] Сгенерированы `assets/maps/7/1..5.txt` (32×16, связные, `+`/`A`–`D`/сыр) + `assets/maps/7/theme.json`.
- [x] `assets/maps/maps.json` пересобран (`generateMapsManifest`) — 36 уровней, мир 7 перед `final`.
- [x] Dev-превью `assets/maps/7/*.png` (`tools/map_preview.py`); PNG gitignored.
- [x] Проверка: `.\gradlew core:test` (PASSED), `.\gradlew build` (`BUILD SUCCESSFUL`).
- [x] Документы: `GAME.md` (п. 98, структура карт `1`…`7`), `CHANGELOG.md`, `PLAN.md`.

## Мир 8 — Гигантская кухня (готово)
Цель: завести мир на готовой теме `kitchen`; лабиринт должен заполнять **всю** карту (без «вырезанных» пустых блоков).
- [x] `tools/gen_levels.py`: запись мира `{"id": 8, "theme": "kitchen", "density": 0.36, "cheese": [9,9,9,9,9]}`; `python tools/gen_levels.py 8`.
- [x] **Генератор исправлен**: `add_extra_walls` добавляет стену, только если все проходные клетки остаются связными (`passable_connected`) — без `keep_largest_component`, который при высокой плотности отрезал куски карты (пустые «блоки» стен). Проход теперь занимает всю карту (~240–250 клеток из 420).
- [x] Сгенерированы `assets/maps/8/1..5.txt` (32×16, единый связный проход, `+`/`A`–`D`/9 сыров) + `assets/maps/8/theme.json` = `{"theme":"kitchen"}`.
- [x] `assets/maps/maps.json` пересобран (`generateMapsManifest`) — **41 уровень**, мир 8 перед `final`.
- [x] Dev-превью `assets/maps/8/*.png`; статика `tests/check_map.py` — OK (все клетки/сыр достижимы).
- [x] Проверка: `.\gradlew build` (`BUILD SUCCESSFUL`).
- [x] Документы: `GAME.md` (п. 101, структура карт `1`…`8`), `CHANGELOG.md`, `PLAN.md`.

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
- [x] Визуальная проверка тем/меню пользователем — подтверждено пользователем.
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

## Этап 11 — Релиз в RuStore (первая версия — 1.2.4, готово к загрузке вручную)
- [x] Ключ подписи `android/catsandmouses-release.jks` (RSA 2048, alias `catsandmouses`), `keystore.properties` (gitignored), `signingConfigs.release`/`buildTypes.release` в `android/build.gradle.kts`.
- [x] Иконка витрины `store/rustore/icon_512.png` (512×512); листинг и FAQ — `store/rustore/version.json` + `store/rustore/FAQ.md` (41 уровень / 8 миров, категории **Аркады** + **Казуальные**, теги `51,52,58,61,21`, возраст **0+**).
- [x] Загрузка/модерация 1.0.49 (историческое): версия прошла модерацию (`READY_FOR_PUBLICATION`, versionId `2064833696`), **решено не публиковать** — первой версией выпускаем 1.2.4, а 1.0.49 архивируем.
- [x] **Скриншоты пересняты:** 9 кадров 1920×1080 (`store/rustore/screenshots/`: grass/castle/ice/desert/volcano/kitchen + меню + настройки + заставка) через dev-флаги `--window/--shot/--shot-screen`; старые (с кадром-паузой) удалены.
- [x] **1.2.4**: фикс языка на Android (`values-ru` + `localeConfig`), кнопка «Выход» в меню на всех платформах, dev-флаги снимка; `versionCode 102004`; ручной прогон release на POCO X6 Pro — ОК.
- [x] План публикации обновлён — `store/rustore/RELEASE-PLAN.md`.
- [x] **1.2.4 загружена в Консоль вручную** (2026-10-12): архивирована 1.0.49 (`ARCHIVED`), загружен `android-release.apk`, поля из `version.json` + иконка + 9 скриншотов, режим **ручной**. Версия 1.2.4 (versionId `2064877739`, code 102004) — **`MODERATION`** (проверка до 3 дней).
- [ ] После одобрения модерации — **опубликовать** (`pwsh tools/rustore-publish.ps1 publish` или в Консоли).
- [ ] Обновления: поднимать `versionCode`/`versionName`, пересобирать тем же ключом.
- [ ] Любые изменения в RuStore — только с явного одобрения (правило 10 `AGENTS.md`).

### Продвижение — статья в Блог RuStore
- [x] Черновик обзора подготовлен — `store/blog/BLOG-ARTICLE.md` (формат «Обзор», категория «Игры», ~5 000 знаков, шапка + 5 разделов + FAQ), требования и порядок подачи — `store/blog/README.md`.
- [x] Обложка `store/blog/01_обложка.png` — HD-иллюстрация (`gemini-3-pro-image` 2K → 1568 × 880 ≥ 784 × 440, заголовок локально `tools/make_blog_cover.py`); пиксель-альтернатива `01_обложка_pixel.png`, RAW — `store/blog/raw/`.
- [x] Вёрстка `store/blog/article.docx` — Arial, название и все подзаголовки стилем «Заголовок 1», обложка в тексте; генератор `tools/make_blog_docx.py` (python-docx, dev-only). В текст добавлены кадры геймплея (`02_геймплей.jpg`, `04_настройки.jpg`) — форма требует ≥ 1 изображение в поле «Изображения».
- [x] Уникальность проверена — **99 %** (сторонний онлайн-сервис антиплагиата), ≥ 90 %.
- [x] **Подано 2026-10-16** через Консоль → Хелп-центр → «Предложить статью» (обзор, «Игры», ссылка на карточку `com.catsandmouses`): приложен `article.docx`, обложка и кадры геймплея; форма подтвердила приём. Обновлены `store/blog/README.md`, `store/blog/BLOG-ARTICLE.md`, `GAME.md`, `CHANGELOG.md`.
- [ ] Дождаться ответа редакции (до 10 рабочих дней); доработки и повторная подача — **только с одобрения пользователя**.

### Продвижение — девлог о разработке (рабочий блог)
- [x] `blog/ai-devlog/`: `ARTICLE.md` — лонгрид (~19 600 знаков; шутливый, честный, с цифрами/суммами/временем), `PLAN.md` (вводные зафиксированы), `README.md`.
- [x] Иллюстрации — `blog/ai-devlog/images/` (12 шт.): схемы/графики — `tools/make_devlog_illustrations.py` (Pillow, dev-only); скриншоты игры и сессии (`shot_dialog.png`, `shot_plan.png`).
- [ ] Публикация во внутреннем рабочем блоге — автором (вне репозитория).

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
- [x] Ручная проверка (запуск Desktop, чеклист) — подтверждено пользователем.

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
- [x] Ручная проверка (запуск Desktop: старт/рестарт/следующая карта, `--no-countdown`) — подтверждено пользователем.

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
- [x] `CheeseAnimation.kt` — акты: покачивание ±15° (0.5 с), разбухание 1.0→1.50→1.0 (0.7 с), сдувание 1.0→0.50→1.0 (0.7 с); `create(seed)`, `cellSeed(col,row)`.
- [x] `Constants`: интервал 2–3 с, длительности/амплитуды; убраны `CHEESE_FRAME_*`.
- [x] `Pellet.draw(...)` — необязательные `scale`/`rotation`/`offsetY` (дефолты = прежнее поведение).
- [x] `GameScreen`: статичный `cheese.png`, аниматор на каждый сыр, тик только в живой игре, `dispose()`.
- [x] Откат кадрового варианта: `sprites/cheese_sheet.png` + `view/` убраны из игры; RAW `sprites/raw/cheese_sheet.png` оставлен архивом (запись в манифесте).
- [x] Тесты: `OccasionalAnimatorTest` (+13), `CheeseAnimationTest` (+9), правка `ConstantsTest`; `AnimatedSpriteTest` (+6, механизм для будущих). `.\gradlew core:test` — **231 PASSED**; `.\gradlew build` — `BUILD SUCCESSFUL`.
- [x] Ручная проверка (запуск Desktop: сыр статичен, изредка акценты; заморозка на паузе/отсчёте/оверлеях) — подтверждено пользователем.

### 17.3 — Анимация выбранного типа в редакторе (готово)
Решение: в редакторе анимируются **объекты на карте** того типа, чей инструмент выбран; HUD/палитра — нет. Реальная анимация пока только у сыра, для остальных — **заглушка**.
- [x] `EditorScreen`: `animationClock` (тикает в `render(delta)`, пауза при открытом диалоге), `markerAnimators` (ленивые аниматоры по клетке, живут между кадрами), `syncAnimators` (чистка лишних/чужих клеток), `animationFactoryFor`.
- [x] Реестр-заглушка: `CHEESE_CHAR → CheeseAnimation::create`; `HEART_CHAR`, `+`, `A`–`D` → `null` (анимации нет, TODO на 17.4+).
- [x] `drawMarkers`: трансформ (scale/rotation, origin по центру) применяется только к выбранному типу, если у него есть анимация; остальные — статично, как раньше. Извлечён `markerSize(ch)`.
- [x] Подтверждение выхода: ESC и закрытие окна (крестик) → диалог **«Выходите!» Да/Нет** (`EditorWindowClose` + `Lwjgl3WindowAdapter.closeRequested`; во время playtest перехват снят).
- [x] Игра не менялась: сыр анимируется, сердца/мышь/коты статичны.
- [x] Версия **1.0.64**; тесты — 231 (новых core-тестов нет: логика под GL, проверка вручную в редакторе).
- [x] Ручная проверка (редактор: выбор «сыр» → сыры анимируются; выбор сердечка/кота/мыши → статично) — подтверждено пользователем.

### 17.4 — Сердечко: постоянный пульс (готово)
Решение: сердечко **пульсирует постоянно** (не редкий акцент), масштабом `1.0 → 1.15 → 1.0`, период 0.9 с, фаза по клетке. Только сердечки **на карте**; в редакторе — при выбранном инструменте-сердечке. HUD-сердца тот же спрайт, без компенсации. Спрайт приведён к виду сыра.
- [x] `IdleAnimator` (общий контракт), `OccasionalAnimator : IdleAnimator`.
- [x] `ContinuousAnimator(period, phase, effect)` — постоянный цикл, чистая логика.
- [x] `ActCurves.pulse(t)`; `HeartAnimation` (`transformAt`, `create`, `cellSeed`).
- [x] `Constants`: `HEART_PULSE_PERIOD = 0.9f`, `HEART_PULSE_SCALE = 1.15f`.
- [x] `Heart.draw(..., scale)`; `GameScreen.heartAnimators` (тик только в живой игре); `EditorScreen` — `HEART_CHAR → HeartAnimation::create`.
- [x] Сыр: `CHEESE_SWELL_SCALE = 1.50f`, `CHEESE_DEFLATE_SCALE = 0.50f`.
- [x] Ассеты: `heart_full`/`heart_grey` → контент `42×38` на холсте `64×64` (как сыр), `heart_empty` удалён; `tools/pad_marker.py`; view + манифесты обновлены.
- [x] Тесты: `ContinuousAnimatorTest` (+8), `HeartAnimationTest` (+8). `.\gradlew core:test` — **247 PASSED**; `.\gradlew build` — `BUILD SUCCESSFUL`.
- [x] Ручная проверка (Desktop `1/1`: сердечко пульсирует; редактор: инструмент-сердечко пульсирует; сыр: разбухание/сдувание 1.50/0.50) — подтверждено пользователем.

### 17.2+ — Дальше (не начато, отдельные этапы)
- Мышь: шаг/покой (кадровый лист).
- Коты: шаг для каждого (4 листа).
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
Статус: **Фаза 1 выполнена** (2026-09-21, версия 1.1.0). На будущее — Фаза 2 (см. ниже).
- ✅ **Ассеты**: музыка — CC0 Juhani Junkala («5 Chiptunes»: Ending → заставка/меню, Level 1 → геймплей); джинглы — Kenney «Music Jingles» (cheese/heart/win/lose); SFX — Kenney «Interface Sounds»/«Digital Audio». Формат **OGG Vorbis**; источники и лицензии — `assets/audio/LICENSES.md`; мастера — `assets/audio/raw/` (в git, исключены из jar/APK через `**/raw/**`).
- ✅ **`AudioManager` (core, `com.catsandmouses.audio`)**: `Music` + пул `Sound`; `playMusic(track, loop)` (no-op, если играет тот же трек), `stopMusic`, `pauseMusic`/`resumeMusic`, `setDuck`, `setMusicVolume`, `setSfxVolume`, `playSfx`, `dispose`; **no-op при `Gdx.audio == null`** (headless-тесты); создаётся и `dispose()` в `MainGame`; `MainGame.pause()/resume()` глушат музыку при сворачивании приложения.
- ✅ **SFX — по дельтам кадра в `GameScreen`** (сыр, сердечко, потеря жизни/телепорт, game over, win, тик отсчёта; клик кнопок/шестерёнки/шага ползунка) — `GameWorld` остаётся чистой.
- ✅ **Музыка**: заставка, меню и Настройки — общий луп (стартует на заставке, не перезапускается при переходе); геймплей — свой луп; на паузе — ducking (`MUSIC_DUCK`); при Победе/поражении — стоп + стингер.
- ✅ **Настройки**: экран `SettingsScreen` (`MENU.md` §3.7) — ползунки «Музыка»/«Звуки» (шаг 10, дефолт 80, 0 = тишина) + «Назад»; компонент `UiSlider`; вход — шестерёнка в правом верхнем углу меню. `ProgressStore.musicVolume()/sfxVolume()` + сеттеры (`KEY_MUSIC_VOLUME`/`KEY_SFX_VOLUME`).
- ✅ **UI-ассеты**: `ui/settings_gear.png`, `ui/slider_track.png`, `ui/slider_knob.png` (SpriteCook `gpt-image-2.5-flare`, 1K/medium, style-ref `ui/button.png`, ~48 кредитов).
- ✅ **Тесты**: `AudioManagerTest` (no-op), `ProgressStoreTest` (дефолт/персист/клэмп), `MenuDefinitionsTest` (подписи настроек); `MainGameEditorTest` зелёный.
- **Фаза 2 (на будущее)**: трек на каждую из 9 тем; раздельные громкости меню/игры; ползунки вибрации/чувствительности свайпов (`GAME.md` §9).

## Этап 120 — Локализация RU/EN (готово, версия 1.2.0)
Цель: игра на русском и английском; язык — авто из системы + переключатель в «Настройках».

Зафиксированные решения:
- Механизм — **in-code словарь** (`core/.../i18n/`), без внешних библиотек и `.properties`.
- Дефолт при локаль не RU/EN — **RU** (аудитория RuStore).
- Device-label EN — **«Cats and Mouses»**.
- Имена котов — по ролям: Chaser/Patrol/Random/Ambush → **Hunter/Prowler/Ziggy/Shadow**.
- Версия — **1.2.0**; `versionCode` — схема «мажор 1 + минор 2 + патч 3» (`major×100000 + minor×1000 + patch` → **102000**).

Задачи:
- [x] `i18n/Lang.kt` (`Lang`, `fromCode`, `system()`), `i18n/Str.kt` (ключи), `i18n/I18n.kt` (словари RU/EN, `t(key, …)`, `toggle`, fallback RU→ключ).
- [x] `MenuDefinitions`: `const val` → функции через `I18n`; экраны берут текст из словаря.
- [x] `Constants`: `KEY_LANGUAGE`, `VERSION = "1.2.0"`; android `versionCode 102000` / `versionName 1.2.0`.
- [x] `ProgressStore`: `savedLanguage()` / `setLanguage()`.
- [x] `MainGame`: применить язык (сохранённый или системный) при старте.
- [x] Экраны: `LoadingScreen`, `MainMenuScreen`, `SettingsScreen` (+строка «Язык»), `GameScreen` (HUD), `GameWorld` (имена котов).
- [x] Android: `res/values-en/strings.xml` (`app_name` = Cats and Mouses).
- [x] Тесты: `I18nTest`, обновлён `MenuDefinitionsTest`, `ProgressStoreTest`; `core:test` PASSED, `build` — `BUILD SUCCESSFUL`.
- [x] Документы: `MENU.md` §3.7, `GAME.md`, `CHANGELOG.md`, `AGENTS.md`, навык `rustore-publish`, `store/common/listing.en.json`.

## Финал — инвертированная логика (готово)
Цель: финальный уровень проходится, только если мышь **не двигалась**; поимка неподвижной мыши = победа.
- [x] `GameWorld`: `finalLevel`/`mouseEverMoved`; фиксация движения в `update` (`mouse.direction != NONE`); в `checkCatCollision` при `finalLevel && !mouseEverMoved` → `State.WIN` (жизнь не тратится).
- [x] Победа по сыру на финале отключена: `if (!finalLevel && pellets.all { it.eaten })`.
- [x] Тесты: `GameWorldSimulationTest` (+2) — поимка без движения = победа; после движения поимка = проигрыш. `core:test` PASSED.
- [x] **Коты из карты (1–4)**: `startLevel` берёт старты из букв `A`–`D` (любое число), цвета/имена/стратегии — по индексу; иначе 4 случайных у углов. Финал — 1 кот, поставленный в редакторе.
- [x] **Скорости** (`Constants`): `MOUSE_SPEED = 150`, `CAT_SPEED = 120`, `CAT_SPEED_FINAL = 200` (кот на финале быстрее мыши).
- [x] Тесты: `GameWorldSimulationTest` (+1: финал с одним котом — старты из карты, скорость кота > скорости мыши). `core:test` PASSED.
- [x] Документы: `GAME.md` (п. 102), `CHANGELOG.md`, `PLAN.md`.

## Финальные титры (`CreditsScreen`)
Цель: после прохождения финала — экран титров с прокруткой снизу вверх, затем «Начать сначала» (с мира 1, карты 1).
- [x] `CreditsScroll` — чистая логика прокрутки (offset/isFinished/skip), тесты `CreditsScrollTest` (+5).
- [x] **Праздник после финала** (`graphics/Celebration.kt`, MENU.md §3.3.1): победа на финале не переключает экран сразу — ~5 с на замороженной карте идёт салют (фейерверки) и шарики снизу + надпись «УРА! ФИНАЛ ПРОЙДЕН!» (`Str.CELEBRATION`), ввод игнорируется; по завершении — титры. Чистая логика (тесты `CelebrationTest` +6), отрисовка — `GameScreen` (ShapeRenderer).
- [x] `CreditsScreen` (core): фон системной темы, текст из `CreditsText` (`assets/text/credits_<lang>.txt`) построчно снизу вверх; после прокрутки — **пасхалка**: карусель фото из `assets/me/` (во весь экран, слегка пикселизованы: 480 px + Nearest; автопрокрутка 6 с/тап/←→) и кнопка `Str.BTN_START_OVER` («Начать сначала») внизу; ввод — Space/Enter/tap/клик. **Пасхалка только для терпеливых:** во время прокрутки любой тап/клавиша (Space/Enter/ESC/BACK) уводит в главное меню (`onSkipToMenu` → `ScreenRouter.toMenu()`), не показывая карусель.
- [x] Фото — `assets/me/*.jpg`; список `assets/me/photos.json` (Gradle-задача `generateMePhotosManifest`, как `maps.json`), загрузчик `MePhotos`; оригиналы — `assets/me/raw/` (в сборку не попадают), игровые готовит dev-скрипт `tools/make_me_photos.py` (1280 px); тест `MePhotosTest`. **В пасхалке — одно фото** (`SELECT = ["08.jpg"]`), **усиленная пикселизация** (`PIXEL_WIDTH = 80`) — снижение рисков по правилам магазина (согласие на изображение/перс. данные).
- [x] Текст титров — правимые файлы `assets/text/credits_ru.txt` / `credits_en.txt` (загрузчик `CreditsText`), без пересборки кода; тесты `CreditsTextTest` (+2).
- [x] i18n: `Str.BTN_START_OVER`; `MenuDefinitions.btnStartOver()`.
- [x] Переход: `GameScreen` (WIN на финале, `saveProgress`) → `progress.clear()` → `ScreenRouter.showCredits()` → `MainGame.showCredits()` → `CreditsScreen { router.startNewGame() }`. В playtest редактора — прежний оверлей.
- [x] `MENU.md` §3.3/§3.8/§4, `GAME.md` (п. 103), `CHANGELOG.md`. `build` — `BUILD SUCCESSFUL`.

## История
- 2026-10-11 — **направление взгляда мыши на старте**: символы карты `+`/`^`/`v`/`<`/`>`; `MazeLoader.findMouseStartFacing()`, `Mouse.setFacing()` (только поворот), `GameWorld.startLevel(..., mouseFacing)`; редактор — символы в палитре и WYSIWYG-поворот маркера; `tests/check_map.py` учитывает символы; тест `MazeLoaderTest`.
- 2026-10-11 — **финальные титры**: после прохождения финала — экран `CreditsScreen` (текст `Str.CREDITS` прокручивается снизу вверх на чёрном фоне), затем «Начать сначала» → сброс прогресса и старт с мира 1, карты 1. Чистая прокрутка `CreditsScroll` +5 тестов; переход через `ScreenRouter.showCredits()`.
- 2026-10-11 — **финал с инвертированной логикой**: победа только если мышь ни разу не двигалась, и тогда поимка котом = победа; после любого движения поимка обычная; победа по сыру на финале отключена. `GameWorld` (`finalLevel`/`mouseEverMoved`), тесты `GameWorldSimulationTest` (+2).
- 2026-10-11 — **миры 4–8 перегенерированы** в новом стиле: лабиринт заполняет всю карту, без «блоков» (наполнение: 4≈32%, 5≈35%, 6≈38%, 7≈40%, 8≈43% стен; мир 1 ручной ≈41%). `tools/gen_levels.py`: связностные стены + опциональный `seed` мира (8 → `8200`). Миры 1–3 и `final` не тронуты.
- 2026-10-11 — **мир 8 «Гигантская кухня»**: заведён 8-й мир на теме `kitchen` (`assets/maps/8/1..5.txt` + `theme.json`, 9 сыров), всего **41 уровень**. Генератор `tools/gen_levels.py` исправлен — стена добавляется только при сохранении связности (`passable_connected`), поэтому лабиринт заполняет всю карту без вырезанных пустых областей; манифест пересобран (мир 8 перед `final`).
- 2026-10-11 — **этап 120 (локализация RU/EN)**: слой `I18n` (`core/.../i18n/` — `Lang`, `Str`, словари RU/EN, `t(key, …)`, fallback RU); все строки UI через словарь (`MenuDefinitions` из констант в функции, заставка, HUD, имена котов Hunter/Prowler/Ziggy/Shadow); язык — авто из локали (не RU/EN → RU) + переключатель «Язык: Рус/Eng» в «Настройках», хранится в `ProgressStore` (`KEY_LANGUAGE`); Android `values-en/strings.xml`. Версия **1.2.0** (versionCode **102000**). Тесты `I18nTest` + обновления; `core:test` PASSED, `build` — `BUILD SUCCESSFUL`.
- 2026-09-21 — **мир 7 «Вулкан»**: добавлены `assets/maps/7/1..5.txt` + `theme.json` (тема `volcano`), мир сгенерирован `tools/gen_levels.py` (density `0.32`, сыр `[8,8,9,9,9]`); генератор получил фильтр по номеру мира (`python tools/gen_levels.py 7`) и при фильтре не трогает `final`; `maps.json` пересобран — **36 уровней** (мир 7 перед `final`); dev-превью `maps/7/*.png`. `core:test` PASSED, `build` — `BUILD SUCCESSFUL`. Версия **1.1.1**.
- 2026-09-21 — **ручные проверки закрыты**: подтверждены этапы 15.3 (сердечки: подбор +1, телепорт/неуязвимость, отсутствие экрана поимки при запасе), 16 (отсчёт «3 → 2 → 1» на старте/рестарте/«Следующая карта», `--no-countdown`), 17.1 (сыр: редкие акценты, заморозка на отсчёте/паузе/оверлеях), 17.3 (редактор: выбранный тип анимируется, остальные статично), 17.4 (пульс сердечка — на `1/1`; в `1/5` сердца нет); обновлены `PLAN.md`/`GAME.md`.
- 2026-09-21 — **этап 17.4 (сердечко: постоянный пульс)**: сердечко на карте непрерывно пульсирует масштабом `1.0→1.15→1.0` (0.9 с), фаза по клетке; механизмы `IdleAnimator` + `ContinuousAnimator` (постоянный цикл) и `HeartAnimation`, кривая `ActCurves.pulse`; работает и в редакторе при выбранном инструменте-сердечке; HUD-сердца тем же спрайтом. Сыр: разбухание `1.50`, сдувание `0.50`. Ассеты: сердечко приведено к виду сыра (контент `42×38` на холсте `64×64`), `heart_empty` удалён, `heart_grey` пересобран, dev-скрипт `tools/pad_marker.py`. Тесты 247, `BUILD SUCCESSFUL`; версия **1.0.65**.
- 2026-09-21 — **этап 17.3 (анимация в редакторе)**: в Desktop-редакторе анимируются объекты на карте того типа, чей инструмент выбран (HUD/палитра — нет); реальная анимация пока только у сыра, для сердечка/мыши/котов — заглушка (`animationFactoryFor → null`); ленивые `markerAnimators` по клетке, `animationClock` в `render(delta)`; версия **1.0.64**.
- 2026-09-21 — **этап 17.1-бис (сыр: процедурные акценты)**: сыр обычно статичен, изредка проигрывает один случайный акт — покачивание ±15°, разбухание (+45%), сдувание (−33.75%) — через случайный интервал 2–3 с; механизм `OccasionalAnimator` (чистая логика, seed по клетке + соль на запуск, без повторов подряд) + `CheeseAnimation`; время тикает только в живой игре; кадровый вариант (лист `cheese_sheet`) откачен, RAW оставлен архивом; тесты 231, `BUILD SUCCESSFUL`; версия **1.0.63**.
- 2026-09-21 — **этап 17.1 (анимация сыра, кадровый вариант — откачен)**: общий механизм `AnimatedSprite` (покадровый лист, `regionAt(elapsed, phase)`, чистая `frameIndexAt`), время анимации в `GameScreen`, сдвиг фазы по клетке; анимация сыра от SpriteCook (6 кадров, блик + покачивание, 20 кредитов) → `tools/normalize_sheet.py` → `sprites/cheese_sheet.png`; тесты 209; версия 1.0.62. Механизм `AnimatedSprite` сохранён для будущих мыши/котов.
- 2026-09-21 — **этап 17.1 (анимация сыра)**: общий механизм `AnimatedSprite` (покадровый лист, `regionAt(elapsed, phase)`, чистая `frameIndexAt`), время анимации в `GameScreen` (`animationClock`, только в живой игре), сдвиг фазы по клетке; анимация сыра от SpriteCook (6 кадров, блик + покачивание, 20 кредитов) → `tools/normalize_sheet.py` → `sprites/cheese_sheet.png`; тесты 209, `BUILD SUCCESSFUL`; версия **1.0.62**; ручная проверка — за пользователем.
- 2026-09-21 — **этап 16 (стартовый отсчёт)**: 3 секунды «3 → 2 → 1» перед началом карты; мышь/коты заморожены, ввод буферизуется, пауза/меню недоступны; визуал с наплывом и затуханием; отсчёт в `GameWorld` (+AI-состояние `COUNTDOWN`); отключение в `--ai-mode`/playtest/`--no-countdown`; тесты 202, `BUILD SUCCESSFUL`; версия **1.0.56**; ручная проверка — за пользователем.
- 2026-09-21 — **этап 15.3 (сердечки: эффект)**: `START_LIVES=1`, `MAX_LIVES 3→7`, подбор +1; поимка с запасом — жизнь −1, телепорт в самую далёкую от котов клетку (BFS), неуязвимость 1.5 с + мигание; жизни копятся и хранятся в `ProgressStore` (`KEY_LIVES`), «Продолжить» восстанавливает; HUD 7×36, неактивные — `heart_grey` (`#5A5A5A`, локально); тесты 198, `BUILD SUCCESSFUL`; версия **1.0.54**; ручная проверка — за пользователем.
- 2026-09-21 — яркость тем: `tools/adjust_theme.py` (RAW → фон RGB × `backgroundShade`, стены `c + (255−c)×wallLighten`, дефолт 15%, затем LANCZOS-масштаб → игровые файлы); значения хранятся в `theme.json` (`backgroundShade`/`wallLighten`) для воспроизводимой перегенерации из `raw/`. Тема `castle` перегенерирована на 15%, `view/` обновлён; навык `generate-theme` дополнен.
- 2026-09-24 — RuStore: статус версии 1.0.49 изменился — модерацию прошла (`READY_FOR_PUBLICATION`, versionCode 49, versionId `2064833696`); решено не публиковать, первой версией выпустить **1.1.1** после доработки карт и ручных тестов на телефоне.
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
