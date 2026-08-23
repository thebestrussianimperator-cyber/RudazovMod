package com.poleesteel.rudazovmod.network;

import com.poleesteel.rudazovmod.capabilities.ActiveSpiritProvider;
import com.poleesteel.rudazovmod.capabilities.ActiveSpiritStorage;
import com.poleesteel.rudazovmod.capabilities.IActiveSpirit;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * S→C: unlock, bind и гримуар. Не каждый тик — только когда книга меняется или клиент пересобирает игрока.
 */
public class PacketSyncSpirit implements IMessage {

    private NBTTagCompound tag = new NBTTagCompound();

    public PacketSyncSpirit() {}

    public PacketSyncSpirit(IActiveSpirit spirit) {
        this.tag = ActiveSpiritStorage.writeBookNBT(spirit);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        NBTTagCompound read = ByteBufUtils.readTag(buf);
        this.tag = read != null ? read : new NBTTagCompound();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeTag(buf, this.tag != null ? this.tag : new NBTTagCompound());
    }

    public static void sendTo(EntityPlayer player) {
        if (!(player instanceof EntityPlayerMP)) {
            return;
        }
        IActiveSpirit spirit = player.getCapability(ActiveSpiritProvider.ACTIVE_SPIRIT_CAP, null);
        if (spirit == null) {
            return;
        }
        PacketHandler.INSTANCE.sendTo(new PacketSyncSpirit(spirit), (EntityPlayerMP) player);
    }

    public static class Handler implements IMessageHandler<PacketSyncSpirit, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketSyncSpirit message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                EntityPlayer player = Minecraft.getMinecraft().player;
                if (player == null) {
                    return;
                }
                IActiveSpirit spirit = player.getCapability(ActiveSpiritProvider.ACTIVE_SPIRIT_CAP, null);
                if (spirit != null) {
                    ActiveSpiritStorage.replaceBook(spirit, message.tag);
                }
            });
            return null;
        }
    }
}
