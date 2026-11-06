package com.example.hotbarcyclerrr;

import com.example.hotbarcyclerrr.screen.HotbarCyclerConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class HotbarCyclerClient implements ClientModInitializer {

    private static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of("hotbarcyclerrr", "hotbarcyclerrr"));

    // ── Keybindings ───────────────────────────────────────────────────────────
    private static KeyBinding nextHotbarKey;
    private static KeyBinding prevHotbarKey;
    private static KeyBinding returnFixedKey;
    private static KeyBinding openSettingsKey;
    private static KeyBinding toggleRowIndicatorKey;

    // ── Runtime state ─────────────────────────────────────────────────────────
    private static int currentPage = 0;
    private static boolean isRowSwapped = false;
    private static boolean rowIndicatorVisible = true;
    private static boolean freezeEnabled = true;
    private static int freezeTicks = 0;
    private static boolean wasHandledScreen = false;

    public record SwapStep(int slotId, int button) {}

    private static class ClientTransition {
        final List<SwapStep> steps;
        final int targetPage;
        final int delayTicks;
        int currentStep = 0;
        int ticksRemaining = 0;

        ClientTransition(List<SwapStep> steps, int targetPage, int delayTicks) {
            this.steps = steps;
            this.targetPage = targetPage;
            this.delayTicks = Math.max(0, delayTicks);
            this.ticksRemaining = 0;
        }
    }

    private static ClientTransition activeTransition = null;

    public static boolean isMovementFrozen() {
        if (!freezeEnabled) return false;
        return activeTransition != null || freezeTicks > 0;
    }

    public static boolean isTransitioning() {
        return activeTransition != null;
    }

    public static int     getCurrentPage()             { return currentPage; }
    public static boolean isRowSwapped()               { return isRowSwapped; }
    public static boolean isRowIndicatorVisible()      { return rowIndicatorVisible; }

    public static KeyBinding getNextHotbarKey() { return nextHotbarKey; }
    public static KeyBinding getPrevHotbarKey() { return prevHotbarKey; }
    public static KeyBinding getReturnFixedKey() { return returnFixedKey; }

    private static int getContainerSlotId(MinecraftClient client, int playerInvIndex) {
        if (client != null && client.player != null && client.player.currentScreenHandler != null) {
            for (Slot slot : client.player.currentScreenHandler.slots) {
                if (slot.inventory == client.player.getInventory() && slot.getIndex() == playerInvIndex) {
                    return slot.id;
                }
            }
        }
        return (client != null && client.currentScreen == null) ? playerInvIndex : -1;
    }

    private static List<SwapStep> buildSwapSteps(MinecraftClient client, int fromPage, int toPage) {
        List<SwapStep> steps = new ArrayList<>();
        if (fromPage >= 1 && fromPage <= 3) {
            for (int col = 0; col < 9; col++) {
                int slotId = getContainerSlotId(client, fromPage * 9 + col);
                if (slotId >= 0) {
                    steps.add(new SwapStep(slotId, col));
                }
            }
        }
        if (toPage >= 1 && toPage <= 3) {
            for (int col = 0; col < 9; col++) {
                int slotId = getContainerSlotId(client, toPage * 9 + col);
                if (slotId >= 0) {
                    steps.add(new SwapStep(slotId, col));
                }
            }
        }
        return steps;
    }

    private static void executeStep(MinecraftClient client, SwapStep step) {
        if (client == null || client.interactionManager == null || client.player == null) return;
        int syncId = client.player.currentScreenHandler != null ? client.player.currentScreenHandler.syncId : 0;
        client.interactionManager.clickSlot(syncId, step.slotId(), step.button(), SlotActionType.SWAP, client.player);
    }

    private static void sendStopSprint(ClientPlayerEntity player) {
        if (player != null && player.networkHandler != null) {
            player.setSprinting(false);
            player.networkHandler.sendPacket(new ClientCommandC2SPacket(
                    player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
        }
    }

    private static void sendCloseInventoryPacket(ClientPlayerEntity player) {
        if (player != null && player.networkHandler != null) {
            player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(0));
        }
    }

    private static void startClientTransition(MinecraftClient client, List<SwapStep> steps, int targetPage, int delayTicks, boolean pauseSprint, boolean freeze) {
        if (client.player == null || steps.isEmpty()) return;
        if (pauseSprint) {
            sendStopSprint(client.player);
        }
        if (freeze) {
            var vel = client.player.getVelocity();
            client.player.setVelocity(0.0, Math.min(0.0, vel.y), 0.0);
        }
        activeTransition = new ClientTransition(steps, targetPage, delayTicks);
        executeStep(client, steps.get(0));
        activeTransition.currentStep = 1;
        activeTransition.ticksRemaining = delayTicks;
    }

    private static void tickClientTransition(MinecraftClient client) {
        if (activeTransition == null || client.player == null) return;

        if (activeTransition.ticksRemaining > 0) {
            activeTransition.ticksRemaining--;
            return;
        }

        if (activeTransition.currentStep < activeTransition.steps.size()) {
            executeStep(client, activeTransition.steps.get(activeTransition.currentStep));
            activeTransition.currentStep++;
            activeTransition.ticksRemaining = activeTransition.delayTicks;
        }

        if (activeTransition.currentStep >= activeTransition.steps.size()) {
            currentPage = activeTransition.targetPage;
            isRowSwapped = (currentPage != 0);
            if (client.currentScreen == null) {
                sendCloseInventoryPacket(client.player);
            }
            activeTransition = null;
            if (freezeEnabled) {
                freezeTicks = 1;
            }
        }
    }

    public static void finishClientTransitionIfRunning(MinecraftClient client) {
        if (activeTransition != null && client != null && client.player != null) {
            while (activeTransition.currentStep < activeTransition.steps.size()) {
                executeStep(client, activeTransition.steps.get(activeTransition.currentStep));
                activeTransition.currentStep++;
            }
            currentPage = activeTransition.targetPage;
            isRowSwapped = (currentPage != 0);
            if (client.currentScreen == null) {
                sendCloseInventoryPacket(client.player);
            }
            activeTransition = null;
            freezeTicks = 0;
        }
    }

    public static void cycleHotbar(boolean next) {
        if (isTransitioning()) return;
        int target = next ? (currentPage + 1) % 4 : (currentPage - 1 + 4) % 4;
        setHotbarRow(target);
    }

    public static void setHotbarRow(int targetRow) {
        if (isTransitioning()) return;
        if (targetRow < 0 || targetRow > 3 || targetRow == currentPage) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        if (client.currentScreen != null && !(client.currentScreen instanceof HandledScreen)) return;

        HotbarCyclerConfig cfg = HotbarCyclerConfig.get();
        int delay = cfg.getEffectiveSlotDelay();
        boolean pauseSprint = cfg.getEffectivePauseSprint();
        freezeEnabled = cfg.getEffectiveFreezeMovement();

        int oldPage = currentPage;
        List<SwapStep> steps = buildSwapSteps(client, oldPage, targetRow);
        if (steps.isEmpty()) return;

        if (delay <= 0) {
            if (pauseSprint) sendStopSprint(client.player);
            if (freezeEnabled && client.currentScreen == null) {
                var vel = client.player.getVelocity();
                client.player.setVelocity(0.0, Math.min(0.0, vel.y), 0.0);
                freezeTicks = 1;
            }
            for (SwapStep step : steps) {
                executeStep(client, step);
            }
            currentPage = targetRow;
            isRowSwapped = (currentPage != 0);
            if (client.currentScreen == null) {
                sendCloseInventoryPacket(client.player);
            }
        } else {
            startClientTransition(client, steps, targetRow, delay, pauseSprint, freezeEnabled && client.currentScreen == null);
        }
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    @Override
    public void onInitializeClient() {
        HotbarCyclerConfig.get();

        nextHotbarKey         = reg("key.hotbarcyclerrr.next");
        prevHotbarKey         = reg("key.hotbarcyclerrr.prev");
        returnFixedKey        = reg("key.hotbarcyclerrr.return_fixed");
        openSettingsKey       = reg("key.hotbarcyclerrr.open_settings");
        toggleRowIndicatorKey = reg("key.hotbarcyclerrr.toggle_row_indicator");

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            currentPage         = 0;
            isRowSwapped        = false;
            activeTransition    = null;
            freezeTicks         = 0;
            wasHandledScreen    = false;
            rowIndicatorVisible = HotbarCyclerConfig.get().showRowIndicator;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            if (freezeTicks > 0) {
                freezeTicks--;
            }

            tickClientTransition(client);

            boolean inHandledScreen = (client.currentScreen instanceof HandledScreen);
            HotbarCyclerConfig cfg = HotbarCyclerConfig.get();

            if (inHandledScreen) {
                if (!wasHandledScreen) {
                    wasHandledScreen = true;
                    if (isTransitioning()) {
                        finishClientTransitionIfRunning(client);
                    }
                    if (cfg.revertToFixedSlots && currentPage != 0) {
                        setHotbarRow(0);
                    }
                }
            } else {
                if (wasHandledScreen) {
                    wasHandledScreen = false;
                    finishClientTransitionIfRunning(client);
                }
            }

            boolean inScreen = (client.currentScreen != null);
            if (!inScreen) {
                while (nextHotbarKey.wasPressed()) {
                    cycleHotbar(true);
                }
                while (prevHotbarKey.wasPressed()) {
                    cycleHotbar(false);
                }
                while (returnFixedKey.wasPressed()) {
                    if (currentPage != 0) {
                        setHotbarRow(0);
                    }
                }
            }

            while (openSettingsKey.wasPressed()) {
                client.setScreen(new HotbarCyclerConfigScreen(client.currentScreen));
            }
            while (toggleRowIndicatorKey.wasPressed()) {
                rowIndicatorVisible = !rowIndicatorVisible;
            }
        });
    }

    private static KeyBinding reg(String id) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(
                id, InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), CATEGORY));
    }
}
