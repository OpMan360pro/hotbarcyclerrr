package com.example.hotbarcycler;

import com.example.hotbarcycler.screen.HotbarCyclerConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class HotbarCyclerClient implements ClientModInitializer {

    private static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of("hotbarcycler", "hotbarcycler"));

    // ── Keybindings ───────────────────────────────────────────────────────────
    private static KeyBinding nextHotbarKey;
    private static KeyBinding prevHotbarKey;
    private static KeyBinding nextColumnKey;
    private static KeyBinding prevColumnKey;
    private static KeyBinding openSettingsKey;
    private static KeyBinding toggleColumnPreviewKey;
    private static KeyBinding toggleRowIndicatorKey;
    private static KeyBinding toggleInventoryOverlayKey;

    // ── Runtime state ─────────────────────────────────────────────────────────
    private static int currentPage = 0;
    private static boolean isRowSwapped = false;
    private static boolean wasInScreen = false;
    private static boolean wasNextDown = false;
    private static boolean wasPrevDown = false;

    private static boolean columnPreviewVisible = true;
    private static boolean rowIndicatorVisible = true;
    private static boolean inventoryOverlayVisible = false;

    // ── Getters ───────────────────────────────────────────────────────────────
    public static int     getCurrentPage()             { return currentPage; }
    public static boolean isRowSwapped()               { return isRowSwapped; }
    public static boolean isColumnPreviewVisible()     { return columnPreviewVisible; }
    public static boolean isRowIndicatorVisible()      { return rowIndicatorVisible; }
    public static boolean isInventoryOverlayVisible()  { return inventoryOverlayVisible; }

    public static KeyBinding getNextHotbarKey() { return nextHotbarKey; }
    public static KeyBinding getPrevHotbarKey() { return prevHotbarKey; }

    public static void cycleHotbar(boolean next) {
        int oldPage = currentPage;
        if (next) {
            currentPage = (currentPage + 1) % 4;
        } else {
            currentPage = (currentPage - 1 + 4) % 4;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.currentScreen == null) {
            if (oldPage != 0) swapLocalRowWithHotbar(client.player, oldPage);
            if (currentPage != 0) swapLocalRowWithHotbar(client.player, currentPage);
            isRowSwapped = (currentPage != 0);
            ClientPlayNetworking.send(new CycleHotbarPayload(currentPage));
        }
    }

    public static void setHotbarRow(int targetRow) {
        if (targetRow < 0 || targetRow > 3 || targetRow == currentPage) return;
        int oldPage = currentPage;
        currentPage = targetRow;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.currentScreen == null) {
            if (oldPage != 0) swapLocalRowWithHotbar(client.player, oldPage);
            if (currentPage != 0) swapLocalRowWithHotbar(client.player, currentPage);
            isRowSwapped = (currentPage != 0);
            ClientPlayNetworking.send(new CycleHotbarPayload(currentPage));
        }
    }

    public static void swapLocalRowWithHotbar(ClientPlayerEntity player, int page) {
        if (player == null || page < 1 || page > 3) return;
        int targetStart = page * 9;
        var stacks = player.getInventory().getMainStacks();
        for (int i = 0; i < 9; i++) {
            ItemStack temp = stacks.get(i);
            stacks.set(i, stacks.get(targetStart + i));
            stacks.set(targetStart + i, temp);
        }
    }

    private static boolean isKeyBindingDown(MinecraftClient client, KeyBinding keyBinding) {
        if (keyBinding == null || keyBinding.isUnbound() || client.getWindow() == null) return false;
        InputUtil.Key boundKey = KeyBindingHelper.getBoundKeyOf(keyBinding);
        long handle = client.getWindow().getHandle();
        if (boundKey.getCategory() == InputUtil.Type.KEYSYM) {
            return GLFW.glfwGetKey(handle, boundKey.getCode()) == GLFW.GLFW_PRESS;
        } else if (boundKey.getCategory() == InputUtil.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(handle, boundKey.getCode()) == GLFW.GLFW_PRESS;
        }
        return false;
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    @Override
    public void onInitializeClient() {
        HotbarCyclerConfig.get();

        nextHotbarKey = reg("key.hotbarcycler.next");
        prevHotbarKey = reg("key.hotbarcycler.prev");
        nextColumnKey = reg("key.hotbarcycler.column_next");
        prevColumnKey = reg("key.hotbarcycler.column_prev");
        openSettingsKey          = reg("key.hotbarcycler.open_settings");
        toggleColumnPreviewKey   = reg("key.hotbarcycler.toggle_column_preview");
        toggleRowIndicatorKey    = reg("key.hotbarcycler.toggle_row_indicator");
        toggleInventoryOverlayKey = reg("key.hotbarcycler.toggle_inventory_overlay");

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            currentPage             = 0;
            isRowSwapped            = false;
            wasInScreen             = false;
            wasNextDown             = false;
            wasPrevDown             = false;
            inventoryOverlayVisible = false;
            columnPreviewVisible = HotbarCyclerConfig.get().showColumnPreview;
            rowIndicatorVisible  = HotbarCyclerConfig.get().showRowIndicator;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            // Synchronize static inventory: restore to static row 0 inside menus,
            // re-apply active hotbar row outside menus.
            boolean inScreen = (client.currentScreen != null);
            if (inScreen && !wasInScreen) {
                if (currentPage != 0 && isRowSwapped) {
                    swapLocalRowWithHotbar(client.player, currentPage);
                    isRowSwapped = false;
                    ClientPlayNetworking.send(new CycleHotbarPayload(0));
                }
            } else if (!inScreen && wasInScreen) {
                if (currentPage != 0 && !isRowSwapped) {
                    swapLocalRowWithHotbar(client.player, currentPage);
                    isRowSwapped = true;
                    ClientPlayNetworking.send(new CycleHotbarPayload(currentPage));
                }
            }
            wasInScreen = inScreen;

            // Process keybinds both in-screen (via low-level GLFW polling) and outside screen
            if (inScreen) {
                boolean nextDown = isKeyBindingDown(client, nextHotbarKey);
                if (nextDown && !wasNextDown) cycleHotbar(true);
                wasNextDown = nextDown;

                boolean prevDown = isKeyBindingDown(client, prevHotbarKey);
                if (prevDown && !wasPrevDown) cycleHotbar(false);
                wasPrevDown = prevDown;
            } else {
                wasNextDown = false;
                wasPrevDown = false;
                while (nextHotbarKey.wasPressed()) {
                    cycleHotbar(true);
                }
                while (prevHotbarKey.wasPressed()) {
                    cycleHotbar(false);
                }
            }

            while (openSettingsKey.wasPressed()) {
                client.setScreen(new HotbarCyclerConfigScreen(client.currentScreen));
            }
            while (toggleColumnPreviewKey.wasPressed()) {
                columnPreviewVisible = !columnPreviewVisible;
            }
            while (toggleRowIndicatorKey.wasPressed()) {
                rowIndicatorVisible = !rowIndicatorVisible;
            }
            while (toggleInventoryOverlayKey.wasPressed()) {
                inventoryOverlayVisible = !inventoryOverlayVisible;
            }

            int sel = client.player.getInventory().getSelectedSlot();
            while (nextColumnKey.wasPressed()) {
                ClientPlayNetworking.send(new CycleColumnPayload(sel, true));
            }
            while (prevColumnKey.wasPressed()) {
                ClientPlayNetworking.send(new CycleColumnPayload(sel, false));
            }
        });
    }

    private static KeyBinding reg(String id) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(
                id, InputUtil.Type.KEYSYM, InputUtil.UNKNOWN_KEY.getCode(), CATEGORY));
    }
}
