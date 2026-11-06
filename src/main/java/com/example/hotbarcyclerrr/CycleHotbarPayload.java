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
public record CycleHotbarPayload(int targetRow) implements CustomPayload {

    public static final CustomPayload.Id<CycleHotbarPayload> ID =
            new CustomPayload.Id<>(Identifier.of("hotbarcyclerrr", "cycle"));

    public static final PacketCodec<PacketByteBuf, CycleHotbarPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> buf.writeInt(value.targetRow()),
                    buf -> new CycleHotbarPayload(buf.readInt())
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
