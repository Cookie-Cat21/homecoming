# Homecoming delivery checklist

Objective: complete the runtime validation, returning-player scenario, briefing selection, journal UI, Xaero integration, pinned modpack pilot, and public alpha described in this chat. An automated test is not a substitute for a real friend-group pilot.

## Gates

- [x] Actual NeoForge server loads with pinned FTB integrations.
- [x] Runtime quest events, hidden/reset quests and team isolation verified with offline identities in GameTest (connected-player pilot pending).
- [x] Returning-player history survives a real process restart (SavedData with simulated reader identities; connected-player scenario remains open).
- [x] Configurable retention, prompt interval and briefing size.
- [x] Membership periods prevent old access after leave/rejoin (real FTB party API/event check with offline identities).
- [x] Related quest completions collapse and suggestions prioritize meaningful progress.
- [x] Native journal screen and authenticated server/client actions.
- [x] Xaero 1.21.1 adapter creates the correct waypoint without touching personal sets (Overworld creation, cross-dimension rejection and restart deduplication checked in a real client).
- [x] UI inspected inside Minecraft at multiple GUI scales.
- [ ] One real modpack release pinned and compatibility tested.
- [ ] Friend-group pilot over actual return visits, with feedback.
- [x] Public alpha JAR, dependency matrix, installation and limitations published.

Evidence and unresolved limits will be recorded here as work proceeds. Baseline: 0.1.0 builds locally and in CI and has six pure journal tests; it has not been live-tested.

## Current follow-up evidence — 2026-11-07

- Four dedicated GameTests pass on NeoForge **21.1.262**: real completion/visibility/reset handling, persistence, grouping/suggestions and party membership changes. Suggestions prefer a started available quest, then a quest in the active chapter; a started but dependency-locked quest is excluded. Two completed quests plus their chapter collapse into one briefing row with the correct acknowledgment cursor.
- Real FTB `createParty`, `join` and `leave` calls with offline identities invoke Homecoming's membership listener. Leaving/rejoining prevents access to previous membership's history while new events remain readable.
- An opt-in development client check ran in the disposable world using Xaero 26.6.0 / XaeroLib 1.7.3. It creates a separate personal fixture, tracks a shared Overworld location, checks duplicate clicks, rejects a Nether location on the Overworld map, and compares the personal marker's name, symbol and coordinates before/after.
- A fresh client process on NeoForge **21.1.262** reloaded the saved map and emitted `HOMECOMING_XAERO_RUNTIME_VERIFIED dimension=minecraft:overworld persisted=true personalMarkers=1`. The shared waypoint remained unique after reload. A durable result is written to the development game directory. These checks are excluded from normal builds.
- Minimum declared NeoForge version is now 21.1.262, after successful compilation, actual client launch/map checks and all four dedicated GameTests there; default development version remains 21.1.265.
- Found E: with sufficient free space, resolving the C: capacity constraint without moving/deleting user files. Downloaded and verified all **5,113** files in the official FTB Skies 2 1.25.0 manifest into `E:/HomecomingTesting/ftb-skies-2-1.25.0`. Full-pack launch is in progress. See `docs/compatibility/ftb-skies-2-1.25.0.json` for exact manifest and dependency pins.
- Full-pack development launch exposed bundled test registration against an absent optional mod. Switched to the official production launcher profile and official install processors, using cached Mojang client/mapping files verified against their official SHA1 values where downloads reset connections. The production launch passed mod initialization and reached resource reload, then failed a native-memory allocation under the current Windows commit limit. A retry with a 5 GB heap and reduced JVM workers is in progress. Full-pack runtime compatibility remains unverified.
- A concrete [friend-group pilot guide](friend-group-pilot.md) now covers two connected players, restart, acknowledgments, quest links, team isolation, leave/rejoin, maps and feedback over two actual return visits. Real participant observations remain outstanding.
- Published [v0.2.0-alpha.1](https://github.com/Cookie-Cat21/homecoming/releases/tag/v0.2.0-alpha.1) with a normal-build JAR, SHA256 sidecar, exact integration baseline, installation steps and explicit outstanding validation. Release points to commit `e9a38a4`; development fixtures are absent from its JAR. This is an experimental pilot build, not a completed friend-group pilot or a full-pack compatibility claim.
- The 5 GB production-pack attempt also hit the Windows memory commit limit (74 MB remaining at failure); Java heap occupancy was about 2.9 GB while committed capacity had grown to about 4.6 GB. Retrying the same complete pack with a 4 GB cap. Desktop captures currently show a clock/screensaver; the user has been asked to unlock before further UI inspection.

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
