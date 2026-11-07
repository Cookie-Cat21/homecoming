# Homecoming 0.2.0-alpha.2

This alpha rebuilds the journal around a readable field-journal layout. It runs inside Minecraft 1.21.1 on NeoForge 21.1.262 or newer within 21.1, with Java 21.

## Interface changes

- Consistent labeled pixel icons for Updates, Next steps, Settings, map tracking, copy, reminders, read acknowledgment and world rules.
- Readable ink on parchment: dark button shadows removed, regular-weight text and stronger contrast. Pack title wording is retained while its presentation is normalized for reading on paper.
- Measured, wrapping entries with a scroll indicator, fixed navigation/footer, visible keyboard focus and Home/End/PgUp/PgDn scrolling.
- Populated quest suggestions open the existing FTB quest screen; no reward claiming or progression changes.
- Personal reminders and participation controls are separate from actual, read-only server rules.
- Turn-off confirmation explains recipient-access removal and supports cancellation with Escape. Confirmed disable and enable are authenticated server actions.
- Pending mutations block repeat submissions, server replies show the authoritative state, and an eight-second no-response path restores controls with a retry explanation.

See the [seven UI and engineering flowcharts](https://github.com/Cookie-Cat21/homecoming/blob/main/docs/ui/ui-flows.md) and [interface specification](https://github.com/Cookie-Cat21/homecoming/blob/main/docs/ui/interface-design.md). Screenshots in `docs/ui/screenshots` come from the running game. The quest names and milestones in those captures are explicit fixtures in a disposable test world.

## Install and compatibility

Install `homecoming-0.2.0-alpha.2.jar` on **both server and clients** for the redesigned journal. Use matching alpha versions: this release uses payload protocol 3 and alpha.1 used protocol 2. Saved world journal data remains schema 1.

The optional integration baseline remains FTB Quests **2101.1.37**, FTB Teams 2101.1.9, FTB Library 2101.1.36 and Architectury 13.0.8. The map adapter targets Xaero's Minimap **26.6.0**, with XaeroLib 1.7.3 bundled in the official minimap distribution. These mods are not bundled with Homecoming.

Run `/recap enable`, then `/recap`. Share your current location with `/recap share <name>`. Closing the journal never marks entries read. History and prompt rules are configured by the world owner, not by ordinary clients.

## Validation and limits

The build passes eleven model tests and four dedicated-server GameTests for completion, visibility/reset filtering, membership periods, grouping/suggestions and persistence. Real-client flow checks cover populated entries, opening the source quest book, acknowledgment, reminder changes, confirmation/cancellation, disable/enable and compact GUI layout. Normal JARs exclude the preview/check fixtures.

This remains an experimental alpha. Full FTB Skies 2 1.25.0 gameplay validation, connected two-client return visits and real friend-group feedback remain outstanding. Prior full-pack launch attempts reached resource loading but encountered the machine's Windows memory commit limit. Standalone integration checks and UI screenshots do not establish full-pack compatibility. See the [delivery checklist](https://github.com/Cookie-Cat21/homecoming/blob/main/docs/implementation-status.md) and [pilot guide](https://github.com/Cookie-Cat21/homecoming/blob/main/docs/friend-group-pilot.md).

Use a disposable or backed-up world. History follows ordinary world saves; abrupt crashes can lose recent updates. Opting out removes your recipient access without deleting teammates' history or revoking already seen coordinates. Xaero tracking requires the target dimension to be active. No AI service, chat logging, chest scanning, generated quests or gameplay automation is included.
