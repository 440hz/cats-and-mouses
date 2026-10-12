# Changelog

Все заметные изменения проекта **CATS AND MOUSES**. Формат — [Keep a Changelog](https://keepachangelog.com/ru/1.1.0/), версионирование — [SemVer](https://semver.org/lang/ru/).

## [Unreleased]

## [1.2.4] — 2026-10-12

### Исправлено
- **Язык интерфейса на Android.** Игра стартовала на английском даже при русской системе: в Android-ресурсах не было `values-ru` (только `values` + `values-en`), поэтому система отдавала приложению конфиг с приоритетом `en` (`Locale.getDefault()=en_US`). Добавлены `android/res/values-ru/strings.xml`, `android/res/xml/locales_config.xml` + `android:localeConfig` в манифесте; `AndroidLauncher` передаёт системную локаль (`resources.configuration.locales[0]`) в `MainGame` (`platformLangCode`). Проверено на POCO X6 Pro (Android 16, ru-RU): `systemLocale=ru_RU`, старт на русском.
- **Кнопка «Выход» в главном меню** теперь показывается и на Android (раньше — только Desktop). `MainMenuScreen` больше не скрывает действие `QUIT_APP` вне Desktop.

### Добавлено
- **Dev-флаги снимка экрана** (только Desktop, в APK не влияют): `--window WxH`, `--shot <path>`, `--shot-screen loading|menu|settings|credits|game`, `--shot-delay <сек>`; enum `ShotScreen` (core) и `graphics/ScreenCapture` (сохранение кадра в PNG с вертикальным флипом). Использованы для съёмки скриншотов витрины 1920×1080.

### Изменено
- Версия **1.2.4** (`Constants.VERSION`, `android` versionCode **102004** / versionName 1.2.4).

### Релиз
- Версия **1.2.4** загружена в RuStore Консоль (вручную) и **отправлена на модерацию** (versionId `2064877739`, `MODERATION`, ручная публикация); версия **1.0.49 архивирована**.

### Документация
- `MENU.md` §3.1 — «Выход» доступен на всех платформах.
- `store/rustore/FAQ.md` — FAQ перенесён из корня `FAQ.md` в `store/rustore/`, содержимое синхронизировано с блоком `faq` в `version.json` (41 уровень / 8 миров).
- `store/rustore/version.json` — `whatsNew` и версия обновлены под 1.2.4; скриншоты витрины пересняты (9 кадров 1920×1080).

## [1.2.3] — 2026-10-12

### Безопасность
- **Очистка git-истории перед публичным GitHub-зеркалом** (`git filter-repo`): из всех коммитов удалены засвеченный RuStore `keyId` и префикс приватного ключа, оригиналы фото-пасхалки (`assets/me/raw/*.jpg`), машинно-специфичные пути и внутренний LAN-адрес; автор коммитов анонимизирован (рабочий аккаунт → `440hz <andrey@440hz.ru>`). Внутренние сессионные планы (`GIT-CLEANUP-PLAN.md`, `SESSION-PLAN.md`) убраны из репозитория.
- Добавлен скан `tools/check-secrets.ps1` (трекаемые файлы: ключи/токены/пути/LAN/e-mail) и хук `.githooks/pre-commit`, блокирующий коммит с утечками.

### Добавлено
- `LICENSE` (MIT, © `440hz`).

### Изменено
- `gradle.properties`: убран машинно-специфичный `org.gradle.java.home` — JDK 21 задаётся через `JAVA_HOME`; `GAME.md` — абсолютные пути заменены плейсхолдерами.

### Документация
- `AGENTS.md` — правило 13: секреты/ключи/личные данные не в git, проверка `check-secrets` перед коммитом.

## [1.2.2] — 2026-10-12

### Исправлено
- **Оригиналы фото-пасхалки убраны из git**: `assets/me/raw/*.jpg` больше не отслеживаются (`git rm --cached`), папка добавлена в `.gitignore` — в репозитории и сборке (APK/jar) не хранятся, остаются локально.

## [1.2.1] — 2026-10-12

### Добавлено
- **Праздник после финала** (MENU.md §3.3.1): победа на финале больше не переключает экран сразу — на **замороженной карте** ~5 с идёт праздник: **салют (фейерверки)** и **шарики, поднимающиеся снизу**, по центру — «УРА! ФИНАЛ ПРОЙДЕН!» (`Str.CELEBRATION`, RU/EN). Ввод во время праздника игнорируется; по завершении запускаются финальные титры. Чистая логика `graphics/Celebration.kt` (детерминирована по seed, тесты `CelebrationTest` +6), отрисовка — `GameScreen` (`ShapeRenderer`). В playtest редактора праздник не запускается (прежний оверлей).

### Изменено
- **Громкость по умолчанию:** музыка **20%** (было 80%), звуки **80%** — две константы `Constants.DEFAULT_MUSIC_VOLUME` / `DEFAULT_SFX_VOLUME` вместо общей `DEFAULT_VOLUME`; `ProgressStore.musicVolume()/sfxVolume()` и `ProgressStoreTest` обновлены (`MUSIC.md` §4, `MENU.md` §3.7).
- **Пасхалка-карусель (титры):** оставлено **одно фото** (`SELECT = ["08.jpg"]` в `tools/make_me_photos.py`) с **усиленной пикселизацией** (`PIXEL_WIDTH = 80`, было 160) — снижение рисков по правилам модерации RuStore (согласие на изображение/перс. данные). Автопрокрутка — 6 с.
- **EN-титры:** `assets/text/credits_en.txt` приведён в точное соответствие русскому (тот же порядок и строки: приветствие → шутка про котов → авторы → благодарность → посвящение).

## [1.2.0] — 2026-10-11

### Добавлено
- **Направление взгляда мыши на старте**: в карте старт мыши — `^` (вверх, по умолчанию), `v`/`<`/`>` (вниз/влево/вправо); `+` — старый эквивалент `^`. `MazeLoader.findMouseStartFacing()`, `Mouse.setFacing()` (поворот **только спрайта**, движение задаёт игрок), `GameWorld.startLevel(..., mouseFacing)`; редактор карт — один инструмент-мышь `^`, клик по стоящей мыши меняет направление по кругу (WYSIWYG-поворот маркера), `tests/check_map.py` обновлён.
- **Финальные титры** (`CreditsScreen`, MENU.md §3.8): после прохождения финала текст титров прокручивается снизу вверх на фоне системной темы; после прокрутки — **пасхалка**: карусель фото из `assets/me/` (список `assets/me/photos.json`, Gradle `generateMePhotosManifest`), во весь экран и слегка пикселизованы (480 px + Nearest), автопрокрутка (6 с)/тап/←→; внизу — кнопка **«Начать сначала»** (`Str.BTN_START_OVER`) → сброс прогресса и старт с мира 1, карты 1. Текст титров — в правимых файлах `assets/text/credits_ru.txt` / `credits_en.txt` (загрузчик `CreditsText`). Прокрутка — чистая логика `CreditsScroll` (тесты +5). Оригиналы фото — `assets/me/raw/` (в сборку не попадают), игровые — dev-скрипт `tools/make_me_photos.py` (уменьшение до 1280 px). **Пасхалка только для терпеливых:** пока титры прокручиваются, любой тап/клавиша (Space/Enter/ESC/BACK) уводит в главное меню — карусель фото видят лишь те, кто титры не трогает.
- **Финал с инвертированной логикой**: финальный уровень проходится, только если мышь **ни разу не двигалась** — тогда поимка котом даёт **победу** (жизнь не тратится); после любого движения поимка обрабатывается как обычно. Победа по сыру на финале отключена. `GameWorld` (`finalLevel`/`mouseEverMoved`), тесты `GameWorldSimulationTest` (+2).
- **Коты из карты (1–4) и скорости**: `GameWorld.startLevel` берёт старты котов из букв `A`–`D` карты (любое число, напр. 1 кот на финале) — расстановка из редактора; скорости вынесены в `Constants` (`MOUSE_SPEED = 150`, `CAT_SPEED = 120`, `CAT_SPEED_FINAL = 200` — кот на финале быстрее мыши).
- **Мир 8 «Гигантская кухня»** (`kitchen`): новый мир прохождения из 5 карт (`assets/maps/8/1..5.txt` + `theme.json`, по 9 сыров) — всего **41 уровень** (8 миров × 5 + финал), мир 8 перед `final`.
- **Локализация RU/EN** (этап 120): слой `I18n` (`core/.../i18n/` — `Lang` RU/EN, ключи `Str`, словари, `t(key, …)` с fallback на RU). Все строки UI переведены: меню/оверлеи (`MenuDefinitions`), заставка, HUD, имена котов по ролям (RU Барсик/Матроскин/Леопольд/Черныш → EN Hunter/Prowler/Ziggy/Shadow). Язык — авто из системной локали (не RU/EN → RU) + переключатель **«Язык: Рус/Eng»** в «Настройках», хранится в `ProgressStore` (`KEY_LANGUAGE`). Android: `values-en/strings.xml` (`app_name` = **Cats and Mouses**). Тесты `I18nTest` (+ обновлён `MenuDefinitionsTest`, `ProgressStoreTest`).
- **Фильтр уровней для мобильной сборки**: в манифесте `assets/maps/maps.json` у каждого уровня появился флаг `"use"` (`true`/`false`). Генератор `generateMapsManifest` (`build.gradle.kts`) сохраняет флаг из предыдущего манифеста при перегенерации, у новых уровней дефолт `true`. В APK (Android) попадают **только** уровни с `"use": true`: merge-задача вычищает неиспользуемые карты `.txt`, их превью, `theme.json` пустых миров и `final.json`, а `maps.json` внутри APK перезаписывается отфильтрованным списком. Desktop/dev-сборки не фильтруются. Проверено: `android:assembleDebug` — `BUILD SUCCESSFUL` (при `use:false` на `maps/7/5.txt` в APK 35 уровней).

### Изменено
- Версия **1.2.0** (`Constants.VERSION`, `android` versionCode **102000** / versionName 1.2.0). Схема `versionCode` — «мажор 1 + минор 2 + патч 3» цифры: `major×100000 + minor×1000 + patch` (напр. 1.2.0 → 102000).
- `MenuDefinitions`: строки из `const val` → функции через `I18n` (RU/EN); экраны (`LoadingScreen`, `MainMenuScreen`, `SettingsScreen`, `GameScreen`, `GameWorld`) берут текст из словаря.
- Переключатель языка в «Настройках» — наглядная дорожка-фон как у ползунков с надписью `Язык: < Рус >` (стрелки подсказывают, что значение меняется); при фокусе — подсветка акцентом, без сдвига.
- **Миры 4–8 перегенерированы** новым генератором: лабиринт заполняет **всю карту** (стены ~32–43%, звено от мира 1 ≈41%), без вырезанных «блоков» стен. `tools/gen_levels.py`: стена добавляется только при сохранении связности (`passable_connected`); у мира можно задать `seed` (мир 8 — `8200`).
- Из HUD игры убрана **легенда котов** (имена и число поимок внизу экрана).

### Исправлено
- **Перенос жизней в прогресс**: при победе на карте `GameScreen.updateWin` сохранял прогресс без `lives` → в сейв уходил `START_LIVES = 1`, и при возврате на следующий уровень (в т.ч. **финал**) через «Продолжить» накопленные сердечки сбрасывались. Теперь передаётся `world.lives`.
- **Навык `check-balances` / OpenCode Go-Zen**: npm-пакет `opencode-balance-mcp` перестал работать — opencode.ai сменил маршруты (`/workspace/<id>/go` → `/console/<id>/go`) и отдаёт данные через консольное API (`GET /console/api/go/status`, `GET /console/api/billing/status`). Добавлен локальный MCP **`tools/opencode-balance-mcp/index.mjs`** (Node, без зависимостей, те же имена инструментов `query_go_usage` / `query_zen_balance`); `opencode.json` переведён на него, ключи — `OPENCODE_CONSOLE_SESSION` (cookie `__Host-console_session`) + `OPENCODE_ORG_ID` вместо `OPENCODE_GO_AUTH_COOKIE`.

### Документация
- `MENU.md` §3.7 — строка «Язык / Language» и описание локализации RU/EN.
- `AGENTS.md` («Структура», «Конвенции игровой логики») — слой `i18n`, схема `versionCode`.
- `store/common/listing.en.json` — EN-тексты листинга (name/short/full/whatsNew/FAQ) для будущих магазинов (этап 117).
- `AGENTS.md` (раздел «Проверка балансов») и `.opencode/skills/check-balances/SKILL.md` обновлены под новый источник данных и процедуру обновления сессии.
- Трекинг релиза RuStore в `GAME.md` и `PLAN.md` актуализирован: версия 1.0.49 прошла модерацию (`READY_FOR_PUBLICATION`), решено её не публиковать — первой версией выпустить **1.1.1** после доработки карт и ручных тестов на телефоне.

## [1.1.1] — 2026-09-21

### Добавлено
- **Мир 7 «Вулкан»** — новый мир прохождения из 5 карт (`assets/maps/7/1..5.txt` + `theme.json` с темой `volcano`); всего уровней стало **36** (мир 7 идёт перед `final`). Тема «Вулкан» теперь используется миром, а не только отладочным `--theme volcano`.

### Изменено
- `tools/gen_levels.py` — фильтр запуска по номерам миров (`python tools/gen_levels.py 7`); при заданном фильтре `final` не перегенерируется (защита карт, правленных вручную в редакторе). Манифест `assets/maps/maps.json` пересобран — **36 уровней**.
- Версия **1.1.1** (`Constants.VERSION`, `android` versionCode 101 / versionName 1.1.1).

### Документация
- `GAME.md` (п. 98 — мир 7, структура карт `1`…`7`) и `PLAN.md` (матрица уровней 36, раздел «Мир 7 — Вулкан»).
- `AGENTS.md` и навык `run-game`: `core:test` отнесён к «тяжёлым» командам — запускать **неблокирующе** (`pwsh tools/build-bg.ps1 core:test`), а не блокирующим `.\gradlew core:test`.

## [1.1.0] — 2026-09-21

### Добавлено
- **Этап 119, Фаза 1 — звук, музыка и экран «Настройки».** `AudioManager` (core, `com.catsandmouses.audio`): музыка заставки/меню/геймплея + пул SFX, **no-op-safe** при `Gdx.audio == null`; меню и Настройки делят один луп (не перезапускается), на паузе — ducking, при Победе/поражении — стоп + стингер; `MainGame.pause()/resume()` глушат музыку при сворачивании приложения.
- Экран **«Настройки»** (`SettingsScreen`): ползунки «Музыка»/«Звуки» (шаг 10, дефолт 80%, 0 = тишина) и «Назад»; компонент `UiSlider` (дорожка, тинт-заливка, 11 насечек, ручка). Вход — **иконка-шестерёнка** в правом верхнем углу главного меню.
- SFX в `GameScreen` по дельтам кадра: сыр, сердечко, потеря жизни/телепорт, game over, win, тик отсчёта; клики кнопок/шестерёнки/шага ползунка.
- UI-ассеты `ui/settings_gear.png`, `ui/slider_track.png`, `ui/slider_knob.png` (SpriteCook `gpt-image-2.5-flare`, 1K/medium, style-ref `ui/button.png`).
- Аудио-ассеты (CC0): `assets/audio/{music,sfx}/` (OGG Vorbis); мастера — `assets/audio/raw/`, лицензии — `assets/audio/LICENSES.md`.

### Изменено
- **Ручные проверки этапов 15.3 (сердечки), 16 (стартовый отсчёт) и 17.1/17.3/17.4 (анимации) закрыты** — подтверждено пользователем; обновлены `PLAN.md` и `GAME.md` (пометки в 17.4 исправлены с `1/5` на `1/1`).
- Звуки подобраны (Kenney «Music Jingles», пиццикато): `cheese` → `PIZZI04`, `heart` → `PIZZI02`, `win` → `PIZZI10`, `game_over` → `PIZZI11`; SFX-джинглы обновлены в `assets/audio/{sfx,raw/sfx}/` и `LICENSES.md`.
- `ProgressStore`: `musicVolume()/sfxVolume()` + сеттеры (клэмп 0…100, дефолт 80); ключи `KEY_MUSIC_VOLUME`/`KEY_SFX_VOLUME`.
- `MenuDefinitions`: `TITLE_SETTINGS`, `BTN_BACK`, `LABEL_MUSIC`/`LABEL_SFX`, `volumeLabel(...)`, `settings()`, `Action.BACK`.
- `MainMenuScreen` — шестерёнка + звук клика; `LoadingScreen` — старт музыки меню (продолжается в меню); `MainGame` — поле `audio`, `showSettings()`, `pause()`/`resume()`; `ScreenRouter.openSettings()`.
- `Constants`: `DEFAULT_VOLUME`, `VOLUME_STEP`, `MUSIC_DUCK`, `GEAR_SIZE`/`GEAR_MARGIN`, `SLIDER_*`.
- `tools/pad_marker.py` — поддержка не-квадратного холста и масштаба по ширине (для рейки/ручки ползунка).

### Документация
- `MENU.md`: экран «Настройки» (§3.7), шестерёнка в меню (§3.1), переходы (§1/§4), правила `UiSlider`/иконки/музыки/SFX (§5), `ProgressStore` (§6), история.
- `GAME.md`, `PLAN.md` (этап 119), `assets/audio/LICENSES.md`, `assets/asset-manifest.json`/`.md` (UI-ассеты).
- Добавлен **`MUSIC.md`** — источник истины по звуку (треки, SFX, громкости, источники, где что играет); в `AGENTS.md` закреплено правило обновлять его при каждой правке звука/музыки; в структуру добавлен локальный архив `music/`.
- Версия **1.1.0** (`Constants.VERSION`, `android` versionCode 100 / versionName 1.1.0).

## [1.0.66] — 2026-09-21

### Изменено
- **Карты:** правки в Desktop-редакторе — в `maps/1/1.txt` добавлено сердечко `H` и перенесён сыр, в `maps/1/5.txt` сердечко убрано.

### Документация
- Версия **1.0.66** (`Constants.VERSION`, `android` versionCode 66 / versionName 1.0.66).

## [1.0.65] — 2026-09-21

### Добавлено
- **Этап 17.4 — постоянный пульс сердечка на карте.** Сердечко-подбор теперь непрерывно пульсирует масштабом `1.0 → 1.15 → 1.0` (период 0.9 с), со сдвигом фазы по клетке — сердца не бьются в один такт. Работает и в игре, и в редакторе (когда выбран инструмент-сердечко).
- `core/.../graphics/IdleAnimator.kt` — общий контракт аниматора (`update`/`transform`); `OccasionalAnimator` его реализует (поведение не изменилось).
- `core/.../graphics/ContinuousAnimator.kt` — постоянный циклический акцент (без пауз), чистая логика без GL.
- `core/.../graphics/HeartAnimation.kt` — пульс сердечка: `transformAt(t)`, `create(seed)`, `cellSeed(col,row)`.
- `ActCurves.pulse(t)` — непрерывная кривая `0.5 − 0.5·cos(2πt)`.

### Изменено
- `Constants`: `HEART_PULSE_PERIOD = 0.9f`, `HEART_PULSE_SCALE = 1.15f`; `CHEESE_SWELL_SCALE 1.45 → 1.50`, `CHEESE_DEFLATE_SCALE 0.6625 → 0.50`.
- `Heart.draw(...)` принимает необязательный `scale` (дефолт = прежнее поведение).
- `GameScreen`: аниматоры сердечек по клетке, пульс тикает только в живой игре (замирает на отсчёте/паузе/оверлеях); `drawHearts()` применяет масштаб. HUD-сердца используют тот же спрайт без компенсации.
- `EditorScreen`: `markerAnimators`/фабрики обобщены до `IdleAnimator`; `HEART_CHAR → HeartAnimation::create` (заглушка убрана).
- **Ассеты:** сердечко приведено к виду сыра — контент `42×38` на холсте `64×64` (намеренно уменьшено, как сыр); `heart_grey` пересобран из нового `heart_full` (тёмно-серый силуэт). Добавлен dev-скрипт `tools/pad_marker.py` (обрезка по alpha>10 → масштаб по высоте → центр на холсте).

### Удалено
- `sprites/heart_empty.png` и его raw/view — в коде не использовался; записи в `asset-manifest.json`/`.md` убраны.

### Документация
- `PLAN.md` (этап 17.4), `GAME.md`, `AGENTS.md`, `asset-manifest.json`/`.md`.
- Версия **1.0.65** (`Constants.VERSION`, `android` versionCode 65 / versionName 1.0.65).

## [1.0.64] — 2026-09-21

### Добавлено
- **Этап 17.3 — анимация выбранного типа в Desktop-редакторе.** В редакторе анимируются **объекты на карте** того типа, чей инструмент выбран; HUD/палитра не анимируются. Реальная анимация пока только у сыра; для сердечка, мыши и котов — **заглушка** (`animationFactoryFor → null`, анимации появятся отдельными этапами).
- `EditorScreen`: `animationClock` (тикает в `render(delta)`, пауза при открытом диалоге), ленивые `markerAnimators` по клетке (живут между кадрами), `syncAnimators` (чистка лишних/чужих клеток), `animationFactoryFor`, `markerSize`.

### Изменено
- `EditorScreen.drawMarkers`: трансформ (scale/rotation, origin по центру) применяется только к выбранному типу, если у него есть анимация; остальные маркеры — статично, как раньше.
- Выход из редактора с подтверждением: по **ESC** и по **закрытию окна** (крестик) показывается диалог **«Выходите!»** с кнопками **Да/Нет** (новый `EditorWindowClose` + `Lwjgl3WindowAdapter.closeRequested`; во время playtest перехват снят).

### Документация
- `PLAN.md` (этап 17.3), `GAME.md`.
- Версия **1.0.64** (`Constants.VERSION`, `android` versionCode 64 / versionName 1.0.64).

## [1.0.63] — 2026-09-21

### Добавлено
- **Этап 17.1 (переработка) — сыр: процедурные idle-акценты.** Сыр теперь **обычно статичен** и изредка (случайный интервал 2–3 с, у каждого свой) проигрывает **один случайный акцент**: покачивание **±15°** (0.5 с), **разбухание** 1.0→1.45→1.0 (0.7 с), **сдувание** 1.0→0.6625→1.0 (0.7 с), затем возврат в стандарт.
- `core/.../graphics/OccasionalAnimator.kt` — общий механизм «изредка-акцент»: `Act`/`ActTransform`/`ActCurves`, случайное расписание и выбор акта, точный возврат в identity; чистая логика без GL, детерминирована по seed.
- `core/.../graphics/CheeseAnimation.kt` — набор актов сыра, `create(seed)`, `cellSeed(col,row)` (своё расписание каждому сыру).

### Изменено
- `Constants`: `CHEESE_ANIM_INTERVAL_MIN/MAX = 2f/3f`, `CHEESE_WOBBLE_DURATION/ANGLE`, `CHEESE_SWELL_DURATION/SCALE`, `CHEESE_DEFLATE_DURATION/SCALE`; убраны `CHEESE_FRAME_COUNT/FPS/DURATION`.
- Разнообразие акцентов: соль на запуск уровня (seed = `cellSeed xor Random.nextLong()`), поэтому открытие меняется между перезапусками; один и тот же акт не выпадает дважды подряд (`OccasionalAnimator.nextActIndex`).
- `Pellet.draw(...)` — необязательные `scale`/`rotation`/`offsetY` (по умолчанию прежнее поведение).
- `GameScreen`: статичный сыр `cheese.png`, аниматор на каждый сыр, тик акцентов только в живой игре (замирает на отсчёте/паузе/оверлеях). Редактор карт — без изменений.

### Удалено
- Кадровый вариант 17.1: `sprites/cheese_sheet.png` и `view/cheese_sheet.png` убраны из игры; RAW `sprites/raw/cheese_sheet.png` оставлен архивом (запись в `asset-manifest`). Механизм `AnimatedSprite` сохранён для будущих анимаций мыши/котов.

### Документация
- `AGENTS.md` (два механизма анимации), `PLAN.md` (этап 17 / 17.1, резерв 17.2+), `GAME.md`.
- Версия **1.0.63** (`Constants.VERSION`, `android` versionCode 63 / versionName 1.0.63).

## [1.0.62] — 2026-09-21

### Добавлено
- **Этап 17 — Анимации. 17.1 — Сыр.** Покадровая анимация сыра: мерцание (блик) + лёгкое покачивание. Источник — SpriteCook `animate_game_art` (`pixel-engine-v1.1`, asset `bffd2610-dd2a-4ff9-ab98-0dfbd1612096`, 6 кадров, от сыра `f2cc9aab…`, списано 20 кредитов → 234).
- **Общий механизм анимации** `core/.../graphics/AnimatedSprite.kt`: обёртка над LibGDX `Animation<TextureRegion>`, `regionAt(elapsed, phase)`, `dispose()`, чистая `frameIndexAt(...)` (тестируется без GL).
- `tools/normalize_sheet.py` — универсальная нормализация анимационного листа (объединённый bbox кадров, общий масштаб и привязка, высота контента, LANCZOS) → `assets/sprites/cheese_sheet.png` (384×64, 6 кадров 64×64).

### Изменено
- `Constants`: `CHEESE_FRAME_COUNT = 6`, `CHEESE_FPS = 9f`, `CHEESE_FRAME_DURATION`.
- `GameScreen`: сыр грузится из `sprites/cheese_sheet.png`; появился `animationClock`, который идёт **только** в живой игре (не на отсчёте, паузе и оверлеях); фаза по клетке (`(x*7 + y*13) % 6`), чтобы сыры мерцали не синхронно. Редактор карт продолжает использовать статичный `sprites/cheese.png`.

### Документация
- `asset-manifest.json`/`.md`: новая запись анимации сыра (`sprites/cheese_sheet.png`, RAW `sprites/raw/cheese_sheet.png`, frames 6, fps 9); `PLAN.md` (этап 17 + 17.1), `GAME.md`, `AGENTS.md` (конвенция анимации).
- Версия **1.0.62** (`Constants.VERSION`, `android` versionCode 62 / versionName 1.0.62).

## [1.0.61] — 2026-09-21

### Документация
- Зарезервирован **Этап 119 — Звук и музыка**: архитектура `AudioManager` (core, no-op-safe при `Gdx.audio == null`), SFX по дельтам кадра в `GameScreen`, фоновая музыка (меню/геймплей) с Android-`LifecycleListener`, экран настроек (тумблеры «Музыка»/«Звуки», `ProgressStore.KEY_MUSIC_ENABLED`/`KEY_SFX_ENABLED`), тесты и открытые вопросы (источник ассетов/OGG, per-theme музыка, громкость). Разделы в `PLAN.md` («Этапы на будущее (зарезервированы)») и `GAME.md` («Зарезервировано на будущее», Этап 9).
- Версия **1.0.61** (`Constants.VERSION`, `android` versionCode 61 / versionName 1.0.61).

## [1.0.60] — 2026-09-21

### Добавлено
- Тема локации **«Вулкан»** (`volcano`, огненный/лавовый мир) — 8-я тема: стены — 15-piece autotile вулканического базальта с трещинами светящейся лавы (asset `52996612-…`, `gemini-3.1-flash-image`, 12 кредитов), фон — бесшовная остывшая лавовая порода с тусклыми оранжевыми прожилками (asset `8aef4913-…`, `gpt-image-2.5-flare`, 8 кредитов). Зарегистрирована в `themes.json` (активная — `grass`), RAW/view и манифесты обновлены; доступна через `--theme volcano`. Мир/карты под тему ещё не заведены. Списано 20 кредитов (294 → 274).
- Тема локации **«Гигантская кухня»** (`kitchen`) — 9-я тема: стены — 15-piece autotile белого керамического кафеля (asset `8986fa0b-…`, `gemini-3.1-flash-image`, 12 кредитов), фон — бесшовные деревянные доски столешницы (asset `5188a99d-…`, `gpt-image-2.5-flare`, 8 кредитов). Зарегистрирована в `themes.json`, RAW/view/`map.png` и манифесты обновлены; доступна через `--theme kitchen`. Списано 20 кредитов (274 → 254).

### Изменено
- `tools/map_preview.py`: новый режим `--theme-map <мир/карта>` — рендер одной карты (по умолчанию `1/1`) во всех темах из `themes.json` в `assets/themes/<id>/view/map.png` (1280×640). Сгенерированы превью для всех 9 тем; `view/` хранится в git, в сборку/APK не попадает.

### Документация
- Навык `generate-theme` обновлён: добавлен шаг §4.1 (превью лабиринта `view/map.png`), уточнена сетка (32×16), таблица сделанных тем расширена до 9.
- Версия **1.0.60** (`Constants.VERSION`, `android` versionCode 60 / versionName 1.0.60).

## [1.0.59] — 2026-09-21

### Документация
- В `PLAN.md` добавлен раздел «Этапы на будущее (зарезервированы)»: **Этап 117** — дополнительные Android-магазины (Huawei AppGallery, Xiaomi GetApps, NashStore) и **Этап 118** — Desktop-релиз (Steam с нуля + itch.io): фазы Ф0–Ф7, сроки ($100 Steam Direct, +30 дней, Coming Soon ≥2 недели), риски (выплаты из РФ, декларация AI-контента) и открытые вопросы.
- В `GAME.md` («Следующие шаги») добавлена краткая выдержка по этапам 117/118 со ссылкой на `PLAN.md`.
- Версия **1.0.59** (`Constants.VERSION`, `android` versionCode 59 / versionName 1.0.59).

## [1.0.58] — 2026-09-21

### Исправлено
- `tools/build-bg.ps1` теперь действительно неблокирующий: запуск сборки идёт через `WScript.Shell.Run` + `.bat`-лаунчер (`cmd /c "…"`), вызов возвращает управление за ~0,4 с (раньше `Start-Process` держал инструмент до 30 с, а вариант с `WScript` не запускал сборку из-за кавычек в путях с пробелами). Логи — `lwjgl3/build/build-logs/build-out.log`.
- Версия **1.0.58** (`Constants.VERSION`, `android` versionCode 58 / versionName 1.0.58).

## [1.0.57] — 2026-09-21

### Изменено
- Обновлены карты мира 1 (Трава): `assets/maps/1/1.txt` и `assets/maps/1/2.txt` (правки в редакторе карт).
- Пересобраны билды: `.\gradlew build` (202 теста), Desktop-дистрибутив (`lwjgl3:installDist`), debug/release APK и release AAB; dev-превью карт перегенерированы (`tools/map_preview.py`).
- Версия **1.0.57** (`Constants.VERSION`, `android` versionCode 57 / versionName 1.0.57).

## [1.0.56] — 2026-09-21

### Добавлено
- **Стартовый отсчёт «3 → 2 → 1»** перед началом карты (`Constants.COUNTDOWN_TIME = 3f`): пока идёт отсчёт, мышь и коты **заморожены**, сыр не собирается, ввод буферизуется (после отсчёта мышь сразу едет в выбранную сторону). Space/ESC/BACK/тап во время отсчёта не открывают паузу/меню.
- Визуал отсчёта: крупная цифра по центру (`Constants.FONT_SIZE_COUNTDOWN = 192`, BoldPixels) с **наплывом и затуханием** (появление alpha 0→1 и масштаб 1.15→1.00; затухание alpha 1→0 и масштаб 1.00→1.08) и синхронным затемнением фона.
- Dev-флаг **`--no-countdown`** (Desktop) отключает отсчёт; в `--ai-mode` и в playtest редактора отсчёт выключен автоматически.

### Изменено
- `GameWorld`: поля `countdownTimer`/`isCountdown`/`countdownNumber`, параметр `startLevel(..., countdown = COUNTDOWN_TIME)`, ранний выход из `update()` во время отсчёта (тестируемо headless).
- `GameScreen`: отрисовка отсчёта, отдельный шрифт цифр (только `1/2/3`), состояние AI `COUNTDOWN` в `getGameState()`.
- `MainGame`/`Lwjgl3Launcher`/`EditorScreen`: прокинут флаг отсчёта.
- Версия **1.0.56** (`Constants.VERSION`, `android` versionCode 56 / versionName 1.0.56).

### Тесты
- `GameWorldSimulationTest` (+3): отсчёт замораживает мышь/сыр/котов, после отсчёта игра начинается, `countdown = 0f` — сразу; `ConstantsTest` (+1). Итого **202 теста PASSED**.
- `tests/check_map.py` (Python-симуляция) отсчёт не моделирует — отмечено в `tests/README.md`.

### Документация
- Обновлены `MENU.md` (оверлей «Отсчёт»), `GAME.md`, `PLAN.md` (этап 16), `CHANGELOG.md`, `tests/README.md`.

## [1.0.55] — 2026-09-21

### Добавлено
- **Навык `check-balances`** (`.opencode/skills/check-balances/`): проверка балансов OpenCode Go/Zen, SpriteCook и DeepSeek с выводом таблицами (сводная «Провайдер · Сумма · Статус» + детали); вынесен из `AGENTS.md`. Ключ DeepSeek добавлен в `opencode.json` (`mcp.opencode-balance.environment.DEEPSEEK_API_KEY`) — рядом с остальными ключами балансов (`GET api.deepseek.com/user/balance`).

### Исправлено
- Карты `maps/1/5` и `maps/2/2`: восстановлен старт мыши `+` (при перерисовке лабиринта он потерялся — `MazeLoader.findMouseStart()` ставил мышь в случайную клетку, `tests/check_map.py` давал FAIL).

### Изменено
- Обновлены карты: `maps/1/1`, `1/3`, `1/5`, `2/2`, `2/3`, `final` (правки в редакторе).
- Пересобраны затронутые билды: `.\gradlew build` (198 тестов PASSED), Desktop-дистрибутив (`lwjgl3:installDist`), debug/release APK (`android:assembleDebug`/`assembleRelease`); превью карт перегенерированы (`tools/map_preview.py`).
- Версия **1.0.55** (`Constants.VERSION`, `android` versionCode 55 / versionName 1.0.55).

### Документация
- Обновлены `AGENTS.md` (раздел «Проверка балансов» → ссылка на навык, правило 3), `GAME.md` (пп. 87–88), `PLAN.md`.

## [1.0.54] — 2026-09-21

### Добавлено
- **Сердечки (этап 15.3) — игровой эффект**: подбор сердечка даёт **+1 жизнь** (до `Constants.MAX_LIVES = 7`), очки не меняются.
- **Жизни мыши**: стартовый запас `Constants.START_LIVES = 1`, копится между картами и хранится в `ProgressStore` (новый ключ `Constants.KEY_LIVES`) — переживает рестарт и «Продолжить».
- **Поимка с запасом жизни**: жизнь тратится, мышь **телепортируется** в достижимую клетку, максимально далёкую от всех котов (BFS от котов), и получает **неуязвимость** `Constants.INVULN_TIME = 1.5 с` (мышь мигает ~10 Гц). На последней жизни — экран «Мышка попалась!».
- **HUD**: 7 слотов-сердечек уменьшенного размера (`Constants.HUD_HEART_SIZE = 36`, шаг 36); неактивные слоты — новый серый силуэт `sprites/heart_grey.png` (производный от `heart_full`, `#5A5A5A`; локально, без SpriteCook).
- `Mouse.teleportTo(col, row)` — мгновенный перенос мыши со сбросом движения.

### Изменено
- `Constants.MAX_LIVES` 3 → **7**; `GameWorld` хранит `lives`/`invulnTimer` и всю логику поимки (тестируемо headless).
- `ProgressStore`: `savedLives()`, `saveOnDeath` сбрасывает жизни к стартовым, `saveOnAdvance(level, score, lives)` переносит накопленные, `clear()` сбрасывает.
- `MainGame.startGameDirect(..., startLives)`, «Продолжить» (`ScreenRouter`) восстанавливает жизни из прогресса.
- `sprites/heart_empty.png` больше не используется в HUD (остаётся в ассетах).
- `tests/check_map.py` без изменений — считает одну попытку без жизней; расхождение задокументировано в `tests/README.md`.
- Версия **1.0.54** (`Constants.VERSION`, `android` versionCode 54 / versionName 1.0.54).

### Документация
- Обновлены `MENU.md` (§3.2/§4/§6 — жизни и «Продолжить»), `GAME.md`, `PLAN.md` (этап 15.3 закрыт), `tests/README.md`, `asset-manifest.json`/`.md` (`heart_grey`).

## [1.0.53] — 2026-09-21

### Добавлено
- **Яркость тем**: dev-скрипт `tools/adjust_theme.py` — читает RAW темы, применяет мягкую коррекцию яркости (фон RGB × `backgroundShade`, стены `c' = c + (255−c)×wallLighten`, alpha без изменений), масштабирует LANCZOS в игровой размер (200×200 / 160×160) и пишет `background.png` / `wall_autotile.png`. Значения хранятся в `theme.json` (`backgroundShade`/`wallLighten`; дефолт 15% — `0.85`/`0.15`) для воспроизводимой перегенерации из `raw/`. Оригиналы `raw/` не трогаются.

### Изменено
- Тема «Старый замок» (`castle`) перегенерирована из RAW с коррекцией 15% (фон темнее, стены мягко светлее); `view/` обновлён.
- Карта `2/1` (мир «Старый замок») обновлена в редакторе карт.
- Версия **1.0.53** (`Constants.VERSION`, `android` versionCode 53 / versionName 1.0.53).

### Документация
- Навык `generate-theme` дополнен разделом о яркости (`adjust_theme.py`) и полями `theme.json`; обновлены `GAME.md`, `PLAN.md`.

## [1.0.52] — 2026-09-21

### Добавлено
- **Сердечки (этап 15)** — новый элемент карты, символ `H` (латинская, от HEART), до **3 на карте** (`Constants.MAX_HEARTS_PER_MAP`):
  - Редактор (15.1): инструмент-сердечко (`heart_full.png`) в палитре, счётчик `×N/3` на свотче и в подсказке, хоткей `H`; ЛКМ ставит только на пустую `' '`, ПКМ стирает; при переборе лимита лишнее игнорируется.
  - `MazeLoader.findHeartCells()`, `MapEditor.ALLOWED += 'H'`, `heartCount()`, `fillHeartRect()` (`H` не считается сыром и не нумеруется `normalizeCheese()`).
  - Игра (15.2): сущность `entities/Heart.kt` (флаг `collected`), список `GameWorld.hearts`, спавн `spawnHearts()` из `findHeartCells()`, отрисовка `GameScreen.drawHearts()` (как сыр) и съедание при наезде мыши — **без игрового эффекта** (очки/жизни не меняются; эффект — этап 15.3).
- **Тултипы свотчей палитры редактора**: при наведении под тулбаром показывается подпись символа («Стена», «Проход (пусто)», «Сыр (до 9)», «Сердечко (до 3)», «Старт мыши», «Старт кота A–D»).
- Тесты: `HeartTest`, тесты сердечек в `MapEditorTest`, `MazeLoaderTest.findHeartCells`, `GameWorldSimulationTest` (спавн по `H`, подбор без эффектов) — всего **192**.

### Изменено
- **Редактор**: режим заливки (`R`) полностью убран (кисть + ПКМ-стирание); **один старт кота на букву** — `MapEditor.setCatStart()` переносит кота, дубликаты `A`–`D` больше не создаются (раньше можно было нарисовать сколько угодно).
- **Desktop-окно стартует стандартно**: убрано `GLFW_FLOATING` (окно было always-on-top и мешало другим окнам) и фокус-хак (`ShowWindow`/`FocusWindow`/`RequestAttention`, `requestFocusDelayed`) — окно больше не всплывает поверх других.
- `tests/check_map.py`: в статике допустим символ `H`.
- **Неблокирующие запуски Desktop** (`lwjgl3/build.gradle.kts`): хелпер `registerDetachedRun` и фоновые задачи `runBg` (игра, аргументы `-PgameArgs="..."`), `runEditor` (`--editor`), `runDistBg` (installDist + дистрибутив) — стартуют отдельный java-процесс и сразу завершают Gradle-задачу (логи в `lwjgl3/build/{run-logs,editor-logs}/`). `run` (JavaExec) и `runDist` (Exec) по-прежнему блокирующие.
- Версия `1.0.52` (`Constants.VERSION`, `android` versionCode 52 / versionName 1.0.52).

### Документация
- Обновлены `GAME.md`, `PLAN.md` (этапы 15.1/15.2), `AGENTS.md`, `README.md` (символ `H`), `CHANGELOG.md`.
- Задокументированы неблокирующие запуски Desktop: `runBg` / `runEditor` / `runDistBg` (отдельный процесс, логи в `lwjgl3/build/{run-logs,editor-logs}/`; `run` и `runDist` блокируют терминал) — `AGENTS.md`, `RUN.md`, навык `run-game`, `GAME.md`. Правило «все запуски — неблокирующие, не ждать в фоне» и предупреждение перед долгой сборкой.

## [1.0.51] — 2026-09-20

### Добавлено
- **Desktop-редактор карт** (`--editor`, dev, вне APK): `EditorScreen.kt` (lwjgl3) — WYSIWYG-рендер через общий `MazeRenderer`, палитра символов, кисть и заливка прямоугольником, undo/redo, листание карт, диалоги, playtest по **F5** с возвратом по **ESC**, сохранение (`Ctrl+S`).
- Логика редактора в `core`: `MapEditor.kt` (сетка 32×16, `setCell`/`fillRect`, `undo`/`redo`, `load`/`toText`, `validate`, `normalizeCheese`, `fillCheeseRect`), `EditorFiles.kt` (поиск папки `assets`, атомарное сохранение `*.tmp` → `rename`).
- `MazeRenderer.kt` (core) — общий рендер фона/стен (dual-grid autotile + классические) для игры и редактора.
- **Сыр**: один инструмент + счётчик (лимит 9), ставится только на пустую клетку `' '`; при сохранении сыр нумеруется `1..N`.
- При сохранении рядом с `.txt` генерируется превью `<карта>.png` (offscreen `FrameBuffer` + `PixmapIO`); окно **«Карта сохранена»** с кнопкой «ОК».
- Тесты редактора: `MapEditorTest`, `EditorAssetsTest`, `MainGameEditorTest` (всего 175).

### Изменено
- `GameScreen`: playtest-параметры `overrideMazeText`/`saveProgress`/`onExitOverride`; рендер фона/стен делегирован `MazeRenderer`; сетка редактора по **G**, маркеры в игровом размере.
- `Lwjgl3Launcher`: флаги `--editor` и `--assets-dir`; `MainGame.editorFactory`.
- Карты мира 1 (`1/1`–`1/4`) перерисованы.
- Версия `1.0.51` (`Constants.VERSION`, `android` versionCode 51 / versionName 1.0.51).

### Исправлено
- Выход из playtest по **ESC** возвращал не в редактор, а закрывал приложение: dispose playtest-экрана и `setScreen` отложены через `Gdx.app.postRunnable` (после текущего кадра).

### Документация
- `AGENTS.md`, `GAME.md`, `PLAN.md`, `RUN.md`, навык `.opencode/skills/run-game/`.

## [1.0.50] — 2026-09-19

### Добавлено
- **Kotlin-тесты игровой логики** (142 теста, JUnit 5 + `kotlin("test")` + headless LibGDX) в `core/src/test/kotlin`; запуск `.\gradlew core:test`, входят в общий `.\gradlew build`. Хелперы `GdxTestBase`/`FakeApplication`/`FakeInput`/`FakePreferences`/`TestFakes`, фикстуры `core/src/test/resources` + реальные `assets`.
- **Релиз в RuStore**: ключ подписи `android/catsandmouses-release.jks` (RSA 2048, alias `catsandmouses`), `keystore.properties` (gitignored), `signingConfigs.release`/`buildTypes.release` в `android/build.gradle.kts`; релизные `.apk`/`.aab`; иконка витрины 512×512 и скриншоты 1920×1080; ASO-листинг (Аркады + Казуальные, 5 тегов, 0+); версия 1.0.49 отправлена на модерацию (ручная публикация).
- **Автоматизация RuStore**: `tools/rustore-publish.ps1` (token/apps/status/release/publish/settings/icon/screens/reviews/rating), `store/rustore/version.json`, Gradle-задача `rustoreRelease`; навыки `.opencode/skills/rustore-aso/` и `.opencode/skills/rustore-publish/`; `FAQ.md`.

### Изменено
- **Заставка**: подсказка «Нажми пробел» → нейтральная **«Нажми, чтобы начать»** (подходит и для клавиатуры, и для тача).
- **Рефакторинг ради тестируемости**: игровая симуляция вынесена из `GameScreen` в `GameWorld` (без рендера), добавлен `MazeLoader.fromText(...)`; поведение сохранено.
- Версия `1.0.50` (`Constants.VERSION`, `android` versionCode 50 / versionName 1.0.50).

### Документация
- Обновлены `AGENTS.md`, `GAME.md`, `PLAN.md`, `MENU.md`, `tests/README.md`.

## [1.0.49] — 2026-09-18

### Добавлено
- **Dev-чекер карт** `tests/check_map.py` (Python, stdlib; в игровую сборку/APK не входит) — комплексный анализ одной карты по полному пути `--map <path>.txt`: статика (формат 16×32, периметр, 4 кота `A–D`, BFS-достижимость сыра/котов, изолированные области, тупики/мосты, дистанции) + точный порт игрового цикла `GameScreen.updatePlaying`/`Cat`/`Mouse` + Monte-Carlo (`--runs`, `--seed`). Вердикт ДА/НЕТ; при ДА печатается маршрут лучшего победного прогона. Прогрессбар прогонов в `stderr` (`--no-progress`).
- Эталонные карты `tests/fixtures/{win_100,fail_100,random_spawn}.txt` (100 % побед / 0 % / микс ~53 %) и `tests/README.md`.

### Изменено
- **Название Android-приложения** — «Кошки-мышки»: манифест использует `@string/app_name`, строка обновлена в `android/res/values/strings.xml` (было `CatsAndMouses`).
- `AGENTS.md`, `GAME.md`, `PLAN.md` — команда и раздел `tests/`, статус dev-чекера.
- Версия `1.0.49`.

## [1.0.48] — 2026-09-18

### Изменено
- **Активная кнопка** (под фокусом) во всех меню и оверлеях визуально приподнята **влево-вверх на 10 px** и подсвечена (`button_pressed.png`); при нажатии (тач/клик или удержание Space/Enter) возвращается на место с обычной текстурой (`button_normal.png`) — эффект отжима. Область нажатия не смещается.
- В `UiButton` разделены `isFocused` (активная) и `isPressed` (фактически нажата); кнопка паузы теперь тоже активная, а не постоянно «нажата».
- Версия `1.0.48`.

## [1.0.47] — 2026-09-18

### Добавлено
- **Системная тема** (`system` = `grass`) в `assets/themes/themes.json`: заставка, главное меню и все оверлеи используют её; `ThemeLoader.system()` / `forId(id)` с откатом на системную.
- **Тема на мир**: `assets/maps/<мир>/theme.json` = `{"theme":"<id>"}`, финал — `assets/maps/final.json`; тема мира применяется к лабиринту и перезагружается при переходе между мирами.
- **6 миров по 5 карт + финал = 31 уровень**: `grass`, `castle`, `ice`, `desert`, `moss`, `swamp`, финал — `space` (открытое поле со стенами-словом «КОНЕЦ», 4 преследователя).
- **`LevelCatalog`** и манифест `assets/maps/maps.json` (Gradle-задача `generateMapsManifest`): авто-подсчёт миров/карт и порядок прохождения (миры → карты → `final`), работает в jar/APK.
- **Генератор уровней** `tools/gen_levels.py` (детерминированный seed, валидация связности, `+`/`A`–`D`/сыр, 32×16) и **dev-превью карт** `tools/map_preview.py` (PNG 1:1 рядом с `.txt`).
- Тема **«Космическая станция»** (`space`) — 7-я тема локации.

### Изменено
- Карты расширены до **32×16** (`MAZE_COLS = 32`, лабиринт 1280×640 без боковых полей); хардкод углов котов заменён на `Constants.MAZE_COLS - 2` / `MAZE_ROWS - 3`.
- Флаг **`--theme <id>`** стал отладочным: форсирует тему всех карт и запускает игру без меню (карта — по `--start-map`, иначе `1/1`); `--start-map` помечается явным флагом.
- Превью карт и `raw/`/`view/` исключены из APK/jar; `assets/maps/**/*.png|jpg` — в `.gitignore`.

### Документация
- `PLAN.md` приведён к этапу 12; обновлены `GAME.md`, `MENU.md`, `AGENTS.md`, навык `run-game`; версия `1.0.47`.

## [1.0.46] — 2026-09-18

### Добавлено
- **15-piece dual-grid autotile стен**: маска 4 соседей и маппинг в 15 спрайтов (`GameScreen.drawAutotileWalls`), со сдвигом тайла на −`CELL_SIZE/2`; поля `wallAutotile`, `tileSize`, `atlasColumns`, `atlasRows` в `theme.json` и их чтение в `ThemeLoader`. Старый режим `wallFiles` оставлен как fallback.
- **Шесть тем локаций** (`assets/themes/<id>/`): «Трава и кусты» (`grass`, autotile кустов), «Старый замок» (`castle`), «Ледяная пещера» (`ice`), «Песчаный каньон» (`desert`), «Мох и древние деревья» (`moss`), «Затерянное болото» (`swamp`). У каждой — свой autotile стен и бесшовный фон; регистрация в `assets/themes/themes.json`.
- Флаг запуска **`--theme <id>`** (`--theme castle`, `--theme=castle`): переопределяет `active` в `themes.json`, применяется к заставке, меню и лабиринту; неизвестная тема — откат на `active`.
- Навык **`generate-theme`** (полная процедура генерации темы) и ссылка на него в `AGENTS.md` и навыке `run-game`.

### Изменено
- Фон темы рисуется **1:1 в натуральном размере текстуры** (200×200) и тайлится по всему экрану, без сжатия в 40×40; лабиринт прижат к нижнему краю (`MAZE_OFFSET_Y = 0`), центрирование по горизонтали сохранено.

## [1.0.45] — 2026-09-17

### Добавлено
- Рядом с версией в HUD, главном меню и на заставке показывается **режим запуска**: `v<версия>(d)`, `(r)`, `(i)`, `(a)` — dev / дистрибутив / IDEA / Android. Определяется автоматически (`LaunchMode.kt`): dev — `-Dlaunch.mode=d` в таске `:lwjgl3:run`, dist — по классам в jar, idea — по `-Dlaunch.mode=i` в конфигурации IDEA.
- Заставка: переход в меню — по **пробелу** или **тапу** (раньше — по любой клавише); текст подсказки «Нажми пробел»; версия выводится мелким шрифтом (16).
- Версия на **всех экранах** (заставка, меню, HUD) выводится мелким шрифтом `FONT_SIZE_HUD` (16); в главном меню для неё заведён отдельный `versionFont`.
- **Три run-конфигурации IDEA** в `.run/` («Run Dev», «Run Idea», «Run Dist») и Gradle-таска `:lwjgl3:runDist` (installDist + запуск дистрибутива); `.run/` убран из `.gitignore`.

## [1.0.44] — 2026-09-17

### Добавлено
- **`MENU.md`** — спецификация экранов, меню, кнопок и переходов (источник истины для навигации, с Mermaid-схемой). Правило «изменения меню начинать с `MENU.md`» закреплено в `AGENTS.md`.
- Классы, сгенерированные по спецификации: **`ScreenRouter`** (все переходы между экранами), **`MenuDefinitions`** (декларативные меню/кнопки), **`ProgressStore`** (единая точка работы с `Preferences`).
- Список карт вынесен в `Constants.MAP_FILES`.

### Изменено
- Главное меню: кнопка **«Играть» → «Начать»** (сброс прогресса, старт с карты 1); **«Продолжить»** стартует с карты, где убили; **«Выход»** завершает приложение.
- Оверлей проигрыша: заголовок **«Мышка попалась!»**, кнопки **«Попробуй ещё!»** (та же карта) и **«Выйти»** (в главное меню) — вместо «GAME OVER» / «Заново» / «В меню». Оформлен как главное меню: полноэкранный `UiMenuOverlay` с фоном темы, заголовком на отступе 40 px и двумя кнопками по центру (без тёмной панели); активная кнопка по умолчанию — верхняя, переключение ↑/↓, активация Space/Enter/tap/клик.
- ESC-меню: заголовок **«Выходим?»**, кнопки **«Отмена»** (в игру) и **«Выйти»** (выход из игры); оформлено так же, как Game Over/Win (полноэкранный `UiMenuOverlay`).
- Заставка (`LoadingScreen`): добавлен заголовок **«Кошки-мышки»** (тёмный текст со светлой обводкой); надпись вынесена в `MenuDefinitions.TITLE_GAME`.
- Главное меню: заголовок сменён на **«Меню»** (`MenuDefinitions.TITLE_MENU`); зазор между кнопками уменьшен на 30 %, затем ещё на 50 % (20 → 14 → 7 px); заголовок поднят к тому же отступу, что у заставки (60 → 40 px от верха).
- Оверлей победы (`winDialog`) оформлен так же, как Game Over: полноэкранный `UiMenuOverlay` (фон темы, заголовок на отступе 40 px, кнопки по центру, активная верхняя, Space/Enter/tap/клик).
- **Счёт и рекорд скрыты** из HUD и главного меню (счёт временно не ведём).

### Исправлено
- Устаревший `winDialog`: признак финальной карты (`isFinal`) запоминался при первом построении и не обновлялся при переходе на финальную карту — кнопка «Следующая карта» вела к выходу `currentLevel++` за границы массива (краш). Диалоги пересобираются при смене уровня, финальная карта показывает «Финал пройден!» / «В меню».
- Удалён неиспользуемый UI-примитив `UiDialog` (все оверлеи переведены на `UiMenuOverlay`).

## [1.0.43] — 2026-09-15

### Исправлено
- Кнопки главного меню не реагировали на тач на Android: координаты тача считались в пикселях экрана без проекции через `viewport.unproject()`. В `MainMenuScreen` добавлен `unprojectTouch()` (как в `GameScreen`). Проверено на устройстве: тап по «Играть» работает. Версия `1.0.43`.
- Кодировка консоли Desktop/Gradle: в `gradle.properties` и `lwjgl3/build.gradle.kts` добавлены `-Dstdout.encoding=UTF-8`/`-Dstderr.encoding=UTF-8` — корректный вывод кириллицы в логах.

## [1.0.42] — 2026-09-15

### Исправлено
- Пауза в AI/MCP-режиме: снятие паузы через `toggle_pause` теперь работает — в блоке паузы обработка переведена на `inputHandler.isPauseJustPressed()` (покрывает Space и pause-запрос от MCP). `GAME.md` — статус 54 дополнен. Версия `1.0.42`.

## [1.0.41] — 2026-09-15

### Изменено
- Экран паузы переработан: вместо диалога «ПАУЗА» с тремя кнопками — затемнение экрана на 25 % и единственная кнопка **«Продолжить»** по центру. Пауза снимается по Space, Enter, ESC, Android BACK и tap/клику по кнопке. `UiButton` получил метод `triggerClick()` для программного нажатия.
- `AGENTS.md`: добавлены разделы «Проверка балансов», «Правила запуска игры для тестирования» и «Шаг 0: Проверка прокси» перед работой со SpriteCook. Версия `1.0.41`.

## [1.0.40] — 2026-09-15

### Изменено
- Шрифт UI заменён с PT Sans на пиксельный **BoldPixels** (`assets/fonts/boldpixels.ttf`, CC BY-SA 4.0): кнопки, HUD, диалоги, титул меню, экран загрузки. Размеры кратны 16 (HUD 16, кнопки 32, оверлеи 32, титул 64) для пиксель-перфекта. Палитра текста: кремовый `#F5E6C8`, тёмно-коричневая обводка `#3B2412` (вшита в глифы через FreeType border), акцент `#C78A52` — нажатые кнопки и «Рекорд:».
- Расстояние между кнопками главного меню уменьшено до 20 px (было 30).
- Атрибуция: **BoldPixels Font by Yūki (@YukiPixels)**, CC BY-SA 4.0, `yukipixels.itch.io/boldpixels`. Версия `1.0.40`.

## [1.0.39] — 2026-09-15

### Добавлено
- Промежуточный ассет `loading_logo_3` (база `loading_logo_4`) внесён в git и манифест (`asset_id` `39c1c21d…`, полный id уточнить — SpriteCook был недоступен): `assets/ui/loading_logo_3.png` (600×429), `ui/raw/loading_logo_3.png` (2048×2048), `ui/view/loading_logo_3.png`. В игре не используется.
- `asset-manifest.json`/`.md` (запись `39c1c21d` → `ui/loading_logo_3.png`), `GAME.md` — статус 52. Версия `1.0.39`.

## [1.0.38] — 2026-09-15

### Добавлено
- Логотип загрузки v4 (`assets/ui/loading_logo_4.png`, 600×430) подключён как заставка: три мыши над деревянной доской (розовая мама с бантиком, серая дочка, чёрный папа), сгенерировано через SpriteCook (`gemini-3-pro-image`, 1K, asset_id `94c50498`). RAW-мастер 1024×1024 JPEG→PNG; игровой файл — кроп y[140..874] + ланцошем 600×430. `LoadingScreen.kt` загружает `ui/loading_logo_4.png` и рисует 600×430.
- `asset-manifest.json`/`.md` (запись `94c50498` → `ui/loading_logo_4.png`, raw 1024×1024), `GAME.md` — статус 51. Версия `1.0.38`.

## [1.0.37] — 2026-09-15

### Добавлено
- Быстрый запуск Desktop без пересборки: автономный дистрибутив `lwjgl3:installDist` (`lwjgl3/build/install/lwjgl3/`, launcher `bin/lwjgl3.bat`, все ассеты в `lib/lwjgl3.jar`, `raw/`/`view/` исключены) и корневой `desktop-run.bat` для запуска двойным кликом. Пересборка дистрибутива — только после изменений кода/ассетов.
- Старая Gradle run-конфигурация IDEA удалена; вместо неё — Application-конфигурация «CATS AND MOUSES Desktop» (`com.catsandmouses.lwjgl3.Lwjgl3Launcher`, module `CatsAndMouses.lwjgl3.main`, working dir `$PROJECT_DIR$/assets`).
- `GAME.md` (раздел «Сборка и запуск», статус 51), `AGENTS.md` (команды `installDist`/`desktop-run.bat`). Версия `1.0.37`.

## [1.0.36] — 2026-09-15

### Добавлено
- Новая композиция логотипа загрузки `assets/ui/loading_logo_2.png` (1024×512): три мыши в центре над широкой деревянной доской (розовая мама с бантиком, серая дочка, чёрный папа) и четыре кота снизу. Сгенерировано через SpriteCook (`gemini-3-pro-image`, 2K, asset_id `20be8e8c`), стиль закреплён style_asset_ids игровых спрайтов (мышь + 4 кота). RAW-мастер 2752×1536 пришёл JPEG-байтами → сконвертирован в PNG (`ui/raw/loading_logo_2.png`); игровой файл — кроп центра до 2:1 (срез только листовых полей) + ланцошем до 1024×512; превью `ui/view/loading_logo_2.png`. Пока НЕ подключён — экран загрузки использует старый `loading_logo.png`.
- `asset-manifest.json`/`.md` (запись `20be8e8c` → `ui/loading_logo_2.png`, raw 2752×1536), `GAME.md` — статус 50. Версия `1.0.36`.

## [1.0.35] — 2026-09-15

### Исправлено
- Экран загрузки держался всего пару секунд и игра стартовала сама: `Lwjgl3Launcher` после старта делал синтетический клик мыши (`java.awt.Robot`) в центр экрана, чтобы вернуть фокус окну, — этот клик воспринимался игрой как реальный ввод и нажимал «Играть» в меню. Синтетический клик убран: фокус возвращается только через GLFW (`glfwFocusWindow`), никакого ложного ввода. Проверено: заставка держится без ввода, переход — только по нажатию/тапу.

## [1.0.34] — 2026-09-15

### Изменено
- Экран загрузки: логотип увеличен до `600×390` (по ширине 600 из raw, пропорции 1.54:1, не искажается). Прогрессбар убран; текст «Нажми любую клавишу» по центру экрана. Переход в меню — только по реальному нажатию/тапу через `InputAdapter` (мин. guard 1 сек, без спуриусных событий фокуса).
- `asset-manifest.json`/`.md` (loading_logo `600x390`), `GAME.md` — статус 48.

## [1.0.33] — 2026-09-15

### Изменено
- Логотип загрузки перегенерирован из raw: обрезка по контенту (alpha>10 → `2001×1300`) и уменьшение ланцошем до `400×260` (пропорции 1.54:1, был квадрат 102×102). `LoadingScreen.kt` рисует логотип `400×260` вместо `400×200` — без искажений.
- `tools/make_view.py` переписан: превью `view/` теперь 1:1 в размер игрового спрайта (клетка шахматки 4px, увеличение отменено); все 16 превью перегенерированы.
- `asset-manifest.json`/`.md` (loading_logo `400x260`), `AGENTS.md` (конвенция view 1:1), `GAME.md` — статус 47.

## [1.0.32] — 2026-09-15

### Изменено
- `raw/` и `view/` исключены из игровой сборки, но теперь хранятся в git: каталоги `assets/**/view/` убраны из `.gitignore`. Из APK вычищаются после merge-задачи ассетов (`android/build.gradle.kts` — AGP не фильтрует `sourceSet.assets` через `.exclude()`); из jar/dist — исключением в jar-задаче (`lwjgl3/build.gradle.kts`, игровые ассеты в jar соблюдены, Desktop-дистрибутив сохранён).
- Конвенция в `AGENTS.md` («Конвенции по спрайтам»): `raw/`/`view/` — в git, в рантайме-игре не используются; `GAME.md` — статус 46.
- Проверено `.\gradlew build` — `BUILD SUCCESSFUL`; в APK (debug/release), jar и distZip `raw/` и `view/` отсутствуют, игровые ассеты (sprites, themes, ui, maps) на месте; `lwjgl3:run` — загрузка до меню.

## [1.0.31] — 2026-09-15

### Добавлено
- Превью-спрайты `view/` рядом с `raw/` (sprites, themes/grass, ui): те же игровые спрайты на чёрно-белой шахматке (1 клетка ≈ 1 пиксель оригинала, масштаб до ~512px) — для визуального контроля границ холста и контента. Генератор — `tools/make_view.py` (Pillow, dev-only).
- Правило в `AGENTS.md` («Конвенции по спрайтам»): после генерации/перегенерации спрайтов создавать `view/` рядом с `raw/`; каталоги `assets/**/view/` добавлены в `.gitignore` (в git/игру/сборку не попадают).
- `GAME.md`: статус 45.

## [1.0.30] — 2026-09-15

### Изменено
- Сыр уменьшен с 25% **до 40%**: `cheese.png` перегенерирован из raw — холст 64×64, контент масштабирован до высоты 38px (60% от 64, ≈34×38) и центрирован; на экране при роли 48×48 сыр выглядит ≈26×29px. Код/`Pellet.SPRITE_SIZE` не менялись — уменьшение через контент внутри холста.
- Конвенция в `AGENTS.md` («Конвенции по спрайтам») и `_comment` манифеста актуализированы: сыр всегда уменьшается на 40% (контент 38px по высоте на холсте 64×64).
- `asset-manifest.json`/`.md`: строка сыра — `content 43x48 → 34x38`; `GAME.md` — статус 44 скорректирован на 40%.

## [1.0.29] — 2026-09-15

### Изменено
- Сыр уменьшен на 25%: `cheese.png` перегенерирован из raw — холст 64×64, контент масштабирован до высоты 48px (≈43×48) и центрирован; на экране при роли 48×48 сыр выглядит ≈32×36px. Код/`Pellet.SPRITE_SIZE` не менялись — уменьшение через контент внутри холста.
- Конвенция закреплена в `AGENTS.md` («Конвенции по спрайтам») и в `asset-manifest.json` (`_comment`): сыр всегда уменьшается на 25% (контент 48px по высоте на холсте 64×64).
- `asset-manifest.json`/`.md`: строка сыра — `content 57x64 → 43x48`.

## [1.0.28] — 2026-09-15

### Добавлено
- `assets/asset-manifest.md` — человекочитаемая таблица манифеста ассетов (рядом с JSON).
- Правила генерации в `AGENTS.md`: только RAW-версия PNG (1024/2K/4K как есть), pixel-варианты для игры не использовать, формат проверять по магическим байтам (не Content-Type), не-PNG → конвертация на месте с сообщением пользователю, обрезка по видимому краю (alpha > 10), ресайз ланцошем.

### Изменено
- Спрайты: 8 игровых спрайтов (мышь, сыр, 4 кота, 2 сердечка) перегенерированы из RAW-мастеров в прозрачные холсты 64×64 с контентом по центру; мышь переориентирована «носом вниз». Кнопки переобработаны из raw 2K с сохранением пропорций: `ui/button.png` → 360×149, `ui/button_pressed.png` → 360×175.
- Размеры ролей на экране → 48×48: `Mouse.kt` `SPRITE_SIZE 32→48` + `SPRITE_ROTATION_OFFSET=90f`, `Cat.kt` `50→48`, `Pellet.kt` `28→48`, `Constants.kt` `HEART_SIZE 28→48`.
- `assets/raw/mouse.png` конвертирован из JPEG в PNG 1024×1024 (ключевание белого фона) — теперь все raw — PNG.
- `asset-manifest.json`: актуализированы `dims`/`content`, добавлено человекочитаемое `asset-manifest.md`.
- Документация `GAME.md`: актуализированы описания спрайтов и «Текущий статус» (статус 43).

## [1.0.27] — 2026-09-14

### Исправлено
- Экран загрузки показывался на чёрном фоне: текстура фона темы грузилась каждый кадр и удалялась (`dispose()`) до `batch.end()` — тайлы рисовались с уже удалённой GL-текстурой. Фон теперь грузится один раз в `show()` (`LoadingScreen.kt`).
- Кнопки меню: `button.png` (398×453) имел непрозрачное «лицо» лишь ~40% высоты внизу холста — видимая кнопка была ~84px вместо задуманных 216. Кнопки обрезаны до непрозрачных границ (`button.png` → 387×182, `button_pressed.png` → 115×64, RAW-мастера в `assets/ui/raw/`), размер кнопки приведён к пропорции арта (`BUTTON_WIDTH 360` / `BUTTON_HEIGHT 170`).
- Раскладка кнопок меню: высота группы считалась по ещё пустому списку кнопок → `startY` уезжал вниз и нижняя кнопка уходила под экран. Группа центрируется по вертикали (зазор 30px), при нехватке места прижимается к краям без обрезки.

### Изменено
- `Constants.kt`: `BUTTON_WIDTH`/`BUTTON_HEIGHT` под пропорцию кнопки-арта; версия → **1.0.27**.
- Версия Android поднята до 1.0.27 (versionCode 27), `Constants.VERSION` обновлён.

## [1.0.26] — 2026-09-14

### Добавлено
- Экран загрузки `LoadingScreen`: фон темы, логотип, полоска прогресса с процентами, автопереход в главное меню.
- Главное меню `MainMenuScreen`: фон темы, титул, рекорд, кнопки «Играть», «Продолжить» (при наличии прогресса), «Выход» (Desktop); навигация клавишами и тачем.
- UI-примитивы `UiElements.kt`: `UiButton` с текстурами normal/pressed, `UiPanel`, `UiDialog`.
- Сохранение прогресса в `Preferences`: `bestScore`, `lastLevel`, `lastScore`, `hasProgress`.
- HUD-плашка сверху: счёт, рекорд, сыр, карта, сердечки жизней (placeholder: 1 из 3).
- Оверлеи в `GameScreen`: пауза, Game Over, Win, диалог «Выйти?».
- Генерация UI-спрайтов через SpriteCook (`gpt-image-2.5-flare`, medium): `assets/ui/button.png`, `button_pressed.png`, `loading_logo.png`, `assets/sprites/heart_full.png`, `heart_empty.png` + raw-оригиналы.

### Изменено
- `MainGame.kt`: медиатор экранов (Loading → Menu → Game), AI/дебуг-режим запускается мимо меню.
- `GameScreen.kt`: удалены авто-таймеры Game Over/Win; ESC открывает диалог «Выйти?»; Android BACK ставит паузу в игре и выходит из приложения в меню.
- `Constants.kt`: UI-константы (`HUD_HEIGHT`, `BUTTON_WIDTH/HEIGHT`, `HEART_SIZE`, `MAX_LIVES`), ключи Preferences.
- `InputHandler.kt`: добавлен `cancelTouch()` для корректной работы с UI.
- `asset-manifest.json`: добавлены asset_id для новых UI-спрайтов и сердечек.
- Версия Android поднята до 1.0.26, `Constants.VERSION` обновлён.

## [1.0.25] — 2026-09-14

### Добавлено
- Выход из игры по **ESC** на Desktop: `Gdx.app.exit()` при нажатии Escape (проверяется только для `ApplicationType.Desktop`), работает в любом состоянии игры.

### Изменено
- Карта `assets/maps/1/1.txt`: добавлены стартовые клетки мыши (`++`), скорректированы стены в правом нижнем углу.
- `README.md`: актуализировано под текущее состояние — 4 кота (добавлен Ambush), прогрессия карт, тач-управление свайпами, темы, AI-режим и MCP-сервер, новый формат карт (`1`–`9`, `+`, `A`–`D`).
- `AGENTS.md`: добавлен раздел «Правила пуша в GitHub» (GitHub — публичное зеркало, пуш только master по явной просьбе).
- Навык `git-workflow`: обычный пуш — только в `origin`; в `github` — только `master` по команде «пуш в github».
- Версия Android поднята до 1.0.25, `Constants.VERSION` обновлён.

## [1.0.24] — 2026-09-14

### Исправлено
- Баг мгновенной победы при переходе на карту без цифр сыра, но со стартами котов (1/2, 1/3, 1/4): `spawnPellets()` считал стартовые клетки котов занятыми, поэтому ветка «сыр на стартах A–D» не создавала ни одного сыра → пустой `pellets` → `pellets.all { it.eaten }` возвращал `true` и игра сразу завершалась `YOU WIN`. Убрана проверка `occupied` в этой ветке (коты уходят со старта в первый кадр). Проверено через AI-режим на карте 1/2: `cheese_total=4`, `cheese_collected=0`.

### Изменено
- Версия Android поднята до 1.0.24, `Constants.VERSION` обновлён.
- `GAME.md`: статус 39 — описание бага и фикса.

## [1.0.23] — 2026-09-14

### Исправлено
- Desktop-окно получает фокус ввода (клавиатуру) без клика при запуске из Gradle/IDE: повторные попытки `glfwFocusWindow` + `glfwRequestWindowAttention` через `requestFocusDelayed()` на render-потоке; при foreground-локе Windows — активация окна синтетическим кликом через `java.awt.Robot` (центр экрана совпадает с центром окна; смещение не распознаётся как свайп).

### Изменено
- Отключена рандомизация дополнительных стен в теме «Трава и кусты»: `theme.json` → `wallFiles: ["wall_0.png"]` (один базовый спрайт, допы `wall_1..3.png` не используются).
- Версия Android поднята до 1.0.23, `Constants.VERSION` обновлён.

## [1.0.22] — 2026-09-14

### Добавлено
- MCP-сервер `mcp-server/` для управления игрой из OpenCode: `server.py` на FastMCP 4.0.3 (64-bit Python 3.12) с инструментами `get_game_state`, `send_move`, `toggle_pause`, `reset_level`; подключён в `opencode.json` (блок `mcp.cats-and-mouses`).
- Тест `mcp-server/test_ai_mode.py`: схема RESET → LEFT/DOWN/RIGHT/UP до стен подтверждает все четыре направления движения мыши в AI-режиме.

### Изменено
- Версия Android поднята до 1.0.22, `Constants.VERSION` обновлён.
- `GAME.md`: статус 36 — MCP-сервер создан и протестирован.

## [1.0.21] — 2026-09-14

### Добавлено
- AI-отладочный режим для Desktop: флаг `--ai-mode [PORT]` запускает встроенный HTTP-сервер (`com.sun.net.httpserver.HttpServer`) для управления мышью извне. Порт настраиваемый (дефолт 8080); без флага сервер не стартует.
- `AiDebugServer` в core: эндпоинт `GET /state` возвращает JSON с состоянием игры (карта, счёт, мышь, коты, сыр, жизни); эндпоинт `POST /input` принимает команды `move`, `pause`, `reset`.
- Публичные методы в `InputHandler` (`setDirection`, `togglePause`, `getLastInputDirection`) и `GameScreen` (`getGameState`, `getMouseCell`, `getMouseDirection`, `getCatsInfo`, `getRemainingCheese`, `getScore`, `getLives`, `resetLevel`) для взаимодействия с AI-сервером.
- Обработка `BindException`: если порт занят, в лог выводится понятное сообщение "Port N is busy, try another".

### Изменено
- `Constants.VERSION` обновлён до `1.0.21` (синхронизация с `android/build.gradle.kts`).
- `GAME.md`: добавлено описание AI-режима и примеры запуска.

## [1.0.20] — 2026-09-14

### Добавлено
- Система тем локаций: `assets/themes/` с индексом `themes.json`, конфигом `theme.json`, чтецом `ThemeLoader.kt` (встроенный `JsonReader`, без новых библиотек). Активная тема «Трава и кусты»: фон `background.png`, основной спрайт стены `wall_0.png` и допы `wall_1..3.png` (цветущий куст, камень, дерево), которые случайно подмешиваются в `extrasCountMin..Max` стенных клеток — чистый визуал, проходимость не меняется.
- Хранилище RAW-оригиналов SpriteCook: `assets/sprites/raw/`, `assets/themes/grass/raw/`, `assets/icon/raw.png` и манифест `assets/asset-manifest.json` (соответствие `asset_id` → игровой спрайт + raw). Правило «после генерации сохранять raw» закреплено в `AGENTS.md`.
- Иконка приложения: сгенерирована через SpriteCook (`assets/icon/app_icon.png`, raw-мастер 2048×2048), нарезана под Android (adaptive icon API 26+ + легаси mipmap), манифест переключён на `@mipmap/ic_launcher`.

### Изменено
- Стены и фон переехали из `assets/sprites/` в `assets/themes/grass/`: фон `grass.png` → `background.png`, стена `bush.png` → `wall_0.png`. `GameScreen.kt` грузит текстуры из активной темы.
- Поле/файл фона темы переименовано: `grass`/`grass.png` → `background`/`background.png` — универсальное название для любой темы (вода, горы и т.д.).

## [1.0.19] — 2026-09-14

### Добавлено
- Тач-управление свайпами для Android: `InputHandler` наследует `InputAdapter`, обрабатывает `touchDown`/`touchUp` и определяет направление по экранным координатам с порогом 40 пикселей.
- Классическая очередь направлений Pac-Man: свайп запоминает желаемое направление, мышь поворачивает при первой возможности (в центре клетки, если нет стены).
- Совмещённый ввод: клавиатура (WASD/стрелки) и свайпы работают одновременно, приоритет у последнего события ввода.
- `GameScreen`: регистрация `inputProcessor`, сброс ввода при старте новой игры, очистка в `dispose()`.

### Изменено
- Версия Android поднята до 1.0.19, `Constants.VERSION` обновлён.
- `GAME.md`: обновлено описание управления Android, статус и планы — тач-управление отмечено выполненным.

## [1.0.18] — 2026-09-13

### Добавлено
- Явные стартовые позиции котов буквами `A`–`D` на карте: `A` = Барсик, `B` = Матроскин, `C` = Леопольд, `D` = Черныш.
- Если на карте заданы все 4 буквы `A`–`D`, коты спавнятся в них; иначе — рандомно в углах.
- Если цифр сыра (`1`–`9`) нет, но заданы `A`–`D`, сыр автоматически спавнится на клетках котов.
- Если на карте несколько символов `+`, мышь спавнится в случайном из них.
- Флаг `--debug` / `--debug-logs` для Desktop-запуска: включает логи LibGDX; по умолчанию логи отключены.
- Параметр `--start-map` для Desktop-запуска: старт с указанной карты (например, `1/2`, `final`).

### Изменено
- Карты `assets/maps/1/1.txt`–`4.txt` и `assets/maps/final.txt`: угловые стартовые позиции котов теперь обозначены буквами `A`–`D`.
- `MazeLoader`: добавлены `findCatStarts()` для поиска `A`–`D` и `findMouseStart()` теперь выбирает случайный `+`.
- `GameScreen`: использует `A`–`D` для стартов котов и корректно спавнит сыр при отсутствии цифр.
- `Lwjgl3Launcher`: парсинг аргументов `--start-map` и `--debug`.
- Версия Android поднята до 1.0.18, `Constants.VERSION` обновлён.
- `GAME.md`: обновлено описание символов карты, параметров запуска и статуса.

## [1.0.17] — 2026-09-13

### Добавлено
- Прогрессия карт: 4 обычных уровня (`assets/maps/1/1.txt`–`4.txt`) + финальная карта (`assets/maps/final.txt`).
- Новый символ карты `+` — стартовая клетка мыши; при отсутствии выбирается проходимая клетка максимально близко к центру.
- Стартовые позиции котов рандомизированы в радиусе +4 клетки от углов в сторону центра.
- Финальная карта: открытое поле без стен, все 4 кота используют стратегию Chaser со скоростью 150.
- HUD: номер текущей карты «Карта N/5».

### Изменено
- Скорость кота вынесена в параметр конструктора `Cat(..., speed)` с значением по умолчанию 120.
- `MazeLoader.spawnPellets()` теперь использует актуальную стартовую клетку мыши вместо жёстко заданной (13,8).

## [1.0.16] — 2026-09-13

### Изменено
- Спрайты котов увеличены до 50×50 (`Cat.SPRITE_SIZE`), коты намеренно вылезают за границы клетки.

## [1.0.15] — 2026-09-13

### Изменено
- Спрайты котов увеличены на 25%: с 36×36 до 45×45 (`Cat.SPRITE_SIZE`).

## [1.0.14] — 2026-09-13

### Добавлено
- Ручная расстановка сыра цифрами `1`–`9` в `assets/maze.txt`: количество сыра равно числу цифр.
- Автозамена недостижимых или занятых ручных клеток сыра на случайные достижимые клетки.

### Изменено
- `GameScreen.spawnPellets()` теперь сначала проверяет ручную расстановку, а при отсутствии цифр использует автогенерацию 3 сыров.
- HUD: счётчик сыра `Pellets: N/всего` вместо фиксированного `/3`.
- `AGENTS.md` и `GAME.md`: обновлены конвенции символов карты и план этапов.
- Версия Android поднята до 1.0.14, `Constants.VERSION` обновлён.

## [1.0.13] — 2026-09-13

### Добавлено
- Пауза по клавише **Space** во время игры (`PLAYING`): останавливает обновление мира, повторное нажатие снимает паузу.

### Исправлено
- Ориентация спрайтов котов: спрайты нарисованы «вниз», а мышь — «вправо»; добавлено смещение `+90°` при отрисовке, чтобы коты смотрели в направлении движения.

## [1.0.12] — 2026-09-13

### Добавлено
- Четвёртый кот **Черныш** со стратегией **Ambush** (предсказание): идёт не к мыши, а к клетке на 2 клетки впереди по её направлению движения.
- Спрайты для всех 4 котов: `cat_chaser.png`, `cat_patrol.png`, `cat_random.png`, `cat_ambush.png` (16-bit пиксель-арт, top-down, прозрачный фон), загружены и отрисованы через `SpriteBatch`.

### Изменено
- `Cat.kt` теперь рисуется спрайтом с поворотом по направлению движения (как у мыши); временные цветные круги убраны.
- `GameScreen.kt`: добавлен 4-й кот в нижний-правый угол, HUD-легенда расширена до 4 строк, передача `mouse.direction` в `cat.update()`.
- Версия Android поднята до 1.0.12, `Constants.VERSION` обновлён.

## [1.0.11] — 2026-09-13

### Добавлено
- Спрайт сыра (`assets/sprites/cheese.png`, 26×26, прозрачный фон, 16-bit пиксель-арт, top-down), сгенерирован через SpriteCook.

### Изменено
- Пеллеты рисуются спрайтом сыра (28×28 по центру клетки) через `SpriteBatch` вместо белых кружков в `ShapeRenderer`; текстура освобождается в `dispose()`.
- В SpriteCook создан проект `CATS AND MOUSES`, все ассеты перенесены в него.
- Версия Android поднята до 1.0.11, `Constants.VERSION` обновлён.

## [1.0.10] — 2026-09-13

### Добавлено
- Спрайт мыши (`assets/sprites/mouse.png`, 70×70, прозрачный фон, 16-bit пиксель-арт, top-down), сгенерирован через SpriteCook.
- Отрисовка мыши через `SpriteBatch` вместо `ShapeRenderer`; спрайт поворачивается по направлению движения (WASD/стрелки); текстура освобождается в `dispose()`.

### Изменено
- Версия Android поднята до 1.0.10, `Constants.VERSION` обновлён.

## [1.0.9] — 2026-09-12

### Добавлено
- Подключены сгенерированные спрайты через SpriteCook: фон — трава (`sprites/grass.png`), стены — кусты (`sprites/bush.png`), тайлы 40×40 в размер клетки, фильтрация Nearest для пиксель-арта.
- Отрисовка через `SpriteBatch` (трава поверх, кусты поверх травы); синие прямоугольники стен убраны из `ShapeRenderer`; текстуры освобождаются в `dispose()`.

### Изменено
- Версия Android поднята до 1.0.9, `Constants.VERSION` обновлён.

## [1.0.8] — 2026-09-12

### Добавлено
- TTF-шрифт PT Sans с поддержкой кириллицы (`assets/fonts/ptsans.ttf`) и зависимость FreeType для `core`, `lwjgl3`, `android`.
- Русские имена котов: `Барсик`, `Матроскин`, `Леопольд`.
- Счётчики поимок (`catchCount`) и экран Game Over с именем поймавшего кота и его цветом (`Поймал Барсик` и т.д.).

### Изменено
- Формат карты `maze.txt`: `#` — стены, пробел — проходы; символ `.` больше не используется, пеллеты спавнятся в 3 случайных достижимых клетках.
- HUD-легенда перенесена в левый верхний угол.
- `MazeLoader` и `GameScreen` работают только с проверкой на `#` для стен.
- Версия Android поднята до 1.0.8.

## [1.0.7] — 2026-09-12

### Исправлено
- Коты больше не накладываются друг на друга: введена система бронирования целевых клеток (`reservedCells`).
- Коты обновляются последовательно; каждый следующий кот видит целевые клетки предыдущих.
- В `reservedCells` добавляются текущие клетки всех котов и клетка мыши.
- Если кот стоит более 1 секунды из-за брони, он игнорирует резервирование и идёт в занятую клетку (с логом `Cat forced move`).

### Изменено
- Версия Android поднята до 1.0.7.

## [1.0.6] — 2026-09-12

### Исправлено
- Коты больше не накладываются друг на друга: при выборе направления на перекрёстке исключаются направления в клетку, занятую другим котом.
- Добавлен метод `Cat.isCellOccupiedByOtherCat(col, row, allCats)` — проверка по клетке.
- Если все доступные направления ведут в занятые клетки, кот выбирает любое из них (лучше временное наложение, чем застревание).

### Изменено
- Версия Android поднята до 1.0.6.

## [1.0.5] — 2026-09-12

### Изменено
- Версия Android: `versionName` поднят до `1.0.5`, чтобы совпадать с `versionCode = 5`.

### Добавлено
- `Constants.VERSION = "1.0.5"` — единая строка версии для игрового модуля.
- Вывод версии игры в HUD: левый нижний угол (`v1.0.5`).

## [1.0.4] — 2026-09-12

### Добавлено
- `README.md` — краткое описание проекта, технический стек, управление, запуск, структура и статус разработки.

### Изменено
- Версия Android поднята до 1.0.4.

## [1.0.3] — 2026-09-12

### Изменено
- Коты теперь различаются по цвету в зависимости от стратегии: Chaser — красный, Patrol — синий, Random — фиолетовый.
- В класс `Cat` добавлено поле `color: Color`; метод `draw()` использует его вместо фиксированного оранжевого.
- `strategy` в классе `Cat` сделан публичным — нужен для подписей легенды.

### Добавлено
- Легенда цветов котов в HUD (правый верхний угол): цветные кружки радиусом 8 и подписи `Chaser`, `Patrol`, `Random`.
- Версия Android поднята до 1.0.3.

## [1.0.2] — 2026-09-12

### Изменено
- `GAME.md`: списки «Текущий статус» и «Следующие шаги» переведены на нумерацию для отметки выполненных пунктов; отмечены выполненными финальная проверка кэша (статус 6) и запуск игры после восстановления кэша (план 1).
- Версия Android поднята до 1.0.2.

## [1.0.1] — 2026-09-12

### Изменено
- Разобран `.gitignore`: убран мусор из шаблона (Eclipse, NetBeans, неиспользуемые исключения), добавлены `.kotlin/`, `*.jks`, `*.keystore`, `captures/`; разрешено версионировать `.opencode/skills/**`.

### Добавлено
- `AGENTS.md` — инструкции для AI-агентов (чтение `GAME.md` и навыков перед работой, обновление статуса/планов после, правила сборки и изменений).
- `CHANGELOG.md` — этот учёт изменений.
- Навык `git-workflow` (`.opencode/skills/git-workflow/SKILL.md`) — процедура коммита и пуша: поднятие patch-версии, запись в CHANGELOG, ветка `X.Y.Z/<тип>/<суть>`, push, merge в `master`.

## [1.0.0] — 2026-09-12

### Добавлено
- Первая играбельная версия: 2D Pac-Man-подобная игра с инверсией ролей (игрок — мышь, три кота охотятся за ней).
- Каркас проекта на Kotlin + LibGDX 1.12.1: модули `core`, `android`, `lwjgl3`.
- Лабиринт 28×16 из `assets/maze.txt` (стены `#`, точки `.`, проходы-пробелы), загрузка и отрисовка.
- Игрок-мышь: движение строго по сетке (150 px/с), управление WASD/стрелки.
- Три кота (120 px/с), не проходят сквозь стены, не разворачиваются на 180° вне перекрёстков:
  - Cat 1 — Chaser: преследование через BFS на перекрёстках;
  - Cat 2 — Patrol: патруль по периметру, агрессия при близости мыши (радиус 5 клеток);
  - Cat 3 — Random: случайный выбор на перекрёстках, 30% шанс движения к мыши.
- Поедание точек: +10 очков, HUD со счётом и оставшимися точками.
- Условия победы (все точки собраны — `YOU WIN`) и поражения (кот коснулся мыши — `GAME OVER`), пауза 2 секунды и рестарт.
- Временные визуальные заглушки: мышь — серый круг, коты — оранжевые круги, стены — синие прямоугольники.
- Desktop-модуль (LWJGL3): окно 1280×720, запуск через `.\gradlew lwjgl3:run`.
- Android-модуль: AndroidLauncher, minSdk 24, targetSdk 35.

### Известные ограничения
- Спрайты не реализованы, вместо них — геометрические заглушки.
- Тач-управление для Android не реализовано (план — свайпы).
- Android-сборка не проверялась на устройстве/эмуляторе.
- Предупреждение Kotlin Gradle plugin при сборке («плагин загружен несколько раз») — безобидно, не исправляется.