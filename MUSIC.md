# MUSIC.md — звук и музыка

Описание всей звуковой подсистемы игры **CATS AND MOUSES**: музыка, SFX, громкости, источники ассетов,
где что играет и как заменять звуки. **Этот файл обновляется при каждой правке в музыку/звук.**

Связанные документы: [GAME.md](GAME.md), [MENU.md](MENU.md) (§3.7 «Настройки», §5 «Правила UI»),
[AGENTS.md](AGENTS.md), [CHANGELOG.md](CHANGELOG.md).

---

## 1. Архитектура

Единственная точка звука — **`AudioManager`** (`core/src/main/kotlin/com/catsandmouses/audio/AudioManager.kt`):

- музыка — по одному `Music` на трек (кэш по имени трека), SFX — кэш `Sound`;
- **все методы — безопасный no-op, если `Gdx.audio == null`** (headless-тесты, dev без звука);
- создаётся и `dispose()` в `MainGame` (`MainGame.audio`), доступен экранам через `mainGame.audio` / параметр;
- `MainGame.pause()` / `resume()` глушат/возобновляют музыку при сворачивании приложения (Android и Desktop);
- `playMusic(track, loop)` — **no-op, если этот же трек уже играет** (меню/настройки не перезапускают музыку);
- формат — **OGG Vorbis** (LibGDX декодирует нативно; `ffmpeg`/`oggenc` в проекте нет — конвертация WAV→OGG
  делается Python-скриптом через `soundfile`/libsndfile, потоково).

API: `playMusic`, `stopMusic`, `pauseMusic`, `resumeMusic`, `setDuck(bool)`,
`setMusicVolume(0…100)`, `setSfxVolume(0…100)`, `playSfx(name)`, `dispose`.

---

## 2. Музыка

| Трек (ключ) | Файл | Источник / лицензия | Где играет |
|---|---|---|---|
| `TRACK_MENU` = `"menu"` | `audio/music/menu.ogg` | Juhani Junkala — «Ending», CC0 (OGG, ~45 с) | **Заставка** (`LoadingScreen`) → **меню** (`MainMenuScreen`) → **Настройки** (`SettingsScreen`). Один непрерывный луп: стартует на заставке и не перезапускается при переходах |
| `TRACK_GAMEPLAY` = `"gameplay"` | `audio/music/gameplay.ogg` | Juhani Junkala — «Level 1», CC0 (OGG, ~74 с) | **Геймплей** (`GameScreen`, со старта уровня/рестарта/следующей карты). Луп |

Поведение:
- **Заставка** запускает музыку меню (ранее был отдельный короткий джингл — убран).
- **Пауза в игре** (overlay) — приглушение (ducking): `volume × MUSIC_DUCK`; снятие паузы возвращает громкость.
- **Победа/поражение** — `stopMusic()` + стингер (`win` / `game_over`).
- **Возврат в меню** — геймплейный трек останавливается, меню-трек возобновляется (`MainMenuScreen.show()`).
- **Сворачивание приложения** — `pauseMusic()` / `resumeMusic()`.

---

## 3. SFX

Все — короткие, играются через `playSfx(name)` (громкость `sfxVolume`; при 0 — no-op).

| Имя | Файл | Источник / лицензия | Когда звучит | Код |
|---|---|---|---|---|
| `cheese` | `audio/sfx/cheese.ogg` | Kenney «Music Jingles» `jingles_PIZZI04`, CC0 (0.56 с) | съеден сыр (счёт вырос) | `GameScreen.playGameplaySfx` |
| `heart` | `audio/sfx/heart.ogg` | Kenney «Music Jingles» `jingles_PIZZI02`, CC0 (0.96 с) | подобрано сердечко (+жизнь) | `GameScreen.playGameplaySfx` |
| `life_lost` | `audio/sfx/life_lost.ogg` | Kenney «Digital Audio» `phaserDown1`, CC0 (0.47 с) | потеря жизни и телепорт (`state == PLAYING`) | `GameScreen.playGameplaySfx` |
| `win` | `audio/sfx/win.ogg` | Kenney «Music Jingles» `jingles_PIZZI10`, CC0 (0.80 с) | `state → WIN` (стингер) | `GameScreen` (render, ветка `WIN`) |
| `game_over` | `audio/sfx/game_over.ogg` | Kenney «Music Jingles» `jingles_PIZZI11`, CC0 (0.79 с) | `state → GAME_OVER` (стингер) | `GameScreen` (render, ветка `GAME_OVER`) |
| `click` | `audio/sfx/click.ogg` | Kenney «Interface Sounds» `click_001`, CC0 (0.10 с) | кнопки и шестерёнка меню; «Назад» и шаг ползунка «Звуки» | `MainMenuScreen`, `SettingsScreen` |
| `tick` | `audio/sfx/tick.ogg` | Kenney «Interface Sounds» `tick_001`, CC0 (0.02 с) | старт отсчёта и смена 3→2→1 | `GameScreen` (`startNewGame` + дельта отсчёта) |

Механика: SFX в игре определяются **по дельтам кадра** (`GameScreen.playGameplaySfx` сравнивает
счёт/жизни/сердечки/номер отсчёта до и после `world.update`), поэтому `GameWorld` остаётся чистой (без `Gdx`).

---

## 4. Громкость и настройки

