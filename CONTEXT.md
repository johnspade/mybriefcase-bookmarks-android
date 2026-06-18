# MyBriefcase Bookmarks Android

Android client for the MyBriefcase Bookmarks system — a local-first bookmark manager using Automerge CRDTs synced over Syncthing.

## Language

**Client**:
A device installation identified by a persistent client_id; writes only to its own directory under the sync root.
_Avoid_: Node, peer, device

**Client ID**:
Stable string identifying this installation in the sync folder. Format: `<model>-MyBriefcaseBookmarks-<suffix>`. Generated once, persisted locally.
_Avoid_: Device ID, user ID

**Document**:
The single Automerge document holding all bookmark data for one logical store. Shared across all clients via the document_id in `.bookmarks-sync`.
_Avoid_: File, database, state

**Sync Root**:
The Syncthing-shared directory where client export files live. In v1.0, a hardcoded path on external storage.
_Avoid_: Data dir, sync folder

**Mutation**:
Any write to the Automerge document, always followed by export and cache re-hydration.
_Avoid_: Update, change, write

**Merge Poll**:
Kotlin-driven call to `trigger_full_merge()` that reads peer directories and incorporates their changes. Triggered on resume, by foreground timer (30s), and by background WorkManager (15min).
_Avoid_: File watcher, sync callback, push notification

**Export (sync)**:
Writing the local document state to a file in sync_root for other clients to merge.
_Avoid_: Save, persist, flush

**Onboarding Wizard**:
A first-launch horizontal pager guiding users to select a Syncthing shared folder via SAF directory picker. Shown when no persisted sync URI exists and no v1.0 path is detected. Skipped for users upgrading from v1.0 with an active external sync path.
_Avoid_: Setup flow, first-run screen

**SAF Directory Picker**:
`ACTION_OPEN_DOCUMENT_TREE` used solely for folder-selection UX. The resulting URI is resolved to a filesystem path for the Rust core. Actual file I/O uses `MANAGE_EXTERNAL_STORAGE`, not the SAF content provider. Limited to primary storage; unresolvable URIs show a clear error.
_Avoid_: Content URI access, DocumentFile I/O

**Share Receiver**:
A standalone transparent-themed Activity (`ShareReceiverActivity`) that handles `ACTION_SEND` intents with `text/plain`. Shows a bottom sheet for folder selection and bookmark saving, then finishes. Redirects to the Onboarding Wizard if the app is not yet initialized.
_Avoid_: Share handler, share target

**Favicon**:
A site icon fetched on explicit user action (button in add/edit dialog), stored content-addressed in `sync_root/favicons/<sha256>.<ext>`. The filename is persisted on the bookmark in the Automerge document. Syncs across clients. Falls back to a letter avatar (first letter of domain) when absent.
_Avoid_: Icon, site image

**Bookmark History**:
An ordered list of changes to a single bookmark, derived from Automerge commit messages tagged with the bookmark ID. Each entry includes a change hash, timestamp, actor, and field diffs. Capped at 50 entries, newest first.
_Avoid_: Version history, changelog, audit log

**Revert**:
Writing old field values from a historical snapshot as a new Mutation. Identified by change hash, not numeric index. Preserves full history (not a rollback).
_Avoid_: Undo, rollback, restore
