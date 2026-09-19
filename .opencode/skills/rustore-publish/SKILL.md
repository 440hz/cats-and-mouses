---
name: rustore-publish
description: Публикация и обновление приложения CATS AND MOUSES в RuStore — сборка и подпись релиза (.apk/.aab), ручная публикация через RuStore Консоль и автоматизация через RuStore Public API (tools/rustore-publish.ps1). Используй при словах rustore, RuStore, публикация, релиз, обновление версии, APK, AAB, keystore, подпись, модерация, черновик, отзывы, рейтинг, RuStore API, а также когда нужно проверить статус модерации или выпустить новую версию.
---

# Публикация в RuStore (CATS AND MOUSES)

Связанные навыки: **`rustore-aso`** (текст карточки, теги, категории), **`git-workflow`** (bump/коммит),
**`run-game`** (проверка сборки). Справочники: `reference/api.md`, `reference/ids.md`.

> **ВАЖНО.** Любые изменения в RuStore — только с явного одобрения пользователя.
> Без подтверждения разрешены лишь read-only команды: `token`, `apps`, `status`, `reviews`, `reviews-csv`, `rating`.
> Мутирующие действия (`release`, `publish`, `settings`, `draft-delete`, `archive`, `icon`,
> `screens -Screens/-DeleteId`, `reply`, Gradle `rustoreRelease`) — только после явного «да».

## 1. Параметры проекта

| Что | Значение |
|---|---|
| `applicationId` | `com.catsandmouses` |
| Версия | `Constants.VERSION` ↔ `android/build.gradle.kts` `versionName`; `versionCode` — целое, строго выше активного |
| Ключ подписи | `android/catsandmouses-release.jks`, alias `catsandmouses` |
| Параметры ключа | `keystore.properties` (gitignored) |
| minSdk / targetSdk | 24 / 35 (Android 7.0+) |
| Ориентация | landscape |
| Тип приложения | `GAMES` |

> Ключ подписи и его пароль — критичны. Потеря ключа = невозможность обновлять приложение.
> Бэкап обязателен. Подпись всех версий — **одним и тем же** ключом.

## 2. Артефакты и пути

| Артефакт | Путь |
|---|---|
| Подпись | `android/catsandmouses-release.jks`, `keystore.properties` (gitignored) |
| APK / AAB | `android/build/outputs/apk/release/android-release.apk`, `android/build/outputs/bundle/release/android-release.aab` |
| Иконка витрины | `store/rustore/icon_512.png` (512×512, непрозрачный фон) |
| Скриншоты | `store/rustore/screenshots/*.jpg` (1920×1080, 16:9; в git не хранятся) |
| Параметры версии | `store/rustore/version.json` |
| FAQ | `FAQ.md` (и блок `faq` в `version.json`) |
| API-ключ | `rustore-api.properties` (gitignored), шаблон — `store/rustore/rustore-api.properties.example` |
| Инструмент | `tools/rustore-publish.ps1` |

## 3. Выпуск новой версии

1. Поднять версию: `Constants.VERSION` (`core/.../Constants.kt`) **и** `versionCode`/`versionName` в `android/build.gradle.kts` (patch, напр. 1.0.50 → 1.0.51).
2. Обновить `whatsNew` в `store/rustore/version.json`.
3. Записать изменения в `GAME.md` (статус) и `PLAN.md` (история).
4. Собрать и проверить: `.\gradlew build` (BUILD SUCCESSFUL).
5. Релизные сборки:
   ```powershell
   .\gradlew android:assembleRelease    # APK
   .\gradlew android:bundleRelease      # AAB
   ```
6. Загрузить версию: через Консоль (см. §5) или API (см. §4).

## 4. Автоматизация через RuStore API

Все команды — из корня проекта, PowerShell 7.

```powershell
pwsh tools/rustore-publish.ps1 <action> [-Config store/rustore/version.json] [-KeyFile rustore-api.properties] [-File <apk>] [-VersionId <n>]
```

| Action | Что делает |
|---|---|
| `token` | проверка авторизации |
| `apps` | список приложений (appStatus, versionCode) |
| `status` | список версий приложения |
| `release` | черновик → загрузка файла → отправка на модерацию |
| `publish` | ручная публикация версии после модерации |
| `settings` | тип/дата/процент публикации (`-PublishType`, `-PublishDate`, `-PartialValue`) |
| `draft-delete` | удаление черновика (`-VersionId`) |
| `archive` | архивация версии (`-VersionId`) |
| `icon` | загрузка иконки (`-File`) |
| `screens` | список/загрузка/удаление скриншотов (`-Screens`, `-DeleteId`) |
| `reviews` | отзывы (`-Page`, `-Size`, `-CommentId`) |
| `reviews-csv` | отзывы в CSV (нужны `-From`, `-To`) |
| `reply` | ответ на отзыв (`-CommentId`, `-Message`) |
| `rating` | рейтинг приложения |

