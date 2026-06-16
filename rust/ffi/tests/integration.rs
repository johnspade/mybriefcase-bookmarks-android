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

// ── Search tests ────────────────────────────────────────────────────────────

#[test]
#[cfg_attr(miri, ignore)]
fn search_by_title_substring() {
    ensure_initialized();
    let tree = get_folder_nav_tree().unwrap();
    let root_children =
        get_folder_children(tree.root_folder_id.clone(), SortOrder::NameAsc).unwrap();
    let folder_id = &root_children.folders[0].id;

    add_bookmark(
        folder_id.clone(),
        "https://search-title.com".to_string(),
        "UniqueSearchTitle".to_string(),
    )
    .unwrap();

    let results = search_bookmarks("UniqueSearch".to_string(), SortOrder::NameAsc).unwrap();
    assert!(
        results.iter().any(|b| b.title == "UniqueSearchTitle"),
        "search by title substring should find the bookmark"
    );
}

#[test]
#[cfg_attr(miri, ignore)]
fn search_by_url_substring() {
    ensure_initialized();
    let tree = get_folder_nav_tree().unwrap();
    let root_children =
        get_folder_children(tree.root_folder_id.clone(), SortOrder::NameAsc).unwrap();
    let folder_id = &root_children.folders[0].id;

    add_bookmark(
        folder_id.clone(),
        "https://unique-url-search-test.dev".to_string(),
        "URL Search Bookmark".to_string(),
    )
    .unwrap();

    let results =
        search_bookmarks("unique-url-search-test".to_string(), SortOrder::NameAsc).unwrap();
    assert!(
        results
            .iter()
            .any(|b| b.url == "https://unique-url-search-test.dev"),
        "search by URL substring should find the bookmark"
    );
}

#[test]
#[cfg_attr(miri, ignore)]
fn search_by_notes_substring() {
    ensure_initialized();
    let tree = get_folder_nav_tree().unwrap();
    let root_children =
        get_folder_children(tree.root_folder_id.clone(), SortOrder::NameAsc).unwrap();
    let folder_id = &root_children.folders[0].id;

    let bm_id = add_bookmark(
        folder_id.clone(),
        "https://notes-search.com".to_string(),
        "Notes Search BM".to_string(),
    )
    .unwrap();

    update_bookmark(
        bm_id,
        None,
        None,
        Some("xyzUniqueNotesContent123".to_string()),
    )
    .unwrap();

    let results =
        search_bookmarks("xyzUniqueNotesContent".to_string(), SortOrder::NameAsc).unwrap();
    assert!(
        results
            .iter()
            .any(|b| b.notes == "xyzUniqueNotesContent123"),
        "search by notes substring should find the bookmark"
    );
}

#[test]
#[cfg_attr(miri, ignore)]
fn search_is_case_insensitive() {
    ensure_initialized();
    let tree = get_folder_nav_tree().unwrap();
    let root_children =
        get_folder_children(tree.root_folder_id.clone(), SortOrder::NameAsc).unwrap();
    let folder_id = &root_children.folders[0].id;

    add_bookmark(
        folder_id.clone(),
        "https://case-test.com".to_string(),
        "CaSeInSeNsItIvE".to_string(),
    )
    .unwrap();

    let results = search_bookmarks("caseinsensitive".to_string(), SortOrder::NameAsc).unwrap();
    assert!(
        results.iter().any(|b| b.title == "CaSeInSeNsItIvE"),
        "search should be case-insensitive"
    );
}

#[test]
#[cfg_attr(miri, ignore)]
fn search_no_matches_returns_empty() {
    ensure_initialized();
    let results = search_bookmarks(
        "zzzNonExistentQueryThatMatchesNothing999".to_string(),
        SortOrder::NameAsc,
    )
    .unwrap();
    assert!(
        results.is_empty(),
        "search with no matches should return empty"
    );
}

