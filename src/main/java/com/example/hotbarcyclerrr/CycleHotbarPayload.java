package com.example.hotbarcyclerrr;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client-to-server packet that carries the target hotbar row:
 *   0 = Default hotbar row (slots 0-8)
 *   1 = Inventory row 1 (slots 9-17)
 *   2 = Inventory row 2 (slots 18-26)
 *   3 = Inventory row 3 (slots 27-35)
 */
public record CycleHotbarPayload(int targetRow, int slotDelayTicks, boolean pauseSprint, boolean freezeMovement) implements CustomPayload {

    public CycleHotbarPayload(int targetRow) {
        this(targetRow, 0, false, false);
    }

    public static final CustomPayload.Id<CycleHotbarPayload> ID =
            new CustomPayload.Id<>(Identifier.of("hotbarcyclerrr", "cycle"));

    public static final PacketCodec<PacketByteBuf, CycleHotbarPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeInt(value.targetRow());
                        buf.writeInt(value.slotDelayTicks());
                        buf.writeBoolean(value.pauseSprint());
                        buf.writeBoolean(value.freezeMovement());
                    },
                    buf -> new CycleHotbarPayload(
                            buf.readInt(),
                            buf.readInt(),
                            buf.readBoolean(),
                            buf.readBoolean()
                    )
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
