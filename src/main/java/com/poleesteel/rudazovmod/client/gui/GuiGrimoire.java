package com.poleesteel.rudazovmod.client.gui;

import com.poleesteel.rudazovmod.capabilities.ActiveSpiritProvider;
import com.poleesteel.rudazovmod.capabilities.IActiveSpirit;
import com.poleesteel.rudazovmod.network.PacketBindSpell;
import com.poleesteel.rudazovmod.network.PacketCraftSpell;
import com.poleesteel.rudazovmod.network.PacketHandler;
import com.poleesteel.rudazovmod.spell.api.CastMode;
import com.poleesteel.rudazovmod.spell.api.Form;
import com.poleesteel.rudazovmod.spell.api.SpellCombination;
import com.poleesteel.rudazovmod.spell.api.SpellCost;
import com.poleesteel.rudazovmod.spell.api.SpellDefinition;
import com.poleesteel.rudazovmod.spell.api.SpellElement;
import com.poleesteel.rudazovmod.spell.api.TargetType;
import com.poleesteel.rudazovmod.spell.engine.SpellEngine;
import com.poleesteel.rudazovmod.spell.engine.SpellRegistry;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Сборка из осей и привязка к слотам Z/X/C/V. Каст отсюда не шлётся.
 */
@SideOnly(Side.CLIENT)
public class GuiGrimoire extends GuiScreen {

    private static final int PANEL_W = 360;
    private static final int PANEL_H = 220;
    private static final int LIST_VISIBLE = 7;
    private static final int ID_SLOT = 0;
    private static final int ID_FORM = 10;
    private static final int ID_TARGET = 20;
    private static final int ID_MODE = 30;
    private static final int ID_ELEMENT = 40;
    private static final int ID_POWER_MINUS = 50;
    private static final int ID_POWER_PLUS = 51;
    private static final int ID_CRAFT = 60;
    private static final int ID_LIST = 100;

    private int guiLeft;
    private int guiTop;
    private int selectedSlot;
    private int listScroll;
    private Form form = Form.RAY;
    private TargetType target = TargetType.NONE;
    private CastMode mode = CastMode.INSTANT;
    private SpellElement element = SpellElement.FIRE;
    private float power = 1.0F;
    private final List<String> hoveredTip = new ArrayList<>();
    private int hoveredX;
    private int hoveredY;
    private int lastBookStamp;

    @Override
    public void initGui() {
        this.guiLeft = (this.width - PANEL_W) / 2;
        this.guiTop = (this.height - PANEL_H) / 2;
        sanitizeAxes();
        buildButtons();
    }

    private void rebuild() {
        this.buttonList.clear();
        buildButtons();
    }

    private void buildButtons() {
        int x = this.guiLeft + 8;
        int y = this.guiTop + 22;
        for (int i = 0; i < 4; i++) {
            GuiButton slot = new GuiButton(ID_SLOT + i, x + i * 86, y, 82, 18, slotLabel(i));
            if (i == this.selectedSlot) {
                slot.packedFGColour = 0x55AAFF;
            }
            this.buttonList.add(slot);
        }

        int listX = this.guiLeft + 8;
        int listY = this.guiTop + 58;
        List<SpellDefinition> owned = ownedSpells();
        int maxScroll = Math.max(0, owned.size() - LIST_VISIBLE);
        if (this.listScroll > maxScroll) {
            this.listScroll = maxScroll;
        }
        int end = Math.min(this.listScroll + LIST_VISIBLE, owned.size());
        for (int i = this.listScroll; i < end; i++) {
            SpellDefinition spell = owned.get(i);
            int row = i - this.listScroll;
            GuiButton btn = new GuiButton(ID_LIST + i, listX, listY + row * 18, 150, 16, shortName(spell));
            if (isBoundToSelected(spell)) {
                btn.packedFGColour = 0x55AAFF;
            }
            this.buttonList.add(btn);
        }

        int cx = this.guiLeft + 168;
        int cy = this.guiTop + 58;
        addEnumRow(ID_FORM, cx, cy, Form.values(), this.form, 58);
        cy += 20;
        addTargetRow(cx, cy);
        cy += 20;
        addModeRow(cx, cy);
        cy += 20;
        addEnumRow(ID_ELEMENT, cx, cy, SpellElement.values(), this.element, 58);
        cy += 22;
        this.buttonList.add(new GuiButton(ID_POWER_MINUS, cx, cy, 18, 16, "-"));
        this.buttonList.add(new GuiButton(ID_POWER_PLUS, cx + 164, cy, 18, 16, "+"));
        cy += 20;
        this.buttonList.add(new GuiButton(ID_CRAFT, cx, cy, 184, 18, I18n.format("gui.rudazovmod.grimoire.craft")));
    }

    private void addEnumRow(int idBase, int x, int y, Enum<?>[] values, Enum<?> selected, int width) {
        for (int i = 0; i < values.length; i++) {
            GuiButton btn = new GuiButton(idBase + i, x + i * (width + 4), y, width, 16, axisName(values[i]));
            if (values[i] == selected) {
                btn.packedFGColour = 0x55AAFF;
            }
            this.buttonList.add(btn);
        }
    }

