# ТЕХНИЧЕСКАЯ СПЕЦИФИКАЦИЯ МОДА: RUDAZOV MOD

Живой документ. При изменении подсистем (магия, предметы, сеть, capabilities) обновлять этот файл в том же PR/коммите. Не путать с `README.md` — тот файл от MDK/Forge, к моду не относится.

## 1. Технический стек
* Версия Minecraft: 1.12.2
* Загрузчик модов: Minecraft Forge (FML / EventBus API)
* Версия Java: байткод Java 8, синтаксис через Jabel (`enableModernJavaSyntax`). Records — с `@Desugar`. Нельзя API Java 9+: `List.of`, `Map.of`, `Optional.isEmpty`.
* Mod ID: `rudazovmod`
* Базовый пакет: `com.poleesteel.rudazovmod`
* Сеть: `SimpleNetworkWrapper`, канал `rudazov_net`
* Данные игрока: Forge Capabilities (`IActiveSpirit`)
* Сборка: `build.gradle` не редактировать вручную (файл меняется автоматически). Ручные правки Gradle — не в нём.

## 2. Зарегистрированные предметы, блоки, сущности и данные

### Предметы
* `ItemBloodChain` (ID: `blood_chain`, вкладка `CreativeTabs.COMBAT`, макс. стак: 1). Оружие ближнего боя и инструмент дальнего контроля. В спецификации ранее указывалась прочность 250 — в коде `setMaxDamage` сейчас нет.
* Базовые предметы (слиток кровавого железа, руда, блоки) — генерация в мире временно отложена.

### Сущности
* `EntityBloodChain` (ID: `blood_chain_entity`, Network ID: 1, tracker: range 64, update 1, sendVelocity true). Невидимая физическая сущность, отвечающая за логику контроля моба и точки привязки рендера.
* `EntitySpellProjectile` (ID: `spell_projectile`, Network ID: 2, tracker: range 64, update 1, sendVelocity true). Универсальный магический снаряд. Несёт стихию, `ProjectileShape`, `Homing` и `power` (`DataParameter`). Один класс на все виды: шар, стрела, копьё, молот. Самонаведение — модификатор этого же снаряда, не новая сущность.

### Рендереры
* `RenderBloodChain` — клиентский процедурный OpenGL-рендерер для `EntityBloodChain`.
* `RenderSpellProjectile` — процедурный рендер без текстуры, цвет из `SpellElement`, геометрия из `ProjectileShape` (шар / игла / копьё / куб-молот).
* `ManaHudOverlay` — полоска маны справа от центра, над голодом; справа ступень чакр, под полоской прогресс до следующей. Скрыта в креативе.
* `SpellSlotHud` — четыре слота Z/X/C/V слева внизу.
* `GuiGrimoire` — сборка из осей и привязка к слотам (клавиша G).

### Capabilities игрока
* `IActiveSpirit` / `ActiveSpiritData` — мана, максимум маны, непрерывное развитие духа (`spiritDevelopment`), ступень чакр (из порогов), мастерство форм и стихий, множество изученных заклинаний, 4 слота хотбара.
* Ключ: `rudazovmod:active_spirit`. Регистрация в `RegistryHandler.registerCapabilities()`, аттач в `MagicEventsHandler`.

## 3. Реализованная логика предметов и сущностей

### `ItemBloodChain`
* `getItemAttributeModifiers`: MAINHAND (+6.0D к урону, -2.4D скорость атаки).
* `itemInteractionForEntity`: ПКМ из любой руки. Защита от спама через `hasCooldown()`. Ограничение «1 игрок = 1 цель»: при активации удаляет все старые `EntityBloodChain` этого хозяина. Спавн сущности на сервере, звук `ITEM_ARMOR_EQUIP_CHAIN`, кулдаун 60 тиков.
* `hitEntity`: ванильная melee-атака с `damageItem` (прочность в конструкторе не задана — удар не ломает предмет).

### `EntityBloodChain`
* `noClip = true`. Спавн строго по координатам цели (`setLocationAndAngles`).
* Синхронизация: `DataParameter<Integer>` `TARGET_ID` и `OWNER_ID`.
* `onUpdate`: привязка к центру хитбокса цели. Разрыв, если хозяин мёртв или дальше 20 блоков.
* Паралич: `motionX/Z = 0`, прыжки гасятся, `SLOWNESS` 10 на 60 тиков, обновление зелья только при остатке ≤ 20 тиков, `showParticles = false`.

