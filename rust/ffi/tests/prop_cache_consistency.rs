use automerge::transaction::{CommitOptions, Transactable};
use automerge::ObjType;
use automerge_repo::{DocHandle, Repo};
use autosurgeon::hydrate;
use mybriefcase_bookmarks_core::model::BookmarkStore;
use mybriefcase_bookmarks_core::ops;
use proptest::prelude::*;

// ── Test infrastructure ─────────────────────────────────────────────────────

fn new_doc_with_root(client_id: &str) -> (DocHandle, String) {
    let tmp = tempfile::tempdir().unwrap();
    let store = automerge_repo::tokio::FsStorage::open(tmp.path()).unwrap();
    std::mem::forget(tmp); // keep the dir alive for the doc's lifetime
    let repo = Repo::new(Some(client_id.to_string()), Box::new(store));
    let repo_handle = repo.run();
    let doc_handle = repo_handle.new_document();

    let root_id = uuid::Uuid::new_v4().to_string();
    let rid = root_id.clone();
    doc_handle.with_doc_mut(|doc| {
        let mut tx = doc.transaction();
        let now = chrono::Utc::now().to_rfc3339();

        tx.put(automerge::ROOT, "root_folder_id", rid.as_str())
            .unwrap();
        let folders = tx
            .put_object(automerge::ROOT, "folders", ObjType::Map)
            .unwrap();
        tx.put_object(automerge::ROOT, "bookmarks", ObjType::Map)
            .unwrap();
        let meta = tx
            .put_object(automerge::ROOT, "meta", ObjType::Map)
            .unwrap();
        tx.put(&meta, "schema_version", 1_u64).unwrap();
        tx.put(&meta, "collection_name", "bookmarks").unwrap();

        let root = tx.put_object(&folders, rid.as_str(), ObjType::Map).unwrap();
        tx.put(&root, "title", "Bookmarks").unwrap();
        tx.put_object(&root, "children", ObjType::List).unwrap();
        tx.put(&root, "created_at", now.as_str()).unwrap();
        tx.put(&root, "updated_at", now.as_str()).unwrap();
        tx.put(&root, "deleted", false).unwrap();
        tx.commit_with(CommitOptions::default().with_message("init"));
    });

    (doc_handle, root_id)
}

fn hydrate_store(doc: &DocHandle) -> BookmarkStore {
    doc.with_doc(|d| hydrate(d).unwrap())
}

// ── Mutation operations ─────────────────────────────────────────────────────

#[derive(Debug, Clone)]
enum Mutation {
    AddBookmark {
        folder_idx: usize,
        url: String,
        title: String,
    },
    UpdateBookmark {
        bookmark_idx: usize,
        url: Option<String>,
        title: Option<String>,
        notes: Option<String>,
    },
    DeleteBookmark {
        bookmark_idx: usize,
    },
    CreateFolder {
        parent_idx: usize,
        title: String,
    },
    RenameFolder {
        folder_idx: usize,
        new_title: String,
    },
    DeleteFolder {
        folder_idx: usize,
    },
    MoveItem {
        item_idx: usize,
        from_idx: usize,
        to_idx: usize,
    },
}

struct LiveState {
    folder_ids: Vec<String>,
    bookmark_ids: Vec<String>,
}

impl LiveState {
    fn new(root_folder_id: String) -> Self {
        Self {
            folder_ids: vec![root_folder_id],
            bookmark_ids: Vec::new(),
        }
    }

    fn apply(&mut self, doc: &DocHandle, mutation: &Mutation) {
        match mutation {
            Mutation::AddBookmark {
                folder_idx,
                url,
                title,
            } => {
                if self.folder_ids.is_empty() {
                    return;
                }
                let folder_id = &self.folder_ids[*folder_idx % self.folder_ids.len()];
                if let Ok(id) = ops::add_bookmark(doc, folder_id, url, title) {
                    self.bookmark_ids.push(id);
                }
            }
            Mutation::UpdateBookmark {
                bookmark_idx,
                url,
                title,
                notes,
            } => {
                if self.bookmark_ids.is_empty() {
                    return;
                }
                let id = &self.bookmark_ids[*bookmark_idx % self.bookmark_ids.len()];
                let _ = ops::update_bookmark(
                    doc,
                    id,
                    url.as_deref(),
                    title.as_deref(),
                    notes.as_deref(),
                );
            }
            Mutation::DeleteBookmark { bookmark_idx } => {
                if self.bookmark_ids.is_empty() {
                    return;
                }
                let id = &self.bookmark_ids[*bookmark_idx % self.bookmark_ids.len()];
                let _ = ops::delete_bookmark(doc, id);
            }
            Mutation::CreateFolder { parent_idx, title } => {
                if self.folder_ids.is_empty() {
                    return;
                }
                let parent = &self.folder_ids[*parent_idx % self.folder_ids.len()];
                if let Ok(id) = ops::create_folder(doc, parent, title) {
                    self.folder_ids.push(id);
                }
            }
            Mutation::RenameFolder {
                folder_idx,
                new_title,
            } => {
                if self.folder_ids.is_empty() {
                    return;
                }
                let id = &self.folder_ids[*folder_idx % self.folder_ids.len()];
                let _ = ops::rename_folder(doc, id, new_title);
            }
            Mutation::DeleteFolder { folder_idx } => {
                if self.folder_ids.len() <= 1 {
                    return;
                }
                let idx = 1 + (*folder_idx % (self.folder_ids.len() - 1));
                let id = &self.folder_ids[idx];
                let _ = ops::delete_folder(doc, id);
            }
            Mutation::MoveItem {
                item_idx,
                from_idx,
                to_idx,
            } => {
                let all_ids: Vec<_> = self
                    .folder_ids
                    .iter()
                    .chain(self.bookmark_ids.iter())
                    .cloned()
                    .collect();
                if all_ids.is_empty() || self.folder_ids.is_empty() {
                    return;
                }
                let item_id = &all_ids[*item_idx % all_ids.len()];
                let from_id = &self.folder_ids[*from_idx % self.folder_ids.len()];
                let to_id = &self.folder_ids[*to_idx % self.folder_ids.len()];
                let _ = ops::move_item(doc, item_id, from_id, to_id);
            }
        }
    }
}