    private void addTargetRow(int x, int y) {
        int col = 0;
        for (int i = 0; i < TargetType.values().length; i++) {
            TargetType value = TargetType.values()[i];
            if (!hasAnyMode(this.form, value)) {
                continue;
            }
            GuiButton btn = new GuiButton(ID_TARGET + i, x + col * 62, y, 58, 16, axisName(value));
            if (value == this.target) {
                btn.packedFGColour = 0x55AAFF;
            }
            this.buttonList.add(btn);
            col++;
        }
    }

    private void addModeRow(int x, int y) {
        int col = 0;
        for (int i = 0; i < CastMode.values().length; i++) {
            CastMode value = CastMode.values()[i];
            if (!SpellCombination.canCast(this.form, this.target, value)) {
                continue;
            }
            GuiButton btn = new GuiButton(ID_MODE + i, x + col * 92, y, 88, 16, axisName(value));
            if (value == this.mode) {
                btn.packedFGColour = 0x55AAFF;
            }
            this.buttonList.add(btn);
            col++;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.hoveredTip.clear();
        drawDefaultBackground();
        drawRect(this.guiLeft, this.guiTop, this.guiLeft + PANEL_W, this.guiTop + PANEL_H, 0xC0101010);
        drawRect(this.guiLeft, this.guiTop, this.guiLeft + PANEL_W, this.guiTop + 18, 0xFF1A1A28);
        this.fontRenderer.drawString(
                I18n.format("gui.rudazovmod.grimoire.title"),
                this.guiLeft + 8, this.guiTop + 5, 0xFFFFFF);
        this.fontRenderer.drawString(
                I18n.format("gui.rudazovmod.grimoire.spells"),
                this.guiLeft + 8, this.guiTop + 46, 0xAAAAAA);
        this.fontRenderer.drawString(
                I18n.format("gui.rudazovmod.grimoire.constructor"),
                this.guiLeft + 168, this.guiTop + 46, 0xAAAAAA);

        String powerText = I18n.format("gui.rudazovmod.grimoire.power") + " " + formatPower(this.power);
        this.fontRenderer.drawString(powerText, this.guiLeft + 190, this.guiTop + 143, 0xFFFFFF);

        if (SpellCombination.canCast(this.form, this.target, this.mode)) {
            float cost = SpellCost.of(this.mode, this.form, this.element, this.power);
            String costKey = this.mode == CastMode.CHANNEL
                    ? "gui.rudazovmod.grimoire.cost_tick"
                    : "gui.rudazovmod.grimoire.cost_once";
            this.fontRenderer.drawString(
                    I18n.format("gui.rudazovmod.grimoire.cost") + " " + formatPower(cost) + " " + I18n.format(costKey),
                    this.guiLeft + 168, this.guiTop + 200, 0x88CCFF);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
        fillHover(mouseX, mouseY);
        if (!this.hoveredTip.isEmpty()) {
            drawHoveringText(this.hoveredTip, this.hoveredX, this.hoveredY);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        int id = button.id;
        if (id >= ID_SLOT && id < ID_SLOT + 4) {
            this.selectedSlot = id - ID_SLOT;
            rebuild();
            return;
        }
        if (id >= ID_FORM && id < ID_FORM + Form.values().length) {
            this.form = Form.values()[id - ID_FORM];
            sanitizeAxes();
            rebuild();
            return;
        }
        if (id >= ID_TARGET && id < ID_TARGET + TargetType.values().length) {
            this.target = TargetType.values()[id - ID_TARGET];
            sanitizeAxes();
            rebuild();
            return;
        }
        if (id >= ID_MODE && id < ID_MODE + CastMode.values().length) {
            this.mode = CastMode.values()[id - ID_MODE];
            rebuild();
            return;
        }
        if (id >= ID_ELEMENT && id < ID_ELEMENT + SpellElement.values().length) {
            this.element = SpellElement.values()[id - ID_ELEMENT];
            rebuild();
            return;
        }
        if (id == ID_POWER_MINUS) {
            this.power = Math.max(0.5F, this.power - 0.5F);
            rebuild();
            return;
        }
        if (id == ID_POWER_PLUS) {
            this.power = Math.min(5.0F, this.power + 0.5F);
            rebuild();
            return;
        }
        if (id == ID_CRAFT) {
            if (SpellCombination.canCast(this.form, this.target, this.mode)) {
                PacketHandler.INSTANCE.sendToServer(new PacketCraftSpell(
                        this.mode, this.target, this.form, this.element, this.power, this.selectedSlot));
            }
            return;
        }
        if (id >= ID_LIST) {
            List<SpellDefinition> owned = ownedSpells();
            int index = id - ID_LIST;
            if (index >= 0 && index < owned.size()) {
                PacketHandler.INSTANCE.sendToServer(
                        new PacketBindSpell(this.selectedSlot, owned.get(index).id().toString()));
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return;
        }
        int mx = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int my = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        if (mx < this.guiLeft + 8 || mx > this.guiLeft + 158
                || my < this.guiTop + 58 || my > this.guiTop + 58 + LIST_VISIBLE * 18) {
            return;
        }
        int maxScroll = Math.max(0, ownedSpells().size() - LIST_VISIBLE);
        if (wheel > 0) {
            this.listScroll = Math.max(0, this.listScroll - 1);
        } else {
            this.listScroll = Math.min(maxScroll, this.listScroll + 1);
        }
        rebuild();
    }

    @Override
    public void updateScreen() {
        if (this.mc.player == null) {
            this.mc.displayGuiScreen(null);
            return;
        }
        int stamp = bookStamp();
        if (stamp != this.lastBookStamp) {
            this.lastBookStamp = stamp;
            rebuild();
        }
    }

    private void sanitizeAxes() {
        if (SpellCombination.canCast(this.form, this.target, this.mode)) {
            return;
        }
        for (TargetType nextTarget : TargetType.values()) {
            for (CastMode nextMode : CastMode.values()) {
                if (SpellCombination.canCast(this.form, nextTarget, nextMode)) {
                    this.target = nextTarget;
                    this.mode = nextMode;
                    return;
                }
            }
        }
    }

    private boolean hasAnyMode(Form form, TargetType target) {
        for (CastMode nextMode : CastMode.values()) {
            if (SpellCombination.canCast(form, target, nextMode)) {
                return true;
            }
        }
        return false;
    }

    private int bookStamp() {
        IActiveSpirit spirit = spirit();
        if (spirit == null) {
            return 0;
        }
        return spirit.getGrimoire().size()
                + 31 * spirit.getUnlockedSpells().size()
                + 17 * spirit.getBoundSpells().hashCode();
    }

    private IActiveSpirit spirit() {
        return this.mc.player == null
                ? null
                : this.mc.player.getCapability(ActiveSpiritProvider.ACTIVE_SPIRIT_CAP, null);
    }

    private List<SpellDefinition> ownedSpells() {
        List<SpellDefinition> result = new ArrayList<>();
        IActiveSpirit spirit = spirit();
        if (spirit == null) {
            return result;
        }
        for (SpellDefinition spell : spirit.getGrimoire()) {
            result.add(spell);
        }
        for (SpellDefinition spell : SpellRegistry.all()) {
            if (spirit.isSpellUnlocked(spell.id().toString()) && !containsId(result, spell.id().toString())) {
                result.add(spell);
            }
        }
        return result;
    }

    private static boolean containsId(List<SpellDefinition> list, String id) {
        for (SpellDefinition spell : list) {
            if (spell.id().toString().equals(id)) {
                return true;
            }
        }
        return false;
    }

    private String slotLabel(int slot) {
        IActiveSpirit spirit = spirit();
        String key = I18n.format("gui.rudazovmod.slot", String.valueOf(slot + 1));
        if (spirit == null) {
            return key;
        }
        String bound = spirit.getBoundSpell(slot);
        if (bound == null || bound.isEmpty()) {
            return key + ": " + I18n.format("gui.rudazovmod.grimoire.empty");
        }
        Optional<SpellDefinition> spell = SpellEngine.findDefinition(this.mc.player, bound);
        if (!spell.isPresent()) {
            return key + ": ?";
        }
        return key + ": " + shortName(spell.get());
    }

    private boolean isBoundToSelected(SpellDefinition spell) {
        IActiveSpirit spirit = spirit();
        if (spirit == null) {
            return false;
        }
        return spell.id().toString().equals(spirit.getBoundSpell(this.selectedSlot));
    }

    private void fillHover(int mouseX, int mouseY) {
        for (GuiButton button : this.buttonList) {
            if (!button.isMouseOver()) {
                continue;
            }
            if (button.id >= ID_LIST) {
                List<SpellDefinition> owned = ownedSpells();
                int index = button.id - ID_LIST;
                if (index >= 0 && index < owned.size()) {
                    SpellDefinition spell = owned.get(index);
                    this.hoveredTip.add(spell.id().toString());
                    this.hoveredTip.add(TextFormatting.GRAY + axes(spell));
                    this.hoveredTip.add(TextFormatting.AQUA + "cost " + formatPower(spell.cost()));
                    this.hoveredX = mouseX;
                    this.hoveredY = mouseY;
                }
            }
        }
    }

    public static String shortName(SpellDefinition spell) {
        return axisName(spell.form()) + " " + axisName(spell.element());
    }

    static String axes(SpellDefinition spell) {
        return axisName(spell.castMode()) + " / " + axisName(spell.targetType())
                + " / " + axisName(spell.form()) + " / " + axisName(spell.element())
                + " p=" + formatPower(spell.power());
    }

    static String axisName(Enum<?> value) {
        String kind;
        if (value instanceof Form) {
            kind = "form";
        } else if (value instanceof TargetType) {
            kind = "target";
        } else if (value instanceof CastMode) {
            kind = "mode";
        } else if (value instanceof SpellElement) {
            kind = "element";
        } else {
            return value.name();
        }
        return I18n.format("spell.rudazovmod." + kind + "." + value.name().toLowerCase(Locale.ROOT));
    }

    static String formatPower(float value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
