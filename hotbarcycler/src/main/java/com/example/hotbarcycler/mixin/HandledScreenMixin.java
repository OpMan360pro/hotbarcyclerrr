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

import java.util.HashMap;
import java.util.Map;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin<T extends ScreenHandler> extends Screen {

    @Shadow @Final protected T handler;

    @Unique private int hotbarcycler$appliedPage = -1;
    @Unique private final Map<Slot, Integer> hotbarcycler$originalY = new HashMap<>();

    protected HandledScreenMixin() {
        super(null);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void hotbarcycler$onInit(CallbackInfo ci) {
        hotbarcycler$restoreSlotPositions();
        hotbarcycler$applySlotPositions();
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void hotbarcycler$onRemoved(CallbackInfo ci) {
        hotbarcycler$restoreSlotPositions();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void hotbarcycler$onRenderHead(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        int currentPage = HotbarCyclerClient.getCurrentPage();
        if (hotbarcycler$appliedPage != currentPage) {
            hotbarcycler$restoreSlotPositions();
            hotbarcycler$applySlotPositions();
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void hotbarcycler$onRenderTail(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        hotbarcycler$drawActiveRowHighlightAndIndicator(context);
    }

    @Unique
    private void hotbarcycler$applySlotPositions() {
        int page = HotbarCyclerClient.getCurrentPage();

        Slot[] hotbarSlots = new Slot[9];
        Slot[] row1Slots   = new Slot[9];
        Slot[] row2Slots   = new Slot[9];
        Slot[] row3Slots   = new Slot[9];

        for (Slot slot : this.handler.slots) {
            if (slot.inventory instanceof PlayerInventory) {
                int idx = slot.getIndex();
                if (idx >= 0 && idx < 9)        hotbarSlots[idx] = slot;
                else if (idx >= 9 && idx < 18)   row1Slots[idx - 9] = slot;
                else if (idx >= 18 && idx < 27)  row2Slots[idx - 18] = slot;
                else if (idx >= 27 && idx < 36)  row3Slots[idx - 27] = slot;
            }
        }

        if (hotbarSlots[0] == null) return;

        // 1. Remove 4px vertical gap between row 3 and bottom hotbar row
        for (int i = 0; i < 9; i++) {
            Slot s = hotbarSlots[i];
            if (s != null) {
                hotbarcycler$originalY.putIfAbsent(s, s.y);
                ((SlotAccessor) s).hotbarcycler$setY(s.y - 4);
            }
        }

        // 2. If page != 0, swap Y with target row so inventory layout remains static
        Slot[] targetSlots = switch (page) {
            case 1 -> row1Slots;
            case 2 -> row2Slots;
            case 3 -> row3Slots;
            default -> null;
        };

        if (targetSlots != null) {
            for (int i = 0; i < 9; i++) {
                Slot s0 = hotbarSlots[i];
                Slot sT = targetSlots[i];
                if (s0 != null && sT != null) {
                    hotbarcycler$originalY.putIfAbsent(sT, sT.y);
                    int tempY = s0.y;
                    ((SlotAccessor) s0).hotbarcycler$setY(sT.y);
                    ((SlotAccessor) sT).hotbarcycler$setY(tempY);
                }
            }
        }

        hotbarcycler$appliedPage = page;
    }

    @Unique
    private void hotbarcycler$restoreSlotPositions() {
        for (Map.Entry<Slot, Integer> entry : hotbarcycler$originalY.entrySet()) {
            ((SlotAccessor) entry.getKey()).hotbarcycler$setY(entry.getValue());
        }
        hotbarcycler$originalY.clear();
        hotbarcycler$appliedPage = -1;
    }

    @Unique
    private void hotbarcycler$drawActiveRowHighlightAndIndicator(DrawContext context) {
        int page = HotbarCyclerClient.getCurrentPage();

        Slot[] hotbarSlots = new Slot[9];
        Slot[] row1Slots   = new Slot[9];
        Slot[] row2Slots   = new Slot[9];
        Slot[] row3Slots   = new Slot[9];

        for (Slot slot : this.handler.slots) {
            if (slot.inventory instanceof PlayerInventory) {
                int idx = slot.getIndex();
                if (idx >= 0 && idx < 9)        hotbarSlots[idx] = slot;
                else if (idx >= 9 && idx < 18)   row1Slots[idx - 9] = slot;
                else if (idx >= 18 && idx < 27)  row2Slots[idx - 18] = slot;
                else if (idx >= 27 && idx < 36)  row3Slots[idx - 27] = slot;
            }
        }

        if (hotbarSlots[0] == null || hotbarSlots[8] == null) return;

        int activeMinX = hotbarSlots[0].x;
        int activeMaxX = hotbarSlots[8].x;
        int activeY    = hotbarSlots[0].y;

        int boxX = activeMinX - 1;
        int boxY = activeY - 1;
        int boxW = (activeMaxX - activeMinX) + 18;
        int boxH = 18;

        // Highlight frame around active row
        context.fill(boxX,             boxY,             boxX + boxW,     boxY + 1,        0xCCFFFFFF);
        context.fill(boxX,             boxY + boxH - 1,  boxX + boxW,     boxY + boxH,     0xCCFFFFFF);
        context.fill(boxX,             boxY,             boxX + 1,        boxY + boxH,     0xCCFFFFFF);
        context.fill(boxX + boxW - 1,  boxY,             boxX + boxW,     boxY + boxH,     0xCCFFFFFF);

        // Row indicator cells to the left of inventory
        int indW = 7;
        int indH = 7;
        int indX = activeMinX - indW - 3;

        int y1 = (page == 1) ? hotbarSlots[0].y : (row1Slots[0] != null ? row1Slots[0].y : -1);
        int y2 = (page == 2) ? hotbarSlots[0].y : (row2Slots[0] != null ? row2Slots[0].y : -1);
        int y3 = (page == 3) ? hotbarSlots[0].y : (row3Slots[0] != null ? row3Slots[0].y : -1);
        int y0 = (page == 0) ? hotbarSlots[0].y : (row1Slots[0] != null && page == 1 ? row1Slots[0].y : (row2Slots[0] != null && page == 2 ? row2Slots[0].y : (row3Slots[0] != null && page == 3 ? row3Slots[0].y : -1)));

        if (y1 != -1) HotbarCyclerUtil.drawIndicatorCell(context, indX, y1 + 5, indW, indH, page == 1);
        if (y2 != -1) HotbarCyclerUtil.drawIndicatorCell(context, indX, y2 + 5, indW, indH, page == 2);
        if (y3 != -1) HotbarCyclerUtil.drawIndicatorCell(context, indX, y3 + 5, indW, indH, page == 3);
        if (y0 != -1) HotbarCyclerUtil.drawIndicatorCell(context, indX, y0 + 5, indW, indH, page == 0);
    }
}
