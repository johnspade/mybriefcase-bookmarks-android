//! Integration tests that exercise the full FFI singleton lifecycle.
//! Since OnceLock can only be set once per process, all tests in this file
//! share a single initialized repo instance.

use mybriefcase_bookmarks_ffi::*;

/// Initialize the repo singleton once for all tests in this module.
/// Uses `std::sync::Once` to ensure it happens exactly once.
fn ensure_initialized() {
    use std::sync::Once;
    static INIT: Once = Once::new();
    static mut DATA_DIR: Option<tempfile::TempDir> = None;

    INIT.call_once(|| {
        let tmp = tempfile::tempdir().unwrap();
        let data_dir = tmp.path().join("data");
        let sync_dir = tmp.path().join("sync");
        std::fs::create_dir_all(&data_dir).unwrap();
        std::fs::create_dir_all(&sync_dir).unwrap();

        init_repo(
            data_dir.to_str().unwrap().to_string(),
            sync_dir.to_str().unwrap().to_string(),
            "test-android-client".to_string(),
        )
        .unwrap();

        // Keep tempdir alive for the process lifetime
        unsafe {
            DATA_DIR = Some(tmp);
        }
    });
}

#[test]
#[cfg_attr(miri, ignore)]
fn fresh_repo_has_root_folder_in_nav_tree() {
    ensure_initialized();
    let tree = get_folder_nav_tree().unwrap();
    assert!(!tree.root_folder_id.is_empty());
    assert!(
        tree.folders.iter().any(|f| f.id == tree.root_folder_id),
        "nav tree should contain the root folder"
    );
}

#[test]
#[cfg_attr(miri, ignore)]
fn fresh_repo_root_has_default_children() {
    ensure_initialized();
    let tree = get_folder_nav_tree().unwrap();
    let children = get_folder_children(tree.root_folder_id.clone(), SortOrder::NameAsc).unwrap();
    // Root folder should have "Bookmarks Bar" and "Other Bookmarks"
    assert_eq!(children.folders.len(), 2);
    let titles: Vec<&str> = children.folders.iter().map(|f| f.title.as_str()).collect();
    assert!(titles.contains(&"Bookmarks Bar"));
    assert!(titles.contains(&"Other Bookmarks"));
    assert!(children.bookmarks.is_empty());
}

#[test]
#[cfg_attr(miri, ignore)]
fn get_folder_children_returns_breadcrumbs() {
    ensure_initialized();
    let tree = get_folder_nav_tree().unwrap();
    let root_children =
        get_folder_children(tree.root_folder_id.clone(), SortOrder::NameAsc).unwrap();

    // Navigate into a subfolder
    let sub_folder_id = &root_children.folders[0].id;
    let sub_children = get_folder_children(sub_folder_id.clone(), SortOrder::NameAsc).unwrap();

    // Breadcrumbs should include root and the subfolder
    assert!(sub_children.breadcrumbs.len() >= 2);
    assert_eq!(sub_children.breadcrumbs[0].id, tree.root_folder_id);
    assert_eq!(sub_children.breadcrumbs.last().unwrap().id, *sub_folder_id);
}

#[test]
#[cfg_attr(miri, ignore)]
fn add_bookmark_and_retrieve_via_folder_children() {
    ensure_initialized();
    let tree = get_folder_nav_tree().unwrap();
    let root_children =
        get_folder_children(tree.root_folder_id.clone(), SortOrder::NameAsc).unwrap();
    let target_folder = &root_children.folders[0].id;

    let bm_id = add_bookmark(
        target_folder.clone(),
        "https://example.com".to_string(),
        "Example Site".to_string(),
    )
    .unwrap();

    let children = get_folder_children(target_folder.clone(), SortOrder::NameAsc).unwrap();
    assert!(
        children.bookmarks.iter().any(|b| b.id == bm_id),
        "bookmark should appear in folder children"
    );
    let bm = children.bookmarks.iter().find(|b| b.id == bm_id).unwrap();
    assert_eq!(bm.title, "Example Site");
    assert_eq!(bm.url, "https://example.com");
}

#[test]
#[cfg_attr(miri, ignore)]
fn sort_order_name_desc() {
    ensure_initialized();
    let tree = get_folder_nav_tree().unwrap();
    let root_children =
        get_folder_children(tree.root_folder_id.clone(), SortOrder::NameDesc).unwrap();
    // "Other Bookmarks" should come before "Bookmarks Bar" in reverse alpha
    assert_eq!(root_children.folders[0].title, "Other Bookmarks");
    assert_eq!(root_children.folders[1].title, "Bookmarks Bar");
}

#[test]
#[cfg_attr(miri, ignore)]
fn sort_order_name_asc() {
    ensure_initialized();
    let tree = get_folder_nav_tree().unwrap();
    let root_children =
        get_folder_children(tree.root_folder_id.clone(), SortOrder::NameAsc).unwrap();
    assert_eq!(root_children.folders[0].title, "Bookmarks Bar");
    assert_eq!(root_children.folders[1].title, "Other Bookmarks");
}

