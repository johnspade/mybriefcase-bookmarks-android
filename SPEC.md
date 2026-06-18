# SPEC: MyBriefcase Bookmarks Android — Local-First Bookmark Manager

**Version:** 0.1.0-draft
**Status:** Implementation-ready specification
**Architecture:** Kotlin/Jetpack Compose + Material 3 UI, Rust core via UniFFI, Syncthing file transport

---

## 1. Overview

This is a native Android client for the MyBriefcase Bookmarks system. It shares
the same Automerge-backed data model and Syncthing sync mechanism as the web
app, reusing the `mybriefcase-bookmarks-core` Rust crate via UniFFI bindings.

The Android app mirrors the web app's functionality: browsing a folder tree of
bookmarks, creating/editing/deleting bookmarks and folders, moving items between
folders, searching, sorting, and import/export via Netscape Bookmark HTML.

### 1.1 Design Principles

| Principle | Implementation |
|---|---|
| Local-first | All reads/writes go through the local Automerge document |
| Native feel | Material 3, edge-to-edge, dynamic color, adaptive layouts |
| Shared core | Rust `mybriefcase-bookmarks-core` via UniFFI — no logic duplication |
| Syncthing transport | Android Syncthing-Fork app syncs the shared folder |
| Offline-capable | Fully functional without network; sync happens when Syncthing runs |

### 1.2 Relationship to Web App

The Android app is a peer in the same sync network as the web app. It writes
to its own client directory (`<sync_root>/android/`) and reads from all peers.
The Automerge CRDT merge guarantees conflict-free convergence across all
clients regardless of edit ordering.

---

## 2. Architecture

### 2.1 Layer Diagram

```
┌─────────────────────────────────────────────┐
│           UI Layer (Jetpack Compose)         │
│  Material 3 · Navigation · Adaptive layout  │
├─────────────────────────────────────────────┤
│          ViewModel Layer (Kotlin)            │
│  State holders · Presentation · Coroutines  │
├─────────────────────────────────────────────┤
│         Repository Layer (Kotlin)           │
│  Bridges UniFFI ↔ ViewModel                 │
├─────────────────────────────────────────────┤
│         UniFFI Bindings (Generated)         │
│  Kotlin bindings ↔ JNI ↔ Rust cdylib       │
├─────────────────────────────────────────────┤
│      mybriefcase-bookmarks-core (Rust)      │
│  Automerge repo · ops · model · watcher     │
├─────────────────────────────────────────────┤
│          Filesystem (Android)               │
│  App-internal storage + SAF-accessible sync │
└─────────────────────────────────────────────┘
```

### 2.2 Responsibility Boundary

| Concern | Owner | Rationale |
|---|---|---|
| CRUD, tree traversal, search, filtering | Rust | Core logic, no duplication |
| Sorting | Rust | FFI accepts `sort_by` param; operates on full list |
| Merge, sync, file watching | Rust | Automerge operations stay in one layer |
| Import/export | Rust | Netscape HTML parsing is in the core crate |
| Date/time formatting | Kotlin | Locale-dependent; Android `DateTimeFormatter` |
| Grouping (folders above bookmarks) | Kotlin | Trivial presentation, avoids FFI churn |
| Intents (open, share, copy) | Kotlin | Android platform APIs |
| Navigation, back stack | Kotlin | Compose Navigation |

### 2.3 Rust FFI Layer

The `mybriefcase-bookmarks-ffi` crate wraps the core crate and exposes
functions via UniFFI. The FFI layer handles:

- Lifecycle management (init repo, shutdown)
- CRUD operations on bookmarks and folders
- Reading the folder tree structure
- Search
- Import/export
- File watching for sync events

All FFI calls that mutate state or perform I/O run on a Tokio runtime managed
within the Rust layer.

