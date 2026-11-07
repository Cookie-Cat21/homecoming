# Homecoming UI and engineering flows

These diagrams cover the implemented journal and its recovery paths. Personal settings belong to the player; world rules belong to the server. The mod never completes quests or claims rewards through this interface.

## 1. Entry, enrollment and navigation

```mermaid
flowchart TD
    Join[Join world] --> Enrolled{Enrolled?}
    Enrolled -->|No| Invite[Optional enable invitation]
    Enrolled -->|Yes, reminder interval reached| Reminder[Catch-up invitation unless muted]
    Enrolled -->|Yes, no reminder needed| Play[Continue normal play]
    Invite --> Open[Open /recap]
    Reminder --> Open
    Manual[Player opens /recap anytime] --> Open
    Open --> Channel{Matching Homecoming client channel?}
    Channel -->|No| Chat[Chat briefing and command actions]
    Channel -->|Yes| Server[Server builds authorized view]
    Server --> Active{Participation enabled?}
    Active -->|No| Intro[Explain future recording + Enable button]
    Intro --> Enable[Authenticated enable request]
    Enable --> Server
    Active -->|Yes| Journal[Journal]
    Journal --> Updates[Updates]
    Journal --> Next[Next steps]
    Journal --> Settings[Settings]
    Journal --> Close[Done or Escape]
    Close --> Preserve[Keep unread state]
```

## 2. Updates, pages and acknowledgment

```mermaid
flowchart TD
    Facts[Server journal facts] --> Audience[Require event-time recipient + current team + membership cutoff]
    Audience --> Source[Recheck source visibility and completion]
    Source --> Group[Group consecutive chapter events]
    Group --> Page[Select configured row count]
    Page --> Offer[Save last displayed event ID as offered cursor]
    Offer --> Rows{Any rows?}
    Rows -->|No| Empty[You are caught up]
    Rows -->|Yes| Read[Read entries; scroll if needed]
    Read --> Place[Track or copy shared location]
    Read --> Ack[Mark displayed range read]
    Ack --> Validate[Server validates authenticated player and offered bound]
    Validate --> Save[Advance acknowledgment; mark SavedData dirty]
    Save --> Rebuild[Return next authorized view]
    Rebuild --> Rows
    Read --> Exit[Close without acknowledgment]
    Exit --> Unread[Entries remain unread]
    Expired[Older history expired] --> Notice[Retention notice; never fabricate missing history]
    Notice --> Read
```

## 3. Next steps and quest navigation

```mermaid
flowchart TD
    FTB{Server FTB integration available?} -->|No| Missing[Explain existing quest integration is unavailable]
    FTB -->|Yes| Filter[Visible quest and chapter; incomplete; tasks can start]
    Filter --> Rank[Started available quest, then active chapter, then stable ID]
    Rank --> Two[At most two suggestions]
    Two --> Any{Any suggestions?}
    Any -->|No| Empty[No available suggestions; continue normal play]
    Any -->|Yes| Cards[Chapter + existing quest + Open quest]
    Cards --> Client{FTB Quests installed on client?}
    Client -->|No| Fallback[Explain how to find the quest in its existing book]
    Client -->|Yes| Book[Open existing quest object]
    Book --> Play[Player chooses normal gameplay]
    Book -->|Unavailable or API error| Fallback
```

## 4. Settings, consent and authority

```mermaid
flowchart TD
    Settings[Settings page] --> Reminders[Return reminders: On or Muted]
    Reminders --> Request[Send personal preference request]
    Request --> Server[Server saves preference and returns view]
    Server --> Result[Show confirmed state]
    Settings --> Participation{Participation enabled?}
    Participation -->|No| Enable[Enable future recording]
    Participation -->|Yes| Off[Turn off...]
    Off --> Confirm[Explain access removal and teammates' retained history]
    Confirm -->|Keep enabled or Escape| Settings
    Confirm -->|Turn off| Disable[Authenticated disable request]
    Disable --> Remove[Remove player from stored recipient audiences]
    Remove --> Reply[Return disabled view]
    Settings --> Rules[World rules: retention, prompts, page size, grouping]
    Rules --> ReadOnly[Read-only: managed by world owner]
    Settings --> Tools[Quest book and map availability]
    Tools --> Explain[Describe available action or fallback]
```

