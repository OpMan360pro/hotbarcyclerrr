package com.example.hotbarcyclerrr;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HotbarCyclerMod implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("hotbarcyclerrr");
    private static final Map<UUID, Integer> activeRows = new ConcurrentHashMap<>();

    @Override
    public void onInitialize() {
        // --- Hotbar row cycling ---
        PayloadTypeRegistry.playC2S().register(CycleHotbarPayload.ID, CycleHotbarPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(CycleHotbarPayload.ID, (payload, context) ->
                context.server().execute(() -> switchHotbarRow(context.player(), payload.targetRow())));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            if (player != null) {
                int current = activeRows.getOrDefault(player.getUuid(), 0);
                if (current != 0) {
                    swapRowWithHotbar(player, current);
                    activeRows.remove(player.getUuid());
                }
            }
        });

        LOGGER.info("Hotbar Cyclerrr initialised.");
    }

    // =========================================================================
    // Hotbar row switching (swaps only target row with hotbar 0-8)
    // =========================================================================

    public static void switchHotbarRow(ServerPlayerEntity player, int targetRow) {
        if (targetRow < 0 || targetRow > 3) return;
        int current = activeRows.getOrDefault(player.getUuid(), 0);
        if (current == targetRow) return;

        if (current != 0) {
            swapRowWithHotbar(player, current);
        }
        if (targetRow != 0) {
            swapRowWithHotbar(player, targetRow);
        }
        activeRows.put(player.getUuid(), targetRow);
        sync(player);
    }

    private static void swapRowWithHotbar(ServerPlayerEntity player, int page) {
        if (page < 1 || page > 3) return;
        int targetStart = page * 9;
        PlayerInventory inv = player.getInventory();
        List<ItemStack> stacks = inv.getMainStacks();
        for (int i = 0; i < 9; i++) {
            ItemStack temp = stacks.get(i);
            stacks.set(i, stacks.get(targetStart + i));
            stacks.set(targetStart + i, temp);
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static void sync(ServerPlayerEntity player) {
        player.getInventory().markDirty();
        player.playerScreenHandler.syncState();
    }
}
