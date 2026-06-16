package dev.jspade.mybriefcase.bookmarks.ui.folder

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import uniffi.mybriefcase_bookmarks_ffi.BookmarkItemDto
import uniffi.mybriefcase_bookmarks_ffi.BreadcrumbDto
import uniffi.mybriefcase_bookmarks_ffi.FolderItemDto
import uniffi.mybriefcase_bookmarks_ffi.FolderNavDto
import uniffi.mybriefcase_bookmarks_ffi.FolderNavTreeDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderScreen(
    viewModel: FolderViewModel,
    onBookmarkClick: (String) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Dialog state (folder CRUD from PR #9)
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var renameFolderTarget by remember { mutableStateOf<FolderItemDto?>(null) }
    var deleteFolderTarget by remember { mutableStateOf<FolderItemDto?>(null) }
    var moveItemTarget by remember { mutableStateOf<MoveTarget?>(null) }

    // Dialog state (bookmark CRUD from PR #10)
    var showFab by remember { mutableStateOf(false) }
    var showAddBookmarkDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var contextMenuBookmarkId by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf<String?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    // Folder CRUD Dialogs
    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onConfirm = { title ->
                viewModel.createFolder(title)
                showCreateFolderDialog = false
            },
            onDismiss = { showCreateFolderDialog = false },
        )
    }

    renameFolderTarget?.let { folder ->
        RenameFolderDialog(
            currentTitle = folder.title,
            onConfirm = { newTitle ->
                viewModel.renameFolder(folder.id, newTitle)
                renameFolderTarget = null
            },
            onDismiss = { renameFolderTarget = null },
        )
    }

    deleteFolderTarget?.let { folder ->
        DeleteFolderDialog(
            folderTitle = folder.title,
            onConfirm = {
                viewModel.deleteFolder(folder.id)
                deleteFolderTarget = null
            },
            onDismiss = { deleteFolderTarget = null },
        )
    }

    moveItemTarget?.let { target ->
        uiState.navTree?.let { tree ->
            MoveItemDialog(
                navTree = tree,
                currentFolderId = uiState.currentFolderId,
                onConfirm = { destinationId ->
                    viewModel.moveItem(target.itemId, uiState.currentFolderId, destinationId)
                    moveItemTarget = null
                },
                onDismiss = { moveItemTarget = null },
            )
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.testTag("drawer_sheet")) {
                Text(
                    text = "Folders",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp),
                )
                uiState.navTree?.let { tree ->
                    FolderNavTree(
                        tree = tree,
                        currentFolderId = uiState.currentFolderId,
                        onFolderClick = { folderId ->
                            viewModel.navigateToFolder(folderId)
                            scope.launch { drawerState.close() }
                        },
                    )
                }
            }
        },
        modifier = modifier,
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(uiState.folderTitle) },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("drawer_button"),
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Open drawer")
                        }
                    },
                    actions = {
                        Box {
                            IconButton(
                                onClick = { showOverflowMenu = true },
                                modifier = Modifier.testTag("overflow_menu_button"),
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More options")
                            }
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Settings") },
                                    onClick = {
                                        showOverflowMenu = false
                                        onSettingsClick()
                                    },
                                    modifier = Modifier.testTag("menu_settings"),
                                )
                            }
                        }
                    },
                )
            },
            floatingActionButton = {
                Column(horizontalAlignment = Alignment.End) {
                    AnimatedVisibility(visible = showFab) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            SmallFloatingActionButton(
                                onClick = {
                                    showFab = false
                                    showAddBookmarkDialog = true
                                },
                                modifier = Modifier.testTag("fab_add_bookmark"),
                            ) {
                                Icon(Icons.Default.Bookmark, contentDescription = "Add Bookmark")
                            }
                            SmallFloatingActionButton(
                                onClick = {
                                    showFab = false
                                    showCreateFolderDialog = true
                                },
                                modifier = Modifier.testTag("fab_add_folder"),
                            ) {
                                Icon(Icons.Default.CreateNewFolder, contentDescription = "Add Folder")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    FloatingActionButton(
                        onClick = { showFab = !showFab },
                        modifier = Modifier.testTag("fab_main"),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }
            },
        ) { innerPadding ->
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center,
                    ) {
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
                        onBookmarkLongClick = { bookmarkId ->
                            contextMenuBookmarkId = bookmarkId
                        },
                        onBreadcrumbClick = { viewModel.navigateToFolder(it) },
                        onFolderRename = { renameFolderTarget = it },
                        onFolderDelete = { deleteFolderTarget = it },
                        onFolderMove = { moveItemTarget = MoveTarget(it.id) },
                        onBookmarkMove = { moveItemTarget = MoveTarget(it.id) },
                        contextMenuBookmarkId = contextMenuBookmarkId,
                        onDismissContextMenu = { contextMenuBookmarkId = null },
                        onEditBookmark = { bookmarkId ->
                            contextMenuBookmarkId = null
                            viewModel.loadBookmarkDetail(bookmarkId)
                            showEditDialog = true
                        },
                        onDeleteBookmark = { bookmarkId ->
                            contextMenuBookmarkId = null
                            showDeleteConfirmation = bookmarkId
                        },
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }

    if (showAddBookmarkDialog) {
        dev.jspade.mybriefcase.bookmarks.ui.bookmark.AddBookmarkDialog(
            onDismiss = { showAddBookmarkDialog = false },
            onConfirm = { url, title ->
                showAddBookmarkDialog = false
                viewModel.addBookmark(url, title)
            },
        )
    }

    if (showEditDialog && uiState.selectedBookmark != null) {
        dev.jspade.mybriefcase.bookmarks.ui.bookmark.EditBookmarkDialog(
            bookmark = uiState.selectedBookmark!!,
            onDismiss = {
                showEditDialog = false
                viewModel.clearSelectedBookmark()
            },
            onConfirm = { url, title, notes ->
                showEditDialog = false
                viewModel.updateBookmark(uiState.selectedBookmark!!.id, url, title, notes)
            },
        )
    }

    showDeleteConfirmation?.let { bookmarkId ->
        DeleteConfirmationDialog(
            onDismiss = { showDeleteConfirmation = null },
            onConfirm = {
                showDeleteConfirmation = null
                viewModel.deleteBookmark(bookmarkId)
            },
        )
    }
}

