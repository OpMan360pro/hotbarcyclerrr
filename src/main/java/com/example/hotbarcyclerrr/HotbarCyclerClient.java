package com.example.hotbarcyclerrr;

import com.example.hotbarcyclerrr.screen.HotbarCyclerConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class HotbarCyclerClient implements ClientModInitializer {

    private static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of("hotbarcyclerrr", "hotbarcyclerrr"));

    private static final ScheduledExecutorService SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "HotbarCycler-Worker");
                t.setDaemon(true);
                return t;
            });

    // ── Keybindings ───────────────────────────────────────────────────────────
    private static KeyBinding nextHotbarKey;
    private static KeyBinding prevHotbarKey;
    private static KeyBinding returnFixedKey;
    private static KeyBinding openSettingsKey;
    private static KeyBinding toggleRowIndicatorKey;

    // ── Runtime state ─────────────────────────────────────────────────────────
    private static int currentPage = 0;
    private static boolean isRowSwapped = false;
    private static boolean blockInputEnabled = true;
    private static boolean freezeMovementEnabled = true;
    private static int freezeTicks = 0;
    private static boolean wasHandledScreen = false;
    private static boolean wasSprinting = false;

    public record SlotTransfer(int col, int fromInvIndex, int toInvIndex) {}

    private static class ClientTransition {
        final long id;
        final List<SlotTransfer> transfers;
        final int targetPage;
        final int delayMs;
        int currentStep = 0;

        ClientTransition(long id, List<SlotTransfer> transfers, int targetPage, int delayMs) {
            this.id = id;
            this.transfers = transfers;
            this.targetPage = targetPage;
            this.delayMs = Math.max(0, delayMs);
            this.currentStep = 0;
        }
    }

    private static final AtomicLong TRANSITION_COUNTER = new AtomicLong(0);
    private static ClientTransition activeTransition = null;

    public static boolean isMovementFrozen() {
        if (!freezeMovementEnabled) return false;
        return activeTransition != null || freezeTicks > 0;
    }

    public static boolean isInputBlocked() {
        if (!blockInputEnabled) return false;
        return activeTransition != null || freezeTicks > 0;
    }

    public static boolean isTransitioning() {
        return activeTransition != null;
    }

    public static int     getCurrentPage()             { return currentPage; }
    public static boolean isRowSwapped()               { return isRowSwapped; }
    public static boolean isRowIndicatorVisible()      { return HotbarCyclerConfig.get().showRowIndicator; }

    public static void setRowIndicatorVisible(boolean visible) {
        HotbarCyclerConfig cfg = HotbarCyclerConfig.get();
        if (cfg != null) {
            cfg.showRowIndicator = visible;
            cfg.save();
        }
    }

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

    private static List<SlotTransfer> buildTransfers(int fromPage, int toPage) {
        List<SlotTransfer> transfers = new ArrayList<>(9);
        for (int col = 0; col < 9; col++) {
            int fromIdx = (fromPage >= 1 && fromPage <= 3) ? (fromPage * 9 + col) : -1;
            int toIdx   = (toPage >= 1 && toPage <= 3)   ? (toPage * 9 + col)   : -1;
            if (fromIdx >= 0 || toIdx >= 0) {
                transfers.add(new SlotTransfer(col, fromIdx, toIdx));
            }
        }
        return transfers;
    }

    private static void executeTransfer(MinecraftClient client, SlotTransfer transfer) {
        if (client == null || client.interactionManager == null || client.player == null) return;

        boolean isCreative = (client.currentScreen instanceof CreativeInventoryScreen)
                || (client.player.currentScreenHandler instanceof CreativeInventoryScreen.CreativeScreenHandler);
        if (isCreative) {
            executeCreativeTransfer(client, transfer);
            return;
        }

        int syncId = client.player.currentScreenHandler != null ? client.player.currentScreenHandler.syncId : 0;
        if (transfer.fromInvIndex() >= 0) {
            int slotId = getContainerSlotId(client, transfer.fromInvIndex());
            if (slotId >= 0) {
                client.interactionManager.clickSlot(syncId, slotId, transfer.col(), SlotActionType.SWAP, client.player);
            }
        }
        if (transfer.toInvIndex() >= 0) {
            int slotId = getContainerSlotId(client, transfer.toInvIndex());
            if (slotId >= 0) {
                client.interactionManager.clickSlot(syncId, slotId, transfer.col(), SlotActionType.SWAP, client.player);
            }
        }
    }

    private static void executeCreativeTransfer(MinecraftClient client, SlotTransfer transfer) {
        if (client == null || client.interactionManager == null || client.player == null) return;
        PlayerInventory inv = client.player.getInventory();
        int col = transfer.col();

        if (transfer.fromInvIndex() >= 0) {
            int fromIdx = transfer.fromInvIndex();
            ItemStack hotbarStack = inv.getStack(col);
            ItemStack fromStack = inv.getStack(fromIdx);
            inv.setStack(col, fromStack);
            inv.setStack(fromIdx, hotbarStack);
            client.interactionManager.clickCreativeStack(fromStack, 36 + col);
            client.interactionManager.clickCreativeStack(hotbarStack, fromIdx);
        }
        if (transfer.toInvIndex() >= 0) {
            int toIdx = transfer.toInvIndex();
            ItemStack hotbarStack = inv.getStack(col);
            ItemStack toStack = inv.getStack(toIdx);
            inv.setStack(col, toStack);
            inv.setStack(toIdx, hotbarStack);
            client.interactionManager.clickCreativeStack(toStack, 36 + col);
            client.interactionManager.clickCreativeStack(hotbarStack, toIdx);
        }

        if (client.player.currentScreenHandler != null) {
            for (Slot slot : client.player.currentScreenHandler.slots) {
                if (slot.inventory == inv) {
                    int idx = slot.getIndex();
                    if (idx == col || idx == transfer.fromInvIndex() || idx == transfer.toInvIndex()) {
                        slot.markDirty();
                    }
                }
            }
        }
    }

    private static void sendStopSprint(ClientPlayerEntity player) {
        if (player != null && player.networkHandler != null) {
            player.setSprinting(false);
            player.networkHandler.sendPacket(new ClientCommandC2SPacket(
                    player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
        }
    }

    private static void sendStartSprint(ClientPlayerEntity player) {
        if (player != null && player.networkHandler != null) {
            player.setSprinting(true);
            player.networkHandler.sendPacket(new ClientCommandC2SPacket(
                    player, ClientCommandC2SPacket.Mode.START_SPRINTING));
        }
    }

    private static void sendCloseInventoryPacket(ClientPlayerEntity player) {
        if (player != null && player.networkHandler != null) {
            player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(0));
        }
    }

    private static void checkResumeSprint(MinecraftClient client) {
        if (wasSprinting && !isInputBlocked() && !isMovementFrozen()) {
            wasSprinting = false;
            if (client != null && client.player != null) {
                sendStartSprint(client.player);
            }
        }
    }

    private static void startClientTransition(MinecraftClient client, List<SlotTransfer> transfers, int targetPage, int delayMs, boolean pauseSprint, boolean freeze) {
        if (client.player == null || transfers.isEmpty()) return;
        if (pauseSprint) {
            sendStopSprint(client.player);
        }
        if (freeze) {
            var vel = client.player.getVelocity();
            client.player.setVelocity(0.0, Math.min(0.0, vel.y), 0.0);
        }

        long id = TRANSITION_COUNTER.incrementAndGet();
        activeTransition = new ClientTransition(id, transfers, targetPage, delayMs);

        executeTransfer(client, transfers.get(0));
        activeTransition.currentStep = 1;

        if (activeTransition.currentStep < transfers.size()) {
            scheduleNextStep(client, id);
        } else {
            completeTransition(client);
        }
    }

    private static void scheduleNextStep(MinecraftClient client, long id) {
        if (activeTransition == null || activeTransition.id != id) return;
        int delay = activeTransition.delayMs;
        SCHEDULER.schedule(() -> client.execute(() -> {
            if (activeTransition != null && activeTransition.id == id && client.player != null) {
                if (activeTransition.currentStep < activeTransition.transfers.size()) {
                    executeTransfer(client, activeTransition.transfers.get(activeTransition.currentStep));
                    activeTransition.currentStep++;
                }

                if (activeTransition.currentStep < activeTransition.transfers.size()) {
                    scheduleNextStep(client, id);
                } else {
                    completeTransition(client);
                }
            }
        }), delay, TimeUnit.MILLISECONDS);
    }

    private static void completeTransition(MinecraftClient client) {
        if (activeTransition == null) return;
        currentPage = activeTransition.targetPage;
        isRowSwapped = (currentPage != 0);
        if (client.currentScreen == null && client.player != null) {
            sendCloseInventoryPacket(client.player);
        }
        activeTransition = null;
        if (blockInputEnabled || freezeMovementEnabled) {
            freezeTicks = 1;
        } else {
            checkResumeSprint(client);
        }
    }

    public static void cycleHotbar(boolean next) {
        if (isTransitioning()) return;
        int target = next ? (currentPage + 1) % 4 : (currentPage - 1 + 4) % 4;
        setHotbarRow(target);
    }

    public static void revertToFixed() {
        if (currentPage == 0) return;
        HotbarCyclerConfig cfg = HotbarCyclerConfig.get();
        int delay = cfg.revertSlotDelay ? cfg.getEffectiveSlotDelay() : 0;
        setHotbarRow(0, delay);
    }

    public static void setHotbarRow(int targetRow) {
        HotbarCyclerConfig cfg = HotbarCyclerConfig.get();
        setHotbarRow(targetRow, cfg.getEffectiveSlotDelay());
    }

    public static void setHotbarRow(int targetRow, int delay) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return;
        if (isTransitioning()) {
            if (client.currentScreen instanceof HandledScreen && targetRow == 0) {
                activeTransition = null;
            } else {
                return;
            }
        }
        if (targetRow < 0 || targetRow > 3 || targetRow == currentPage) return;
        if (client.currentScreen != null && !(client.currentScreen instanceof HandledScreen)) return;

        HotbarCyclerConfig cfg = HotbarCyclerConfig.get();
        boolean pauseSprint = cfg.getEffectivePauseSprint();
        blockInputEnabled = cfg.getEffectiveBlockInput();
        freezeMovementEnabled = cfg.getEffectiveFreezeMovement();

        if (!pauseSprint && client.player.isSprinting()) {
            wasSprinting = true;
        }

        int oldPage = currentPage;
        List<SlotTransfer> transfers = buildTransfers(oldPage, targetRow);
        if (transfers.isEmpty()) return;

        if (delay <= 0) {
            if (pauseSprint) sendStopSprint(client.player);
            if (freezeMovementEnabled && client.currentScreen == null) {
                var vel = client.player.getVelocity();
                client.player.setVelocity(0.0, Math.min(0.0, vel.y), 0.0);
            }
            if ((blockInputEnabled || freezeMovementEnabled) && client.currentScreen == null) {
                freezeTicks = 1;
            }
            for (SlotTransfer transfer : transfers) {
                executeTransfer(client, transfer);
            }
            currentPage = targetRow;
            isRowSwapped = (currentPage != 0);
            if (client.currentScreen == null) {
                sendCloseInventoryPacket(client.player);
            }
            if (!blockInputEnabled && !freezeMovementEnabled) {
                checkResumeSprint(client);
            }
        } else {
            startClientTransition(client, transfers, targetRow, delay,
                    pauseSprint && client.currentScreen == null,
                    freezeMovementEnabled && client.currentScreen == null);
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
            wasSprinting        = false;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            if (freezeTicks > 0) {
                freezeTicks--;
                if (freezeTicks == 0) {
                    checkResumeSprint(client);
                }
            }

            boolean inHandledScreen = (client.currentScreen instanceof HandledScreen);
            HotbarCyclerConfig cfg = HotbarCyclerConfig.get();

            if (inHandledScreen) {
                if (!wasHandledScreen) {
                    wasHandledScreen = true;
                    if (cfg.revertToFixedSlots && currentPage != 0) {
                        revertToFixed();
                    }
                }
            } else {
                if (wasHandledScreen) {
                    wasHandledScreen = false;
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
                        revertToFixed();
                    }
                }
            }

            while (openSettingsKey.wasPressed()) {
                client.setScreen(new HotbarCyclerConfigScreen(client.currentScreen));
            }
            while (toggleRowIndicatorKey.wasPressed()) {
                setRowIndicatorVisible(!isRowIndicatorVisible());
            }
        });
    }

    private static KeyBinding reg(String id) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(
                id, InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), CATEGORY));
    }
}
