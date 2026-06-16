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
