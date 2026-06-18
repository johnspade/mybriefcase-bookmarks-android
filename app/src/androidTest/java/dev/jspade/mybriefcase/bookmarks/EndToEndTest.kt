package dev.jspade.mybriefcase.bookmarks

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import dev.jspade.mybriefcase.bookmarks.data.BookmarkRepository
import dev.jspade.mybriefcase.bookmarks.ui.folder.FolderScreen
import dev.jspade.mybriefcase.bookmarks.ui.folder.FolderViewModel
import dev.jspade.mybriefcase.bookmarks.ui.search.SearchScreen
import dev.jspade.mybriefcase.bookmarks.ui.search.SearchViewModel
import dev.jspade.mybriefcase.bookmarks.ui.theme.MyBriefcaseBookmarksTheme
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class EndToEndTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var repository: BookmarkRepository

    @Before
    fun setup() {
        repository = MyBriefcaseApp.instance.repository
    }

    @Test
    fun addBookmark_appearsInFolderList() {
        val viewModel = FolderViewModel(repository = repository)

        composeTestRule.setContent {
            MyBriefcaseBookmarksTheme {
                FolderScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
            }
        }

        composeTestRule.waitForIdle()

        // Open FAB menu
        composeTestRule.onNodeWithTag("fab_main").performClick()
        composeTestRule.waitForIdle()

        // Click add bookmark
        composeTestRule.onNodeWithTag("fab_add_bookmark").performClick()
        composeTestRule.waitForIdle()

        // Fill in the URL and title
        composeTestRule.onNodeWithTag("add_bookmark_url").performTextInput("https://example.com")
        composeTestRule.onNodeWithTag("add_bookmark_title").performTextInput("Example Site")

        // Confirm
        composeTestRule.onNodeWithTag("add_bookmark_confirm").performClick()
        composeTestRule.waitForIdle()

        // Verify bookmark appears in the list
        composeTestRule.onNodeWithText("Example Site").assertIsDisplayed()
        composeTestRule.onNodeWithText("https://example.com").assertIsDisplayed()
    }

    @Test
    fun createFolder_navigateInto_verifyEmpty_addBookmark() {
        val viewModel = FolderViewModel(repository = repository)

        composeTestRule.setContent {
            MyBriefcaseBookmarksTheme {
                FolderScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
            }
        }

        composeTestRule.waitForIdle()

        // Create a folder
        composeTestRule.onNodeWithTag("fab_main").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("fab_add_folder").performClick()
        composeTestRule.waitForIdle()

        // Type folder name and confirm
        composeTestRule.onNode(hasText("Folder name")).performTextInput("E2E Folder")
        composeTestRule.onNodeWithText("Create").performClick()

        // Wait for the folder to appear in the list
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("E2E Folder").fetchSemanticsNodes().isNotEmpty()
        }

        // Navigate into the new folder
        composeTestRule.onAllNodesWithText("E2E Folder")[0].performClick()
        composeTestRule.waitForIdle()

        // Verify empty state
        composeTestRule.onNodeWithText("This folder is empty").assertIsDisplayed()

        // Add a bookmark inside
        composeTestRule.onNodeWithTag("fab_main").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("fab_add_bookmark").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("add_bookmark_url").performTextInput("https://test.org")
        composeTestRule.onNodeWithTag("add_bookmark_title").performTextInput("Test Bookmark")
        composeTestRule.onNodeWithTag("add_bookmark_confirm").performClick()
        composeTestRule.waitForIdle()

        // Verify bookmark appears
        composeTestRule.onNodeWithText("Test Bookmark").assertIsDisplayed()
        composeTestRule.onNodeWithText("https://test.org").assertIsDisplayed()
    }

    @Test
    fun importHtml_verifyCountsAndContent() {
        val html =
            """
            <!DOCTYPE NETSCAPE-Bookmark-file-1>
            <DL><p>
                <DT><H3>Imported E2E Folder</H3>
                <DL><p>
                    <DT><A HREF="https://one.com">Bookmark One</A>
                    <DT><A HREF="https://two.com">Bookmark Two</A>
                </DL><p>
                <DT><A HREF="https://three.com">Bookmark Three E2E</A>
            </DL><p>
            """.trimIndent()

        val result =
            runBlocking {
                val rootFolderId = repository.getFolderNavTree().rootFolderId
                repository.importHtml(rootFolderId, html)
            }

        // Verify import counts
        assert(result.bookmarksImported == 3u) {
            "Expected 3 bookmarks imported, got ${result.bookmarksImported}"
        }
        assert(result.foldersImported == 1u) {
            "Expected 1 folder imported, got ${result.foldersImported}"
        }

        // Verify via UI that the imported content appears
        val viewModel = FolderViewModel(repository = repository)

        composeTestRule.setContent {
            MyBriefcaseBookmarksTheme {
                FolderScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
            }
        }

        composeTestRule.waitForIdle()

        // The imported folder should exist (may appear in both drawer nav tree + list)
        composeTestRule.onAllNodesWithText("Imported E2E Folder")[0].assertExists()
        // The root-level bookmark should appear
        composeTestRule.onNodeWithText("Bookmark Three E2E").assertIsDisplayed()
    }

    @Test
    fun searchBookmarkByTitle_appearsInResults() {
        // Add a bookmark via repository
        runBlocking {
            val rootFolderId = repository.getFolderNavTree().rootFolderId
            repository.addBookmark(rootFolderId, "https://kotlin.org", "Kotlin Language E2E")
        }

        val searchViewModel = SearchViewModel(repository = repository, debounceMs = 0L)

        composeTestRule.setContent {
            MyBriefcaseBookmarksTheme {
                SearchScreen(
                    viewModel = searchViewModel,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        composeTestRule.waitForIdle()

        // Type a partial query (not the full title to avoid matching the input field itself)
        composeTestRule.onNodeWithTag("search_input").performTextInput("Kotlin")
        composeTestRule.waitForIdle()

        // Verify the matching bookmark appears (title and URL in search results)
        composeTestRule.onNodeWithText("Kotlin Language E2E").assertIsDisplayed()
        composeTestRule.onNodeWithText("https://kotlin.org").assertIsDisplayed()
    }
}
