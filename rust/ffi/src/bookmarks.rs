use crate::{refresh_cache, repo, FfiError};
use log::debug;
use mybriefcase_bookmarks_core::ops;

#[uniffi::export]
pub fn add_bookmark(folder_id: String, url: String, title: String) -> Result<String, FfiError> {
    let state = repo()?;
    let id = ops::add_bookmark(&state.doc_handle, &folder_id, &url, &title)?;
    refresh_cache(state);
    debug!("[EXPORT] add_bookmark: export");
    state
        .exporter
        .export(&state.doc_handle, std::time::SystemTime::now())?;
    Ok(id)
}

#[uniffi::export]
pub fn update_bookmark(
    bookmark_id: String,
    url: Option<String>,
    title: Option<String>,
    notes: Option<String>,
) -> Result<(), FfiError> {
    let state = repo()?;
    ops::update_bookmark(
        &state.doc_handle,
        &bookmark_id,
        url.as_deref(),
        title.as_deref(),
        notes.as_deref(),
    )?;
    refresh_cache(state);
    debug!("[EXPORT] update_bookmark: export");
    state
        .exporter
        .export(&state.doc_handle, std::time::SystemTime::now())
        .inspect_err(|e| {
            debug!("[EXPORT] update_bookmark: write FAILED: {e}");
        })?;
    debug!("[EXPORT] update_bookmark: write succeeded");
    Ok(())
}

#[uniffi::export]
pub fn delete_bookmark(bookmark_id: String) -> Result<(), FfiError> {
    let state = repo()?;
    ops::delete_bookmark(&state.doc_handle, &bookmark_id)?;
    refresh_cache(state);
    debug!("[EXPORT] delete_bookmark: export");
    state
        .exporter
        .export(&state.doc_handle, std::time::SystemTime::now())?;
    Ok(())
}
