use crate::{repo, BookmarkDto, FfiError, SortOrder};

#[uniffi::export]
pub fn search_bookmarks(query: String, sort_by: SortOrder) -> Result<Vec<BookmarkDto>, FfiError> {
    let state = repo()?;
    let cache = state.cache.read().unwrap();
    let query_lower = query.to_lowercase();

    let mut results: Vec<BookmarkDto> = cache
        .bookmarks
        .iter()
        .filter(|(_, bm)| {
            !bm.deleted
                && (bm.title.to_lowercase().contains(&query_lower)
                    || bm.url.to_lowercase().contains(&query_lower)
                    || bm.notes.to_lowercase().contains(&query_lower))
        })
        .map(|(id, bm)| BookmarkDto {
            id: id.clone(),
            url: bm.url.clone(),
            title: bm.title.clone(),
            notes: bm.notes.clone(),
            created_at: bm.created_at.clone(),
            updated_at: bm.updated_at.clone(),
        })
        .collect();

    match sort_by {
        SortOrder::NameAsc => results.sort_by_key(|a| a.title.to_lowercase()),
        SortOrder::NameDesc => {
            results.sort_by_key(|a| std::cmp::Reverse(a.title.to_lowercase()));
        }
        SortOrder::DateDesc => {
            results.sort_by_key(|a| std::cmp::Reverse(a.created_at.clone()));
        }
        SortOrder::DateAsc => results.sort_by_key(|a| a.created_at.clone()),
    }

    Ok(results)
}
