use mybriefcase_bookmarks_core::avatar;

#[uniffi::export]
pub fn domain_letter(url: String) -> String {
    avatar::domain_letter(&url)
}

#[uniffi::export]
pub fn domain_color(url: String) -> String {
    avatar::domain_color(&url)
}
