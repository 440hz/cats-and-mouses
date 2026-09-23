#!/usr/bin/env node
/**
 * opencode-balance-mcp (local fork для CATS AND MOUSES)
 *
 * MCP-сервер (stdio) для баланса OpenCode через КОНСОЛЬНОЕ API.
 * Заменяет npm-пакет `opencode-balance-mcp`, который сломался после того,
 * как opencode.ai сменил маршруты/API (был `/workspace/<id>/go` + SSR,
 * стало `/console/api/...`).
 *
 * Инструменты:
 *   - query_go_usage    → квота OpenCode Go (rolling 5h / week / month)
 *   - query_zen_balance → баланс OpenCode Zen (pay-as-you-go)
 *
 * Учётные данные (любой источник, приоритет — CLI > env):
 *   CLI:  --session st_...   --org-id wrk_...
 *   env:  OPENCODE_CONSOLE_SESSION / OPENCODE_ORG_ID
 *   (устаревшие fallback: OPENCODE_GO_AUTH_COOKIE / OPENCODE_GO_WORKSPACE_ID)
 *
 * Cookie передаётся как `__Host-console_session=<session>`, плюс заголовок
 * `x-org-id: <org>`. Значения хранятся в `opencode.json` (gitignored).
 */
import { createInterface } from "node:readline";

const BASE_URL = "https://opencode.ai";
const GO_PATH = "/console/api/go/status";
const BILLING_PATH = "/console/api/billing/status";
const AUTO_RECHARGE_PATH = "/console/api/billing/auto-recharge";
const MICROCENTS_PER_USD = 100_000_000;

/* ---------------- CLI-аргументы ---------------- */

function parseArgs(argv) {
    const args = { session: null, orgId: null };
    for (let i = 0; i < argv.length; i++) {
        const a = argv[i];
        if (a === "--session") args.session = argv[++i];
        else if (a === "--org-id") args.orgId = argv[++i];
        else if (a.startsWith("--session=")) args.session = a.slice("--session=".length);
        else if (a.startsWith("--org-id=")) args.orgId = a.slice("--org-id=".length);
    }
    return args;
}

/** Разобрать значение cookie: убрать возможный префикс `__Host-console_session=`/`auth=`. */
function stripCookiePrefix(value) {
    const eq = value.indexOf("=");
    if (eq > 0 && !value.slice(0, eq).includes(" ")) return value.slice(eq + 1);
    return value;
}

/* ---------------- Учётные данные (читаются один раз) ---------------- */

let cachedCredentials = null;

function loadCredentials() {
    if (cachedCredentials) return cachedCredentials;
    const cli = parseArgs(process.argv.slice(2));
    const session =
        cli.session ||
        process.env.OPENCODE_CONSOLE_SESSION ||
        process.env.OPENCODE_GO_AUTH_COOKIE;
    const orgId =
        cli.orgId ||
        process.env.OPENCODE_ORG_ID ||
        process.env.OPENCODE_GO_WORKSPACE_ID;
    if (!session || !orgId) {
        throw new Error(
            "Нет учётных данных: задай OPENCODE_CONSOLE_SESSION и OPENCODE_ORG_ID " +
                "(или аргументы --session / --org-id).",
        );
    }
    cachedCredentials = { session: stripCookiePrefix(session.trim()), orgId: orgId.trim() };
    return cachedCredentials;
}

/* ---------------- Запросы ---------------- */

async function fetchJson(path) {
    const { session, orgId } = loadCredentials();
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 15000);
    try {
        const resp = await fetch(`${BASE_URL}${path}`, {
            headers: {
                Cookie: `__Host-console_session=${session}`,
                "x-org-id": orgId,
                Accept: "application/json",
                "User-Agent": "opencode-balance-mcp",
            },
            signal: controller.signal,
        });
        if (resp.status === 401 || resp.status === 403) {
            throw new Error(
                `Авторизация не прошла (HTTP ${resp.status}): сессия '${path}' устарела. ` +
                    "Перелогинься на opencode.ai и обнови OPENCODE_CONSOLE_SESSION (cookie `__Host-console_session`).",
            );
        }
        if (!resp.ok) throw new Error(`Запрос не удался (HTTP ${resp.status}) для ${path}`);
        return await resp.json();
    } catch (err) {
        if (err.name === "AbortError") throw new Error("Таймаут запроса (15s)");
        throw err;
    } finally {
        clearTimeout(timer);
    }
}

/* ---------------- Форматирование ---------------- */

function microcentsToUsd(value) {
    const n = Number(value ?? 0);
    return Number.isFinite(n) ? n / MICROCENTS_PER_USD : 0;
}

function round2(n) {
    return Math.round(n * 100) / 100;
}

