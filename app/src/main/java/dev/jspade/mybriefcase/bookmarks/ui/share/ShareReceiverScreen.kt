package dev.jspade.mybriefcase.bookmarks.ui.share

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.jspade.mybriefcase.bookmarks.ui.bookmark.FaviconFetchState
import dev.jspade.mybriefcase.bookmarks.ui.bookmark.FaviconHero
import uniffi.mybriefcase_bookmarks_ffi.FolderNavDto
import uniffi.mybriefcase_bookmarks_ffi.FolderNavTreeDto

@Composable
fun ShareReceiverScreen(
    viewModel: ShareReceiverViewModel,
    onFinish: () -> Unit,
    onRedirectToWizard: () -> Unit,
    syncRoot: String? = null,
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.isInitialized) {
        if (!state.isInitialized) {
            onRedirectToWizard()
        }
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            onFinish()
        }
    }

    if (!state.isInitialized) return

    var showFolderPicker by remember { mutableStateOf(false) }
    val selectedFolderName =
        remember(state.selectedFolderId, state.navTree) {
            val tree = state.navTree ?: return@remember "Loading..."
            val folderId = state.selectedFolderId ?: tree.rootFolderId
            tree.folders.find { it.id == folderId }?.title ?: "Root"
        }

    AlertDialog(
        onDismissRequest = onFinish,
        title = { Text("Save Bookmark") },
        text = {
            Column {
                if (viewModel.isFaviconFetchEnabled) {
                    val fetchState = state.faviconFetchState
                    FaviconHero(
                        url = state.url,
                        favicon =
                            when (fetchState) {
                                is FaviconFetchState.Success -> fetchState.filename
                                else -> null
                            },
                        syncRoot = syncRoot,
                        fetchState = fetchState,
                        onFetch = { viewModel.fetchFavicon(state.url) },
                    )
                }
                OutlinedTextField(
                    value = state.url,
                    onValueChange = { viewModel.updateUrl(it) },
                    label = { Text("URL") },
                    isError = state.urlError != null,
                    supportingText = state.urlError?.let { { Text(it) } },
                    singleLine = true,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .testTag("share_url_field"),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.title,
                    onValueChange = { viewModel.updateTitle(it) },
                    label = { Text("Title (optional)") },
                    singleLine = true,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .testTag("share_title_field"),
                )
                Spacer(modifier = Modifier.height(12.dp))
                ListItem(
                    headlineContent = { Text(selectedFolderName) },
                    leadingContent = {
                        Icon(Icons.Default.Folder, contentDescription = null)
                    },
                    supportingContent = { Text("Save to folder") },
                    modifier =
                        Modifier
                            .clickable { showFolderPicker = true }
                            .testTag("share_folder_picker"),
                )

                state.error?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { viewModel.save() },
                modifier = Modifier.testTag("share_save_button"),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onFinish,
                modifier = Modifier.testTag("share_cancel_button"),
            ) {
                Text("Cancel")
            }
        },
    )

    if (showFolderPicker && state.navTree != null) {
        FolderPickerDialog(
            navTree = state.navTree!!,
            selectedFolderId = state.selectedFolderId ?: state.navTree!!.rootFolderId,
            onSelect = { folderId ->
                viewModel.selectFolder(folderId)
                showFolderPicker = false
            },
            onDismiss = { showFolderPicker = false },
        )
    }
}

@Composable
private fun FolderPickerDialog(
    navTree: FolderNavTreeDto,
    selectedFolderId: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val flatList =
        remember(navTree) {
            buildFlatFolderList(navTree.rootFolderId, navTree.folders.associateBy { it.id }, 0)
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select folder") },
        text = {
            LazyColumn {
                items(flatList) { (folder, depth) ->
                    val isSelected = folder.id == selectedFolderId
                    ListItem(
                        headlineContent = { Text(folder.title) },
                        leadingContent = {
                            Icon(Icons.Default.Folder, contentDescription = null)
                        },
                        colors =
                            if (isSelected) {
                                ListItemDefaults.colors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                )
                            } else {
                                ListItemDefaults.colors()
                            },
                        modifier =
                            Modifier
                                .padding(start = (depth * 16).dp)
                                .clickable { onSelect(folder.id) },
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

private fun buildFlatFolderList(
    folderId: String,
    folderMap: Map<String, FolderNavDto>,
    depth: Int,
): List<Pair<FolderNavDto, Int>> {
    val folder = folderMap[folderId] ?: return emptyList()
    val result = mutableListOf(folder to depth)
    for (childId in folder.childFolderIds) {
        result.addAll(buildFlatFolderList(childId, folderMap, depth + 1))
    }
    return result
}
