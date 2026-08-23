package com.poleesteel.rudazovmod.spell.api;

/**
 * Стоимость каста из осей. Не хранится в NBT и не хардкодится в записи.
 * INSTANT — один раз за каст, CHANNEL — каждый серверный тик.
 * {@code cost = modeBase * formMult * element.manaMultiplier * power}
 */
public final class SpellCost {

    public static final float INSTANT_BASE = 8.0F;
    public static final float CHANNEL_BASE = 0.4F;

    public static final float RAY_MULT = 1.0F;
    public static final float HOLD_MULT = 1.25F;
    public static final float SELF_MULT = 0.9F;

    private SpellCost() {}

    public static float of(SpellDefinition spell) {
        return of(spell.castMode(), spell.form(), spell.element(), spell.power());
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
