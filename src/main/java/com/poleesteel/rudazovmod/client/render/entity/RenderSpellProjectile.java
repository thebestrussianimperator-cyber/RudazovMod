package com.poleesteel.rudazovmod.client.render.entity;

import com.poleesteel.rudazovmod.entities.EntitySpellProjectile;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderSpellProjectile extends Render<EntitySpellProjectile> {

    public RenderSpellProjectile(RenderManager renderManager) {
        super(renderManager);
    }

    @Override
    public void doRender(EntitySpellProjectile entity, double x, double y, double z, float entityYaw, float partialTicks) {
        GlStateManager.pushMatrix();
        GlStateManager.translate((float)x, (float)y + 0.15F, (float)z);

        // Поворачиваем сгусток магии лицом к камере игрока (биллборд)
        GlStateManager.rotate(-this.renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate((float)(this.renderManager.options.thirdPersonView == 2 ? -1 : 1) * this.renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(0.3F, 0.3F, 0.3F);

        // Настройки неонового свечения
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

        int color = entity.getElement().getColor();
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        // Рисуем светящееся ядро снаряда
        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(-0.5D, -0.5D, 0.0D).color(r, g, b, 220).endVertex();
        buffer.pos( 0.5D, -0.5D, 0.0D).color(r, g, b, 220).endVertex();
        buffer.pos( 0.5D,  0.5D, 0.0D).color(r, g, b, 220).endVertex();
        buffer.pos(-0.5D,  0.5D, 0.0D).color(r, g, b, 220).endVertex();
        tessellator.draw();

        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();

        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntitySpellProjectile entity) {
        return null; // Текстура не нужна, всё рисует чистый код!
    }
}