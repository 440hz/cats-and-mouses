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