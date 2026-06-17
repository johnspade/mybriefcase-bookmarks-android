use crate::{refresh_cache, repo, FfiError};
use mybriefcase_bookmarks_core::ops;
use mybriefcase_bookmarks_core::repo::export_doc_to_shared;

#[uniffi::export]
pub fn add_bookmark(folder_id: String, url: String, title: String) -> Result<String, FfiError> {
    let state = repo();
    let id = ops::add_bookmark(&state.doc_handle, &folder_id, &url, &title)?;
    refresh_cache(state);
    export_doc_to_shared(&state.doc_handle, &state.sync_root, &state.client_id)?;
    Ok(id)
}

#[uniffi::export]
pub fn update_bookmark(
    bookmark_id: String,
    url: Option<String>,
    title: Option<String>,
    notes: Option<String>,
) -> Result<(), FfiError> {
    let state = repo();
    ops::update_bookmark(
        &state.doc_handle,
        &bookmark_id,
        url.as_deref(),
        title.as_deref(),
        notes.as_deref(),
    )?;
    refresh_cache(state);
    export_doc_to_shared(&state.doc_handle, &state.sync_root, &state.client_id)?;
    Ok(())
}

#[uniffi::export]
pub fn delete_bookmark(bookmark_id: String) -> Result<(), FfiError> {
    let state = repo();
    ops::delete_bookmark(&state.doc_handle, &bookmark_id)?;
    refresh_cache(state);
    export_doc_to_shared(&state.doc_handle, &state.sync_root, &state.client_id)?;
    Ok(())
}
