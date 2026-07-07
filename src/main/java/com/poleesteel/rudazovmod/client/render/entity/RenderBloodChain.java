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

@SideOnly(Side.CLIENT)
public class RenderBloodChain extends Render<EntityBloodChain> {

    // Путь к нашей будущей текстуре одного звена цепи
    private static final ResourceLocation CHAIN_TEXTURE = new ResourceLocation("rudazovmod:textures/entity/blood_chain_link.png");

    public RenderBloodChain(RenderManager renderManager) {
        super(renderManager);
    }

    @Override
    public void doRender(EntityBloodChain entity, double x, double y, double z, float entityYaw, float partialTicks) {
        Entity target = entity.getTarget();
        if (target == null) return;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);

        // Включаем текстуры и отключаем освещение, чтобы цепь "светилась" зловещим красным
        this.bindTexture(CHAIN_TEXTURE);
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F); // Цвет берется напрямую из текстуры

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        // --- ПРОЦЕДУРНАЯ ГЕОМЕТРИЯ 3D-ЗВЕНЬЕВ ---

        // 1. Считаем радиус на основе ДИАГОНАЛИ моба + небольшой отступ, чтобы не проваливаться в углы
        float radius = (float) Math.sqrt((target.width * target.width) * 2) / 2.0F + 0.15F;
        float height = target.height;
        int segments = 30; // Количество звеньев цепи
        float loops = 2.5F; // Количество витков вокруг моба
        float timeOffset = (entity.ticksExisted + partialTicks) * 0.05F; // Скорость вращения

        for (int i = 0; i < segments; i++) {
            // Текущая точка спирали
            float t1 = (float) i / segments;
            float angle1 = t1 * (float) Math.PI * 2.0F * loops + timeOffset;
            float cx = (float) Math.cos(angle1) * radius;
            float cz = (float) Math.sin(angle1) * radius;
            float cy = (t1 * height) - (height / 2.0F);

            // Следующая точка спирали (куда должно смотреть звено)
            float t2 = (float) (i + 1) / segments;
            float angle2 = t2 * (float) Math.PI * 2.0F * loops + timeOffset;
            float nx = (float) Math.cos(angle2) * radius;
            float nz = (float) Math.sin(angle2) * radius;
            float ny = (t2 * height) - (height / 2.0F);

            // Вычисляем вектор направления
            double dx = nx - cx;
            double dy = ny - cy;
            double dz = nz - cz;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double horizDist = Math.sqrt(dx * dx + dz * dz);

            // Вычисляем углы поворота (Yaw и Pitch), чтобы звено смотрело точно на следующую точку
            float yaw = (float) (Math.atan2(dz, dx) * (180D / Math.PI)) - 90.0F;
            float pitch = (float) -(Math.atan2(dy, horizDist) * (180D / Math.PI));

            GlStateManager.pushMatrix();

            // ОТКЛЮЧАЕМ CULLING (чтобы текстуру было видно с обеих сторон)
            GlStateManager.disableCull();
            // Включаем поддержку полупрозрачности (альфа-канала) для PNG
            GlStateManager.enableBlend();
            GlStateManager.alphaFunc(516, 0.1F);
            // Перемещаемся в текущую точку
            GlStateManager.translate(cx, cy, cz);
            // Поворачиваем звено в сторону следующей точки
            GlStateManager.rotate(yaw, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F);

            // МАГИЯ: Каждое четное звено поворачиваем на 90 градусов по оси Z, чтобы они цеплялись друг за друга
            if (i % 2 == 0) {
                GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
            }

            // Рисуем 3D плоскость (одно звено)
            // МАГИЯ НАХЛЁСТА:
            float w = 0.15F; // Ширина звена
            // Делаем звено в 1.5 раза длиннее расстояния между точками
            float l = (float) dist * 1.5F;
            // И сдвигаем его немного назад, чтобы оно "надевалось" на предыдущее
            float offset = (float) dist * 0.25F;

            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            // Обрати внимание, мы используем offset по оси Z
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
}