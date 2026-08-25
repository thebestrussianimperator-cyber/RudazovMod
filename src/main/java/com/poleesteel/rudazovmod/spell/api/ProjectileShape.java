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
                return 1.22F;
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

    public float splashRadius(float power) {
        float p = Math.max(0.5F, power);
        switch (this) {
            case ORB:
                return 1.4F + 0.35F * p;
            case HAMMER:
                return 1.0F + 0.25F * p;
            default:
                return 0.0F;
        }
    }

    public float splashPowerFactor() {
        return this == HAMMER ? 0.35F : 0.45F;
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
                return 1.85F * p;
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
