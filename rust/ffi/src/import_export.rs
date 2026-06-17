use crate::{refresh_cache, repo, FfiError, ImportResultDto};
use mybriefcase_bookmarks_core::export::export_netscape_html;
use mybriefcase_bookmarks_core::import::parse_netscape_html;
use mybriefcase_bookmarks_core::ops::import_items;
use mybriefcase_bookmarks_core::repo::export_doc_to_shared;

#[uniffi::export]
pub fn import_html(folder_id: String, html: String) -> Result<ImportResultDto, FfiError> {
    let state = repo();
    let items = parse_netscape_html(&html);
    let (bc, fc) = import_items(&state.doc_handle, &folder_id, &items)?;
    refresh_cache(state);
    export_doc_to_shared(&state.doc_handle, &state.sync_root, &state.client_id)?;
    Ok(ImportResultDto {
        bookmarks_imported: bc as u32,
        folders_imported: fc as u32,
    })
}

#[uniffi::export]
pub fn export_html() -> Result<String, FfiError> {
    let state = repo();
    let cache = state.cache.read().unwrap();
    let mut buf = Vec::new();
    export_netscape_html(&cache, &mut buf).map_err(|e| FfiError::General {
        message: format!("export failed: {e}"),
    })?;
    String::from_utf8(buf).map_err(|e| FfiError::General {
        message: format!("export produced invalid UTF-8: {e}"),
    })
}
