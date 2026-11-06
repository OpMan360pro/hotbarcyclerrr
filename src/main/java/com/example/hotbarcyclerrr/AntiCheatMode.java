package com.example.hotbarcyclerrr;

public enum AntiCheatMode {
    NONE("None (Instant)", 0, false, false, "Instant swap (Default). No anti-cheat adjustments."),
    GRIM_AC("GrimAC", 1, true, true, "1 tick/slot (50ms) + sprint pause & freeze. GrimAC safe."),
    LIGHTNING_GRIM("Lightning GrimAC", 1, true, true, "1 tick/slot (50ms) + sprint pause & freeze. Fast Grim safe."),
    POLAR("Polar", 2, true, true, "2 ticks/slot (100ms) + freeze. Polar InvMove safe."),
    VULCAN("Vulcan", 2, true, true, "2 ticks/slot (100ms) + freeze. Vulcan FastInventory & InvMove safe."),
    MATRIX("Matrix", 2, true, true, "2 ticks/slot (100ms) + freeze. Matrix InvMove safe."),
    MLSAC("MLSAC", 3, true, true, "3 ticks/slot (150ms) + freeze. MLSAC burst & rate limit safe."),
    SLOTH_AC("SlothAC", 2, true, true, "2 ticks/slot (100ms) + freeze. SlothAC safe."),
    NIMBUS("Nimbus AntiCheat", 2, true, true, "2 ticks/slot (100ms) + freeze. Nimbus safe pacing."),
    GUARD_AC("GuardAC", 2, true, true, "2 ticks/slot (100ms) + freeze. GuardAC profile."),
    ANTI_GUARD("AntiGuard", 2, true, true, "2 ticks/slot (100ms) + freeze. AntiGuard profile."),
    CUSTOM("Custom", 1, true, true, "User-defined slot delay, sprint, and freeze behavior.");

    private final String displayName;
    private final int delayTicks;
    private final boolean pauseSprint;
    private final boolean freezeMovement;
    private final String description;

    AntiCheatMode(String displayName, int delayTicks, boolean pauseSprint, boolean freezeMovement, String description) {
        this.displayName = displayName;
        this.delayTicks = delayTicks;
        this.pauseSprint = pauseSprint;
        this.freezeMovement = freezeMovement;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getDelayTicks() {
        return delayTicks;
    }

    public boolean isPauseSprint() {
        return pauseSprint;
    }

    public boolean isFreezeMovement() {
        return freezeMovement;
    }

    public String getDescription() {
        return description;
    }

    public AntiCheatMode next() {
        AntiCheatMode[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

    public AntiCheatMode prev() {
        AntiCheatMode[] values = values();
        return values[(this.ordinal() - 1 + values.length) % values.length];
    }
}
