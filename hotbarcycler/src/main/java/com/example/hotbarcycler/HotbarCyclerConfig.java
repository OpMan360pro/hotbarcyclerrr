package com.example.hotbarcycler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class HotbarCyclerConfig {

    // ── Row Indicator ─────────────────────────────────────────────────────────
    public boolean showRowIndicator        = true;
    public boolean seamlessHotbar          = false;
    public int     rowIndicatorOffsetX     = 0;
    public int     rowIndicatorOffsetY     = 0;
    public int     rowIndicatorSquareW     = 8;
    public int     rowIndicatorSquareH     = 8;

    // ── Persistence ──────────────────────────────────────────────────────────
    private static final Gson   GSON      = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "hotbarcycler.json";
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
                if (cfg != null) return cfg;
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
