package io.github.zyrouge.symphony.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.zyrouge.symphony.services.groove.Groove
import io.github.zyrouge.symphony.services.groove.Song
import io.github.zyrouge.symphony.ui.helpers.ViewContext
import io.github.zyrouge.symphony.ui.view.SettingsViewRoute
import io.github.zyrouge.symphony.ui.view.settings.GrooveSettingsViewRoute
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DupeList(
    context: ViewContext,
    songIds: List<String>,
    trailingOptionsContent: (@Composable ColumnScope.(Int, Song, () -> Unit) -> Unit)? = null,
    cardThumbnailLabel: (@Composable (Int, Song) -> Unit)? = null,
    cardThumbnailLabelStyle: SongCardThumbnailLabelStyle = SongCardThumbnailLabelStyle.Default,
    disableHeartIcon: Boolean = false,
) {
    val count = remember(context.symphony.groove.dupes.count) {
        context.symphony.groove.dupes.count
    }
    val isRefreshing = remember(context.symphony.groove.dupes.isRefreshing) {
        context.symphony.groove.dupes.isRefreshing
    }
    val similarityMap = context.symphony.groove.dupes.similarityMap
    val pullRefreshState = rememberPullToRefreshState()
    val coroutineScope = rememberCoroutineScope()
    val refreshDupes: () -> Unit = {
        coroutineScope.launch {
            context.symphony.groove.dupes.fetchDupes(songIds, force = true)
        }
    }

    LaunchedEffect(similarityMap) {
        context.symphony.groove.dupes.fetchDupes(songIds, force = false)
    }

    MediaSortBarScaffold(
        mediaSortBar = {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("$count Dupes", Modifier.weight(4f), textAlign = TextAlign.Center)
            }
        },
        content = {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = refreshDupes,
                state = pullRefreshState,
                contentAlignment = Alignment.Center,
            ) {
                when {
                    similarityMap.isEmpty() ->
                        IconTextBody(
                            icon = { modifier ->
                                Icon(Icons.Filled.MusicNote, null, modifier = modifier)
                            },
                            content = {
                                Text("There are no dupes!")
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Re-open this menu when your library has loaded or when you have duplicates",
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
                        )
                    else -> {
                        val lazyListState = rememberLazyListState()
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.drawScrollBar(lazyListState)
                        ) {
                            itemsIndexed(
                                similarityMap.keys.toList(),
                                key = { i, x -> "$i-$x" },
                                contentType = { _, _ -> Groove.Kind.SONG }
                            ) { i, ii ->
                                Text(
                                    i.toString(),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
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
                                            //TODO: implement ability that removes the song from the dupe list if deleted
                                            //playing probably deleted songs is a bad choice
//                                        context.symphony.radio.shorty.playQueue(
//                                            similarityMap[ii]!!.toList(),
//                                            Radio.PlayOptions(index = j)
//                                        )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        }
    )
}