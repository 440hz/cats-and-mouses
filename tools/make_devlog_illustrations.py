"""Иллюстрации к девлогу blog/ai-devlog (dev-only, вне игры/APK).

Рисует схемы и графики локально (Pillow, единый стиль) в blog/ai-devlog/images/.
Запуск: python tools/make_devlog_illustrations.py
"""

import os
from PIL import Image, ImageDraw, ImageFont

OUT = "blog/ai-devlog/images"
FONT = "assets/fonts/ptsans.ttf"

BG = (244, 247, 243)
INK = (26, 33, 28)
MUTED = (108, 118, 110)
ACCENT = (78, 138, 91)
ACCENT_D = (54, 104, 66)
AMBER = (214, 158, 46)
CARD = (255, 255, 255)
BORDER = (208, 217, 208)
SHADOW = (226, 232, 226)

W, H = 1600, 900


def font(size):
    return ImageFont.truetype(FONT, size)


def canvas():
    img = Image.new("RGB", (W, H), BG)
    return img, ImageDraw.Draw(img)


def tw(d, text, f):
    return d.textbbox((0, 0), text, font=f)[2]


def center(d, cx, cy, text, f, fill=INK):
    w = tw(d, text, f)
    d.text((cx - w / 2, cy - f.size / 2 - 2), text, font=f, fill=fill)


def card(d, x, y, w, h, r=22, fill=CARD, outline=BORDER, width=3, shadow=True):
    if shadow:
        d.rounded_rectangle([x + 4, y + 6, x + w + 4, y + h + 6], r, fill=SHADOW)
    d.rounded_rectangle([x, y, x + w, y + h], r, fill=fill, outline=outline, width=width)


def title(d, main, sub=None):
    f = font(46)
    d.text((70, 58), main, font=f, fill=INK)
    if sub:
        d.text((72, 120), sub, font=font(26), fill=MUTED)


def arrow(d, x1, y1, x2, y2, color=ACCENT, w=6, head=18):
    d.line([x1, y1, x2, y2], fill=color, width=w)
    import math
    a = math.atan2(y2 - y1, x2 - x1)
    for s in (0.5, -0.5):
        d.line([x2, y2, x2 - head * math.cos(a + s), y2 - head * math.sin(a + s)], fill=color, width=w)


# 1. Память проекта
def scheme_memory():
    img, d = canvas()
    title(d, "Память проекта", "Правило: сначала правишь документ — потом код")
    docs = [
        ("GAME.md", "что это за игра"),
        ("PLAN.md", "что делаем и что дальше"),
        ("MENU.md", "какой экран куда ведёт"),
        ("CHANGELOG.md", "что менялось от версии к версии"),
        ("MUSIC.md", "где какой звук"),
    ]
    x0, y0, cw, ch, gap = 90, 210, 560, 96, 26
    for i, (name, desc) in enumerate(docs):
        y = y0 + i * (ch + gap)
        card(d, x0, y, cw, ch)
        d.text((x0 + 28, y + 22), name, font=font(30), fill=ACCENT_D)
        d.text((x0 + 28, y + 58), desc, font=font(22), fill=MUTED)
        arrow(d, x0 + cw + 6, y + ch / 2, 1010, y + ch / 2, ACCENT, 4, 14)
    card(d, 1040, 300, 470, 230, fill=(238, 246, 239))
    center(d, 1275, 380, "AI-агент", font(46), ACCENT_D)
    center(d, 1275, 440, "(DeepSeek 4.1 Flash)", font(20), MUTED)
    center(d, 1275, 490, "читает и пишет документы", font(22), INK)
    d.text((70, 820), "Документы — внешняя память: агент «вспоминает» через файлы, а не через контекст.",
           font=font(24), fill=MUTED)
    img.save(f"{OUT}/scheme_memory.png")


