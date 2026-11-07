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
    public Integer       slotDelay             = null;
    public int           customSlotDelay       = 50;
    public boolean       customPauseSprint     = true;
    public boolean       customBlockInput      = true;
    public boolean       customFreezeMovement  = true;

    // ── Row Indicator ─────────────────────────────────────────────────────────
    public boolean           showRowIndicator    = true;
    public RowIndicatorStyle rowIndicatorStyle   = RowIndicatorStyle.DEFAULT;
    public boolean           seamlessHotbar      = false;
    public boolean           revertToFixedSlots  = false;
    public boolean           revertSlotDelay     = false;
    public int               rowIndicatorOffsetX = 0;
    public int               rowIndicatorOffsetY = 0;
    public int               rowIndicatorSquareW = 8;
    public int               rowIndicatorSquareH = 8;
    public String            rowIndicatorAccentColor = "00AAFF";

    // ── Inventory Row Highlight ───────────────────────────────────────────────
    public String highlightBorderColor   = "FFFFFF";
    public int    highlightBorderOpacity = 100; // 0 - 100%
    public String highlightFillColor     = "FFFFFF";
    public int    highlightFillOpacity   = 21;  // 0 - 100% (~0x35 alpha)

    public int getHighlightBorderArgb() {
        return parseColor(highlightBorderColor, highlightBorderOpacity, 0x00FFFFFF, 255);
    }

    public int getHighlightFillArgb() {
        return parseColor(highlightFillColor, highlightFillOpacity, 0x00FFFFFF, 53);
    }

    public int getRowIndicatorAccentRgb() {
        return parseRgb(rowIndicatorAccentColor, 0x00AAFF);
    }

    public static int parseRgb(String hex, int defaultRgb) {
        if (hex == null) return defaultRgb;
        String clean = hex.replace("#", "").trim();
        if (clean.isEmpty()) return defaultRgb;
        try {
            return (int) Long.parseLong(clean, 16) & 0x00FFFFFF;
        } catch (Exception e) {
            return defaultRgb;
        }
    }

    private static int parseColor(String hex, int opacityPercent, int defaultRgb, int defaultAlpha) {
        int rgb;
        try {
            if (hex != null && !hex.trim().isEmpty()) {
                String clean = hex.replace("#", "").trim();
                rgb = (int) Long.parseLong(clean, 16) & 0x00FFFFFF;
            } else {
                rgb = defaultRgb;
            }
        } catch (Exception e) {
            rgb = defaultRgb;
        }
        int alpha = (opacityPercent >= 0 && opacityPercent <= 100)
                ? Math.round(opacityPercent * 255.0f / 100.0f)
                : defaultAlpha;
        alpha = Math.max(0, Math.min(255, alpha));
        return (alpha << 24) | rgb;
    }

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

    public boolean getEffectiveBlockInput() {
        if (antiCheatMode == null) return false;
        if (antiCheatMode == AntiCheatMode.CUSTOM) {
            return customBlockInput;
        }
        return antiCheatMode.isBlockInput();
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
                    if (cfg.rowIndicatorStyle == null) cfg.rowIndicatorStyle = RowIndicatorStyle.DEFAULT;
                    if (cfg.rowIndicatorAccentColor == null) cfg.rowIndicatorAccentColor = "00AAFF";
                    if (cfg.highlightBorderColor == null) cfg.highlightBorderColor = "FFFFFF";
                    if (cfg.highlightFillColor == null) cfg.highlightFillColor = "FFFFFF";
                    if (cfg.slotDelay != null) {
                        cfg.customSlotDelay = cfg.slotDelay;
                    } else {
                        cfg.slotDelay = cfg.customSlotDelay;
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
        this.slotDelay = this.customSlotDelay;
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
