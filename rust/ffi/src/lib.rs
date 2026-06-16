uniffi::setup_scaffolding!();

pub mod bookmarks;
pub mod folders;
pub mod import_export;
pub mod lifecycle;
pub mod read;
pub mod search;
pub mod sync;

// Re-export all FFI functions for integration tests
pub use bookmarks::{add_bookmark, delete_bookmark, update_bookmark};
pub use folders::{create_folder, delete_folder, move_item, rename_folder};
pub use import_export::{export_html, import_html};
pub use lifecycle::{init_repo, shutdown};
pub use read::{get_bookmark, get_folder_children, get_folder_nav_tree};
pub use search::search_bookmarks;
pub use sync::trigger_full_merge;

use automerge_repo::{DocHandle, RepoHandle};
use autosurgeon::hydrate;
use mybriefcase_bookmarks_core::model::BookmarkStore;
use std::sync::{OnceLock, RwLock};
use tokio::runtime::Runtime;

/// Shared state held in a module-level singleton.
struct RepoState {
    #[allow(dead_code)] // held to keep the runtime alive
    runtime: Runtime,
    _repo_handle: RepoHandle,
    doc_handle: DocHandle,
    sync_root: std::path::PathBuf,
    client_id: String,
    cache: RwLock<BookmarkStore>,
}

static REPO: OnceLock<RepoState> = OnceLock::new();

/// Access the singleton, panicking if not initialized.
fn repo() -> &'static RepoState {
    REPO.get()
        .expect("repo not initialized: call init_repo first")
}

/// Re-hydrates the cache from the document. Call after mutations and merges.
fn refresh_cache(state: &RepoState) {
    let store: BookmarkStore = state.doc_handle.with_doc(|doc| hydrate(doc).unwrap());
    *state.cache.write().unwrap() = store;
}

// ── FFI Error type ──────────────────────────────────────────────────────────

#[derive(Debug, thiserror::Error, uniffi::Error)]
#[uniffi(flat_error)]
pub enum FfiError {
    #[error("{message}")]
    General { message: String },
}

impl From<anyhow::Error> for FfiError {
    fn from(e: anyhow::Error) -> Self {
        FfiError::General {
            message: format!("{e:#}"),
        }
    }
}

// ── DTOs ────────────────────────────────────────────────────────────────────

#[derive(uniffi::Record, Clone, Debug)]
pub struct BookmarkDto {
    pub id: String,
    pub url: String,
    pub title: String,
    pub notes: String,
    pub created_at: String,
    pub updated_at: String,
}

#[derive(uniffi::Record, Clone, Debug)]
pub struct FolderDto {
    pub id: String,
    pub title: String,
    pub children: Vec<String>,
    pub created_at: String,
    pub updated_at: String,
}

#[derive(uniffi::Record, Clone, Debug)]
pub struct BookmarkItemDto {
    pub id: String,
    pub title: String,
    pub url: String,
    pub created_at: String,
}

#[derive(uniffi::Record, Clone, Debug)]
pub struct FolderItemDto {
    pub id: String,
    pub title: String,
    pub item_count: u32,
}

#[derive(uniffi::Record, Clone, Debug)]
pub struct FolderNavDto {
    pub id: String,
    pub title: String,
    pub item_count: u32,
    pub child_folder_ids: Vec<String>,
}

#[derive(uniffi::Record, Clone, Debug)]
pub struct FolderNavTreeDto {
    pub root_folder_id: String,
    pub folders: Vec<FolderNavDto>,
}

#[derive(uniffi::Record, Clone, Debug)]
pub struct BreadcrumbDto {
    pub id: String,
    pub title: String,
}

#[derive(uniffi::Record, Clone, Debug)]
pub struct FolderChildrenDto {
    pub folder_title: String,
    pub breadcrumbs: Vec<BreadcrumbDto>,
    pub folders: Vec<FolderItemDto>,
    pub bookmarks: Vec<BookmarkItemDto>,
}

#[derive(uniffi::Record, Clone, Debug)]
pub struct ImportResultDto {
    pub bookmarks_imported: u32,
    pub folders_imported: u32,
}

#[derive(uniffi::Enum, Clone, Copy, Debug)]
pub enum SortOrder {
    NameAsc,
    NameDesc,
    DateDesc,
    DateAsc,
}
