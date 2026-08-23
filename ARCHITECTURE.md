# ТЕХНИЧЕСКАЯ СПЕЦИФИКАЦИЯ МОДА: RUDAZOV MOD

Живой документ. При изменении подсистем (магия, предметы, сеть, capabilities) обновлять этот файл в том же PR/коммите. Не путать с `README.md` — тот файл от MDK/Forge, к моду не относится.

## 1. Технический стек
* Версия Minecraft: 1.12.2
* Загрузчик модов: Minecraft Forge (FML / EventBus API)
* Версия Java: Java 8 (никаких `var`, records, `List.of`, text blocks)
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
* Синхронизация маны на клиент: `PacketSyncMana` раз в 5 тиков. Изученные заклинания и бинды на клиент **не** синкаются (клиенту для каста достаточно номера слота).
* Смерть: `PlayerEvent.Clone` копирует ману, чакры, unlock и bind. Копирование идёт полями, не через NBT storage — на 1.12.2 у `event.getOriginal()` capability иногда уже инвалидирован; копировать лучше через `IStorage.writeNBT/readNBT`.
* Прокачка: `upgradeChakras()` увеличивает уровень и `maxMana += 50`. Вызова из геймплея пока нет.

### 4.2. Каст

1. Клавиши Z/X/C/V: START (`PacketCastSpell`) на нажатие слота, STOP (`PacketStopCast`) на отпускание. Один слот за раз.
2. Сервер читает bind, проверяет unlock, кастует через `SpellEngine`.
3. Команды: `/rudazovmod unlock <all|spell_id>`, `/rudazovmod bind <1-4> <spell_id>`. Id без `:` → namespace `rudazovmod`.
4. Тестовые записи: `test_ray` (FIRE, INSTANT, RAY, NONE), `test_ice` (ICE, INSTANT, RAY, NONE), `test_beam` (FIRE, CHANNEL, RAY, NONE), `test_hold` (EARTH, CHANNEL, HOLD, ENTITY).

### 4.3. Формы и стихия

* `SpellElement` в `spell.api`: FIRE / ICE / EARTH. `onHit(target, power, source)`.
* `RAY` INSTANT + NONE — спавн `EntitySpellProjectile`. CHANNEL + NONE — луч по взгляду (частицы, `onHit` раз в 5 тиков).
* `HOLD` + ENTITY — тянет цель в 4 блоках перед глазами. ITEM/BLOCK у `HOLD` пока no-op.

### 4.4. Удалено при чистке прототипа

`TelekinesisLogic`, `PacketUseMagic`, `magic.SpellRegistry`, `AbstractSpell`, `SpellTelekinesis`, `SpellTestProjectile`, `CustomSpell`, `SpellForm`.

### 4.5. Ещё нет

* Кулдаун заклинаний (есть только у предмета цепи).
* Синк unlock/bind на клиент (GUI хотбара).
* Ось стихии в `SpellDefinition`.
* Реальный эффект `RAY` / `HOLD`.

## 5. Особенности архитектуры (общее)

* Регистрация (`RegistryHandler` в пакете `init`): ванильные события Forge (`@SubscribeEvent`). Предметы, `EntityEntryBuilder`, модели и рендереры (`@SideOnly(Side.CLIENT)` + `RenderingRegistry`).
* Жёсткое разделение: визуал в `client.render` / `client.input` / `client.render.hud`, серверная физика в `entities`, `spell`, `magic` (мана/capabilities). Сеть в `network`.
* Авторитет сервера: клиент шлёт только номер слота, никогда id заклинания и никогда «я попал».
* Не плодить Event-хендлеры ради логики одного предмета или одного спелла. Исключение — тики маны, capabilities, ввод, HUD.

## 6. Движок заклинаний (`spell.*`)

Скелет живёт в `com.poleesteel.rudazovmod.spell`. Пакет `magic` — старый прототип (телекинез, тестовые снаряды); его не расширять. Scripted-спеллы не пишем.

### Пакеты

