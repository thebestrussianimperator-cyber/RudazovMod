package com.poleesteel.rudazovmod.spell.api;

/**
 * Прогрессия осей: непрерывное развитие духа, ступень открывает комбинации.
 * Цифры временные — таблица должна работать, баланс подкрутим позже.
 *
 * <pre>
 * развитие 0–19.99 → ступень 1: RAY + FIRE/ICE, INSTANT, power ≤ 2
 * 20–39.99 → 2: CHANNEL, HOLD, цели ITEM/BLOCK, power ≤ 3.5
 * 40–59.99 → 3: SELF, EARTH, LIFE, power ≤ 6
 * 60–79.99 → 4+: потолок power как у конструктора
 * 80+ → 5 и дальше (шаг 20)
 * </pre>
 */
public final class SpellProgression {

    public static final int MIN_CHAKRA = 1;
    public static final int MAX_CHAKRA = 7;
    /** Ширина одной ступени по {@code spiritDevelopment}. */
    public static final float STAGE_WIDTH = 20.0F;

    public static final float MASTERY_MIN = 0.0F;
    public static final float MASTERY_MAX = 100.0F;

    /** База maxMana на 1-й чакре; каждая следующая чакра даёт +50. */
    public static final float CHAKRA_MANA_BASE = 100.0F;
    public static final float CHAKRA_MANA_STEP = 50.0F;
    /** Мягкий потолок практики сверх базы чакр. */
    public static final float PRACTICE_MANA_ROOM = 80.0F;

    private SpellProgression() {}

    public static int stageOf(float development) {
        float value = clampDevelopment(development);
        int stage = MIN_CHAKRA + (int) (value / STAGE_WIDTH);
        return Math.max(MIN_CHAKRA, Math.min(MAX_CHAKRA, stage));
    }

    /** Нижняя граница ступени (ступень 1 = 0, ступень 2 = 20, …). */
    public static float stageStart(int stage) {
        int level = Math.max(MIN_CHAKRA, Math.min(MAX_CHAKRA, stage));
        return STAGE_WIDTH * (level - 1);
    }

    /** Порог следующей ступени; на максимуме — {@link Float#POSITIVE_INFINITY}. */
    public static float nextStageAt(int stage) {
        if (stage >= MAX_CHAKRA) {
            return Float.POSITIVE_INFINITY;
        }
        return STAGE_WIDTH * Math.max(MIN_CHAKRA, stage);
    }

