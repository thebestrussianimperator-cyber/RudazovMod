package com.poleesteel.rudazovmod.spell.engine;

import com.poleesteel.rudazovmod.capabilities.ActiveSpiritProvider;
import com.poleesteel.rudazovmod.capabilities.IActiveSpirit;
import com.poleesteel.rudazovmod.network.PacketSyncSpirit;
import com.poleesteel.rudazovmod.spell.api.CastMode;
import com.poleesteel.rudazovmod.spell.api.Form;
import com.poleesteel.rudazovmod.spell.api.SpellCombination;
import com.poleesteel.rudazovmod.spell.api.SpellDefinition;
import com.poleesteel.rudazovmod.spell.api.ProjectileShape;
import com.poleesteel.rudazovmod.spell.api.SpellElement;
import com.poleesteel.rudazovmod.spell.api.SpellProgression;
import com.poleesteel.rudazovmod.spell.api.TargetType;
import net.minecraft.entity.player.EntityPlayer;

import java.util.Optional;

/**
 * Сборка и привязка на сервере. GUI и команда ходят сюда, не в обход движка.
 */
public final class SpellBook {

    public static final float MIN_POWER = 0.25F;
    public static final float MAX_POWER = 10.0F;

    private SpellBook() {}

    public static Optional<SpellDefinition> craft(
            EntityPlayer player,
            CastMode mode,
            TargetType target,
            Form form,
            SpellElement element,
            float power,
            int bindSlot) {
        return craft(player, mode, target, form, element, power, ProjectileShape.ORB, bindSlot);
    }

    public static Optional<SpellDefinition> craft(
            EntityPlayer player,
            CastMode mode,
            TargetType target,
            Form form,
            SpellElement element,
            float power,
            ProjectileShape shape,
            int bindSlot) {
        if (player == null || player.world.isRemote) {
            return Optional.empty();
        }
        if (mode == null || target == null || form == null || element == null) {
            return Optional.empty();
        }
        if (!SpellCombination.canCast(form, target, mode)) {
            return Optional.empty();
        }
        if (Float.isNaN(power) || Float.isInfinite(power) || power < MIN_POWER) {
            return Optional.empty();
        }
        if (power > MAX_POWER) {
            power = MAX_POWER;
        }
        ProjectileShape resolved = shape == null ? ProjectileShape.ORB : shape;
        if (!SpellCombination.usesProjectileShape(form, target, mode)) {
            resolved = ProjectileShape.ORB;
        }

        IActiveSpirit spirit = player.getCapability(ActiveSpiritProvider.ACTIVE_SPIRIT_CAP, null);
        if (spirit == null) {
            return Optional.empty();
        }
        float cap = SpellProgression.maxPower(spirit.getChakraLevel());
        if (power > cap) {
            power = cap;
        }
        if (!SpellProgression.meetsChakra(spirit.getChakraLevel(), form, target, mode, element, power, resolved)) {
            return Optional.empty();
        }

        SpellDefinition spell = SpellDefinition.createCustom(mode, target, form, element, power, resolved);
        spirit.putSpell(spell);
        spirit.unlockSpell(spell.id().toString());
        if (bindSlot >= 0 && bindSlot < 4) {
            spirit.bindSpell(bindSlot, spell.id().toString());
        }
        PacketSyncSpirit.sendTo(player);
        return Optional.of(spell);
    }

    public static boolean bind(EntityPlayer player, int slot, String rawId) {
        if (player == null || player.world.isRemote || slot < 0 || slot > 3) {
            return false;
        }
        IActiveSpirit spirit = player.getCapability(ActiveSpiritProvider.ACTIVE_SPIRIT_CAP, null);
        if (spirit == null) {
            return false;
        }
        Optional<SpellDefinition> spell = SpellEngine.findDefinition(player, rawId);
        if (!spell.isPresent()) {
            return false;
        }
        String id = spell.get().id().toString();
        if (!spirit.ownsSpell(id)) {
            return false;
        }
        spirit.bindSpell(slot, id);
        PacketSyncSpirit.sendTo(player);
        return true;
    }
}