function formatWindow(label, meter, limitMicroCentsFallback) {
    const limitMicroCents = meter?.limitMicroCents ?? limitMicroCentsFallback;
    const usedMicroCents = meter?.usedMicroCents ?? "0";
    const limitUsd = microcentsToUsd(limitMicroCents);
    const spentUsd = microcentsToUsd(usedMicroCents);
    const usagePercent = limitUsd > 0 ? round2((spentUsd / limitUsd) * 100) : 0;
    return {
        window: label,
        limitUsd: round2(limitUsd),
        spentUsd: round2(spentUsd),
        usagePercent,
        resetsAt: meter?.resetsAt ?? null,
    };
}

async function fetchGoUsage() {
    const creds = loadCredentials();
    const data = await fetchJson(GO_PATH);
    const meters = data?.access?.meters ?? {};
    return {
        timestamp: new Date().toISOString(),
        orgId: creds.orgId,
        plan: {
            accessStartsAt: data?.access?.startsAt ?? null,
            accessEndsAt: data?.access?.endsAt ?? null,
            cancelAtPeriodEnd: data?.cancelAtPeriodEnd ?? null,
        },
        rolling: formatWindow("fiveHour", meters.fiveHour, 1_200_000_000),
        weekly: formatWindow("week", meters.week, 3_000_000_000),
        monthly: formatWindow("month", meters.month, 6_000_000_000),
    };
}

async function fetchZenBalance() {
    const creds = loadCredentials();
    const [data, autoReload] = await Promise.all([
        fetchJson(BILLING_PATH),
        fetchJson(AUTO_RECHARGE_PATH).catch(() => null),
    ]);
    const balanceUsd = microcentsToUsd(data?.balanceMicroCents);
    return {
        timestamp: new Date().toISOString(),
        orgId: creds.orgId,
        billingMode: data?.billingMode ?? null,
        mode: data?.mode ?? null,
        balanceMicroCents: data?.balanceMicroCents ?? null,
        balanceUsd: round2(balanceUsd),
        balanceFormatted: `$${balanceUsd.toFixed(2)}`,
        autoReload: {
            enabled: autoReload?.enabled ?? null,
            triggerUsd: autoReload?.thresholdDollars ?? null,
            reloadUsd: autoReload?.rechargeAmountDollars ?? null,
        },
    };
}

/* ---------------- MCP stdio ---------------- */

const TOOL_GO = "query_go_usage";
const TOOL_ZEN = "query_zen_balance";

const tools = [
    {
        name: TOOL_GO,
        description:
            "Квота OpenCode Go: расход по 5-часовому (rolling), недельному и месячному окнам (в USD и %), лимиты и время сброса.",
        inputSchema: { type: "object", properties: {} },
    },
    {
        name: TOOL_ZEN,
        description:
            "Баланс OpenCode Zen (pay-as-you-go) в USD и настройки автопополнения.",
        inputSchema: { type: "object", properties: {} },
    },
];

const handlers = { [TOOL_GO]: fetchGoUsage, [TOOL_ZEN]: fetchZenBalance };

function send(msg) {
    process.stdout.write(JSON.stringify(msg) + "\n");
}

const rl = createInterface({ input: process.stdin, crlfDelay: Infinity });

rl.on("line", async (line) => {
    if (!line.trim()) return;
    let msg;
    try {
        msg = JSON.parse(line);
    } catch {
        return;
    }
    const { id, method, params } = msg;
    if (method === "notifications/initialized" || method === "notifications/cancelled") return;
    if (id === undefined) return;
    try {
        switch (method) {
            case "initialize":
                send({
                    jsonrpc: "2.0",
                    id,
                    result: {
                        protocolVersion: "2024-11-05",
                        capabilities: { tools: {} },
                        serverInfo: { name: "opencode-balance-mcp", version: "2.0.0-local" },
                    },
                });
                break;
            case "tools/list":
                send({ jsonrpc: "2.0", id, result: { tools } });
                break;
            case "tools/call": {
                const handler = handlers[params?.name];
                if (!handler) {
                    send({ jsonrpc: "2.0", id, error: { code: -32602, message: `Unknown tool: ${params?.name}` } });
                    break;
                }
                try {
                    const data = await handler();
                    send({
                        jsonrpc: "2.0",
                        id,
                        result: { content: [{ type: "text", text: JSON.stringify(data, null, 2) }], isError: false },
                    });
                } catch (err) {
                    send({
                        jsonrpc: "2.0",
                        id,
                        result: { content: [{ type: "text", text: `Error: ${err.message}` }], isError: true },
                    });
                }
                break;
            }
            default:
                send({ jsonrpc: "2.0", id, error: { code: -32601, message: `Unknown method: ${method}` } });
        }
    } catch (err) {
        send({ jsonrpc: "2.0", id, error: { code: -32603, message: err.message } });
    }
});

rl.on("close", () => process.exit(0));