#[test]
#[cfg_attr(miri, ignore)]
fn sort_order_date_asc_and_desc_for_bookmarks() {
    ensure_initialized();
    let tree = get_folder_nav_tree().unwrap();
    let root_children =
        get_folder_children(tree.root_folder_id.clone(), SortOrder::NameAsc).unwrap();
    let target_folder = &root_children.folders[0].id;

    // Add two bookmarks with slight time gap
    let _id1 = add_bookmark(
        target_folder.clone(),
        "https://first.com".to_string(),
        "First Added".to_string(),
    )
    .unwrap();
    std::thread::sleep(std::time::Duration::from_millis(10));
    let _id2 = add_bookmark(
        target_folder.clone(),
        "https://second.com".to_string(),
        "Second Added".to_string(),
    )
    .unwrap();

    // Date ASC: first added should be first
    let asc = get_folder_children(target_folder.clone(), SortOrder::DateAsc).unwrap();
    let asc_titles: Vec<&str> = asc.bookmarks.iter().map(|b| b.title.as_str()).collect();
    let first_pos = asc_titles.iter().position(|t| *t == "First Added");
    let second_pos = asc_titles.iter().position(|t| *t == "Second Added");
    assert!(
        first_pos < second_pos,
        "DateAsc should put earlier bookmark first"
    );

    // Date DESC: second added should be first
    let desc = get_folder_children(target_folder.clone(), SortOrder::DateDesc).unwrap();
    let desc_titles: Vec<&str> = desc.bookmarks.iter().map(|b| b.title.as_str()).collect();
    let first_pos = desc_titles.iter().position(|t| *t == "First Added");
    let second_pos = desc_titles.iter().position(|t| *t == "Second Added");
    assert!(
        second_pos < first_pos,
        "DateDesc should put later bookmark first"
    );
}

#[test]
#[cfg_attr(miri, ignore)]
fn get_folder_children_nonexistent_folder_errors() {
    ensure_initialized();
    let result = get_folder_children("nonexistent-id".to_string(), SortOrder::NameAsc);
    assert!(result.is_err());
}

#[test]
#[cfg_attr(miri, ignore)]
fn nav_tree_includes_nested_folders() {
    ensure_initialized();
    let tree = get_folder_nav_tree().unwrap();
    let root_children =
        get_folder_children(tree.root_folder_id.clone(), SortOrder::NameAsc).unwrap();
    let parent_id = &root_children.folders[0].id;

    // Create a nested folder
    let child_id = create_folder(parent_id.clone(), "Nested".to_string()).unwrap();

    let tree = get_folder_nav_tree().unwrap();
    let parent_nav = tree.folders.iter().find(|f| f.id == *parent_id).unwrap();
    assert!(
        parent_nav.child_folder_ids.contains(&child_id),
        "parent nav node should list nested folder as child"
    );

    let child_nav = tree.folders.iter().find(|f| f.id == child_id).unwrap();
    assert_eq!(child_nav.title, "Nested");
}

#[test]
#[cfg_attr(miri, ignore)]
fn get_bookmark_returns_full_dto() {
    ensure_initialized();
    let tree = get_folder_nav_tree().unwrap();
    let root_children =
        get_folder_children(tree.root_folder_id.clone(), SortOrder::NameAsc).unwrap();
    let folder_id = &root_children.folders[0].id;

    let bm_id = add_bookmark(
        folder_id.clone(),
        "https://detail.com".to_string(),
        "Detail Test".to_string(),
    )
    .unwrap();

    let bm = get_bookmark(bm_id.clone()).unwrap().unwrap();
    assert_eq!(bm.id, bm_id);
    assert_eq!(bm.url, "https://detail.com");
    assert_eq!(bm.title, "Detail Test");
    assert_eq!(bm.notes, "");
    assert!(!bm.created_at.is_empty());
    assert!(!bm.updated_at.is_empty());
}

#[test]
#[cfg_attr(miri, ignore)]
fn get_bookmark_deleted_returns_none() {
    ensure_initialized();
    let tree = get_folder_nav_tree().unwrap();
    let root_children =
        get_folder_children(tree.root_folder_id.clone(), SortOrder::NameAsc).unwrap();
    let folder_id = &root_children.folders[0].id;

    let bm_id = add_bookmark(
        folder_id.clone(),
        "https://todelete.com".to_string(),
        "To Delete".to_string(),
    )
    .unwrap();
    delete_bookmark(bm_id.clone()).unwrap();

    let result = get_bookmark(bm_id).unwrap();
    assert!(result.is_none());
}

#[test]
#[cfg_attr(miri, ignore)]
fn folder_children_item_count_is_correct() {
    ensure_initialized();
    let tree = get_folder_nav_tree().unwrap();
    let root_children =
        get_folder_children(tree.root_folder_id.clone(), SortOrder::NameAsc).unwrap();

    // Find the "Other Bookmarks" folder (should have 0 items initially)
    let other = root_children
        .folders
        .iter()
        .find(|f| f.title == "Other Bookmarks")
        .unwrap();
    let initial_count = other.item_count;

    // Add a bookmark to it
    add_bookmark(
        other.id.clone(),
        "https://count.com".to_string(),
        "Count Test".to_string(),
    )
    .unwrap();

    let updated_children =
        get_folder_children(tree.root_folder_id.clone(), SortOrder::NameAsc).unwrap();
    let other_updated = updated_children
        .folders
        .iter()
        .find(|f| f.title == "Other Bookmarks")
        .unwrap();
    assert_eq!(other_updated.item_count, initial_count + 1);
}