# 2. Конвейер фичи
def scheme_pipeline():
    img, d = canvas()
    title(d, "Путь одной фичи", "От просьбы словами — до готового результата")
    steps = [
        ("1. Постановка", "«хочу так-то»"),
        ("2. Документы", "план, меню, журнал"),
        ("3. Код + тесты", "≈10 мин кода, 5 мин тестов"),
        ("4. Приёмка", "запускаю и придираюсь"),
        ("5. Коммит", "«годится»"),
    ]
    n = len(steps)
    cw, gap = 250, 70
    total = n * cw + (n - 1) * gap
    x = (W - total) / 2
    y = 360
    for i, (t, s) in enumerate(steps):
        card(d, x, y, cw, 200)
        center(d, x + cw / 2, y + 62, t, font(26), ACCENT_D)
        # wrap sub
        words = s.split()
        line = ""
        lines = []
        for w in words:
            if tw(d, (line + " " + w).strip(), font(20)) > cw - 36:
                lines.append(line)
                line = w
            else:
                line = (line + " " + w).strip()
        lines.append(line)
        for j, ln in enumerate(lines):
            center(d, x + cw / 2, y + 118 + j * 28, ln, font(20), MUTED)
        if i < n - 1:
            arrow(d, x + cw + 4, y + 100, x + cw + gap - 6, y + 100, AMBER, 6, 16)
        x += cw + gap
    d.text((70, 820), "Каждый шаг фиксируется в документах — иначе агент «плывёт» и теряет контекст.",
           font=font(24), fill=MUTED)
    img.save(f"{OUT}/scheme_pipeline.png")


# 3. Три инструмента (MCP)
def scheme_mcp():
    img, d = canvas()
    title(d, "Инструменты наружу (MCP)", "Три «руки», которыми агент дотягивается до мира")
    items = [
        ("Картинки", "SpriteCook:\ngemini-3-pro-image,\ngemini-3.1-flash-image,\ngpt-image-2.5-flare"),
        ("Балансы", "OpenCode Go/Zen,\nSpriteCook,\nDeepSeek"),
        ("Управление игрой", "свой MCP-сервер:\nдвигать мышь, пауза,\nрестарт, сценарии"),
    ]
    cw, ch, cy = 380, 230, 210
    xs = [90, 610, 1130]
    for x, (t, s) in zip(xs, items):
        card(d, x, cy, cw, ch)
        center(d, x + cw / 2, cy + 48, t, font(30), ACCENT_D)
        for j, ln in enumerate(s.split("\n")):
            center(d, x + cw / 2, cy + 105 + j * 30, ln, font(21), MUTED)
    bx, bw_, bh_, by = 590, 420, 190, 580
    card(d, bx, by, bw_, bh_, fill=(238, 246, 239))
    center(d, bx + bw_ / 2, by + 68, "AI-агент", font(44), ACCENT_D)
    center(d, bx + bw_ / 2, by + 122, "OpenCode + модель", font(20), MUTED)
    targets = [bx + 110, bx + bw_ / 2, bx + bw_ - 110]
    for x, tx in zip(xs, targets):
        arrow(d, x + cw / 2, cy + ch + 4, tx, by - 6, ACCENT, 5, 16)
    d.text((70, 830), "Всё, что тратит деньги или публикует наружу, — только с явного «да» человека.",
           font=font(24), fill=MUTED)
    img.save(f"{OUT}/scheme_mcp.png")


# 4. План vs код
def scheme_plan_vs_code():
    img, d = canvas()
    title(d, "Куда уходит время", "Планирование — часы, генерация кода — минуты")
    # big bars
    bar = [
        ("Проработка плана", "несколько часов", 700, ACCENT),
        ("Генерация кода", "≈10 минут", 120, AMBER),
        ("Тесты", "≈5 минут", 70, ACCENT_D),
    ]
    y = 300
    label_w = 420
    for t, v, w, c in bar:
        d.text((90, y + 6), t, font=font(30), fill=INK)
        d.rounded_rectangle([label_w + 90, y, label_w + 90 + w, y + 60], 12, fill=c)
        d.text((label_w + 90 + w + 20, y + 8), v, font=font(28), fill=c)
        y += 130
    d.text((90, 780), "Хорошее ТЗ — это когда код почти пишется сам. Плохое — когда наматываешь круги.",
           font=font(24), fill=MUTED)
    img.save(f"{OUT}/scheme_plan_vs_code.png")


# 5. Эволюция
def scheme_growth():
    img, d = canvas()
    title(d, "Как росла игра", "Каждая версия — рабочая")
    steps = [
        ("Старт", "мышка бегает\nпо лабиринту\n(стены: 2 тайла)"),
        ("+ кот", "один кот\nгоняется"),
        ("3 кота", "разные\nстратегии"),
        ("4 кота", "погоня\nи засада"),
        ("Сердечки", "жизни\n(идея дочки)"),
    ]
    n = len(steps)
    cw, gap = 270, 45
    total = n * cw + (n - 1) * gap
    x = (W - total) / 2
    y = 320
    for i, (t, s) in enumerate(steps):
        c = ACCENT if i < 4 else AMBER
        card(d, x, y, cw, 230, outline=c, width=4)
        center(d, x + cw / 2, y + 58, t, font(34), c)
        for j, ln in enumerate(s.split("\n")):
            center(d, x + cw / 2, y + 120 + j * 34, ln, font(23), MUTED)
        if i < n - 1:
            arrow(d, x + cw + 4, y + 115, x + cw + gap - 6, y + 115, ACCENT, 6, 16)
        x += cw + gap
    d.text((70, 820), "Снежный ком: не «полгода, потом покажем», а по чуть-чуть, но живое.",
           font=font(24), fill=MUTED)
    img.save(f"{OUT}/scheme_growth.png")


