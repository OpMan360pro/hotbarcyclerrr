package com.example.hotbarcyclerrr.screen;

import com.example.hotbarcyclerrr.AntiCheatMode;
import com.example.hotbarcyclerrr.HotbarCyclerConfig;
import com.example.hotbarcyclerrr.HotbarCyclerUtil;
import com.example.hotbarcyclerrr.RowIndicatorStyle;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class HotbarCyclerConfigScreen extends Screen {

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int BTN_W = 150;
    private static final int BTN_H = 20;

    // ── Colors ────────────────────────────────────────────────────────────────
    private static final int C_WHITE = 0xFFFFFFFF;
    private static final int C_GOLD  = 0xFFFFD700;
    private static final int C_GRAY  = 0xFFAAAAAA;

    private enum ColorTarget {
        BORDER("Border"),
        FILL("Fill"),
        ACCENT("Accent");

        private final String title;
        ColorTarget(String title) { this.title = title; }
        public String getTitle() { return title; }
    }

    // ── State ─────────────────────────────────────────────────────────────────
    private final Screen parent;
    private int currentTab = 0; // 0 = General & Anti-Cheat, 1 = Visuals & Design

    // ── Tab Navigation ────────────────────────────────────────────────────────
    private ButtonWidget tabGeneralBtn;
    private ButtonWidget tabVisualsBtn;
    private final List<ClickableWidget> tab0Widgets = new ArrayList<>();
    private final List<ClickableWidget> tab1Widgets = new ArrayList<>();

    // ── Tab 0 Widgets (Anti-Cheat & Gameplay) ──────────────────────────────────
    private ButtonWidget    antiCheatBtn, pauseSprintBtn, blockInputBtn, freezeMovementBtn;
    private TextFieldWidget delayField;
    private ButtonWidget    revertToFixedBtn, revertDelayBtn;

    // ── Tab 1 Widgets (Visuals & Indicator) ───────────────────────────────────
    private ButtonWidget      rowShowBtn, rowStyleBtn, accentQuickBtn;
    private TextFieldWidget   rowOffX, rowOffY, rowSqW, rowSqH;

    // ── Color Configuration Widgets ───────────────────────────────────────────
    private ColorTarget       activeTarget = ColorTarget.BORDER;
    private ButtonWidget      targetBorderBtn, targetFillBtn, targetAccentBtn;
    private TextFieldWidget   hexInputField;
    private ColorSliderWidget sliderR, sliderG, sliderB, sliderA;
    private boolean           updatingSliders = false;

    // ── Working state ─────────────────────────────────────────────────────────
    private AntiCheatMode     wMode;
    private boolean           wPauseSprint, wBlockInput, wFreezeMovement;
    private int               wDelay;
    private boolean           wRowShow, wRevertToFixedSlots, wRevertSlotDelay;
    private RowIndicatorStyle wStyle;
    private String            wBorderHex, wFillHex, wAccentHex;
    private int               wBorderAlpha, wFillAlpha;

    public HotbarCyclerConfigScreen(Screen parent) {
        super(Text.literal("Hotbar Cyclerrr Settings"));
        this.parent = parent;
    }

    @Override public boolean shouldCloseOnEsc() { return true; }
    @Override public boolean shouldPause()      { return false; }
    @Override public void close() { if (client != null) client.setScreen(parent); }

    @Override
    protected void init() {
        tab0Widgets.clear();
        tab1Widgets.clear();

        HotbarCyclerConfig cfg = HotbarCyclerConfig.get();

        wMode               = (cfg.antiCheatMode != null) ? cfg.antiCheatMode : AntiCheatMode.NONE;
        wPauseSprint        = cfg.getEffectivePauseSprint();
        wBlockInput         = cfg.getEffectiveBlockInput();
        wFreezeMovement     = cfg.getEffectiveFreezeMovement();
        wDelay              = (wMode == AntiCheatMode.CUSTOM) ? cfg.customSlotDelay : cfg.getEffectiveSlotDelay();
        wRowShow            = com.example.hotbarcyclerrr.HotbarCyclerClient.isRowIndicatorVisible();
        wStyle              = (cfg.rowIndicatorStyle != null) ? cfg.rowIndicatorStyle : RowIndicatorStyle.DEFAULT;
        wRevertToFixedSlots = cfg.revertToFixedSlots;
        wRevertSlotDelay    = cfg.revertSlotDelay;
        wBorderHex          = (cfg.highlightBorderColor != null) ? cfg.highlightBorderColor : "FFFFFF";
        wBorderAlpha        = (cfg.highlightBorderOpacity >= 0 && cfg.highlightBorderOpacity <= 100) ? cfg.highlightBorderOpacity : 100;
        wFillHex            = (cfg.highlightFillColor != null) ? cfg.highlightFillColor : "FFFFFF";
        wFillAlpha          = (cfg.highlightFillOpacity >= 0 && cfg.highlightFillOpacity <= 100) ? cfg.highlightFillOpacity : 21;
        wAccentHex          = (cfg.rowIndicatorAccentColor != null) ? cfg.rowIndicatorAccentColor : "00AAFF";

        int cx = width / 2;

        // ── Tab Buttons ───────────────────────────────────────────────────────
        tabGeneralBtn = addDrawableChild(ButtonWidget.builder(Text.literal("General & Anti-Cheat"), b -> switchTab(0))
                .dimensions(cx - 154, 18, 150, BTN_H).build());
        tabVisualsBtn = addDrawableChild(ButtonWidget.builder(Text.literal("Visuals & Design"), b -> switchTab(1))
                .dimensions(cx + 4, 18, 150, BTN_H).build());

        // ── TAB 0: General & Anti-Cheat ───────────────────────────────────────
        antiCheatBtn = registerTab0(ButtonWidget.builder(antiCheatMsg(wMode), b -> {
            wMode = wMode.next();
            if (wMode != AntiCheatMode.CUSTOM) {
                wPauseSprint    = wMode.isPauseSprint();
                wBlockInput     = wMode.isBlockInput();
                wFreezeMovement = wMode.isFreezeMovement();
                wDelay          = wMode.getDelayMs();
            } else {
                wPauseSprint    = cfg.customPauseSprint;
                wBlockInput     = cfg.customBlockInput;
                wFreezeMovement = cfg.customFreezeMovement;
                wDelay          = cfg.customSlotDelay;
            }
            b.setMessage(antiCheatMsg(wMode));
            if (pauseSprintBtn != null) pauseSprintBtn.setMessage(toggleMsg("Pause Sprint", wPauseSprint));
            if (blockInputBtn != null) blockInputBtn.setMessage(toggleMsg("Block Input", wBlockInput));
            if (freezeMovementBtn != null) freezeMovementBtn.setMessage(toggleMsg("Freeze Movement", wFreezeMovement));
            if (delayField != null) delayField.setText(String.valueOf(wDelay));
        }).dimensions(cx - 154, 54, 308, BTN_H).build());

        pauseSprintBtn = registerTab0(ButtonWidget.builder(toggleMsg("Pause Sprint", wPauseSprint), b -> {
            wPauseSprint = !wPauseSprint;
            wMode = AntiCheatMode.CUSTOM;
            antiCheatBtn.setMessage(antiCheatMsg(wMode));
            b.setMessage(toggleMsg("Pause Sprint", wPauseSprint));
        }).dimensions(cx - 154, 92, 150, BTN_H).build());

        blockInputBtn = registerTab0(ButtonWidget.builder(toggleMsg("Block Input", wBlockInput), b -> {
            wBlockInput = !wBlockInput;
            wMode = AntiCheatMode.CUSTOM;
            antiCheatBtn.setMessage(antiCheatMsg(wMode));
            b.setMessage(toggleMsg("Block Input", wBlockInput));
        }).dimensions(cx + 4, 92, 150, BTN_H).build());

        freezeMovementBtn = registerTab0(ButtonWidget.builder(toggleMsg("Freeze Movement", wFreezeMovement), b -> {
            wFreezeMovement = !wFreezeMovement;
            wMode = AntiCheatMode.CUSTOM;
            antiCheatBtn.setMessage(antiCheatMsg(wMode));
            b.setMessage(toggleMsg("Freeze Movement", wFreezeMovement));
        }).dimensions(cx - 154, 116, 150, BTN_H).build());

        delayField = new TextFieldWidget(textRenderer, cx + 78, 116, 40, 18, Text.literal(""));
        delayField.setText(String.valueOf(wDelay));
        delayField.setMaxLength(5);
        delayField.setTextPredicate(s -> s.matches("\\d{0,5}"));
        delayField.setChangedListener(s -> {
            wDelay = parseInt(delayField, 0);
            if (wDelay > 0 && wMode != AntiCheatMode.CUSTOM) {
                wMode = AntiCheatMode.CUSTOM;
                if (antiCheatBtn != null) antiCheatBtn.setMessage(antiCheatMsg(wMode));
            }
        });
        registerTab0(delayField);

        revertToFixedBtn = registerTab0(ButtonWidget.builder(toggleMsg("Revert to Fixed Slots", wRevertToFixedSlots), b -> {
            wRevertToFixedSlots = !wRevertToFixedSlots;
            b.setMessage(toggleMsg("Revert to Fixed Slots", wRevertToFixedSlots));
            if (revertDelayBtn != null) revertDelayBtn.active = wRevertToFixedSlots;
        }).dimensions(cx - 154, 156, 150, BTN_H).build());

        revertDelayBtn = registerTab0(ButtonWidget.builder(toggleMsg("Revert Slot Delay", wRevertSlotDelay), b -> {
            wRevertSlotDelay = !wRevertSlotDelay;
            b.setMessage(toggleMsg("Revert Slot Delay", wRevertSlotDelay));
        }).dimensions(cx + 4, 156, 150, BTN_H).build());
        revertDelayBtn.active = wRevertToFixedSlots;

        // ── TAB 1: Visuals & Design ───────────────────────────────────────────
        rowShowBtn = registerTab1(ButtonWidget.builder(toggleMsg("Show Row Indicator", wRowShow), b -> {
            wRowShow = !wRowShow;
            b.setMessage(toggleMsg("Show Row Indicator", wRowShow));
        }).dimensions(cx - 154, 52, 150, BTN_H).build());

        rowStyleBtn = registerTab1(ButtonWidget.builder(indicatorStyleMsg(wStyle), b -> {
            wStyle = wStyle.next();
            b.setMessage(indicatorStyleMsg(wStyle));
            updateStyleLayout();
        }).dimensions(cx + 4, 52, 150, BTN_H).build());

        accentQuickBtn = registerTab1(ButtonWidget.builder(Text.literal("Color"), b -> {
            switchColorTarget(ColorTarget.ACCENT);
        }).dimensions(cx + 116, 52, 38, BTN_H).build());

        int fX1 = cx - 105, fX2 = cx - 35, fX3 = cx + 35, fX4 = cx + 105;
        rowOffX = registerTab1(mkField(fX1 - 18, 85, cfg.rowIndicatorOffsetX, true));
        rowOffY = registerTab1(mkField(fX2 - 18, 85, cfg.rowIndicatorOffsetY, true));
        rowSqW  = registerTab1(mkField(fX3 - 18, 85, cfg.rowIndicatorSquareW, false));
        rowSqH  = registerTab1(mkField(fX4 - 18, 85, cfg.rowIndicatorSquareH, false));

        targetBorderBtn = registerTab1(ButtonWidget.builder(Text.literal("Border"), b -> switchColorTarget(ColorTarget.BORDER))
                .dimensions(cx - 154, 117, 100, 18).build());
        targetFillBtn = registerTab1(ButtonWidget.builder(Text.literal("Fill"), b -> switchColorTarget(ColorTarget.FILL))
                .dimensions(cx - 50, 117, 100, 18).build());
        targetAccentBtn = registerTab1(ButtonWidget.builder(Text.literal("Accent"), b -> switchColorTarget(ColorTarget.ACCENT))
                .dimensions(cx + 54, 117, 100, 18).build());

        hexInputField = registerTab1(mkHexField(cx - 96, 138, 48, wBorderHex));
        hexInputField.setChangedListener(s -> {
            if (updatingSliders) return;
            if (s.length() == 6) {
                try {
                    int rgb = (int) Long.parseLong(s, 16) & 0x00FFFFFF;
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    String upper = s.toUpperCase();
                    switch (activeTarget) {
                        case BORDER -> wBorderHex = upper;
                        case FILL -> wFillHex = upper;
                        case ACCENT -> wAccentHex = upper;
                    }
                    updatingSliders = true;
                    sliderR.setValueExact(r / 255.0);
                    sliderG.setValueExact(g / 255.0);
                    sliderB.setValueExact(b / 255.0);
                    updatingSliders = false;
                } catch (NumberFormatException ignored) {}
            }
        });

        sliderR = registerTab1(new ColorSliderWidget(cx - 154, 158, 150, 16, "Red", 1.0, false, v -> onSliderChange()));
        sliderG = registerTab1(new ColorSliderWidget(cx + 4,   158, 150, 16, "Green", 1.0, false, v -> onSliderChange()));
        sliderB = registerTab1(new ColorSliderWidget(cx - 154, 177, 150, 16, "Blue", 1.0, false, v -> onSliderChange()));
        sliderA = registerTab1(new ColorSliderWidget(cx + 4,   177, 150, 16, "Opacity", 1.0, true, v -> onSliderChange()));

        updateStyleLayout();
        switchColorTarget(ColorTarget.BORDER);

        // ── Bottom Buttons ────────────────────────────────────────────────────
        int by = Math.max(228, height - 26);
        addDrawableChild(ButtonWidget.builder(Text.literal("Save & Close"),
                b -> saveAndClose()).dimensions(cx - 154, by, 150, BTN_H).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"),
                b -> close()).dimensions(cx + 4, by, 150, BTN_H).build());

        switchTab(currentTab);
    }

    private <T extends ClickableWidget> T registerTab0(T widget) {
        tab0Widgets.add(widget);
        return addDrawableChild(widget);
    }

    private <T extends ClickableWidget> T registerTab1(T widget) {
        tab1Widgets.add(widget);
        return addDrawableChild(widget);
    }

    private void switchTab(int tab) {
        currentTab = tab;
        tabGeneralBtn.setMessage(Text.literal(currentTab == 0 ? "§e§lGeneral & Anti-Cheat§r" : "General & Anti-Cheat"));
        tabVisualsBtn.setMessage(Text.literal(currentTab == 1 ? "§e§lVisuals & Design§r" : "Visuals & Design"));
        for (ClickableWidget w : tab0Widgets) {
            w.visible = (currentTab == 0);
        }
        for (ClickableWidget w : tab1Widgets) {
            w.visible = (currentTab == 1);
        }
        if (currentTab == 1) {
            updateStyleLayout();
            syncSlidersToActiveTarget();
        }
    }

    private static void setBounds(ClickableWidget w, int x, int y, int width, int height) {
        if (w != null) {
            w.setPosition(x, y);
            w.setDimensions(width, height);
        }
    }

    private void updateStyleLayout() {
        int cx = width / 2;
        boolean isAccent = (wStyle == RowIndicatorStyle.ACCENT);
        if (isAccent) {
            setBounds(rowStyleBtn, cx + 4, 52, 108, BTN_H);
            accentQuickBtn.visible = (currentTab == 1);
            setBounds(accentQuickBtn, cx + 116, 52, 38, BTN_H);

            setBounds(targetBorderBtn, cx - 154, 117, 100, 18);
            setBounds(targetFillBtn, cx - 50, 117, 100, 18);
            targetAccentBtn.visible = (currentTab == 1);
            setBounds(targetAccentBtn, cx + 54, 117, 100, 18);
        } else {
            setBounds(rowStyleBtn, cx + 4, 52, 150, BTN_H);
            accentQuickBtn.visible = false;

            setBounds(targetBorderBtn, cx - 154, 117, 150, 18);
            setBounds(targetFillBtn, cx + 4, 117, 150, 18);
            targetAccentBtn.visible = false;

            if (activeTarget == ColorTarget.ACCENT) {
                switchColorTarget(ColorTarget.BORDER);
            }
        }
        updateTargetButtons();
    }

    private void updateTargetButtons() {
        targetBorderBtn.setMessage(Text.literal(activeTarget == ColorTarget.BORDER ? "§a§l▶ Border ◀§r" : "Border"));
        targetFillBtn.setMessage(Text.literal(activeTarget == ColorTarget.FILL ? "§a§l▶ Fill ◀§r" : "Fill"));
        targetAccentBtn.setMessage(Text.literal(activeTarget == ColorTarget.ACCENT ? "§a§l▶ Accent ◀§r" : "Accent"));
    }

    private void switchColorTarget(ColorTarget target) {
        this.activeTarget = target;
        updateTargetButtons();
        syncSlidersToActiveTarget();
    }

    private void syncSlidersToActiveTarget() {
        updatingSliders = true;
        String hex;
        int alpha;
        switch (activeTarget) {
            case BORDER -> {
                hex = wBorderHex;
                alpha = wBorderAlpha;
                sliderA.visible = (currentTab == 1);
            }
            case FILL -> {
                hex = wFillHex;
                alpha = wFillAlpha;
                sliderA.visible = (currentTab == 1);
            }
            case ACCENT -> {
                hex = wAccentHex;
                alpha = 100;
                sliderA.visible = false;
            }
            default -> {
                hex = "FFFFFF";
                alpha = 100;
            }
        }

        int rgb = parseRgb(hex, 0xFFFFFF);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        sliderR.setValueExact(r / 255.0);
        sliderG.setValueExact(g / 255.0);
        sliderB.setValueExact(b / 255.0);
        sliderA.setValueExact(alpha / 100.0);

        if (hexInputField != null) {
            hexInputField.setText(String.format("%06X", rgb));
        }
        updatingSliders = false;
    }

    private void onSliderChange() {
        if (updatingSliders) return;
        int r = (int) Math.round(sliderR.getValue() * 255.0);
        int g = (int) Math.round(sliderG.getValue() * 255.0);
        int b = (int) Math.round(sliderB.getValue() * 255.0);
        int a = (int) Math.round(sliderA.getValue() * 100.0);
        String hex = String.format("%02X%02X%02X", r, g, b);

        switch (activeTarget) {
            case BORDER -> {
                wBorderHex = hex;
                wBorderAlpha = a;
            }
            case FILL -> {
                wFillHex = hex;
                wFillAlpha = a;
            }
            case ACCENT -> {
                wAccentHex = hex;
            }
        }

        if (hexInputField != null) {
            hexInputField.setText(hex);
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        int cx = width / 2;
        ctx.drawCenteredTextWithShadow(textRenderer, title, cx, 6, C_WHITE);

        super.render(ctx, mouseX, mouseY, delta);

        if (currentTab == 0) {
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("-- Anti-Cheat Compatibility --"), cx, 44, C_GOLD);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§7" + wMode.getDescription() + "§r"), cx, 78, C_GRAY);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Slot Delay:"), cx + 38, 121, C_WHITE);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("ms"), cx + 128, 121, C_WHITE);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("-- Fixed Slots Behavior --"), cx, 144, C_GOLD);
        } else {
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("-- HUD Row Indicator --"), cx, 42, C_GOLD);

            if (accentQuickBtn != null && accentQuickBtn.visible) {
                int accRgb = parseRgb(wAccentHex, 0x00AAFF);
                ctx.fill(cx + 143, 54, cx + 151, 62, 0xFF000000 | accRgb);
                HotbarCyclerUtil.outline(ctx, cx + 143, 54, 8, 8, 0xFFFFFFFF);
            }

            int fX1 = cx - 105, fX2 = cx - 35, fX3 = cx + 35, fX4 = cx + 105;
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Offset X"), fX1, 74, C_WHITE);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Offset Y"), fX2, 74, C_WHITE);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Width"),    fX3, 74, C_WHITE);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Height"),   fX4, 74, C_WHITE);

            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("-- Color Customization (RGB Sliders) --"), cx, 105, C_GOLD);

            ctx.drawTextWithShadow(textRenderer, Text.literal("HEX: #"), cx - 146, 142, C_WHITE);
            int curTargetRgb = parseRgb(hexInputField != null ? hexInputField.getText() : "FFFFFF", 0xFFFFFF);
            ctx.fill(cx - 44, 139, cx - 30, 153, 0xFF000000 | curTargetRgb);
            HotbarCyclerUtil.outline(ctx, cx - 44, 139, 14, 14, 0xFFFFFFFF);
            ctx.drawTextWithShadow(textRenderer, Text.literal("§7[ " + activeTarget.getTitle() + " Color ]§r"), cx - 24, 142, C_WHITE);

            renderPreview(ctx, cx, 197);
        }
    }

    private void renderPreview(DrawContext ctx, int cx, int y) {
        int prevW = 162;
        int prevH = 18;
        int prevX = cx - prevW / 2;

        ctx.fill(prevX - 3, y - 3, prevX + prevW + 3, y + prevH + 3, 0xFF181818);

        for (int i = 0; i < 9; i++) {
            int sx = prevX + i * 18;
            ctx.fill(sx + 1, y + 1, sx + 17, y + 17, 0xFF353535);
        }

        int bColor = parseColor(wBorderHex, wBorderAlpha, 0x00FFFFFF, 255);
        int fColor = parseColor(wFillHex, wFillAlpha, 0x00FFFFFF, 53);

        if ((bColor >>> 24) > 0) {
            ctx.fill(prevX - 1,         y - 1,         prevX + prevW + 1, y + 1,         bColor);
            ctx.fill(prevX - 1,         y + prevH - 1, prevX + prevW + 1, y + prevH + 1, bColor);
            ctx.fill(prevX - 1,         y - 1,         prevX + 1,         y + prevH + 1, bColor);
            ctx.fill(prevX + prevW - 1, y - 1,         prevX + prevW + 1, y + prevH + 1, bColor);
        }
        if ((fColor >>> 24) > 0) {
            ctx.fill(prevX + 1, y + 1, prevX + prevW - 1, y + prevH - 1, fColor);
        }

        // Draw preview of HUD indicator next to hotbar slots
        int indX = prevX - 14;
        int indSize = 6;
        for (int row = 0; row < 4; row++) {
            int iy = y + row * (indSize + 1);
            boolean active = (row == 0);
            if (wStyle == RowIndicatorStyle.ACCENT) {
                int rgb = parseRgb(wAccentHex, 0x00AAFF);
                if (active) {
                    ctx.fill(indX, iy, indX + indSize, iy + indSize, (0x66 << 24) | rgb);
                    HotbarCyclerUtil.outline(ctx, indX, iy, indSize, indSize, (0xFF << 24) | rgb);
                    ctx.fill(indX + 1, iy + 1, indX + indSize - 1, iy + indSize - 1, (0xEE << 24) | rgb);
                } else {
                    ctx.fill(indX, iy, indX + indSize, iy + indSize, 0x44000000);
                    HotbarCyclerUtil.outline(ctx, indX, iy, indSize, indSize, (0x44 << 24) | rgb);
                }
            } else {
                HotbarCyclerUtil.drawIndicatorCell(ctx, indX, iy, indSize, indSize, active, row, wStyle);
            }
        }

        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§7Live Preview: Indicator & Hotbar Highlight§r"), cx, y + 21, C_GRAY);
    }

    private static int parseColor(String hex, int opacityPercent, int defaultRgb, int defaultAlpha) {
        int rgb = parseRgb(hex, defaultRgb);
        int alpha = (opacityPercent >= 0 && opacityPercent <= 100)
                ? Math.round(opacityPercent * 255.0f / 100.0f)
                : defaultAlpha;
        alpha = Math.max(0, Math.min(255, alpha));
        return (alpha << 24) | rgb;
    }

    private void saveAndClose() {
        HotbarCyclerConfig cfg = HotbarCyclerConfig.get();

        cfg.antiCheatMode        = wMode;
        cfg.customPauseSprint    = wPauseSprint;
        cfg.customBlockInput     = wBlockInput;
        cfg.customFreezeMovement = wFreezeMovement;
        int delayVal = parseInt(delayField, wDelay);
        cfg.customSlotDelay      = delayVal;
        cfg.slotDelay            = delayVal;

        if (rowShowBtn != null) {
            cfg.showRowIndicator = wRowShow;
            com.example.hotbarcyclerrr.HotbarCyclerClient.setRowIndicatorVisible(wRowShow);
        }
        if (wStyle != null) {
            cfg.rowIndicatorStyle = wStyle;
        }
        if (revertToFixedBtn != null) cfg.revertToFixedSlots = wRevertToFixedSlots;
        if (revertDelayBtn   != null) cfg.revertSlotDelay    = wRevertSlotDelay;
        if (rowOffX          != null) cfg.rowIndicatorOffsetX = parseInt(rowOffX, 0);
        if (rowOffY          != null) cfg.rowIndicatorOffsetY = parseInt(rowOffY, 0);
        if (rowSqW           != null) cfg.rowIndicatorSquareW = Math.max(4, parseInt(rowSqW, 8));
        if (rowSqH           != null) cfg.rowIndicatorSquareH = Math.max(4, parseInt(rowSqH, 8));

        cfg.highlightBorderColor    = sanitizeHex(wBorderHex, "FFFFFF");
        cfg.highlightBorderOpacity  = Math.max(0, Math.min(100, wBorderAlpha));
        cfg.highlightFillColor      = sanitizeHex(wFillHex, "FFFFFF");
        cfg.highlightFillOpacity    = Math.max(0, Math.min(100, wFillAlpha));
        cfg.rowIndicatorAccentColor = sanitizeHex(wAccentHex, "00AAFF");

        cfg.save();
        close();
    }

    private TextFieldWidget mkField(int x, int y, int value, boolean signed) {
        TextFieldWidget f = new TextFieldWidget(textRenderer, x, y, 36, 16, Text.literal(""));
        f.setText(String.valueOf(value));
        f.setMaxLength(5);
        f.setTextPredicate(signed ? s -> s.matches("-?\\d{0,4}") : s -> s.matches("\\d{0,4}"));
        return f;
    }

    private TextFieldWidget mkHexField(int x, int y, int width, String hex) {
        TextFieldWidget f = new TextFieldWidget(textRenderer, x, y, width, 16, Text.literal(""));
        f.setText(hex);
        f.setMaxLength(6);
        f.setTextPredicate(s -> s.matches("[0-9a-fA-F]{0,6}"));
        return f;
    }

    private static String sanitizeHex(String text, String fallback) {
        if (text == null) return fallback;
        String clean = text.replace("#", "").trim();
        if (clean.isEmpty()) return fallback;
        try {
            Long.parseLong(clean, 16);
            return clean.toUpperCase();
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static Text antiCheatMsg(AntiCheatMode mode) {
        String color = (mode == AntiCheatMode.NONE) ? "§c" : "§a";
        return Text.literal("Anti-Cheat: " + color + mode.getDisplayName() + "§r");
    }

    private static Text indicatorStyleMsg(RowIndicatorStyle style) {
        return Text.literal("Indicator Style: §a" + style.getDisplayName() + "§r");
    }

    private static Text toggleMsg(String label, boolean on) {
        return Text.literal(label + ": " + (on ? "§aON§r" : "§cOFF§r"));
    }

    private static int parseInt(TextFieldWidget f, int fallback) {
        if (f == null) return fallback;
        try { return Integer.parseInt(f.getText().trim()); }
        catch (NumberFormatException e) { return fallback; }
    }

    private static int parseRgb(String hex, int defaultRgb) {
        if (hex == null) return defaultRgb;
        String clean = hex.replace("#", "").trim();
        if (clean.isEmpty()) return defaultRgb;
        try {
            return (int) Long.parseLong(clean, 16) & 0x00FFFFFF;
        } catch (Exception e) {
            return defaultRgb;
        }
    }

    private static class ColorSliderWidget extends SliderWidget {
        private final String label;
        private final boolean isPercent;
        private final java.util.function.DoubleConsumer onValueChange;

        public ColorSliderWidget(int x, int y, int width, int height, String label, double initialValue, boolean isPercent, java.util.function.DoubleConsumer onValueChange) {
            super(x, y, width, height, Text.literal(""), Math.max(0.0, Math.min(1.0, initialValue)));
            this.label = label;
            this.isPercent = isPercent;
            this.onValueChange = onValueChange;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            if (isPercent) {
                int pct = (int) Math.round(this.value * 100.0);
                setMessage(Text.literal(label + ": " + pct + "%"));
            } else {
                int val = (int) Math.round(this.value * 255.0);
                setMessage(Text.literal(label + ": " + val));
            }
        }

        @Override
        protected void applyValue() {
            if (onValueChange != null) {
                onValueChange.accept(this.value);
            }
        }

        public void setValueExact(double newValue) {
            this.value = Math.max(0.0, Math.min(1.0, newValue));
            updateMessage();
        }

        public double getValue() {
            return this.value;
        }
    }
}
