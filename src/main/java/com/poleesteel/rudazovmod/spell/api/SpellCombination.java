package com.poleesteel.rudazovmod.spell.api;

/**
 * Законные пары осей (ARCHITECTURE §7.2) и то, что форма уже умеет кастовать.
 * GUI/craft показывают только {@link #canCast}, движок отвергает остальное.
 */
public final class SpellCombination {

    private SpellCombination() {}

    public static boolean isLegal(SpellDefinition spell) {
        return spell != null && isLegal(spell.form(), spell.targetType(), spell.castMode());
    }

    /**
     * Матрица конструктора, включая ещё недобитые {@code HOLD}+ITEM/BLOCK.
     */
    public static boolean isLegal(Form form, TargetType target, CastMode mode) {
        if (form == null || target == null || mode == null) {
            return false;
        }
        switch (form) {
            case RAY:
                if (target == TargetType.NONE) {
                    return mode == CastMode.INSTANT || mode == CastMode.CHANNEL;
                }
                if (target == TargetType.ENTITY) {
                    return mode == CastMode.INSTANT;
                }
                return false;
            case HOLD:
                if (mode != CastMode.CHANNEL) {
                    return false;
                }
                return target == TargetType.ENTITY
                        || target == TargetType.ITEM
                        || target == TargetType.BLOCK;
            default:
                return false;
        }
    }

    public static boolean isImplemented(SpellDefinition spell) {
        return spell != null && isImplemented(spell.form(), spell.targetType(), spell.castMode());
    }

    /**
     * Форма реально доставляет эффект. {@code HOLD}+ITEM/BLOCK пока no-op — не кастовать.
     */
    public static boolean isImplemented(Form form, TargetType target, CastMode mode) {
        if (!isLegal(form, target, mode)) {
            return false;
        }
        if (form == Form.HOLD) {
            return target == TargetType.ENTITY;
        }
        return true;
    }

    public static boolean canCast(SpellDefinition spell) {
        return isImplemented(spell);
    }

    public static boolean canCast(Form form, TargetType target, CastMode mode) {
        return isImplemented(form, target, mode);
    }
}
