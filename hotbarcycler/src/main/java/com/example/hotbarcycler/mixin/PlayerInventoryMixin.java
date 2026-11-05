package com.example.hotbarcycler.mixin;

import com.example.hotbarcycler.HotbarCyclerClient;
import com.example.hotbarcycler.HotbarCyclerConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryMixin {

    @Shadow @Final private DefaultedList<ItemStack> main;
    @Shadow private int selectedSlot;

    @Inject(method = "getSelectedStack", at = @At("HEAD"), cancellable = true)
    private void hotbarcycler$getVisualSelectedStack(CallbackInfoReturnable<ItemStack> cir) {
        HotbarCyclerConfig cfg = HotbarCyclerConfig.get();
        if (cfg != null && cfg.seamlessHotbar) {
            int page = HotbarCyclerClient.getCurrentPage();
            if (page > 0 && page < 4) {
                if (!HotbarCyclerClient.isRowSwapped()) {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client != null && client.player != null && (Object) this == client.player.getInventory()) {
                        int idx = page * 9 + this.selectedSlot;
                        if (idx >= 0 && idx < this.main.size()) {
                            cir.setReturnValue(this.main.get(idx));
                        }
                    }
                }
            }
        }
    }
}
