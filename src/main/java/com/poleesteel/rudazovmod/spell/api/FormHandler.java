package com.poleesteel.rudazovmod.spell.api;

public interface FormHandler {

    Form form();

    void onStart(CastContext context);

    void onTick(CastContext context);

    void onEnd(CastContext context);

    /** Цель в мире уже исчезла, но форма её ещё держит (вынутый блок). */
    default boolean isTargetStillHeld(CastContext context) {
        return false;
    }

    /** Отказ до списания маны и TRACKER.begin: незахватываемый блок и т.п. */
    default boolean canStart(CastContext context) {
        return true;
    }
}
