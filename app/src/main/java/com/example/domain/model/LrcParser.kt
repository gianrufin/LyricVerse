package com.example.domain.model

object LrcParser {

    private val TIME_TAG_REGEX = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")
    private val WORD_TAG_REGEX = Regex("""<(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?>""")
    private val META_TAG_REGEX = Regex("""^\[([a-zA-Z]+):([^]]*)]$""")
    private val WHITESPACE_REGEX = Regex("""\s+""")

    fun parse(rawLrc: String): List<LyricLine> {
        if (rawLrc.isBlank()) return emptyList()

        var globalOffsetMs = 0L
        val rawEntries = mutableListOf<Triple<Long, String, List<LyricWord>>>()

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
            val rawLyricBody = trimmed.substring(lastMatchEnd).trim()

            timeMatches.forEach { match ->
                val timestamp = parseMatchMillis(match, globalOffsetMs)
                val (cleanText, explicitWords) = extractEnhancedWords(
                    rawBody = rawLyricBody,
                    lineTimestampMs = timestamp,
                    globalOffsetMs = globalOffsetMs
                )
                val displayText = if (cleanText.isEmpty()) "♪" else cleanText
                rawEntries.add(Triple(timestamp, displayText, explicitWords))
            }
        }

        val sorted = rawEntries.sortedBy { it.first }
        return sorted.mapIndexed { idx, (time, text, explicitWords) ->
            val nextTime = sorted.getOrNull(idx + 1)?.first
            val lineEndMs = if (nextTime != null && nextTime > time) {
                minOf(nextTime, time + 5_200L)
            } else {
                time + 3_500L
            }
            val resolvedWords = if (explicitWords.isNotEmpty()) {
                explicitWords
            } else {
                buildProportionalWords(text = text, startMs = time, endMs = lineEndMs)
            }
            LyricLine(
                index = idx,
                timestampMs = time,
                text = text,
                words = resolvedWords,
            )
        }
    }

    fun buildProportionalWords(text: String, startMs: Long, endMs: Long): List<LyricWord> {
        val tokens = text.trim().split(WHITESPACE_REGEX).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return emptyList()
        if (tokens.size == 1) {
            return listOf(LyricWord(text = tokens[0], startMs = startMs, endMs = maxOf(endMs, startMs + 250L)))
        }

        val totalSpanMs = (endMs - startMs).coerceAtLeast(tokens.size * 180L)
        val weights = tokens.map { token -> (token.length.coerceAtLeast(1) + 1.5) }
        val totalWeight = weights.sum().coerceAtLeast(1.0)

        var cursorMs = startMs
        return tokens.mapIndexed { idx, token ->
            val fraction = weights[idx] / totalWeight
            val sliceMs = (totalSpanMs * fraction).toLong().coerceAtLeast(140L)
            val wordStart = cursorMs
            val wordEnd = if (idx == tokens.lastIndex) {
                maxOf(wordStart + 160L, endMs)
            } else {
                (wordStart + sliceMs).coerceAtMost(endMs)
            }
            cursorMs = wordEnd
            LyricWord(text = token, startMs = wordStart, endMs = wordEnd)
        }
    }

    private fun extractEnhancedWords(
        rawBody: String,
        lineTimestampMs: Long,
        globalOffsetMs: Long,
    ): Pair<String, List<LyricWord>> {
        val wordMatches = WORD_TAG_REGEX.findAll(rawBody).toList()
        if (wordMatches.isEmpty()) {
            return rawBody to emptyList()
        }

        val parsedSegments = mutableListOf<Pair<Long, String>>()
        val prefix = rawBody.substring(0, wordMatches.first().range.first).trim()
        if (prefix.isNotEmpty()) {
            parsedSegments.add(lineTimestampMs to prefix)
        }

        for (i in wordMatches.indices) {
            val match = wordMatches[i]
            val segStart = match.range.last + 1
            val segEnd = if (i + 1 < wordMatches.size) wordMatches[i + 1].range.first else rawBody.length
            val segText = rawBody.substring(segStart, segEnd).trim()
            if (segText.isNotEmpty()) {
                val wordTime = parseMatchMillis(match, globalOffsetMs)
                parsedSegments.add(wordTime to segText)
            }
        }

        val cleanText = parsedSegments.joinToString(" ") { it.second }.trim()
        val words = mutableListOf<LyricWord>()
        for (i in parsedSegments.indices) {
            val (segStartMs, segText) = parsedSegments[i]
            val segEndMs = parsedSegments.getOrNull(i + 1)?.first?.takeIf { it > segStartMs }
                ?: (segStartMs + 550L)
            val subTokens = segText.split(WHITESPACE_REGEX).filter { it.isNotEmpty() }
            if (subTokens.size <= 1) {
                words.add(LyricWord(text = segText, startMs = segStartMs, endMs = segEndMs))
            } else {
                words.addAll(buildProportionalWords(segText, segStartMs, segEndMs))
            }
        }
        return cleanText to words
    }

    private fun parseMatchMillis(match: MatchResult, globalOffsetMs: Long): Long {
        val minutes = match.groupValues[1].toLongOrNull() ?: 0L
        val seconds = match.groupValues[2].toLongOrNull() ?: 0L
        val fractionStr = match.groupValues[3]
        val millis = when (fractionStr.length) {
            0 -> 0L
            1 -> (fractionStr.toLongOrNull() ?: 0L) * 100L
            2 -> (fractionStr.toLongOrNull() ?: 0L) * 10L
            else -> fractionStr.take(3).toLongOrNull() ?: 0L
        }
        return ((minutes * 60_000L) + (seconds * 1_000L) + millis + globalOffsetMs)
            .coerceAtLeast(0L)
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
