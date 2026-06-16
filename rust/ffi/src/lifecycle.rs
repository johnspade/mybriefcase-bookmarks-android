use crate::{FfiError, RepoState, REPO};
use mybriefcase_bookmarks_core::model::BookmarkStore;
use mybriefcase_bookmarks_core::repo;
use std::path::Path;
use std::sync::RwLock;

#[uniffi::export]
pub fn init_repo(data_dir: String, sync_dir: String, client_id: String) -> Result<(), FfiError> {
    let runtime = tokio::runtime::Runtime::new().map_err(|e| FfiError::General {
        message: format!("failed to create tokio runtime: {e}"),
    })?;

    let data_path = Path::new(&data_dir).to_path_buf();
    let sync_path = Path::new(&sync_dir).to_path_buf();
    let cid = client_id.clone();

    let (repo_handle, doc_handle, _doc_id) =
        runtime.block_on(async { repo::init_repo(&data_path, &sync_path, &cid).await });

    let store: BookmarkStore = doc_handle.with_doc(|doc| autosurgeon::hydrate(doc).unwrap());

    let state = RepoState {
        runtime,
        _repo_handle: repo_handle,
        doc_handle,
        sync_root: sync_path,
        client_id,
        cache: RwLock::new(store),
    };

    REPO.set(state).map_err(|_| FfiError::General {
        message: "init_repo called more than once".to_string(),
    })?;

    Ok(())
}

#[uniffi::export]
pub fn shutdown() {
    // OnceLock cannot be reset, but we drop the runtime by letting it idle.
    // In practice the app process is killed, so this is a best-effort cleanup.
    if let Some(state) = REPO.get() {
        // Export current doc state before shutdown
        mybriefcase_bookmarks_core::repo::export_doc_to_shared(
            &state.doc_handle,
            &state.sync_root,
            &state.client_id,
        );
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    #[cfg_attr(miri, ignore)]
    fn init_repo_creates_state() {
        // This test must run in isolation since REPO is a global singleton.
        // We test the underlying core init instead.
        let tmp = tempfile::tempdir().unwrap();
        let data_dir = tmp.path().join("data");
        let sync_dir = tmp.path().join("sync");
        std::fs::create_dir_all(&data_dir).unwrap();
        std::fs::create_dir_all(&sync_dir).unwrap();

        let rt = tokio::runtime::Runtime::new().unwrap();
        let (_repo_handle, doc_handle, _) =
            rt.block_on(async { repo::init_repo(&data_dir, &sync_dir, "test-client").await });

        let store: BookmarkStore = doc_handle.with_doc(|doc| autosurgeon::hydrate(doc).unwrap());
        assert!(!store.root_folder_id.is_empty());
        assert!(store.folders.contains_key(&store.root_folder_id));
    }
}
