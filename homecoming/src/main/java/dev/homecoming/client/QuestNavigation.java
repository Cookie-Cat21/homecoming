package dev.homecoming.client;

import dev.ftb.mods.ftbquests.client.ClientQuestFile;

final class QuestNavigation {
    static void open(String id) { ClientQuestFile.openBookToQuestObject(Long.parseUnsignedLong(id, 16)); }
    private QuestNavigation() {}
}
