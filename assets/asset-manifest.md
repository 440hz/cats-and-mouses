# Манифест ассетов (человекочитаемая таблица)

Машиночитаемая версия — `asset-manifest.json` (ключ — стабильный `asset_id` SpriteCook). Обновлять оба файла вместе.

«Размер файла» — размер игрового файла (холст/канвас). «Реальный размер» — видимая часть картинки внутри холста (контент, обрезанный по alpha>10).

| asset_id (короткий) | Название | Игровой файл | Размер файла (холст) | Реальный размер (контент) | RAW-мастер |
|---|---|---|---|---|---|
| `086c09eb` | мышь (игрок), top-down, серая, 16-bit | `sprites/mouse.png` | 64×64 | 48×64 (нос вниз) | `sprites/raw/mouse.png` 1024×1024 (PNG) |
| `f2cc9aab` | сыр (собираемый), top-down | `sprites/cheese.png` | 64×64 | 34×38 (намеренно −40%) | `sprites/raw/cheese.png` 1024×1024 |
| `1297baca` | кот Chaser (красный) | `sprites/cat_chaser.png` | 64×64 | 44×64 | `sprites/raw/cat_chaser.png` 1024×1024 |
| `6da9d056` | кот Patrol (синий) | `sprites/cat_patrol.png` | 64×64 | 48×64 | `sprites/raw/cat_patrol.png` 1024×1024 |
| `15b2af49` | кот Random (фиолетовый) | `sprites/cat_random.png` | 64×64 | 36×64 | `sprites/raw/cat_random.png` 1024×1024 |
| `273f07ad` | кот Ambush (тёмно-серый) | `sprites/cat_ambush.png` | 64×64 | 24×64 | `sprites/raw/cat_ambush.png` 1024×1024 |
| `e54c495a` | иконка приложения | `icon/app_icon.png` | 1024×1024 | 1024×1024 | `icon/raw.png` 2048×2048 |
| `cd669f6b` | куст (стена), тема «трава» | `themes/grass/wall_0.png` | 88×88 | 88×88 | `themes/grass/raw/wall_0.png` 1024×1024 |
| `7597fe5e` | куст цветущий (стена) | `themes/grass/wall_1.png` | 114×114 | 114×114 | `themes/grass/raw/wall_1.png` 1024×1024 |
| `a5e713e8` | камень (стена) | `themes/grass/wall_2.png` | 34×34 | 34×34 | `themes/grass/raw/wall_2.png` 1024×1024 |
| `5cafc564` | дерево (стена) | `themes/grass/wall_3.png` | 32×32 | 32×32 | `themes/grass/raw/wall_3.png` 1024×1024 |
| `9b23ff34` | фон темы «трава» (тайловый) | `themes/grass/background.png` | 200×200 | 200×200 | `themes/grass/raw/background.png` 1024×1024 |
| `52e2e7e3` | сердечко полное | `sprites/heart_full.png` | 64×64 | 64×59 | `sprites/raw/heart_full.png` 1024×1024 |
| `c0225f37` | сердечко пустое (контур) | `sprites/heart_empty.png` | 64×64 | 64×55 | `sprites/raw/heart_empty.png` 1024×1024 |
| `dd02ce39` | кнопка UI, normal | `ui/button.png` | 360×149 | 360×149 | `ui/raw/button.png` 2048×2048 |
| `f93b03d6` | кнопка UI, pressed | `ui/button_pressed.png` | 360×175 | 360×175 | `ui/raw/button_pressed.png` 2048×2048 |
| `7720de56` | логотип экрана загрузки | `ui/loading_logo.png` | 600×390 | 600×390 | `ui/raw/loading_logo.png` 2048×2048 |
| `20be8e8c` | логотип загрузки v2 (3 мыши + 4 кота) | `ui/loading_logo_2.png` | 1024×512 | 1024×512 (непрозрачный фон) | `ui/raw/loading_logo_2.png` 2752×1536 |
| `94c50498` | логотип загрузки v4 (3 мыши над доской) | `ui/loading_logo_4.png` | 600×430 | 600×430 | `ui/raw/loading_logo_4.png` 1024×1024 |
| `39c1c21d`* | логотип загрузки v3 (3 мыши над доской; база для v4) | `ui/loading_logo_3.png` | 600×429 | 600×429 | `ui/raw/loading_logo_3.png` 2048×2048 |
| `cee551ee` | 15-piece autotile стена-куст, тема «трава» | `themes/grass/wall_autotile.png` | 160×160 (4×4 тайлов по 40×40) | 160×160 | `themes/grass/raw/wall_autotile.png` 128×128 |
| `1b3e9538` | 15-piece autotile каменная стена замка, тема «Старый замок» | `themes/castle/wall_autotile.png` | 160×160 (4×4 тайлов по 40×40) | 160×160 | `themes/castle/raw/wall_autotile.png` 128×128 |
| `ba7814d1` | фон темы «Старый замок» (каменные плиты, тайловый) | `themes/castle/background.png` | 200×200 | 200×200 | `themes/castle/raw/background.png` 1024×1024 |
| `54361830` | 15-piece autotile ледяные стены, тема «Ледяная пещера» | `themes/ice/wall_autotile.png` | 160×160 (4×4 тайлов по 40×40) | 160×160 | `themes/ice/raw/wall_autotile.png` 128×128 |
| `d9afd3c7` | фон темы «Ледяная пещера» (снег и лёд, тайловый) | `themes/ice/background.png` | 200×200 | 200×200 | `themes/ice/raw/background.png` 1024×1024 |
| `73ec559d` | 15-piece autotile скалы/песчаник, тема «Песчаный каньон» | `themes/desert/wall_autotile.png` | 160×160 (4×4 тайлов по 40×40) | 160×160 | `themes/desert/raw/wall_autotile.png` 128×128 |
| `bef5b628` | фон темы «Песчаный каньон» (золотой песок, тайловый) | `themes/desert/background.png` | 200×200 | 200×200 | `themes/desert/raw/background.png` 1024×1024 |
| `d90af37d` | 15-piece autotile стволы древних деревьев во мху, тема «Мох и древние деревья» | `themes/moss/wall_autotile.png` | 160×160 (4×4 тайлов по 40×40) | 160×160 | `themes/moss/raw/wall_autotile.png` 128×128 |
| `7dc7e51f` | фон темы «Мох и древние деревья» (мшистая лесная почва, тайловый) | `themes/moss/background.png` | 200×200 | 200×200 | `themes/moss/raw/background.png` 1024×1024 |
| `47c5aecd` | 15-piece autotile камыши и мшистые скалы, тема «Затерянное болото» | `themes/swamp/wall_autotile.png` | 160×160 (4×4 тайлов по 40×40) | 160×160 | `themes/swamp/raw/wall_autotile.png` 128×128 |
| `26a9a88a` | фон темы «Затерянное болото» (мелкая мутная вода, тайловый) | `themes/swamp/background.png` | 200×200 | 200×200 | `themes/swamp/raw/background.png` 1024×1024 |
| `6c620a3f` | 15-piece autotile металлические панели корпуса станции, тема «Космическая станция» | `themes/space/wall_autotile.png` | 160×160 (4×4 тайлов по 40×40) | 160×160 | `themes/space/raw/wall_autotile.png` 128×128 |
| `b5107c09` | фон темы «Космическая станция» (металлический пол отсека, тайловый) | `themes/space/background.png` | 200×200 | 200×200 | `themes/space/raw/background.png` 1024×1024 |

\* полный asset_id уточнить — API SpriteCook недоступен