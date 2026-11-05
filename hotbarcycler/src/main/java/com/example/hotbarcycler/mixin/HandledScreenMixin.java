package com.example.hotbarcycler.mixin;

import com.example.hotbarcycler.HotbarCyclerUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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
        if (!((Object)this instanceof InventoryScreen)) {
            HotbarCyclerUtil.drawActiveRowHighlight(this.handler, context, this.x, this.y);
        }
    }
}