The FFI crate holds a module-level singleton (`OnceLock`) containing the Tokio
runtime, `RepoHandle`, `DocHandle`, `DocumentId`, and a cached
`RwLock<BookmarkStore>`. `init_repo` populates it, `shutdown` tears it down,
and all other FFI functions access it via the singleton. This avoids passing
opaque Rust handles across the FFI boundary.

The cached `BookmarkStore` is hydrated once at init and re-hydrated after every
local mutation and peer merge. Read-path FFI calls (`get_folder_children`,
`search_bookmarks`, etc.) read from the cache under a shared lock, making them
essentially free.

### 2.3 Initialization

The Rust repo is initialized synchronously in `Application.onCreate()`. This
is local filesystem I/O only (no network), expected <100ms. The Android splash
screen (via `androidx.core.splashscreen`) covers this latency. By the time any
Activity or ViewModel runs, the repo singleton is guaranteed ready.

### 2.4 Threading Model

```
Main thread (UI)
    │
    ▼
ViewModel (coroutines on Dispatchers.IO)
    │
    ▼
UniFFI call (blocks on Rust Tokio runtime)
    │
    └── Repo operations (async within Rust)
```

### 2.5 Sync Detection (Polling)

The Rust `notify`-based file watcher is **not used on Android**. inotify does
not reliably detect changes on `/storage/emulated/0/` (FUSE mount on API 30+),
and both Syncthing-android and DecSync apps abandoned this approach.

Instead, sync detection uses Kotlin-driven polling:

| Trigger | Mechanism | Scope |
|---|---|---|
| App open (cold start) | `init_repo` does full merge internally | Catches everything synced while dead |
| App resume | `trigger_full_merge()` called from `onResume` | Catches changes synced while backgrounded |
| Foreground polling | ViewModel coroutine, 30s interval | Near-real-time while app is visible |
| Background periodic | WorkManager `PeriodicWorkRequest`, 15min | Catch-up while app is not visible |

`trigger_full_merge()` returns `true` if changes were merged. When it returns
`true`, the ViewModel re-fetches data from the (already re-hydrated) cache.

---

## 3. Data Model

Identical to the web app — defined in `mybriefcase-bookmarks-core`. See the
web app SPEC.md §2 for the full schema. The Android app hydrates the same
Automerge document into the same `BookmarkStore` struct.

### 3.1 FFI Data Transfer Objects

```kotlin
// Detail views (full data)
data class BookmarkDto(
    val id: String,
    val url: String,
    val title: String,
    val notes: String,
    val createdAt: String,
    val updatedAt: String,
)

data class FolderDto(
    val id: String,
    val title: String,
    val children: List<String>,
    val createdAt: String,
    val updatedAt: String,
)

// List views (lightweight)
data class BookmarkItemDto(
    val id: String,
    val title: String,
    val url: String,
    val createdAt: String,
)

data class FolderItemDto(
    val id: String,
    val title: String,
    val itemCount: UInt,
)

// Navigation drawer tree
data class FolderNavDto(
    val id: String,
    val title: String,
    val itemCount: UInt,
    val childFolderIds: List<String>,
)

data class FolderNavTreeDto(
    val rootFolderId: String,
    val folders: List<FolderNavDto>,
)

// Breadcrumbs
data class BreadcrumbDto(
    val id: String,
    val title: String,
)

// Composite responses
data class FolderChildrenDto(
    val folderTitle: String,
    val breadcrumbs: List<BreadcrumbDto>,
    val folders: List<FolderItemDto>,
    val bookmarks: List<BookmarkItemDto>,
)

// Import result
data class ImportResult(
    val bookmarksImported: UInt,
    val foldersImported: UInt,
)

// Enums
enum class SortOrder {
    NAME_ASC,
    NAME_DESC,
    DATE_DESC,
    DATE_ASC,
}
```

---

## 4. Storage and Sync

### 4.1 Directory Layout

The sync root lives inside an existing Syncthing shared folder. The app only
needs its own internal storage for the local repo and actor ID.

