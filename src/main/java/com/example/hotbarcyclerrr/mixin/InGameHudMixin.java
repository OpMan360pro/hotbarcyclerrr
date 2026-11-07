package com.example.hotbarcyclerrr.mixin;

import com.example.hotbarcyclerrr.HotbarCyclerClient;
import com.example.hotbarcyclerrr.HotbarCyclerConfig;
import com.example.hotbarcyclerrr.HotbarCyclerUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    @Unique private static final int HOTBAR_HALF_W = 91;
    @Unique private static final int HOTBAR_HEIGHT = 22;
    @Unique private static final int IND_ROWS      = 4;
    @Unique private static final int IND_GAP       = 2;
    @Unique private static final int IND_MARGIN    = 4;
    @Unique private static final int PREV_SLOT_GAP = 2;

    // Shared transparent-inventory palette used by HUD elements
    @Unique private static final int OUTLINE_DIM    = 0x55FFFFFF;
    @Unique private static final int OUTLINE_MID    = 0x88FFFFFF;
    @Unique private static final int OUTLINE_BRIGHT = 0xAAFFFFFF;
    @Unique private static final int SELECTED_FILL  = 0x3300AAFF;
    @Unique private static final int SELECTED_EDGE  = 0xBB00AAFF;
    @Unique private static final int SEP_LINE       = 0x44FFFFFF;

    @Unique
    private static int darkWithAlpha(int alpha) {
        int a = Math.max(0, Math.min(255, alpha));
        if (a == 0) return 0;
        return (a << 24) | 0x1B1B1B;
    }

    @Unique
    private static void outline(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x,         y,         x + w, y + 1,     color);
        ctx.fill(x,         y + h - 1, x + w, y + h,     color);
        ctx.fill(x,         y,         x + 1, y + h,     color);
        ctx.fill(x + w - 1, y,         x + w, y + h,     color);
    }

    @Redirect(
        method = "renderHotbar",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerInventory;getStack(I)Lnet/minecraft/item/ItemStack;"
        )
    )
    private ItemStack hotbarcyclerrr$redirectHotbarSlot(PlayerInventory inv, int slot) {
        HotbarCyclerConfig cfg = HotbarCyclerConfig.get();
        if (cfg != null && cfg.seamlessHotbar && slot >= 0 && slot < 9) {
            int page = HotbarCyclerClient.getCurrentPage();
            if (page > 0 && page < 4) {
                if (!HotbarCyclerClient.isRowSwapped()) {
                    int targetIdx = page * 9 + slot;
                    if (targetIdx >= 0 && targetIdx < inv.getMainStacks().size()) {
                        return inv.getMainStacks().get(targetIdx);
                    }
                }
            }
        }
        return inv.getStack(slot);
    }

    @Inject(method = "renderHotbar", at = @At("TAIL"))
    private void hotbarcyclerrr$onTail(DrawContext ctx,
                                     RenderTickCounter tickCounter,
                                     CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.currentScreen != null) return;

        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();
        int hotbarLeft = sw / 2 - HOTBAR_HALF_W;
        int hotbarTop  = sh - HOTBAR_HEIGHT;
        HotbarCyclerConfig cfg = HotbarCyclerConfig.get();

        if (HotbarCyclerClient.isRowIndicatorVisible())
            hotbarcyclerrr$drawRowIndicator(ctx, hotbarLeft, hotbarTop, cfg);
    }

    @Unique
    private void hotbarcyclerrr$drawRowIndicator(DrawContext ctx,
                                               int hotbarLeft, int hotbarTop,
                                               HotbarCyclerConfig cfg) {
        int sqW    = Math.max(4, cfg.rowIndicatorSquareW);
        int sqH    = Math.max(4, cfg.rowIndicatorSquareH);
        int stackH = IND_ROWS * sqH + (IND_ROWS - 1) * IND_GAP;
        int top    = hotbarTop + (HOTBAR_HEIGHT - stackH) / 2 + cfg.rowIndicatorOffsetY;
        int sqR    = hotbarLeft - IND_MARGIN + cfg.rowIndicatorOffsetX;
        int sqL    = sqR - sqW;
        int active = HotbarCyclerClient.getCurrentPage();
        com.example.hotbarcyclerrr.RowIndicatorStyle style = (cfg != null && cfg.rowIndicatorStyle != null)
                ? cfg.rowIndicatorStyle : com.example.hotbarcyclerrr.RowIndicatorStyle.DEFAULT;

        int[] rowOrder = { 1, 2, 3, 0 };
        for (int i = 0; i < IND_ROWS; i++) {
            int rowId = rowOrder[i];
            int t     = top + i * (sqH + IND_GAP);
            HotbarCyclerUtil.drawIndicatorCell(ctx, sqL, t, sqW, sqH, rowId == active, rowId, style);
        }
    }
}
