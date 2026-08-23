package com.poleesteel.rudazovmod.spell.form;

import com.poleesteel.rudazovmod.spell.api.CastContext;
import com.poleesteel.rudazovmod.spell.api.Form;
import com.poleesteel.rudazovmod.spell.api.FormHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.WorldServer;

/**
 * Доставка на кастера. Цели в мире нет — только {@code TargetType.NONE}.
 */
public final class SelfFormHandler implements FormHandler {

    @Override
    public Form form() {
        return Form.SELF;
    }

    @Override
    public void onStart(CastContext context) {
        if (context.world().isRemote) {
            return;
        }
        apply(context, true);
        spawnParticles(context);
    }

    @Override
    public void onTick(CastContext context) {
        if (context.world().isRemote) {
            return;
        }
        spawnParticles(context);
        if (context.ticksHeld() > 0 && context.ticksHeld() % 10 == 0) {
            apply(context, false);
        }
    }

    @Override
    public void onEnd(CastContext context) {}

    private static void apply(CastContext context, boolean burst) {
        EntityPlayer caster = context.caster();
        context.spell().element().onSelf(caster, context.spell().power(), burst);
    }

    private static void spawnParticles(CastContext context) {
        if (!(context.world() instanceof WorldServer)) {
            return;
        }
        EntityPlayer caster = context.caster();
        ((WorldServer) context.world()).spawnParticle(
                context.spell().element().trailParticle(),
                caster.posX, caster.posY + caster.height * 0.6D, caster.posZ,
                6, 0.35D, 0.4D, 0.35D, 0.0D);
    }
}
