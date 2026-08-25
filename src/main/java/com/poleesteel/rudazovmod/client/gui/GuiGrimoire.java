package com.poleesteel.rudazovmod.client.gui;

import com.poleesteel.rudazovmod.capabilities.ActiveSpiritProvider;
import com.poleesteel.rudazovmod.capabilities.IActiveSpirit;
import com.poleesteel.rudazovmod.network.PacketBindSpell;
import com.poleesteel.rudazovmod.network.PacketCraftSpell;
import com.poleesteel.rudazovmod.network.PacketHandler;
import com.poleesteel.rudazovmod.spell.api.CastMode;
import com.poleesteel.rudazovmod.spell.api.Form;
import com.poleesteel.rudazovmod.spell.api.Homing;
import com.poleesteel.rudazovmod.spell.api.ProjectileShape;
import com.poleesteel.rudazovmod.spell.api.SpellCombination;
import com.poleesteel.rudazovmod.spell.api.SpellCost;
import com.poleesteel.rudazovmod.spell.api.SpellDefinition;
import com.poleesteel.rudazovmod.spell.api.SpellElement;
import com.poleesteel.rudazovmod.spell.api.SpellProgression;
import com.poleesteel.rudazovmod.spell.api.TargetType;
import com.poleesteel.rudazovmod.spell.engine.SpellBook;
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
    private static final int PANEL_H_BASE = 220;
    private static final int AXIS_ROW_H = 28;
    private static final int LIST_VISIBLE = 7;
    private static final int ID_SLOT = 0;
    private static final int ID_FORM = 10;
    private static final int ID_TARGET = 20;
    private static final int ID_MODE = 30;
    private static final int ID_ELEMENT = 40;
    private static final int ID_POWER_MINUS = 50;
    private static final int ID_POWER_PLUS = 51;
    private static final int ID_CRAFT = 60;
    private static final int ID_SHAPE = 70;
    private static final int ID_HOMING = 80;
    private static final int ID_LIST = 100;

    private int guiLeft;
    private int guiTop;
    private int selectedSlot;
    private int listScroll;
    private Form form = Form.RAY;
    private TargetType target = TargetType.NONE;
    private CastMode mode = CastMode.INSTANT;
    private SpellElement element = SpellElement.FIRE;
    private ProjectileShape shape = ProjectileShape.ORB;
    private Homing homing = Homing.NONE;
    private float power = 1.0F;
    private int powerTextY;
    private int costTextY;
    private final List<String> hoveredTip = new ArrayList<>();
    private int hoveredX;
    private int hoveredY;
    private int lastBookStamp;

    @Override
    public void initGui() {
        sanitizeAxes();
        layout();
        buildButtons();
    }

    private void layout() {
        this.guiLeft = (this.width - PANEL_W) / 2;
        this.guiTop = (this.height - panelH()) / 2;
    }

    private int panelH() {
        int extra = 0;
        if (showsShape()) {
            extra += AXIS_ROW_H;
        }
        if (showsHoming()) {
            extra += AXIS_ROW_H;
        }
        return PANEL_H_BASE + extra;
    }

    private void rebuild() {
        layout();
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
        addEnumRow(ID_FORM, cx, cy, Form.values(), this.form, 52);
        cy += 20;
        addTargetRow(cx, cy);
        cy += 20;
        addModeRow(cx, cy);
        cy += 20;
        addEnumRow(ID_ELEMENT, cx, cy, SpellElement.values(), this.element, 44);
        cy += 20;
        if (showsShape()) {
            addShapeRow(cx, cy);
            cy += 20;
        }
        if (showsHoming()) {
            addHomingRow(cx, cy);
            cy += 20;
        }
        cy += 2;
        this.powerTextY = cy + 4;
        this.buttonList.add(new GuiButton(ID_POWER_MINUS, cx, cy, 18, 16, "-"));
        this.buttonList.add(new GuiButton(ID_POWER_PLUS, cx + 164, cy, 18, 16, "+"));
        cy += 20;
        GuiButton craft = new GuiButton(ID_CRAFT, cx, cy, 184, 18, I18n.format("gui.rudazovmod.grimoire.craft"));
        craft.enabled = comboOpen(
                this.form, this.target, this.mode, this.element, this.power, this.shape, this.homing);
        this.buttonList.add(craft);
        this.costTextY = cy + 22;
    }

    private void addShapeRow(int x, int y) {
        int col = 0;
        for (int i = 0; i < ProjectileShape.values().length; i++) {
            ProjectileShape value = ProjectileShape.values()[i];
            if (!shapeOpen(value)) {
                continue;
            }
            GuiButton btn = new GuiButton(ID_SHAPE + i, x + col * 48, y, 44, 16, axisName(value));
            if (value == this.shape) {
                btn.packedFGColour = 0x55AAFF;
            }
            this.buttonList.add(btn);
            col++;
        }
    }

    private void addHomingRow(int x, int y) {
        int col = 0;
        for (int i = 0; i < Homing.values().length; i++) {
            Homing value = Homing.values()[i];
            if (!homingOpen(value)) {
                continue;
            }
            GuiButton btn = new GuiButton(ID_HOMING + i, x + col * 62, y, 58, 16, axisName(value));
            if (value == this.homing) {
                btn.packedFGColour = 0x55AAFF;
            }
            this.buttonList.add(btn);
            col++;
        }
    }

    private void addEnumRow(int idBase, int x, int y, Enum<?>[] values, Enum<?> selected, int width) {
        int col = 0;
        for (int i = 0; i < values.length; i++) {
            Enum<?> value = values[i];
            if (idBase == ID_FORM && value instanceof Form && !formOpen((Form) value)) {
                continue;
            }
            if (idBase == ID_ELEMENT && value instanceof SpellElement && !elementOpen((SpellElement) value)) {
                continue;
            }
            GuiButton btn = new GuiButton(idBase + i, x + col * (width + 4), y, width, 16, axisName(value));
            if (value == selected) {
                btn.packedFGColour = 0x55AAFF;
            }
            this.buttonList.add(btn);
            col++;
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
            if (!comboOpen(this.form, this.target, value, this.element, this.power)) {
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
        drawRect(this.guiLeft, this.guiTop, this.guiLeft + PANEL_W, this.guiTop + panelH(), 0xC0101010);
        drawRect(this.guiLeft, this.guiTop, this.guiLeft + PANEL_W, this.guiTop + 18, 0xFF1A1A28);
        this.fontRenderer.drawString(
                I18n.format("gui.rudazovmod.grimoire.title"),
                this.guiLeft + 8, this.guiTop + 5, 0xFFFFFF);
        String stageText = stageLabel();
        int stageWidth = this.fontRenderer.getStringWidth(stageText);
        this.fontRenderer.drawString(stageText, this.guiLeft + PANEL_W - 8 - stageWidth, this.guiTop + 5, 0xFFD080);
        this.fontRenderer.drawString(
                I18n.format("gui.rudazovmod.grimoire.spells"),
                this.guiLeft + 8, this.guiTop + 46, 0xAAAAAA);
        this.fontRenderer.drawString(
                I18n.format("gui.rudazovmod.grimoire.constructor"),
                this.guiLeft + 168, this.guiTop + 46, 0xAAAAAA);

        String powerText = I18n.format("gui.rudazovmod.grimoire.power") + " " + formatPower(this.power);
        this.fontRenderer.drawString(powerText, this.guiLeft + 190, this.powerTextY, 0xFFFFFF);

        if (comboOpen(this.form, this.target, this.mode, this.element, this.power, this.shape, this.homing)) {
            IActiveSpirit spirit = spirit();
            float formM = spirit == null ? 0.0F : spirit.getFormMastery(this.form);
            float elemM = spirit == null ? 0.0F : spirit.getElementMastery(this.element);
            float cost = SpellCost.of(
                    this.mode, this.form, this.element, this.power,
                    effectiveShape(), effectiveHoming(), formM, elemM);
            String costKey = this.mode == CastMode.CHANNEL
                    ? "gui.rudazovmod.grimoire.cost_tick"
                    : "gui.rudazovmod.grimoire.cost_once";
            this.fontRenderer.drawString(
                    I18n.format("gui.rudazovmod.grimoire.cost") + " " + formatPower(cost) + " " + I18n.format(costKey),
                    this.guiLeft + 168, this.costTextY, 0x88CCFF);
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
            sanitizeAxes();
            rebuild();
            return;
        }
        if (id >= ID_ELEMENT && id < ID_ELEMENT + SpellElement.values().length) {
            this.element = SpellElement.values()[id - ID_ELEMENT];
            rebuild();
            return;
        }
        if (id >= ID_SHAPE && id < ID_SHAPE + ProjectileShape.values().length) {
            this.shape = ProjectileShape.values()[id - ID_SHAPE];
            rebuild();
            return;
        }
        if (id >= ID_HOMING && id < ID_HOMING + Homing.values().length) {
            this.homing = Homing.values()[id - ID_HOMING];
            rebuild();
            return;
        }
        if (id == ID_POWER_MINUS) {
            this.power = Math.max(0.5F, this.power - 0.5F);
            rebuild();
            return;
        }
        if (id == ID_POWER_PLUS) {
            this.power = Math.min(guiMaxPower(), this.power + 0.5F);
            rebuild();
            return;
        }
        if (id == ID_CRAFT) {
            if (comboOpen(this.form, this.target, this.mode, this.element, this.power, this.shape, this.homing)) {
                PacketHandler.INSTANCE.sendToServer(new PacketCraftSpell(
                        this.mode, this.target, this.form, this.element,
                        effectiveShape(), effectiveHoming(), this.power, this.selectedSlot));
            }
            return;
        }
        if (id >= ID_LIST) {
            List<SpellDefinition> owned = ownedSpells();
            int index = id - ID_LIST;
            if (index >= 0 && index < owned.size()) {
                SpellDefinition spell = owned.get(index);
                loadAxes(spell);
                PacketHandler.INSTANCE.sendToServer(
                        new PacketBindSpell(this.selectedSlot, spell.id().toString()));
                rebuild();
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
            sanitizeAxes();
            rebuild();
        }
    }

    private void sanitizeAxes() {
        if (!formOpen(this.form)) {
            for (Form next : Form.values()) {
                if (formOpen(next)) {
                    this.form = next;
                    break;
                }
            }
        }
        if (!elementOpen(this.element)) {
            for (SpellElement next : SpellElement.values()) {
                if (elementOpen(next)) {
                    this.element = next;
                    break;
                }
            }
        }
        clampPower();
        if (!showsShape() || !shapeOpen(this.shape)) {
            this.shape = ProjectileShape.ORB;
            if (showsShape() && !shapeOpen(this.shape)) {
                for (ProjectileShape next : ProjectileShape.values()) {
                    if (shapeOpen(next)) {
                        this.shape = next;
                        break;
                    }
                }
            }
        }
        if (!showsHoming() || !homingOpen(this.homing)) {
            this.homing = Homing.NONE;
            if (showsHoming() && !homingOpen(this.homing)) {
                for (Homing next : Homing.values()) {
                    if (homingOpen(next)) {
                        this.homing = next;
                        break;
                    }
                }
            }
        }
        if (comboOpen(this.form, this.target, this.mode, this.element, this.power, this.shape, this.homing)) {
            return;
        }
        for (TargetType nextTarget : TargetType.values()) {
            for (CastMode nextMode : CastMode.values()) {
                if (comboOpen(this.form, nextTarget, nextMode, this.element, this.power, this.shape, this.homing)) {
                    this.target = nextTarget;
                    this.mode = nextMode;
                    if (!showsShape()) {
                        this.shape = ProjectileShape.ORB;
                    }
                    if (!showsHoming()) {
                        this.homing = Homing.NONE;
                    }
                    return;
                }
            }
        }
    }

    private void loadAxes(SpellDefinition spell) {
        this.form = spell.form();
        this.target = spell.targetType();
        this.mode = spell.castMode();
        this.element = spell.element();
        this.power = spell.power();
        this.shape = spell.projectileShape();
        this.homing = spell.homing();
        sanitizeAxes();
    }

    private void clampPower() {
        this.power = Math.max(0.5F, Math.min(guiMaxPower(), this.power));
    }

    private float guiMaxPower() {
        return Math.min(5.0F, SpellProgression.maxPower(chakra()));
    }

    private boolean hasAnyMode(Form form, TargetType target) {
        for (CastMode nextMode : CastMode.values()) {
            if (comboOpen(form, target, nextMode, this.element, this.power)) {
                return true;
            }
        }
        return false;
    }

    private boolean formOpen(Form form) {
        int level = chakra();
        for (TargetType target : TargetType.values()) {
            for (CastMode mode : CastMode.values()) {
                if (!SpellCombination.canCast(form, target, mode)) {
                    continue;
                }
                for (SpellElement element : SpellElement.values()) {
                    if (SpellProgression.meetsChakra(level, form, target, mode, element, SpellBook.MIN_POWER)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean elementOpen(SpellElement element) {
        return SpellProgression.meetsChakra(
                chakra(), Form.RAY, TargetType.NONE, CastMode.INSTANT, element, SpellBook.MIN_POWER);
    }

    private boolean comboOpen(Form form, TargetType target, CastMode mode, SpellElement element, float power) {
        return comboOpen(form, target, mode, element, power, this.shape, this.homing);
    }

    private boolean comboOpen(
            Form form, TargetType target, CastMode mode, SpellElement element, float power, ProjectileShape shape) {
        return comboOpen(form, target, mode, element, power, shape, this.homing);
    }

    private boolean comboOpen(
            Form form, TargetType target, CastMode mode, SpellElement element, float power,
            ProjectileShape shape, Homing homing) {
        boolean projectile = SpellCombination.usesProjectileShape(form, target, mode);
        ProjectileShape resolvedShape = projectile
                ? (shape == null ? ProjectileShape.ORB : shape)
                : ProjectileShape.ORB;
        Homing resolvedHoming = projectile
                ? (homing == null ? Homing.NONE : homing)
                : Homing.NONE;
        return SpellCombination.canCast(form, target, mode)
                && SpellProgression.meetsChakra(
                        chakra(), form, target, mode, element, power, resolvedShape, resolvedHoming);
    }

    private boolean showsShape() {
        return SpellCombination.usesProjectileShape(this.form, this.target, this.mode);
    }

    private boolean showsHoming() {
        return SpellCombination.usesHoming(this.form, this.target, this.mode);
    }

    private boolean shapeOpen(ProjectileShape shape) {
        if (shape == null || !showsShape()) {
            return shape == ProjectileShape.ORB;
        }
        return SpellProgression.meetsChakra(
                chakra(), this.form, this.target, this.mode, this.element, this.power, shape, Homing.NONE);
    }

    private boolean homingOpen(Homing homing) {
        if (homing == null || !showsHoming()) {
            return homing == Homing.NONE;
        }
        return SpellProgression.meetsChakra(
                chakra(), this.form, this.target, this.mode, this.element, this.power, this.shape, homing);
    }

    private ProjectileShape effectiveShape() {
        return showsShape() ? this.shape : ProjectileShape.ORB;
    }

    private Homing effectiveHoming() {
        return showsHoming() ? this.homing : Homing.NONE;
    }

    private int chakra() {
        IActiveSpirit spirit = spirit();
        return spirit == null ? SpellProgression.MIN_CHAKRA : spirit.getChakraLevel();
    }

    private int bookStamp() {
        IActiveSpirit spirit = spirit();
        if (spirit == null) {
            return 0;
        }
        return spirit.getGrimoire().size()
                + 31 * spirit.getUnlockedSpells().size()
                + 17 * spirit.getBoundSpells().hashCode()
                + 13 * spirit.getChakraLevel()
                + Float.floatToIntBits(spirit.getSpiritDevelopment());
    }

    private String stageLabel() {
        IActiveSpirit spirit = spirit();
        int stage = spirit == null ? SpellProgression.MIN_CHAKRA : spirit.getChakraLevel();
        float development = spirit == null ? 0.0F : spirit.getSpiritDevelopment();
        float next = SpellProgression.nextStageAt(stage);
        if (Float.isInfinite(next)) {
            return I18n.format("gui.rudazovmod.grimoire.stage", String.valueOf(stage));
        }
        return I18n.format("gui.rudazovmod.grimoire.stage_next",
                String.valueOf(stage), formatPower(development), formatPower(next));
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
                    IActiveSpirit spirit = spirit();
                    float cost = spirit == null
                            ? spell.cost()
                            : SpellCost.of(spell, spirit.getFormMastery(spell.form()),
                                    spirit.getElementMastery(spell.element()));
                    this.hoveredTip.add(TextFormatting.AQUA + "cost " + formatPower(cost));
                    int need = SpellProgression.requiredChakra(spell);
                    if (chakra() < need) {
                        this.hoveredTip.add(TextFormatting.RED + I18n.format("gui.rudazovmod.grimoire.need_stage", need));
                    }
                    this.hoveredX = mouseX;
                    this.hoveredY = mouseY;
                }
            }
        }
    }

    public static String shortName(SpellDefinition spell) {
        if (SpellCombination.usesProjectileShape(spell)) {
            String name = axisName(spell.projectileShape()) + " " + axisName(spell.element());
            if (spell.homing() != Homing.NONE) {
                name += " " + axisName(spell.homing());
            }
            return name;
        }
        return axisName(spell.form()) + " " + axisName(spell.element());
    }

    static String axes(SpellDefinition spell) {
        String text = axisName(spell.castMode()) + " / " + axisName(spell.targetType())
                + " / " + axisName(spell.form()) + " / " + axisName(spell.element());
        if (SpellCombination.usesProjectileShape(spell)) {
            text += " / " + axisName(spell.projectileShape());
            text += " / " + axisName(spell.homing());
        }
        return text + " p=" + formatPower(spell.power());
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
        } else if (value instanceof ProjectileShape) {
            kind = "shape";
        } else if (value instanceof Homing) {
            kind = "homing";
        } else {
            return value.name();
        }
        return I18n.format("spell.rudazovmod." + kind + "." + value.name().toLowerCase(Locale.ROOT));
    }

    static String formatPower(float value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
