use crate::{refresh_cache, repo, FfiError};
use log::debug;
use mybriefcase_bookmarks_core::repo::full_merge_pass;

#[uniffi::export]
pub fn trigger_full_merge() -> Result<bool, FfiError> {
    let state = repo()?;
    let changed = full_merge_pass(&state.doc_handle, &state.sync_root, &state.client_id);
    if changed {
        refresh_cache(state);
        state
            .exporter
            .export(&state.doc_handle, std::time::SystemTime::now())?;
        debug!("[EXPORT] trigger_full_merge: merged=true, exported");
    } else {
        debug!("[EXPORT] trigger_full_merge: merged=false");
    }
    Ok(changed)
}
