# Homecoming

A catch-up companion for Minecraft **1.21.1 / NeoForge 21.1**. Return to your friends' world, see what changed, and find an existing quest you can pick up.

Homecoming reuses FTB Quests and FTB Teams instead of asking everyone to maintain another quest book. It works without an AI service.

## Early implementation

- Team quest and chapter completion briefings.
- Selected vanilla progression milestones.
- Shared locations with dimension and copyable coordinates.
- Up to two currently available visible quests.
- Opt-in participation and explicit read acknowledgment.
- History stored with the world, bounded to 30 days / 2,000 updates.

**Status:** version 0.1.0 compiles and passes six journal tests. Live two-player FTB testing is still needed. The current interface is clickable chat. The custom journal screen and Xaero waypoint adapter are proposed work.

## Build

Install a Java 21 JDK, then:

```sh
cd homecoming
./gradlew build --no-daemon --max-workers=2
```

On Windows, use `gradlew.bat`. The JAR is written to `homecoming/build/libs/`.

See the [mod README](homecoming/README.md) for commands, integration versions, installation and the first two-player test.

## Project material

- [Source and tests](homecoming/src)
- [Scope and integration plan](homecoming-scope.md)
- [Opportunity research and evidence](minecraft-opportunity-research-2026-11-06.md)
- [Interactive UI concept source](docs/ui/homecoming-ui.fragment.html)

The UI concept is a conversation-rendered HTML fragment with proposed interactions; it is not the current Minecraft screen. Research records public-source observations and uncertainty, not an exhaustive market survey.

## Licensing and attribution

Homecoming's original code is currently **All Rights Reserved**, pending a license choice. Public availability does not grant an open-source license. The NeoForge MDK template retains its [MIT notice](homecoming/TEMPLATE_LICENSE.txt). Optional integrations are referenced and are not bundled.
