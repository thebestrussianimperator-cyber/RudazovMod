package com.poleesteel.rudazovmod.spell.engine;

import com.poleesteel.rudazovmod.RudazovMod;
import com.poleesteel.rudazovmod.Tags;
import com.poleesteel.rudazovmod.spell.api.CastMode;
import com.poleesteel.rudazovmod.spell.api.Form;
import com.poleesteel.rudazovmod.spell.api.SpellCombination;
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
        if (!SpellCombination.canCast(spell)) {
            RudazovMod.LOGGER.error("[spell] Пропуск некастуемой записи {}", spell.id());
            return;
        }
        SPELLS.put(spell.id(), spell);
    }

    /** Тестовые composed-записи, чтобы проверить INSTANT/CHANNEL без уникальных классов. */
    public static void registerDefaults() {
        register(new SpellDefinition(
                new ResourceLocation(Tags.MODID, "test_ray"),
                CastMode.INSTANT, TargetType.NONE, Form.RAY, SpellElement.FIRE, 2.0F));
        register(new SpellDefinition(
                new ResourceLocation(Tags.MODID, "test_ice"),
                CastMode.INSTANT, TargetType.NONE, Form.RAY, SpellElement.ICE, 2.0F));
        register(new SpellDefinition(
                new ResourceLocation(Tags.MODID, "test_beam"),
                CastMode.CHANNEL, TargetType.NONE, Form.RAY, SpellElement.FIRE, 1.0F));
        register(new SpellDefinition(
                new ResourceLocation(Tags.MODID, "test_hold"),
                CastMode.CHANNEL, TargetType.ENTITY, Form.HOLD, SpellElement.EARTH, 1.0F));
        register(new SpellDefinition(
                new ResourceLocation(Tags.MODID, "test_hold_item"),
                CastMode.CHANNEL, TargetType.ITEM, Form.HOLD, SpellElement.EARTH, 1.0F));
        register(new SpellDefinition(
                new ResourceLocation(Tags.MODID, "test_hold_block"),
                CastMode.CHANNEL, TargetType.BLOCK, Form.HOLD, SpellElement.EARTH, 1.0F));
        register(new SpellDefinition(
                new ResourceLocation(Tags.MODID, "test_heal"),
                CastMode.INSTANT, TargetType.ENTITY, Form.RAY, SpellElement.LIFE, 2.0F));
        register(new SpellDefinition(
                new ResourceLocation(Tags.MODID, "test_drain"),
                CastMode.CHANNEL, TargetType.ENTITY, Form.HOLD, SpellElement.LIFE, 1.0F));
        register(new SpellDefinition(
                new ResourceLocation(Tags.MODID, "test_self"),
                CastMode.INSTANT, TargetType.NONE, Form.SELF, SpellElement.LIFE, 2.0F));
        register(new SpellDefinition(
                new ResourceLocation(Tags.MODID, "test_self_ward"),
                CastMode.CHANNEL, TargetType.NONE, Form.SELF, SpellElement.FIRE, 1.0F));
    }

    public static Optional<SpellDefinition> get(ResourceLocation id) {
        return Optional.ofNullable(SPELLS.get(id));
    }

    public static Optional<SpellDefinition> get(String id) {
        if (id == null || id.isEmpty()) {
            return Optional.empty();
        }
        try {
            return get(SpellDefinition.parseId(id));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public static Collection<SpellDefinition> all() {
        return Collections.unmodifiableCollection(SPELLS.values());
    }
}
