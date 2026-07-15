package com.poleesteel.rudazovmod.magic.spells;

import com.poleesteel.rudazovmod.capabilities.ActiveSpiritProvider;
import com.poleesteel.rudazovmod.capabilities.IActiveSpirit;
import com.poleesteel.rudazovmod.magic.AbstractSpell;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class SpellTelekinesis extends AbstractSpell {

    public SpellTelekinesis() {
        super("telekinesis", "Телекинез");
    }

    /**
     * 1. ВАЖНО: Заклинание сработает ТОЛЬКО если мы реально смотрим на цель!
     * Если цель не в прицеле, мана тратиться не будет.
     */
    @Override
    public boolean canCast(EntityPlayer player) {
        Entity target = findTargetEntity(player, 16.0D);
        if (target == null) return false; // Нет цели — нет каста!

        IActiveSpirit spirit = player.getCapability(ActiveSpiritProvider.ACTIVE_SPIRIT_CAP, null);
        return spirit != null && spirit.getMana() >= getManaCost(player);
    }

    @Override
    public float getManaCost(EntityPlayer player) {
        Entity target = findTargetEntity(player, 16.0D);
        if (target != null) {
            // ЛОР РУДАЗОВА: чем крупнее и тяжелее цель, тем больше маны она жрет
            return Math.max(0.5F, (float) (target.width * target.height) * 1.2F);
        }
        return 0.5F;
    }

    @Override
    public void onCast(EntityPlayer player) {
        Entity target = findTargetEntity(player, 16.0D);
        if (target != null) {
            Vec3d eyePos = player.getPositionEyes(1.0F);
            Vec3d lookVec = player.getLook(1.0F);

            // Точка левитации: ровно в 4 блоках перед глазами игрока
            Vec3d holdPos = eyePos.add(lookVec.x * 4.0D, lookVec.y * 4.0D, lookVec.z * 4.0D);

            double dx = holdPos.x - target.posX;
            double dy = holdPos.y - target.posY;
            double dz = holdPos.z - target.posZ;

            // Придаем вектор тяги к точке перед игроком
            target.motionX = dx * 0.4D;
            target.motionY = dy * 0.4D;
            target.motionZ = dz * 0.4D;

            // === КРИТИЧЕСКИ ВАЖНЫЕ ФЛАГИ ФИЗИКИ ДЛЯ 1.12.2 ===
            target.isAirBorne = true;       // Отключаем сопротивление гравитации и ИИ
            target.fallDistance = 0.0F;     // Чтобы моб не разбился после отпускания
            target.velocityChanged = true;  // Заставляем сервер синхронизировать полет с клиентом
        }
    }

    /**
     * Поиск сущности по взгляду (RayTrace).
     */
    private Entity findTargetEntity(EntityPlayer player, double range) {
        Vec3d eyePos = player.getPositionEyes(1.0F);
        Vec3d lookVec = player.getLook(1.0F);
        Vec3d endPos = eyePos.add(lookVec.x * range, lookVec.y * range, lookVec.z * range);

        // Создаем область поиска перед игроком
        AxisAlignedBB searchBox = player.getEntityBoundingBox()
                .expand(lookVec.x * range, lookVec.y * range, lookVec.z * range)
                .grow(1.0D);

        List<Entity> list = player.world.getEntitiesWithinAABBExcludingEntity(player, searchBox);

        Entity closest = null;
        double minDistance = range * range;

        for (Entity entity : list) {
            if (entity.canBeCollidedWith() || entity instanceof EntityLivingBase) {
                // Чуть увеличиваем хитбокс моба (на 0.5), чтобы магией было легче прицелиться
                AxisAlignedBB aabb = entity.getEntityBoundingBox().grow(0.5D);
                RayTraceResult result = aabb.calculateIntercept(eyePos, endPos);

                if (result != null) {
                    double dist = eyePos.squareDistanceTo(result.hitVec);
                    if (dist < minDistance) {
                        minDistance = dist;
                        closest = entity;
                    }
                }
            }
        }
        return closest;
    }
}