### `RenderBloodChain`
* `shouldRender` всегда `true` (ванильный frustum culling отключён).
* Поводок: процедурная линия звеньев от сущности до груди/рук игрока.
* Спираль вокруг хитбокса (диагональ радиуса, высота моба, 45 сегментов, 2.5 витка).
* Геометрия: `GL_QUADS`, чётные звенья повёрнуты на 90° по Z. Текстура `rudazovmod:textures/entity/blood_chain_link.png`.

## 4. Магия: текущее состояние

Каст идёт через `spell.*`. Пакет `magic` — capability-тик (`MagicEventsHandler`) и остаток стихии для снаряда (`SpellElement`). Старый реестр уникальных спеллов удалён.

### 4.1. Игрок и ресурс

Capability `IActiveSpirit`:
* Стартовые значения: 50 / 100 маны, `spiritDevelopment` 0 (ступень 1), мастерство осей 0.
* Ступень чакр не хранится отдельно: `getChakraLevel()` = порог по развитию (шаг 20: 0–19.99 → 1, 20–39.99 → 2, …, 80+ → 5 и дальше, потолок ступени 7).
* Реген: `+0.05F * ступень` за тик на сервере (`TickEvent.PlayerTickEvent`, фаза END).
* Синхронизация маны на клиент: `PacketSyncMana` раз в 5 тиков (мана / max / `spiritDevelopment`). Гримуар / unlock / bind / мастерство / развитие: `PacketSyncSpirit` на логин, респавн, смену измерения, после `unlock`/`bind`/`craft` и после практики каста. Каст по-прежнему шлёт только номер слота.
* Смерть / клон игрока: `PlayerEvent.Clone` копирует spirit через `IStorage.writeNBT/readNBT` (мана, развитие, мастерство, unlock, bind, гримуар). Старый NBT с одним `ChakraLevel` читается как порог ступени.
* Практика (сервер, после успешного списания маны): мастерство формы и стихии +0.1…0.5, `spiritDevelopment` ~0.12–0.35 за INSTANT (CHANNEL от длительности), `maxMana` +0.01…0.05 с мягким потолком `100 + 50*(ступень-1) + 80`.
* `upgradeChakras()` — отладка: `+20` к развитию (одна ступень). Из игры: `/rudazovmod chakra up`. Основной рост — только касты.

### 4.2. Каст

1. Клавиши Z/X/C/V: START (`PacketCastSpell`) на нажатие слота, STOP (`PacketStopCast`) на отпускание. Один слот за раз. G открывает `GuiGrimoire`.
2. Сервер читает bind, ищет определение в гримуаре затем в `SpellRegistry`, проверяет `ownsSpell`, кастует через `SpellEngine`.
3. Команды: `/rudazovmod unlock <all|spell_id>`, `/rudazovmod bind <1-4> <spell_id>`, `/rudazovmod craft <mode> <target> <form> <element> <power> [shape] [homing]`, `/rudazovmod list`, `/rudazovmod mana` (полная мана, для теста), `/rudazovmod chakra` (ступень и развитие), `/rudazovmod chakra up` (отладка, +20 к развитию). Id без `:` → namespace `rudazovmod`. `craft` пишет в гримуар id `rudazovmod:custom/<uuid>`, не в статический реестр. `craft` отвергает оси, которые ещё закрыты ступенью. `shape` и `homing` опциональны (`ORB` / `NONE` по умолчанию) и учитываются только для `RAY`+`INSTANT`+`NONE`. Если седьмой аргумент — значение `Homing`, shape остаётся `ORB`.
4. Тестовые записи: `test_ray` (ORB), `test_arrow`, `test_spear`, `test_hammer`, `test_ice`, `test_beam`, `test_hold`, `test_hold_item`, `test_hold_block`, `test_heal` (LIFE RAY ENTITY), `test_drain` (LIFE HOLD ENTITY), `test_self` (LIFE SELF INSTANT), `test_self_ward` (FIRE SELF CHANNEL).

### 4.3. Формы и стихия

