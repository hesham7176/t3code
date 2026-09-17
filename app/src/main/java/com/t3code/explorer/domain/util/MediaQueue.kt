package com.t3code.explorer.domain.util

/** Playback queue model. Pure Kotlin so queue/next/previous behaviour can be unit tested. */
class MediaQueue<T>(
    items: List<T> = emptyList(),
    startIndex: Int = 0,
    var repeatMode: RepeatMode = RepeatMode.OFF,
    var shuffleEnabled: Boolean = false
) {
    enum class RepeatMode { OFF, ONE, ALL }

    private val entries: List<T> = items.toList()
    private var index: Int = if (entries.isEmpty()) -1 else startIndex.coerceIn(0, entries.lastIndex)

    val size: Int get() = entries.size
    val currentIndex: Int get() = index
    val isEmpty: Boolean get() = entries.isEmpty()

    fun current(): T? = entries.getOrNull(index)

    fun indexOf(predicate: (T) -> Boolean): Int = entries.indexOfFirst(predicate)

    fun moveTo(target: Int) {
        if (target in 0..entries.lastIndex) index = target
    }

    /** Next index to play, or null when playback should stop at the end of the queue. */
    fun nextIndex(): Int? = when {
        entries.isEmpty() -> null
        shuffleEnabled && entries.size > 1 -> entries.indices.filter { it != index }.random()
        index < entries.lastIndex -> index + 1
        repeatMode == RepeatMode.ALL -> 0
        else -> null
    }

    /** Previous index to play, or null when the queue should stay at the first item. */
    fun previousIndex(): Int? = when {
        entries.isEmpty() -> null
        index > 0 -> index - 1
        repeatMode == RepeatMode.ALL -> entries.lastIndex
        else -> null
    }

    fun hasNext(): Boolean = nextIndex() != null
    fun hasPrevious(): Boolean = previousIndex() != null

    /** Moves the cursor to the next item and returns it, or null when the queue is exhausted. */
    fun advanceNext(): T? = nextIndex()?.also { index = it }?.let { entries[it] }

    /** Moves the cursor to the previous item and returns it, or null when already at the start. */
    fun advancePrevious(): T? = previousIndex()?.also { index = it }?.let { entries[it] }
}
