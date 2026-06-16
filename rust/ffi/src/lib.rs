uniffi::setup_scaffolding!();

use mybriefcase_bookmarks_core::model::BookmarkStore;
use std::path::Path;

#[derive(uniffi::Record)]
pub struct BookmarkDto {
    pub id: String,
    pub url: String,
    pub title: String,
}

#[derive(uniffi::Record)]
pub struct FolderDto {
    pub id: String,
    pub title: String,
}

#[uniffi::export]
pub fn list_bookmarks(data_dir: String) -> Vec<BookmarkDto> {
    let rt = tokio::runtime::Runtime::new().unwrap();
    rt.block_on(async {
        let data_path = Path::new(&data_dir);
        let sync_path = data_path.join("sync");
        let (_repo_handle, doc_handle, _doc_id) =
            mybriefcase_bookmarks_core::repo::init_repo(data_path, &sync_path, "android").await;

        let store: BookmarkStore = doc_handle.with_doc(|doc| autosurgeon::hydrate(doc).unwrap());

        store
            .bookmarks
            .iter()
            .filter(|(_, b)| !b.deleted)
            .map(|(id, b)| BookmarkDto {
                id: id.clone(),
                url: b.url.clone(),
                title: b.title.clone(),
            })
            .collect()
    })
}

#[uniffi::export]
pub fn list_folders(data_dir: String) -> Vec<FolderDto> {
    let rt = tokio::runtime::Runtime::new().unwrap();
    rt.block_on(async {
        let data_path = Path::new(&data_dir);
        let sync_path = data_path.join("sync");
        let (_repo_handle, doc_handle, _doc_id) =
            mybriefcase_bookmarks_core::repo::init_repo(data_path, &sync_path, "android").await;

        let store: BookmarkStore = doc_handle.with_doc(|doc| autosurgeon::hydrate(doc).unwrap());

        store
            .folders
            .iter()
            .filter(|(_, f)| !f.deleted)
            .map(|(id, f)| FolderDto {
                id: id.clone(),
                title: f.title.clone(),
            })
            .collect()
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn list_bookmarks_returns_empty_for_fresh_repo() {
        let tmp = tempfile::tempdir().unwrap();
        let result = list_bookmarks(tmp.path().to_str().unwrap().to_string());
        assert!(result.is_empty());
    }

    #[test]
    fn list_folders_returns_root_for_fresh_repo() {
        let tmp = tempfile::tempdir().unwrap();
        let result = list_folders(tmp.path().to_str().unwrap().to_string());
        assert!(
            !result.is_empty(),
            "Fresh repo should have at least one folder"
        );
    }
}
