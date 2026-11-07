# Homecoming friend-group pilot

This checks whether returning to a shared world feels easier. Automated checks prove specific behavior; this pilot needs real people and at least two genuine return visits.

## Setup

Use a disposable world or a backed-up copy. Install the same Homecoming alpha on server and clients for the journal UI. Use Minecraft 1.21.1, Java 21 and NeoForge 21.1.262–21.1.x. The tested integration baseline is FTB Quests 2101.1.37, Teams 2101.1.9, Library 2101.1.36 and Architectury 13.0.8. The optional map adapter targets Xaero's Minimap 26.6.0 with its bundled XaeroLib 1.7.3.

The full-pack candidate is FTB Skies 2 1.25.0. Its release and integration pins are recorded in [the compatibility manifest](compatibility/ftb-skies-2-1.25.0.json). Manifest download verification does not establish runtime compatibility; use [the delivery checklist](implementation-status.md) for current evidence before installing into an existing pack.

Each participant runs `/recap enable`. Join the existing FTB party; Homecoming does not create another team. Enrollment records future shared progress only. `/recap disable` removes a player's recipient access to saved updates. Do not share places that teammates should not know about.

## Functional session

Use two teammates and, if available, one player in a different party.

1. Both teammates enable Homecoming. One logs out.
2. The remaining player completes two existing quests in one chapter and shares a useful location with `/recap share Community storage`.
3. Stop and restart the server normally. The returning player reconnects and opens `/recap`.
4. Confirm that the completed quests and shared location appear. Related completions should group. Team progress should be described as team progress, without claiming that the absent player participated.
5. Close and reopen the journal without acknowledging. The same updates should remain unread. Click **Mark these read**; the displayed range should disappear while later updates remain.
6. Open **Next steps** and follow **Open quest**. It should lead to an available existing quest. It must not complete tasks, claim rewards or change progression.
7. Track the shared location in Xaero, if installed. Confirm the coordinates and dimension. Click again, restart the client and try again: the same saved location should stay unique. Personal waypoint sets should remain intact. A different-dimension location should request entry to that dimension before tracking.
8. Have the other party open its recap. It must not receive the first party's facts. Leave and rejoin a party: history from the previous membership period must stay unavailable; new events should still arrive.
9. Mute return prompts in Settings. `/recap` should remain usable. To test a return prompt immediately, use the disposable world's server config to set `returnPromptHours` to 0, reconnect once, then restore the chosen interval. The default is 24 hours.

Record pass/fail and the first unexpected behavior for each step. Include exact Homecoming/loader/mod versions when reporting a failure. Screenshots of the journal and relevant coordinates are useful; raw chat history and private locations are not required.

## Actual return visits

After the functional session, play normally. Do not manufacture progress or keep an extra project board for Homecoming. Let at least one participant miss a normal session, then return; repeat across another return visit.

Ask the returning player immediately after each visit:

- Did the briefing explain the changes you cared about? Which useful change was missing?
- Did one of the existing-quest suggestions help you decide what to do next?
- Did anything feel repetitive, misleading, spoiler-heavy or like another chore?
- How long did it take to choose your next activity, and would you have opened this voluntarily?

Use a short record for each visit:

| Field | Observation |
| --- | --- |
| Date, pack release, Homecoming version | |
| Approximate time away | |
| Relevant update found / missed | |
| Suggested activity followed / ignored, and why | |
| Extra upkeep required | |
| Defect or privacy issue | |
| Player would use it again | |

The pilot is complete only when these observations come from real participants. Synthetic players, developer-written feedback and a successful launch do not fill these fields.
