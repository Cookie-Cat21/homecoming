# Homecoming 0.1.0 — Minecraft 1.21.1 / NeoForge

Validation: `gradlew build` succeeded with Java 21; all 6 journal tests passed (audience isolation, acknowledgment, opt-out, retention/deduplication, enrollment, and serialization). A live two-player FTB session and Xaero client integration have not been tested.

An early, opt-in catch-up mod for a small friend-group SMP. Play normally; returning players can read shared milestones without maintaining another quest book.

## Current features

- `/recap enable` enrolls you in future briefings; `/recap disable` stops enrollment and removes your access to stored entries.
- FTB Quests completion events, team audience snapshots, current-team checks, and up to two currently available visible quests.
- Selected vanilla advancement milestones (Nether, End, dragon and netherite armor).
- `/recap share <name>` explicitly shares your current dimension and coordinates with enrolled current teammates. Coordinates in a recap can be copied. Without FTB, locations and milestones are personal only.
- `/recap` displays five unread updates; `/recap read` acknowledges only the displayed range. Repeat to read additional pages. Reading does not acknowledge automatically.
- A quiet return prompt after 24 hours away; `/recap` is always available.
- World-local persistence in `data/homecoming.dat`, with 30-day / 2,000-entry retention and notices when older history expires.
- `/recap status` reports whether FTB integration is loaded.

## Install / build

Use **Minecraft 1.21.1, NeoForge 21.1.265 or newer within 21.1, Java 21**. Put `homecoming-0.1.0.jar` from `build/libs` in the server's mods folder. The current text-only interface uses vanilla chat and does not require a Homecoming client mod. Optional mods have their own client/server requirements.

Compile-time integration target:

| Mod | Version |
| --- | --- |
| FTB Quests (NeoForge) | 2101.1.37 |
| FTB Teams (NeoForge) | 2101.1.9 |
| FTB Library (NeoForge) | 2101.1.36 |
| Architectury (NeoForge) | 13.0.8 |

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

No API key, cloud service, raw chat logging, automatic chest scanning, generated quests, reward claiming or gameplay automation. No retroactive event history before installation/enrollment. No automatic Xaero waypoint creation yet: location display is a coordinate fallback pending a version-pinned client adapter. No KubeJS, Create, MineColonies or storage-network adapters yet.

History uses Minecraft SavedData and follows normal world saves; an abrupt crash can lose updates since the last save. Keep normal world backups. Player preferences/session records remain until the world data is removed; opting out removes recipient access, not other players' shared history. Already seen coordinates cannot be made secret again. Team membership is checked at event capture and display, but full membership-era tracking across leave-and-rejoin cycles is not implemented.

## Provenance

Build wrapper/template: NeoForgeMDKs/MDK-1.21.1-ModDevGradle. See TEMPLATE_LICENSE.txt for template terms. Optional FTB APIs are referenced, not redistributed. Homecoming's own licensing is currently All Rights Reserved pending the owner's choice.
