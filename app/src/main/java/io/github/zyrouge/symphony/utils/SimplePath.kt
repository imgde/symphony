package io.github.zyrouge.symphony.utils

import androidx.compose.runtime.Immutable
import kotlin.text.split

@Immutable
class SimplePath(val parts: List<String>) {
    constructor(path: String) : this(n(p(path)))
    constructor(path: String, vararg subParts: String) : this(n(p(path) + p(*subParts)))
    constructor(path: SimplePath, vararg subParts: String) : this(n(path.parts + p(*subParts)))

    val name get() = parts.last()
    val nameWithoutExtension get() = name.substringBeforeLast(".")
    val extension get() = name.substringAfterLast(".", "")
    private var _parent: SimplePath? = null
    val parent get() = _parent
    val size get() = parts.size
    val pathString get() = parts.joinToString("/")

    init {
        if(size > 1) {
            _parent = SimplePath(parts.subList(0, parts.lastIndex))
        }
    }

    fun join(vararg nParts: String) = SimplePath(this, *nParts)

    override fun toString() = pathString

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SimplePath
        return pathString == other.pathString
    }

    override fun hashCode(): Int {
        var result = parts.hashCode()
        result = 31 * result + (_parent?.hashCode() ?: 0)
        result = 31 * result + size
        result = 31 * result + name.hashCode()
        result = 31 * result + nameWithoutExtension.hashCode()
        result = 31 * result + extension.hashCode()
        result = 31 * result + (parent?.hashCode() ?: 0)
        result = 31 * result + pathString.hashCode()
        return result
    }

    companion object {
        private fun p(vararg path: String) = path.fold(listOf<String>()) { prev, curr ->
            prev + curr.split("/", "\\")
        }

        private fun n(parts: List<String>): List<String> {
            val normalized = mutableListOf<String>()
            for (x in parts) {
                when {
                    x.isEmpty() -> {}
                    x == "." -> {}
                    x == ".." -> normalized.removeAt(normalized.lastIndex)
                    else -> normalized.add(x)
                }
            }
            return normalized
        }
    }
}
