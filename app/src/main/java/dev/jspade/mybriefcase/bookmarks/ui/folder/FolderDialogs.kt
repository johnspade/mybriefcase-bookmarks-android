package dev.jspade.mybriefcase.bookmarks.ui.folder

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uniffi.mybriefcase_bookmarks_ffi.FolderNavDto
import uniffi.mybriefcase_bookmarks_ffi.FolderNavTreeDto

@Composable
fun CreateFolderDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create folder") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Folder name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title) },
                enabled = title.isNotBlank(),
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun RenameFolderDialog(
    currentTitle: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf(currentTitle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename folder") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Folder name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title) },
                enabled = title.isNotBlank(),
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun DeleteFolderDialog(
    folderTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete \"$folderTitle\"?") },
        text = { Text("This folder and all its contents will be deleted.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun MoveItemDialog(
    navTree: FolderNavTreeDto,
    currentFolderId: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val folderMap = remember(navTree) { navTree.folders.associateBy { it.id } }
    val disabledIds = remember(navTree, currentFolderId) {
        collectDescendantIds(currentFolderId, folderMap) + currentFolderId
    }
    var selectedFolderId by remember { mutableStateOf<String?>(null) }

    // Build flat list with depth for indentation
    val flatList = remember(navTree) {
        buildFlatFolderList(navTree.rootFolderId, folderMap, depth = 0)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to folder") },
        text = {
            LazyColumn {
                items(flatList) { (folder, depth) ->
                    val isDisabled = folder.id in disabledIds
                    val isSelected = folder.id == selectedFolderId
                    ListItem(
                        headlineContent = {
                            Text(
                                text = folder.title,
                                color = if (isDisabled)
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                else
                                    MaterialTheme.colorScheme.onSurface,
                            )
                        },
                        leadingContent = {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                tint = if (isDisabled)
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        colors = if (isSelected) ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ) else ListItemDefaults.colors(),
                        modifier = Modifier
                            .padding(start = (depth * 16).dp)
                            .clickable(enabled = !isDisabled) {
                                selectedFolderId = folder.id
                            },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedFolderId?.let { onConfirm(it) } },
                enabled = selectedFolderId != null,
            ) {
                Text("Move here")
            }
        },
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

private fun collectDescendantIds(
    folderId: String,
    folderMap: Map<String, FolderNavDto>,
): Set<String> {
    val folder = folderMap[folderId] ?: return emptySet()
    val result = mutableSetOf<String>()
    for (childId in folder.childFolderIds) {
        result.add(childId)
        result.addAll(collectDescendantIds(childId, folderMap))
    }
    return result
}
