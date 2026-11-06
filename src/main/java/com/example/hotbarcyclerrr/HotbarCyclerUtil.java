package com.example.hotbarcyclerrr;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

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

    public static void drawActiveRowHighlight(ScreenHandler handler, DrawContext context, int originX, int originY) {
        if (handler == null) return;
        int page = HotbarCyclerClient.getCurrentPage();

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        boolean found = false;

        for (Slot slot : handler.slots) {
            if (!slot.isEnabled() || slot.x < -100 || slot.y < -100) continue;
            int row = getSlotRow(slot);
            if (row == page) {
                int sx = originX + slot.x;
                int sy = originY + slot.y;
                minX = Math.min(minX, sx);
                maxX = Math.max(maxX, sx + 16);
                minY = Math.min(minY, sy);
                maxY = Math.max(maxY, sy + 16);
                found = true;
            }
        }

        if (found && maxX > minX && maxY > minY) {
            int boxX = minX - 1;
            int boxY = minY - 1;
            int boxW = (maxX - minX) + 2;
            int boxH = (maxY - minY) + 2;

            // 2px solid white border around active row
            context.fill(boxX - 1,        boxY - 1,        boxX + boxW + 1, boxY + 1,        0xFFFFFFFF);
            context.fill(boxX - 1,        boxY + boxH - 1, boxX + boxW + 1, boxY + boxH + 1, 0xFFFFFFFF);
            context.fill(boxX - 1,        boxY - 1,        boxX + 1,        boxY + boxH + 1, 0xFFFFFFFF);
            context.fill(boxX + boxW - 1, boxY - 1,        boxX + boxW + 1, boxY + boxH + 1, 0xFFFFFFFF);
            // Clearly visible translucent tint
            context.fill(boxX + 1,        boxY + 1,        boxX + boxW - 1, boxY + boxH - 1, 0x35FFFFFF);
        }
    }

    public static int getSlotRow(Slot slot) {
        if (!(slot.inventory instanceof PlayerInventory)) return -1;
        int idx = slot.getIndex();

        // Rows 1, 2, 3 (consistent across Survival, Creative, and Containers)
        if (idx >= 9 && idx < 18) return 1;
        if (idx >= 18 && idx < 27) return 2;
        if (idx >= 27 && idx < 36) return 3;

        // Hotbar (Row 0):
        // In Survival & Containers: idx 0..8, slot.y >= 100
        // In Creative inventory: idx 36..44, slot.y >= 100
        // Armor (idx 36..39 in Survival, idx 5..8 in Creative) has slot.y < 80, so it is strictly excluded.
        if (slot.y >= 100) {
            if (idx >= 0 && idx < 9) return 0;
            if (idx >= 36 && idx < 45) return 0;
        }

        return -1;
    }

    public static ItemStack getVisualStack(Slot slot) {
        if (slot == null) return ItemStack.EMPTY;
        HotbarCyclerConfig cfg = HotbarCyclerConfig.get();
        if (cfg == null || !cfg.seamlessHotbar) {
            return slot.getStack();
        }
        int page = HotbarCyclerClient.getCurrentPage();
        if (page == 0) {
            return slot.getStack();
        }
        if (!(slot.inventory instanceof PlayerInventory inv)) {
            return slot.getStack();
        }
        int row = getSlotRow(slot);
        if (row != 0 && row != page) {
            return slot.getStack();
        }
        int col = slot.getIndex() % 9;
        if (col < 0 || col > 8) {
            return slot.getStack();
        }
        int targetIdx = (row == 0) ? (page * 9 + col) : col;
        if (targetIdx >= 0 && targetIdx < inv.getMainStacks().size()) {
            return inv.getMainStacks().get(targetIdx);
        }
        return slot.getStack();
    }
}
