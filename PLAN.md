# PLAN.md — текущий план разработки

Живой план этапа: цели, решения, задачи и статусы. Обновляется по ходу работ.
Связанные документы: [GAME.md](GAME.md) (общее описание и учёт),
[MENU.md](MENU.md) (экраны/меню/переходы — источник истины навигации),
[AGENTS.md](AGENTS.md) (правила работы агента).

## Текущий этап
**Этап 11 — Релиз в RuStore. Версия отправлена на модерацию (ручная публикация).**
Параллельно выполнен **Этап 13 — Kotlin-тесты игровой логики** (171 тест, `core:test`; игровой цикл вынесен в `GameWorld`).
Также выполнен **Этап 14 — Desktop-редактор карт** (`--editor`, версия 1.0.51; вне RuStore/APK).
Предыдущее: Этап 12 — Применение тем + миры + карта 32×16 + превью карт (готово);
Этап 7 — Темы (7 тем: `grass`, `castle`, `ice`, `desert`, `moss`, `swamp`, `space`).
Обновлено: 2026-09-20.

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
- Игровой эффект съедания — отложен (этап 15.3).
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

### 15.3 — Эффект при съедании (отложено)
Варианты: +1 жизнь (до `MAX_LIVES = 7`); +1 жизнь, при переполнении — очки; временная неуязвимость; полное восстановление жизней; жизнь + очки; «щит» от одной поимки.
Открытые вопросы: хранить жизни между картами или сбрасывать; стартовое число жизней; формат HUD (иконка + число); поведение экрана «Мышка попалась!».

## История
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
