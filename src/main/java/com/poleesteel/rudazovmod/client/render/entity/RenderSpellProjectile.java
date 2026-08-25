package com.poleesteel.rudazovmod.client.render.entity;

import com.poleesteel.rudazovmod.entities.EntitySpellProjectile;
import com.poleesteel.rudazovmod.spell.api.ProjectileShape;
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
        ProjectileShape shape = entity.getShape();
        float scale = 0.28F * entity.getShape().sizeScale(entity.getPower());
        int color = entity.getElement().getColor();
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y + entity.height * 0.5F, (float) z);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        switch (shape) {
            case ARROW:
                orientAlongFlight(entity, partialTicks);
                GlStateManager.scale(scale, scale, scale * 2.6F);
                drawBolt(buffer, tessellator, r, g, b, 0.10F, 1.05F, 210);
                break;
            case SPEAR:
                orientAlongFlight(entity, partialTicks);
                GlStateManager.scale(scale * 1.15F, scale * 1.15F, scale * 3.4F);
                drawBolt(buffer, tessellator, r, g, b, 0.14F, 1.25F, 220);
                break;
            case HAMMER:
                orientAlongFlight(entity, partialTicks);
                GlStateManager.rotate((entity.ticksExisted + partialTicks) * 14.0F, 1.0F, 0.2F, 0.0F);
                GlStateManager.scale(scale * 2.4F, scale * 2.4F, scale * 2.4F);
                drawHammer(buffer, tessellator, r, g, b);
                break;
            case ORB:
            default:
                GlStateManager.rotate(-this.renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(
                        (float) (this.renderManager.options.thirdPersonView == 2 ? -1 : 1)
                                * this.renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
                GlStateManager.scale(scale * 1.35F, scale * 1.35F, scale * 1.35F);
                drawOrb(buffer, tessellator, r, g, b);
                break;
        }

        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();

        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    private static void orientAlongFlight(EntitySpellProjectile entity, float partialTicks) {
        float yaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks;
        float pitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;
        GlStateManager.rotate(yaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-pitch, 1.0F, 0.0F, 0.0F);
    }

    private static void drawOrb(BufferBuilder buffer, Tessellator tessellator, int r, int g, int b) {
        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        quad(buffer, -0.55D, -0.55D, 0.0D, 0.55D, -0.55D, 0.0D, 0.55D, 0.55D, 0.0D, -0.55D, 0.55D, 0.0D, r, g, b, 80);
        quad(buffer, -0.32D, -0.32D, 0.01D, 0.32D, -0.32D, 0.01D, 0.32D, 0.32D, 0.01D, -0.32D, 0.32D, 0.01D, r, g, b, 220);
        int cr = Math.min(255, r + 40);
        int cg = Math.min(255, g + 40);
        int cb = Math.min(255, b + 40);
        quad(buffer, -0.14D, -0.14D, 0.02D, 0.14D, -0.14D, 0.02D, 0.14D, 0.14D, 0.02D, -0.14D, 0.14D, 0.02D, cr, cg, cb, 255);
        tessellator.draw();
    }

    private static void drawBolt(
            BufferBuilder buffer, Tessellator tessellator, int r, int g, int b, float half, float length, int alpha) {
        double w = half;
        double z0 = -length;
        double z1 = length;
        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        quad(buffer, -w, 0.0D, z0, w, 0.0D, z0, w, 0.0D, z1, -w, 0.0D, z1, r, g, b, alpha);
        quad(buffer, 0.0D, -w, z0, 0.0D, w, z0, 0.0D, w, z1, 0.0D, -w, z1, r, g, b, alpha);
        int cr = Math.min(255, r + 50);
        int cg = Math.min(255, g + 50);
        int cb = Math.min(255, b + 50);
        double tip = z1 + 0.35D;
        quad(buffer, -w * 1.6D, 0.0D, z1 - 0.15D, w * 1.6D, 0.0D, z1 - 0.15D, 0.0D, 0.0D, tip, 0.0D, 0.0D, tip, cr, cg, cb, 255);
        quad(buffer, 0.0D, -w * 1.6D, z1 - 0.15D, 0.0D, w * 1.6D, z1 - 0.15D, 0.0D, 0.0D, tip, 0.0D, 0.0D, tip, cr, cg, cb, 255);
        tessellator.draw();
    }

    private static void drawHammer(BufferBuilder buffer, Tessellator tessellator, int r, int g, int b) {
        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        box(buffer, -0.18D, -0.18D, -0.85D, 0.18D, 0.18D, 0.15D, r, g, b, 200);
        int darkR = (int) (r * 0.65F);
        int darkG = (int) (g * 0.65F);
        int darkB = (int) (b * 0.65F);
        box(buffer, -0.55D, -0.38D, 0.10D, 0.55D, 0.38D, 0.75D, darkR, darkG, darkB, 230);
        int cr = Math.min(255, r + 35);
        int cg = Math.min(255, g + 35);
        int cb = Math.min(255, b + 35);
        box(buffer, -0.22D, -0.22D, 0.22D, 0.22D, 0.22D, 0.62D, cr, cg, cb, 255);
        tessellator.draw();
    }

    private static void box(
            BufferBuilder buffer,
            double x0, double y0, double z0, double x1, double y1, double z1,
            int r, int g, int b, int a) {
        quad(buffer, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, r, g, b, a);
        quad(buffer, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, r, g, b, a);
        quad(buffer, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, r, g, b, a);
        quad(buffer, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1, r, g, b, a);
        quad(buffer, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, r, g, b, a);
        quad(buffer, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, r, g, b, a);
    }

    private static void quad(
            BufferBuilder buffer,
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            double x3, double y3, double z3,
            double x4, double y4, double z4,
            int r, int g, int b, int a) {
        buffer.pos(x1, y1, z1).color(r, g, b, a).endVertex();
        buffer.pos(x2, y2, z2).color(r, g, b, a).endVertex();
        buffer.pos(x3, y3, z3).color(r, g, b, a).endVertex();
        buffer.pos(x4, y4, z4).color(r, g, b, a).endVertex();
    }

    @Override
    protected ResourceLocation getEntityTexture(EntitySpellProjectile entity) {
        return null;
    }
}
