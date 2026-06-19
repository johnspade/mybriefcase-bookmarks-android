package dev.jspade.mybriefcase.bookmarks.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.jspade.mybriefcase.bookmarks.data.BookmarkError
import uniffi.mybriefcase_bookmarks_ffi.BookmarkDto
import uniffi.mybriefcase_bookmarks_ffi.SortOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    modifier: Modifier = Modifier,
    onBookmarkClick: (String) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val error by viewModel.error.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(error) {
        val err = error ?: return@LaunchedEffect
        when (err) {
            is BookmarkError.IoError -> {
                val result =
                    snackbarHostState.showSnackbar(
                        message = err.message,
                        actionLabel = "Retry",
                        duration = SnackbarDuration.Long,
                    )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.setQuery(viewModel.query.value)
                }
            }
            else -> {
                snackbarHostState.showSnackbar(
                    message = err.message,
                    duration = SnackbarDuration.Long,
                )
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = query,
                        onValueChange = { viewModel.setQuery(it) },
                        placeholder = { Text("Search bookmarks...") },
                        singleLine = true,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .testTag("search_input"),
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.clearSearch() },
                                    modifier = Modifier.testTag("clear_search"),
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("back_button"),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            // Sort chip
            SortChip(
                currentSort = sortOrder,
                onSortChange = { viewModel.setSortOrder(it) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            when {
                query.isBlank() -> {
                    // No query yet — show nothing
                }
                results.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No results for '$query'",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("empty_state"),
                        )
                    }
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(results, key = { it.id }) { bookmark ->
                            SearchResultItem(
                                bookmark = bookmark,
                                onClick = { onBookmarkClick(bookmark.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SortChip(
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
            modifier = Modifier.testTag("sort_chip"),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            SortOrder.entries.forEach { sort ->
                DropdownMenuItem(
                    text = { Text(sort.displayName()) },
                    onClick = {
                        onSortChange(sort)
                        expanded = false
                    },
                    modifier = Modifier.testTag("sort_option_${sort.name}"),
                )
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    bookmark: BookmarkDto,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(bookmark.title) },
        supportingContent = { Text(bookmark.url) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

fun SortOrder.displayName(): String =
    when (this) {
        SortOrder.NAME_ASC -> "Name A-Z"
        SortOrder.NAME_DESC -> "Name Z-A"
        SortOrder.DATE_DESC -> "Date newest"
        SortOrder.DATE_ASC -> "Date oldest"
        SortOrder.RELEVANCE -> "Relevance"
    }
