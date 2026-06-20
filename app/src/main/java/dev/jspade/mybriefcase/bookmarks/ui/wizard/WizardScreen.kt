package dev.jspade.mybriefcase.bookmarks.ui.wizard

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.launch

private const val PAGE_COUNT = 3
private const val SYNCTHING_PACKAGE = "com.github.catfriend1.syncthingfork"
private const val SYNCTHING_FDROID_URL = "https://f-droid.org/packages/com.github.catfriend1.syncthingfork/"

@Composable
fun WizardScreen(
    viewModel: WizardViewModel,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    if (uiState.isComplete) {
        onComplete()
        return
    }

    val dirPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree(),
        ) { uri: Uri? ->
            uri?.let { viewModel.onDirectorySelected(it) }
        }

    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier =
                    Modifier
                        .weight(1f)
                        .testTag("wizard_pager"),
            ) { page ->
                when (page) {
                    0 -> WelcomeSlide()
                    1 -> SyncthingSlide(context)
                    2 ->
                        DirectorySlide(
                            selectedPath = uiState.selectedPath,
                            error = uiState.error,
                            onChooseDirectory = { dirPickerLauncher.launch(null) },
                        )
                }
            }

            // Bottom bar: [Back] dots [Next/Done]
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .testTag("wizard_bottom_bar"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (pagerState.currentPage > 0) {
                    TextButton(
                        onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                        modifier = Modifier.testTag("wizard_back"),
                    ) {
                        Text("Back")
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }

                // Dot indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("wizard_indicators"),
                ) {
                    repeat(PAGE_COUNT) { index ->
                        Box(
                            modifier =
                                Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (index == pagerState.currentPage) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant
                                        },
                                    ),
                        )
                    }
                }

                if (pagerState.currentPage < PAGE_COUNT - 1) {
                    FilledTonalButton(
                        onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                        modifier = Modifier.testTag("wizard_next"),
                    ) {
                        Text("Next")
                    }
                } else {
                    FilledTonalButton(
                        onClick = { viewModel.finish() },
                        enabled = uiState.canFinish,
                        modifier = Modifier.testTag("wizard_done"),
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeSlide() {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(32.dp)
                .testTag("wizard_slide_welcome"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(id = dev.jspade.mybriefcase.bookmarks.R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(96.dp),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Welcome to MyBriefcase Bookmarks",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "A local-first bookmark manager that syncs across devices using Syncthing.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SyncthingSlide(context: android.content.Context) {
    val isInstalled =
        try {
            context.packageManager.getPackageInfo(SYNCTHING_PACKAGE, 0)
            true
        } catch (_: Exception) {
            false
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(32.dp)
                .testTag("wizard_slide_syncthing"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Sync,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Syncthing keeps your bookmarks in sync across devices.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, SYNCTHING_FDROID_URL.toUri())
                context.startActivity(intent)
            },
            modifier = Modifier.testTag("wizard_install_syncthing"),
        ) {
            Text("Install Syncthing-Fork")
        }
        Spacer(modifier = Modifier.height(16.dp))
        AssistChip(
            onClick = {},
            label = {
                Text(if (isInstalled) "Installed" else "Not detected")
            },
            colors =
                if (isInstalled) {
                    AssistChipDefaults.assistChipColors(
                        labelColor = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    AssistChipDefaults.assistChipColors()
                },
            modifier = Modifier.testTag("wizard_syncthing_status"),
        )
    }
}

@Composable
private fun DirectorySlide(
    selectedPath: String?,
    error: String?,
    onChooseDirectory: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(32.dp)
                .testTag("wizard_slide_directory"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Card(
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Your Syncthing folders are typically in Internal storage/Syncthing/",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp),
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onChooseDirectory,
            modifier = Modifier.testTag("wizard_choose_directory"),
        ) {
            Text("Choose Directory")
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (selectedPath != null) {
            Text(
                text = selectedPath,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("wizard_selected_path"),
            )
        }
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("wizard_error"),
            )
        }
    }
}
