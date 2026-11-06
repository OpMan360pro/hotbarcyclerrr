package com.example.hotbarcyclerrr;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HotbarCyclerMod implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("hotbarcyclerrr");

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(CycleHotbarPayload.ID, CycleHotbarPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(CycleHotbarPayload.ID, (payload, context) -> {
            // Handled natively via standard vanilla ClickSlotC2SPacket
        });

        LOGGER.info("Hotbar Cyclerrr initialised.");
    }
}
