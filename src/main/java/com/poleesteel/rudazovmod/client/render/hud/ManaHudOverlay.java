package com.poleesteel.rudazovmod.client.render.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import com.poleesteel.rudazovmod.capabilities.ActiveSpiritProvider;
import com.poleesteel.rudazovmod.capabilities.IActiveSpirit;
import com.poleesteel.rudazovmod.spell.api.SpellProgression;

import java.util.Locale;

@Mod.EventBusSubscriber(Side.CLIENT)
public class ManaHudOverlay {

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onRenderGui(RenderGameOverlayEvent.Post event) {
        // Отрисовываемся только во время рендера ТЕКСТА, чтобы полоска не дублировалась
        if (event.getType() != RenderGameOverlayEvent.ElementType.TEXT) return;

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null || player.isCreative()) return; // В креативе можно скрыть, или убрать проверку, если хочешь видеть всегда

        IActiveSpirit spirit = player.getCapability(ActiveSpiritProvider.ACTIVE_SPIRIT_CAP, null);
        if (spirit == null) return;

        float mana = spirit.getMana();
        float maxMana = spirit.getMaxMana();

        ScaledResolution res = event.getResolution();
        int width = res.getScaledWidth();
        int height = res.getScaledHeight();

        // Координаты: справа от центра (над полоской голода в 1.12.2)
        int x = width / 2 + 10;
        int y = height - 49;

        int barWidth = 81;
        int barHeight = 5;

        // Считаем ширину заполнения
        int filledWidth = (int) ((mana / maxMana) * barWidth);

        // 1. Фон полоски (чёрный полупрозрачный, ARGB)
        Gui.drawRect(x - 1, y - 1, x + barWidth + 1, y + barHeight + 1, 0xAA000000);

        // 2. Сама мана (магический синий цвет, ARGB: 0xFF0066FF)
        Gui.drawRect(x, y, x + filledWidth, y + barHeight, 0xFF0066FF);

        // 3. Текст по центру полоски с тенью (например: "50 / 100")
        String text = (int)mana + " / " + (int)maxMana;
        int textWidth = mc.fontRenderer.getStringWidth(text);

        // Смещаем текст чуть выше или прямо на полоску (на -1 пиксель по Y)
        mc.fontRenderer.drawStringWithShadow(text, x + (barWidth - textWidth) / 2.0F, y - 1, 0xFFFFFF);

        int stage = spirit.getChakraLevel();
        float development = spirit.getSpiritDevelopment();
        float next = SpellProgression.nextStageAt(stage);
        String stageText = Float.isInfinite(next)
                ? I18n.format("gui.rudazovmod.hud.stage", String.valueOf(stage))
                : I18n.format("gui.rudazovmod.hud.stage_next",
                        String.valueOf(stage),
                        String.format(Locale.ROOT, "%.0f", development),
                        String.format(Locale.ROOT, "%.0f", next));
        mc.fontRenderer.drawStringWithShadow(stageText, x + barWidth + 4, y - 1, 0xFFD080);

        if (!Float.isInfinite(next) && next > 0.0F) {
            float start = SpellProgression.stageStart(stage);
            float span = next - start;
            float filled = span <= 0.0F ? 0.0F : Math.max(0.0F, Math.min(1.0F, (development - start) / span));
            int pipH = 2;
            int pipY = y + barHeight + 1;
            Gui.drawRect(x - 1, pipY, x + barWidth + 1, pipY + pipH, 0xAA000000);
            Gui.drawRect(x, pipY, x + (int) (filled * barWidth), pipY + pipH, 0xFFFFD080);
        }
    }
}