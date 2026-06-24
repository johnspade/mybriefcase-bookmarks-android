package dev.jspade.mybriefcase.bookmarks

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.jspade.mybriefcase.bookmarks.ui.share.ShareReceiverScreen
import dev.jspade.mybriefcase.bookmarks.ui.share.ShareReceiverViewModel
import dev.jspade.mybriefcase.bookmarks.ui.theme.MyBriefcaseBookmarksTheme
import dev.jspade.mybriefcase.bookmarks.ui.wizard.StartupDecision

class ShareReceiverActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val extraText = intent?.getStringExtra(Intent.EXTRA_TEXT)
        val extraSubject = intent?.getStringExtra(Intent.EXTRA_SUBJECT)
        val isInitialized = StartupDecision.getPersistedSyncDir(this) != null

        val app = MyBriefcaseApp.instance
        val syncDir = if (isInitialized) app.syncDir else null

        val viewModel =
            ShareReceiverViewModel(
                repository = app.repository,
                extraText = extraText,
                extraSubject = extraSubject,
                syncDirPath = syncDir,
                faviconFetchEnabled = if (isInitialized) app.faviconSettings.fetchEnabled else false,
            )

        setContent {
            MyBriefcaseBookmarksTheme {
                ShareReceiverScreen(
                    viewModel = viewModel,
                    onFinish = { finish() },
                    onRedirectToWizard = {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    },
                    syncRoot = syncDir,
                )
            }
        }
    }
}