#[test]
#[cfg_attr(miri, ignore)]
fn search_results_sort_name_asc_and_desc() {
    ensure_initialized();
    let tree = get_folder_nav_tree().unwrap();
    let root_children =
        get_folder_children(tree.root_folder_id.clone(), SortOrder::NameAsc).unwrap();
    let folder_id = &root_children.folders[0].id;

    add_bookmark(
        folder_id.clone(),
        "https://sort-c.com".to_string(),
        "SortTestC".to_string(),
    )
    .unwrap();
    add_bookmark(
        folder_id.clone(),
        "https://sort-a.com".to_string(),
        "SortTestA".to_string(),
    )
    .unwrap();
    add_bookmark(
        folder_id.clone(),
        "https://sort-b.com".to_string(),
        "SortTestB".to_string(),
    )
    .unwrap();

    // NameAsc
    let results = search_bookmarks("SortTest".to_string(), SortOrder::NameAsc).unwrap();
    let titles: Vec<&str> = results.iter().map(|b| b.title.as_str()).collect();
    let a_pos = titles.iter().position(|t| *t == "SortTestA").unwrap();
    let b_pos = titles.iter().position(|t| *t == "SortTestB").unwrap();
    let c_pos = titles.iter().position(|t| *t == "SortTestC").unwrap();
    assert!(
        a_pos < b_pos && b_pos < c_pos,
        "NameAsc should sort A < B < C"
    );

    // NameDesc
    let results = search_bookmarks("SortTest".to_string(), SortOrder::NameDesc).unwrap();
    let titles: Vec<&str> = results.iter().map(|b| b.title.as_str()).collect();
    let a_pos = titles.iter().position(|t| *t == "SortTestA").unwrap();
    let b_pos = titles.iter().position(|t| *t == "SortTestB").unwrap();
    let c_pos = titles.iter().position(|t| *t == "SortTestC").unwrap();
    assert!(
        c_pos < b_pos && b_pos < a_pos,
        "NameDesc should sort C < B < A"
    );
}

#[test]
#[cfg_attr(miri, ignore)]
fn search_results_sort_date_asc_and_desc() {
    ensure_initialized();
    let tree = get_folder_nav_tree().unwrap();
    let root_children =
        get_folder_children(tree.root_folder_id.clone(), SortOrder::NameAsc).unwrap();
    let folder_id = &root_children.folders[0].id;

    add_bookmark(
        folder_id.clone(),
        "https://datesort-first.com".to_string(),
        "DateSortFirst".to_string(),
    )
    .unwrap();
    std::thread::sleep(std::time::Duration::from_millis(10));
    add_bookmark(
        folder_id.clone(),
        "https://datesort-second.com".to_string(),
        "DateSortSecond".to_string(),
    )
    .unwrap();

    let asc = search_bookmarks("DateSort".to_string(), SortOrder::DateAsc).unwrap();
    let asc_titles: Vec<&str> = asc.iter().map(|b| b.title.as_str()).collect();
    let first_pos = asc_titles
        .iter()
        .position(|t| *t == "DateSortFirst")
        .unwrap();
    let second_pos = asc_titles
        .iter()
        .position(|t| *t == "DateSortSecond")
        .unwrap();
    assert!(first_pos < second_pos, "DateAsc should put earlier first");

    let desc = search_bookmarks("DateSort".to_string(), SortOrder::DateDesc).unwrap();
    let desc_titles: Vec<&str> = desc.iter().map(|b| b.title.as_str()).collect();
    let first_pos = desc_titles
        .iter()
        .position(|t| *t == "DateSortFirst")
        .unwrap();
    let second_pos = desc_titles
        .iter()
        .position(|t| *t == "DateSortSecond")
        .unwrap();
    assert!(second_pos < first_pos, "DateDesc should put later first");
}

