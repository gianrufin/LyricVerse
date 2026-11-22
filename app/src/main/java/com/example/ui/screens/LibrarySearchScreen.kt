package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.LyricsCacheEntity
import com.example.domain.model.LyricDocument
import com.example.domain.model.LyricSyncType
import com.example.ui.LyricVerseUiState
import com.example.ui.theme.LyricActive
import com.example.ui.theme.StageAmber
import com.example.ui.theme.StageObsidian
import com.example.ui.theme.StageSurfaceElevated
import com.example.ui.theme.SuccessMint
import com.example.ui.theme.TextMuted
import java.util.concurrent.TimeUnit

@Composable
fun LibrarySearchScreen(
    uiState: LyricVerseUiState,
    onQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onLoadDocument: (LyricDocument) -> Unit,
    onToggleOnlineLookup: (Boolean) -> Unit,
    onToggleOfflineCache: (Boolean) -> Unit,
    onClearCache: () -> Unit,
    onDeleteCacheEntry: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val library = uiState.searchLibrary
    val prefs = uiState.syncState.preferences

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("library_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Lyrics Catalog & Cache",
                style = MaterialTheme.typography.headlineMedium,
                color = LyricActive
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Search the Demo LRC catalog or LRCLIB public API when an external player omits metadata.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = library.query,
                    onValueChange = onQueryChange,
                    label = { Text("Artist or song title") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearchSubmit() }),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_input")
                )
                Button(
                    onClick = onSearchSubmit,
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("search_button")
                ) {
                    Icon(Icons.Rounded.Search, contentDescription = "Search lyrics")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Find")
                }
            }
        }

        if (library.isSearching) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }
            }
        }

        if (library.results.isNotEmpty()) {
            item {
                Text(
                    text = "Search Results (${library.results.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = LyricActive
                )
            }
            items(library.results) { doc ->
                SearchResultCard(
                    doc = doc,
                    onLoad = { onLoadDocument(doc) }
                )
            }
        } else if (library.hasSearched && !library.isSearching) {
            item {
                Surface(
                    color = StageSurfaceElevated,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No matching lyrics found for \"${library.query}\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Provider & Offline Cache Controls",
                style = MaterialTheme.typography.titleMedium,
                color = LyricActive
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StageSurfaceElevated),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "LRCLIB Online Lookup",
                                style = MaterialTheme.typography.titleSmall,
                                color = LyricActive
                            )
                            Text(
                                text = "Sends only track title, artist, album & duration with a LyricVerse User-Agent.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                        Switch(
                            checked = prefs.onlineLookupEnabled,
                            onCheckedChange = onToggleOnlineLookup,
                            modifier = Modifier.testTag("online_lookup_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Permitted Offline Cache",
                                style = MaterialTheme.typography.titleSmall,
                                color = LyricActive
                            )
                            Text(
                                text = "Caches lyrics locally for 14 days (3 days for misses) only when provider terms allow.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                        Switch(
                            checked = prefs.offlineCacheEnabled,
                            onCheckedChange = onToggleOfflineCache,
                            modifier = Modifier.testTag("offline_cache_switch")
                        )
                    }

                    OutlinedButton(
                        onClick = onClearCache,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clear_cache_button")
                    ) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear Cached Lyrics (${library.cachedEntries.size})")
                    }
                }
            }
        }

        if (library.cachedEntries.isNotEmpty()) {
            item {
                Text(
                    text = "Cached Tracks",
                    style = MaterialTheme.typography.titleMedium,
                    color = LyricActive
                )
            }
            items(library.cachedEntries, key = { it.trackKey }) { entry ->
                CachedEntryRow(
                    entry = entry,
                    onLoad = {
                        if (!entry.isNotFound) {
                            onLoadDocument(entry.toDomainDocument())
                        }
                    },
                    onDelete = { onDeleteCacheEntry(entry.trackKey) }
                )
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    doc: LyricDocument,
    onLoad: () -> Unit,
) {
    Surface(
        color = StageSurfaceElevated,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Lyrics,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = doc.trackTitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = LyricActive,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${doc.artistName} · ${doc.providerDisplayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val tagColor = if (doc.syncType == LyricSyncType.SYNCED) SuccessMint else StageAmber
                    Text(
                        text = if (doc.syncType == LyricSyncType.SYNCED) "Time-Synced LRC" else "Plain Lyrics",
                        style = MaterialTheme.typography.labelSmall,
                        color = tagColor
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onLoad) {
                Text("Load")
            }
        }
    }
}

@Composable
private fun CachedEntryRow(
    entry: LyricsCacheEntity,
    onLoad: () -> Unit,
    onDelete: () -> Unit,
) {
    val remainingDays = TimeUnit.MILLISECONDS.toDays(
        (entry.expiresAtEpochMs - System.currentTimeMillis()).coerceAtLeast(0L)
    )
    Surface(
        color = StageObsidian.copy(alpha = 0.75f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Cached,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = entry.trackTitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = LyricActive,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${entry.artistName} · ${if (entry.isNotFound) "Negative cache" else entry.syncType} · Expires in ${remainingDays}d",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (!entry.isNotFound) {
                OutlinedButton(onClick = onLoad) {
                    Text("Open")
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete cached entry",
                    tint = TextMuted
                )
            }
        }
    }
}
