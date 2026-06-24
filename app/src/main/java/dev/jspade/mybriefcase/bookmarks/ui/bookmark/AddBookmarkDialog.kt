package dev.jspade.mybriefcase.bookmarks.ui.bookmark

import android.util.Patterns
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import uniffi.mybriefcase_bookmarks_ffi.FolderNavDto
import uniffi.mybriefcase_bookmarks_ffi.FolderNavTreeDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBookmarkDialog(
    onDismiss: () -> Unit,
    onConfirm: (url: String, title: String, folderId: String?) -> Unit,
    initialUrl: String = "",
    initialTitle: String = "",
    validationError: String? = null,
    onValidationErrorClear: () -> Unit = {},
    faviconFetchEnabled: Boolean = false,
    faviconFetchState: FaviconFetchState = FaviconFetchState.Idle,
    onFetchFavicon: (String) -> Unit = {},
    syncRoot: String? = null,
    navTree: FolderNavTreeDto? = null,
    currentFolderId: String? = null,
    confirmEnabled: Boolean = true,
) {
    var url by remember { mutableStateOf(initialUrl) }
    var title by remember { mutableStateOf(initialTitle) }
    var urlErrorMessage by remember { mutableStateOf<String?>(null) }
    var selectedFolderId by remember { mutableStateOf(currentFolderId) }
    var folderPickerExpanded by remember { mutableStateOf(false) }

    val showError = urlErrorMessage != null || validationError != null
    val errorText = validationError ?: urlErrorMessage

    val folderMap =
        remember(navTree) {
            navTree?.folders?.associateBy { it.id } ?: emptyMap()
        }
    val flatFolderList =
        remember(navTree) {
            if (navTree == null) {
                emptyList()
            } else {
                buildFlatFolderList(navTree.rootFolderId, navTree.folders.associateBy { it.id }, 0)
            }
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Bookmark") },
        text = {
            Column {
                if (faviconFetchEnabled) {
                    FaviconHero(
                        url = url,
                        favicon =
                            when (faviconFetchState) {
                                is FaviconFetchState.Success -> faviconFetchState.filename
                                else -> null
                            },
                        syncRoot = syncRoot,
                        fetchState = faviconFetchState,
                        onFetch = { onFetchFavicon(url) },
                    )
                }
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        urlErrorMessage = null
                        if (validationError != null) onValidationErrorClear()
                    },
                    label = { Text("URL") },
                    isError = showError,
                    supportingText = errorText?.let { { Text(it) } },
                    singleLine = true,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .testTag("add_bookmark_url"),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (optional)") },
                    singleLine = true,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .testTag("add_bookmark_title"),
                )
                if (navTree != null && currentFolderId != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = folderPickerExpanded,
                        onExpandedChange = { folderPickerExpanded = it },
                        modifier = Modifier.testTag("add_bookmark_folder_picker"),
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
                                        selectedFolderId = folder.id
                                        folderPickerExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmedUrl = url.trim()
                    if (trimmedUrl.isBlank()) {
                        urlErrorMessage = "URL is required"
                    } else if (!Patterns.WEB_URL.matcher(trimmedUrl).matches()) {
                        urlErrorMessage = "Invalid URL format"
                    } else {
                        val folderId =
                            if (selectedFolderId != null && selectedFolderId != currentFolderId) {
                                selectedFolderId
                            } else {
                                null
                            }
                        onConfirm(trimmedUrl, title.trim().ifEmpty { trimmedUrl }, folderId)
                    }
                },
                enabled = confirmEnabled,
                modifier = Modifier.testTag("add_bookmark_confirm"),
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        modifier = Modifier.testTag("add_bookmark_dialog"),
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
