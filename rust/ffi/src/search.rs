use mybriefcase_bookmarks_core::search;

use crate::{repo, BookmarkDto, FfiError, SortOrder};

impl From<SortOrder> for search::SortOrder {
    fn from(s: SortOrder) -> Self {
        match s {
            SortOrder::NameAsc => Self::NameAsc,
            SortOrder::NameDesc => Self::NameDesc,
            SortOrder::DateDesc => Self::DateDesc,
            SortOrder::DateAsc => Self::DateAsc,
            SortOrder::Relevance => Self::Relevance,
        }
    }
}

#[uniffi::export]
pub fn search_bookmarks(query: String, sort_by: SortOrder) -> Result<Vec<BookmarkDto>, FfiError> {
    let state = repo()?;
    let cache = state.cache.read().unwrap();

    let hits = search::search_bookmarks(&cache, &query, sort_by.into());

    Ok(hits
        .into_iter()
        .map(|h| BookmarkDto {
            id: h.id,
            url: h.url,
            title: h.title,
            notes: h.notes,
            created_at: h.created_at,
            updated_at: h.updated_at,
        })
        .collect())
}