```
<app_internal>/                          # Context.filesDir
├── actor_id                             # persistent Automerge ActorId
├── client_id                            # persistent client identity string
└── repo_store/                          # local Repo FsStore
    └── ...

<syncthing_shared_folder>/               # on shared storage, managed by Syncthing
├── .bookmarks-sync                      # shared document metadata
├── android/                             # this client's directory
│   ├── info.json
│   └── store/
└── <other_clients>/                     # peer directories (desktop, etc.)
    └── ...
```

### 4.2 Syncthing Integration (DecSync-style)

Like DecSync apps, this app does **not** own the Syncthing folder. Instead,
the user already has Syncthing-Fork running with a shared folder, and the app
reads/writes a directory within it. The app never configures Syncthing itself.

**v1.0 (hardcoded path):** The sync root is
`/storage/emulated/0/Syncthing/mybriefcase_bookmarks`. This avoids SAF
complexity for the initial version and is sufficient for testing. The path is
a build-time constant. Requires `MANAGE_EXTERNAL_STORAGE` permission on
API 30+ (acceptable for v1.0; replaced by SAF in v1.1).

**v1.1 (wizard + SAF):** A wizard-style onboarding flow (like DecSync CC's
AppIntro) will let the user pick or create a directory inside their Syncthing
shared folder via `ACTION_OPEN_DOCUMENT_TREE`. The selected URI is persisted
with `takePersistableUriPermission()`.

### 4.3 SAF and Rust Filesystem Access

The Rust core operates on real filesystem paths (`std::fs`). SAF exposes
content URIs, not paths. The approach for v1.1:

1. User picks a directory via SAF → we get a content URI
2. Resolve the underlying filesystem path from the URI (works for primary
   external storage on most devices via `DocumentsContract` path extraction)
3. Pass that path to the Rust core

If path resolution fails (e.g. SD card or unusual storage providers), fall
back to an error message directing the user to pick a directory on primary
storage. This is the same limitation DecSync apps have in practice.

For v1.0, this is a non-issue since the path is hardcoded.

### 4.4 Client Identity

Format: `<device_model>-MyBriefcaseBookmarks-<suffix>`.

- `device_model`: `android.os.Build.MODEL` with spaces replaced by hyphens
- `suffix`: 4 random hex characters, generated once on first launch

Example: `Pixel-7-MyBriefcaseBookmarks-a3f2`.

The full client_id string is persisted at `<app_internal>/client_id`. On first
launch it is generated and written; on subsequent launches it is read as-is.
Never regenerated — this prevents orphaning the client's sync directory. A
factory reset (which wipes app-internal storage) correctly creates a new
identity since the actor_id is also lost.

### 4.5 Sync Flow

1. **Outbound**: After each local mutation, the Rust core exports chunks to
   `<sync_root>/<client_id>/store/`. Syncthing propagates these files to peers.
2. **Inbound**: Kotlin calls `trigger_full_merge()` which reads all peer
   directories, merges new data, and re-hydrates the cache. Returns whether
   anything changed so the ViewModel knows to re-fetch UI data.

---

## 5. UI Design

### 5.1 Navigation Structure

The app uses a single-activity architecture with Jetpack Compose Navigation.

```
┌─────────────────────────────────────┐
│  Top App Bar                        │
│  [≡ Drawer] Title     [🔍] [⋮]     │
├─────────────────────────────────────┤
│                                     │
│  Content Area                       │
│  (Folder contents / Search results) │
│                                     │
├─────────────────────────────────────┤
│  (Bottom sheet for detail/edit)     │
└─────────────────────────────────────┘
```

**Navigation drawer**: Folder tree (mirrors web app sidebar)
**Content area**: List of sub-folders and bookmarks for the current folder
**Bottom sheet / Dialog**: Bookmark detail, edit form, move picker