* `SpellElement` окрашивает форму, не подменяет её отдельным Java-классом. Помимо `onHit`: тяга HOLD, баллистика снаряда, частицы, `onWorldHit`.
  * FIRE — горение, поджог блока, плавка дропа на HOLD, быстрый снаряд.
  * ICE — замедление, заморозка воды/лавы, вязкая тяга HOLD, медленный снаряд.
  * EARTH — тяжёлый удар и отброс, сильная тяга, швырок при отпускании HOLD, снаряд с гравитацией; INSTANT ломает мягкий блок.
  * LIFE — `onHit` лечит живых и бьёт нежить. `HOLD`+ENTITY высасывает HP в кастера. `SELF` лечит кастера. `RAY` по блоку — рост (`IGrowable`).
* `RAY` INSTANT + NONE — `EntitySpellProjectile`. Вид задаёт `ProjectileShape` (баллистика, размер, удар); `Homing` слегка или сильнее корректирует курс к ближайшему живому мобу (см. §6.6). Стихия окрашивает след, частицы и `onHit`/`onWorldHit`. CHANNEL + NONE — луч (частицы стихии, `onHit` и `onWorldHit` раз в 5 тиков). Форма снаряда и самонаведение на канал не влияют.
* `SELF` + NONE — эффект на кастера: LIFE хил, FIRE огнестойкость, ICE сопротивление, EARTH поглощение.
* `HOLD` + ENTITY — тянет цель; стихия жжёт / морозит / швыряет / высасывает (`LIFE`).
* `HOLD` + ITEM — то же для `EntityItem`; FIRE за ~1с плавит по рецепту печи.
* `HOLD` + BLOCK — вынуть/нести/поставить; частицы стихии вокруг груза.

### 4.4. Удалено при чистке прототипа

`TelekinesisLogic`, `PacketUseMagic`, `magic.SpellRegistry`, `AbstractSpell`, `SpellTelekinesis`, `SpellTestProjectile`, `CustomSpell`, `SpellForm`.

### 4.5. Ещё нет

* Кулдаун заклинаний (есть только у предмета цепи).
* Форма `AOE`.

## 5. Особенности архитектуры (общее)

* Регистрация (`RegistryHandler` в пакете `init`): ванильные события Forge (`@SubscribeEvent`). Предметы, `EntityEntryBuilder`, модели и рендереры (`@SideOnly(Side.CLIENT)` + `RenderingRegistry`).
* Жёсткое разделение: визуал в `client.render` / `client.input` / `client.render.hud` / `client.gui`, серверная физика в `entities`, `spell`, `magic` (мана/capabilities). Сеть в `network`.
* Авторитет сервера: клиент шлёт только номер слота, никогда id заклинания и никогда «я попал».
* Не плодить Event-хендлеры ради логики одного предмета или одного спелла. Исключение — тики маны, capabilities, ввод, HUD.

## 6. Движок заклинаний (`spell.*`)

Скелет живёт в `com.poleesteel.rudazovmod.spell`. Пакет `magic` — старый прототип (телекинез, тестовые снаряды); его не расширять. Scripted-спеллы не пишем.

### Пакеты

* `spell.api` — `CastMode` (INSTANT, CHANNEL), `TargetType` (NONE, ENTITY, ITEM, BLOCK), `Form` (RAY, HOLD, SELF), `ProjectileShape` (ORB, ARROW, SPEAR, HAMMER — только снаряд), `Homing` (NONE, WEAK, STRONG — только снаряд), `SpellDefinition` (record: id, оси, power, projectileShape, homing; `cost()` из осей; `writeNBT`/`readNBT`), `SpellCost`, `SpellCombination`, `SpellProgression` (чакры, мастерство, практика), `SpellTarget` (закрытый набор nested-record’ов Entity/Item/Block/None; `sealed` Jabel 1.0.1 не умеет), `CastContext`, `TargetResolver`, `FormHandler`.
* `spell.engine` — `SpellEngine` (`startCast` / `tick` / `endCast` / `canCast`: матрица + ступень чакр + мана; практика после успешного каста: мастерство, развитие, maxMana), `SpellRegistry` (`registerDefaults()` — пресеты), `ActiveCastTracker` (один CHANNEL на игрока).
* `spell.resolve` — `None` / `Entity` / `Item` / `Block` резолверы. ITEM — только `EntityItem`. ENTITY — не дроп. BLOCK — `RayTraceResult` (BlockHitResult в 1.12.2 нет).
* `spell.form` — `RayFormHandler`, `HoldFormHandler` (`HOLD`+ENTITY/ITEM/BLOCK), `SelfFormHandler` (`SELF`+NONE).

