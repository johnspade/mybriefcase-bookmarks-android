package dev.jspade.mybriefcase.bookmarks.ui.folder

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uniffi.mybriefcase_bookmarks_ffi.BookmarkItemDto
import uniffi.mybriefcase_bookmarks_ffi.BreadcrumbDto
import uniffi.mybriefcase_bookmarks_ffi.FolderItemDto

@Composable
fun FolderScreen(
    viewModel: FolderViewModel,
    onBookmarkClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    when {
        uiState.isLoading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        uiState.error != null -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = uiState.error ?: "Unknown error",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        else -> {
            FolderContent(
                breadcrumbs = uiState.breadcrumbs,
                folders = uiState.folders,
                bookmarks = uiState.bookmarks,
                onFolderClick = { viewModel.navigateToFolder(it) },
                onBookmarkClick = onBookmarkClick,
                onBreadcrumbClick = { viewModel.navigateToFolder(it) },
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun FolderContent(
    breadcrumbs: List<BreadcrumbDto>,
    folders: List<FolderItemDto>,
    bookmarks: List<BookmarkItemDto>,
    onFolderClick: (String) -> Unit,
    onBookmarkClick: (String) -> Unit,
    onBreadcrumbClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Breadcrumbs
        if (breadcrumbs.size > 1) {
            BreadcrumbBar(breadcrumbs = breadcrumbs, onClick = onBreadcrumbClick)
        }

        if (folders.isEmpty() && bookmarks.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "This folder is empty",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(folders, key = { it.id }) { folder ->
                    FolderListItem(folder = folder, onClick = { onFolderClick(folder.id) })
                }
                items(bookmarks, key = { it.id }) { bookmark ->
                    BookmarkListItem(
                        bookmark = bookmark,
                        onClick = { onBookmarkClick(bookmark.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BreadcrumbBar(
    breadcrumbs: List<BreadcrumbDto>,
    onClick: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        breadcrumbs.forEachIndexed { index, crumb ->
            if (index > 0) {
                Text(
                    text = ">",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = crumb.title,
                style = MaterialTheme.typography.bodySmall,
                color = if (index == breadcrumbs.lastIndex)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(enabled = index < breadcrumbs.lastIndex) {
                    onClick(crumb.id)
                },
            )
        }
    }
}

@Composable
private fun FolderListItem(folder: FolderItemDto, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(folder.title) },
        supportingContent = { Text("${folder.itemCount} items") },
        leadingContent = {
            Icon(Icons.Default.Folder, contentDescription = null)
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun BookmarkListItem(bookmark: BookmarkItemDto, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(bookmark.title) },
        supportingContent = { Text(bookmark.url) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
