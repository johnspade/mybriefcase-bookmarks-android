package dev.jspade.mybriefcase.bookmarks

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dev.jspade.mybriefcase.bookmarks.ui.navigation.AppNavHost
import dev.jspade.mybriefcase.bookmarks.ui.theme.MyBriefcaseBookmarksTheme

class MainActivity : ComponentActivity() {
    private var showStorageRationale by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestStoragePermissionIfNeeded()

        setContent {
            MyBriefcaseBookmarksTheme {
                AppNavHost(modifier = Modifier.fillMaxSize())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Check storage permission result when coming back from Settings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            showStorageRationale = false
        }
    }

    private fun requestStoragePermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            val intent =
                Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:$packageName"),
                )
            startActivity(intent)
        }
    }
}