Хотбар (Z/X/C/V) шлёт START на нажатие слота и STOP на отпускание. Сервер: unlock/bind → `SpellEngine`.

### 6.1. Слои

```
Ввод (клавиша слота)
  → PacketCastSpell / PacketStopCast
    → SpellEngine.startCast / tick / endCast
      → findDefinition (гримуар | SpellRegistry)
        → TargetResolver.resolve
          → FormHandler.onStart / onTick / onEnd
```

`SpellEngine` проверяет `SpellCombination.canCast`, затем `SpellProgression` (ступень из `spiritDevelopment` / опционально мастерство), списывает ману с учётом скидки мастерства и держит CHANNEL в `ActiveCastTracker` (снимок `SpellDefinition`, не id из реестра). Поиск определения: гримуар игрока, иначе `SpellRegistry`. База стоимости — `SpellDefinition.cost()` ← `SpellCost`; каст считает `SpellCost.of(spell, formMastery, elementMastery)`. Если ступени не хватает, каста нет и мана не списывается.

### 6.2. Оси определения

| Ось | Значения в скелете |
|---|---|
| `id` | `ResourceLocation` |
| `castMode` | `INSTANT`, `CHANNEL` |
| `targetType` | `NONE`, `ENTITY`, `ITEM`, `BLOCK` |
| `form` | `RAY`, `HOLD`, `SELF` |
| `element` | `FIRE`, `ICE`, `EARTH`, `LIFE` |
| `projectileShape` | `ORB`, `ARROW`, `SPEAR`, `HAMMER`. Живёт только у `RAY`+`INSTANT`+`NONE` (снаряд). Иначе в определении всегда `ORB`. Старые NBT без ключа читаются как `ORB`. |
| `homing` | `NONE`, `WEAK`, `STRONG`. Живёт только у `RAY`+`INSTANT`+`NONE` (снаряд). Иначе всегда `NONE`. По умолчанию `NONE`. Старые NBT без ключа читаются как `NONE`. CHANNEL-луч и остальные формы модификатор игнорируют. |
| `power` | float на `SpellDefinition` |
| `cost()` | не поле: `SpellCost` = `modeBase * formMult * element.manaMultiplier * shape.manaMultiplier * homing.manaMultiplier * power * (1 - masteryBonus)`. INSTANT base 8, CHANNEL 0.4/тик; RAY 1.0, HOLD 1.25, SELF 0.9. Shape: ORB 1.00, ARROW 1.08, SPEAR 1.12, HAMMER 1.22. Homing: NONE 1.00, WEAK 1.30, STRONG 1.70. `masteryBonus` — среднее мастерства формы и стихии / 100 × 0.40 (макс. скидка 40%, не ниже 50% базы). В NBT не пишется, всегда пересчёт. |

`SELF`/`AREA` нет. Scripted-спеллы не делать.

### 6.3. `TargetType`: почему не один «телекинез на всё»

Сущность, лежащий предмет и блок — три разных объекта мира. Смешивать их в одном заклинании нельзя:

| Цель | Raytrace | Что держим в `ActiveCast` | Что делаем каждый тик | Типичный провал |
|---|---|---|---|---|
| `ENTITY` | AABB живых / коллизируемых | `entityId` | `motion*` у сущности | цель умерла, ушла в другой мир |
| `ITEM` | AABB `EntityItem` | `entityId` дропа | тащить дроп, не давать pickup | предмет подобрали / деспавн |
| `BLOCK` | блок по взгляду | `BlockPos` + `BlockState` (+ TE NBT) | не физика моба, а вынуть/нести/поставить блок | блок защищён, грув, тайл с инвентарём |
| `NONE` | нет | `NoneTarget` | форма без точки в мире (луч по взгляду) | — |

Одно заклинание выбирает **один** `TargetType`. Движок резолвит цель до вызова формы. Форма не делает свой тройной raytrace.

`ITEM` не схлопывать в `ENTITY`, даже если в Minecraft дроп — это `Entity`. Иначе один луч хватает крипера вместо алмаза, стоимость «по хитбоксу» ломается, и pickup-логика дерётся с grab. Это дешёвая экономия, которая потом взорвётся.

