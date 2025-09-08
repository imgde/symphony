package io.github.zyrouge.symphony.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.zyrouge.symphony.services.groove.Groove
import io.github.zyrouge.symphony.services.groove.Song
import io.github.zyrouge.symphony.services.radio.Radio
import io.github.zyrouge.symphony.ui.helpers.ViewContext
import io.github.zyrouge.symphony.ui.view.SettingsViewRoute
import io.github.zyrouge.symphony.ui.view.settings.GrooveSettingsViewRoute

@Composable
fun DupeList(
    context: ViewContext,
    songIds: List<String>,
    leadingContent: (LazyListScope.() -> Unit)? = null,
    trailingContent: (LazyListScope.() -> Unit)? = null,
    trailingOptionsContent: (@Composable ColumnScope.(Int, Song, () -> Unit) -> Unit)? = null,
    cardThumbnailLabel: (@Composable (Int, Song) -> Unit)? = null,
    cardThumbnailLabelStyle: SongCardThumbnailLabelStyle = SongCardThumbnailLabelStyle.Default,
    disableHeartIcon: Boolean = false,
    enableAddMediaFoldersHint: Boolean = false,
) {
    var count by remember { mutableIntStateOf(0) }
    val similarityMap = remember { mutableStateMapOf<String, SnapshotStateList<String>>() }

    LaunchedEffect(count, similarityMap) {
        fun insert(id: String, id2: String) {

            val min = minOf(id, id2)
            val max = if (id == min) id2 else id
            var list: SnapshotStateList<String>

            if (similarityMap[id] != null) {
                list = similarityMap[id]!!
            } else if (similarityMap[id2] != null) {
                list = similarityMap[id2]!!
            } else {
                similarityMap[min] = mutableStateListOf<String>(min)
                count += 1
                list = similarityMap[min]!!
            }
            if (!list.contains(max)) {
                list.add(max)
                count += 1
            }
        }

        songIds.forEachIndexed { i, songId ->
            val song = context.symphony.groove.song.get(songId)
            if (song == null) {
                return@forEachIndexed
            }
            songIds.forEachIndexed { j, songId2 ->
                val song2 = context.symphony.groove.song.get(songId2)
                if (song2 == null) {
                    return@forEachIndexed
                }
                if (song.id == song2.id) {
                    return@forEachIndexed
                }
                if (song.title == song2.title && song.duration == song2.duration) {
                    insert(songId, songId2)
                }
            }
        }
    }

    MediaSortBarScaffold(
        mediaSortBar = {
            Text("$count Dupes", Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        },
        content = {
            when {
                similarityMap.isEmpty() -> IconTextBody(
                    icon = { modifier ->
                        Icon(Icons.Filled.MusicNote, null, modifier = modifier)
                    },
                    content = {
                        Text(context.symphony.t.DamnThisIsSoEmpty)
                        if (enableAddMediaFoldersHint) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                context.symphony.t.HintAddMediaFolders,
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .clickable {
                                        context.navController.navigate(
                                            GrooveSettingsViewRoute(SettingsViewRoute.ELEMENT_MEDIA_FOLDERS)
                                        )
                                    }
                                    .padding(2.dp),
                            )
                        }
                    }
                )

                else -> {
                    val lazyListState = rememberLazyListState()

                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.drawScrollBar(lazyListState)
                    ) {
                        leadingContent?.invoke(this)
                        itemsIndexed(
                            similarityMap.keys.toList(),
                            key = { i, x -> "$i-$x" },
                            contentType = { _, _ -> Groove.Kind.SONG }
                        ) { i, ii ->
                            Text(i.toString())
                            similarityMap[ii]!!.forEachIndexed { j, songId ->
                                context.symphony.groove.song.get(songId)?.let { song ->
                                    SongCard(
                                        context,
                                        song = song,
                                        thumbnailLabel = cardThumbnailLabel?.let {
                                            { it(i, song) }
                                        },
                                        thumbnailLabelStyle = cardThumbnailLabelStyle,
                                        disableHeartIcon = disableHeartIcon,
                                        trailingOptionsContent = trailingOptionsContent?.let {
                                            { onDismissRequest -> it(i, song, onDismissRequest) }
                                        },
                                    ) {
                                        context.symphony.radio.shorty.playQueue(
                                            similarityMap[ii]!!.toList(),
                                            Radio.PlayOptions(index = j)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                        trailingContent?.invoke(this)
                    }
                }
            }
        }
    )
}