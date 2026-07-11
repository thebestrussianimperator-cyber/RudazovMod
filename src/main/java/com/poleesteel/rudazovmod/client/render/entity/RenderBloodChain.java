package com.poleesteel.rudazovmod.client.render.entity;

import com.poleesteel.rudazovmod.entities.EntityBloodChain;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.culling.ICamera;

@SideOnly(Side.CLIENT)
public class RenderBloodChain extends Render<EntityBloodChain> {

    private static final ResourceLocation CHAIN_TEXTURE = new ResourceLocation("rudazovmod:textures/entity/blood_chain_link.png");

    public RenderBloodChain(RenderManager renderManager) {
        super(renderManager);
    }

    @Override
    public void doRender(EntityBloodChain entity, double x, double y, double z, float entityYaw, float partialTicks) {
        Entity target = entity.getTarget();
        if (target == null) return;

        Entity owner = entity.getOwner();

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);

        this.bindTexture(CHAIN_TEXTURE);
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        // --- 1. ОТРИСОВКА НАТЯНУТОЙ ЦЕПИ (ПОВОДОК К ИГРОКУ) ---
        if (owner != null && owner != target) {
            // Вычисляем точные координаты центра тела/рук игрока
            double ownerX = owner.prevPosX + (owner.posX - owner.prevPosX) * partialTicks;
            double ownerY = owner.prevPosY + (owner.posY - owner.prevPosY) * partialTicks + (owner.height * 0.5D);
            double ownerZ = owner.prevPosZ + (owner.posZ - owner.prevPosZ) * partialTicks;

            double targetX = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks;
            double targetY = entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks;
            double targetZ = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks;

            // Вектор от моба к игроку
            double dx = ownerX - targetX;
            double dy = ownerY - targetY;
            double dz = ownerZ - targetZ;

            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double horizDist = Math.sqrt(dx * dx + dz * dz);

            // ИСПРАВЛЕННАЯ МАТЕМАТИКА: Точное прицеливание в системе координат Minecraft
            float yaw = (float) (Math.atan2(dx, dz) * (180D / Math.PI));
            float pitch = (float) -(Math.atan2(dy, horizDist) * (180D / Math.PI));

            GlStateManager.pushMatrix();
            GlStateManager.rotate(yaw, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F);

            int leashLinks = Math.max(1, (int) (dist / 0.25D));
            float linkLen = (float) (dist / leashLinks);
            float w = 0.15F;

            GlStateManager.disableCull();
            for (int i = 0; i < leashLinks; i++) {
                GlStateManager.pushMatrix();
                GlStateManager.translate(0, 0, i * linkLen);
                if (i % 2 == 0) {
                    GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
                }
                buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
                buffer.pos(-w, 0, 0).tex(0, 0).endVertex();
                buffer.pos(w, 0, 0).tex(1, 0).endVertex();
                buffer.pos(w, 0, linkLen).tex(1, 1).endVertex();
                buffer.pos(-w, 0, linkLen).tex(0, 1).endVertex();
                tessellator.draw();
                GlStateManager.popMatrix();
            }
            GlStateManager.enableCull();
            GlStateManager.popMatrix();
        }

        // --- 2. ОТРИСОВКА СПИРАЛИ ВОКРУГ МОБА ---
        GlStateManager.disableCull();
        float radius = (float) Math.sqrt((target.width * target.width) * 2) / 2.0F + 0.15F;
        float height = target.height;
        int segments = 45;
        float loops = 2.5F;
        float timeOffset = (entity.ticksExisted + partialTicks) * 0.05F;

        for (int i = 0; i < segments; i++) {
            float t1 = (float) i / segments;
            float angle1 = t1 * (float) Math.PI * 2.0F * loops + timeOffset;
            float cx = (float) Math.cos(angle1) * radius;
            float cz = (float) Math.sin(angle1) * radius;
            float cy = (t1 * height) - (height / 2.0F);

            float t2 = (float) (i + 1) / segments;
            float angle2 = t2 * (float) Math.PI * 2.0F * loops + timeOffset;
            float nx = (float) Math.cos(angle2) * radius;
            float nz = (float) Math.sin(angle2) * radius;
            float ny = (t2 * height) - (height / 2.0F);

            double dx = nx - cx;
            double dy = ny - cy;
            double dz = nz - cz;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double horizDist = Math.sqrt(dx * dx + dz * dz);

            // Здесь тоже обновили формулу для идеальной стыковки звеньев
            float yaw = (float) (Math.atan2(dx, dz) * (180D / Math.PI));
            float pitch = (float) -(Math.atan2(dy, horizDist) * (180D / Math.PI));

            GlStateManager.pushMatrix();
            GlStateManager.translate(cx, cy, cz);
            GlStateManager.rotate(yaw, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F);

            if (i % 2 == 0) {
                GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
            }

            float w = 0.15F;
            float l = (float) dist * 1.5F;
            float offset = (float) dist * 0.25F;

            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            buffer.pos(-w, 0, -offset).tex(0, 0).endVertex();
            buffer.pos(w, 0, -offset).tex(1, 0).endVertex();
            buffer.pos(w, 0, l - offset).tex(1, 1).endVertex();
            buffer.pos(-w, 0, l - offset).tex(0, 1).endVertex();
            tessellator.draw();

            GlStateManager.popMatrix();
        }
        GlStateManager.enableCull();

        GlStateManager.enableLighting();
        GlStateManager.popMatrix();

        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityBloodChain entity) {
        return CHAIN_TEXTURE;
    }

    // Отключаем ванильное отсечение (Frustum Culling).
    // Теперь игра будет рисовать цепь всегда, даже если моб находится за спиной или на краю экрана!
    @Override
    public boolean shouldRender(EntityBloodChain livingEntity, ICamera camera, double camX, double camY, double camZ) {
        return true;
    }
}