`BLOCK` никогда не живёт в том же коде, что `motionX`. Это мутация мира, не физика сущности.

#### Как тогда телекинез — потом, не сейчас

Не два и не три уникальных класса. Одна форма захвата (рабочее имя `HOLD` / `GRAB`) + стихия силы (когда появится) + **три записи реестра**:

* `hold` + `ENTITY` + CHANNEL — мобы и, при желании, другие живые
* `hold` + `ITEM` + CHANNEL — дроп
* `hold` + `BLOCK` + CHANNEL — блок (скорее всего отдельная реализация формы `HOLD_BLOCK`, потому что тик другой; это всё ещё форма движка, не «уникальный спелл»)

Игроку это три заклинания. Движку — `HOLD` + три `TargetType`. Пресеты: `test_hold`, `test_hold_item`, `test_hold_block`.

### 6.4. Режимы каста

* **INSTANT** — клиент шлёт пакет по фронту нажатия (`isPressed`). Один каст: снаряд, волна, самобафф.
* **CHANNEL** — START при нажатии, STOP при отпускании / смерти / потере цели / нулевой мане. Состояние: `ActiveCastTracker.ActiveCast { spell (снимок), startTick, SpellTarget }`. Каждый серверный тик: `isStillValid` или `form.isTargetStillHeld` (вынутый блок) → списание маны → `form.onTick`.
* **CHARGE** — не в первой итерации.

`CHANNEL` — про удержание кнопки, не про телекинез. Первая проверка канала в движке может быть «луч/волна, пока держишь», без захвата мобов.

Клиент больше не спамит INSTANT каждые 5 тиков.

`SpellTarget` — размеченное значение (entityId **или** BlockPos **или** пусто), не одно поле `targetId`. Иначе канал по блоку некуда сохранить.

### 6.5. `CastContext`

Кастер, `SpellDefinition`, `SpellTarget`, `ticksHeld`. Взгляд и мир — с кастера. Miss на resolve → каста нет, мана не списывается. Формы цель сами не ищут.

### 6.6. Формы

* `RAY` — снаряд (INSTANT + NONE) или луч по взгляду (CHANNEL). Снаряд один (`EntitySpellProjectile`), вид — `ProjectileShape`:
  * `ORB` — средняя скорость, обычная гравитация, средний размер, небольшой взрыв/область при ударе.
  * `ARROW` — высокая скорость, почти без гравитации, тонкий, точный урон, слабый отброс, дальняя дистанция.
  * `SPEAR` — высокая скорость, слабая гравитация, удлинённый, пробивает несколько целей.
  * `HAMMER` — низкая скорость, сильная гравитация, крупный, короткий полёт, сильный отброс (и небольшой удар по окружению). Ступень 2.
  Самонаведение (`Homing`) — отдельная ось того же снаряда, не новый Entity и не класс спелла:
  * `NONE` — летит как задала форма (по умолчанию).
  * `WEAK` — ступень 2. Слабая коррекция курса к ближайшему живому мобу в конусе ~100°: малый угол поворота (~6°/тик после штрафа), радиус захвата 16, старт через 4 тика. Не видит сквозь стены.
  * `STRONG` — ступень 3. Агрессивнее: конус ~160°, поворот 18°/тик, радиус 28, старт через 2 тика. Всё ещё не разворачивается на месте, не проходит стены и не ведет цель — быстрый снаряд (ARROW) легко промахивается на близкой дистанции.
  Наведение только на сервере, цель — живой `EntityLivingBase` кроме кастера, с линией видимости (`rayTraceBlocks`). CHANNEL-луч форму снаряда и Homing не читает.
* `HOLD` — удержание `ENTITY`/`ITEM` перед кастером; `BLOCK` вынимается из мира и ставится по отпусканию. Стихия: множитель тяги, `onHoldTick` / `onHoldRelease`.
* `SELF` — только `NONE`. INSTANT вспышка / CHANNEL импульс раз в 10 тиков. Стихия: `onSelf` (LIFE лечит, FIRE огнестойкость, ICE сопротивление, EARTH поглощение).

Стихия окрашивает форму. Доставка остаётся `RAY`/`HOLD`, не «уникальный спелл огня» и не класс `SpellHeal`. `LIFE` — знак эффекта (лечение / вампиризм), не четвёртый урон.

