use crate::{refresh_cache, repo, FfiError};
use mybriefcase_bookmarks_core::ops;
use mybriefcase_bookmarks_core::repo::export_doc_to_shared;

#[uniffi::export]
pub fn create_folder(parent_folder_id: String, title: String) -> Result<String, FfiError> {
    let state = repo();
    let id = ops::create_folder(&state.doc_handle, &parent_folder_id, &title)?;
    refresh_cache(state);
    export_doc_to_shared(&state.doc_handle, &state.sync_root, &state.client_id);
    Ok(id)
}

#[uniffi::export]
pub fn rename_folder(folder_id: String, title: String) -> Result<(), FfiError> {
    let state = repo();
    ops::rename_folder(&state.doc_handle, &folder_id, &title)?;
    refresh_cache(state);
    export_doc_to_shared(&state.doc_handle, &state.sync_root, &state.client_id);
    Ok(())
}

#[uniffi::export]
pub fn delete_folder(folder_id: String) -> Result<(), FfiError> {
    let state = repo();
    ops::delete_folder(&state.doc_handle, &folder_id)?;
    refresh_cache(state);
    export_doc_to_shared(&state.doc_handle, &state.sync_root, &state.client_id);
    Ok(())
}

#[uniffi::export]
pub fn move_item(
    item_id: String,
    from_folder_id: String,
    to_folder_id: String,
) -> Result<(), FfiError> {
    let state = repo();
    ops::move_item(&state.doc_handle, &item_id, &from_folder_id, &to_folder_id)?;
    refresh_cache(state);
    export_doc_to_shared(&state.doc_handle, &state.sync_root, &state.client_id);
    Ok(())
}
