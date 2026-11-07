# Homecoming 0.2.0-alpha.1 — Minecraft 1.21.1 / NeoForge

Validation: eleven model tests pass. Dedicated GameTest runs loaded the pinned FTB integrations, verified SavedData across process restarts and exercised actual CheckmarkTask completion, hidden-chapter filtering, recipient isolation and reset filtering using offline identities. The native UI was inspected at GUI scales 1 and 2; enrollment, prompt mute and acknowledgment work. A shared Overworld location was saved as a Xaero waypoint. Cross-dimension rejection, restart deduplication and personal marker preservation passed in a real client. Connected multiplayer and full-pack compatibility remain in progress; see [current evidence](../docs/implementation-status.md).

An early, opt-in catch-up mod for a small friend-group SMP. Play normally; returning players can read shared milestones without maintaining another quest book.

## Current features

- `/recap enable` enrolls you in future briefings; `/recap disable` stops enrollment and removes your access to stored entries.
- FTB Quests completion events, team audience snapshots, current-team checks, and up to two currently available visible quests.
- Selected vanilla advancement milestones (Nether, End, dragon and netherite armor).
- `/recap share <name>` explicitly shares your current dimension and coordinates with enrolled current teammates. Coordinates in a recap can be copied. Without FTB, locations and milestones are personal only.
- `/recap` opens the native journal when Homecoming is installed on both ends, with Updates, Next steps and Settings tabs. Server-only installation falls back to chat. The default page contains five rows; related consecutive quest events can collapse into one row. Explicit acknowledgment consumes only the displayed range.
- A return prompt after 24 hours away; `/recap quiet` or Settings mutes it. `/recap` is always available.
- World-local persistence in `data/homecoming.dat`, with 30-day / 2,000-entry retention and notices when older history expires.
- `/recap status` reports whether FTB integration is loaded.

## Install / build

Use **Minecraft 1.21.1, NeoForge 21.1.262 or newer within 21.1, Java 21**. Put `homecoming-0.2.0-alpha.1.jar` from `build/libs` in server and client mods folders for the journal UI. Server-only installation uses chat. Optional mods have their own client/server requirements.

Compile-time integration target:

| Mod | Version |
| --- | --- |
| FTB Quests (NeoForge) | 2101.1.37 |
| FTB Teams (NeoForge) | 2101.1.9 |
| FTB Library (NeoForge) | 2101.1.36 |
| Architectury (NeoForge) | 13.0.8 |
| Xaero's Minimap (client; adapter target) | 26.6.0 |
| XaeroLib (bundled in official minimap distribution) | 1.7.3 |

Install optional FTB mods and their normal dependencies using their official distributions. These are not bundled into Homecoming. No pack or quest definitions are included. Compatibility with other releases is not established by compilation.

Build with a Java 21 JDK:

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-21'
.\gradlew.bat build --no-daemon --max-workers=2
```

The parent workspace has a project-local JDK under `.tooling/jdk21`; it does not change your system Java installation. The build skips Minecraft decompilation using ModDevGradle's supported binary pipeline to reduce setup memory requirements.

## First two-player check

1. Start a disposable NeoForge world with the listed FTB integrations and a small quest definition.
2. Join Alice and Bob to the same FTB team. Both run `/recap enable`.
3. Alice logs out. Bob completes a quest and shares a location with `/recap share Community storage`.
4. Stop the server normally, restart, and reconnect Alice.
5. Alice runs `/recap`: team completion and location should appear with timestamps. There is no automatic return prompt for a break shorter than 24 hours.
6. Run `/recap read`; the displayed updates should disappear, while later ones stay unread.
7. Repeat with another team and a hidden/reset quest. Alice must not receive that team's history or hidden/unavailable quest suggestions.

## Deliberate limits

This is a first implementation, not a verified full-modpack release. Compilation and model tests do not substitute for the two-player check above. FTB titles are resolved on the server; untranslated pack language keys may be shown. Team quest completion is described as team progress, never credited to an offline individual. Current source completion and visibility are checked again when displaying FTB entries; resets/deleted quests are omitted rather than narrated as fresh completion.

No API key, cloud service, raw chat logging, automatic chest scanning, generated quests, reward claiming or gameplay automation. No retroactive event history before installation/enrollment. The optional Xaero adapter tracks a location only on explicit click and requires the target world/dimension to be active; Overworld creation, cross-dimension rejection, restart deduplication and personal marker preservation passed in real-client checks. No KubeJS, Create, MineColonies or storage-network adapters yet.

History uses Minecraft SavedData and follows normal world saves; an abrupt crash can lose updates since the last save. Keep normal world backups. Player preferences/session records remain until the world data is removed; opting out removes recipient access, not other players' shared history. Already seen coordinates cannot be made secret again. Membership changes establish a new history cutoff; live FTB leave/rejoin event validation is pending.

## Server settings and development checks

The world server config controls `returnPromptHours`, `retentionDays`, `maxHistoryEntries`, `briefingPageSize` and `groupingMinutes`. A grouping window of zero disables grouping.

`gradlew runClient -PwithFtb -PwithXaero` starts a development client with the pinned integrations. If using the official downloaded minimap distribution, add `-PxaeroDistribution=/absolute/path/to/the.jar`; it includes XaeroLib.

`gradlew runGameTestServer -PwithFtb -PruntimeChecks` runs the dedicated persistence check in `run-gametest`. Run it twice: the first emits `HOMECOMING_RESTART_FIXTURE_WRITTEN`, the second `HOMECOMING_RESTART_VERIFIED`. The runtime-check source set is excluded from normal builds and published JARs.

The same suite also exercises actual FTB completion, hidden/reset filtering, group selection, suggestion priority and party leave/rejoin events using offline identities. Add `-PneoForgeVersion=21.1.262` to reproduce checks on the selected pack's loader version.

For opt-in client map regression checks, use a disposable development world and add `-PruntimeChecks -PclientChecks "-PtestWorld=Your disposable world"` to `runClient -PwithFtb -PwithXaero`. This creates personal/shared test markers in that world's map, writes `homecoming-xaero-result.txt`, and exits normally after the check. Run twice to test saved waypoint deduplication. It is a test harness and is excluded from normal builds.

## Provenance

Build wrapper/template: NeoForgeMDKs/MDK-1.21.1-ModDevGradle. See TEMPLATE_LICENSE.txt for template terms. Optional FTB APIs are referenced, not redistributed. Homecoming's own licensing is currently All Rights Reserved pending the owner's choice.
