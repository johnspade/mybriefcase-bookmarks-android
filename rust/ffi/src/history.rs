use crate::{refresh_cache, repo, FfiError};
use mybriefcase_bookmarks_core::history;
use mybriefcase_bookmarks_core::ops;
use mybriefcase_bookmarks_core::repo::export_doc_to_shared;

#[derive(uniffi::Record, Clone, Debug)]
pub struct BookmarkHistoryEntryDto {
    pub change_hash: String,
    pub timestamp: i64,
    pub actor: String,
    pub changed_fields: Vec<FieldChangeDto>,
}

#[derive(uniffi::Record, Clone, Debug)]
pub struct FieldChangeDto {
    pub field: String,
    pub old_value: Option<String>,
    pub new_value: Option<String>,
}

#[uniffi::export]
pub fn get_bookmark_history(bookmark_id: String) -> Result<Vec<BookmarkHistoryEntryDto>, FfiError> {
    let state = repo();
    let entries = history::bookmark_history(&state.doc_handle, &bookmark_id);
    Ok(entries
        .into_iter()
        .map(|e| BookmarkHistoryEntryDto {
            change_hash: e.hash,
            timestamp: e.timestamp,
            actor: e.actor,
            changed_fields: e
                .changed_fields
                .into_iter()
                .map(|f| FieldChangeDto {
                    field: f.field,
                    old_value: f.old_value,
                    new_value: f.new_value,
                })
                .collect(),
        })
        .collect())
}

#[uniffi::export]
pub fn revert_bookmark(bookmark_id: String, change_hash: String) -> Result<(), FfiError> {
    let state = repo();
    let hash = history::parse_change_hash(&change_hash).ok_or_else(|| FfiError::General {
        message: format!("invalid change hash: {change_hash}"),
    })?;
    ops::revert_bookmark(&state.doc_handle, &bookmark_id, &hash)?;
    refresh_cache(state);
    export_doc_to_shared(&state.doc_handle, &state.sync_root, &state.client_id)?;
    Ok(())
}