### Настройка ключа

1. Консоль → **Разработчик** (или **Компания**) → **API RuStore** → **Создать ключ**: выбрать приложение и методы.
2. Создать `rustore-api.properties` (gitignored) из шаблона:
   ```
   keyId=<ID ключа из Консоли>
   privateKey=<приватный ключ>
   ```
3. Проверить: `git check-ignore -v rustore-api.properties` и `pwsh tools/rustore-publish.ps1 token`.

### Ограничения API

- API работает только при **наличии хотя бы одной активной версии** приложения.
- Один черновик на приложение; черновик не редактируется (только удалить и создать заново).
- Токен живёт 900 c — скрипт получает его заново на каждый запуск.
- Для обновления иконка и скриншоты **наследуются** из активной версии — грузить их заново нужно только при изменении.
- `versionCode` должен быть выше активного; `categories` и `seoTagIds` — строго для `GAMES`.

## 5. Публикация через Консоль (вручную)

1. [RuStore Консоль](https://console.rustore.ru/sign-in) → вкладка **Приложения** → выбрать приложение → **Загрузить версию**.
2. Тип приложения — **Универсальный**, тип версии — **Игра**.
3. Загрузить файл: **APK** (проще — подпись уже внутри) или **AAB** (нужно отдельно загрузить подпись приложения — ZIP от `pepk.jar` по команде Консоли + PEM-сертификат).
4. Заполнить поля (тексты — из `rustore-aso`/`version.json`):

| Поле | Лимит |
|---|---|
| Название | ≤30 симв., совпадает с названием на устройстве (`strings.xml`) |
| Краткое описание | ≤80 |
| Полное описание | ≤4000 |
| Поисковые теги | ≤5 |
| FAQ | ≤10 (вопрос ≤120, ответ ≤500) |

5. Категории: **Аркады** + **Казуальные**. Возраст: **0+**. Маркировка контента — без ограничений.
6. Медиа: иконка `store/rustore/icon_512.png`; скриншоты ≥3 (16:9, из `store/rustore/screenshots/`).
7. Контакты разработчика — минимум одно поле (email / VK / сайт / MAX).
8. Режим публикации: **ручной** (для первой публикации — контроль момента выхода).
9. **Отправить на модерацию**. После одобрения — **Опубликовать** (или `pwsh tools/rustore-publish.ps1 publish`).

## 6. Чек-лист перед отправкой

- [ ] `.\gradlew build` — BUILD SUCCESSFUL.
- [ ] `versionCode` выше активного; `versionName` совпадает с `Constants.VERSION`.
- [ ] Подпись — тем же ключом `android/catsandmouses-release.jks`.
- [ ] APK/AAB запускается, играется, без крашей; все заявленные функции работают.
- [ ] Название и иконка на витрине совпадают с приложением на устройстве.
- [ ] Скриншоты актуальны, отражают реальный геймплей, без чужих интерфейсов.
- [ ] Нет запрещённых разрешений (`uses-permission` в манифесте отсутствуют).
- [ ] Тексты на русском, без кликбейта («лучший», «самый», «официальный» без подтверждения).
- [ ] Обновлены `GAME.md` и `PLAN.md`.

## 7. Отзывы и рейтинг

Отвечать на отзывы — влияет на рейтинг и, косвенно, на позицию в поиске (ASO).

```powershell
pwsh tools/rustore-publish.ps1 reviews
pwsh tools/rustore-publish.ps1 reply -CommentId 123456 -Message "Спасибо за отзыв!"
pwsh tools/rustore-publish.ps1 rating
```

## 8. Ограничения и грабли

- API-ключ привязан к **компании**; при утечке — заменить в Консоли (**Заменить ключ**).
- `rustore-api.properties` и `*.jks`/`*.keystore` — **никогда** не коммитить (уже в `.gitignore`).
- Один черновик; загруженный файл виден в Консоли только после отправки на модерацию.
- Модерация может вернуть версию на доработку — исправить и отправить заново (нужен новый `versionCode`).
- Скриншоты: минимум 3, 16:9; первый кадр задаёт ориентацию.
- Публикация платной версии для бесплатного приложения обрабатывается как бесплатная.

## 9. Проверка инструмента

```powershell
pwsh tools/rustore-publish.ps1 -Help
pwsh tools/rustore-publish.ps1 token
pwsh tools/rustore-publish.ps1 apps
pwsh tools/rustore-publish.ps1 status
```
