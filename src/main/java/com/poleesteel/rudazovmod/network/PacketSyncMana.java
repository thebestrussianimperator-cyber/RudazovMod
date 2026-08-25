package com.poleesteel.rudazovmod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import com.poleesteel.rudazovmod.capabilities.ActiveSpiritProvider;
import com.poleesteel.rudazovmod.capabilities.IActiveSpirit;

public class PacketSyncMana implements IMessage {
    private float mana;
    private float maxMana;
    private float spiritDevelopment;

    public PacketSyncMana() {} // Обязателен пустой конструктор для Forge!

    public PacketSyncMana(float mana, float maxMana, float spiritDevelopment) {
        this.mana = mana;
        this.maxMana = maxMana;
        this.spiritDevelopment = spiritDevelopment;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.mana = buf.readFloat();
        this.maxMana = buf.readFloat();
        this.spiritDevelopment = buf.readFloat();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeFloat(this.mana);
        buf.writeFloat(this.maxMana);
        buf.writeFloat(this.spiritDevelopment);
    }

    public static void sendTo(EntityPlayer player) {
        if (!(player instanceof EntityPlayerMP)) {
            return;
        }
        IActiveSpirit spirit = player.getCapability(ActiveSpiritProvider.ACTIVE_SPIRIT_CAP, null);
        if (spirit == null) {
            return;
        }
        PacketHandler.INSTANCE.sendTo(
                new PacketSyncMana(spirit.getMana(), spirit.getMaxMana(), spirit.getSpiritDevelopment()),
                (EntityPlayerMP) player);
    }

    public static class Handler implements IMessageHandler<PacketSyncMana, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketSyncMana message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                EntityPlayer player = Minecraft.getMinecraft().player;
                if (player != null) {
                    IActiveSpirit spirit = player.getCapability(ActiveSpiritProvider.ACTIVE_SPIRIT_CAP, null);
                    if (spirit != null) {
                        spirit.setMaxMana(message.maxMana);
                        spirit.setMana(message.mana);
                        spirit.setSpiritDevelopment(message.spiritDevelopment);
                    }
                }
            });
            return null;
        }
    }
}
