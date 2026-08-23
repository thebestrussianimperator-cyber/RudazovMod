package com.poleesteel.rudazovmod.network;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class PacketHandler {
    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel("rudazov_net");

    public static void init() {
        int id = 0;
        INSTANCE.registerMessage(PacketSyncMana.Handler.class, PacketSyncMana.class, id++, Side.CLIENT);
        INSTANCE.registerMessage(PacketCastSpell.Handler.class, PacketCastSpell.class, id++, Side.SERVER);
        INSTANCE.registerMessage(PacketStopCast.Handler.class, PacketStopCast.class, id++, Side.SERVER);
        INSTANCE.registerMessage(PacketSyncSpirit.Handler.class, PacketSyncSpirit.class, id++, Side.CLIENT);
        INSTANCE.registerMessage(PacketCraftSpell.Handler.class, PacketCraftSpell.class, id++, Side.SERVER);
        INSTANCE.registerMessage(PacketBindSpell.Handler.class, PacketBindSpell.class, id++, Side.SERVER);
    }
}