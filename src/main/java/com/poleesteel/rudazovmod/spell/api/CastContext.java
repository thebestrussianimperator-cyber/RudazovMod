package com.poleesteel.rudazovmod.spell.api;

import com.github.bsideup.jabel.Desugar;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Objects;

/**
 * Снимок каста на один вызов resolve/onStart/onTick/onEnd.
 * Формы цель сами не ищут — она уже лежит в {@link #target()}.
 */
@Desugar
public record CastContext(
        EntityPlayer caster,
        SpellDefinition spell,
        SpellTarget target,
        int ticksHeld
) {
    public CastContext {
        Objects.requireNonNull(caster, "caster");
        Objects.requireNonNull(spell, "spell");
        Objects.requireNonNull(target, "target");
        if (ticksHeld < 0) {
            ticksHeld = 0;
        }
    }

    public static CastContext start(EntityPlayer caster, SpellDefinition spell) {
        return new CastContext(caster, spell, SpellTarget.none(), 0);
    }

    public World world() {
        return caster.world;
    }

    public Vec3d eyePos() {
        return caster.getPositionEyes(1.0F);
    }

    public Vec3d lookVec() {
        return caster.getLook(1.0F);
    }

    public CastContext withTarget(SpellTarget newTarget) {
        return new CastContext(caster, spell, newTarget, ticksHeld);
    }

    public CastContext withTicksHeld(int newTicksHeld) {
        return new CastContext(caster, spell, target, newTicksHeld);
    }
}
