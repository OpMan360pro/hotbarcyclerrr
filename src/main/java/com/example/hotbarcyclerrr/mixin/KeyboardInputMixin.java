package com.example.hotbarcyclerrr.mixin;

import com.example.hotbarcyclerrr.HotbarCyclerClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {

    @Inject(method = "tick", at = @At("TAIL"))
    private void hotbarcyclerrr$suppressInputWhenFrozen(CallbackInfo ci) {
        if (HotbarCyclerClient.isMovementFrozen()) {
            this.playerInput = PlayerInput.DEFAULT;
            this.movementVector = Vec2f.ZERO;
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                com.example.hotbarcyclerrr.HotbarCyclerConfig cfg = com.example.hotbarcyclerrr.HotbarCyclerConfig.get();
                if (cfg != null && cfg.getEffectivePauseSprint()) {
                    client.player.setSprinting(false);
                }
                var vel = client.player.getVelocity();
                client.player.setVelocity(0.0, Math.min(0.0, vel.y), 0.0);
            }
        }
    }
}
