package com.poleesteel.rudazovmod.network;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class PacketHandler {
    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel("rudazov_net");

    public static void init() {
        int id = 0;
        // Регистрируем наш пакет: он летит на СТОРОНУ КЛИЕНТА (Side.CLIENT)
        INSTANCE.registerMessage(PacketSyncMana.Handler.class, PacketSyncMana.class, id++, Side.CLIENT);
        // Клиент -> Сервер (телекинез)
        INSTANCE.registerMessage(PacketUseTelekinesis.Handler.class, PacketUseTelekinesis.class, id++, Side.SERVER);
    }
}