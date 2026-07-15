package com.poleesteel.rudazovmod.magic;

import com.poleesteel.rudazovmod.magic.spells.SpellTelekinesis;
import net.minecraft.util.ResourceLocation;
import java.util.HashMap;
import java.util.Map;

public class SpellRegistry {
    private static final Map<ResourceLocation, AbstractSpell> SPELLS = new HashMap<>();

    // Регистрируем спелл
    public static void register(AbstractSpell spell) {
        SPELLS.put(spell.getId(), spell);
    }

    // Ищем спелл по ID
    public static AbstractSpell getSpell(ResourceLocation id) {
        return SPELLS.get(id);
    }

    public static AbstractSpell getSpell(String idString) {
        return SPELLS.get(new ResourceLocation(idString));
    }

    // Вызывается при старте игры (в preInit)
    public static void init() {
        register(new SpellTelekinesis());
        // В будущем тут будет:
        // register(new SpellFireball());
        // register(new SpellHeal());
    }
}