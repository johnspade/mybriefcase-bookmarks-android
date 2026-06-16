package dev.jspade.mybriefcase.bookmarks.ui.navigation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.jspade.mybriefcase.bookmarks.MyBriefcaseApp
import dev.jspade.mybriefcase.bookmarks.ui.bookmark.BookmarkDetailSheet
import dev.jspade.mybriefcase.bookmarks.ui.folder.FolderScreen
import dev.jspade.mybriefcase.bookmarks.ui.folder.FolderViewModel
import dev.jspade.mybriefcase.bookmarks.ui.settings.SettingsScreen
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private enum class Screen {
    FOLDER,
    SETTINGS,
}

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val folderViewModel: FolderViewModel = viewModel()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
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
    var currentScreen by remember { mutableStateOf(Screen.FOLDER) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by folderViewModel.uiState.collectAsState()

    var showDetailSheet by remember { mutableStateOf(false) }

    // SAF file picker for import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val html = context.contentResolver.openInputStream(it)?.bufferedReader()?.readText()
            if (html != null) {
                folderViewModel.importHtml(html)
            }
        }
    }

    // SAF file picker for export
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/html")
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
                onBookmarkClick = { bookmarkId ->
                    folderViewModel.loadBookmarkDetail(bookmarkId)
                    showDetailSheet = true
                },
                onSettingsClick = { currentScreen = Screen.SETTINGS },
                modifier = modifier,
            )
        }
        Screen.SETTINGS -> {
            SettingsScreen(
                syncDir = MyBriefcaseApp.SYNC_ROOT,
                clientId = "device-client",
                appVersion = "1.0",
                onBack = { currentScreen = Screen.FOLDER },
                onImport = {
                    importLauncher.launch(arrayOf("text/html"))
                },
                onExport = {
                    folderViewModel.exportHtml()
                    val suggestedName = "bookmarks_${LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}.html"
                    exportLauncher.launch(suggestedName)
                },
            )
        }
    }

    // Import result handling
    uiState.importResult?.let { result ->
        Toast.makeText(
            context,
            "Imported ${result.bookmarksImported} bookmarks and ${result.foldersImported} folders",
            Toast.LENGTH_LONG,
        ).show()
        folderViewModel.clearImportResult()
    }

    // Bookmark detail bottom sheet
    if (showDetailSheet && uiState.selectedBookmark != null) {
        BookmarkDetailSheet(
            bookmark = uiState.selectedBookmark!!,
            onDismiss = {
                showDetailSheet = false
                folderViewModel.clearSelectedBookmark()
            },
            onEdit = {
                showDetailSheet = false
                // Edit is handled in FolderScreen via the edit dialog trigger
            },
            onOpenInBrowser = { url ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            },
            onShare = { url ->
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, url)
                }
                context.startActivity(Intent.createChooser(intent, "Share URL"))
            },
            onCopyUrl = { url ->
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("URL", url))
                Toast.makeText(context, "URL copied", Toast.LENGTH_SHORT).show()
            },
        )
    }
}