/** Identifies an item to be moved. */
data class MoveTarget(val itemId: String)

@Composable
private fun DeleteConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Bookmark") },
        text = { Text("Are you sure you want to delete this bookmark?") },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag("delete_confirm_button"),
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        modifier = Modifier.testTag("delete_confirmation_dialog"),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderContent(
    breadcrumbs: List<BreadcrumbDto>,
    folders: List<FolderItemDto>,
    bookmarks: List<BookmarkItemDto>,
    onFolderClick: (String) -> Unit,
    onBookmarkClick: (String) -> Unit,
    onBookmarkLongClick: (String) -> Unit,
    onBreadcrumbClick: (String) -> Unit,
    onFolderRename: (FolderItemDto) -> Unit,
    onFolderDelete: (FolderItemDto) -> Unit,
    onFolderMove: (FolderItemDto) -> Unit,
    onBookmarkMove: (BookmarkItemDto) -> Unit,
    contextMenuBookmarkId: String?,
    onDismissContextMenu: () -> Unit,
    onEditBookmark: (String) -> Unit,
    onDeleteBookmark: (String) -> Unit,
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
                    FolderListItem(
                        folder = folder,
                        onClick = { onFolderClick(folder.id) },
                        onRename = { onFolderRename(folder) },
                        onDelete = { onFolderDelete(folder) },
                        onMove = { onFolderMove(folder) },
                    )
                }
                items(bookmarks, key = { it.id }) { bookmark ->
                    Box {
                        BookmarkListItem(
                            bookmark = bookmark,
                            onClick = { onBookmarkClick(bookmark.id) },
                            onLongClick = { onBookmarkLongClick(bookmark.id) },
                        )
                        DropdownMenu(
                            expanded = contextMenuBookmarkId == bookmark.id,
                            onDismissRequest = onDismissContextMenu,
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = { onEditBookmark(bookmark.id) },
                                modifier = Modifier.testTag("context_edit"),
                            )
                            DropdownMenuItem(
                                text = { Text("Move") },
                                onClick = {
                                    onDismissContextMenu()
                                    onBookmarkMove(bookmark)
                                },
                                modifier = Modifier.testTag("context_move"),
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = { onDeleteBookmark(bookmark.id) },
                                modifier = Modifier.testTag("context_delete"),
                            )
                        }
                    }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderListItem(
    folder: FolderItemDto,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onMove: () -> Unit,
) {
    var showContextMenu by remember { mutableStateOf(false) }

    Box {
        ListItem(
            headlineContent = { Text(folder.title) },
            supportingContent = { Text("${folder.itemCount} items") },
            leadingContent = {
                Icon(Icons.Default.Folder, contentDescription = null)
            },
            modifier = Modifier.combinedClickable(
                onClick = onClick,
                onLongClick = { showContextMenu = true },
            ),
        )
        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text("Rename") },
                onClick = {
                    showContextMenu = false
                    onRename()
                },
            )
            DropdownMenuItem(
                text = { Text("Move") },
                onClick = {
                    showContextMenu = false
                    onMove()
                },
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    showContextMenu = false
                    onDelete()
                },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookmarkListItem(
    bookmark: BookmarkItemDto,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(bookmark.title) },
        supportingContent = { Text(bookmark.url) },
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick,
        ),
    )
}

@Composable
private fun FolderNavTree(
    tree: FolderNavTreeDto,
    currentFolderId: String,
    onFolderClick: (String) -> Unit,
) {
    val folderMap = remember(tree) { tree.folders.associateBy { it.id } }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        folderMap[tree.rootFolderId]?.let { root ->
            FolderNavNode(
                folder = root,
                folderMap = folderMap,
                currentFolderId = currentFolderId,
                depth = 0,
                onFolderClick = onFolderClick,
            )
        }
    }
}

@Composable
private fun FolderNavNode(
    folder: FolderNavDto,
    folderMap: Map<String, FolderNavDto>,
    currentFolderId: String,
    depth: Int,
    onFolderClick: (String) -> Unit,
) {
    val hasChildren = folder.childFolderIds.isNotEmpty()
    var expanded by remember { mutableStateOf(true) }

    NavigationDrawerItem(
        label = { Text(folder.title) },
        selected = folder.id == currentFolderId,
        onClick = { onFolderClick(folder.id) },
        icon = { Icon(Icons.Default.Folder, contentDescription = null) },
        badge = if (hasChildren) {
            {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                    )
                }
            }
        } else {
            null
        },
        modifier = Modifier.padding(start = (depth * 16).dp),
    )

    if (hasChildren) {
        AnimatedVisibility(visible = expanded) {
            Column {
                for (childId in folder.childFolderIds) {
                    folderMap[childId]?.let { child ->
                        FolderNavNode(
                            folder = child,
                            folderMap = folderMap,
                            currentFolderId = currentFolderId,
                            depth = depth + 1,
                            onFolderClick = onFolderClick,
                        )
                    }
                }
            }
        }
    }
}
