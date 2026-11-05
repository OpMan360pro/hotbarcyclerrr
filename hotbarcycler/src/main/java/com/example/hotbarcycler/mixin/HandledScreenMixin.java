package com.example.hotbarcycler.mixin;

import com.example.hotbarcycler.HotbarCyclerClient;
import com.example.hotbarcycler.HotbarCyclerUtil;
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

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin<T extends ScreenHandler> extends Screen {

    @Shadow @Final protected T handler;
    @Shadow protected int x;
    @Shadow protected int y;
    @Shadow protected int backgroundWidth;
    @Shadow protected int backgroundHeight;

    @Unique private final Map<Slot, Integer> hotbarcycler$originalY = new HashMap<>();
    @Unique private final Set<Slot> hotbarcycler$shiftedSlots = new HashSet<>();
    @Unique private static Field hotbarcycler$creativeSlotField = null;

    protected HandledScreenMixin() {
        super(null);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void hotbarcycler$onInit(CallbackInfo ci) {
        hotbarcycler$restoreSlotPositions();
        hotbarcycler$applySlotShift();
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void hotbarcycler$onRemoved(CallbackInfo ci) {
        hotbarcycler$restoreSlotPositions();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void hotbarcycler$onRenderHead(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        hotbarcycler$applySlotShift();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void hotbarcycler$onRenderTail(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        hotbarcycler$drawActiveRowHighlightAndIndicator(context, mouseX, mouseY);
    }

    @Unique
    private void hotbarcycler$applySlotShift() {
        hotbarcycler$shiftedSlots.retainAll(this.handler.slots);
        for (Slot slot : this.handler.slots) {
            if (hotbarcycler$isHotbarSlot(slot) && !hotbarcycler$shiftedSlots.contains(slot)) {
                hotbarcycler$originalY.put(slot, slot.y);
                ((SlotAccessor) slot).hotbarcycler$setY(slot.y - 4);
                hotbarcycler$shiftedSlots.add(slot);
            }
        }
    }

    @Unique
    private void hotbarcycler$restoreSlotPositions() {
        for (Map.Entry<Slot, Integer> entry : hotbarcycler$originalY.entrySet()) {
            ((SlotAccessor) entry.getKey()).hotbarcycler$setY(entry.getValue());
        }
        hotbarcycler$originalY.clear();
        hotbarcycler$shiftedSlots.clear();
    }

    @Unique
    private void hotbarcycler$drawActiveRowHighlightAndIndicator(DrawContext context, int mouseX, int mouseY) {
        int page = HotbarCyclerClient.getCurrentPage();

        Slot[] activeRowSlots = new Slot[9];
        Slot[] rowAnchors = new Slot[4];
        Slot[] hotbarSlots = new Slot[9];

        for (Slot slot : this.handler.slots) {
            if (!slot.isEnabled() || slot.x < -100 || slot.y < -100) continue;

            int row = hotbarcycler$getSlotRow(slot);
            int col = hotbarcycler$getSlotColumn(slot);
            if (row == page && col >= 0 && col < 9) {
                activeRowSlots[col] = slot;
            }
            if (row == 0 && col >= 0 && col < 9) {
                hotbarSlots[col] = slot;
            }
            if (row >= 0 && row < 4 && rowAnchors[row] == null) {
                rowAnchors[row] = slot;
            }
        }

        // Seamless hotbar attachment: cover old slot gap and draw clean shifted slot boxes
        if (hotbarSlots[0] != null && hotbarSlots[8] != null) {
            int hStartX = this.x + hotbarSlots[0].x - 1;
            int hEndX   = this.x + hotbarSlots[8].x + 17;
            int hY      = this.y + hotbarSlots[0].y;

            // 1. Cover the old gap below shifted hotbar so container background is seamless
            int gapTop    = hY + 17;
            int gapBottom = gapTop + 5;
            context.fill(hStartX, gapTop, hEndX, gapBottom, 0xFFC6C6C6);
            context.fill(hStartX, gapBottom, hEndX, gapBottom + 1, 0xFF373737);

            // 2. Render clean slot boxes for shifted hotbar slots
            for (int col = 0; col < 9; col++) {
                Slot slot = hotbarSlots[col];
                if (slot == null) continue;
                int sx = this.x + slot.x;
                int sy = this.y + slot.y;

                // Slot background
                context.fill(sx, sy, sx + 16, sy + 16, 0xFF8B8B8B);
                // Borders
                context.fill(sx - 1, sy - 1, sx + 17, sy, 0xFF373737);
                context.fill(sx - 1, sy, sx, sy + 17, 0xFF373737);
                context.fill(sx + 16, sy, sx + 17, sy + 16, 0xFFFFFFFF);
                context.fill(sx, sy + 16, sx + 17, sy + 17, 0xFFFFFFFF);

                // Slot stack
                var stack = slot.getStack();
                if (!stack.isEmpty()) {
                    context.drawItem(stack, sx, sy);
                    context.drawStackOverlay(this.textRenderer, stack, sx, sy);
                }

                // Hover highlight
                if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) {
                    context.fill(sx, sy, sx + 16, sy + 16, 0x80FFFFFF);
                }
            }
        }

        Slot anchor = rowAnchors[0] != null ? rowAnchors[0] : rowAnchors[1];
        if (anchor == null) return;

        // Draw active row highlight frame around the 9 slots
        if (activeRowSlots[0] != null && activeRowSlots[8] != null) {
            int minX = this.x + activeRowSlots[0].x;
            int maxX = this.x + activeRowSlots[8].x;
            int y    = this.y + activeRowSlots[0].y;

            int boxX = minX - 1;
            int boxY = y - 1;
            int boxW = (maxX - minX) + 18;
            int boxH = 18;

            context.fill(boxX,             boxY,             boxX + boxW,     boxY + 1,        0xFFFFFFFF);
            context.fill(boxX,             boxY + boxH - 1,  boxX + boxW,     boxY + boxH,     0xFFFFFFFF);
            context.fill(boxX,             boxY,             boxX + 1,        boxY + boxH,     0xFFFFFFFF);
            context.fill(boxX + boxW - 1,  boxY,             boxX + boxW,     boxY + boxH,     0xFFFFFFFF);
            context.fill(boxX + 1,         boxY + 1,         boxX + boxW - 1, boxY + boxH - 1, 0x1AFFFFFF);
        }

        // Draw 4 indicator cells to the left of the player inventory
        int indW = 7;
        int indH = 7;
        int indX = this.x + anchor.x - indW - 3;

        int[] rowOrder = { 1, 2, 3, 0 };
        for (int r : rowOrder) {
            Slot s = rowAnchors[r];
            if (s != null) {
                int indY = this.y + s.y + (18 - indH) / 2;
                HotbarCyclerUtil.drawIndicatorCell(context, indX, indY, indW, indH, r == page);
            }
        }
    }

    @Unique
    private static Slot hotbarcycler$getRealSlot(Slot slot) {
        if (slot.inventory instanceof PlayerInventory) return slot;
        if (slot.getClass().getName().contains("CreativeSlot")) {
            try {
                if (hotbarcycler$creativeSlotField == null) {
                    for (Field f : slot.getClass().getDeclaredFields()) {
                        if (Slot.class.isAssignableFrom(f.getType())) {
                            hotbarcycler$creativeSlotField = f;
                            hotbarcycler$creativeSlotField.setAccessible(true);
                            break;
                        }
                    }
                }
                if (hotbarcycler$creativeSlotField != null) {
                    Object wrapped = hotbarcycler$creativeSlotField.get(slot);
                    if (wrapped instanceof Slot realSlot) return realSlot;
                }
            } catch (Exception ignored) {}
        }
        return slot;
    }

    @Unique
    private static boolean hotbarcycler$isHotbarSlot(Slot slot) {
        return hotbarcycler$getSlotRow(slot) == 0;
    }

    @Unique
    private static int hotbarcycler$getSlotRow(Slot slot) {
        Slot real = hotbarcycler$getRealSlot(slot);
        if (!(real.inventory instanceof PlayerInventory)) return -1;
        int idx = real.getIndex();
        if (idx >= 0 && idx < 9) return 0; // hotbar
        if (idx >= 9 && idx < 18) return 1;
        if (idx >= 18 && idx < 27) return 2;
        if (idx >= 27 && idx < 36) return 3;
        return -1;
    }

    @Unique
    private static int hotbarcycler$getSlotColumn(Slot slot) {
        Slot real = hotbarcycler$getRealSlot(slot);
        if (!(real.inventory instanceof PlayerInventory)) return -1;
        int idx = real.getIndex();
        if (idx >= 0 && idx < 9) return idx;
        if (idx >= 9 && idx < 36) return (idx - 9) % 9;
        return -1;
    }
}
