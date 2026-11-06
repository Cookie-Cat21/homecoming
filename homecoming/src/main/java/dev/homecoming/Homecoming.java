package dev.homecoming;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Mod("homecoming")
public final class Homecoming {
    static Integration integration = new Integration() {};
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d HH:mm 'UTC'").withZone(ZoneOffset.UTC);
    public Homecoming() {
        if (ModList.get().isLoaded("ftbquests") && ModList.get().isLoaded("ftbteams")) {
            integration = new FtbIntegration();
        }
        NeoForge.EVENT_BUS.addListener(this::commands);
        NeoForge.EVENT_BUS.addListener(this::login);
        NeoForge.EVENT_BUS.addListener(this::logout);
        NeoForge.EVENT_BUS.addListener(this::advancement);
    }
    interface Integration {
        default String team(ServerPlayer player) { return player.getStringUUID(); }
        default Set<String> members(ServerPlayer player) { return Set.of(player.getStringUUID()); }
        default boolean visible(ServerPlayer player, Journal.Fact fact) { return !fact.source().startsWith("ftb"); }
        default List<String> suggestions(ServerPlayer player) { return List.of(); }
        default String status() { return "Vanilla milestones; FTB Quests + Teams not installed"; }
    }
    private void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var data = JournalData.get(player.server);
        var state = data.journal.player(player.getStringUUID());
        if (!state.enabled) {
            player.sendSystemMessage(Component.literal("Homecoming: optional catch-up briefings for shared progress. ")
                    .append(button("[Enable]", "/recap enable")));
        } else if (System.currentTimeMillis() - state.lastLogout >= 24L * 60 * 60 * 1000) {
            player.sendSystemMessage(Component.literal("Welcome back. ").append(button("[Catch up]", "/recap")));
        }
        data.setDirty();
    }
    private void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var data = JournalData.get(player.server);
        data.journal.player(player.getStringUUID()).lastLogout = System.currentTimeMillis();
        data.setDirty();
    }
    private void advancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // Only milestone titles; never fill the journal with recipe unlocks or inventory churn.
        if (!Set.of("story/enter_the_nether", "story/enter_the_end", "end/kill_dragon", "nether/netherite_armor")
                .contains(event.getAdvancement().id().getPath())) return;
        var display = event.getAdvancement().value().display();
        if (display.isEmpty()) return;
        var data = JournalData.get(player.server);
        if (!data.journal.player(player.getStringUUID()).enabled) return;
        data.journal.add(System.currentTimeMillis(), "advancement", player.getStringUUID() + ":" + event.getAdvancement().id(),
                integration.team(player), integration.members(player),
                player.getGameProfile().getName() + " earned " + display.get().getTitle().getString(), null);
        data.setDirty();
    }
    static Component button(String label, String command) {
        return Component.literal(label).withStyle(style -> style.withColor(0x70D6B5)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command)));
    }
    private void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("recap")
                .executes(ctx -> show(ctx.getSource().getPlayerOrException()))
                .then(Commands.literal("enable").executes(ctx -> {
                    var p = ctx.getSource().getPlayerOrException();
                    var d = JournalData.get(p.server);
                    var s = d.journal.player(p.getStringUUID());
                    if (!s.enabled) { s.enabled = true; s.acknowledged = d.journal.sequence; s.offered = d.journal.sequence; }
                    d.setDirty();
                    p.sendSystemMessage(Component.literal("Homecoming enabled. Future shared milestones will appear here. /recap disable to stop."));
                    return 1;
                }))
                .then(Commands.literal("disable").executes(ctx -> {
                    var p = ctx.getSource().getPlayerOrException();
                    var d = JournalData.get(p.server); d.journal.disable(p.getStringUUID()); d.setDirty();
                    p.sendSystemMessage(Component.literal("Homecoming disabled; your access to saved briefing entries has been cleared.")); return 1;
                }))
                .then(Commands.literal("read").executes(ctx -> {
                    var p = ctx.getSource().getPlayerOrException(); var d = JournalData.get(p.server);
                    d.journal.acknowledge(p.getStringUUID()); d.setDirty();
                    p.sendSystemMessage(Component.literal("Displayed updates marked read. Newer updates stay unread.")); return 1;
                }))
                .then(Commands.literal("status").executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal(integration.status()), false); return 1;
                }))
                .then(Commands.literal("share").then(Commands.argument("name", StringArgumentType.greedyString()).executes(ctx -> {
                    var p = ctx.getSource().getPlayerOrException(); var d = JournalData.get(p.server);
                    if (!d.journal.player(p.getStringUUID()).enabled) { p.sendSystemMessage(Component.literal("Enable Homecoming first: /recap enable")); return 0; }
                    var name = StringArgumentType.getString(ctx, "name").strip();
                    if (name.isEmpty() || name.length() > 64 || name.chars().anyMatch(Character::isISOControl)) return 0;
                    var pos = p.blockPosition();
                    d.journal.add(System.currentTimeMillis(), "place", p.getStringUUID() + ":" + name,
                            integration.team(p), integration.members(p), p.getGameProfile().getName() + " shared " + name,
                            new Journal.Place(p.level().dimension().location().toString(), pos.getX(), pos.getY(), pos.getZ()));
                    d.setDirty(); p.sendSystemMessage(Component.literal("Shared with enrolled current team members: " + name)); return 1;
                }))));
    }
    private int show(ServerPlayer player) {
        var data = JournalData.get(player.server); var journal = data.journal;
        var state = journal.player(player.getStringUUID());
        if (!state.enabled) { player.sendSystemMessage(button("[Enable Homecoming]", "/recap enable")); return 0; }
        var facts = journal.unread(player.getStringUUID(), integration.team(player), System.currentTimeMillis()).stream()
                .filter(f -> integration.visible(player, f)).limit(5).toList();
        player.sendSystemMessage(Component.literal("— Homecoming —"));
        if (state.acknowledged < journal.droppedThrough)
            player.sendSystemMessage(Component.literal("Some older history expired (30 days / 2,000 updates)."));
        if (facts.isEmpty()) player.sendSystemMessage(Component.literal("No new shared updates recorded for you."));
        for (var fact : facts) {
            player.sendSystemMessage(Component.literal(DATE.format(Instant.ofEpochMilli(fact.time())) + " · " + fact.message()));
            if (fact.place() != null) {
                var p = fact.place();
                player.sendSystemMessage(Component.literal("  " + p.dimension() + " · " + p.x() + ", " + p.y() + ", " + p.z())
                        .withStyle(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD,
                                p.x() + " " + p.y() + " " + p.z()))));
            }
        }
        if (!facts.isEmpty()) {
            state.offered = facts.getLast().id();
            player.sendSystemMessage(button("[Mark these read]", "/recap read").copy().append("  ").append(button("[Refresh]", "/recap")));
        }
        for (String suggestion : integration.suggestions(player))
            player.sendSystemMessage(Component.literal("Available in your quest book: " + suggestion));
        data.setDirty(); return facts.size();
    }
}
