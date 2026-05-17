package com.livingword.network;

import com.livingword.network.payload.JoinListeningSessionPayload;
import com.livingword.network.payload.LeaveListeningSessionPayload;
import com.livingword.network.payload.OpenBiblePayload;
import com.livingword.network.payload.PlaybackControlPayload;
import com.livingword.network.payload.TimestampCorrectionPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class LivingWordNetwork {
    private LivingWordNetwork() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(LivingWordNetwork::registerPayloadHandlers);
    }

    private static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1").optional();
        registrar.playToClient(OpenBiblePayload.TYPE, OpenBiblePayload.STREAM_CODEC, (payload, context) -> {
        });
        registrar.playToServer(JoinListeningSessionPayload.TYPE, JoinListeningSessionPayload.STREAM_CODEC, (payload, context) -> {
        });
        registrar.playToServer(LeaveListeningSessionPayload.TYPE, LeaveListeningSessionPayload.STREAM_CODEC, (payload, context) -> {
        });
        registrar.playBidirectional(PlaybackControlPayload.TYPE, PlaybackControlPayload.STREAM_CODEC, (payload, context) -> {
        });
        registrar.playToClient(TimestampCorrectionPayload.TYPE, TimestampCorrectionPayload.STREAM_CODEC, (payload, context) -> {
        });
    }
}
