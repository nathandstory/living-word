# Bible Study Tools Design

## Goal
Make the Bible item feel like a real study and listening tool by improving search results, adding notes, adding verse collections/playlists, and replacing the single audio button with clearer local playback controls.

## Approved Direction
The user approved the recommended next feature set: Bible reader polish plus study tools. The scope stays vanilla-adjacent and peaceful. No combat, magic, powers, or unrelated progression systems.

## UX Design
The Bible screen remains a parchment reader with compact controls. Top-level reader tabs switch the main content area between:

- `Reading`: current chapter text.
- `Search`: a scrollable list of search results with reference and verse snippet.
- `Highlighted`: saved highlighted verses.
- `Notes`: saved notes for verses.
- `Collections`: named verse lists that can act like simple playlists.

The tools row stays optional behind the existing Tools toggle. It gains concise actions for highlight, note, collection, copy, and local audio. The audio controls expose current state plainly: `Play`, `Stop`, `Prev`, `Next`, and `Queue`. The goal is to make behavior visible without covering the text.

## Data Model
Study state continues to live in `livingword/bible_state.json` through `BibleClientPreferences`. The stored state will add:

- `notes`: stable verse reference plus text.
- `collections`: named collections containing ordered verse references.
- `audioQueue`: ordered chapter references for local Bible playback.

The state layer owns deduplication and ordering. GUI rendering only reads lists and issues simple state commands.

## Architecture
Keep Bible UI state in `BibleGuiState`, persistence in `BibleClientPreferences`, and client playback in `LivingWordClient`. Add small value records for notes and collections rather than spreading parallel arrays through the GUI. Use the existing `BibleReference` stable IDs for storage.

Search still uses `BibleDataManager.search`, but the GUI should keep all results in state and render them as clickable rows. `Go` populates the Search tab; `Next` still cycles for keyboard-friendly behavior.

## Error Handling
Malformed saved notes or collections should be ignored rather than crashing the client. Empty notes delete the note. Missing verse data should show the reference with an empty snippet rather than failing the screen.

## Testing
Add tests before production code for:

- `BibleGuiState` notes, collections, search-result tab state, and audio queue behavior.
- `BibleClientPreferences` persistence of notes and collections.
- `BibleScreen` source contracts for Search/Notes/Collections views and audio controls.
- Layout contract that the new tab row and tool row do not overlap the reading area on compact screens.

