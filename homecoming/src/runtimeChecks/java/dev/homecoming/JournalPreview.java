package dev.homecoming;

import dev.homecoming.client.JournalScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opt-in screenshots of the real screen using a disposable integrated test world. */
@EventBusSubscriber(modid="homecoming", value=Dist.CLIENT)
public final class JournalPreview {
    private static int ticks, stage;
    private static BriefingView snapshot;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("homecoming.previewJournal")) return;
        var mc=Minecraft.getInstance();
        if (mc.level==null || mc.player==null || mc.getSingleplayerServer()==null) return;
        if (++ticks < 80) return;
        if (stage==0) {
            stage=1; ticks=0;
            var uuid=mc.player.getUUID(); var server=mc.getSingleplayerServer();
            server.execute(() -> {
                var player=server.getPlayerList().getPlayer(uuid);
                var commands=server.getCommands();
                commands.performPrefixedCommand(player.createCommandSourceStack(),"recap enable");
                commands.performPrefixedCommand(player.createCommandSourceStack(),"recap share Shared meeting point");
                var view=Homecoming.view(player);
                mc.execute(() -> { snapshot=view; mc.setScreen(new JournalScreen(view,0)); });
            });
        } else if (stage==1 && snapshot!=null && mc.screen instanceof JournalScreen) {
            Screenshot.grab(mc.gameDirectory,"homecoming-updates.png",mc.getMainRenderTarget(),message -> System.out.println(message.getString()));
            stage=2; ticks=0;
        } else if (stage==2) {
            mc.setScreen(new JournalScreen(snapshot,2)); stage=3; ticks=0;
        } else if (stage==3) {
            Screenshot.grab(mc.gameDirectory,"homecoming-settings.png",mc.getMainRenderTarget(),message -> System.out.println(message.getString()));
            stage=4; ticks=0;
        } else if (stage==4) {
            System.out.println("HOMECOMING_JOURNAL_SCREENSHOTS_SAVED"); mc.stop(); stage=5;
        }
    }
}
