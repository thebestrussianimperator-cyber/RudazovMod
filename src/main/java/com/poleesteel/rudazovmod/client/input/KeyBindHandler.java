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
import com.poleesteel.rudazovmod.capabilities.ActiveSpiritProvider;
import com.poleesteel.rudazovmod.capabilities.IActiveSpirit;
import com.poleesteel.rudazovmod.network.PacketCastSpell;
import com.poleesteel.rudazovmod.network.PacketHandler;

@Mod.EventBusSubscriber(Side.CLIENT)
public class KeyBindHandler {

    // Жестко инициализируем 4 слота под заклинания
    public static final KeyBinding SPELL_SLOT_1 = new KeyBinding("key.rudazovmod.slot1", Keyboard.KEY_Z, "key.categories.rudazovmod");
    public static final KeyBinding SPELL_SLOT_2 = new KeyBinding("key.rudazovmod.slot2", Keyboard.KEY_X, "key.categories.rudazovmod");
    public static final KeyBinding SPELL_SLOT_3 = new KeyBinding("key.rudazovmod.slot3", Keyboard.KEY_C, "key.categories.rudazovmod");
    public static final KeyBinding SPELL_SLOT_4 = new KeyBinding("key.rudazovmod.slot4", Keyboard.KEY_V, "key.categories.rudazovmod");

    public static void init() {
        // Регистрируем только строго существующие объекты
        registerSafe(SPELL_SLOT_1);
        registerSafe(SPELL_SLOT_2);
        registerSafe(SPELL_SLOT_3);
        registerSafe(SPELL_SLOT_4);
    }

    // Защита от NullPointerException в меню управления
    private static void registerSafe(KeyBinding key) {
        if (key != null) {
            ClientRegistry.registerKeyBinding(key);
        }
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null || mc.isGamePaused()) return;

        // Чтобы снаряды вылетали плавно (4 раза в секунду), проверяем каждый 5-й тик
        if (mc.player.ticksExisted % 5 == 0) {
            if (SPELL_SLOT_1.isKeyDown()) castFromSlot(0);
            else if (SPELL_SLOT_2.isKeyDown()) castFromSlot(1);
            else if (SPELL_SLOT_3.isKeyDown()) castFromSlot(2);
            else if (SPELL_SLOT_4.isKeyDown()) castFromSlot(3);
        }
    }

    private static void castFromSlot(int slotIndex) {
        // Просто передаем номер слота на сервер! Вся логика проверок теперь там.
        PacketHandler.INSTANCE.sendToServer(new PacketCastSpell(slotIndex));
    }
}