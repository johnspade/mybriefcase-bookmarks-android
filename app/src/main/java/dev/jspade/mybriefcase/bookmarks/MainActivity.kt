package dev.jspade.mybriefcase.bookmarks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dev.jspade.mybriefcase.bookmarks.ui.navigation.AppNavHost
import dev.jspade.mybriefcase.bookmarks.ui.theme.MyBriefcaseBookmarksTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyBriefcaseBookmarksTheme {
                AppNavHost(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
