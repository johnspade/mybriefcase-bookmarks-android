package dev.jspade.mybriefcase.bookmarks.ui.share

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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

@OptIn(ExperimentalMaterial3Api::class)
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

    var folderPickerExpanded by remember { mutableStateOf(false) }
    val folderMap =
        remember(state.navTree) {
            state.navTree?.folders?.associateBy { it.id } ?: emptyMap()
        }
    val flatFolderList =
        remember(state.navTree) {
            val tree = state.navTree ?: return@remember emptyList()
            buildFlatFolderList(tree.rootFolderId, tree.folders.associateBy { it.id }, 0)
        }
    val selectedFolderId = state.selectedFolderId ?: state.navTree?.rootFolderId

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
                if (state.navTree != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = folderPickerExpanded,
                        onExpandedChange = { folderPickerExpanded = it },
                        modifier = Modifier.testTag("share_folder_picker"),
                    ) {
                        OutlinedTextField(
                            value = folderMap[selectedFolderId]?.title ?: "",
                            onValueChange = {},
                            label = { Text("Folder") },
                            readOnly = true,
                            singleLine = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = folderPickerExpanded)
                            },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        )
                        ExposedDropdownMenu(
                            expanded = folderPickerExpanded,
                            onDismissRequest = { folderPickerExpanded = false },
                        ) {
                            flatFolderList.forEach { (folder, depth) ->
                                DropdownMenuItem(
                                    text = { Text(" ".repeat(depth * 4) + folder.title) },
                                    onClick = {
                                        viewModel.selectFolder(folder.id)
                                        folderPickerExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }

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