### 6.7. Состояние игрока (расширение capability)

Есть: мана, maxMana, `spiritDevelopment`, ступень чакр (вычисляется), мастерство форм (`RAY`/`HOLD`/`SELF`) и стихий (`FIRE`/`ICE`/`EARTH`/`LIFE`) 0…100, unlocked, bound[4], гримуар (список `SpellDefinition`). Клиент получает книгу, мастерство и развитие через `PacketSyncSpirit`; мана и развитие ещё через `PacketSyncMana`.

Добавить позже:
* кулдаун по spellId

Идентификаторы в NBT всегда полные `rudazovmod:id`. Парсер: если нет `:`, подставлять namespace мода.

### 6.8. Сеть

| Пакет | Сторона | Назначение |
|---|---|---|
| `PacketCastSpell` | C→S | INSTANT: slotIndex. Либо START channel |
| `PacketStopCast` | C→S | отпускание CHANNEL |
| `PacketSyncMana` | S→C | мана / max / `spiritDevelopment`, каждые 5 тиков. Ступень клиент считает сам. |
| `PacketSyncSpirit` | S→C | unlock + binds + гримуар + мастерство осей + развитие (id пакета 3). Не каждый тик. |
| `PacketCraftSpell` | C→S | оси + `ProjectileShape` + `Homing` + power + слот (−1 = без bind). Id выдаёт сервер. |
| `PacketBindSpell` | C→S | slot + id уже существующего определения. Сервер проверяет `ownsSpell`. |

Клиент не сообщает цель. Сервер резолвит её по `TargetType` спелла из слота.

### 6.9. Что не делать

* Не писать уникальные заклинания. Скелет (resolver + engine + form stubs) уже стоит.
* Не склеивать entity/item/block в один спелл и не плодить три scripted-телекинеза.
* Не воскрешать удалённый прототип (`AbstractSpell`, `TelekinesisLogic`, `CustomSpell`).
* Не плодить Entity на каждую стихию, не плодить Entity на каждый `ProjectileShape` и не плодить Entity на каждый `Homing`.
* Не вешать логику каста на клиентский тик кроме START/STOP/фронта нажатия.
* Не вешать рост мастерства, maxMana и `spiritDevelopment` на клиент — только сервер после списания маны.
* Не редактировать `build.gradle`.
* Не читать `README.md` MDK как документацию мода.
* Не тащить чужие магические API.

## 7. Конструктор заклинаний (цель движка)

Игрок не пишет Java и не получает «уникальный класс». Он собирает `SpellDefinition` из осей. Реестр `registerDefaults()` — только пресеты для отладки.

### 7.1. Что хранить

Слот хотбара указывает на **готовое определение**, не на «имя пресета из кода».

* Пресеты (`test_ray` и т.п.) живут в `SpellRegistry` как шаблоны.
* Собранные игроком — NBT в capability (гримуар): список `SpellDefinition`.
* Id кастома: `rudazovmod:custom/<uuid>` или индекс в гримуаре. Каст: достать определение → `SpellEngine.startCast(player, definition)` без обязательной записи в статический реестр.

`SpellDefinition` сериализуется через `writeNBT` / `readNBT` (id, оси, power, `ProjectileShape`, `Homing`). Id без `:` → namespace мода. Ключ `ProjectileShape` опционален: нет ключа → `ORB` (старые гримуары). Ключ `Homing` опционален: нет ключа → `NONE`. Стоимость **считать** из осей (`SpellCost`), не хардкодить и не доверять NBT. Незаконная комбинация не создаётся (компактный конструктор), мусор отвергается `SpellEngine` / `SpellRegistry.register` / `craft`. Не-снарядные комбинации нормализуют shape в `ORB` и homing в `NONE`.

Гримуар в `IActiveSpirit` (NBT-ключ `Grimoire`). Каст: `findDefinition` → `SpellEngine.startCast(player, definition)` без записи кастома в статический реестр. `craft` создаёт `rudazovmod:custom/<uuid>`, кладёт в гримуар и unlock.

### 7.2. Какие комбинации вообще законны

Конструктор показывает только валидные пары. Движок отвергает остальное при касте.
`SpellCombination.isLegal` / `isImplemented` / `canCast` — текущая матрица целиком кастуется. GUI и `/craft` опираются на это.

