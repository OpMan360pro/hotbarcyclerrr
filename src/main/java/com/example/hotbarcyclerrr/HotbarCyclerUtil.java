package com.example.hotbarcyclerrr;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class HotbarCyclerUtil {

    public static void swapSingleSlot(PlayerInventory inv, int fromRow, int toRow, int slotIndex) {
        if (inv == null || slotIndex < 0 || slotIndex >= 9) return;
        var stacks = inv.getMainStacks();
        if (fromRow >= 1 && fromRow <= 3) {
            int fromIdx = fromRow * 9 + slotIndex;
            if (fromIdx < stacks.size() && slotIndex < stacks.size()) {
                ItemStack temp = stacks.get(slotIndex);
                stacks.set(slotIndex, stacks.get(fromIdx));
                stacks.set(fromIdx, temp);
            }
        }
        if (toRow >= 1 && toRow <= 3) {
            int toIdx = toRow * 9 + slotIndex;
            if (toIdx < stacks.size() && slotIndex < stacks.size()) {
                ItemStack temp = stacks.get(slotIndex);
                stacks.set(slotIndex, stacks.get(toIdx));
                stacks.set(toIdx, temp);
            }
        }
    }

    public static void outline(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x,         y,         x + w, y + 1,     color);
        ctx.fill(x,         y + h - 1, x + w, y + h,     color);
        ctx.fill(x,         y,         x + 1, y + h,     color);
        ctx.fill(x + w - 1, y,         x + w, y + h,     color);
    }

    public static void drawIndicatorCell(DrawContext ctx, int x, int y, int w, int h, boolean active) {
        drawIndicatorCell(ctx, x, y, w, h, active, 0, RowIndicatorStyle.DEFAULT);
    }

    public static void drawIndicatorCell(DrawContext ctx, int x, int y, int w, int h, boolean active, int rowId, RowIndicatorStyle style) {
        if (style == null) style = RowIndicatorStyle.DEFAULT;

        switch (style) {
            case MINIMAL -> {
                if (active) {
                    ctx.fill(x, y, x + w, y + h, 0xEEFFFFFF);
                    outline(ctx, x, y, w, h, 0xFF222222);
                } else {
                    ctx.fill(x, y, x + w, y + h, 0x44000000);
                    outline(ctx, x, y, w, h, 0x66FFFFFF);
                }
            }
            case DOTS -> {
                ctx.fill(x, y, x + w, y + h, 0x33000000);
                if (active) {
                    int pad = Math.max(1, Math.min(w, h) / 4);
                    ctx.fill(x + pad, y + pad, x + w - pad, y + h - pad, 0xFFFFFFFF);
                } else {
                    int pad = Math.max(2, Math.min(w, h) * 3 / 8);
                    ctx.fill(x + pad, y + pad, x + w - pad, y + h - pad, 0x77FFFFFF);
                }
            }
            case ACCENT -> {
                HotbarCyclerConfig cfg = HotbarCyclerConfig.get();
                int rgb = (cfg != null) ? cfg.getRowIndicatorAccentRgb() : 0x00AAFF;
                if (active) {
                    ctx.fill(x, y, x + w, y + h, (0x66 << 24) | rgb);
                    outline(ctx, x, y, w, h, (0xFF << 24) | rgb);
                    int pad = Math.max(1, Math.min(w, h) >= 8 ? 2 : 1);
                    ctx.fill(x + pad, y + pad, x + w - pad, y + h - pad, (0xEE << 24) | rgb);
                } else {
                    ctx.fill(x, y, x + w, y + h, 0x44000000);
                    outline(ctx, x, y, w, h, (0x44 << 24) | rgb);
                }
            }
            case DEFAULT -> {
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

            HotbarCyclerConfig cfg = HotbarCyclerConfig.get();
            int borderColor = (cfg != null) ? cfg.getHighlightBorderArgb() : 0xFFFFFFFF;
            int fillColor   = (cfg != null) ? cfg.getHighlightFillArgb()   : 0x35FFFFFF;

            if ((borderColor >>> 24) > 0) {
                // 2px solid border around active row
                context.fill(boxX - 1,        boxY - 1,        boxX + boxW + 1, boxY + 1,        borderColor);
                context.fill(boxX - 1,        boxY + boxH - 1, boxX + boxW + 1, boxY + boxH + 1, borderColor);
                context.fill(boxX - 1,        boxY - 1,        boxX + 1,        boxY + boxH + 1, borderColor);
                context.fill(boxX + boxW - 1, boxY - 1,        boxX + boxW + 1, boxY + boxH + 1, borderColor);
            }
            if ((fillColor >>> 24) > 0) {
                // Clearly visible translucent tint
                context.fill(boxX + 1,        boxY + 1,        boxX + boxW - 1, boxY + boxH - 1, fillColor);
            }
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
