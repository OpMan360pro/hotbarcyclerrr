package com.example.hotbarcyclerrr;

public enum AntiCheatMode {
    NONE("None (Instant)", 0, false, false, false, "Instant swap (Default). No anti-cheat adjustments."),
    CUSTOM("Custom", 50, true, true, true, "User-defined slot delay, sprint, input, and freeze behavior.");

    private final String displayName;
    private final int delayMs;
    private final boolean pauseSprint;
    private final boolean blockInput;
    private final boolean freezeMovement;
    private final String description;

    AntiCheatMode(String displayName, int delayMs, boolean pauseSprint, boolean blockInput, boolean freezeMovement, String description) {
        this.displayName = displayName;
        this.delayMs = delayMs;
        this.pauseSprint = pauseSprint;
        this.blockInput = blockInput;
        this.freezeMovement = freezeMovement;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getDelayMs() {
        return delayMs;
    }

    public boolean isPauseSprint() {
        return pauseSprint;
    }

    public boolean isBlockInput() {
        return blockInput;
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


