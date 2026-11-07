# Homecoming 0.2.0-alpha.1

An experimental catch-up journal for Minecraft **1.21.1 / NeoForge 21.1**. Return to an existing world, see shared progress and pick up an available quest without maintaining another quest book. No AI service or API key is required.

## Install

Use Java 21 and NeoForge **21.1.262 or newer within 21.1**. Put `homecoming-0.2.0-alpha.1.jar` in the server and client `mods` folders for the native journal. A server-only installation falls back to chat. Start with a disposable world or a backed-up copy.

Run `/recap enable` to enroll in future updates, then `/recap` to open the journal. Closing it preserves unread updates; **Mark these read** acknowledges only the displayed range. `/recap share <name>` shares your current dimension and coordinates with enrolled current teammates. Settings can mute return prompts. `/recap disable` removes your recipient access to saved entries.

Optional integrations are not bundled. The dedicated runtime baseline is:

| Integration | Version |
| --- | --- |
| FTB Quests (NeoForge) | **2101.1.37** (required exact version when installed) |
| FTB Teams (NeoForge) | 2101.1.9 |
| FTB Library (NeoForge) | 2101.1.36 |
| Architectury (NeoForge) | 13.0.8 |
| Xaero's Minimap (optional client adapter) | **26.6.0** |
| XaeroLib | 1.7.3, bundled in the official minimap distribution |

The native journal offers Updates, Next steps and Settings. Related consecutive quest completions group into rows. Suggestions favor an available started quest and then available work in an active chapter. Opening a suggestion uses the existing quest book; Homecoming does not claim rewards or change progression.

The world server config controls prompt interval, retention, history capacity, page size and grouping interval. Defaults are 24 hours away, 30 days of history, 2,000 facts, five rows and a ten-minute grouping window.

## Verified for this alpha

- Eleven model tests pass, including audience isolation, acknowledgment boundaries, opt-out, retention, serialization, grouping and membership cutoffs.
- Four dedicated NeoForge GameTests pass with the pinned FTB integrations. They exercise actual task completion, hidden/reset filtering, offline recipients, suggestion priority, grouping, actual party leave/rejoin events and SavedData across process restarts.
- A real client opened the journal at GUI scales 1 and 2. Enrollment, prompt muting and acknowledgment round-tripped through the server.
- Real-client Xaero checks created a saved Overworld waypoint, rejected a different-dimension location, preserved a personal marker and kept the shared marker unique after a client restart.
- Compilation and runtime checks cover NeoForge 21.1.262 and 21.1.265. Normal builds exclude the development checks and their fixtures.

## Outstanding validation and limits

This is an alpha for testing. Connected two-client multiplayer and actual friend-group feedback over return visits are outstanding. The selected full-pack candidate is **FTB Skies 2 1.25.0**; its files and dependency versions are pinned, but full-pack gameplay compatibility is not yet established. Do not treat download verification or standalone integration tests as full-pack certification. See the current [delivery checklist](https://github.com/Cookie-Cat21/homecoming/blob/main/docs/implementation-status.md).

The Xaero adapter requires the target dimension to be active before tracking. It uses a dedicated Homecoming Shared set and selects that set on explicit tracking. Seen or copied coordinates cannot be revoked. No automatic private-location discovery, raw-chat collection, reward claiming, economy changes or gameplay automation is included.

History begins after installation/enrollment and follows ordinary world saves; abrupt crashes can lose updates since the last save. Keep normal world backups. Completed quests that are now hidden, reset or deleted are filtered out. Membership cutoffs prevent old history reappearing after a leave/rejoin cycle. Opting out removes a player's recipient access while retaining teammates' shared history.

Please use the [friend-group pilot guide](https://github.com/Cookie-Cat21/homecoming/blob/main/docs/friend-group-pilot.md) to test the return flow and report the first unexpected behavior with exact mod/loader versions. Real participant feedback remains necessary before the pilot can be called complete.
