package com.example.hotbarcyclerrr;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class HotbarCyclerConfig {

    // ── Anti-Cheat Compatibility ──────────────────────────────────────────────
    public AntiCheatMode antiCheatMode         = AntiCheatMode.NONE;
    public int           customSlotDelay       = 50;
    public boolean       customPauseSprint     = true;
    public boolean       customFreezeMovement  = true;

    // ── Row Indicator ─────────────────────────────────────────────────────────
    public boolean showRowIndicator        = true;
    public boolean seamlessHotbar          = false;
    public boolean revertToFixedSlots      = false;
    public int     rowIndicatorOffsetX     = 0;
    public int     rowIndicatorOffsetY     = 0;
    public int     rowIndicatorSquareW     = 8;
    public int     rowIndicatorSquareH     = 8;

    public int getEffectiveSlotDelay() {
        if (antiCheatMode == null) return 0;
        if (antiCheatMode == AntiCheatMode.CUSTOM) {
            return Math.max(0, customSlotDelay);
        }
        return antiCheatMode.getDelayMs();
    }

    public boolean getEffectivePauseSprint() {
        if (antiCheatMode == null) return false;
        if (antiCheatMode == AntiCheatMode.CUSTOM) {
            return customPauseSprint;
        }
        return antiCheatMode.isPauseSprint();
    }

    public boolean getEffectiveFreezeMovement() {
        if (antiCheatMode == null) return false;
        if (antiCheatMode == AntiCheatMode.CUSTOM) {
            return customFreezeMovement;
        }
        return antiCheatMode.isFreezeMovement();
    }

    // ── Persistence ──────────────────────────────────────────────────────────
    private static final Gson   GSON      = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "hotbarcyclerrr.json";
    private static HotbarCyclerConfig instance;

    public static HotbarCyclerConfig get() {
        if (instance == null) instance = load();
        return instance;
    }

    private static HotbarCyclerConfig load() {
        Path path = configPath();
        if (Files.exists(path)) {
            try (Reader r = Files.newBufferedReader(path)) {
                HotbarCyclerConfig cfg = GSON.fromJson(r, HotbarCyclerConfig.class);
                if (cfg != null) {
                    if (cfg.antiCheatMode == null) cfg.antiCheatMode = AntiCheatMode.NONE;
                    if (cfg.customSlotDelay > 0 && cfg.customSlotDelay <= 5) {
                        cfg.customSlotDelay *= 50;
                    }
                    return cfg;
                }
            } catch (IOException | com.google.gson.JsonParseException e) {
                HotbarCyclerMod.LOGGER.warn("Failed to load config, using defaults: {}", e.getMessage());
            }
        }
        HotbarCyclerConfig cfg = new HotbarCyclerConfig();
        cfg.save();
        return cfg;
    }

    public void save() {
        try {
            Path path = configPath();
            Files.createDirectories(path.getParent());
            try (Writer w = Files.newBufferedWriter(path)) {
                GSON.toJson(this, w);
            }
        } catch (IOException e) {
            HotbarCyclerMod.LOGGER.warn("Failed to save config: {}", e.getMessage());
        }
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }
}
