package com.poleesteel.rudazovmod.magic;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import com.poleesteel.rudazovmod.capabilities.ActiveSpiritProvider;
import com.poleesteel.rudazovmod.capabilities.IActiveSpirit;

import java.util.List;

public class TelekinesisLogic {

    /**
     * Главный метод телекинеза: вызывается на сервере, когда игрок удерживает кнопку
     */
    public static void useTelekinesis(EntityPlayer player) {
        IActiveSpirit spirit = player.getCapability(ActiveSpiritProvider.ACTIVE_SPIRIT_CAP, null);
        if (spirit == null || spirit.getMana() <= 0) return;

        // 1. Ищем моба, на которого смотрит игрок (в радиусе 16 блоков)
        double range = 16.0D;
        Entity target = findTargetEntity(player, range);

        if (target != null) {
            // 2. ЛОР РУДАЗОВА: Расчет стоимости маны зависит от габаритов (массы) цели!
            // Курица (0.4 * 0.7) потратит ~0.5 маны/тик. Голем (1.4 * 2.7) потратит ~5 маны/тик!
            float massCost = (float) (target.width * target.height) * 1.2F;
            float manaCost = Math.max(0.5F, massCost); // Не меньше 0.5 маны за тик

            // 3. Если маны хватает — сжигаем её и двигаем моба!
            if (spirit.consumeMana(manaCost)) {
                grabAndHoldEntity(player, target);
            }
        }
    }

    /**
     * Физика "Невидимой руки": удерживаем моба в 4 блоках перед глазами игрока
     */
    private static void grabAndHoldEntity(EntityPlayer player, Entity target) {
        Vec3d eyePos = player.getPositionEyes(1.0F);
        Vec3d lookVec = player.getLook(1.0F);

        // Точка в пространстве, куда мы тянем жертву (4 блока перед лицом игрока)
        Vec3d holdPos = eyePos.add(lookVec.x * 4.0D, lookVec.y * 4.0D, lookVec.z * 4.0D);

        // Вычисляем вектор тяги (разницу между точкой удержания и текущей позицией моба)
        double dx = holdPos.x - target.posX;
        double dy = holdPos.y - target.posY;
        double dz = holdPos.z - target.posZ;

        // Придаем скорость (коэффициент 0.4 делает полет плавным, без резких дерганий)
        target.motionX = dx * 0.4D;
        target.motionY = dy * 0.4D;
        target.motionZ = dz * 0.4D;

        // Сбрасываем дистанцию падения, чтобы моб не разбился насмерть сразу после отпускания
        target.fallDistance = 0.0F;

        // ВАЖНО ДЛЯ FORGE 1.12.2: сообщаем серверу, что скорость сущности изменилась принудительно,
        // чтобы он мгновенно отправил новые координаты клиентам!
        target.velocityChanged = true;
    }

    /**
     * Серверный RayTrace: сканирует вектор взгляда игрока в поисках живых мишеней
     */
    private static Entity findTargetEntity(EntityPlayer player, double range) {
        Vec3d eyePos = player.getPositionEyes(1.0F);
        Vec3d lookVec = player.getLook(1.0F);
        Vec3d endPos = eyePos.add(lookVec.x * range, lookVec.y * range, lookVec.z * range);

        List<Entity> list = player.world.getEntitiesWithinAABBExcludingEntity(player,
                player.getEntityBoundingBox().expand(lookVec.x * range, lookVec.y * range, lookVec.z * range).grow(1.0D));

        Entity closest = null;
        double minDistance = range * range;

        for (Entity entity : list) {
            if (entity.canBeCollidedWith() || entity instanceof EntityLivingBase) {
                AxisAlignedBB aabb = entity.getEntityBoundingBox().grow(0.3D);
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