| Что | Где | Значение |
|---|---|---|
| Дефолтная громкость | `Constants.DEFAULT_VOLUME` | **80** (%) |
| Шаг ползунка | `Constants.VOLUME_STEP` | **10** |
| Приглушение на паузе | `Constants.MUSIC_DUCK` | **0.3** |
| Ключ «Музыка» | `Constants.KEY_MUSIC_VOLUME` = `"musicVolume"` | 0…100 |
| Ключ «Звуки» | `Constants.KEY_SFX_VOLUME` = `"sfxVolume"` | 0…100 |
| Чтение/запись | `ProgressStore.musicVolume()` / `sfxVolume()` / `setMusicVolume()` / `setSfxVolume()` | кламп 0…100, `flush()` |
| UI | экран **«Настройки»** (`SettingsScreen`, `MENU.md §3.7`), компонент `UiSlider` | ползунки «Музыка»/«Звуки», подпись с процентом |
| Подписи | `MenuDefinitions.LABEL_MUSIC` / `LABEL_SFX` / `volumeLabel(prefix, value)` | «Музыка 80%» |

`0` = тишина (SFX не играют; музыка слышима на нулевой громкости = беззвучна, но продолжает играть).
Настройки применяются и сохраняются сразу при движении ползунка.

---

## 5. Файлы и структура

```
assets/audio/
  music/  menu.ogg  gameplay.ogg
  sfx/    cheese.ogg heart.ogg life_lost.ogg win.ogg game_over.ogg click.ogg tick.ogg
  raw/    music/…  sfx/…        # мастера-источники; в git, НЕ входят в jar/APK (исключение **/raw/**)
  LICENSES.md                   # источники, произведения, индексы, лицензии (CC0)
music/                          # корень проекта: локальный архив скачанных паков (Juhani + Kenney),
                                #   распакованные + zip. В игру/APK НЕ входит, в assets не подключён.
```

- Игровые файлы — OGG Vorbis в `assets/audio/{music,sfx}/`; мастера дублируются в `assets/audio/raw/`.
- Каталоги `raw/` (как и спрайтовые) исключаются из jar (`lwjgl3/build.gradle.kts`) и APK
  (`android/build.gradle.kts`) существующим правилом `**/raw/**` — отдельных правок Gradle для звука не нужно.
- **`music/`** — локальный архив исходников (≈104 МБ), **не** часть игровых ассетов. В git не коммитить.

---

## 6. Источники (все CC0 — без атрибуции)

- **Juhani Junkala**, «5 Chiptunes (Action)» — https://opengameart.org/content/5-chiptunes-action
  (в архиве WAV; в игре: `menu.ogg` ← «Ending», `gameplay.ogg` ← «Level 1»).
- **Kenney**, «Music Jingles» — https://kenney.nl/assets/music-jingles (джинглы NES/PIZZI/SAX/STEEL/HIT).
- **Kenney**, «Interface Sounds» — https://kenney.nl/assets/interface-sounds.
- **Kenney**, «Digital Audio» — https://kenney.nl/assets/digital-audio.

Полная таблица «файл → источник → оригинал → лицензия» — в **`assets/audio/LICENSES.md`**.

---

## 7. Как заменить или добавить звук

1. Выбрать файл: в архиве `music/` (Kenney/Juhani) или скачать CC0.
2. Привести к OGG Vorbis; при необходимости обрезать (частые звуки — короче). Конвертация — Python `soundfile`
   (потоково, если файл длинный: одиночная запись большого WAV падала), либо любой внешний конвертер.
3. Положить игровой файл в `assets/audio/sfx/<имя>.ogg` (или `assets/audio/music/<трек>.ogg`),
   а мастер — в `assets/audio/raw/sfx/<имя>.ogg` (`raw/music/<трек>.wav|ogg`).
4. Обновить: `assets/audio/LICENSES.md`, **этот файл** (`MUSIC.md`), `CHANGELOG.md`; при новом событии —
   вызов `playSfx(...)` в `GameScreen`/`MainMenuScreen`/`SettingsScreen`; при новом треке — `AudioManager.TRACK_*`.
5. Проверить: `.\gradlew core:test` (и `.\gradlew build`), затем запуск и прослушивание.

Правила подбора:
- частые события (сыр) — короткие (< ~0.7 с), иначе накладываются друг на друга;
- музыка — луп без резких склеек; для меню/заставки — один общий трек;
- стингеры win/lose — короткие джинглы.

---

## 8. Где что в коде

| Сущность | Файл |
|---|---|
| API звука | `core/.../audio/AudioManager.kt` |
| Заставка (старт музыки меню) | `core/.../LoadingScreen.kt` (`show()`) |
| Клик кнопок и шестерёнки | `core/.../MainMenuScreen.kt` |
| Экран настроек, ползунки, клик шага | `core/.../SettingsScreen.kt` |
| Геймплей: музыка, SFX по дельтам, стингеры | `core/.../GameScreen.kt` (`startNewGame`, `playGameplaySfx`, render) |
| Константы громкости/шага/duck | `core/.../Constants.kt` |
| Подписи настроек | `core/.../MenuDefinitions.kt` |
| Хранение громкостей | `core/.../ProgressStore.kt` |
| Компонент ползунка | `core/.../UiElements.kt` (`UiSlider`) |

---

## 9. Тесты

- `AudioManagerTest` — no-op при `Gdx.audio == null`, имена треков;
- `ProgressStoreTest` — дефолт/персист/клэмп громкостей;
- `MenuDefinitionsTest` — подписи и `settings()`.

Запуск: `.\gradlew core:test` (входят в `.\gradlew build`).

---

## 10. История правок

- **1.1.0** — звук и музыка (этап 119, Фаза 1): `AudioManager`, музыка заставки/меню/геймплея,
  SFX, ducking, экран «Настройки» с ползунками, шестерёнка; ассеты CC0 (Juhani Junkala + Kenney), OGG.
  Затем: `cheese`→PIZZI04, `heart`→PIZZI02, `win`→PIZZI10, `game_over`→PIZZI11; музыка меню/заставки —
  Juhani «Ending» (отдельный джингл заставки убран).
