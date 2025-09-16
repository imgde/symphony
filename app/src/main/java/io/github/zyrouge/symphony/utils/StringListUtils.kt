package io.github.zyrouge.symphony.utils

object StringListUtils {
    enum class SortBy {
        CUSTOM,
        NAME,
    }

    fun sort(values: List<String>, by: SortBy, reverse: Boolean): List<String> {
        val sorted = when (by) {
            SortBy.CUSTOM -> values
            SortBy.NAME -> values.sorted()
        }
        return if (reverse) sorted.reversed() else sorted
    }

    fun <T, R: Comparable<R>> sort(values: List<T>, keyExtractor: (T) -> R, by: SortBy, reverse: Boolean): List<T> {
        val sorted: List<T> = when (by) {
            SortBy.CUSTOM -> values
            SortBy.NAME -> values.sortedBy(keyExtractor)
        }
        return if (reverse) sorted.reversed() else sorted
    }
}
