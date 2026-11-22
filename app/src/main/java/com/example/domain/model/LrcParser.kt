package com.example.domain.model

object LrcParser {

    private val TIME_TAG_REGEX = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")
    private val META_TAG_REGEX = Regex("""^\[([a-zA-Z]+):([^]]*)]$""")

    fun parse(rawLrc: String): List<LyricLine> {
        if (rawLrc.isBlank()) return emptyList()

        var globalOffsetMs = 0L
        val rawEntries = mutableListOf<Pair<Long, String>>()

        rawLrc.lineSequence().forEach { rawLine ->
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) return@forEach

            val timeMatches = TIME_TAG_REGEX.findAll(trimmed).toList()
            if (timeMatches.isEmpty()) {
                val metaMatch = META_TAG_REGEX.matchEntire(trimmed)
                if (metaMatch != null && metaMatch.groupValues[1].equals("offset", ignoreCase = true)) {
                    globalOffsetMs = metaMatch.groupValues[2].trim().toLongOrNull() ?: 0L
                }
                return@forEach
            }

            val lastMatchEnd = timeMatches.last().range.last + 1
            val lyricText = trimmed.substring(lastMatchEnd).trim()
            val displayText = if (lyricText.isEmpty()) "♪" else lyricText

            timeMatches.forEach { match ->
                val minutes = match.groupValues[1].toLongOrNull() ?: 0L
                val seconds = match.groupValues[2].toLongOrNull() ?: 0L
                val fractionStr = match.groupValues[3]
                val millis = when (fractionStr.length) {
                    0 -> 0L
                    1 -> (fractionStr.toLongOrNull() ?: 0L) * 100L
                    2 -> (fractionStr.toLongOrNull() ?: 0L) * 10L
                    else -> fractionStr.take(3).toLongOrNull() ?: 0L
                }
                val timestamp = ((minutes * 60_000L) + (seconds * 1_000L) + millis + globalOffsetMs)
                    .coerceAtLeast(0L)
                rawEntries.add(timestamp to displayText)
            }
        }

        return rawEntries
            .sortedBy { it.first }
            .mapIndexed { idx, (time, text) ->
                LyricLine(index = idx, timestampMs = time, text = text)
            }
    }

    fun toLrcString(lines: List<LyricLine>): String {
        return lines.joinToString("\n") { line ->
            val totalSeconds = line.timestampMs / 1000L
            val minutes = totalSeconds / 60L
            val seconds = totalSeconds % 60L
            val centis = (line.timestampMs % 1000L) / 10L
            "[%02d:%02d.%02d]%s".format(minutes, seconds, centis, line.text)
        }
    }

    fun findActiveLineIndex(lines: List<LyricLine>, effectivePositionMs: Long): Int {
        if (lines.isEmpty()) return -1
        if (effectivePositionMs < lines.first().timestampMs) return -1

        var low = 0
        var high = lines.lastIndex
        var result = -1
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (lines[mid].timestampMs <= effectivePositionMs) {
                result = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return result
    }

    fun computeNextTransitionDelayMs(
        lines: List<LyricLine>,
        activeIndex: Int,
        effectivePositionMs: Long,
        playbackSpeed: Float,
    ): Long? {
        val nextIndex = activeIndex + 1
        if (nextIndex !in lines.indices) return null
        val deltaSongMs = (lines[nextIndex].timestampMs - effectivePositionMs).coerceAtLeast(16L)
        val safeSpeed = playbackSpeed.coerceIn(0.25f, 3.0f)
        return (deltaSongMs / safeSpeed).toLong().coerceAtLeast(16L)
    }
}
