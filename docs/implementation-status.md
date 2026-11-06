# Homecoming delivery checklist

Objective: complete the runtime validation, returning-player scenario, briefing selection, journal UI, Xaero integration, pinned modpack pilot, and public alpha described in this chat. An automated test is not a substitute for a real friend-group pilot.

## Gates

- [x] Actual NeoForge server loads with pinned FTB integrations.
- [x] Runtime quest events, hidden/reset quests and team isolation verified with offline identities in GameTest (connected-player pilot pending).
- [ ] Returning-player history survives a real process restart.
- [x] Configurable retention, prompt interval and briefing size.
- [x] Membership periods prevent old access after leave/rejoin (model tests; live FTB event coverage pending).
- [ ] Related quest completions collapse and suggestions prioritize meaningful progress.
- [x] Native journal screen and authenticated server/client actions.
- [ ] Xaero 1.21.1 adapter creates the correct waypoint without touching personal sets.
- [x] UI inspected inside Minecraft at multiple GUI scales.
- [ ] One real modpack release pinned and compatibility tested.
- [ ] Friend-group pilot over actual return visits, with feedback.
- [ ] Public alpha JAR, dependency matrix, installation and limitations published.

Evidence and unresolved limits will be recorded here as work proceeds. Baseline: 0.1.0 builds locally and in CI and has six pure journal tests; it has not been live-tested.

## Validation in progress — 2026-11-07

- Eleven JUnit tests pass, including consecutive grouping, zero-window behavior, membership periods and configured retention after serialization.
- A real NeoForge 21.1.265 GameTest process loaded FTB Quests 2101.1.37, Teams 2101.1.9, Library 2101.1.36 and Architectury 13.0.8 with Homecoming.
- Two separate GameTest JVM runs wrote and reloaded the actual world SavedData file. The second emitted `HOMECOMING_RESTART_VERIFIED`: one simulated reader's acknowledgment persisted, the offline reader retained unread history, and another team had no access. This does not yet cover two connected players or an actual FTB completion callback.
- Opened TLauncher using Computer Use; launched an isolated development client/world instead of modifying the user's pack. `/recap` opened the native screen and enrollment round-tripped through its server payload. Found vanilla blur obscuring journal text, fixed it, and awaiting visual recheck.
- Official Xaero Minimap distribution 26.6.0 downloaded from Modrinth and SHA512 matched its API metadata. It bundles XaeroLib 1.7.3. The Maven host and Mojang asset host reset connections locally; development launch uses existing cached Minecraft assets.
- Visual recheck confirmed the blur fix. Updates, empty Next steps and Settings render legibly at the default 2x scale in an 854x480 client viewport. Settings mute round-tripped successfully. An explicitly shared Overworld location at 71,99,-22 created a saved Xaero waypoint in `Homecoming Shared` at those exact coordinates. Observed Xaero's save format omits third-party origin, so deduplication was corrected to use the persistent symbol and coordinates inside that set; recheck pending.
- Journal inspected again at explicit GUI scale 1: panel, text, tracking action and acknowledgment button fit. Clicking Mark these read produced the caught-up state via the server response. Full build passes with all eleven JUnit tests. Populated quest suggestions, long histories, cross-dimension rejection and personal waypoint preservation remain to be exercised.
- Commit `7b62e92` is public on main and its GitHub Actions build passed. The local machine had only about 686 MB free before removing our redundant 205 MB downloaded JDK archive; the pack manifest lists about 1.43 GB of files before setup. Full-pack installation needs more disk space and is not represented as tested.
- Dedicated FTB runtime check now passes alongside the restart check: real CheckmarkTask progress triggered the completion event, enrolled offline team recipients received it, a nonmember did not, hidden-chapter quests were excluded, and resetting the completed quest invalidated its recap entry. This found and fixed a real bug: quest visibility alone does not include its chapter's visibility. Both capture and display now enforce chapter visibility. GameTest identities are offline to avoid the fixture's unnegotiated FTB network channels; this is not a two-client multiplayer session.
- Selected public-pack candidate: FTB Skies 2 **1.25.0**, official pack ID 129 / version ID 100589. Manifest pins Minecraft 1.21.1, NeoForge 21.1.262, FTB Quests 2101.1.37, Teams 2101.1.11, Library 2101.1.37, Architectury 13.0.11. Full pack has not been installed/tested; Homecoming currently requires NeoForge 21.1.265, so this mismatch must be resolved before claiming pack compatibility.
