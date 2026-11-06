package dev.homecoming;

import com.google.gson.Gson;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.fml.loading.FMLEnvironment;
import java.util.*;

public final class HomecomingNetwork {
    private static final Gson GSON = new Gson();
    private static final Map<UUID, Long> LAST_REQUEST = new HashMap<>();
    public record Request(String action, long id) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("homecoming", "request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.of(
                (buf, p) -> { buf.writeUtf(p.action, 16); buf.writeLong(p.id); },
                buf -> new Request(buf.readUtf(16), buf.readLong()));
        @Override public Type<Request> type() { return TYPE; }
    }
    public record Response(String json) implements CustomPacketPayload {
        public static final Type<Response> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("homecoming", "briefing"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Response> CODEC = StreamCodec.of(
                (buf, p) -> buf.writeUtf(p.json, 30000), buf -> new Response(buf.readUtf(30000)));
        @Override public Type<Response> type() { return TYPE; }
    }
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("2").optional();
        registrar.playToServer(Request.TYPE, Request.CODEC, (request, context) -> context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            long now = System.currentTimeMillis();
            if (now - LAST_REQUEST.getOrDefault(player.getUUID(), 0L) < 150) return;
            LAST_REQUEST.put(player.getUUID(), now);
            switch (request.action) {
                case "open" -> send(player);
                case "ack" -> {
                    var data = JournalData.get(player.server);
                    var state = data.journal.player(player.getStringUUID());
                    // Bound acknowledgment to a range actually offered by the server.
                    if (request.id >= 0 && request.id <= state.offered) {
                        state.acknowledged = Math.max(state.acknowledged, request.id);
                        data.setDirty();
                    }
                    send(player);
                }
                case "quiet" -> {
                    var data = JournalData.get(player.server); var state = data.journal.player(player.getStringUUID());
                    state.quiet = !state.quiet; data.setDirty(); send(player);
                }
                case "enable" -> { Homecoming.enable(player); send(player); }
                default -> { /* Unknown actions never execute commands or mutate arbitrary source data. */ }
            }
        }));
        if (FMLEnvironment.dist.isClient()) {
            registrar.playToClient(Response.TYPE, Response.CODEC,
                    (response, context) -> context.enqueueWork(() -> dev.homecoming.client.HomecomingClient.receive(GSON.fromJson(response.json, BriefingView.class))));
        } else registrar.playToClient(Response.TYPE, Response.CODEC, (response, context) -> {});
    }
    public static boolean supported(ServerPlayer player) { return NetworkRegistry.hasChannel(player.connection, Response.TYPE.id()); }
    public static void send(ServerPlayer player) {
        if (supported(player)) PacketDistributor.sendToPlayer(player, new Response(GSON.toJson(Homecoming.view(player))));
    }
    public static void forget(UUID player) { LAST_REQUEST.remove(player); }
    private HomecomingNetwork() {}
}
