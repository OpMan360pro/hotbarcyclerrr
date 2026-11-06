package com.example.hotbarcyclerrr.screen;

import com.example.hotbarcyclerrr.AntiCheatMode;
import com.example.hotbarcyclerrr.HotbarCyclerConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class HotbarCyclerConfigScreen extends Screen {

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int BTN_W        = 150;
    private static final int BTN_H        = 20;
    private static final int FIELD_W      = 38;
    private static final int FIELD_H      = 18;
    private static final int CONTENT_TOP  = 24;

    // ── Colors ────────────────────────────────────────────────────────────────
    private static final int C_WHITE   = 0xFFFFFFFF;
    private static final int C_GOLD    = 0xFFFFD700;
    private static final int C_GRAY    = 0xFFAAAAAA;

    // ── State ─────────────────────────────────────────────────────────────────
    private final Screen parent;

    // ── Widgets ───────────────────────────────────────────────────────────────
    private ButtonWidget    antiCheatBtn, pauseSprintBtn, freezeMovementBtn;
    private TextFieldWidget delayField;
    private ButtonWidget    rowShowBtn, revertToFixedBtn;
    private TextFieldWidget rowOffX, rowOffY, rowSqW, rowSqH;

    // ── Working state ─────────────────────────────────────────────────────────
    private AntiCheatMode wMode;
    private boolean       wPauseSprint, wFreezeMovement;
    private int           wDelay;
    private boolean       wRowShow, wRevertToFixedSlots;

    // ── Post-render text entries
    private record LabelEntry(int cx, int y, String text, int color, boolean shadow) {}
    private final List<LabelEntry> postLabels = new ArrayList<>();

    public HotbarCyclerConfigScreen(Screen parent) {
        super(Text.literal("Hotbar Cyclerrr Settings"));
        this.parent = parent;
    }

    @Override public boolean shouldCloseOnEsc() { return true; }
    @Override public boolean shouldPause()      { return false; }
    @Override public void close() { if (client != null) client.setScreen(parent); }

    @Override
    protected void init() {
        postLabels.clear();
        HotbarCyclerConfig cfg = HotbarCyclerConfig.get();

        wMode               = (cfg.antiCheatMode != null) ? cfg.antiCheatMode : AntiCheatMode.NONE;
        wPauseSprint        = cfg.getEffectivePauseSprint();
        wFreezeMovement     = cfg.getEffectiveFreezeMovement();
        wDelay              = cfg.getEffectiveSlotDelay();
        wRowShow            = cfg.showRowIndicator;
        wRevertToFixedSlots = cfg.revertToFixedSlots;

        int cx = width / 2;

        // ── Anti-Cheat Section ────────────────────────────────────────────────
        sectionLabel("-- Anti-Cheat Compatibility --", cx, 18);

        antiCheatBtn = addDrawableChild(ButtonWidget.builder(antiCheatMsg(wMode), b -> {
            wMode = wMode.next();
            if (wMode != AntiCheatMode.CUSTOM) {
                wPauseSprint    = wMode.isPauseSprint();
                wFreezeMovement = wMode.isFreezeMovement();
                wDelay          = wMode.getDelayMs();
            }
            b.setMessage(antiCheatMsg(wMode));
            if (pauseSprintBtn != null) pauseSprintBtn.setMessage(toggleMsg("Pause Sprint", wPauseSprint));
            if (freezeMovementBtn != null) freezeMovementBtn.setMessage(toggleMsg("Freeze Movement", wFreezeMovement));
            if (delayField != null) delayField.setText(String.valueOf(wDelay));
        }).dimensions(cx - 154, 29, 308, BTN_H).build());

        // Note: description rendered dynamically at y = 52 in render()

        pauseSprintBtn = addDrawableChild(ButtonWidget.builder(toggleMsg("Pause Sprint", wPauseSprint), b -> {
            wPauseSprint = !wPauseSprint;
            wMode = AntiCheatMode.CUSTOM;
            antiCheatBtn.setMessage(antiCheatMsg(wMode));
            b.setMessage(toggleMsg("Pause Sprint", wPauseSprint));
        }).dimensions(cx - 154, 64, 150, BTN_H).build());

        freezeMovementBtn = addDrawableChild(ButtonWidget.builder(toggleMsg("Freeze Movement", wFreezeMovement), b -> {
            wFreezeMovement = !wFreezeMovement;
            wMode = AntiCheatMode.CUSTOM;
            antiCheatBtn.setMessage(antiCheatMsg(wMode));
            b.setMessage(toggleMsg("Freeze Movement", wFreezeMovement));
        }).dimensions(cx + 4, 64, 150, BTN_H).build());

        postLabels.add(new LabelEntry(cx - 28, 93, "Slot Delay:", C_WHITE, true));
        delayField = new TextFieldWidget(textRenderer, cx + 12, 88, 42, 18, Text.empty());
        delayField.setText(String.valueOf(wDelay));
        delayField.setMaxLength(5);
        delayField.setTextPredicate(s -> s.matches("\\d{0,5}"));
        delayField.setChangedListener(s -> {
            wDelay = parseInt(delayField, 0);
            if (wMode != AntiCheatMode.CUSTOM && wDelay != wMode.getDelayMs()) {
                wMode = AntiCheatMode.CUSTOM;
                if (antiCheatBtn != null) antiCheatBtn.setMessage(antiCheatMsg(wMode));
            }
        });
        addDrawableChild(delayField);
        postLabels.add(new LabelEntry(cx + 70, 93, "ms", C_WHITE, true));

        // ── Visibility & Revert Section ──────────────────────────────────────
        sectionLabel("-- Visibility & Fixed Slots --", cx, 114);

        rowShowBtn = addDrawableChild(ButtonWidget.builder(toggleMsg("Show Row Indicator", wRowShow), b -> {
            wRowShow = !wRowShow; b.setMessage(toggleMsg("Show Row Indicator", wRowShow));
        }).dimensions(cx - 154, 126, 150, BTN_H).build());

        revertToFixedBtn = addDrawableChild(ButtonWidget.builder(toggleMsg("Revert to Fixed Slots", wRevertToFixedSlots), b -> {
            wRevertToFixedSlots = !wRevertToFixedSlots; b.setMessage(toggleMsg("Revert to Fixed Slots", wRevertToFixedSlots));
        }).dimensions(cx + 4, 126, 150, BTN_H).build());

        smallLabel("Left: HUD squares indicator.  Right: return items on menu open.", cx, 150);

        // ── Position & Size Section ───────────────────────────────────────────
        sectionLabel("-- Indicator Position & Size --", cx, 165);

        int fX1 = cx - 105, fX2 = cx - 35, fX3 = cx + 35, fX4 = cx + 105;
        postLabels.add(new LabelEntry(fX1, 177, "Offset X", C_WHITE, true));
        postLabels.add(new LabelEntry(fX2, 177, "Offset Y", C_WHITE, true));
        postLabels.add(new LabelEntry(fX3, 177, "Width",    C_WHITE, true));
        postLabels.add(new LabelEntry(fX4, 177, "Height",   C_WHITE, true));

        rowOffX = mkField(fX1 - 18, 188, cfg.rowIndicatorOffsetX, true);
        rowOffY = mkField(fX2 - 18, 188, cfg.rowIndicatorOffsetY, true);
        rowSqW  = mkField(fX3 - 18, 188, cfg.rowIndicatorSquareW, false);
        rowSqH  = mkField(fX4 - 18, 188, cfg.rowIndicatorSquareH, false);

        smallLabel("Default: Offset 0, 0 | Size 8x8", cx, 210);

        int by = Math.max(225, height - 26);
        addDrawableChild(ButtonWidget.builder(Text.literal("Save & Close"),
                b -> saveAndClose()).dimensions(cx - 154, by, 150, BTN_H).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"),
                b -> close()).dimensions(cx + 4, by, 150, BTN_H).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 6, C_WHITE);

        super.render(ctx, mouseX, mouseY, delta);

        for (LabelEntry e : postLabels) {
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(e.text()), e.cx(), e.y(), e.color());
        }

        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§7" + wMode.getDescription() + "§r"), width / 2, 52, C_GRAY);
    }

    private void saveAndClose() {
        HotbarCyclerConfig cfg = HotbarCyclerConfig.get();

        cfg.antiCheatMode        = wMode;
        cfg.customPauseSprint    = wPauseSprint;
        cfg.customFreezeMovement = wFreezeMovement;
        cfg.customSlotDelay      = parseInt(delayField, wDelay);
        if (rowShowBtn       != null) cfg.showRowIndicator   = wRowShow;
        if (revertToFixedBtn != null) cfg.revertToFixedSlots = wRevertToFixedSlots;
        if (rowOffX          != null) cfg.rowIndicatorOffsetX = parseInt(rowOffX, 0);
        if (rowOffY          != null) cfg.rowIndicatorOffsetY = parseInt(rowOffY, 0);
        if (rowSqW           != null) cfg.rowIndicatorSquareW = Math.max(1, parseInt(rowSqW, 8));
        if (rowSqH           != null) cfg.rowIndicatorSquareH = Math.max(1, parseInt(rowSqH, 8));

        cfg.save();
        close();
    }

    private void sectionLabel(String text, int cx, int y) {
        postLabels.add(new LabelEntry(cx, y, text, C_GOLD, true));
    }

    private void smallLabel(String text, int cx, int y) {
        postLabels.add(new LabelEntry(cx, y, text, C_GRAY, false));
    }

    private TextFieldWidget mkField(int x, int y, int value, boolean signed) {
        TextFieldWidget f = new TextFieldWidget(textRenderer, x, y, 36, 18, Text.empty());
        f.setText(String.valueOf(value));
        f.setMaxLength(5);
        f.setTextPredicate(signed ? s -> s.matches("-?\\d{0,4}") : s -> s.matches("\\d{0,4}"));
        return addDrawableChild(f);
    }

    private static Text antiCheatMsg(AntiCheatMode mode) {
        String color = (mode == AntiCheatMode.NONE) ? "§c" : "§a";
        return Text.literal("Anti-Cheat: " + color + mode.getDisplayName() + "§r");
    }

    private static Text toggleMsg(String label, boolean on) {
        return Text.literal(label + ": " + (on ? "§aON§r" : "§cOFF§r"));
    }

    private static int parseInt(TextFieldWidget f, int fallback) {
        if (f == null) return fallback;
        try { return Integer.parseInt(f.getText().trim()); }
        catch (NumberFormatException e) { return fallback; }
    }
}
