---
name: check-balances
description: Проверка балансов всех сервисов проекта CATS AND MOUSES (OpenCode Go/Zen, SpriteCook, DeepSeek) с выводом в таблицах. Используй, когда пользователь просит «проверить балансы», «проверить баланс», «сколько кредитов/денег осталось», «баланс deepseek / opencode / spritecook».
---

# Проверка балансов (CATS AND MOUSES)

По просьбе «проверить балансы» (или «баланс X») собери все источники **параллельно** и выведи результат **таблицами** (сначала сводная «Провайдер · Сумма · Статус», потом детали по сервисам), кратко, без лишнего текста.

## Источники

| Сервис | Инструмент / команда |
|--------|----------------------|
| OpenCode Go | инструмент `opencode-balance_query_go_usage` |
| OpenCode Zen | инструмент `opencode-balance_query_zen_balance` |
| SpriteCook | инструмент `spritecook_get_credit_balance` |
| DeepSeek | REST-запрос (см. ниже), ключ из `opencode.json` → `mcp.opencode-balance.environment` |

Инструменты `opencode-balance_*` даёт **локальный MCP** `tools/opencode-balance-mcp/index.mjs` (Node, без зависимостей). Он обращается к консольному API opencode.ai и заменяет npm-пакет `opencode-balance-mcp`, который сломался после смены маршрутов сайта.

## Где хранятся ключи балансов

Все ключи — в **`opencode.json`** в корне проекта (gitignored, не коммитится):

| Сервис | Путь в `opencode.json` |
|--------|------------------------|
| SpriteCook | `mcp.spritecook.headers.Authorization` (`Bearer sc_live_…`) |
| OpenCode Go/Zen | `mcp.opencode-balance.environment` (`OPENCODE_CONSOLE_SESSION`, `OPENCODE_ORG_ID`) |
| DeepSeek | `mcp.opencode-balance.environment.DEEPSEEK_API_KEY` |

Как MCP ходит в API opencode.ai:

- база `https://opencode.ai`;
- cookie `__Host-console_session=<OPENCODE_CONSOLE_SESSION>` и заголовок `x-org-id: <OPENCODE_ORG_ID>`;
- квота Go: `GET /console/api/go/status`;
- баланс Zen: `GET /console/api/billing/status` + `GET /console/api/billing/auto-recharge`.

### Если OpenCode-инструменты вернули ошибку авторизации

Сессия `__Host-console_session` живёт ~1 месяц. При `HTTP 401/403` (текст вида «Авторизация не прошла»):

1. Зайди на https://opencode.ai/console (через Google/GitHub).
2. DevTools → Application → Cookies → `https://opencode.ai` → скопируй значение cookie **`__Host-console_session`** (начинается с `st_`).
3. Обнови `OPENCODE_CONSOLE_SESSION` в `opencode.json` и перепроверь.

## DeepSeek

Ключ хранится в **`opencode.json`** (корень проекта, gitignored) — рядом с остальными ключами балансов: `mcp.opencode-balance.environment.DEEPSEEK_API_KEY`. Env-переменная `DEEPSEEK_API_KEY` — только fallback.

```powershell
$cfg = Get-Content ".\opencode.json" -Raw | ConvertFrom-Json
$k = $cfg.mcp.'opencode-balance'.environment.DEEPSEEK_API_KEY
if (-not $k) { $k = $env:DEEPSEEK_API_KEY }
Invoke-RestMethod -Uri "https://api.deepseek.com/user/balance" -Headers @{ Authorization = "Bearer $k" }
```

Эндпоинт: `GET https://api.deepseek.com/user/balance`. Ответ: `is_available` + `balance_infos[]` (`currency`, `total_balance`, `granted_balance`, `topped_up_balance`).

Если ключа нет — сообщи одной строкой, что `DEEPSEEK_API_KEY` не задан, и пропусти DeepSeek.

## SpriteCook и прокси

`get_credit_balance` — read-only, правило «Шаг 0: Проверка прокси» к нему не относится. Прокси спрашивай только перед **генерацией** (см. `AGENTS.md`).

## Формат вывода

Всегда начинай со **сводной таблицы** «Провайдер · Сумма · Статус», затем — детализация по каждому сервису отдельной таблицей.

### Сводная таблица (обязательно)

| Провайдер | Сумма | Статус |
|---|---:|---|
| **OpenCode Go** | лимит $`monthly.limitUsd` (мес.) · израсходовано ~$`monthly.spentUsd` (`monthly.usagePercent`% мес.) | активен / процент месячной квоты |
| **OpenCode Zen** | `balanceFormatted` | «требуется пополнение» если ≤ 0, иначе «ок» |
| **SpriteCook** | **`total_credits`** кредитов | `subscription_status` (`tier`) |
| **DeepSeek** | **$`total_balance`** | доступен, если `is_available` |

Сумма — денежная величина или кредиты; для OpenCode Go (подписка) — месячный лимит и процент. Статус — короткая словесная оценка, не сырое поле.

### OpenCode Go (`query_go_usage`)

| Период | Использовано | Лимит | Сброс |
|---|---:|---:|---|
| Rolling (5h) | $`rolling.spentUsd` (`rolling.usagePercent`%) | $`rolling.limitUsd` | `rolling.resetsAt` |
| Weekly | $`weekly.spentUsd` (`weekly.usagePercent`%) | $`weekly.limitUsd` | `weekly.resetsAt` |
| Monthly | $`monthly.spentUsd` (`monthly.usagePercent`%) | $`monthly.limitUsd` | `monthly.resetsAt` |

Дополнительно: срок доступа `plan.accessStartsAt`–`plan.accessEndsAt`, `plan.cancelAtPeriodEnd`. Пустой `resetsAt` = окно ещё не стартовало.

### OpenCode Zen (`query_zen_balance`)

| Поле | Значение |
|---|---|
| Баланс | `balanceFormatted` |
| Режим | `billingMode` / `mode` |
| Автопополнение | `autoReload.enabled` (порог $`autoReload.triggerUsd`, сумма $`autoReload.reloadUsd`) |

### SpriteCook

| Поле | Значение |
|---|---|
| Кредиты | `total_credits` (подписка `subscription_credits`, top-up `topup_credits`) |
| Уровень | `tier` |
| Статус подписки | `subscription_status` |
| Параллельных задач | `concurrent_jobs` |

### DeepSeek

| Поле | Значение |
|---|---|
| Баланс | `total_balance` `<currency>` |
| Пополнено | `topped_up_balance` |
| Бонусы | `granted_balance` |
| Статус | `is_available` |

## Правила

- Если пользователь просит баланс одного сервиса — выводи только его таблицу.
- Если просит «сводную таблицу» — выводи только сводную (без детализации по сервисам).
- Ничего не выдумывать: только значения из ответов инструментов / API.
- Не запрашивай подтверждений — проверка балансов безопасна и read-only.
