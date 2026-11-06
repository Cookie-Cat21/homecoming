package dev.homecoming.client;

import dev.homecoming.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class JournalScreen extends Screen {
    private static final int INK = 0xFF292C28, MUTED = 0xFF51564E;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, HH:mm").withZone(ZoneId.systemDefault());
    private final BriefingView view;
    private int tab, left, top, panelWidth, panelHeight, scroll, contentHeight;
    private String feedback = "";
    private final List<RowButton> rowButtons = new ArrayList<>();
    private record RowButton(Button button, int contentY) {}
    public JournalScreen(BriefingView view, int tab) { super(Component.literal("Homecoming")); this.view = view; this.tab = tab; }
    public int tab() { return tab; }
    @Override protected void init() {
        panelWidth = Math.min(580, width - 16); panelHeight = Math.min(370, height - 16);
        left = (width - panelWidth) / 2; top = (height - panelHeight) / 2;
        for (int index = 0; index < 3; index++) {
            final int selected = index;
            var button = Button.builder(Component.literal(new String[]{"Updates", "Next steps", "Settings"}[index]), b -> {
                tab = selected; scroll = 0; rebuildWidgets();
            }).bounds(left + 12 + index * 88, top + 43, 84, 20).build();
            button.active = tab != index; addRenderableWidget(button);
        }
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(left + panelWidth - 74, top + panelHeight - 30, 62, 20).build());
        if (tab == 0 && view.enabled() && !view.updates().isEmpty()) {
            long through = view.updates().getLast().id();
            addRenderableWidget(Button.builder(Component.literal("Mark these read"), b -> request("ack", through))
                    .bounds(left + 12, top + panelHeight - 30, Math.min(130, panelWidth - 100), 20).build());
        }
        rowButtons.clear();
        int y = 0;
        if (!view.enabled()) {
            addRowButton("Enable Homecoming", y + 42, () -> request("enable", 0)); y = 90;
        } else if (tab == 0) {
            if (view.historyGap()) y += 30;
            for (var entry : view.updates()) {
                int lines = font.split(localized(entry.messageJson(), entry.message()), panelWidth - 40).size();
                y += 15 + lines * 10;
                if (entry.place() != null) {
                    y += 14;
                    addRowButton(ModList.get().isLoaded("xaerominimap") ? "Track in Xaero's" : "Copy coordinates", y,
                            () -> track(entry)); y += 25;
                }
                y += 14;
            }
        } else if (tab == 1) {
            for (var suggestion : view.suggestions()) {
                y += 17 + font.split(localized(suggestion.titleJson(), suggestion.title()), panelWidth - 40).size() * 10;
                addRowButton("Open quest", y, () -> openQuest(suggestion)); y += 40;
            }
        } else {
            addRowButton(view.quiet() ? "Enable return prompts" : "Mute return prompts", 25, () -> request("quiet", 0));
            y = 110;
        }
        contentHeight = y;
        positionRowButtons();
    }
    private void addRowButton(String label, int y, Runnable action) {
        var button = addRenderableWidget(Button.builder(Component.literal(label), b -> action.run())
                .bounds(left + 16, top + 72 + y - scroll, Math.min(160, panelWidth - 40), 20).build());
        rowButtons.add(new RowButton(button, y));
    }
    private int viewportHeight() { return panelHeight - 122; }
    private void positionRowButtons() {
        scroll = Math.max(0, Math.min(scroll, Math.max(0, contentHeight - viewportHeight())));
        for (var row : rowButtons) {
            int y = top + 72 + row.contentY - scroll; row.button.setY(y);
            row.button.visible = y >= top + 70 && y + 20 <= top + panelHeight - 49;
        }
    }
    private Component localized(String json, String fallback) {
        if (json == null || minecraft.level == null) return Component.literal(fallback);
        try { var c = Component.Serializer.fromJson(json, minecraft.level.registryAccess()); return c == null ? Component.literal(fallback) : c; }
        catch (RuntimeException ignored) { return Component.literal(fallback); }
    }
    private void track(BriefingView.Entry entry) {
        var p = entry.place();
        if (ModList.get().isLoaded("xaerominimap")) {
            try { feedback = XaeroIntegration.track(entry.id(), entry.message(), p); }
            catch (RuntimeException | LinkageError error) { feedback = "Map integration unavailable; coordinates copied."; copy(p); }
        } else { copy(p); feedback = "Coordinates copied (" + p.dimension() + ")."; }
    }
    private void copy(Journal.Place p) { minecraft.keyboardHandler.setClipboard(p.x() + " " + p.y() + " " + p.z()); }
    private void openQuest(BriefingView.Suggestion suggestion) {
        if (!ModList.get().isLoaded("ftbquests")) { feedback = "FTB Quests is not installed on this client."; return; }
        try { QuestNavigation.open(suggestion.questId()); }
        catch (RuntimeException | LinkageError error) { feedback = "Open your quest book: " + suggestion.chapter() + " / " + suggestion.title(); }
    }
    private static void request(String action, long id) { PacketDistributor.sendToServer(new HomecomingNetwork.Request(action, id)); }
    // Screen.render invokes this before widgets. The journal paints its own background;
    // vanilla's blur here would also blur all of the text already painted above it.
    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xA018211B);
        graphics.fill(left - 2, top - 2, left + panelWidth + 2, top + panelHeight + 2, 0xFF232720);
        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xFFD8D5CA);
        graphics.fill(left, top, left + panelWidth, top + 2, 0xFFF0EDE3);
        graphics.drawString(font, "HOMECOMING", left + 12, top + 10, MUTED, false);
        graphics.drawString(font, "Welcome back, " + (minecraft.player == null ? "traveler" : minecraft.player.getGameProfile().getName()) + ".", left + 12, top + 25, INK, false);
        int y = top + 73 - scroll;
        graphics.enableScissor(left + 10, top + 70, left + panelWidth - 10, top + panelHeight - 49);
        if (!view.enabled()) {
            drawWrapped(graphics, Component.literal("Catch up on shared progress without another quest book. Enable to record future updates."), y);
        } else if (tab == 0) {
            if (view.historyGap()) { drawWrapped(graphics, Component.literal("Some older updates expired under server retention settings."), y); y += 30; }
            if (view.updates().isEmpty()) drawWrapped(graphics, Component.literal("You're caught up. Future shared milestones will appear here."), y);
            for (var entry : view.updates()) {
                graphics.drawString(font, DATE.format(Instant.ofEpochMilli(entry.time())), left + 16, y, MUTED, false); y += 15;
                y += drawWrapped(graphics, localized(entry.messageJson(), entry.message()), y);
                if (entry.place() != null) {
                    var p = entry.place();
                    graphics.drawString(font, font.plainSubstrByWidth(p.dimension() + " | " + p.x() + ", " + p.y() + ", " + p.z(), panelWidth - 40), left + 16, y + 2, MUTED, false);
                    y += 39;
                }
                y += 14;
            }
        } else if (tab == 1) {
            if (view.suggestions().isEmpty()) drawWrapped(graphics, Component.literal("No available quest suggestions right now. Your quest book remains the source of progress."), y);
            for (var suggestion : view.suggestions()) {
                graphics.drawString(font, font.plainSubstrByWidth(localized(suggestion.chapterJson(), suggestion.chapter()).getString(), panelWidth - 40), left + 16, y, MUTED, false); y += 17;
                y += drawWrapped(graphics, localized(suggestion.titleJson(), suggestion.title()), y); y += 40;
            }
        } else {
            drawWrapped(graphics, Component.literal("Return prompts: " + (view.quiet() ? "muted" : "enabled")), y);
            drawWrapped(graphics, Component.literal(view.status()), y + 60);
        }
        graphics.disableScissor();
        positionRowButtons(); super.render(graphics, mouseX, mouseY, partialTick);
        if (!feedback.isEmpty()) graphics.drawString(font, font.plainSubstrByWidth(feedback, panelWidth - 28), left + 12, top + panelHeight - 44, MUTED, false);
        else if (contentHeight > viewportHeight()) graphics.drawString(font, "Scroll for more", left + 12, top + panelHeight - 44, MUTED, false);
    }
    private int drawWrapped(GuiGraphics graphics, Component text, int y) {
        var lines = font.split(text, panelWidth - 40);
        for (FormattedCharSequence line : lines) { graphics.drawString(font, line, left + 16, y, INK, false); y += 10; }
        return lines.size() * 10;
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        scroll -= (int) (vertical * 24); positionRowButtons(); return true;
    }
    @Override public boolean isPauseScreen() { return false; }
}