#[test]
#[cfg_attr(miri, ignore)]
fn folder_children_item_count_increases_after_add() {
    ensure_initialized();
    let tree = get_folder_nav_tree().unwrap();
    let root_children =
        get_folder_children(tree.root_folder_id.clone(), SortOrder::NameAsc).unwrap();

    // Create a fresh folder to avoid interference from other tests
    let parent_id = &root_children.folders[0].id;
    let fresh_folder = create_folder(parent_id.clone(), "CountTest".to_string()).unwrap();

    // Initially empty
    let children = get_folder_children(fresh_folder.clone(), SortOrder::NameAsc).unwrap();
    assert_eq!(children.bookmarks.len(), 0);

    // Add a bookmark to it
    add_bookmark(
        fresh_folder.clone(),
        "https://count.com".to_string(),
        "Count Test".to_string(),
    )
    .unwrap();

    // Check that the parent now shows item_count = 1 for the fresh folder
    let parent_children = get_folder_children(parent_id.clone(), SortOrder::NameAsc).unwrap();
    let fresh = parent_children
        .folders
        .iter()
        .find(|f| f.id == fresh_folder)
        .unwrap();
    assert_eq!(fresh.item_count, 1);
}

#[test]
#[cfg_attr(miri, ignore)]
fn update_bookmark_changes_fields() {
    ensure_initialized();
    let tree = get_folder_nav_tree().unwrap();
    let root_children =
        get_folder_children(tree.root_folder_id.clone(), SortOrder::NameAsc).unwrap();
    let folder_id = &root_children.folders[0].id;

    let bm_id = add_bookmark(
        folder_id.clone(),
        "https://original.com".to_string(),
        "Original Title".to_string(),
    )
    .unwrap();

    // Update URL and title
    update_bookmark(
        bm_id.clone(),
        Some("https://updated.com".to_string()),
        Some("Updated Title".to_string()),
        Some("Some notes".to_string()),
    )
    .unwrap();

    let bm = get_bookmark(bm_id).unwrap().unwrap();
    assert_eq!(bm.url, "https://updated.com");
    assert_eq!(bm.title, "Updated Title");
    assert_eq!(bm.notes, "Some notes");
}

#[test]
#[cfg_attr(miri, ignore)]
fn update_bookmark_partial_update() {
    ensure_initialized();
    let tree = get_folder_nav_tree().unwrap();
    let root_children =
        get_folder_children(tree.root_folder_id.clone(), SortOrder::NameAsc).unwrap();
    let folder_id = &root_children.folders[0].id;

    let bm_id = add_bookmark(
        folder_id.clone(),
        "https://partial.com".to_string(),
        "Partial Title".to_string(),
    )
    .unwrap();

    // Only update notes, leave url and title unchanged
    update_bookmark(bm_id.clone(), None, None, Some("Just notes".to_string())).unwrap();

    let bm = get_bookmark(bm_id).unwrap().unwrap();
    assert_eq!(bm.url, "https://partial.com");
    assert_eq!(bm.title, "Partial Title");
    assert_eq!(bm.notes, "Just notes");
}

#[test]
#[cfg_attr(miri, ignore)]
fn delete_bookmark_removes_from_folder_children() {
    ensure_initialized();
    let tree = get_folder_nav_tree().unwrap();
    let root_children =
        get_folder_children(tree.root_folder_id.clone(), SortOrder::NameAsc).unwrap();
    let folder_id = &root_children.folders[0].id;

    let bm_id = add_bookmark(
        folder_id.clone(),
        "https://willdelete.com".to_string(),
        "Will Delete".to_string(),
    )
    .unwrap();

    // Verify it exists in folder children
    let children = get_folder_children(folder_id.clone(), SortOrder::NameAsc).unwrap();
    assert!(children.bookmarks.iter().any(|b| b.id == bm_id));

    delete_bookmark(bm_id.clone()).unwrap();

    // Verify it no longer appears in folder children
    let children_after = get_folder_children(folder_id.clone(), SortOrder::NameAsc).unwrap();
    assert!(!children_after.bookmarks.iter().any(|b| b.id == bm_id));
}

#[test]
#[cfg_attr(miri, ignore)]
fn export_empty_store_returns_valid_html() {
    ensure_initialized();
    let html = export_html().unwrap();
    assert!(html.contains("<!DOCTYPE NETSCAPE-Bookmark-file-1>") || html.contains("<DL>"));
}

