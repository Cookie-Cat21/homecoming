package dev.homecoming.client;

import dev.homecoming.BriefingView;
import net.minecraft.client.Minecraft;

/** Only reached on the physical client, on its main thread. */
public final class HomecomingClient {
    public static void receive(BriefingView view) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;
        int tab = minecraft.screen instanceof JournalScreen screen ? screen.tab() : 0;
        minecraft.setScreen(new JournalScreen(view, tab));
    }
    private HomecomingClient() {}
}
