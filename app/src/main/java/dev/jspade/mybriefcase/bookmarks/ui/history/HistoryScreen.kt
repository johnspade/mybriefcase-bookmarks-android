package dev.jspade.mybriefcase.bookmarks.ui.history

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import uniffi.mybriefcase_bookmarks_ffi.BookmarkHistoryEntryDto
import uniffi.mybriefcase_bookmarks_ffi.FieldChangeDto
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    entries: List<BookmarkHistoryEntryDto>,
    isLoading: Boolean,
    onRevert: (changeHash: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        modifier = modifier,
    ) { padding ->
        var confirmingEntry by remember { mutableStateOf<BookmarkHistoryEntryDto?>(null) }

        confirmingEntry?.let { entry ->
            RevertConfirmationDialog(
                entry = entry,
                onConfirm = {
                    onRevert(entry.changeHash)
                    confirmingEntry = null
                },
                onDismiss = { confirmingEntry = null },
            )
        }

        when {
            isLoading -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .testTag("history_loading"),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            entries.isEmpty() -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No history entries yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(entries) { index, entry ->
                        HistoryEntryCard(
                            entry = entry,
                            index = index,
                            onRevert = { confirmingEntry = entry },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryEntryCard(
    entry: BookmarkHistoryEntryDto,
    index: Int,
    onRevert: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("history_entry_$index"),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = formatTimestamp(entry.timestamp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = entry.changeHash.take(8),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            entry.changedFields.forEach { change ->
                FieldChangeLine(change)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onRevert,
                modifier = Modifier.testTag("revert_button_$index"),
            ) {
                Text("Revert to this version")
            }
        }
    }
}

@Composable
private fun FieldChangeLine(change: FieldChangeDto) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = change.field,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        if (change.oldValue != null) {
            Text(
                text = change.oldValue!!,
                style = MaterialTheme.typography.bodySmall,
                textDecoration = TextDecoration.LineThrough,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (change.newValue != null) {
            Text(
                text = change.newValue!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun RevertConfirmationDialog(
    entry: BookmarkHistoryEntryDto,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("revert_confirm_dialog"),
        title = { Text("Revert to this version?") },
        text = {
            Column {
                Text(
                    text = "This will restore the following values:",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                entry.changedFields.forEach { change ->
                    if (change.oldValue != null) {
                        Text(
                            text = "${change.field}: ${change.oldValue}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag("revert_confirm_button"),
            ) {
                Text("Revert")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

private const val MILLIS_THRESHOLD = 1_000_000_000_000L
private const val MILLIS_PER_SECOND = 1000L

@Suppress("SwallowedException")
private fun formatTimestamp(millis: Long): String =
    try {
        val secs = if (millis > MILLIS_THRESHOLD) millis / MILLIS_PER_SECOND else millis
        val instant = Instant.ofEpochSecond(secs)
        val formatter =
            DateTimeFormatter
                .ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (e: Exception) {
        "Unknown date"
    }
