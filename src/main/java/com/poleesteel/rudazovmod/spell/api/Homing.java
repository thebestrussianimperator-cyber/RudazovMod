package com.poleesteel.rudazovmod.spell.api;

/**
 * Самонаведение снаряда {@code RAY + INSTANT + NONE}. Не отдельная сущность и не класс спелла.
 * Для остальных комбинаций в определении всегда {@link #NONE}.
 * Даже {@link #STRONG} ограничен углом, дистанцией и линией видимости — не ракета.
 */
public enum Homing {
    NONE,
    WEAK,
    STRONG;

    /** Множитель маны поверх формы снаряда. NONE — база. */
    public float manaMultiplier() {
        switch (this) {
            case WEAK:
                return 1.30F;
            case STRONG:
                return 1.70F;
            case NONE:
            default:
                return 1.0F;
        }
    }

    /** Максимальный поворот за тик, градусы. */
    public float maxTurnDegrees() {
        switch (this) {
            case WEAK:
                return 8.0F;
            case STRONG:
                return 18.0F;
            case NONE:
            default:
                return 0.0F;
        }
    }

    /** Радиус захвата цели, блоки. */
    public double acquireRange() {
        switch (this) {
            case WEAK:
                return 16.0D;
            case STRONG:
                return 28.0D;
            case NONE:
            default:
                return 0.0D;
        }
    }

    /**
     * Минимальный косинус угла между скоростью и направлением на цель.
     * WEAK ~100° конус, STRONG ~160° — прямо назад не берём.
     */
    public double minForwardDot() {
        switch (this) {
            case WEAK:
                return 0.643D;
            case STRONG:
                return 0.174D;
            case NONE:
            default:
                return 1.0D;
        }
    }

    /** Тики прямого полёта до включения наведения. */
    public int startDelayTicks() {
        switch (this) {
            case WEAK:
                return 4;
            case STRONG:
                return 2;
            case NONE:
            default:
                return Integer.MAX_VALUE;
        }
    }

    /** Ступень чакр, с которой ось доступна. */
    public int requiredChakra() {
        switch (this) {
            case WEAK:
                return 2;
            case STRONG:
                return 3;
            case NONE:
            default:
                return 1;
        }
    }
}
