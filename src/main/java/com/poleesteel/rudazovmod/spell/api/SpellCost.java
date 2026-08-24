package com.poleesteel.rudazovmod.spell.api;

/**
 * Стоимость каста из осей. Не хранится в NBT и не хардкодится в записи.
 * INSTANT — один раз за каст, CHANNEL — каждый серверный тик.
 * {@code cost = modeBase * formMult * element.manaMultiplier * power * (1 - masteryBonus)}
 */
public final class SpellCost {

    public static final float INSTANT_BASE = 8.0F;
    public static final float CHANNEL_BASE = 0.4F;

    public static final float RAY_MULT = 1.0F;
    public static final float HOLD_MULT = 1.25F;
    public static final float SELF_MULT = 0.9F;

    /** Максимальная скидка при мастерстве 100/100 по форме и стихии. */
    public static final float MAX_MASTERY_DISCOUNT = 0.40F;
    /** Ниже этой доли базы стоимость не опускается. */
    public static final float MIN_COST_FACTOR = 0.50F;

    private SpellCost() {}

    public static float of(SpellDefinition spell) {
        return of(spell.castMode(), spell.form(), spell.element(), spell.power());
    }

    public static float of(SpellDefinition spell, float formMastery, float elementMastery) {
        return applyMastery(of(spell), formMastery, elementMastery);
    }

    public static float of(CastMode mode, Form form, SpellElement element, float power) {
        if (mode == null || form == null || element == null) {
            throw new IllegalArgumentException("mode/form/element");
        }
        if (power <= 0.0F || Float.isNaN(power) || Float.isInfinite(power)) {
            throw new IllegalArgumentException("power");
        }
        return base(mode) * formMult(form) * element.getManaMultiplier() * power;
    }

    public static float of(
            CastMode mode, Form form, SpellElement element, float power,
            float formMastery, float elementMastery) {
        return applyMastery(of(mode, form, element, power), formMastery, elementMastery);
    }

    public static float applyMastery(float base, float formMastery, float elementMastery) {
        float bonus = masteryBonus(formMastery, elementMastery);
        return Math.max(base * MIN_COST_FACTOR, base * (1.0F - bonus));
    }

    public static float masteryBonus(float formMastery, float elementMastery) {
        float avg = (SpellProgression.clampMastery(formMastery)
                + SpellProgression.clampMastery(elementMastery)) * 0.5F;
        return (avg / SpellProgression.MASTERY_MAX) * MAX_MASTERY_DISCOUNT;
    }

    private static float base(CastMode mode) {
        switch (mode) {
            case CHANNEL:
                return CHANNEL_BASE;
            case INSTANT:
            default:
                return INSTANT_BASE;
        }
    }

    private static float formMult(Form form) {
        switch (form) {
            case HOLD:
                return HOLD_MULT;
            case SELF:
                return SELF_MULT;
            case RAY:
            default:
                return RAY_MULT;
        }
    }
}
