package dev.jspade.mybriefcase.bookmarks.ui.folder

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
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
    onCreateFolder: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Dialog state
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var renameFolderTarget by remember { mutableStateOf<FolderItemDto?>(null) }
    var deleteFolderTarget by remember { mutableStateOf<FolderItemDto?>(null) }
    var moveItemTarget by remember { mutableStateOf<MoveTarget?>(null) }

    // Dialogs
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
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        if (onCreateFolder != null) {
                            onCreateFolder()
                        } else {
                            showCreateFolderDialog = true
                        }
                    },
                    modifier = Modifier.testTag("fab_create_folder"),
                ) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = "Create folder")
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
                        onBreadcrumbClick = { viewModel.navigateToFolder(it) },
                        onFolderRename = { renameFolderTarget = it },
                        onFolderDelete = { deleteFolderTarget = it },
                        onFolderMove = { moveItemTarget = MoveTarget(it.id) },
                        onBookmarkMove = { moveItemTarget = MoveTarget(it.id) },
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}

/** Identifies an item to be moved. */
data class MoveTarget(val itemId: String)

@Composable
private fun FolderContent(
    breadcrumbs: List<BreadcrumbDto>,
    folders: List<FolderItemDto>,
    bookmarks: List<BookmarkItemDto>,
    onFolderClick: (String) -> Unit,
    onBookmarkClick: (String) -> Unit,
    onBreadcrumbClick: (String) -> Unit,
    onFolderRename: (FolderItemDto) -> Unit,
    onFolderDelete: (FolderItemDto) -> Unit,
    onFolderMove: (FolderItemDto) -> Unit,
    onBookmarkMove: (BookmarkItemDto) -> Unit,
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
                    BookmarkListItem(
                        bookmark = bookmark,
                        onClick = { onBookmarkClick(bookmark.id) },
                        onMove = { onBookmarkMove(bookmark) },
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
    onMove: () -> Unit,
) {
    var showContextMenu by remember { mutableStateOf(false) }

    Box {
        ListItem(
            headlineContent = { Text(bookmark.title) },
            supportingContent = { Text(bookmark.url) },
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
                text = { Text("Move") },
                onClick = {
                    showContextMenu = false
                    onMove()
                },
            )
        }
    }
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