#[test]
#[cfg_attr(miri, ignore)]
fn import_valid_netscape_html() {
    ensure_initialized();
    let tree = get_folder_nav_tree().unwrap();
    let root_children =
        get_folder_children(tree.root_folder_id.clone(), SortOrder::NameAsc).unwrap();
    let folder_id = &root_children.folders[1].id; // Use "Other Bookmarks" to avoid collisions

    let html = r#"<!DOCTYPE NETSCAPE-Bookmark-file-1>
<META HTTP-EQUIV="Content-Type" CONTENT="text/html; charset=UTF-8">
<TITLE>Bookmarks</TITLE>
<H1>Bookmarks</H1>
<DL><p>
    <DT><H3>Imported Folder</H3>
    <DL><p>
        <DT><A HREF="https://imported1.com">Imported Site 1</A>
        <DT><A HREF="https://imported2.com">Imported Site 2</A>
    </DL><p>
    <DT><A HREF="https://imported3.com">Imported Site 3</A>
</DL><p>"#;

    let result = import_html(folder_id.clone(), html.to_string()).unwrap();
    assert_eq!(result.bookmarks_imported, 3);
    assert_eq!(result.folders_imported, 1);
}

#[test]
#[cfg_attr(miri, ignore)]
fn import_then_export_roundtrip() {
    ensure_initialized();
    let tree = get_folder_nav_tree().unwrap();
    let root_children =
        get_folder_children(tree.root_folder_id.clone(), SortOrder::NameAsc).unwrap();
    let folder_id = &root_children.folders[1].id;

    let html_input = r#"<!DOCTYPE NETSCAPE-Bookmark-file-1>
<META HTTP-EQUIV="Content-Type" CONTENT="text/html; charset=UTF-8">
<TITLE>Bookmarks</TITLE>
<H1>Bookmarks</H1>
<DL><p>
    <DT><A HREF="https://roundtrip.com">Roundtrip Test</A>
</DL><p>"#;

    import_html(folder_id.clone(), html_input.to_string()).unwrap();
    let exported = export_html().unwrap();
    assert!(exported.contains("https://roundtrip.com"));
    assert!(exported.contains("Roundtrip Test"));
}

#[test]
#[cfg_attr(miri, ignore)]
fn create_folder_appears_in_parent_children_and_nav_tree() {
    ensure_initialized();
    let tree = get_folder_nav_tree().unwrap();
    let root_children =
        get_folder_children(tree.root_folder_id.clone(), SortOrder::NameAsc).unwrap();
    let parent_id = &root_children.folders[0].id;

    let new_id = create_folder(parent_id.clone(), "CRUD Test Folder".to_string()).unwrap();

    // Appears in parent's children
    let children = get_folder_children(parent_id.clone(), SortOrder::NameAsc).unwrap();
    assert!(
        children.folders.iter().any(|f| f.id == new_id),
        "new folder should appear in parent's children"
    );
    let folder_item = children.folders.iter().find(|f| f.id == new_id).unwrap();
    assert_eq!(folder_item.title, "CRUD Test Folder");

    // Appears in nav tree
    let tree = get_folder_nav_tree().unwrap();
    assert!(
        tree.folders.iter().any(|f| f.id == new_id),
        "new folder should appear in nav tree"
    );
    let nav_node = tree.folders.iter().find(|f| f.id == new_id).unwrap();
    assert_eq!(nav_node.title, "CRUD Test Folder");
}

#[test]
#[cfg_attr(miri, ignore)]
fn rename_folder_updates_title_in_children_and_nav_tree() {
    ensure_initialized();
    let tree = get_folder_nav_tree().unwrap();
    let root_children =
        get_folder_children(tree.root_folder_id.clone(), SortOrder::NameAsc).unwrap();
    let parent_id = &root_children.folders[0].id;

    let folder_id = create_folder(parent_id.clone(), "Before Rename".to_string()).unwrap();

    rename_folder(folder_id.clone(), "After Rename".to_string()).unwrap();

    // Title updated in parent's children
    let children = get_folder_children(parent_id.clone(), SortOrder::NameAsc).unwrap();
    let renamed = children.folders.iter().find(|f| f.id == folder_id).unwrap();
    assert_eq!(renamed.title, "After Rename");

    // Title updated in nav tree
    let tree = get_folder_nav_tree().unwrap();
    let nav_node = tree.folders.iter().find(|f| f.id == folder_id).unwrap();
    assert_eq!(nav_node.title, "After Rename");
}

