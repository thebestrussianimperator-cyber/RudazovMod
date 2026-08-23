package com.poleesteel.rudazovmod.spell.form;

import com.poleesteel.rudazovmod.spell.api.CastContext;
import com.poleesteel.rudazovmod.spell.api.Form;
import com.poleesteel.rudazovmod.spell.api.FormHandler;
import com.poleesteel.rudazovmod.spell.api.SpellTarget;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

/**
 * Удержание цели перед кастером. ITEM/BLOCK пока не двигаем — это отдельные TargetType.
 */
public final class HoldFormHandler implements FormHandler {

    private static final double HOLD_DISTANCE = 4.0D;
    private static final double PULL = 0.4D;

    @Override
    public Form form() {
        return Form.HOLD;
    }

    @Override
    public void onStart(CastContext context) {
        pull(context);
    }

    @Override
    public void onTick(CastContext context) {
        pull(context);
    }

    @Override
    public void onEnd(CastContext context) {}

    private static void pull(CastContext context) {
        if (context.world().isRemote) {
            return;
        }
        if (!(context.target() instanceof SpellTarget.EntityTarget entityTarget)) {
            return;
        }
        Entity entity = entityTarget.find(context.world());
        if (entity == null || !entity.isEntityAlive()) {
            return;
        }

        Vec3d eye = context.eyePos();
        Vec3d look = context.lookVec();
        Vec3d holdPos = eye.add(look.x * HOLD_DISTANCE, look.y * HOLD_DISTANCE, look.z * HOLD_DISTANCE);

        entity.motionX = (holdPos.x - entity.posX) * PULL;
        entity.motionY = (holdPos.y - entity.posY) * PULL;
        entity.motionZ = (holdPos.z - entity.posZ) * PULL;
        entity.isAirBorne = true;
        entity.fallDistance = 0.0F;
        entity.velocityChanged = true;
    }
}
