package dev.homecoming.client;

import dev.homecoming.BriefingView;
import net.minecraft.client.Minecraft;

/** Only reached on the physical client, on its main thread. */
public final class HomecomingClient {
    public static void receive(BriefingView view) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;
        if (minecraft.screen instanceof JournalScreen screen) screen.receive(view);
        else minecraft.setScreen(new JournalScreen(view, 0));
    }
    private HomecomingClient() {}
}
