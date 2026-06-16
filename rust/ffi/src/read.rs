use crate::{
    repo, BookmarkDto, BookmarkItemDto, BreadcrumbDto, FfiError, FolderChildrenDto, FolderItemDto,
    FolderNavDto, FolderNavTreeDto, SortOrder,
};

#[uniffi::export]
pub fn get_folder_children(
    folder_id: String,
    sort_by: SortOrder,
) -> Result<FolderChildrenDto, FfiError> {
    let state = repo();
    let cache = state.cache.read().unwrap();

    let folder = cache
        .folders
        .get(&folder_id)
        .ok_or_else(|| FfiError::General {
            message: format!("folder not found: {folder_id}"),
        })?;

    let mut folder_items = Vec::new();
    let mut bookmark_items = Vec::new();

    for child_id in &folder.children {
        if let Some(sub) = cache.folders.get(child_id) {
            if sub.deleted {
                continue;
            }
            let item_count = sub
                .children
                .iter()
                .filter(|c| {
                    cache
                        .folders
                        .get(*c)
                        .map(|f| !f.deleted)
                        .unwrap_or_else(|| {
                            cache.bookmarks.get(*c).map(|b| !b.deleted).unwrap_or(false)
                        })
                })
                .count() as u32;
            folder_items.push(FolderItemDto {
                id: child_id.clone(),
                title: sub.title.clone(),
                item_count,
            });
        } else if let Some(bm) = cache.bookmarks.get(child_id) {
            if bm.deleted {
                continue;
            }
            bookmark_items.push(BookmarkItemDto {
                id: child_id.clone(),
                title: bm.title.clone(),
                url: bm.url.clone(),
                created_at: bm.created_at.clone(),
            });
        }
    }

    // Sort folders
    sort_folder_items(&mut folder_items, sort_by);
    // Sort bookmarks
    sort_bookmark_items(&mut bookmark_items, sort_by);

    // Build breadcrumbs
    let breadcrumbs = build_breadcrumbs(&cache, &folder_id);

    Ok(FolderChildrenDto {
        folder_title: folder.title.clone(),
        breadcrumbs,
        folders: folder_items,
        bookmarks: bookmark_items,
    })
}

#[uniffi::export]
pub fn get_folder_nav_tree() -> Result<FolderNavTreeDto, FfiError> {
    let state = repo();
    let cache = state.cache.read().unwrap();

    let mut nav_folders = Vec::new();
    for (id, folder) in &cache.folders {
        if folder.deleted {
            continue;
        }
        let child_folder_ids: Vec<String> = folder
            .children
            .iter()
            .filter(|c| cache.folders.get(*c).map(|f| !f.deleted).unwrap_or(false))
            .cloned()
            .collect();
        let item_count = folder
            .children
            .iter()
            .filter(|c| {
                cache
                    .folders
                    .get(*c)
                    .map(|f| !f.deleted)
                    .unwrap_or_else(|| cache.bookmarks.get(*c).map(|b| !b.deleted).unwrap_or(false))
            })
            .count() as u32;
        nav_folders.push(FolderNavDto {
            id: id.clone(),
            title: folder.title.clone(),
            item_count,
            child_folder_ids,
        });
    }

    Ok(FolderNavTreeDto {
        root_folder_id: cache.root_folder_id.clone(),
        folders: nav_folders,
    })
}

#[uniffi::export]
pub fn get_bookmark(bookmark_id: String) -> Result<Option<BookmarkDto>, FfiError> {
    let state = repo();
    let cache = state.cache.read().unwrap();

    Ok(cache.bookmarks.get(&bookmark_id).and_then(|bm| {
        if bm.deleted {
            None
        } else {
            Some(BookmarkDto {
                id: bookmark_id.clone(),
                url: bm.url.clone(),
                title: bm.title.clone(),
                notes: bm.notes.clone(),
                created_at: bm.created_at.clone(),
                updated_at: bm.updated_at.clone(),
            })
        }
    }))
}

fn sort_folder_items(items: &mut [FolderItemDto], sort_by: SortOrder) {
    match sort_by {
        SortOrder::NameAsc | SortOrder::DateDesc | SortOrder::DateAsc => {
            items.sort_by_key(|a| a.title.to_lowercase());
        }
        SortOrder::NameDesc => {
            items.sort_by_key(|a| std::cmp::Reverse(a.title.to_lowercase()));
        }
    }
}

fn sort_bookmark_items(items: &mut [BookmarkItemDto], sort_by: SortOrder) {
    match sort_by {
        SortOrder::NameAsc => items.sort_by_key(|a| a.title.to_lowercase()),
        SortOrder::NameDesc => {
            items.sort_by_key(|a| std::cmp::Reverse(a.title.to_lowercase()));
        }
        SortOrder::DateDesc => items.sort_by_key(|a| std::cmp::Reverse(a.created_at.clone())),
        SortOrder::DateAsc => items.sort_by_key(|a| a.created_at.clone()),
    }
}

fn build_breadcrumbs(
    store: &mybriefcase_bookmarks_core::model::BookmarkStore,
    target_folder_id: &str,
) -> Vec<BreadcrumbDto> {
    // Build parent map
    let mut parent_map: std::collections::HashMap<&str, &str> = std::collections::HashMap::new();
    for (id, folder) in &store.folders {
        if folder.deleted {
            continue;
        }
        for child_id in &folder.children {
            if store.folders.contains_key(child_id) {
                parent_map.insert(child_id.as_str(), id.as_str());
            }
        }
    }

    // Walk up from target to root
    let mut path = Vec::new();
    let mut current = target_folder_id;
    loop {
        if let Some(folder) = store.folders.get(current) {
            path.push(BreadcrumbDto {
                id: current.to_string(),
                title: folder.title.clone(),
            });
        }
        if current == store.root_folder_id {
            break;
        }
        match parent_map.get(current) {
            Some(parent) => current = parent,
            None => break,
        }
    }

    path.reverse();
    path
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::SortOrder;

    #[test]
    fn sort_bookmark_items_by_name_asc() {
        let mut items = vec![
            BookmarkItemDto {
                id: "1".into(),
                title: "Zebra".into(),
                url: "https://z.com".into(),
                created_at: "2024-01-01T00:00:00Z".into(),
            },
            BookmarkItemDto {
                id: "2".into(),
                title: "Apple".into(),
                url: "https://a.com".into(),
                created_at: "2024-01-02T00:00:00Z".into(),
            },
        ];
        sort_bookmark_items(&mut items, SortOrder::NameAsc);
        assert_eq!(items[0].title, "Apple");
        assert_eq!(items[1].title, "Zebra");
    }

    #[test]
    fn sort_bookmark_items_by_date_desc() {
        let mut items = vec![
            BookmarkItemDto {
                id: "1".into(),
                title: "Old".into(),
                url: "https://old.com".into(),
                created_at: "2024-01-01T00:00:00Z".into(),
            },
            BookmarkItemDto {
                id: "2".into(),
                title: "New".into(),
                url: "https://new.com".into(),
                created_at: "2024-06-01T00:00:00Z".into(),
            },
        ];
        sort_bookmark_items(&mut items, SortOrder::DateDesc);
        assert_eq!(items[0].title, "New");
        assert_eq!(items[1].title, "Old");
    }
}