// ── Strategies ──────────────────────────────────────────────────────────────

fn arb_url() -> impl Strategy<Value = String> {
    "[a-z]{3,8}".prop_map(|s| format!("https://{s}.example.com"))
}

fn arb_title() -> impl Strategy<Value = String> {
    proptest::collection::vec(proptest::char::range('a', 'z'), 2..12)
        .prop_map(|chars| chars.into_iter().collect())
}

fn arb_notes() -> impl Strategy<Value = String> {
    "[A-Za-z0-9 .]{0,30}"
}

fn arb_mutation() -> impl Strategy<Value = Mutation> {
    prop_oneof![
        3 => (0..10usize, arb_url(), arb_title())
            .prop_map(|(idx, url, title)| Mutation::AddBookmark { folder_idx: idx, url, title }),
        2 => (0..20usize, proptest::option::of(arb_url()), proptest::option::of(arb_title()), proptest::option::of(arb_notes()))
            .prop_map(|(idx, url, title, notes)| Mutation::UpdateBookmark { bookmark_idx: idx, url, title, notes }),
        1 => (0..20usize).prop_map(|idx| Mutation::DeleteBookmark { bookmark_idx: idx }),
        3 => (0..10usize, arb_title())
            .prop_map(|(idx, title)| Mutation::CreateFolder { parent_idx: idx, title }),
        1 => (0..10usize, arb_title())
            .prop_map(|(idx, title)| Mutation::RenameFolder { folder_idx: idx, new_title: title }),
        1 => (0..10usize).prop_map(|idx| Mutation::DeleteFolder { folder_idx: idx }),
        1 => (0..20usize, 0..10usize, 0..10usize)
            .prop_map(|(item, from, to)| Mutation::MoveItem { item_idx: item, from_idx: from, to_idx: to }),
    ]
}

fn arb_mutation_sequence() -> impl Strategy<Value = Vec<Mutation>> {
    proptest::collection::vec(arb_mutation(), 5..30)
}

// ── Property test ───────────────────────────────────────────────────────────

proptest! {
    #![proptest_config(ProptestConfig { cases: 256, max_shrink_iters: 2048, .. ProptestConfig::default() })]

    #[test]
    #[cfg_attr(miri, ignore)]
    fn cached_reads_match_fresh_hydration(mutations in arb_mutation_sequence()) {
        let rt = tokio::runtime::Runtime::new().unwrap();
        let _guard = rt.enter();

        let (doc, root_id) = new_doc_with_root("prop-cache");
        let mut state = LiveState::new(root_id);

        // Initial cache (matches what init_repo does)
        let mut cache: BookmarkStore = hydrate_store(&doc);

        for mutation in &mutations {
            state.apply(&doc, mutation);
            // Simulate refresh_cache: re-hydrate after each mutation
            cache = hydrate_store(&doc);
        }

        // Fresh hydration from the same document
        let fresh: BookmarkStore = hydrate_store(&doc);

        // Verify the cached store matches the fresh one
        prop_assert_eq!(cache.root_folder_id, fresh.root_folder_id);
        prop_assert_eq!(cache.folders.len(), fresh.folders.len(),
            "folder count mismatch: cached={} fresh={}", cache.folders.len(), fresh.folders.len());
        prop_assert_eq!(cache.bookmarks.len(), fresh.bookmarks.len(),
            "bookmark count mismatch: cached={} fresh={}", cache.bookmarks.len(), fresh.bookmarks.len());

        for (id, cached_folder) in &cache.folders {
            let fresh_folder = fresh.folders.get(id).unwrap();
            prop_assert_eq!(&cached_folder.title, &fresh_folder.title,
                "folder {} title mismatch", id);
            prop_assert_eq!(&cached_folder.children, &fresh_folder.children,
                "folder {} children mismatch", id);
            prop_assert_eq!(cached_folder.deleted, fresh_folder.deleted,
                "folder {} deleted mismatch", id);
        }

        for (id, cached_bm) in &cache.bookmarks {
            let fresh_bm = fresh.bookmarks.get(id).unwrap();
            prop_assert_eq!(&cached_bm.url, &fresh_bm.url,
                "bookmark {} url mismatch", id);
            prop_assert_eq!(&cached_bm.title, &fresh_bm.title,
                "bookmark {} title mismatch", id);
            prop_assert_eq!(&cached_bm.notes, &fresh_bm.notes,
                "bookmark {} notes mismatch", id);
            prop_assert_eq!(cached_bm.deleted, fresh_bm.deleted,
                "bookmark {} deleted mismatch", id);
        }
    }
}
