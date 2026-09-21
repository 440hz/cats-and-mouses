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

## Где хранятся ключи балансов

Все ключи — в **`opencode.json`** в корне проекта (gitignored, не коммитится):

| Сервис | Путь в `opencode.json` |
|--------|------------------------|
| SpriteCook | `mcp.spritecook.headers.Authorization` (`Bearer sc_live_…`) |
| OpenCode Go/Zen | `mcp.opencode-balance.environment` (`OPENCODE_GO_WORKSPACE_ID`, `OPENCODE_GO_AUTH_COOKIE`) |
| DeepSeek | `mcp.opencode-balance.environment.DEEPSEEK_API_KEY` |

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
| **OpenCode Go** | лимит $`limitUsd` (мес.) · израсходовано ~$`estSpentUsd` (`usagePercent`% мес.) | активен / процент месячной квоты |
| **OpenCode Zen** | `balanceFormatted` | «требуется пополнение» если отрицательный, иначе «ок» |
| **SpriteCook** | **`total_credits`** кредитов | `subscription_status` (`tier`) |
| **DeepSeek** | **$`total_balance`** | доступен, если `is_available` |

Сумма — денежная величина или кредиты; для OpenCode Go (подписка) — месячный лимит и процент. Статус — короткая словесная оценка, не сырое поле.

### OpenCode Go

| Период | Использовано | Лимит | Сброс |
|---|---|---|---|
| Rolling (5h) | `usagePercent`% (~$low–high) | $`limitUsd` | `resetsIn` |
| Weekly | … | … | … |
| Monthly | … | … | … |

### OpenCode Zen (pay-as-you-go)

| Поле | Значение |
|---|---|
| Баланс | `balanceFormatted` |
| Автопополнение | порог $`triggerUsd`, сумма $`reloadUsd` |

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