# 6. Время по неделям
def chart_time():
    img, d = canvas()
    title(d, "Время: ~5 недель, ≈75–95 часов", "В основном по вечерам, выходные — чуть больше")
    data = [("Н1", 28), ("Н2", 18), ("Н3", 11), ("Н4", 11), ("Н5", 8)]
    base_y, max_h = 720, 460
    bw, gap = 150, 90
    total = len(data) * bw + (len(data) - 1) * gap
    x = (W - total) / 2
    mx = max(v for _, v in data)
    for lab, v in data:
        h = int(max_h * v / mx)
        d.rounded_rectangle([x, base_y - h, x + bw, base_y], 10, fill=ACCENT)
        center(d, x + bw / 2, base_y - h - 34, f"~{v} ч", font(28), ACCENT_D)
        center(d, x + bw / 2, base_y + 34, lab, font(26), MUTED)
        x += bw + gap
    d.line([70, base_y, W - 70, base_y], fill=BORDER, width=3)
    d.text((70, 810), "Н1 — 3–5 ч/день, Н2 — 2–3 ч/день, дальше — 1–2 ч/день.", font=font(24), fill=MUTED)
    img.save(f"{OUT}/chart_time.png")


# 7. Деньги
def chart_money():
    img, d = canvas()
    title(d, "Деньги", "Все счета — с нуля")
    rows = [
        ("Модели ИИ", "≈ $0 потрачено", ACCENT),
        ("DeepSeek — остаток", "$64.49", MUTED),
        ("OpenCode Zen — остаток", "$6.69", MUTED),
        ("SpriteCook — остаток", "942 кредита", AMBER),
        ("Основной расход", "кредиты на картинки", ACCENT_D),
    ]
    y = 230
    for t, v, c in rows:
        card(d, 120, y, W - 240, 96)
        d.text((160, y + 28), t, font=font(30), fill=INK)
        rw = tw(d, v, font(30))
        d.text((W - 160 - rw, y + 28), v, font=font(30), fill=c)
        y += 116
    d.text((70, 840), "Биллинг на 2026-10-16; точную сумму кредитов сверить перед публикацией.",
           font=font(22), fill=MUTED)
    img.save(f"{OUT}/chart_money.png")


# 8. Масштаб
def chart_scale():
    img, d = canvas()
    title(d, "Масштаб проекта", "Цифры вместо обещаний")
    cells = [
        ("112", "коммитов"), ("75", "версий"), ("~250", "автотестов"),
        ("41", "уровень"), ("9", "тем"), ("0", "строк кода\nот меня"),
    ]
    cw, ch, gap = 420, 240, 40
    cols = 3
    total = cols * cw + (cols - 1) * gap
    x0 = (W - total) / 2
    y0 = 250
    for i, (num, lab) in enumerate(cells):
        r, c = divmod(i, cols)
        x = x0 + c * (cw + gap)
        y = y0 + r * (ch + gap)
        c_ = AMBER if lab.startswith("строк") else ACCENT_D
        card(d, x, y, cw, ch)
        center(d, x + cw / 2, y + 100, num, font(72), c_)
        for j, ln in enumerate(lab.split("\n")):
            center(d, x + cw / 2, y + 175 + j * 30, ln, font(24), MUTED)
    d.text((70, 830), "8 миров + финал, 82 файла / ~6 300 строк, ~5 недель по вечерам.",
           font=font(22), fill=MUTED)
    img.save(f"{OUT}/chart_scale.png")


def main():
    os.makedirs(OUT, exist_ok=True)
    scheme_memory()
    scheme_pipeline()
    scheme_mcp()
    scheme_plan_vs_code()
    scheme_growth()
    chart_time()
    chart_money()
    chart_scale()
    print("OK:", OUT)


if __name__ == "__main__":
    main()