**Back stack behavior**: Tapping a sub-folder pushes onto the stack (back
returns to parent). Selecting a folder from the navigation drawer clears the
stack and rebuilds it from the folder hierarchy (back navigates up the tree
toward root, then exits). This matches standard drawer-based navigation —
the drawer is a "jump to" action that resets context.

### 5.2 Screens

| Screen | Description |
|---|---|
| Folder view | Main screen. Shows breadcrumbs, sub-folders, and bookmarks for a folder |
| Bookmark detail | Bottom sheet showing title, URL, notes, dates |
| Add bookmark | Dialog with URL (required) and title (optional, defaults to URL), folder picker |
| Edit bookmark | Dialog with editable URL, title, notes, folder picker |
| Add folder | Dialog with title field |
| Move item | Dialog with folder tree picker |
| Search | Full-screen search with results list |
| Import/Export | Settings sub-screen for file operations |
| Settings | Sync directory config, client ID, about |

### 5.3 Folder View (Main Screen)

The primary screen displays the contents of the currently selected folder:

- **Breadcrumb bar**: Tappable path showing `Root > Parent > Current`
- **Sort control**: Dropdown/chip for Name A-Z, Name Z-A, Date newest, Date oldest
- **Sub-folders section**: Cards or list items with folder icon, title, item count
- **Bookmarks section**: List items with domain favicon/letter, title, URL, date

Tapping a sub-folder navigates into it. Tapping a bookmark opens the detail
bottom sheet. Long-pressing shows a context menu (edit, move, delete).

### 5.4 Folder Tree (Navigation Drawer)

A collapsible tree in the navigation drawer, matching the web app sidebar:

- Indented folders with expand/collapse chevrons
- Bookmark count badge per folder
- Current folder highlighted
- Tap navigates to that folder

### 5.5 Search

Triggered from the top app bar search icon:

- Full-text search across bookmark title, URL, and notes
- Results displayed as a flat bookmark list (same layout as folder view)
- Search is local and instant (no network)

### 5.6 Bookmark Actions

| Action | Trigger | Implementation |
|---|---|---|
| Open in browser | Tap URL in detail sheet | `ACTION_VIEW` intent |
| Copy URL | Long-press URL or menu item | Clipboard manager |
| Edit | Detail sheet edit button or context menu | Edit dialog |
| Move | Context menu or detail sheet | Folder picker dialog |
| Delete | Context menu with confirmation | Soft-delete via core |
| Share | Share button in detail sheet | `ACTION_SEND` intent |

### 5.7 Material 3 Theming

- Dynamic color (Material You) on Android 12+
- Dark/light mode following system setting
- Edge-to-edge with proper inset handling
- Standard Material 3 components: TopAppBar, NavigationDrawer, ListItem,
  BottomSheet, FloatingActionButton, AlertDialog, SearchBar

### 5.8 FAB (Floating Action Button)

A FAB on the main folder view with an "Add" icon. Tapping it shows a menu
to add a bookmark or add a sub-folder to the current folder.

---

## 6. FFI Interface

### 6.1 Exposed Functions

```
// Lifecycle
init_repo(data_dir: String, sync_dir: String, client_id: String)
shutdown()

// Read
get_folder_nav_tree() -> FolderNavTreeDto
get_folder_children(folder_id: String, sort_by: SortOrder) -> FolderChildrenDto
get_bookmark(bookmark_id: String) -> BookmarkDto?

// Write
add_bookmark(folder_id: String, url: String, title: String) -> String
update_bookmark(bookmark_id: String, url: String?, title: String?, notes: String?)
delete_bookmark(bookmark_id: String)
create_folder(parent_folder_id: String, title: String) -> String
rename_folder(folder_id: String, title: String)
delete_folder(folder_id: String)
move_item(item_id: String, from_folder_id: String, to_folder_id: String)

// Search
search_bookmarks(query: String, sort_by: SortOrder) -> List<BookmarkDto>

// Import/Export
import_html(folder_id: String, html: String) -> ImportResult
export_html() -> String

// Sync
trigger_full_merge() -> Boolean
```

