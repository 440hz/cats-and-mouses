# Аудио-ассеты — источники и лицензии

Все аудио в игре — **CC0** (public domain): можно использовать и распространять без атрибуции.
Формат игровых файлов — **OGG Vorbis**. Мастера-источники лежат в `raw/` (хранятся в git,
в игру/APK не попадают — см. исключения `**/raw/**`).

## Музыка (`music/`)

| Файл | Источник | Произведение | Лицензия |
|---|---|---|---|
| `menu.ogg` | Juhani Junkala — Retro Game Music Pack | «Ending» (заставка + меню) | CC0 |
| `gameplay.ogg` | Juhani Junkala — Retro Game Music Pack | «Level 1» | CC0 |

- Juhani Junkala, «5 Chiptunes (Action)»: https://opengameart.org/content/5-chiptunes-action
  (в архиве — WAV; сконвертированы в OGG Vorbis потоково через `soundfile`/libsndfile).
- Kenney, «Music Jingles»: https://kenney.nl/assets/music-jingles
- Заставка и меню используют один трек `menu.ogg`: он стартует на экране загрузки и без разрыва
  продолжается в меню (и в «Настройках»).

## Звуковые эффекты (`sfx/`)

| Файл | Источник | Оригинал | Лицензия |
|---|---|---|---|
| `click.ogg` | Kenney — Interface Sounds | `click_001` | CC0 |
| `tick.ogg` | Kenney — Interface Sounds | `tick_001` | CC0 |
| `cheese.ogg` | Kenney — Music Jingles | `jingles_PIZZI04` (Pizzicato) | CC0 |
| `heart.ogg` | Kenney — Music Jingles | `jingles_PIZZI02` (Pizzicato) | CC0 |
| `life_lost.ogg` | Kenney — Digital Audio | `phaserDown1` | CC0 |
| `win.ogg` | Kenney — Music Jingles | `jingles_PIZZI10` (Pizzicato) | CC0 |
| `game_over.ogg` | Kenney — Music Jingles | `jingles_PIZZI11` (Pizzicato) | CC0 |

- Kenney, «Interface Sounds»: https://kenney.nl/assets/interface-sounds
- Kenney, «Digital Audio»: https://kenney.nl/assets/digital-audio

## Замечания

- `win.ogg` / `game_over.ogg` — короткие пиццикато-джинглы (`jingles_PIZZI10` / `jingles_PIZZI11`).
- `cheese.ogg` — пиццикато-джингл `jingles_PIZZI04` (0.56 с) на поедание сыра.
- `heart.ogg` — пиццикато-джингл `jingles_PIZZI02` (0.96 с) на подбор сердечка.
- `menu.ogg` — «Ending»: играется лупом на заставке, в меню и «Настройках».
- Громкости музыки/звуков задаются в игре (`ProgressStore`, экран «Настройки»), не в файлах.
