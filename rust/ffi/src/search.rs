use mybriefcase_bookmarks_core::model::Bookmark;

use crate::{repo, BookmarkDto, FfiError, SortOrder};

const TITLE_WEIGHT: u32 = 30;
const URL_WEIGHT: u32 = 20;
const NOTES_WEIGHT: u32 = 10;
const PREFIX_BONUS: u32 = 5;

fn relevance_score(bm: &Bookmark, query: &str) -> u32 {
    let title = bm.title.to_lowercase();
    let url = bm.url.to_lowercase();
    let notes = bm.notes.to_lowercase();

    let mut score = 0u32;
    if title.contains(query) {
        score += TITLE_WEIGHT;
        if title.starts_with(query) {
            score += PREFIX_BONUS;
        }
    }
    if url.contains(query) {
        score += URL_WEIGHT;
        if url.starts_with(query) {
            score += PREFIX_BONUS;
        }
    }
    if notes.contains(query) {
        score += NOTES_WEIGHT;
        if notes.starts_with(query) {
            score += PREFIX_BONUS;
        }
    }
    score
}

#[uniffi::export]
pub fn search_bookmarks(query: String, sort_by: SortOrder) -> Result<Vec<BookmarkDto>, FfiError> {
    let state = repo();
    let cache = state.cache.read().unwrap();
    let query_lower = query.to_lowercase();

    let mut results: Vec<(BookmarkDto, u32)> = cache
        .bookmarks
        .iter()
        .filter(|(_, bm)| !bm.deleted)
        .filter_map(|(id, bm)| {
            let score = relevance_score(bm, &query_lower);
            if score == 0 {
                return None;
            }
            Some((
                BookmarkDto {
                    id: id.clone(),
                    url: bm.url.clone(),
                    title: bm.title.clone(),
                    notes: bm.notes.clone(),
                    created_at: bm.created_at.clone(),
                    updated_at: bm.updated_at.clone(),
                },
                score,
            ))
        })
        .collect();

    match sort_by {
        SortOrder::Relevance => {
            results.sort_by(|a, b| b.1.cmp(&a.1).then_with(|| a.0.id.cmp(&b.0.id)));
        }
        SortOrder::NameAsc => results.sort_by_key(|a| a.0.title.to_lowercase()),
        SortOrder::NameDesc => {
            results.sort_by_key(|a| std::cmp::Reverse(a.0.title.to_lowercase()));
        }
        SortOrder::DateDesc => {
            results.sort_by_key(|a| std::cmp::Reverse(a.0.created_at.clone()));
        }
        SortOrder::DateAsc => results.sort_by_key(|a| a.0.created_at.clone()),
    }

    Ok(results.into_iter().map(|(dto, _)| dto).collect())
}
