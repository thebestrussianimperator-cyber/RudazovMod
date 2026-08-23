package com.poleesteel.rudazovmod.spell.api;

public interface FormHandler {

    Form form();

    void onStart(CastContext context);

    void onTick(CastContext context);

    void onEnd(CastContext context);
}
