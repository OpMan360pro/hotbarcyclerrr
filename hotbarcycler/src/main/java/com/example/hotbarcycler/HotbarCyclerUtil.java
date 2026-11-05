package com.example.hotbarcycler;

import net.minecraft.client.gui.DrawContext;

public class HotbarCyclerUtil {

    public static void drawIndicatorCell(DrawContext ctx, int x, int y, int w, int h, boolean active) {
        // Slot background
        ctx.fill(x, y, x + w, y + h, 0x88000000);

        // Slot beveled borders (vanilla hotbar slot styling)
        ctx.fill(x,         y,         x + w,     y + 1,     0xFF373737); // top dark
        ctx.fill(x,         y,         x + 1,     y + h,     0xFF373737); // left dark
        ctx.fill(x + w - 1, y,         x + w,     y + h,     0xFF8B8B8B); // right highlight
        ctx.fill(x,         y + h - 1, x + w,     y + h,     0xFF8B8B8B); // bottom highlight

        if (active) {
            int pad = Math.max(1, Math.min(w, h) >= 8 ? 2 : 1);
            ctx.fill(x + pad, y + pad, x + w - pad, y + h - pad, 0xFFFFFFFF);
        }
    }
}
