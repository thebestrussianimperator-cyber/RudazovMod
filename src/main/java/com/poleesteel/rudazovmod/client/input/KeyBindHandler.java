package com.poleesteel.rudazovmod.client.input;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import com.poleesteel.rudazovmod.network.PacketHandler;
import com.poleesteel.rudazovmod.network.PacketUseTelekinesis;

@Mod.EventBusSubscriber(Side.CLIENT)
public class KeyBindHandler {

    // Регистрируем кнопку Z в настройках управления Minecraft
    public static final KeyBinding KEY_TELEKINESIS = new KeyBinding(
            "key.rudazovmod.telekinesis",
            Keyboard.KEY_Z,
            "key.categories.rudazovmod"
    );

    // Вызывается один раз при старте игры
    public static void init() {
        ClientRegistry.registerKeyBinding(KEY_TELEKINESIS);
    }

    // Проверяем нажатие клавиши каждый тик клиента
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null || mc.isGamePaused()) return;

        // Если игрок УДЕРЖИВАЕТ кнопку телекинеза
        if (KEY_TELEKINESIS.isKeyDown()) {
            // Чтобы не спамить сеть на 100%, отправляем пакет каждый 2-й тик (10 раз в секунду)
            if (mc.player.ticksExisted % 2 == 0) {
                PacketHandler.INSTANCE.sendToServer(new PacketUseTelekinesis());
            }
        }
    }
}