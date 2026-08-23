package com.poleesteel.rudazovmod.network;

import com.poleesteel.rudazovmod.spell.api.CastMode;
import com.poleesteel.rudazovmod.spell.api.Form;
import com.poleesteel.rudazovmod.spell.api.SpellElement;
import com.poleesteel.rudazovmod.spell.api.TargetType;
import com.poleesteel.rudazovmod.spell.engine.SpellBook;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * C→S: оси конструктора. Id кастома выдаёт сервер. Слот −1 = только в гримуар.
 */
public class PacketCraftSpell implements IMessage {

    private int mode;
    private int target;
    private int form;
    private int element;
    private float power;
    private int bindSlot;

    public PacketCraftSpell() {}

    public PacketCraftSpell(CastMode mode, TargetType target, Form form, SpellElement element, float power, int bindSlot) {
        this.mode = mode.ordinal();
        this.target = target.ordinal();
        this.form = form.ordinal();
        this.element = element.ordinal();
        this.power = power;
        this.bindSlot = bindSlot;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.mode = buf.readByte();
        this.target = buf.readByte();
        this.form = buf.readByte();
        this.element = buf.readByte();
        this.power = buf.readFloat();
        this.bindSlot = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(this.mode);
        buf.writeByte(this.target);
        buf.writeByte(this.form);
        buf.writeByte(this.element);
        buf.writeFloat(this.power);
        buf.writeInt(this.bindSlot);
    }

    public static class Handler implements IMessageHandler<PacketCraftSpell, IMessage> {
        @Override
        public IMessage onMessage(PacketCraftSpell message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                CastMode mode = ordinal(CastMode.values(), message.mode);
                TargetType target = ordinal(TargetType.values(), message.target);
                Form form = ordinal(Form.values(), message.form);
                SpellElement element = ordinal(SpellElement.values(), message.element);
                if (mode == null || target == null || form == null || element == null) {
                    return;
                }
                SpellBook.craft(player, mode, target, form, element, message.power, message.bindSlot);
            });
            return null;
        }

        private static <T extends Enum<T>> T ordinal(T[] values, int index) {
            if (index < 0 || index >= values.length) {
                return null;
            }
            return values[index];
        }
    }
}
