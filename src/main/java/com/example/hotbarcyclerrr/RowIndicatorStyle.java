package com.example.hotbarcyclerrr;

public enum RowIndicatorStyle {
    DEFAULT("Default"),
    MINIMAL("Minimal"),
    DOTS("Dots"),
    ACCENT("Accent");

    private final String displayName;

    RowIndicatorStyle(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public RowIndicatorStyle next() {
        RowIndicatorStyle[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