`trigger_full_merge()` merges all peer data, re-hydrates the cache, and
returns `true` if the document changed. No callback interface — Kotlin
drives the timing via polling and lifecycle events.

---

## 7. Feature Parity with Web App

### 7.1 Included (v1.0)

| Feature | Web app | Android app |
|---|---|---|
| Browse folder tree | Sidebar + content area | Drawer + content area |
| Create bookmark | Form in toolbar | FAB → dialog |
| Edit bookmark | Detail panel form | Bottom sheet → edit dialog |
| Delete bookmark | Context menu | Context menu |
| Create folder | Form in toolbar | FAB → dialog |
| Rename folder | Context menu | Context menu → dialog |
| Delete folder | Context menu with cascade | Context menu with confirmation |
| Move item | Modal folder picker | Dialog folder picker |
| Search | Top bar search | Search bar |
| Sort | Dropdown in toolbar | Chip/dropdown |
| Import Netscape HTML | Settings page file upload | Settings → SAF file picker |
| Export Netscape HTML | Settings page download | Settings → SAF save-to |
| Live sync | SSE push | Polling (30s foreground, 15min WorkManager background) |
| Breadcrumbs | Clickable path | Clickable path |

### 7.2 Excluded from v1.0

| Feature | Reason |
|---|---|
| Onboarding wizard + SAF directory picker | Hardcoded path in v1.0; wizard in v1.1 |
| Favicons | Requires network fetch + storage; defer to v1.1 |
| Bookmark history/revert | Complex UI; defer to v1.1 |
| Share-to-app (receive) | Nice-to-have; v1.1 |
| Search relevance scoring | v1.0 uses substring match + sort (parity with web app); v1.1 could rank title matches above URL/notes matches |
| Typed FFI errors | v1.0 uses `FfiError { message: String }` shown in Snackbar; v1.1 maps from core's typed errors for programmatic handling |

### 7.3 Android-specific additions

| Feature | Description |
|---|---|
| Open in browser | Tap to open bookmark URL via intent |
| Share URL | Share bookmark URL to other apps |
| Copy URL | Copy bookmark URL to clipboard |
| Receive shared URLs | Handle `ACTION_SEND` intents to add bookmarks |

---

## 8. Technical Stack

### 8.1 Dependencies

**Kotlin/Android:**
- Jetpack Compose + Material 3
- Compose Navigation
- Kotlin Coroutines
- AndroidX Lifecycle (ViewModel)
- AndroidX WorkManager (background sync polling)
- AndroidX SplashScreen (init latency cover)
- JNA (for UniFFI)

**Testing (Kotlin):**
- JUnit
- Turbine (Flow/StateFlow testing)
- Kotlin Coroutines Test
- Compose UI Testing (`createComposeRule`)
- Roborazzi (screenshot regression, JVM-based)

**Testing (Rust):**
- `proptest` (cache consistency property tests)

**Rust (FFI crate):**
- `mybriefcase-bookmarks-core` (from git)
- `uniffi` 0.28
- `tokio` (multi-threaded runtime)
- `autosurgeon` 0.9

### 8.2 Build

The Rust FFI crate cross-compiles to `aarch64-linux-android` and
`x86_64-linux-android` (for emulator). The resulting `.so` files are placed
in `app/src/main/jniLibs/<abi>/`. UniFFI generates the Kotlin bindings at
`app/src/main/java/uniffi/mybriefcase_bookmarks_ffi/`.

### 8.3 Testing

