package com.example.socialisolation.network;

import com.example.socialisolation.SocialIsolation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Packet sent from server → client every second, carrying the player's
 * current social meter value and lifetime points so the client HUD can render both bars.
 */
public record SocialMeterPayload(float meter, float totalPointsRegained) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SocialMeterPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(SocialIsolation.MODID, "social_meter"));

    public static final StreamCodec<FriendlyByteBuf, SocialMeterPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeFloat(payload.meter);
                        buf.writeFloat(payload.totalPointsRegained);
                    },
                    buf -> new SocialMeterPayload(buf.readFloat(), buf.readFloat())
            );

    @Override
    public CustomPacketPayload.Type<SocialMeterPayload> type() {
        return TYPE;
    }
}
