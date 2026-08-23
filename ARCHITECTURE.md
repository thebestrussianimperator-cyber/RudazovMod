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
* `EntitySpellProjectile` (ID: `spell_projectile`, Network ID: 2, tracker: range 64, update 1, sendVelocity true). Универсальный магический снаряд. Несёт стихию (`DataParameter` ordinal) и мощность (`power`, только сервер).

### Рендереры
* `RenderBloodChain` — клиентский процедурный OpenGL-рендерер для `EntityBloodChain`.
* `RenderSpellProjectile` — биллборд без текстуры, цвет из `SpellElement`.
* `ManaHudOverlay` — полоска маны справа от центра, над голодом. Скрыта в креативе.
* `SpellSlotHud` — четыре слота Z/X/C/V слева внизу.
* `GuiGrimoire` — сборка из осей и привязка к слотам (клавиша G).

### Capabilities игрока
* `IActiveSpirit` / `ActiveSpiritData` — мана, максимум маны, уровень чакр, множество изученных заклинаний, 4 слота хотбара.
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
* Стартовые значения: 50 / 100 маны, уровень чакр 1.
* Реген: `+0.05F * chakraLevel` за тик на сервере (`TickEvent.PlayerTickEvent`, фаза END).
* Синхронизация маны на клиент: `PacketSyncMana` раз в 5 тиков. Гримуар / unlock / bind: `PacketSyncSpirit` на логин, респавн, смену измерения и после `unlock`/`bind`/`craft`. Каст по-прежнему шлёт только номер слота.
* Смерть / клон игрока: `PlayerEvent.Clone` копирует spirit через `IStorage.writeNBT/readNBT` (мана, чакры, unlock, bind, гримуар).
* Прокачка: `upgradeChakras()` увеличивает уровень и `maxMana += 50`. Вызова из геймплея пока нет.

### 4.2. Каст

1. Клавиши Z/X/C/V: START (`PacketCastSpell`) на нажатие слота, STOP (`PacketStopCast`) на отпускание. Один слот за раз. G открывает `GuiGrimoire`.
2. Сервер читает bind, ищет определение в гримуаре затем в `SpellRegistry`, проверяет `ownsSpell`, кастует через `SpellEngine`.
3. Команды: `/rudazovmod unlock <all|spell_id>`, `/rudazovmod bind <1-4> <spell_id>`, `/rudazovmod craft <mode> <target> <form> <element> <power>`, `/rudazovmod list`. Id без `:` → namespace `rudazovmod`. `craft` пишет в гримуар id `rudazovmod:custom/<uuid>`, не в статический реестр.
4. Тестовые записи: `test_ray` (FIRE, INSTANT, RAY, NONE), `test_ice` (ICE, INSTANT, RAY, NONE), `test_beam` (FIRE, CHANNEL, RAY, NONE), `test_hold` (EARTH, CHANNEL, HOLD, ENTITY), `test_hold_item` (HOLD+ITEM), `test_hold_block` (HOLD+BLOCK), `test_heal` (LIFE, INSTANT, RAY, ENTITY), `test_drain` (LIFE, CHANNEL, HOLD, ENTITY).

### 4.3. Формы и стихия

* `SpellElement` окрашивает форму, не подменяет её отдельным Java-классом. Помимо `onHit`: тяга HOLD, баллистика снаряда, частицы, `onWorldHit`.
  * FIRE — горение, поджог блока, плавка дропа на HOLD, быстрый снаряд.
  * ICE — замедление, заморозка воды/лавы, вязкая тяга HOLD, медленный снаряд.
  * EARTH — тяжёлый удар и отброс, сильная тяга, швырок при отпускании HOLD, снаряд с гравитацией; INSTANT ломает мягкий блок.
  * LIFE — `onHit` лечит живых и бьёт нежить. `HOLD`+ENTITY высасывает HP в кастера. `RAY` по блоку — рост (`IGrowable`). Форма `SELF` ещё нет.
* `RAY` INSTANT + NONE — `EntitySpellProjectile` (скорость/гравитация/след стихии). CHANNEL + NONE — луч (частицы стихии, `onHit` и `onWorldHit` раз в 5 тиков).
* `HOLD` + ENTITY — тянет цель; стихия жжёт / морозит / швыряет / высасывает (`LIFE`).
* `HOLD` + ITEM — то же для `EntityItem`; FIRE за ~1с плавит по рецепту печи.
* `HOLD` + BLOCK — вынуть/нести/поставить; частицы стихии вокруг груза.

### 4.4. Удалено при чистке прототипа