    public static float clampDevelopment(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return 0.0F;
        }
        return Math.max(0.0F, value);
    }

    /**
     * Прирост развития духа за каст. Медленнее мастерства: ~80–120 кастов на ступень
     * при типичном RAY INSTANT power 2.
     */
    public static float developmentGain(SpellDefinition spell, int ticksHeld) {
        if (spell == null) {
            return 0.0F;
        }
        float gain = 0.12F + 0.06F * Math.min(spell.power(), 5.0F);
        if (spell.castMode() == CastMode.CHANNEL) {
            float seconds = Math.max(ticksHeld, 1) / 20.0F;
            gain *= Math.min(2.0F, Math.max(0.25F, seconds));
            return Math.min(0.70F, gain);
        }
        return Math.min(0.35F, gain);
    }

    public static int requiredChakra(SpellDefinition spell) {
        return spell == null
                ? Integer.MAX_VALUE
                : requiredChakra(spell.form(), spell.targetType(), spell.castMode(), spell.element(), spell.power());
    }

    public static int requiredChakra(
            Form form, TargetType target, CastMode mode, SpellElement element, float power) {
        int level = MIN_CHAKRA;
        if (mode == CastMode.CHANNEL) {
            level = Math.max(level, 2);
        }
        if (form == Form.HOLD) {
            level = Math.max(level, 2);
        }
        if (form == Form.SELF) {
            level = Math.max(level, 3);
        }
        if (element == SpellElement.EARTH || element == SpellElement.LIFE) {
            level = Math.max(level, 3);
        }
        if (target == TargetType.ITEM || target == TargetType.BLOCK) {
            level = Math.max(level, 2);
        }
        if (power > maxPower(1)) {
            level = Math.max(level, 2);
        }
        if (power > maxPower(2)) {
            level = Math.max(level, 3);
        }
        if (power > maxPower(3)) {
            level = Math.max(level, 4);
        }
        return level;
    }

    public static float maxPower(int chakraLevel) {
        if (chakraLevel <= 1) {
            return 2.0F;
        }
        if (chakraLevel == 2) {
            return 3.5F;
        }
        if (chakraLevel == 3) {
            return 6.0F;
        }
        return 10.0F;
    }

    /** Пока порог 0: крючок есть, гейт не мешает тесту. */
    public static float requiredFormMastery(SpellDefinition spell) {
        return spell == null ? MASTERY_MAX : MASTERY_MIN;
    }

    public static float requiredElementMastery(SpellDefinition spell) {
        return spell == null ? MASTERY_MAX : MASTERY_MIN;
    }

    public static boolean meetsChakra(int chakraLevel, SpellDefinition spell) {
        return spell != null && meetsChakra(
                chakraLevel, spell.form(), spell.targetType(), spell.castMode(), spell.element(), spell.power());
    }

    public static boolean meetsChakra(
            int chakraLevel, Form form, TargetType target, CastMode mode, SpellElement element, float power) {
        if (form == null || target == null || mode == null || element == null) {
            return false;
        }
        if (chakraLevel < requiredChakra(form, target, mode, element, power)) {
            return false;
        }
        return power <= maxPower(chakraLevel) + 0.0001F;
    }

    public static boolean meetsMastery(float formMastery, float elementMastery, SpellDefinition spell) {
        if (spell == null) {
            return false;
        }
        return formMastery + 0.0001F >= requiredFormMastery(spell)
                && elementMastery + 0.0001F >= requiredElementMastery(spell);
    }

    public static boolean canCast(
            int chakraLevel, float formMastery, float elementMastery, SpellDefinition spell) {
        return meetsChakra(chakraLevel, spell) && meetsMastery(formMastery, elementMastery, spell);
    }

    /**
     * Прирост мастерства формы и стихии за каст. INSTANT: {@code ticksHeld == 0}.
     * CHANNEL масштабируется длительностью (1 с ≈ INSTANT, до ×2).
     */
    public static float masteryGain(SpellDefinition spell, int ticksHeld) {
        if (spell == null) {
            return 0.0F;
        }
        float gain = 0.12F + 0.076F * Math.min(spell.power(), 5.0F);
        if (spell.castMode() == CastMode.CHANNEL) {
            float seconds = Math.max(ticksHeld, 1) / 20.0F;
            gain *= Math.min(2.0F, Math.max(0.25F, seconds));
            return Math.min(1.0F, gain);
        }
        return Math.min(0.5F, gain);
    }

    public static float maxManaGain(SpellDefinition spell, int ticksHeld, float currentMax, int chakraLevel) {
        if (spell == null) {
            return 0.0F;
        }
        float room = practiceHeadroom(currentMax, chakraLevel);
        if (room <= 0.0F) {
            return 0.0F;
        }
        float gain = 0.01F + 0.008F * Math.min(spell.power(), 5.0F);
        if (spell.castMode() == CastMode.CHANNEL) {
            float seconds = Math.max(ticksHeld, 1) / 20.0F;
            gain *= Math.min(2.0F, Math.max(0.25F, seconds));
            gain = Math.min(0.10F, gain);
        } else {
            gain = Math.min(0.05F, gain);
        }
        return gain * (room / PRACTICE_MANA_ROOM);
    }

    public static float chakraMaxMana(int chakraLevel) {
        int level = Math.max(MIN_CHAKRA, chakraLevel);
        return CHAKRA_MANA_BASE + CHAKRA_MANA_STEP * (level - 1);
    }

    public static float practiceCap(int chakraLevel) {
        return chakraMaxMana(chakraLevel) + PRACTICE_MANA_ROOM;
    }

    public static float clampMastery(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return MASTERY_MIN;
        }
        return Math.max(MASTERY_MIN, Math.min(MASTERY_MAX, value));
    }

    private static float practiceHeadroom(float currentMax, int chakraLevel) {
        if (Float.isNaN(currentMax) || Float.isInfinite(currentMax)) {
            return 0.0F;
        }
        return Math.max(0.0F, practiceCap(chakraLevel) - currentMax);
    }
}
