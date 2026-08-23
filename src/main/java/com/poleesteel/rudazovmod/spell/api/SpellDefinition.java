package com.poleesteel.rudazovmod.spell.api;

import com.github.bsideup.jabel.Desugar;
import net.minecraft.util.ResourceLocation;

import java.util.Objects;

/**
 * Данные заклинания в реестре. Поведение задаётся осями, не Java-классом спелла.
 */
@Desugar
public record SpellDefinition(
        ResourceLocation id,
        CastMode castMode,
        TargetType targetType,
        Form form,
        SpellElement element,
        float power,
        float cost
) {
    public SpellDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(castMode, "castMode");
        Objects.requireNonNull(targetType, "targetType");
        Objects.requireNonNull(form, "form");
        Objects.requireNonNull(element, "element");
    }
}