## 5. Location and map safety

```mermaid
flowchart TD
    Share[Run /recap share name] --> Validate[Server validates name and uses actual player position]
    Validate --> Audience[Snapshot enrolled current teammates]
    Audience --> Place
    Place[Authorized shared place] --> Choice{Xaero installed?}
    Choice -->|No| Copy[Copy dimension and coordinates]
    Choice -->|Yes| Track[Track in Xaero]
    Track --> Dim{Player in target dimension?}
    Dim -->|No| Explain[Explain required dimension; keep Copy available]
    Dim -->|Yes| Ready{Xaero session and current map world ready?}
    Ready -->|No| Retry[Loading explanation; try again]
    Ready -->|Yes| Set[Use Homecoming Shared set in current world]
    Set --> Existing{Persistent event symbol + coordinates already present?}
    Existing -->|Yes| Keep[Keep one waypoint]
    Existing -->|No| Add[Add shared waypoint]
    Keep --> Save[Save current map world]
    Add --> Save
    Save --> Feedback[Show result; personal waypoint sets untouched]
    Save -->|Save failed| Session[Explain session-only result]
    Track -->|Integration failure| Fallback[Copy coordinates and explain fallback]
```

## 6. Client/server request lifecycle and recovery

```mermaid
sequenceDiagram
    actor Player
    participant UI as Journal screen
    participant Net as Optional versioned payload channel
    participant Server as Server main thread
    participant Store as World SavedData
    Player->>UI: Enable / mute / mark read / disable / refresh
    UI->>UI: Pending feedback; block repeat mutation
    UI->>Net: Whitelisted action + offered event ID
    Net->>Server: Authenticated connection's player
    Server->>Server: Rate limit and action validation
    Server->>Store: Apply permitted player-specific mutation
    Store-->>Server: Current authoritative state
    Server->>Server: Reapply membership, visibility and page selection
    alt Response arrives
        Server-->>Net: Authorized view + read-only world rules
        Net-->>UI: Schedule on client main thread
        UI->>UI: Rebuild measured content; clear pending; confirm result
    else No response before timeout
        UI->>UI: Restore controls; explain retry
    end
```

## 7. Rendering and input layout

```mermaid
flowchart LR
    View[Authorized view + GUI-scaled viewport] --> Measure[Measure wrapped titles, details and actions]
    Measure --> Layout[Header + tabs + scroll viewport + fixed footer]
    Layout --> Cards[One shared card layout for draw and hit positions]
    Cards --> Clip[Scissor body; hide buttons outside viewport]
    Clip --> Input[Native focus, narration, Enter/Space, mouse wheel]
    Input --> Scroll[Clamp scroll and draw actual scrollbar]
    Resize[GUI scale or window resize] --> Measure
    Reply[Server response or tab switch] --> Measure
    Pending[Pending request / error / confirmation] --> Measure
```

## State and acceptance matrix

| State | User sees | Required behavior |
| --- | --- | --- |
| Not enrolled | Recording explanation and Enable | No retroactive import; server enrolls only this player |
| No unread entries | Caught-up entry | No mark-read action |
| Populated updates | Time, readable event, location action where relevant | Only authorized rows; close preserves unread state |
| Long title/history | Wrapped text, scroll thumb | Every action remains reachable at small GUI sizes |
| Suggestions unavailable | Clear empty/integration explanation | No invented quest or progression change |
| Pending mutation | Saving/refreshing status | No repeated mutation until reply or timeout |
| No reply | Retry explanation | Controls recover; no false saved message |
| Disable confirmation | Effect and explicit choices | Cancel preserves participation; confirm changes server state |
| Missing map/quest client mod | Copy/find-in-book fallback | Journal stays usable |
| Wrong dimension | Required dimension explanation | No waypoint added to wrong map |
| World rules | Actual server values, read-only label | Client cannot edit server policy |
| Keyboard or GUI resize | Visible focus and recomputed layout | No clipped reachable hit targets or icon-only mystery controls |