#[test]
#[cfg_attr(miri, ignore)]
fn delete_folder_removes_folder_and_nested_items() {
    ensure_initialized();
    let tree = get_folder_nav_tree().unwrap();
    let root_children =
        get_folder_children(tree.root_folder_id.clone(), SortOrder::NameAsc).unwrap();
    let parent_id = &root_children.folders[0].id;

    // Create a folder with a nested bookmark
    let folder_id = create_folder(parent_id.clone(), "To Delete Folder".to_string()).unwrap();
    let _nested_bm = add_bookmark(
        folder_id.clone(),
        "https://nested.com".to_string(),
        "Nested BM".to_string(),
    )
    .unwrap();

    delete_folder(folder_id.clone()).unwrap();

    // Folder gone from parent's children
    let children = get_folder_children(parent_id.clone(), SortOrder::NameAsc).unwrap();
    assert!(
        !children.folders.iter().any(|f| f.id == folder_id),
        "deleted folder should not appear in parent's children"
    );

    // Folder gone from nav tree
    let tree = get_folder_nav_tree().unwrap();
    assert!(
        !tree.folders.iter().any(|f| f.id == folder_id),
        "deleted folder should not appear in nav tree"
    );

    // Nested bookmark also gone
    let bm = get_bookmark(_nested_bm).unwrap();
    assert!(bm.is_none(), "nested bookmark should be deleted");
}

#[test]
#[cfg_attr(miri, ignore)]
fn move_item_moves_between_folders() {
    ensure_initialized();
    let tree = get_folder_nav_tree().unwrap();
    let root_children =
        get_folder_children(tree.root_folder_id.clone(), SortOrder::NameAsc).unwrap();
    let source_id = &root_children.folders[0].id;
    let dest_id = &root_children.folders[1].id;

    // Create a bookmark in source folder
    let bm_id = add_bookmark(
        source_id.clone(),
        "https://moveme.com".to_string(),
        "Move Me".to_string(),
    )
    .unwrap();

    move_item(bm_id.clone(), source_id.clone(), dest_id.clone()).unwrap();

    // Removed from source
    let source_children = get_folder_children(source_id.clone(), SortOrder::NameAsc).unwrap();
    assert!(
        !source_children.bookmarks.iter().any(|b| b.id == bm_id),
        "moved item should not be in source folder"
    );

    // Present in destination
    let dest_children = get_folder_children(dest_id.clone(), SortOrder::NameAsc).unwrap();
    assert!(
        dest_children.bookmarks.iter().any(|b| b.id == bm_id),
        "moved item should be in destination folder"
    );
}

#[test]
#[cfg_attr(miri, ignore)]
fn move_item_into_self_or_descendant_returns_error() {
    ensure_initialized();
    let tree = get_folder_nav_tree().unwrap();
    let root_children =
        get_folder_children(tree.root_folder_id.clone(), SortOrder::NameAsc).unwrap();
    let parent_id = &root_children.folders[0].id;

    // Create a parent folder with a child folder
    let folder_a = create_folder(parent_id.clone(), "Folder A".to_string()).unwrap();
    let folder_b = create_folder(folder_a.clone(), "Folder B (child of A)".to_string()).unwrap();

    // Moving a folder into itself should error
    let result = move_item(folder_a.clone(), parent_id.clone(), folder_a.clone());
    assert!(
        result.is_err(),
        "moving folder into itself should return error"
    );

    // Moving a folder into its own descendant should error
    let result = move_item(folder_a.clone(), parent_id.clone(), folder_b.clone());
    assert!(
        result.is_err(),
        "moving folder into its own descendant should return error"
    );
}
