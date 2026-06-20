use crate::{FfiError, RepoState, REPO};
use mybriefcase_bookmarks_core::model::BookmarkStore;
use mybriefcase_bookmarks_core::repo;
use mybriefcase_bookmarks_core::repo::Exporter;
use std::path::Path;
use std::sync::RwLock;

#[uniffi::export]
pub fn init_repo(data_dir: String, sync_dir: String, client_id: String) -> Result<(), FfiError> {
    android_logger::init_once(
        android_logger::Config::default()
            .with_max_level(log::LevelFilter::Debug)
            .with_tag("MBB_FFI"),
    );

    let runtime = tokio::runtime::Runtime::new().map_err(|e| FfiError::Internal {
        msg: format!("failed to create tokio runtime: {e}"),
    })?;

    let data_path = Path::new(&data_dir).to_path_buf();
    let sync_path = Path::new(&sync_dir).to_path_buf();
    let cid = client_id.clone();

    let (repo_handle, doc_handle) = runtime.block_on(async {
        repo::init_repo(&data_path, &sync_path, &cid, chrono::Utc::now()).await
    })?;

    let store: BookmarkStore = doc_handle.with_doc(|doc| autosurgeon::hydrate(doc).unwrap());
    let exporter = Exporter::new(&sync_path, &client_id);

    let state = RepoState {
        runtime,
        _repo_handle: repo_handle,
        doc_handle,
        sync_root: sync_path,
        client_id,
        cache: RwLock::new(store),
        exporter,
    };

    REPO.set(state).map_err(|_| FfiError::Internal {
        msg: "init_repo called more than once".to_string(),
    })?;

    Ok(())
}

#[uniffi::export]
pub fn shutdown() {
    if let Some(state) = REPO.get() {
        let _ = state
            .exporter
            .export(&state.doc_handle, std::time::SystemTime::now());
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    #[cfg_attr(miri, ignore)]
    fn init_repo_creates_state() {
        let tmp = tempfile::tempdir().unwrap();
        let data_dir = tmp.path().join("data");
        let sync_dir = tmp.path().join("sync");
        std::fs::create_dir_all(&data_dir).unwrap();
        std::fs::create_dir_all(&sync_dir).unwrap();

        let rt = tokio::runtime::Runtime::new().unwrap();
        let (_repo_handle, doc_handle) = rt
            .block_on(async {
                repo::init_repo(&data_dir, &sync_dir, "test-client", chrono::Utc::now()).await
            })
            .unwrap();

        let store: BookmarkStore = doc_handle.with_doc(|doc| autosurgeon::hydrate(doc).unwrap());
        assert!(!store.root_folder_id.is_empty());
        assert!(store.folders.contains_key(&store.root_folder_id));
    }
}