| Form | TargetType | CastMode | Смысл | Сейчас в коде |
|---|---|---|---|---|
| RAY | NONE | INSTANT | снаряд (`ProjectileShape`) | да |
| RAY | NONE | CHANNEL | луч по взгляду | да |
| RAY | ENTITY | INSTANT | хитскан в моба | частично (ветка есть) |
| HOLD | ENTITY | CHANNEL | телекинез моба | да |
| HOLD | ITEM | CHANNEL | телекинез дропа | да |
| HOLD | BLOCK | CHANNEL | нести блок | да |
| SELF | NONE | INSTANT | вспышка на кастера | да |
| SELF | NONE | CHANNEL | импульс на кастера, пока держишь | да |

Бессмысленные (не предлагать в GUI): `HOLD`+`NONE`, `HOLD`+`INSTANT`, `SELF`+ENTITY/ITEM/BLOCK, `RAY`+`ITEM` пока нет эффекта на дроп.

Новые формы (`AOE`) — не заранее.

### 7.3. Прогрессия

Открываются **оси**, не готовые спеллы: стихия, форма, режим, тип цели, потолок `power`. Ступень считается из `spiritDevelopment` (непрерывный рост от кастов). Таблица в `SpellProgression` (цифры временные):

| Ступень | Развитие | Что открыто |
|---|---|---|
| 1 | 0–19.99 | `RAY` + `FIRE`/`ICE`, `INSTANT`, снаряды `ORB`/`ARROW`/`SPEAR`, Homing `NONE`, power ≤ 2 |
| 2 | 20–39.99 | `CHANNEL`, `HOLD`, цели `ITEM`/`BLOCK`, снаряд `HAMMER`, Homing `WEAK`, power ≤ 3.5 |
| 3 | 40–59.99 | `SELF`, `EARTH`, `LIFE`, Homing `STRONG`, power ≤ 6 |
| 4 | 60–79.99 | потолок power 10 |
| 5+ | 80+ | дальше по шагу 20, ступень не выше 7 |

`SpellEngine` и `SpellBook.craft` смотрят на ступень, не на сырое число. Конструктор показывает оси текущей ступени и прогресс в шапке. Команда `unlock` по-прежнему открывает пресеты в книге, но каст закрытой комбинации не проходит. Мастерство снижает стоимость, порог мастерства для каста пока 0. Предметов и ритуалов для повышения чакр нет.

### 7.4. Порядок работ (GUI в конце)

1. NBT у `SpellDefinition`, формула маны, валидатор комбинаций — **сделано** (`writeNBT`/`readNBT`, `SpellCost`, `SpellCombination`).
2. Гримуар в `IActiveSpirit`, каст из него, `/rudazovmod craft` и `list` — **сделано**.
3. Матрица `HOLD`+ITEM и `HOLD`+BLOCK — **сделано** (`SpellCombination.isImplemented`, пресеты `test_hold_item` / `test_hold_block`).
4. Синк гримуара на клиент — **сделано** (`PacketSyncSpirit`).
5. GUI сборки и слотов — **сделано** (`GuiGrimoire`, `SpellSlotHud`, `PacketCraftSpell` / `PacketBindSpell`). План §7 закрыт.
6. Прогрессия осей — **сделано**: непрерывное `spiritDevelopment` от кастов, ступени по порогам 20, мастерство и рост maxMana. `upgradeChakras()` только отладка.

Конструктор предлагает `SpellCombination.canCast` и оси, открытые текущей ступенью. Выбор `ProjectileShape` и `Homing` в `GuiGrimoire` только при `RAY`+`INSTANT`+`NONE`. Клик по записи гримуара копирует оси (включая shape и homing) в конструктор и биндит слот. Каст по-прежнему шлёт только номер слота.

## 8. Что не делать

* Не писать уникальные Java-классы спеллов и не воскрешать `AbstractSpell` / `CustomSpell`.
* Не склеивать entity/item/block в один спелл.
* Не плодить Entity на каждую стихию, не плодить Entity на каждый `ProjectileShape` и не плодить Entity на каждый `Homing`.
* Не вешать логику каста на клиентский тик кроме START/STOP.
* Не редактировать `build.gradle`.
* Не читать `README.md` MDK как документацию мода.
