package com.poleesteel.rudazovmod.network;

import com.poleesteel.rudazovmod.spell.api.CastMode;
import com.poleesteel.rudazovmod.spell.api.Form;
import com.poleesteel.rudazovmod.spell.api.Homing;
import com.poleesteel.rudazovmod.spell.api.ProjectileShape;
import com.poleesteel.rudazovmod.spell.api.SpellElement;
import com.poleesteel.rudazovmod.spell.api.TargetType;
import com.poleesteel.rudazovmod.spell.engine.SpellBook;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * C→S: оси конструктора, включая форму снаряда и самонаведение. Id кастома выдаёт сервер. Слот −1 = только в гримуар.
 */
public class PacketCraftSpell implements IMessage {

    private int mode;
    private int target;
    private int form;
    private int element;
    private int shape;
    private int homing;
    private float power;
    private int bindSlot;

    public PacketCraftSpell() {}

    public PacketCraftSpell(CastMode mode, TargetType target, Form form, SpellElement element, float power, int bindSlot) {
        this(mode, target, form, element, ProjectileShape.ORB, Homing.NONE, power, bindSlot);
    }

    public PacketCraftSpell(
            CastMode mode, TargetType target, Form form, SpellElement element,
            ProjectileShape shape, float power, int bindSlot) {
        this(mode, target, form, element, shape, Homing.NONE, power, bindSlot);
    }

    public PacketCraftSpell(
            CastMode mode, TargetType target, Form form, SpellElement element,
            ProjectileShape shape, Homing homing, float power, int bindSlot) {
        this.mode = mode.ordinal();
        this.target = target.ordinal();
        this.form = form.ordinal();
        this.element = element.ordinal();
        this.shape = (shape == null ? ProjectileShape.ORB : shape).ordinal();
        this.homing = (homing == null ? Homing.NONE : homing).ordinal();
        this.power = power;
        this.bindSlot = bindSlot;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.mode = buf.readByte();
        this.target = buf.readByte();
        this.form = buf.readByte();
        this.element = buf.readByte();
        this.shape = buf.readByte();
        this.homing = buf.readByte();
        this.power = buf.readFloat();
        this.bindSlot = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(this.mode);
        buf.writeByte(this.target);
        buf.writeByte(this.form);
        buf.writeByte(this.element);
        buf.writeByte(this.shape);
        buf.writeByte(this.homing);
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
                ProjectileShape shape = ordinal(ProjectileShape.values(), message.shape);
                Homing homing = ordinal(Homing.values(), message.homing);
                if (mode == null || target == null || form == null || element == null) {
                    return;
                }
                SpellBook.craft(player, mode, target, form, element, message.power,
                        shape == null ? ProjectileShape.ORB : shape,
                        homing == null ? Homing.NONE : homing, message.bindSlot);
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
