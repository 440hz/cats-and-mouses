# Справочник: RuStore Public API

Хост: `https://public-api.rustore.ru`
Все защищённые методы принимают заголовок `Public-Token: <JWE>`.

## Авторизация

1. Ключ создаётся в RuStore Консоли: **Разработчик** (или **Компания**) → **API RuStore** → **Создать ключ**. Выбираются приложение и методы. Приватный ключ показывается один раз.
2. Обмен ключа на JWE-токен: `POST /public/auth/`, тело:

```json
{ "keyId": "<число>", "timestamp": "<ISO с зоной>", "signature": "<base64>" }
```

- `timestamp` — текущее время, формат `yyyy-MM-ddTHH:mm:ss.fffzzz`, расхождение с сервером не более 60 c.
- `signature` — `base64( RSA_PKCS1_SHA512( SHA-512( keyId + timestamp ) ) )`.
- Приватный ключ — PKCS8, отдаётся в base64; перед подписью декодируется из base64.

Ответ: `body.jwe` (сам токен) и `body.ttl` (срок жизни, 900 c). Токен многоразовый.

Ошибки авторизации: `Range timestamp not valid`, `Signature encode error`, `KeyId must not be null`.

## Методы (используются скриптом)

| Назначение | Метод | Путь |
|---|---|---|
| Авторизация | POST | `/public/auth/` |
| Список приложений | GET | `/public/v1/application` |
| Список/статус версий | GET | `/public/v1/application/{pkg}/version?ids=&versionStatuses=&page=&size=` |
| Создание черновика | POST | `/public/v1/application/{pkg}/version` |
| Изменение настроек публикации | POST | `/public/v1/application/{pkg}/version/{id}/publish-settings` |
| Удаление черновика | DELETE | `/public/v1/application/{pkg}/version/{id}` |
| Загрузка APK | POST | `/public/v1/application/{pkg}/version/{id}/apk?isMainApk=true&servicesType=Unknown` |
| Загрузка AAB | POST | `/public/v1/application/{pkg}/version/{id}/aab` |
| Загрузка иконки | POST | `/public/v1/application/{pkg}/version/{id}/image/icon` |
| Отправка на модерацию | POST | `/public/v1/application/{pkg}/version/{id}/commit?priorityUpdate=0` |
| Ручная публикация | POST | `/public/v1/application/{pkg}/version/{id}/publish` |
| Архивирование | POST | `/public/v1/application/{pkg}/version/{id}/archive` |
| Список скриншотов | GET | `/public/v2/application/{pkg}/version/{id}/image/screenshot?size=&page=` |
| Загрузка скриншотов | POST | `/public/v2/application/{pkg}/version/{id}/image/screenshot/{orientation}/{ordinal}/{type}` |
| Удаление скриншота | DELETE | `/public/v2/application/{pkg}/version/{id}/image/screenshot/{id}` |
| Отзывы | GET | `/public/v1/application/{pkg}/comment?id=&page=&size=` |
| Отзывы CSV | GET | `/public/v1/application/{pkg}/comment/export?from=&to=` |
| Ответ на отзыв | POST | `/public/v1/application/{pkg}/feedback?commentId=` |
| Рейтинг приложения | GET | `/public/v1/application/{pkg}/comment/statistic` |

Точные формы запросов/ответов и query-параметры — в официальной документации (ссылки ниже).

## Тело черновика (`POST .../version`)

Поля: `appName` (≤50), `appType` (`GAMES`/`MAIN`), `categories` (≤2), `ageLegal`, `shortDescription` (≤80), `fullDescription` (≤4000), `whatsNew` (≤5000), `moderInfo` (≤180), `priceValue`, `seoTagIds` (≤5), `publishType` (`MANUAL`/`INSTANTLY`/`DELAYED`), `publishDateTime`, `partialValue`, `faq` (≤10; вопрос ≤120, ответ ≤500), `developerContacts`.

Отсутствующие поля наследуются из активной версии. Ответ: `body` = `versionId`.

## Статусы версий

`DRAFT`, `AUTO_CHECK`, `AUTO_CHECK_FAILED`, `TAKEN_FOR_MODERATION`, `MODERATION`, `READY_FOR_PUBLICATION`, `PUBLISHED`, `ACTIVE`, `PARTIAL_ACTIVE`, `PREVIOUS_ACTIVE`, `ARCHIVED`, `REJECTED_BY_MODERATOR`, `REJECTED_BY_SECURITY`, `DELETED_DRAFT`, `NOT_PUBLISHED`.

## Ограничения

- API доступен только при **наличии хотя бы одной активной версии** приложения.
- Для приложения — **один черновик**; черновик нельзя редактировать, только удалить и создать заново.
- Токен живёт **900 c**, получается заново на каждый запуск.
- APK/AAB: ≤5 ГБ; `versionCode` должен быть выше активного; подпись — та же.
- Иконка: 512×512, PNG/JPEG, ≤3 МБ.
- Загруженный через API установочный файл появляется в веб-Консоли только после отправки на модерацию.
- Отзывы/рейтинг/доступы/платежи — отдельные группы методов (ключ должен иметь соответствующие права).

## Ссылки

- API RuStore (обзор): https://www.rustore.ru/help/work-with-rustore-api
- Авторизация: https://www.rustore.ru/help/work-with-rustore-api/api-authorization-process
- Токен: https://www.rustore.ru/help/work-with-rustore-api/api-authorization-token
- Загрузка/публикация: https://www.rustore.ru/help/work-with-rustore-api/api-upload-publication-app
- Отзывы: https://www.rustore.ru/help/work-with-rustore-api/api-feedback-process
- Категории: https://www.rustore.ru/help/work-with-rustore-api/api-upload-publication-app/app-category-list
- Теги: https://www.rustore.ru/help/work-with-rustore-api/api-upload-publication-app/app-tag-list
