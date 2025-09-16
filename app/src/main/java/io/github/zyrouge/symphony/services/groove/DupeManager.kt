package io.github.zyrouge.symphony.services.groove

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import io.github.zyrouge.symphony.Symphony
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.collections.set

class DupeManager(val symphony: Symphony) {
    var count by mutableIntStateOf(0) //TODO: dupe count is (probably) counted wrong
    val similarityMap = mutableStateMapOf<String, SnapshotStateList<String>>()
    val coroutineScope = CoroutineScope(Dispatchers.Default)
    var isDone = false
    var isRefreshing = false

    private fun insert(id: String, id2: String) {

        val min = minOf(id, id2)
        val max = if (id == min) id2 else id
        var list: SnapshotStateList<String>
        for(simList in similarityMap.values) {
            if(simList.contains(id) || simList.contains(id2)) {
                list = simList
            }
        }
        if (similarityMap[id] != null) {
            list = similarityMap[id]!!
        } else if (similarityMap[id2] != null) {
            list = similarityMap[id2]!!
        } else {
            similarityMap[min] = mutableStateListOf(min)
            count += 1
            list = similarityMap[min]!!
        }
        if (!list.contains(max)) {
            list.add(max)
            count += 1
        }
    }

    fun fetchDupes(songIds: List<String>, force: Boolean = false) {
        if(force) {
            isRefreshing = false
            isDone = false
            count = 0
            similarityMap.clear()
        }
        if(isRefreshing) {
            return
        }
        if(isDone || songIds.isEmpty()){
            isRefreshing = false
            return
        }
        coroutineScope.launch {
            isRefreshing = true
            songIds.forEachIndexed { i, songId ->
                val song = symphony.groove.song.get(songId)
                if (song == null) {
                    return@forEachIndexed
                }
                songIds.forEachIndexed { j, songId2 ->
                    val song2 = symphony.groove.song.get(songId2)
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
            isRefreshing = false
            isDone = true
        }
    }
}
