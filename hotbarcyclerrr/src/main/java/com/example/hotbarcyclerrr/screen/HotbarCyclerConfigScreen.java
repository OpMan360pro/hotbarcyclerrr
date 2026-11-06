package com.example.hotbarcyclerrr.screen;

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
    private static final int FIELD_W      = 48;
    private static final int FIELD_H      = 18;
    private static final int ITEM_H       = 60;
    private static final int CONTENT_TOP  = 30;

    // ── Colors ────────────────────────────────────────────────────────────────
    private static final int C_WHITE   = 0xFFFFFFFF;
    private static final int C_GOLD    = 0xFFFFD700;
    private static final int C_GRAY    = 0xFFAAAAAA;

    // ── State ─────────────────────────────────────────────────────────────────
    private final Screen parent;

    // ── Widgets ───────────────────────────────────────────────────────
    private ButtonWidget    rowShowBtn, seamlessHotbarBtn;
    private TextFieldWidget rowOffX, rowOffY, rowSqW, rowSqH;

    // ── Working booleans ──────────────────────────────────────────────────────
    private boolean wRowShow, wSeamlessHotbar;

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
        rowShowBtn = null; seamlessHotbarBtn = null;
        rowOffX = null; rowOffY = null; rowSqW = null; rowSqH = null;

        HotbarCyclerConfig cfg = HotbarCyclerConfig.get();
        wRowShow  = cfg.showRowIndicator;
        wSeamlessHotbar = cfg.seamlessHotbar;

        int cx = width / 2;
        int y = CONTENT_TOP;

        sectionLabel("-- Visibility --", cx, y); y += 18;

        rowShowBtn = addDrawableChild(ButtonWidget.builder(toggleMsg("Show Row Indicator", wRowShow), b -> {
            wRowShow = !wRowShow; b.setMessage(toggleMsg("Show Row Indicator", wRowShow));
        }).dimensions(cx - BTN_W - 4, y, BTN_W, BTN_H).build());

        seamlessHotbarBtn = addDrawableChild(ButtonWidget.builder(toggleMsg("Seamless Hotbar", wSeamlessHotbar), b -> {
            wSeamlessHotbar = !wSeamlessHotbar; b.setMessage(toggleMsg("Seamless Hotbar", wSeamlessHotbar));
        }).dimensions(cx + 4, y, BTN_W, BTN_H).build());

        smallLabel("Left: 4 squares by hotbar.  Right: keeps active row visible in menu hotbar.", cx, y + BTN_H + 6);
        smallLabel("Seamless is purely visual and does not alter server-side item movements.", cx, y + BTN_H + 18);
        y += BTN_H + 40;

        sectionLabel("-- Position --", cx, y); y += 18;

        fieldRow("Offset X",
                 "Offset Y",
                 cx, y, cfg.rowIndicatorOffsetX, cfg.rowIndicatorOffsetY);
        rowOffX = fieldLeft; rowOffY = fieldRight;
        smallLabel("X shifts the squares left(−)/right(+). Y shifts up(−)/down(+).", cx, y + FIELD_H + 6);
        y += ITEM_H;

        sectionLabel("-- Size --", cx, y); y += 18;

        fieldRow("Square Width",
                 "Square Height",
                 cx, y, cfg.rowIndicatorSquareW, cfg.rowIndicatorSquareH);
        rowSqW = fieldLeft; rowSqH = fieldRight;
        smallLabel("Pixel dimensions of each square. Default: 5×5.", cx, y + FIELD_H + 6);

        int by = height - 26;
        addDrawableChild(ButtonWidget.builder(Text.literal("Save & Close"),
                b -> saveAndClose()).dimensions(cx - BTN_W - 4, by, BTN_W, BTN_H).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"),
                b -> close()).dimensions(cx + 4, by, BTN_W, BTN_H).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);

        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 7, C_WHITE);

        super.render(ctx, mouseX, mouseY, delta);

        for (LabelEntry e : postLabels) {
            if (e.shadow()) {
                ctx.drawCenteredTextWithShadow(textRenderer,
                        Text.literal(e.text()), e.cx(), e.y(), e.color());
            } else {
                ctx.drawCenteredTextWithShadow(textRenderer,
                        Text.literal(e.text()), e.cx(), e.y(), e.color());
            }
        }
    }

    private void saveAndClose() {
        HotbarCyclerConfig cfg = HotbarCyclerConfig.get();

        if (rowShowBtn        != null) cfg.showRowIndicator        = wRowShow;
        if (seamlessHotbarBtn != null) cfg.seamlessHotbar          = wSeamlessHotbar;
        if (rowOffX           != null) cfg.rowIndicatorOffsetX     = parseInt(rowOffX,    0);
        if (rowOffY     != null) cfg.rowIndicatorOffsetY     = parseInt(rowOffY,    0);
        if (rowSqW      != null) cfg.rowIndicatorSquareW     = Math.max(1, parseInt(rowSqW, 5));
        if (rowSqH      != null) cfg.rowIndicatorSquareH     = Math.max(1, parseInt(rowSqH, 5));

        cfg.save();
        close();
    }

    private void sectionLabel(String text, int cx, int y) {
        postLabels.add(new LabelEntry(cx, y, text, C_GOLD, true));
    }

    private void smallLabel(String text, int cx, int y) {
        postLabels.add(new LabelEntry(cx, y, text, C_GRAY, false));
    }

    private TextFieldWidget fieldLeft, fieldRight;

    private void fieldRow(String leftLabel, String rightLabel,
                          int cx, int y, int leftVal, int rightVal) {
        int lCx = cx - 45;
        int rCx = cx + 45;
        postLabels.add(new LabelEntry(lCx, y - 14, leftLabel,  C_WHITE, true));
        postLabels.add(new LabelEntry(rCx, y - 14, rightLabel, C_WHITE, true));
        fieldLeft  = mkField(lCx - FIELD_W / 2, y, leftVal);
        fieldRight = mkField(rCx - FIELD_W / 2, y, rightVal);
    }

    private TextFieldWidget mkField(int x, int y, int value) {
        TextFieldWidget f = new TextFieldWidget(textRenderer, x, y, FIELD_W, FIELD_H, Text.empty());
        f.setText(String.valueOf(value));
        f.setMaxLength(6);
        f.setTextPredicate(s -> s.matches("-?\\d{0,5}"));
        return addDrawableChild(f);
    }

    private static Text toggleMsg(String label, boolean on) {
        return Text.literal(label + ": " + (on ? "§aON§r" : "§cOFF§r"));
    }

    private static int parseInt(TextFieldWidget f, int fallback) {
        try { return Integer.parseInt(f.getText().trim()); }
        catch (NumberFormatException e) { return fallback; }
    }
}
