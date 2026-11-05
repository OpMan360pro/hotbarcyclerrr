package com.example.hotbarcycler.mixin;

import com.example.hotbarcycler.HotbarCyclerClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin<T extends ScreenHandler> extends Screen {

    @Shadow @Final protected T handler;
    @Shadow protected int x;
    @Shadow protected int y;

    protected HandledScreenMixin() {
        super(null);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void hotbarcycler$onRenderTail(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        hotbarcycler$drawActiveRowHighlight(context);
    }

    @Unique
    private void hotbarcycler$drawActiveRowHighlight(DrawContext context) {
        int page = HotbarCyclerClient.getCurrentPage();

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        boolean found = false;

        for (Slot slot : this.handler.slots) {
            if (!slot.isEnabled() || slot.x < -100 || slot.y < -100) continue;
            int row = hotbarcycler$getSlotRow(slot);
            if (row == page) {
                int sx = this.x + slot.x;
                int sy = this.y + slot.y;
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

    @Unique
    private static int hotbarcycler$getSlotRow(Slot slot) {
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
}
