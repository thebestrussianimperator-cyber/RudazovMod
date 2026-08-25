package com.poleesteel.rudazovmod.spell.api;

/**
 * Вид снаряда для {@code RAY + INSTANT + NONE}. Не отдельная сущность и не класс спелла.
 * Для остальных комбинаций в определении всегда {@link #ORB}.
 */
public enum ProjectileShape {
    ORB,
    ARROW,
    SPEAR,
    HAMMER;

    /** Множитель маны поверх стихии. ORB — база. */
    public float manaMultiplier() {
        switch (this) {
            case ARROW:
                return 1.08F;
            case SPEAR:
                return 1.12F;
            case HAMMER:
                return 1.35F;
            case ORB:
            default:
                return 1.0F;
        }
    }

    /** Базовая скорость выстрела. Стихия чуть множит её. */
    public float speed() {
        switch (this) {
            case ARROW:
                return 3.9F;
            case SPEAR:
                return 3.15F;
            case HAMMER:
                return 0.80F;
            case ORB:
            default:
                return 1.65F;
        }
    }

    public float gravity() {
        switch (this) {
            case ARROW:
                return 0.0015F;
            case SPEAR:
                return 0.012F;
            case HAMMER:
                return 0.16F;
            case ORB:
            default:
                return 0.03F;
        }
    }

    public float width() {
        switch (this) {
            case ARROW:
                return 0.16F;
            case SPEAR:
                return 0.28F;
            case HAMMER:
                return 0.85F;
            case ORB:
            default:
                return 0.42F;
        }
    }

    public float height() {
        return width();
    }

    /** Тики полёта до самоуничтожения. Молот специально короткий. */
    public int maxLife() {
        switch (this) {
            case ARROW:
                return 90;
            case SPEAR:
                return 70;
            case HAMMER:
                return 22;
            case ORB:
            default:
                return 55;
        }
    }

    public float inaccuracy() {
        switch (this) {
            case ARROW:
                return 0.0F;
            case SPEAR:
                return 0.12F;
            case HAMMER:
                return 1.15F;
            case ORB:
            default:
                return 0.55F;
        }
    }

    /** Сколько ещё целей после первой. Только копьё. */
    public int extraPierce(float power) {
        if (this != SPEAR) {
            return 0;
        }
        return Math.max(1, (int) power);
    }

    /**
     * Радиус урона по площади. ORB — маленький хлопок, не усиливать.
     * HAMMER — врождённый удар по области (~2 блока на power 1, растёт с силой).
     */
    public float splashRadius(float power) {
        float p = Math.max(0.5F, power);
        switch (this) {
            case ORB:
                return 1.4F + 0.35F * p;
            case HAMMER:
                return 1.75F + 0.50F * p;
            default:
                return 0.0F;
        }
    }

    /** Доля силы для целей в сплэше (до множителя удара формы). */
    public float splashPowerFactor() {
        return this == HAMMER ? 0.60F : 0.45F;
    }

    /** Множитель отброса для целей в сплэше, не для основной. */
    public float splashKnockbackFactor() {
        return this == HAMMER ? 0.58F : 0.45F;
    }

    /** Подброс основной цели. Только молот. */
    public float slamLift(float power) {
        if (this != HAMMER) {
            return 0.0F;
        }
        return 0.45F * Math.max(0.5F, power);
    }

    public float hitPowerMultiplier() {
        switch (this) {
            case ARROW:
                return 1.2F;
            case SPEAR:
                return 1.05F;
            case HAMMER:
                return 1.3F;
            case ORB:
            default:
                return 0.9F;
        }
    }

    public float knockbackStrength(float power) {
        float p = Math.max(0.5F, power);
        switch (this) {
            case ARROW:
                return 0.08F * p;
            case SPEAR:
                return 0.35F * p;
            case HAMMER:
                return 2.15F * p;
            case ORB:
            default:
                return 0.28F * p;
        }
    }

    /** Доля собственного отброса стихии, которую оставляем после удара. */
    public float elementKnockbackRetain() {
        switch (this) {
            case ARROW:
                return 0.2F;
            case HAMMER:
                return 1.0F;
            default:
                return 0.7F;
        }
    }

    public float sizeScale(float power) {
        return 0.75F + 0.15F * Math.max(0.5F, power);
    }

    public boolean slamsOnExpire() {
        return this == HAMMER;
    }
}