| Layer | Type | Framework | Scope |
|---|---|---|---|
| Rust FFI crate | Integration | `#[tokio::test]` | Caching, sort, breadcrumbs, error mapping |
| Rust FFI crate | Property | `proptest` | Cache consistency: cached reads always match fresh hydration after any mutation sequence |
| Kotlin ViewModel | Unit | JUnit + Turbine | State transitions, re-fetch after mutation, error handling |
| Kotlin Repository | Unit | JUnit + Coroutines Test | FFI call dispatching, polling logic |
| Compose UI | UI | Compose Testing (`createComposeRule`) | Screen rendering, navigation, user interactions |
| Compose UI | Screenshot | Roborazzi | Visual regression against golden PNGs (JVM, no emulator) |
| Full stack | End-to-end | Instrumented test + real .so | Real Rust FFI on device, real filesystem, full user flows |

Compose UI tests use a fake repository (no Rust FFI), verifying that screens
render correct content, handle empty states, and respond to user actions
(tap folder, long-press context menu, FAB menu, search input, etc.).

End-to-end tests run on an emulator/device with the real Rust `.so` loaded,
exercising full flows: add bookmark → verify in list → navigate folder →
import HTML → verify count.

### 8.4 Minimum SDK

- `minSdk`: 24 (Android 7.0)
- `targetSdk`: 36
- `compileSdk`: 36

---

## 9. Sync Configuration

### 9.1 Initial Setup Flow (v1.0)

1. On launch, the app checks for the sync directory:
   - If absent: creates it (`mkdirs`)
2. Checks for `.bookmarks-sync` in the sync directory:
   - If present: loads the existing shared document (joins the network)
   - If absent: creates `.bookmarks-sync` and initialises a new document
     with the default folder structure
3. If the directory was freshly created (no prior `.bookmarks-sync`), shows a
   dismissible banner: "To sync with other devices, add this folder to
   Syncthing-Fork." The app is fully functional as a local-only bookmark
   manager without Syncthing.

### 9.2 Initial Setup Flow (v1.1 — Wizard)

DecSync-style onboarding wizard:

1. **Welcome slide**: explains the app needs a Syncthing shared folder
2. **Syncthing slide**: link to install Syncthing-Fork if not present
3. **Directory picker slide**: `ACTION_OPEN_DOCUMENT_TREE` with
   `EXTRA_INITIAL_URI` hinting at the conventional Syncthing location;
   persist with `takePersistableUriPermission()`; resolve to filesystem path
4. Same `.bookmarks-sync` detection as v1.0

### 9.3 Sync Directory Selection

In v1.0, the path is a build-time constant. In v1.1, accessible from Settings
via the SAF directory picker.

---

## 10. Error Handling

| Scenario | Behavior |
|---|---|
| Rust FFI call fails | Show Snackbar with error message; log to Logcat |
| Sync directory not found | Show setup prompt in settings |
| No bookmarks yet | Show empty state with "Add your first bookmark" prompt |
| Corrupt document | Core crate handles gracefully; log and show error state |
| Syncthing not installed | App works fully offline; note in settings |

---

## 11. Summary: Operation Mapping

| User action | UI trigger | FFI call | Core operation |
|---|---|---|---|
| Open folder | Tap folder | `get_folder_children` | Hydrate + filter |
| Add bookmark | FAB → dialog → save | `add_bookmark` | `ops::add_bookmark` |
| Edit bookmark | Detail → edit → save | `update_bookmark` | `ops::update_bookmark` |
| Delete bookmark | Context menu → confirm | `delete_bookmark` | `ops::delete_bookmark` |
| Create folder | FAB → dialog → save | `create_folder` | `ops::create_folder` |
| Rename folder | Context menu → dialog | `rename_folder` | `ops::rename_folder` |
| Delete folder | Context menu → confirm | `delete_folder` | `ops::delete_folder` |
| Move item | Context menu → picker | `move_item` | `ops::move_item` |
| Search | Search bar → type | `search_bookmarks` | Hydrate + filter |
| Import | Settings → pick file | `import_html` | `ops::import_items` |
| Export | Settings → save file | `export_html` | `export::export_netscape_html` |
| Sync poll | Timer / onResume / WorkManager | `trigger_full_merge` | `repo::full_merge_pass` |
