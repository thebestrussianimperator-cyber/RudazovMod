package com.poleesteel.rudazovmod.command;

import com.poleesteel.rudazovmod.Tags;
import com.poleesteel.rudazovmod.capabilities.ActiveSpiritProvider;
import com.poleesteel.rudazovmod.capabilities.IActiveSpirit;
import com.poleesteel.rudazovmod.spell.api.CastMode;
import com.poleesteel.rudazovmod.spell.api.Form;
import com.poleesteel.rudazovmod.spell.api.SpellCombination;
import com.poleesteel.rudazovmod.spell.api.SpellCost;
import com.poleesteel.rudazovmod.spell.api.SpellDefinition;
import com.poleesteel.rudazovmod.spell.api.SpellElement;
import com.poleesteel.rudazovmod.spell.api.SpellProgression;
import com.poleesteel.rudazovmod.spell.api.TargetType;
import com.poleesteel.rudazovmod.network.PacketSyncMana;
import com.poleesteel.rudazovmod.network.PacketSyncSpirit;
import com.poleesteel.rudazovmod.spell.engine.SpellBook;
import com.poleesteel.rudazovmod.spell.engine.SpellEngine;
import com.poleesteel.rudazovmod.spell.engine.SpellRegistry;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class CommandRudazov extends CommandBase {

    @Override
    public String getName() {
        return "rudazovmod";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/rudazovmod <unlock|bind|craft|list|mana|chakra> <args>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if (!(sender instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) sender;
        IActiveSpirit spirit = player.getCapability(ActiveSpiritProvider.ACTIVE_SPIRIT_CAP, null);
        if (spirit == null) {
            return;
        }

        if (args.length == 0) {
            msg(player, TextFormatting.RED, "Использование: " + getUsage(sender));
            return;
        }

        String sub = args[0].toLowerCase();
        if ("unlock".equals(sub) && args.length >= 2) {
            unlock(player, spirit, args[1]);
        } else if ("bind".equals(sub) && args.length >= 3) {
            bind(player, spirit, args[1], args[2]);
        } else if ("craft".equals(sub) && args.length >= 6) {
            craft(player, spirit, args);
        } else if ("list".equals(sub)) {
            list(player, spirit);
        } else if ("mana".equals(sub)) {
            refillMana(player, spirit);
        } else if ("chakra".equals(sub)) {
            if (args.length >= 2 && "up".equalsIgnoreCase(args[1])) {
                upgradeChakras(player, spirit);
            } else {
                showChakra(player, spirit);
            }
        } else {
            msg(player, TextFormatting.RED, "Использование: " + getUsage(sender));
            msg(player, TextFormatting.GRAY, "/rudazovmod craft <mode> <target> <form> <element> <power>");
            msg(player, TextFormatting.GRAY, "/rudazovmod bind <1-4> <spell_id>");
            msg(player, TextFormatting.GRAY, "/rudazovmod chakra [up]  (up — отладка, +20 к развитию)");
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "unlock", "bind", "craft", "list", "mana", "chakra");
        }
        if ("chakra".equalsIgnoreCase(args[0]) && args.length == 2) {
            return getListOfStringsMatchingLastWord(args, "up");
        }
        String sub = args[0].toLowerCase();
        if ("unlock".equals(sub) && args.length == 2) {
            List<String> ids = knownIds(sender);
            ids.add("all");
            return getListOfStringsMatchingLastWord(args, ids);
        }
        if ("bind".equals(sub)) {
            if (args.length == 2) {
                return getListOfStringsMatchingLastWord(args, "1", "2", "3", "4");
            }
            if (args.length == 3) {
                return getListOfStringsMatchingLastWord(args, knownIds(sender));
            }
        }
        if ("craft".equals(sub)) {
            if (args.length == 2) {
                return getListOfStringsMatchingLastWord(args, enumNames(CastMode.class));
            }
            if (args.length == 3) {
                return getListOfStringsMatchingLastWord(args, enumNames(TargetType.class));
            }
            if (args.length == 4) {
                return getListOfStringsMatchingLastWord(args, enumNames(Form.class));
            }
            if (args.length == 5) {
                return getListOfStringsMatchingLastWord(args, enumNames(SpellElement.class));
            }
        }
        return Collections.emptyList();
    }

    private static void refillMana(EntityPlayerMP player, IActiveSpirit spirit) {
        spirit.setMana(spirit.getMaxMana());
        PacketSyncMana.sendTo(player);
        msg(player, TextFormatting.AQUA, "Мана восстановлена: "
                + (int) spirit.getMana() + " / " + (int) spirit.getMaxMana());
    }

    private static void showChakra(EntityPlayerMP player, IActiveSpirit spirit) {
        msg(player, TextFormatting.GOLD, formatStage(spirit));
        msg(player, TextFormatting.AQUA, "Мана: "
                + formatNum(spirit.getMana()) + " / " + formatNum(spirit.getMaxMana())
                + " (потолок практики " + formatNum(SpellProgression.practiceCap(spirit.getChakraLevel())) + ")");
        StringBuilder forms = new StringBuilder("Формы:");
        for (Form form : Form.values()) {
            forms.append(' ').append(form.name()).append('=')
                    .append(formatNum(spirit.getFormMastery(form)));
        }
        msg(player, TextFormatting.WHITE, forms.toString());
        StringBuilder elements = new StringBuilder("Стихии:");
        for (SpellElement element : SpellElement.values()) {
            elements.append(' ').append(element.name()).append('=')
                    .append(formatNum(spirit.getElementMastery(element)));
        }
        msg(player, TextFormatting.WHITE, elements.toString());
        msg(player, TextFormatting.GRAY, "Рост ступени — от кастов. Отладка: /rudazovmod chakra up");
    }

    private static void upgradeChakras(EntityPlayerMP player, IActiveSpirit spirit) {
        spirit.upgradeChakras();
        PacketSyncMana.sendTo(player);
        PacketSyncSpirit.sendTo(player);
        msg(player, TextFormatting.GREEN, "Отладка: развитие +"
                + formatNum(SpellProgression.STAGE_WIDTH) + " → " + formatStage(spirit));
    }

    private static String formatStage(IActiveSpirit spirit) {
        int stage = spirit.getChakraLevel();
        float development = spirit.getSpiritDevelopment();
        float next = SpellProgression.nextStageAt(stage);
        if (Float.isInfinite(next)) {
            return "Ступень " + stage + " (" + formatNum(development) + ")";
        }
        return "Ступень " + stage + " (" + formatNum(development) + " / " + formatNum(next) + ")";
    }

    private static void unlock(EntityPlayerMP player, IActiveSpirit spirit, String rawId) {
        if ("all".equalsIgnoreCase(rawId)) {
            for (SpellDefinition spell : SpellRegistry.all()) {
                spirit.unlockSpell(spell.id().toString());
            }
            msg(player, TextFormatting.GREEN, "Вы изучили все пресеты реестра.");
            PacketSyncSpirit.sendTo(player);
            return;
        }
        Optional<SpellDefinition> spell = SpellEngine.findDefinition(player, rawId);
        if (!spell.isPresent()) {
            msg(player, TextFormatting.RED, "Заклинание не найдено: " + rawId);
            return;
        }
        String id = spell.get().id().toString();
        spirit.unlockSpell(id);
        msg(player, TextFormatting.GREEN, "Заклинание изучено: " + id);
        PacketSyncSpirit.sendTo(player);
    }

    private static void bind(EntityPlayerMP player, IActiveSpirit spirit, String slotRaw, String rawId) {
        int slot;
        try {
            slot = Integer.parseInt(slotRaw) - 1;
        } catch (NumberFormatException e) {
            msg(player, TextFormatting.RED, "Номер слота должен быть числом от 1 до 4.");
            return;
        }
        if (slot < 0 || slot > 3) {
            msg(player, TextFormatting.RED, "Номер слота должен быть числом от 1 до 4.");
            return;
        }
        Optional<SpellDefinition> spell = SpellEngine.findDefinition(player, rawId);
        if (!spell.isPresent()) {
            msg(player, TextFormatting.RED, "Заклинание не найдено: " + rawId);
            return;
        }
        String id = spell.get().id().toString();
        if (!SpellBook.bind(player, slot, id)) {
            msg(player, TextFormatting.RED, "Сначала изучите это заклинание или соберите его через craft.");
            return;
        }
        msg(player, TextFormatting.GOLD, "Слот " + (slot + 1) + " привязан к: " + id);
    }

    private static void craft(EntityPlayerMP player, IActiveSpirit spirit, String[] args) {
        Optional<CastMode> mode = SpellDefinition.parseEnum(CastMode.class, args[1]);
        Optional<TargetType> target = SpellDefinition.parseEnum(TargetType.class, args[2]);
        Optional<Form> form = SpellDefinition.parseEnum(Form.class, args[3]);
        Optional<SpellElement> element = SpellDefinition.parseEnum(SpellElement.class, args[4]);
        if (!mode.isPresent()) {
            msg(player, TextFormatting.RED, "Неизвестный режим. Варианты: " + joinNames(CastMode.class));
            return;
        }
        if (!target.isPresent()) {
            msg(player, TextFormatting.RED, "Неизвестный тип цели. Варианты: " + joinNames(TargetType.class));
            return;
        }
        if (!form.isPresent()) {
            msg(player, TextFormatting.RED, "Неизвестная форма. Варианты: " + joinNames(Form.class));
            return;
        }
        if (!element.isPresent()) {
            msg(player, TextFormatting.RED, "Неизвестная стихия. Варианты: " + joinNames(SpellElement.class));
            return;
        }

        float power;
        try {
            power = Float.parseFloat(args[5]);
        } catch (NumberFormatException e) {
            msg(player, TextFormatting.RED, "power должен быть числом больше 0.");
            return;
        }
        if (power <= 0.0F || Float.isNaN(power) || Float.isInfinite(power)) {
            msg(player, TextFormatting.RED, "power должен быть числом больше 0.");
            return;
        }

        if (!SpellCombination.canCast(form.get(), target.get(), mode.get())) {
            if (SpellCombination.isLegal(form.get(), target.get(), mode.get())) {
                msg(player, TextFormatting.RED,
                        "Форма ещё не умеет " + form.get() + "+" + target.get() + "+" + mode.get() + ".");
            } else {
                msg(player, TextFormatting.RED,
                        "Незаконная комбинация: " + form.get() + "+" + target.get() + "+" + mode.get() + ".");
            }
            return;
        }
        int need = SpellProgression.requiredChakra(form.get(), target.get(), mode.get(), element.get(), power);
        if (spirit.getChakraLevel() < need) {
            msg(player, TextFormatting.RED, "Нужна ступень чакр " + need
                    + " (сейчас " + spirit.getChakraLevel() + ").");
            return;
        }
        float cap = SpellProgression.maxPower(spirit.getChakraLevel());
        if (power > cap) {
            msg(player, TextFormatting.RED, "Сила " + formatNum(power)
                    + " недоступна на ступени " + spirit.getChakraLevel()
                    + " (макс. " + formatNum(cap) + ").");
            return;
        }

        Optional<SpellDefinition> made = SpellBook.craft(
                player, mode.get(), target.get(), form.get(), element.get(), power, -1);
        if (!made.isPresent()) {
            msg(player, TextFormatting.RED, "Не удалось собрать заклинание.");
            return;
        }
        SpellDefinition spell = made.get();
        msg(player, TextFormatting.GREEN, "Собрано: " + spell.id());
        msg(player, TextFormatting.GRAY, formatSpell(spirit, spell));
        msg(player, TextFormatting.GRAY, "Привязка: /rudazovmod bind 1 " + spell.id());
    }

    private static void list(EntityPlayerMP player, IActiveSpirit spirit) {
        msg(player, TextFormatting.GOLD, "Гримуар:");
        if (spirit.getGrimoire().isEmpty()) {
            msg(player, TextFormatting.GRAY, "  (пусто)");
        } else {
            for (SpellDefinition spell : spirit.getGrimoire()) {
                msg(player, lockColor(spirit, spell), "  " + spell.id() + " — " + formatSpell(spirit, spell));
            }
        }
        msg(player, TextFormatting.GOLD, "Пресеты:");
        for (SpellDefinition spell : SpellRegistry.all()) {
            String mark = spirit.isSpellUnlocked(spell.id().toString()) ? "*" : " ";
            msg(player, lockColor(spirit, spell),
                    "  " + mark + " " + spell.id() + " — " + formatSpell(spirit, spell));
        }
    }

    private static TextFormatting lockColor(IActiveSpirit spirit, SpellDefinition spell) {
        return SpellProgression.meetsChakra(spirit.getChakraLevel(), spell)
                ? TextFormatting.WHITE
                : TextFormatting.DARK_GRAY;
    }

    private static String formatSpell(IActiveSpirit spirit, SpellDefinition spell) {
        float cost = SpellCost.of(
                spell, spirit.getFormMastery(spell.form()), spirit.getElementMastery(spell.element()));
        int need = SpellProgression.requiredChakra(spell);
        String lock = spirit.getChakraLevel() >= need ? "" : " [ступень " + need + "]";
        return spell.castMode() + " " + spell.targetType() + " " + spell.form() + " " + spell.element()
                + " p=" + spell.power() + " cost=" + formatNum(cost) + lock;
    }

    private static String formatNum(float value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static List<String> knownIds(ICommandSender sender) {
        List<String> ids = new ArrayList<>();
        for (SpellDefinition spell : SpellRegistry.all()) {
            ids.add(spell.id().toString());
            if (Tags.MODID.equals(spell.id().getNamespace())) {
                ids.add(spell.id().getPath());
            }
        }
        if (sender instanceof EntityPlayerMP) {
            IActiveSpirit spirit = ((EntityPlayerMP) sender).getCapability(ActiveSpiritProvider.ACTIVE_SPIRIT_CAP, null);
            if (spirit != null) {
                for (SpellDefinition spell : spirit.getGrimoire()) {
                    ids.add(spell.id().toString());
                }
            }
        }
        return ids;
    }

    private static String[] enumNames(Class<? extends Enum<?>> type) {
        Enum<?>[] values = type.getEnumConstants();
        String[] names = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            names[i] = values[i].name();
        }
        return names;
    }

    private static String joinNames(Class<? extends Enum<?>> type) {
        return String.join(", ", enumNames(type));
    }

    private static void msg(EntityPlayerMP player, TextFormatting color, String text) {
        player.sendMessage(new TextComponentString(color + text));
    }
}
