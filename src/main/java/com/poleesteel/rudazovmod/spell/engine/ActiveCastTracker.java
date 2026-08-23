package com.poleesteel.rudazovmod.spell.engine;

import com.github.bsideup.jabel.Desugar;
import com.poleesteel.rudazovmod.spell.api.SpellDefinition;
import com.poleesteel.rudazovmod.spell.api.SpellTarget;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Серверное состояние CHANNEL-каста. Один активный каст на игрока.
 */
public final class ActiveCastTracker {

    @Desugar
    public record ActiveCast(UUID playerId, ResourceLocation spellId, SpellTarget target, int startTick) {}

    private final Map<UUID, ActiveCast> active = new HashMap<>();

    public void begin(EntityPlayer player, SpellDefinition spell, SpellTarget target) {
        active.put(player.getUniqueID(),
                new ActiveCast(player.getUniqueID(), spell.id(), target, player.ticksExisted));
    }

    public Optional<ActiveCast> get(EntityPlayer player) {
        return Optional.ofNullable(active.get(player.getUniqueID()));
    }

    public Optional<ActiveCast> remove(EntityPlayer player) {
        return Optional.ofNullable(active.remove(player.getUniqueID()));
    }

    public boolean isActive(EntityPlayer player) {
        return active.containsKey(player.getUniqueID());
    }
}
