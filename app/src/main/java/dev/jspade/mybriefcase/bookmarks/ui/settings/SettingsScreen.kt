package dev.jspade.mybriefcase.bookmarks.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    syncDir: String,
    clientId: String,
    appVersion: String,
    onBack: () -> Unit,
    onChangeSyncDir: () -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
        ) {
            // Sync section
            Text(
                text = "Sync",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag("settings_sync_header"),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Sync directory",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = syncDir,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("settings_sync_dir"),
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onChangeSyncDir,
                modifier = Modifier.testTag("settings_change_sync_dir"),
            ) {
                Text("Change")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Client ID",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = clientId,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("settings_client_id"),
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Import/Export section
            Text(
                text = "Import / Export",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag("settings_import_export_header"),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onImport,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag("settings_import_button"),
            ) {
                Text("Import bookmarks")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onExport,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag("settings_export_button"),
            ) {
                Text("Export bookmarks")
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // About section
            Text(
                text = "About",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag("settings_about_header"),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "MyBriefcase Bookmarks",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Version $appVersion",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("settings_version"),
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