* `spell.api` — `CastMode` (INSTANT, CHANNEL), `TargetType` (NONE, ENTITY, ITEM, BLOCK), `Form` (RAY, HOLD), `SpellDefinition` (record: id, оси, power, cost), `SpellTarget` (закрытый набор nested-record’ов Entity/Item/Block/None; `sealed` Jabel 1.0.1 не умеет), `CastContext`, `TargetResolver`, `FormHandler`.
* `spell.engine` — `SpellEngine` (`startCast` / `tick` / `endCast`), `SpellRegistry` (пока пустой), `ActiveCastTracker` (один CHANNEL на игрока).
* `spell.resolve` — `None` / `Entity` / `Item` / `Block` резолверы. ITEM — только `EntityItem`. ENTITY — не дроп. BLOCK — `RayTraceResult` (BlockHitResult в 1.12.2 нет).
* `spell.form` — `RayFormHandler`, `HoldFormHandler` (заглушки, логи).

Хотбар (Z/X/C/V) шлёт START на нажатие слота и STOP на отпускание. Сервер: unlock/bind → `SpellEngine`.

### 6.1. Слои

```
Ввод (клавиша слота)
  → PacketCastSpell / PacketStopCast
    → SpellEngine.startCast / tick / endCast
      → SpellRegistry.get
        → TargetResolver.resolve
          → FormHandler.onStart / onTick / onEnd
```

`SpellEngine` списывает ману и держит CHANNEL в `ActiveCastTracker`. Реестр — данные (`SpellDefinition`), не Java-класс спелла.

### 6.2. Оси определения

| Ось | Значения в скелете |
|---|---|
| `id` | `ResourceLocation` |
| `castMode` | `INSTANT`, `CHANNEL` |
| `targetType` | `NONE`, `ENTITY`, `ITEM`, `BLOCK` |
| `form` | `RAY`, `HOLD` |
| `element` | `FIRE`, `ICE`, `EARTH` |
| `power` / `cost` | float на `SpellDefinition` |

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

Игроку это три заклинания. Движку — `HOLD` + три `TargetType`. Записи в реестр не класть, пока формы — заглушки.

### 6.4. Режимы каста

* **INSTANT** — клиент шлёт пакет по фронту нажатия (`isPressed`). Один каст: снаряд, волна, самобафф.
* **CHANNEL** — START при нажатии, STOP при отпускании / смерти / потере цели / нулевой мане. Состояние: `ActiveCastTracker.ActiveCast { spellId, startTick, SpellTarget }`. Каждый серверный тик: `isStillValid` → списание маны → `form.onTick`.
* **CHARGE** — не в первой итерации.

`CHANNEL` — про удержание кнопки, не про телекинез. Первая проверка канала в движке может быть «луч/волна, пока держишь», без захвата мобов.

Клиент больше не спамит INSTANT каждые 5 тиков.

`SpellTarget` — размеченное значение (entityId **или** BlockPos **или** пусто), не одно поле `targetId`. Иначе канал по блоку некуда сохранить.

### 6.5. `CastContext`

Кастер, `SpellDefinition`, `SpellTarget`, `ticksHeld`. Взгляд и мир — с кастера. Miss на resolve → каста нет, мана не списывается. Формы цель сами не ищут.

### 6.6. Формы

* `RAY` — снаряд (INSTANT + NONE) или луч по взгляду (CHANNEL).
* `HOLD` — удержание `ENTITY` перед кастером. ITEM/BLOCK не реализованы.

Стихия задаёт `onHit`, не доставку.

### 6.7. Состояние игрока (расширение capability)

Оставить: мана, maxMana, chakraLevel, unlocked, bound[4].

Добавить:
* кулдаун по spellId
* `ActiveCast` с `SpellTarget`
* позже, для GUI слотов — полная синхронизация spirit, не только мана

Идентификаторы в NBT всегда полные `rudazovmod:id`. Парсер: если нет `:`, подставлять namespace мода.

### 6.8. Сеть

| Пакет | Сторона | Назначение |
|---|---|---|
| `PacketCastSpell` | C→S | INSTANT: slotIndex. Либо START channel |
| `PacketStopCast` (новый) | C→S | отпускание CHANNEL |
| `PacketSyncMana` | S→C | мана / max / чакры (уже есть) |
| `PacketSyncSpirit` (позже) | S→C | unlock + binds для GUI |

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

## 7. План внедрения движка

Скелет, каст, чистка, стихия, живые `RAY`/`HOLD`(entity) — сделаны. Дальше:

1. `HOLD` для `ITEM` и `BLOCK` (остальные две composed-записи телекинеза).
2. Кулдауны в capability. GUI/полный sync spirit — когда понадобится интерфейс слотов.

Не добавлять GUI крафта, пока не нужны слоты на клиенте.
