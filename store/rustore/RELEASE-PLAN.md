# План: публикация CATS AND MOUSES в RuStore

Составлен 2026-09-30. **Обновлено 2026-10-12:** первой версией публикуется **1.2.4**
(фиксы языка Android и кнопки «Выход»); `versionCode 102004`.
**Статус: 1.2.4 загружена и отправлена на модерацию (versionId `2064877739`), 1.0.49 архивирована.**
Осталось: после одобрения — **опубликовать**.
Связанные документы: `PLAN.md`, `GAME.md`, навык `.opencode/skills/rustore-publish/SKILL.md`.

## Текущее состояние

- Приложение `com.catsandmouses`, appStatus **NOT_PUBLISHED** (публикуется впервые).
- Версия **1.2.4** (versionCode 102004, versionId `2064877739`) — **`MODERATION`**, MANUAL (проверка до 3 дней).
- Версия **1.0.49** — **`ARCHIVED`**.
- Локально: `Constants.VERSION = 1.2.4`, android versionCode **102004** / versionName 1.2.4.
- Релизный APK: `android/build/outputs/apk/release/android-release.apk` (подписан `android/catsandmouses-release.jks`).
- Публикация — **вручную в Консоли RuStore**.

## Решения

1. Публикуем **только 1.2.4** (1.0.49 не публикуем → в архив).
2. Формат — **APK** (подпись внутри, обновления тем же ключом).
3. Публикация — **вручную в Консоли** (API не используем для первой версии).
4. Черновик/версия 1.0.49 → **Архивировать**.
5. Тексты — из `store/rustore/version.json`; FAQ — `store/rustore/FAQ.md`. «Что нового» — по 1.2.4.
6. Скриншоты — **новые, 9 кадров** 1920×1080 в `store/rustore/screenshots/`.
7. Локализация: движок игры RU/EN (авто по системе + переключатель); карточка RuStore — на русском.

## Артефакты

| Что | Значение |
|---|---|
| App / пакет | `com.catsandmouses` |
| Версия | **1.2.4** / versionCode **102004** |
| APK | `android/build/outputs/apk/release/android-release.apk` |
| Иконка | `store/rustore/icon_512.png` (512×512) |
| Скриншоты | `store/rustore/screenshots/*.jpg` — 9 шт., 1920×1080, 16:9 |
| Поля карточки | `store/rustore/version.json` (`draft`) |
| FAQ | `store/rustore/FAQ.md` (+ блок `faq` в `version.json`) |

## Скриншоты (порядок загрузки = порядок в витрине)

1. `01_gameplay_grass.jpg` — мир 1 (grass)
2. `02_gameplay_castle.jpg` — мир 2 (castle)
3. `03_gameplay_ice.jpg` — мир 3 (ice)
4. `04_gameplay_desert.jpg` — мир 4 (desert)
5. `05_gameplay_volcano.jpg` — мир 7 (volcano)
6. `06_gameplay_kitchen.jpg` — мир 8 (kitchen)
7. `07_menu.jpg` — главное меню
8. `08_settings.jpg` — «Настройки»
9. `09_loading.jpg` — заставка

Снимки — реальный Desktop (dev-флаги `--window 1920x1080 --shot ... --shot-screen ...`), без оверлея паузы и без отсчёта «3-2-1». Папка `screenshots/` вне git.

## Шаг 1. Тексты — `store/rustore/version.json`

- `whatsNew` — про **1.2.4** (актуально в файле).
- `fullDescription`/FAQ — 41 уровень / 8 миров (актуально).
- Категории `arcade` + `casual`, теги `51,52,58,61,21`, возраст `0+`, `publishType = MANUAL`.

## Шаг 2. APK

- `.\gradlew android:assembleRelease` (неблокирующе: `pwsh tools/build-bg.ps1 android:assembleRelease`).
- Проверить: versionCode **102004**, versionName **1.2.4**, подпись тем же ключом.

## Шаг 3. Действия в Консоли RuStore (вручную) — ВЫПОЛНЕНО 2026-10-12, версия на модерации

1. Версия **1.0.49** → **Архивировать**.
2. **Загрузить версию**: тип приложения **Универсальный**, тип версии **Игра** →
   `android/build/outputs/apk/release/android-release.apk`.
3. Заполнить поля из `store/rustore/version.json` (название, краткое/полное описание,
   поисковые теги, категории, возраст, «Что нового», FAQ из `store/rustore/FAQ.md`).
4. Медиа: `store/rustore/icon_512.png` + 9 скриншотов из `store/rustore/screenshots/`.
5. Контакты разработчика — минимум одно поле.
6. Режим публикации — **ручной** → **Отправить на модерацию**.
7. После одобрения — **Опубликовать**.

## Шаг 4. После публикации

- Обновить `GAME.md`, `PLAN.md`, `CHANGELOG.md`.
- Коммит/пуш — только по отдельному подтверждению (навык `git-workflow`).

## Ограничения

- Любые мутирующие действия в RuStore (`archive`, `draft-delete`, `release`, `publish`,
  `icon`, `screens`, `reply`, Gradle `rustoreRelease`) — **только после явного одобрения**
  (правило 10 `AGENTS.md`).
