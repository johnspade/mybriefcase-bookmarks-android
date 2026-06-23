package dev.jspade.mybriefcase.bookmarks.ui.navigation

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.jspade.mybriefcase.bookmarks.BuildConfig
import dev.jspade.mybriefcase.bookmarks.MyBriefcaseApp
import dev.jspade.mybriefcase.bookmarks.ui.bookmark.BookmarkDetailSheetWithActions
import dev.jspade.mybriefcase.bookmarks.ui.bookmark.EditBookmarkDialog
import dev.jspade.mybriefcase.bookmarks.ui.folder.FolderScreen
import dev.jspade.mybriefcase.bookmarks.ui.folder.FolderViewModel
import dev.jspade.mybriefcase.bookmarks.ui.history.HistoryScreen
import dev.jspade.mybriefcase.bookmarks.ui.history.HistoryViewModel
import dev.jspade.mybriefcase.bookmarks.ui.search.SearchScreen
import dev.jspade.mybriefcase.bookmarks.ui.search.SearchViewModel
import dev.jspade.mybriefcase.bookmarks.ui.settings.SettingsScreen
import dev.jspade.mybriefcase.bookmarks.ui.wizard.StartupDecision
import dev.jspade.mybriefcase.bookmarks.ui.wizard.StartupDestination
import dev.jspade.mybriefcase.bookmarks.ui.wizard.SyncDirResolver
import dev.jspade.mybriefcase.bookmarks.ui.wizard.WizardScreen
import dev.jspade.mybriefcase.bookmarks.ui.wizard.WizardViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private enum class Screen {
    WIZARD,
    FOLDER,
    SEARCH,
    SETTINGS,
    HISTORY,
}

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val initialScreen =
        remember {
            when (StartupDecision.decide(context)) {
                StartupDestination.WIZARD -> Screen.WIZARD
                StartupDestination.FOLDER -> Screen.FOLDER
            }
        }
    var currentScreen by remember { mutableStateOf(initialScreen) }

    if (currentScreen == Screen.WIZARD) {
        val wizardViewModel: WizardViewModel = viewModel()
        WizardScreen(
            viewModel = wizardViewModel,
            onComplete = {
                MyBriefcaseApp.instance.initFromWizard()
                currentScreen = Screen.FOLDER
            },
            modifier = modifier,
        )
        return
    }

    val folderViewModel: FolderViewModel = viewModel()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> {
                        folderViewModel.refresh()
                        folderViewModel.startPolling()
                    }
                    Lifecycle.Event.ON_PAUSE -> {
                        folderViewModel.stopPolling()
                    }
                    else -> {}
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val uiState by folderViewModel.uiState.collectAsState()

    var showDetailSheet by remember { mutableStateOf(false) }
    var showEditFromSearch by remember { mutableStateOf(false) }
    var historyBookmarkId by remember { mutableStateOf("") }
    var pendingSyncDirChange by remember { mutableStateOf<String?>(null) }

    // SAF directory picker for changing sync dir
    val syncDirLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree(),
        ) { uri: Uri? ->
            uri?.let {
                val path = SyncDirResolver.resolveTreeUri(it)
                if (path != null) {
                    pendingSyncDirChange = path
                } else {
                    Toast.makeText(context, "Please select a folder on internal storage", Toast.LENGTH_SHORT).show()
                }
            }
        }

    // SAF file picker for import
    val importLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri: Uri? ->
            uri?.let {
                val html =
                    context.contentResolver
                        .openInputStream(it)
                        ?.bufferedReader()
                        ?.readText()
                if (html != null) {
                    folderViewModel.importHtml(html)
                }
            }
        }

    // SAF file picker for export
    val exportLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("text/html"),
        ) { uri: Uri? ->
            uri?.let {
                val html = uiState.exportedHtml
                if (html != null) {
                    context.contentResolver.openOutputStream(it)?.use { os ->
                        os.write(html.toByteArray())
                    }
                    folderViewModel.clearExportedHtml()
                    Toast.makeText(context, "Bookmarks exported", Toast.LENGTH_SHORT).show()
                }
            }
        }

    when (currentScreen) {
        Screen.FOLDER -> {
            FolderScreen(
                viewModel = folderViewModel,
                onSettingsClick = { currentScreen = Screen.SETTINGS },
                onSearchClick = { currentScreen = Screen.SEARCH },
                onHistoryClick = { bookmarkId ->
                    historyBookmarkId = bookmarkId
                    currentScreen = Screen.HISTORY
                },
                modifier = modifier,
                faviconFetchEnabled = MyBriefcaseApp.instance.faviconSettings.fetchEnabled,
            )
        }
        Screen.SEARCH -> {
            val searchViewModel: SearchViewModel = viewModel()
            SearchScreen(
                viewModel = searchViewModel,
                syncRoot = uiState.syncRoot,
                onBookmarkClick = { bookmarkId ->
                    folderViewModel.loadBookmarkDetail(bookmarkId)
                    showDetailSheet = true
                },
                onBack = { currentScreen = Screen.FOLDER },
                modifier = modifier,
            )
        }
        Screen.SETTINGS -> {
            val faviconSettings = MyBriefcaseApp.instance.faviconSettings
            SettingsScreen(
                syncDir = MyBriefcaseApp.instance.syncDir,
                clientId = MyBriefcaseApp.instance.clientId,
                appVersion = BuildConfig.VERSION_NAME,
                onBack = { currentScreen = Screen.FOLDER },
                onChangeSyncDir = { syncDirLauncher.launch(null) },
                onImport = {
                    importLauncher.launch(arrayOf("text/html"))
                },
                onExport = {
                    folderViewModel.exportHtml()
                    val suggestedName = "bookmarks_${LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}.html"
                    exportLauncher.launch(suggestedName)
                },
                faviconFetchEnabled = faviconSettings.fetchEnabled,
                useDuckDuckGo = faviconSettings.useDuckDuckGo,
                onFaviconFetchEnabledChange = { faviconSettings.setFetchEnabled(it) },
                onUseDuckDuckGoChange = { faviconSettings.setUseDuckDuckGo(it) },
            )
        }
        Screen.WIZARD -> {} // handled above with early return
        Screen.HISTORY -> {
            val historyViewModel: HistoryViewModel = viewModel()
            val historyState by historyViewModel.uiState.collectAsState()

            androidx.compose.runtime.LaunchedEffect(historyBookmarkId) {
                historyViewModel.loadHistory(historyBookmarkId)
            }

            if (historyState.reverted) {
                folderViewModel.loadBookmarkDetail(historyBookmarkId)
                folderViewModel.navigateToFolder(uiState.currentFolderId)
                currentScreen = Screen.FOLDER
            } else {
                HistoryScreen(
                    entries = historyState.entries,
                    isLoading = historyState.isLoading,
                    onRevert = { changeHash ->
                        historyViewModel.revertBookmark(historyBookmarkId, changeHash)
                    },
                    onBack = { currentScreen = Screen.FOLDER },
                    modifier = modifier,
                )
            }
        }
    }

    // Import result handling
    uiState.importResult?.let { result ->
        Toast
            .makeText(
                context,
                "Imported ${result.bookmarksImported} bookmarks and ${result.foldersImported} folders",
                Toast.LENGTH_LONG,
            ).show()
        folderViewModel.clearImportResult()
    }

    // Bookmark detail bottom sheet (from search screen)
    if (showDetailSheet && uiState.selectedBookmark != null) {
        BookmarkDetailSheetWithActions(
            bookmark = uiState.selectedBookmark!!,
            onDismiss = {
                showDetailSheet = false
                folderViewModel.clearSelectedBookmark()
            },
            onEdit = {
                showDetailSheet = false
                showEditFromSearch = true
            },
            onHistory = {
                showDetailSheet = false
                historyBookmarkId = uiState.selectedBookmark!!.id
                currentScreen = Screen.HISTORY
            },
        )
    }

    if (showEditFromSearch && uiState.selectedBookmark != null) {
        EditBookmarkDialog(
            bookmark = uiState.selectedBookmark!!,
            navTree = uiState.navTree,
            currentFolderId = uiState.currentFolderId,
            onDismiss = {
                showEditFromSearch = false
                folderViewModel.clearSelectedBookmark()
            },
            onConfirm = { url, title, notes, newFolderId ->
                showEditFromSearch = false
                folderViewModel.updateBookmarkAndMove(
                    uiState.selectedBookmark!!.id,
                    url,
                    title,
                    notes,
                    newFolderId,
                )
            },
        )
    }

    pendingSyncDirChange?.let { newPath ->
        AlertDialog(
            onDismissRequest = { pendingSyncDirChange = null },
            title = { Text("Change sync directory?") },
            text = { Text("The app will use the new location. Existing data will not be moved.") },
            confirmButton = {
                TextButton(onClick = {
                    MyBriefcaseApp.instance.changeSyncDir(newPath)
                    val intent =
                        context.packageManager
                            .getLaunchIntentForPackage(context.packageName)!!
                            .addFlags(
                                android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                                    android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK,
                            )
                    context.startActivity(intent)
                    Runtime.getRuntime().exit(0)
                }) {
                    Text("Change")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingSyncDirChange = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}
