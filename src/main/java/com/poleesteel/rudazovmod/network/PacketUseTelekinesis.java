package com.poleesteel.rudazovmod.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import com.poleesteel.rudazovmod.magic.TelekinesisLogic;

public class PacketUseTelekinesis implements IMessage {

    public PacketUseTelekinesis() {} // Пустой конструктор

    @Override public void fromBytes(ByteBuf buf) {}
    @Override public void toBytes(ByteBuf buf) {}

    // Обработчик пакета: что делает СЕРВЕР, когда получает сигнал от клиента
    public static class Handler implements IMessageHandler<PacketUseTelekinesis, IMessage> {
        @Override
        public IMessage onMessage(PacketUseTelekinesis message, MessageContext ctx) {
            // Получаем игрока, который отправил пакет
            EntityPlayerMP player = ctx.getServerHandler().player;

            // Выполняем магию строго в основном потоке сервера
            player.getServerWorld().addScheduledTask(() -> {
                TelekinesisLogic.useTelekinesis(player);
            });
            return null;
        }
    }
}