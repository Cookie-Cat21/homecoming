package dev.homecoming;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class HomecomingConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.IntValue RETURN_HOURS = BUILDER.comment("Hours away before a quiet return prompt.")
            .defineInRange("returnPromptHours", 24, 0, 720);
    public static final ModConfigSpec.IntValue RETENTION_DAYS = BUILDER.defineInRange("retentionDays", 30, 1, 365);
    public static final ModConfigSpec.IntValue MAX_FACTS = BUILDER.defineInRange("maxHistoryEntries", 2000, 100, 20000);
    public static final ModConfigSpec.IntValue PAGE_SIZE = BUILDER.defineInRange("briefingPageSize", 5, 1, 8);
    public static final ModConfigSpec.IntValue GROUP_MINUTES = BUILDER.comment("Collapse consecutive completions in the same chapter.")
            .defineInRange("groupingMinutes", 10, 0, 120);
    public static final ModConfigSpec SPEC = BUILDER.build();
    private HomecomingConfig() {}
}
