package com.poleesteel.rudazovmod.spell.api;

/**
 * Ровно один тип цели на заклинание. ITEM не схлопывается в ENTITY.
 */
public enum TargetType {
    NONE,
    ENTITY,
    ITEM,
    BLOCK
}