`TelekinesisLogic`, `PacketUseMagic`, `magic.SpellRegistry`, `AbstractSpell`, `SpellTelekinesis`, `SpellTestProjectile`, `CustomSpell`, `SpellForm`.

### 4.5. Ещё нет

* Кулдаун заклинаний (есть только у предмета цепи).
* Прогрессия осей (пока конструктор показывает всю матрицу `canCast`).
* Форма `SELF` (хил без цели в мире).

## 5. Особенности архитектуры (общее)

* Регистрация (`RegistryHandler` в пакете `init`): ванильные события Forge (`@SubscribeEvent`). Предметы, `EntityEntryBuilder`, модели и рендереры (`@SideOnly(Side.CLIENT)` + `RenderingRegistry`).
* Жёсткое разделение: визуал в `client.render` / `client.input` / `client.render.hud` / `client.gui`, серверная физика в `entities`, `spell`, `magic` (мана/capabilities). Сеть в `network`.
* Авторитет сервера: клиент шлёт только номер слота, никогда id заклинания и никогда «я попал».
* Не плодить Event-хендлеры ради логики одного предмета или одного спелла. Исключение — тики маны, capabilities, ввод, HUD.

## 6. Движок заклинаний (`spell.*`)

Скелет живёт в `com.poleesteel.rudazovmod.spell`. Пакет `magic` — старый прототип (телекинез, тестовые снаряды); его не расширять. Scripted-спеллы не пишем.

### Пакеты

* `spell.api` — `CastMode` (INSTANT, CHANNEL), `TargetType` (NONE, ENTITY, ITEM, BLOCK), `Form` (RAY, HOLD), `SpellDefinition` (record: id, оси, power; `cost()` из осей; `writeNBT`/`readNBT`), `SpellCost`, `SpellCombination`, `SpellTarget` (закрытый набор nested-record’ов Entity/Item/Block/None; `sealed` Jabel 1.0.1 не умеет), `CastContext`, `TargetResolver`, `FormHandler`.
* `spell.engine` — `SpellEngine` (`startCast` / `tick` / `endCast`, отказ `!canCast`), `SpellRegistry` (`registerDefaults()` — пресеты), `ActiveCastTracker` (один CHANNEL на игрока).
* `spell.resolve` — `None` / `Entity` / `Item` / `Block` резолверы. ITEM — только `EntityItem`. ENTITY — не дроп. BLOCK — `RayTraceResult` (BlockHitResult в 1.12.2 нет).
* `spell.form` — `RayFormHandler`, `HoldFormHandler` (`HOLD`+ENTITY/ITEM/BLOCK).

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

`SpellEngine` проверяет `SpellCombination.canCast`, списывает ману и держит CHANNEL в `ActiveCastTracker` (снимок `SpellDefinition`, не id из реестра). Поиск определения: гримуар игрока, иначе `SpellRegistry`. Стоимость — `SpellDefinition.cost()` ← `SpellCost`.

### 6.2. Оси определения

| Ось | Значения в скелете |
|---|---|
| `id` | `ResourceLocation` |
| `castMode` | `INSTANT`, `CHANNEL` |
| `targetType` | `NONE`, `ENTITY`, `ITEM`, `BLOCK` |
| `form` | `RAY`, `HOLD` |
| `element` | `FIRE`, `ICE`, `EARTH`, `LIFE` |
| `power` | float на `SpellDefinition` |
| `cost()` | не поле: `SpellCost` = `modeBase * formMult * element.manaMultiplier * power`. INSTANT base 8, CHANNEL 0.4/тик; RAY 1.0, HOLD 1.25. В NBT не пишется, всегда пересчёт. |

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

* `RAY` — снаряд (INSTANT + NONE) или луч по взгляду (CHANNEL). Стихия: след, баллистика, удар по мобу и блоку.
* `HOLD` — удержание `ENTITY`/`ITEM` перед кастером; `BLOCK` вынимается из мира и ставится по отпусканию. Стихия: множитель тяги, `onHoldTick` / `onHoldRelease`.

Стихия окрашивает форму. Доставка остаётся `RAY`/`HOLD`, не «уникальный спелл огня» и не класс `SpellHeal`. `LIFE` — знак эффекта (лечение / вампиризм), не четвёртый урон.

### 6.7. Состояние игрока (расширение capability)

Есть: мана, maxMana, chakraLevel, unlocked, bound[4], гримуар (список `SpellDefinition`). Клиент получает книгу через `PacketSyncSpirit`.

Добавить позже:
* кулдаун по spellId

Идентификаторы в NBT всегда полные `rudazovmod:id`. Парсер: если нет `:`, подставлять namespace мода.

