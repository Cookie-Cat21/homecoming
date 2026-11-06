package dev.homecoming;

import com.google.gson.Gson;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class JournalData extends SavedData {
    private static final Gson GSON = new Gson();
    public final Journal journal;
    public JournalData() { journal = new Journal(); }
    private JournalData(CompoundTag tag, HolderLookup.Provider provider) {
        if (tag.getInt("schema") != 1) throw new IllegalStateException("Unsupported Homecoming journal schema");
        journal = GSON.fromJson(new String(tag.getByteArray("journal"), StandardCharsets.UTF_8), Journal.class);
        if (journal == null || journal.facts == null || journal.players == null)
            throw new IllegalStateException("Invalid Homecoming journal; restore its backup");
    }
    public static JournalData get(MinecraftServer server) {
        var data = server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(() -> {
                    // SavedData catches decoder failures. Never silently replace an unreadable existing journal.
                    if (Files.exists(server.getWorldPath(LevelResource.ROOT).resolve("data/homecoming.dat")))
                        throw new IllegalStateException("Existing Homecoming data could not be loaded; restore a backup before continuing");
                    return new JournalData();
                }, JournalData::new, null), "homecoming");
        data.journal.configure(HomecomingConfig.RETENTION_DAYS.get() * 86_400_000L, HomecomingConfig.MAX_FACTS.get());
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putInt("schema", 1);
        tag.putByteArray("journal", GSON.toJson(journal).getBytes(StandardCharsets.UTF_8));
        return tag;
    }
}
