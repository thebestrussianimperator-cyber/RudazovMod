package com.poleesteel.rudazovmod.spell.form;

import com.poleesteel.rudazovmod.entities.EntitySpellProjectile;
import com.poleesteel.rudazovmod.spell.api.CastContext;
import com.poleesteel.rudazovmod.spell.api.CastMode;
import com.poleesteel.rudazovmod.spell.api.Form;
import com.poleesteel.rudazovmod.spell.api.FormHandler;
import com.poleesteel.rudazovmod.spell.api.SpellTarget;
import com.poleesteel.rudazovmod.spell.resolve.LookTrace;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;

public final class RayFormHandler implements FormHandler {

    @Override
    public Form form() {
        return Form.RAY;
    }

    @Override
    public void onStart(CastContext context) {
        if (context.world().isRemote) {
            return;
        }
        if (context.spell().castMode() == CastMode.INSTANT) {
            applyInstant(context);
        } else {
            applyBeam(context);
        }
    }

    @Override
    public void onTick(CastContext context) {
        if (context.world().isRemote) {
            return;
        }
        applyBeam(context);
    }

    @Override
    public void onEnd(CastContext context) {}

    private static void applyInstant(CastContext context) {
        SpellTarget target = context.target();
        if (target instanceof SpellTarget.EntityTarget entityTarget) {
            applyElement(context, entityTarget.find(context.world()));
            return;
        }
        EntitySpellProjectile projectile = new EntitySpellProjectile(
                context.world(), context.caster(), context.spell().element(), context.spell().power());
        context.world().spawnEntity(projectile);
    }

    private static void applyBeam(CastContext context) {
        spawnBeamParticles(context);
        if (context.ticksHeld() % 5 != 0) {
            return;
        }
        LookTrace.findEntity(context.caster(), e -> e instanceof EntityLivingBase && e.isEntityAlive())
                .ifPresent(entity -> applyElement(context, entity));
        LookTrace.findBlock(context.caster()).ifPresent(hit ->
                context.spell().element().onWorldHit(
                        context.world(), hit.getBlockPos(), hit.sideHit,
                        context.spell().power(), context.caster(), false));
    }

    private static void applyElement(CastContext context, Entity entity) {
        if (entity instanceof EntityLivingBase living && living != context.caster()) {
            context.spell().element().onHit(living, context.spell().power(), context.caster());
        }
    }

    private static void spawnBeamParticles(CastContext context) {
        if (!(context.world() instanceof WorldServer)) {
            return;
        }
        WorldServer world = (WorldServer) context.world();
        Vec3d eye = context.eyePos();
        Vec3d look = context.lookVec();
        EnumParticleTypes particle = context.spell().element().trailParticle();
        for (int i = 1; i <= 12; i++) {
            Vec3d pos = eye.add(look.x * i, look.y * i, look.z * i);
            world.spawnParticle(particle, pos.x, pos.y, pos.z, 2, 0.04D, 0.04D, 0.04D, 0.0D);
        }
    }
}
