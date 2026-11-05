package com.example.hotbarcycler;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record CycleColumnPayload(int selectedSlot, boolean next) implements CustomPayload {

    public static final CustomPayload.Id<CycleColumnPayload> ID =
            new CustomPayload.Id<>(Identifier.of("hotbarcycler", "cycle_column"));

    public static final PacketCodec<PacketByteBuf, CycleColumnPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> {
                        buf.writeInt(value.selectedSlot());
                        buf.writeBoolean(value.next());
                    },
                    buf -> new CycleColumnPayload(buf.readInt(), buf.readBoolean())
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
