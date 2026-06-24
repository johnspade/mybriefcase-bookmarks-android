package dev.jspade.mybriefcase.bookmarks.ui.share

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import dev.jspade.mybriefcase.bookmarks.ui.bookmark.AddBookmarkDialog

@Composable
fun ShareReceiverScreen(
    viewModel: ShareReceiverViewModel,
    onFinish: () -> Unit,
    onRedirectToWizard: () -> Unit,
    syncRoot: String? = null,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.isInitialized) {
        if (!state.isInitialized) {
            onRedirectToWizard()
        }
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            Toast.makeText(context, "Bookmark saved", Toast.LENGTH_SHORT).show()
            onFinish()
        }
    }

    if (!state.isInitialized) return

    val rootFolderId = state.navTree?.rootFolderId

    AddBookmarkDialog(
        onDismiss = onFinish,
        onConfirm = { url, title, folderId ->
            viewModel.save(url, title, folderId ?: rootFolderId ?: return@AddBookmarkDialog)
        },
        initialUrl = state.url,
        initialTitle = state.title,
        faviconFetchEnabled = viewModel.isFaviconFetchEnabled,
        faviconFetchState = state.faviconFetchState,
        onFetchFavicon = { url -> viewModel.fetchFavicon(url) },
        syncRoot = syncRoot,
        navTree = state.navTree,
        currentFolderId = rootFolderId,
        confirmEnabled = rootFolderId != null,
    )
}