### 6.8. Сеть

| Пакет | Сторона | Назначение |
|---|---|---|
| `PacketCastSpell` | C→S | INSTANT: slotIndex. Либо START channel |
| `PacketStopCast` | C→S | отпускание CHANNEL |
| `PacketSyncMana` | S→C | мана / max / чакры, каждые 5 тиков |
| `PacketSyncSpirit` | S→C | unlock + binds + гримуар (id пакета 3). Не каждый тик. |
| `PacketCraftSpell` | C→S | оси + power + слот (−1 = без bind). Id выдаёт сервер. |
| `PacketBindSpell` | C→S | slot + id уже существующего определения. Сервер проверяет `ownsSpell`. |

Клиент не сообщает цель. Сервер резолвит её по `TargetType` спелла из слота.

### 6.9. Что не делать

* Не писать уникальные заклинания. Скелет (resolver + engine + form stubs) уже стоит.
* Не склеивать entity/item/block в один спелл и не плодить три scripted-телекинеза.
* Не воскрешать удалённый прототип (`AbstractSpell`, `TelekinesisLogic`, `CustomSpell`).
* Не плодить Entity на каждую стихию.
* Не вешать логику каста на клиентский тик кроме START/STOP/фронта нажатия.
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

`SpellDefinition` сериализуется через `writeNBT` / `readNBT` (id, оси, power). Id без `:` → namespace мода. Стоимость **считать** из осей (`SpellCost`), не хардкодить и не доверять NBT. Незаконная комбинация не создаётся (компактный конструктор), мусор отвергается `SpellEngine` / `SpellRegistry.register` / `craft`.

Гримуар в `IActiveSpirit` (NBT-ключ `Grimoire`). Каст: `findDefinition` → `SpellEngine.startCast(player, definition)` без записи кастома в статический реестр. `craft` создаёт `rudazovmod:custom/<uuid>`, кладёт в гримуар и unlock.

### 7.2. Какие комбинации вообще законны

Конструктор показывает только валидные пары. Движок отвергает остальное при касте.
`SpellCombination.isLegal` / `isImplemented` / `canCast` — текущая матрица целиком кастуется. GUI и `/craft` опираются на это.

| Form | TargetType | CastMode | Смысл | Сейчас в коде |
|---|---|---|---|---|
| RAY | NONE | INSTANT | снаряд | да |
| RAY | NONE | CHANNEL | луч по взгляду | да |
| RAY | ENTITY | INSTANT | хитскан в моба | частично (ветка есть) |
| HOLD | ENTITY | CHANNEL | телекинез моба | да |
| HOLD | ITEM | CHANNEL | телекинез дропа | да |
| HOLD | BLOCK | CHANNEL | нести блок | да |

Бессмысленные (не предлагать в GUI): `HOLD`+`NONE`, `HOLD`+`INSTANT`, `RAY`+`ITEM` пока нет эффекта на дроп.

Новые формы (`SELF`, `AOE`) — только когда текущая матрица кастуется из гримуара, не заранее.

### 7.3. Прогрессия

Открываются **оси**, не готовые спеллы: стихия, форма, режим, тип цели, потолок `power`. Игрок собирает то, что уже открыл. Команды `unlock` временно могут открывать всё.

### 7.4. Порядок работ (GUI в конце)

1. NBT у `SpellDefinition`, формула маны, валидатор комбинаций — **сделано** (`writeNBT`/`readNBT`, `SpellCost`, `SpellCombination`).
2. Гримуар в `IActiveSpirit`, каст из него, `/rudazovmod craft` и `list` — **сделано**.
3. Матрица `HOLD`+ITEM и `HOLD`+BLOCK — **сделано** (`SpellCombination.isImplemented`, пресеты `test_hold_item` / `test_hold_block`).
4. Синк гримуара на клиент — **сделано** (`PacketSyncSpirit`).
5. GUI сборки и слотов — **сделано** (`GuiGrimoire`, `SpellSlotHud`, `PacketCraftSpell` / `PacketBindSpell`). План §7 закрыт.

Конструктор предлагает только `SpellCombination.canCast`. Каст по-прежнему шлёт только номер слота.

## 8. Что не делать

* Не писать уникальные Java-классы спеллов и не воскрешать `AbstractSpell` / `CustomSpell`.
* Не склеивать entity/item/block в один спелл.
* Не плодить Entity на каждую стихию.
* Не вешать логику каста на клиентский тик кроме START/STOP.
* Не редактировать `build.gradle`.
* Не читать `README.md` MDK как документацию мода.
