package com.poleesteel.rudazovmod.spell.engine;

import com.poleesteel.rudazovmod.Tags;
import com.poleesteel.rudazovmod.spell.api.CastMode;
import com.poleesteel.rudazovmod.spell.api.Form;
import com.poleesteel.rudazovmod.spell.api.SpellDefinition;
import com.poleesteel.rudazovmod.spell.api.SpellElement;
import com.poleesteel.rudazovmod.spell.api.TargetType;
import net.minecraft.util.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Реестр определений. Уникальные scripted-спеллы сюда не класть.
 */
public final class SpellRegistry {

    private static final Map<ResourceLocation, SpellDefinition> SPELLS = new HashMap<>();

    private SpellRegistry() {}

    public static void register(SpellDefinition spell) {
        SPELLS.put(spell.id(), spell);
    }

    /** Тестовые composed-записи, чтобы проверить INSTANT/CHANNEL без уникальных классов. */
    public static void registerDefaults() {
        register(new SpellDefinition(
                new ResourceLocation(Tags.MODID, "test_ray"),
                CastMode.INSTANT, TargetType.NONE, Form.RAY, SpellElement.FIRE, 2.0F, 15.0F));
        register(new SpellDefinition(
                new ResourceLocation(Tags.MODID, "test_ice"),
                CastMode.INSTANT, TargetType.NONE, Form.RAY, SpellElement.ICE, 2.0F, 15.0F));
        register(new SpellDefinition(
                new ResourceLocation(Tags.MODID, "test_beam"),
                CastMode.CHANNEL, TargetType.NONE, Form.RAY, SpellElement.FIRE, 1.0F, 0.5F));
        register(new SpellDefinition(
                new ResourceLocation(Tags.MODID, "test_hold"),
                CastMode.CHANNEL, TargetType.ENTITY, Form.HOLD, SpellElement.EARTH, 1.0F, 0.5F));
    }

    public static Optional<SpellDefinition> get(ResourceLocation id) {
        return Optional.ofNullable(SPELLS.get(id));
    }

    public static Optional<SpellDefinition> get(String id) {
        if (id == null || id.isEmpty()) {
            return Optional.empty();
        }
        ResourceLocation location = id.indexOf(':') >= 0
                ? new ResourceLocation(id)
                : new ResourceLocation(Tags.MODID, id);
        return get(location);
    }

    public static Collection<SpellDefinition> all() {
        return Collections.unmodifiableCollection(SPELLS.values());
    }
}
