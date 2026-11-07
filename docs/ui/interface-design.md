# Homecoming interface specification

## Intent and visual language

The player has returned to a friend's ongoing world and wants to understand what changed, choose one useful activity and get back to playing. The selected direction is the catch-up journal. Reading must feel calm, optional and quick; no extra maintenance is required.

Domain: field journal, shared landmark, quest book, camp map, bookmark, party, return visit.

Color world: parchment `#E9E3D2`, fresh paper `#F5EFDF`, ink `#26332A`, spruce `#334C3D`, copper `#946B43`, map amber `#B48535`, muted pencil `#56604F`. Color expresses action, reading hierarchy and warnings.

Signature: a spruce bookmark in the journal header, with the player's current page and unread row count. Its language repeats in selected navigation, the milestone icon tile, the read acknowledgment and the caught-up state.

Defaults replaced: full-width gray buttons become compact labeled pixel-icon controls; a flat wall of text becomes measured journal entries; a technical settings dump becomes personal controls plus clearly read-only world rules.

## Component decisions

All components use Minecraft's native pixel font, four-pixel spacing increments, square corners and surface color shifts. Text remains at the native readable size. The player can use Tab/Shift+Tab and Enter/Space through native Button behavior; each icon retains a text label or an explicit tooltip/narration label. Focus is visible without relying on color alone.

| Component | Intent | Palette and depth | Surface / typography / spacing |
| --- | --- | --- | --- |
| Journal frame and navigation | Know where you are and switch pages quickly | Spruce bookmark, ink labels, parchment; quiet single-pixel separation | Native bold title, labeled pixel icons, 4px grid |
| Update and suggestion entries | Read one coherent change and take its existing action | Fresh-paper entries on parchment; spruce action emphasis | Wrapped titles, muted source/time, measured body/action height |
| Buttons and pixel icons | Make actions identifiable without decorative noise | Spruce primary, paper secondary, warning ink for turn-off | Consistent 10px icon family, native focus/narration, 20px controls |
| Settings and confirmation | Understand what you can change and what the world owner controls | Personal actions on paper; read-only rule labels with lock icon | Short explanations, separate on/off state, explicit destructive confirmation |
| Status and recovery | Explain saved, pending and failed actions in place | Pencil for normal status; amber warning with text | Wrapped feedback, timeout recovery, no silent acknowledgment |

## Layout and behavior

- Frame fits the current GUI-scaled viewport with 12px margins, up to 600 by 390 GUI pixels.
- Header names Homecoming and the player; tabs stay visible on every page.
- Body is scrollable independently of navigation and footer. Entries are measured once and rendered using those same positions. Partially clipped buttons cannot receive clicks.
- Footer contains one read-range action when applicable, an explicit refresh and a close action. Closing never marks updates read.
- A visible scrollbar and overflow count replace the old unconditional “Scroll for more” hint.
- Settings show participation, reminders, connected tools, and authoritative server rules. Retention/page size are read-only here; personal settings never pretend to change server policy.
- Labels and body text use regular weight without Minecraft's dark button-text shadow. Source quest titles retain translated wording but use the journal's readable ink styling. Icon/text groups stay together in navigation.
- Turn-off requires confirmation and is applied by the authenticated server. It explains recipient-access removal without claiming to erase teammates' records or revoke already seen coordinates.
- Pending server mutations disable repeat submissions, show feedback and recover after a timeout. The server reply replaces the view and confirms the resulting state.

## Scope

This implements the existing mod journal, not a web mockup. Screenshots are captured from Minecraft. Any populated presentation-only preview is explicitly identified as fixture content; separate captures exercise real server responses. Full-pack and real-player pilot gates remain independent of UI polish.
