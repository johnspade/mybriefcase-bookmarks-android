package dev.jspade.mybriefcase.bookmarks.ui.folder

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import dev.jspade.mybriefcase.bookmarks.data.BookmarkError
import dev.jspade.mybriefcase.bookmarks.ui.bookmark.BookmarkDetailSheetWithActions
import dev.jspade.mybriefcase.bookmarks.ui.bookmark.BookmarkFavicon
import dev.jspade.mybriefcase.bookmarks.ui.search.displayName
import kotlinx.coroutines.launch
import uniffi.mybriefcase_bookmarks_ffi.BookmarkItemDto
import uniffi.mybriefcase_bookmarks_ffi.BreadcrumbDto
import uniffi.mybriefcase_bookmarks_ffi.FolderItemDto
import uniffi.mybriefcase_bookmarks_ffi.FolderNavDto
import uniffi.mybriefcase_bookmarks_ffi.FolderNavTreeDto
import uniffi.mybriefcase_bookmarks_ffi.SortOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderScreen(
    viewModel: FolderViewModel,
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onHistoryClick: ((bookmarkId: String) -> Unit)? = null,
    faviconFetchEnabled: Boolean = true,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle error-variant-specific behavior
    LaunchedEffect(uiState.error) {
        val error = uiState.error ?: return@LaunchedEffect
        when (error) {
            is BookmarkError.NotFound -> {
                viewModel.navigateUp()
                viewModel.clearError()
            }
            is BookmarkError.PermissionDenied -> {
                val result =
                    snackbarHostState.showSnackbar(
                        message = "Storage permission required. Grant access to sync bookmarks.",
                        actionLabel = "Grant",
                        duration = SnackbarDuration.Long,
                    )
                if (result == SnackbarResult.ActionPerformed &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                ) {
                    val intent =
                        Intent(
                            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                            "package:${context.packageName}".toUri(),
                        )
                    context.startActivity(intent)
                }
                viewModel.clearError()
            }
            is BookmarkError.IoError -> {
                val result =
                    snackbarHostState.showSnackbar(
                        message = error.message,
                        actionLabel = "Retry",
                        duration = SnackbarDuration.Long,
                    )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.refresh()
                }
                viewModel.clearError()
            }
            is BookmarkError.Internal -> {
                snackbarHostState.showSnackbar(
                    message = error.message,
                    duration = SnackbarDuration.Long,
                )
                viewModel.clearError()
            }
            is BookmarkError.InvalidInput,
            is BookmarkError.NotInitialized,
            -> {}
        }
    }

    BackHandler(enabled = uiState.breadcrumbs.size > 1) {
        viewModel.navigateUp()
    }

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
    var showDetailSheet by remember { mutableStateOf(false) }

    // Dismiss dialogs on successful mutation
    LaunchedEffect(uiState.mutationVersion) {
        if (uiState.mutationVersion > 0L) {
            showCreateFolderDialog = false
            renameFolderTarget = null
            showAddBookmarkDialog = false
            showEditDialog = false
        }
    }

    // Folder CRUD Dialogs
    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onConfirm = { title -> viewModel.createFolder(title) },
            onDismiss = {
                showCreateFolderDialog = false
                viewModel.clearValidationError()
            },
            validationError = uiState.validationError,
            onValidationErrorClear = { viewModel.clearValidationError() },
        )
    }

    renameFolderTarget?.let { folder ->
        RenameFolderDialog(
            currentTitle = folder.title,
            onConfirm = { newTitle -> viewModel.renameFolder(folder.id, newTitle) },
            onDismiss = {
                renameFolderTarget = null
                viewModel.clearValidationError()
            },
            validationError = uiState.validationError,
            onValidationErrorClear = { viewModel.clearValidationError() },
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
                movedFolderId = (target as? MoveTarget.Folder)?.itemId,
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
                        expandedFolderIds = uiState.expandedFolderIds,
                        onFolderClick = { folderId ->
                            viewModel.navigateToFolder(folderId)
                            scope.launch { drawerState.close() }
                        },
                        onToggleExpanded = { folderId ->
                            viewModel.toggleFolderExpanded(folderId)
                        },
                    )
                }
            }
        },
        modifier = modifier,
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        IconButton(
                            onClick = onSearchClick,
                            modifier = Modifier.testTag("search_button"),
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
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
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error is BookmarkError.NotInitialized -> {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = uiState.error?.message ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                else -> {
                    FolderContent(
                        breadcrumbs = uiState.breadcrumbs,
                        folders = uiState.folders,
                        bookmarks = uiState.bookmarks,
                        syncRoot = uiState.syncRoot,
                        sortOrder = uiState.sortOrder,
                        showSyncBanner = uiState.showSyncBanner,
                        onFolderClick = { viewModel.navigateToFolder(it) },
                        onBookmarkClick = { bookmarkId ->
                            viewModel.loadBookmarkDetail(bookmarkId)
                            showDetailSheet = true
                        },
                        onBookmarkLongClick = { bookmarkId ->
                            contextMenuBookmarkId = bookmarkId
                        },
                        onBreadcrumbClick = { viewModel.navigateToFolder(it) },
                        onFolderRename = { renameFolderTarget = it },
                        onFolderDelete = { deleteFolderTarget = it },
                        onFolderMove = { moveItemTarget = MoveTarget.Folder(it.id) },
                        onBookmarkMove = { moveItemTarget = MoveTarget.Bookmark(it.id) },
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
                        onSortChange = { viewModel.setSortOrder(it) },
                        onDismissSyncBanner = { viewModel.dismissSyncBanner() },
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }

    if (showAddBookmarkDialog) {
        dev.jspade.mybriefcase.bookmarks.ui.bookmark.AddBookmarkDialog(
            onDismiss = {
                showAddBookmarkDialog = false
                viewModel.clearValidationError()
                viewModel.clearFaviconFetchState()
            },
            onConfirm = { url, title -> viewModel.addBookmark(url, title) },
            validationError = uiState.validationError,
            onValidationErrorClear = { viewModel.clearValidationError() },
            faviconFetchEnabled = faviconFetchEnabled,
            faviconFetchState = uiState.faviconFetchState,
            onFetchFavicon = { url -> viewModel.fetchFavicon(url) },
            syncRoot = uiState.syncRoot,
        )
    }

    if (showEditDialog && uiState.selectedBookmark != null) {
        dev.jspade.mybriefcase.bookmarks.ui.bookmark.EditBookmarkDialog(
            bookmark = uiState.selectedBookmark!!,
            navTree = uiState.navTree,
            currentFolderId = uiState.currentFolderId,
            onDismiss = {
                showEditDialog = false
                viewModel.clearSelectedBookmark()
                viewModel.clearValidationError()
                viewModel.clearFaviconFetchState()
            },
            onConfirm = { url, title, notes, newFolderId ->
                val faviconState = uiState.faviconFetchState
                if (faviconState is dev.jspade.mybriefcase.bookmarks.ui.bookmark.FaviconFetchState.Success) {
                    viewModel.saveFavicon(uiState.selectedBookmark!!.id, faviconState.filename)
                }
                viewModel.updateBookmarkAndMove(
                    uiState.selectedBookmark!!.id,
                    url,
                    title,
                    notes,
                    newFolderId,
                )
            },
            validationError = uiState.validationError,
            onValidationErrorClear = { viewModel.clearValidationError() },
            faviconFetchEnabled = faviconFetchEnabled,
            faviconFetchState = uiState.faviconFetchState,
            onFetchFavicon = { url -> viewModel.fetchFavicon(url) },
            onDeleteFavicon = { viewModel.deleteFavicon(uiState.selectedBookmark!!.id) },
            syncRoot = uiState.syncRoot,
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

    if (showDetailSheet && uiState.selectedBookmark != null) {
        BookmarkDetailSheetWithActions(
            bookmark = uiState.selectedBookmark!!,
            onDismiss = {
                showDetailSheet = false
                viewModel.clearSelectedBookmark()
            },
            onEdit = {
                showDetailSheet = false
                showEditDialog = true
            },
            onHistory = {
                showDetailSheet = false
                onHistoryClick?.invoke(uiState.selectedBookmark!!.id)
            },
            syncRoot = uiState.syncRoot,
        )
    }
}

sealed interface MoveTarget {
    val itemId: String

    data class Folder(
        override val itemId: String,
    ) : MoveTarget

    data class Bookmark(
        override val itemId: String,
    ) : MoveTarget
}

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
    syncRoot: String?,
    sortOrder: SortOrder,
    showSyncBanner: Boolean,
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
    onSortChange: (SortOrder) -> Unit,
    onDismissSyncBanner: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Sync banner
        if (showSyncBanner) {
            SyncBanner(onDismiss = onDismissSyncBanner)
        }

        // Breadcrumbs
        if (breadcrumbs.size > 1) {
            BreadcrumbBar(breadcrumbs = breadcrumbs, onClick = onBreadcrumbClick)
        }

        // Sort chip
        FolderSortChip(
            currentSort = sortOrder,
            onSortChange = onSortChange,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

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
                items(folders, key = { it.id }, contentType = { "folder" }) { folder ->
                    FolderListItem(
                        folder = folder,
                        onClick = { onFolderClick(folder.id) },
                        onRename = { onFolderRename(folder) },
                        onDelete = { onFolderDelete(folder) },
                        onMove = { onFolderMove(folder) },
                    )
                    HorizontalDivider()
                }
                items(bookmarks, key = { it.id }, contentType = { "bookmark" }) { bookmark ->
                    Box {
                        BookmarkListItem(
                            bookmark = bookmark,
                            syncRoot = syncRoot,
                            onClick = { onBookmarkClick(bookmark.id) },
                            onLongClick = { onBookmarkLongClick(bookmark.id) },
                        )
                        if (contextMenuBookmarkId == bookmark.id) {
                            DropdownMenu(
                                expanded = true,
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
                    HorizontalDivider()
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
    val scrollState = rememberScrollState()
    LaunchedEffect(breadcrumbs) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .horizontalScroll(scrollState),
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
                color =
                    if (index == breadcrumbs.lastIndex) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                modifier =
                    Modifier.clickable(enabled = index < breadcrumbs.lastIndex) {
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
            supportingContent = { Text("${folder.itemCount} " + if (folder.itemCount == 1u) "item" else "items") },
            leadingContent = {
                Icon(Icons.Default.Folder, contentDescription = null)
            },
            colors =
                ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                ),
            modifier =
                Modifier.combinedClickable(
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
    syncRoot: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(bookmark.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(
                bookmark.url,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.outline,
            )
        },
        leadingContent = {
            BookmarkFavicon(
                url = bookmark.url,
                favicon = bookmark.favicon,
                syncRoot = syncRoot,
            )
        },
        modifier =
            Modifier.combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    )
}

@Composable
private fun SyncBanner(onDismiss: () -> Unit) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("sync_banner"),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "To sync with other devices, add this folder to Syncthing-Fork",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss")
            }
        }
    }
}

@Composable
private fun FolderSortChip(
    currentSort: SortOrder,
    onSortChange: (SortOrder) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        FilterChip(
            selected = true,
            onClick = { expanded = true },
            label = { Text(currentSort.displayName()) },
            modifier = Modifier.testTag("folder_sort_chip"),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            SortOrder.entries.filter { it != SortOrder.RELEVANCE }.forEach { sort ->
                DropdownMenuItem(
                    text = { Text(sort.displayName()) },
                    onClick = {
                        onSortChange(sort)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun FolderNavTree(
    tree: FolderNavTreeDto,
    currentFolderId: String,
    expandedFolderIds: Set<String>,
    onFolderClick: (String) -> Unit,
    onToggleExpanded: (String) -> Unit,
) {
    val folderMap = remember(tree) { tree.folders.associateBy { it.id } }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        folderMap[tree.rootFolderId]?.let { root ->
            FolderNavNode(
                folder = root,
                folderMap = folderMap,
                currentFolderId = currentFolderId,
                expandedFolderIds = expandedFolderIds,
                depth = 0,
                onFolderClick = onFolderClick,
                onToggleExpanded = onToggleExpanded,
            )
        }
    }
}

@Composable
private fun FolderNavNode(
    folder: FolderNavDto,
    folderMap: Map<String, FolderNavDto>,
    currentFolderId: String,
    expandedFolderIds: Set<String>,
    depth: Int,
    onFolderClick: (String) -> Unit,
    onToggleExpanded: (String) -> Unit,
) {
    val hasChildren = folder.childFolderIds.isNotEmpty()
    val expanded = depth == 0 || folder.id in expandedFolderIds

    NavigationDrawerItem(
        label = { Text(folder.title) },
        selected = folder.id == currentFolderId,
        onClick = { onFolderClick(folder.id) },
        icon = { Icon(Icons.Default.Folder, contentDescription = null) },
        badge = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (depth > 0 && folder.itemCount > 0u) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.testTag("nav_folder_count_${folder.id}"),
                    ) {
                        Text(
                            text = folder.itemCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                if (hasChildren && depth > 0) {
                    IconButton(onClick = { onToggleExpanded(folder.id) }) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                        )
                    }
                }
            }
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
                            expandedFolderIds = expandedFolderIds,
                            depth = depth + 1,
                            onFolderClick = onFolderClick,
                            onToggleExpanded = onToggleExpanded,
                        )
                    }
                }
            }
        }
    }
}
