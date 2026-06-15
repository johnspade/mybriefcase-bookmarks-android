package dev.jspade.mybriefcase.bookmarks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.jspade.mybriefcase.bookmarks.ui.theme.MyBriefcaseBookmarksTheme
import uniffi.mybriefcase_bookmarks_ffi.listFolders

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyBriefcaseBookmarksTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BookmarkScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun BookmarkScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val folders = remember {
        try {
            listFolders(context.filesDir.absolutePath)
        } catch (e: Exception) {
            emptyList()
        }
    }
    Column(modifier = modifier) {
        Text("Folders from Rust core: ${folders.size}")
        folders.forEach { folder ->
            Text("  - ${folder.title}")
        }
    }
}
