# Homecoming — first-version scope

Working name only. Prepared 6 November 2026. Status: first implementation created; see README for validation.

**Implementation target confirmed:** Minecraft **1.21.1**, **NeoForge 21.1**, **Java 21**. Initial code is in `homecoming/`; its README records the actual implemented subset and validation limits. Earlier Paper/Java 25 assumptions are superseded.

**Scope revision:** The integration-first plan below supersedes the original manual-project-first MVP and default Paper platform recommendation. Earlier sections remain useful background, but the first pilot should read an existing modpack's progress rather than ask players to maintain another project system.

## Revised direction: reuse the systems players already use

User priority: Homecoming must not become another chore. Success means installing/configuring the addon once, playing normally, and receiving a useful return briefing without daily reporting, duplicate teams or a second quest book.

### Recommended first pilot

Choose one actual quest-driven pack containing compatible FTB Quests and FTB Teams builds, then implement the corresponding loader/version addon. Pin the pack release and all integration versions. The exact pack has not yet been chosen; there is no verified universal 26.3 modpack target. Keep the recommendation conditional on that compatibility check. Do not promise that a Paper plugin can load Forge/NeoForge/Fabric mods.

First integrations:

1. **FTB Quests:** consume authoritative start/completion/reset state as available on the pinned version; import titles, identifiers, descriptions, dependencies and visibility using its supported code/API. Current upstream source posts quest progress events: [Quest.java](https://github.com/FTBTeam/FTB-Quests/blob/main/common/src/main/java/dev/ftb/mods/ftbquests/quest/Quest.java). Main-branch evidence establishes feasibility, not compatibility with every released version.
2. **FTB Teams:** use the existing team and relevant membership history to choose whose updates to show, without asking players to form another group. [FTB Teams](https://github.com/FTBTeam/FTB-Teams) explicitly supports shared quest progression. Team completion must not be narrated as personal participation by an offline player. Do not backfill another team's private history after a membership change.
3. **Vanilla milestones:** record selected advancement/dimension milestones on the same server. Deduplicate events already represented by a quest and collapse routine events.
4. **KubeJS bridge, when present:** let pack authors emit explicit semantic milestones from their existing scripts. [FTB XMod Compat documentation](https://kube.wiki/wiki/addons/ftb-xmod-compat) includes quest completion, team and chunk hooks. Event names vary by release; verify them on the pinned pack. Pack authors configure these once; ordinary players do not write scripts.

The pilot reads existing state; it does not claim rewards, complete quests, alter the economy or change progression. Existing quest screens remain the source interface. Open the corresponding quest if a supported client API is available; otherwise display its chapter/title and a practical route to it. Rich mod-screen navigation may require a client addon and should not be promised as server-only functionality.

### First return briefing

Illustrative output, only when corresponding facts exist:

> Your team finished the Basic Power chapter while you were away.
> Maya completed the Nether Exploration milestone.
> You last worked on the Storage chapter. Here are two currently available quests to pick up.

Chapter completion is a fact from FTB Quests. “Basic Power” is an example pack-defined title. Do not infer that a machine was physically built or is operating from quest completion unless the pack's task proves that state. Suggestions must account for quest dependency, repeatable/reset status, hidden/spoiler rules and reward state separately. If the pack shares all quest progress, prioritize context and optional shared work; do not manufacture a personal backlog of already completed quests.

### Integration roadmap and limits

**Location integration refinement: Xaero's Minimap / World Map.** Prefer the player's existing in-game waypoint display for the first modpack pilot, with BlueMap as a later optional web view. Xaero's officially documents waypoint sets, in-world markers and chat sharing, plus developer dependency setup: [official project page](https://www.curseforge.com/minecraft/mc-mods/xaeros-minimap). Do not treat its developer dependency instructions as a promise of a stable waypoint API; inspect the pinned version before selecting an integration method.

Keep Homecoming's authorized shared locations on the server and use an optional client adapter to offer “Track location” from a briefing. The adapter can add/update a waypoint in a dedicated Homecoming set, with explicit stable server/world/dimension IDs, and leave personal sets alone. Actual ability to select a destination or open a particular map screen must be verified. Without the adapter, show the shared name, dimension and coordinates. A server plugin alone cannot inspect a player's local waypoint database.

Players can explicitly share an existing personal waypoint once; do not upload personal waypoints or explored map tiles automatically. Import only permitted shared locations, remove managed markers when access is revoked where technically possible, and explain that already viewed coordinates cannot be made secret again. A waypoint provides location/name evidence, not proof of project completion, a working machine, a safe route or teleport permission.

Existing [Server Waypoint](https://github.com/2676959/server_waypoint) advertises shared lists and Xaero synchronization. Evaluate its exact version, interfaces, audience controls and replacement behavior before reusing it rather than creating a second synchronization system. Its existence establishes feasibility, not a verified Homecoming integration. First location acceptance test: a teammate shares one location, the returning authorized player chooses Track, the correct dimension/world marker appears, and private personal markers remain untouched.

| Integration | Benefit | Stage / limits |
|---|---|---|
| FTB Quests + Teams | Automatic milestones, shared context and relevant existing goals | First pilot; exact pack APIs must be verified |
| KubeJS | Pack-specific meaning such as a declared factory unlock | Early if present; authored scripts, not generic machine understanding |
| BlueMap | Reuse explicitly shared place names and link to map locations | Later; its [marker API](https://bluemap.bluecolored.de/wiki/customization/Markers.html) supports integrations. Marker visibility is not authorization; import only approved sets and do not publish private places to a public map |
| MineColonies | Potential building upgrades and colony needs | Later candidate; inspect APIs/version support before promising events or request access |
| AE2 storage | Potential confirmation of available shared materials | Later; [AE2's API](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/main/API.md) exposes storage services, but network identity, authorization, power and loaded state complicate access. Never scan or expose all networks globally |
| Create | Potential meaningful factory activity | Later; a placed block is insufficient to prove a working factory. Prefer pack quests or explicit scripts first |
| CoreProtect on Paper | Aggregate recent work near authorized shared places | Separate Paper pilot; [lookup API](https://docs.coreprotect.net/api/version/v13/) exists. An observation should say “recent building activity,” not “completed storage room” |
| Discord | Optional delivery channel | Deferred; no new source of truth and no required posting workflow |

### Adapter architecture

Keep a platform-independent briefing model plus small version-specific adapters. Proposed facts contain source namespace, source object ID, source event ID/revision, actor if known, event time, audience/team ID, world ID, event type, label, evidence reference and spoiler/visibility metadata. Persist stable source IDs and the event-time membership needed to interpret them.

Prefer source events over repetitive polling. Bootstrap a baseline on install, then record changes; current completion state alone cannot establish when or by whom a quest was completed before tracking started. Missing/unloaded integrations produce an unavailable status, not invented progress. Deduplicate by source event and suppress repeated reset/completion loops. Revalidate permissions and current quest availability when displaying a cached briefing or opening a linked action.

Store only the facts needed to explain a change, not a second editable copy of every quest. Respect integration licenses and pack distribution rules; start with a separate addon and setup instructions. Implement one loader and one pack first, rather than a hybrid Paper/mod server.

### Revised MVP and acceptance gate

Replace the initial project commands with auto-detected integrations, a short briefing, open-source-detail actions, optional pin/dismiss controls and preferences. Manual projects may return later as a fallback for freeform builds, never a requirement for useful recaps.

First demonstration: A logs off; B completes a team quest; the server restarts; A returns to an accurate team update, with no implication that A personally did the task. Show an actually available next quest when one exists. Verify hidden quests stay hidden, outsiders cannot see team history, resets are reflected, duplicate events collapse and provider failure still produces a factual template.

Before expanding, play normally for a few sessions and check whether the briefings stay useful with zero project bookkeeping. If they do not, improve filtering/source interpretation before adding more integrations.

The earlier timeline is a baseline only. Allow approximately 2–4 focused weeks for a narrow one-pack pilot including integration fixes and testing; pack/version/client requirements can change that substantially. Broader loader/version compatibility needs its own estimate after the first adapter works.

## Product promise

When a player returns to a small survival multiplayer server after a break, give them a short, accurate briefing about relevant shared changes and one or two useful things they can choose to do next.

Intended initial audience: an opt-in friend-group SMP of roughly 3–15 players, one server and one shared world group. This is a design assumption, not a measured market segment. The goal is to help someone reconnect with their friends' world. Retention is an outcome to observe, not a promise.

## First experience

After at least 24 hours away (configurable), show one quiet chat message: “Welcome back. Three shared updates since your last visit. [Catch up]”. Do not interrupt movement or open a forced screen. `/recap` opens the same briefing at any time, paginated in chat for the first version.

Example, based on recorded facts:

> The group completed the Storage Hall project. Maya marked it done yesterday.
> Two members of your group earned the Enter the End advancement.
> The Riverside Bridge project still needs 48 oak logs. You pledged to help with it.
> [View project] [Show shared location] [Dismiss]

Opening a recap must not erase unread updates. Track a separate explicit acknowledgment watermark. Reopening the same interval remains possible. Avoid repeating automatic prompts within the same session. A first-time player gets a short orientation using public places and projects, not a fabricated history.

## Version-one features

1. Player sessions: UUID identity, joins/quits, last-seen time, explicit recap acknowledgment, preferences and group membership.
2. A filtered event history: selected advancement/milestone events, project changes and shared place announcements. Deduplicate global milestones; distinguish individual and group achievements.
3. Shared places: players explicitly register a name, dimension, position and visibility. Do not infer or publish private base locations. Place ownership and membership determine who may view a location.
4. Small project system: title, short description, optional shared location, open/paused/completed/archived status, optional material targets and volunteer membership. Owners or authorized collaborators mark completion. No automatic aesthetic/completion judgment.
5. Project progress: manual reporting only in the first slice. Display target and owner-reported remaining count with a timestamp. Never imply that a pledge is a physical delivery. Item escrow and automatic inventory/chest accounting are separate later work.
6. Per-player briefing: relevant changes since their acknowledged point, maximum five entries, dates and provenance, plus at most two optional project suggestions.
7. Controls: opt in/out, quiet mode, archive/forget supported personal records, admin retention settings, bounded storage and event queues.

## Initial commands

Player-facing syntax is provisional. Keep most actions clickable so players do not need to memorize these.

- `/recap` — briefing, with actions to acknowledge, browse projects and change preferences.
- `/recap settings` — tracking enrollment and return-prompt preference.
- `/place share <name>` — explicitly share the current position with the group.
- `/project create <name>` — create a project; description, visibility and material target can be edited through guided prompts.
- `/project list` and `/project view <id>`.
- `/project join <id>` — volunteer, without claiming completion or material delivery.
- `/project progress <id>` — authorized owner updates remaining quantities and status.
- `/project complete <id>` — authorized manual completion.

Permission design: members may read their own/group-authorized updates, owners manage their places/projects, explicit collaborators may edit project progress, administrators configure the plugin. Recheck visibility when rendering cached summaries or resolving click actions.

## How it knows things

Use structured server events and player declarations as source facts. Every briefing entry links to a stable fact ID, time and source type.

| Statement | Required evidence |
|---|---|
| “Storage Hall is complete” | Authorized player marked the named project complete; attribute that claim to the player |
| “Two group members entered the End” | Selected advancement events for those players; only if sharing is enabled |
| “The bridge needs 48 logs” | Latest authorized project progress report, shown with its timestamp |
| “This activity may suit you” | You joined the project, it is open and visible, and its owner marked it available to helpers |

Do not infer that a stronghold has been found from entering the End, infer a named room from block types, or say that an area is safe from limited observations. Registration starts the history: no retroactive reconstruction of unrecorded offline events.

The first recommendation engine is deterministic: prioritize joined projects, then shared projects seeking help; prefer recent confirmed updates, suppress stale/paused/complete work, and allow a player to hide a suggestion. Inventory-based suitability and time-to-complete estimates are deferred until their assumptions can be tested.

## Technical plan

Recommended default: a standalone Paper plugin for Minecraft Java 26.3, Java 25, Gradle, SQLite, and Paper's text/component interfaces. Pin one tested Paper build when implementation begins. The current Paper project setup documents 26.3 and Java 25: [project setup](https://docs.papermc.io/paper/dev/project-setup/) and [runtime requirements](https://docs.papermc.io/paper/getting-started/).

This requires owner/admin access to a server capable of installing the plugin. It cannot be installed by a normal player on an arbitrary public server or directly into a hosted Realm. If the actual target is an existing Fabric server, build a Fabric server mod instead; do not migrate a world merely to fit this recommendation. The data/briefing design can stay similar, but loader adapters and release artifacts differ. Start with one platform.

Pipeline:

server events + authorized project/place changes -> immutable fact records -> visibility/relevance filter -> deterministic briefing -> optional text rewrite -> chat interface

SQLite entities: players, sessions, groups, memberships, facts, places, projects, project_members, material_targets, progress_reports, recap_receipts, preferences, schema_version. All references use UUIDs/IDs, not player names. Project updates are transactional. Read times use UTC; display elapsed time/local formatting to players. Expired raw history produces an honest limited-history notice; retain current project state separately.

Read game state on its permitted server thread, copy the minimal data needed, then write/query the database asynchronously through a bounded worker queue. Never perform network requests on the tick thread. Do not poll every block, scan the whole world, log movement continuously or watch every inventory slot. Paper documents event observation/cancellation and scheduling constraints: [listeners](https://docs.papermc.io/paper/dev/event-listeners/), [scheduling](https://docs.papermc.io/paper/dev/scheduler/), [databases](https://docs.papermc.io/paper/dev/using-databases/).

Optional Bedrock access through an existing cross-play setup is a later compatibility check, not guaranteed by using chat alone. Folia and proxy-wide networks are outside the first release.

## Where AI fits

The entire first version works without an AI provider. Templates can produce an accurate and readable recap from a small number of facts.

Optional second-stage AI: send only authorized structured facts to a configurable text-generation provider; request short wording with cited fact IDs. Validate references, numbers and locations; reject unsupported content and fall back to templates. Provider text cannot execute commands, select hidden data, alter project progress or award items. Treat project descriptions as untrusted content. Keep all permission checks before provider calls and after cache reads. Keep keys server-side and out of logs.

Allow one cached rewrite for a player/interval/visibility revision, strict timeouts and output limits, explicit admin provider enrollment, and no chat-log collection. Usage can be measured as generated briefings times average input/output tokens. No defensible dollar forecast exists before choosing a model and measuring payloads. No API key or subscription is required for the local pilot.

## Required resources

- Minecraft Java client/account for manual testing; a separate friend/tester is useful for the two-player scenario. No bot account is needed for the product.
- Local or private test server with permission to install plugins. Local hosting is sufficient initially; dedicated hosting is optional.
- Java 25 JDK and Gradle wrapper. Environment inspection found Java 8 on the current PATH; verify other installed JDKs or install/configure a project-local toolchain before building. Node 22 is available but not required for this plugin.
- A disposable test world. Use a copy of a real SMP only for later integration testing.
- A few testers who actually take breaks between sessions, with permission to record the limited shared activity used by the pilot.
- Optional API credentials only when testing AI wording.

## Delivery slices and rough effort

These are engineering estimates for focused work, not promised dates. They assume one platform, limited UI and no existing-plugin integration.

| Slice | Deliverable | Rough effort |
|---|---|---|
| A | Plugin builds/loads; sessions and one milestone persist across restart; `/recap` shows the right interval | 2–3 working days |
| B | Named shared places, project creation/status/progress, group visibility and meaningful briefing | 3–5 working days |
| C | Permissions, retention, failure handling, deduplication and performance checks | 2–4 working days |
| D | Optional AI wording with validation, caching and fallback | 1–3 additional working days |

A small playable pilot is plausibly about 1–2 focused weeks; a dependable beta is more reasonably 2–4 weeks once iteration is included. Validate on a real small SMP over at least a couple of return visits; wall-clock pilot learning cannot be compressed into a single coding session.

## Acceptance scenarios

- Player A leaves; player B completes a visible project and earns a tracked milestone; after a restart, A returns and sees both once, with accurate actors/timestamps.
- A project/place visible only to another group never appears in A's recap, navigation action, generated summary or cache.
- Viewing and acknowledging are separate; retrying a failed request cannot silently consume updates.
- Paused/completed/stale projects do not appear as fresh available tasks. Manual progress is labeled as reported, with a timestamp.
- Canceled server actions do not become successful milestones; high-volume duplicate activity cannot dominate the recap.
- An empty interval produces a short honest message. Retention gaps are disclosed. No history is invented for events before installation.
- Database errors and queue overflow are reported and bounded without blocking the tick loop or crashing the server. Orderly shutdown drains pending writes; crash loss bounds are documented.
- If AI is enabled, timeout, malformed responses, invented facts and provider failure all return a valid deterministic recap.

## Success measures

Track with tester agreement: factual corrections, relevance ratings, whether a returning player understands what changed, whether they choose a suggested project, whether they find the prompts annoying, and owner time required to keep project state current. Do not equate clicks or increased playtime with usefulness. If players ignore project updates, reduce required manual input before expanding AI.

## Deferred scope

Autonomous gameplay/Mindcraft integration; automatic build recognition; chest search; item escrow; XP catch-up; equipment giveaways; quest generation; player-chat surveillance; Discord bot; web dashboard; multi-server syncing; every Minecraft version/loader; guaranteed retention. Each adds a separate product or compatibility burden.

## First implementation milestone

Prove one complete scenario with two players: A leaves, B marks a named shared project done, the server restarts, A returns and receives an accurate persisted briefing. That is the smallest demonstration of the product promise.

## Inputs to confirm before connecting to a real world

Actual server platform/version, whether the owner permits installation, and whether it is a new server or an existing friend-group SMP. Local implementation can start with the Paper 26.3 assumption; live integration depends on those facts.
