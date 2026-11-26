export function parseLrc(rawText, trackTitle = 'Unknown Track', artistName = 'Unknown Artist') {
  if (!rawText || typeof rawText !== 'string') {
    return {
      trackTitle,
      artistName,
      syncType: 'NONE',
      lines: [],
      plainText: rawText || '',
      offsetMs: 0
    }
  }

  const lines = []
  const rawLines = rawText.split(/\r?\n/)
  let hasSynced = false
  let parsedTitle = trackTitle
  let parsedArtist = artistName
  let parsedOffsetMs = 0

  const timeTagGlobalRegex = /\[(\d{1,3}):(\d{2})(?:\.(\d{1,3}))?\]/g
  const metadataRegex = /^\[(ti|ar|al|offset):([^\]]*)\]$/i

  for (let i = 0; i < rawLines.length; i++) {
    const trimmed = (rawLines[i] || '').trim()
    if (!trimmed) continue

    const metaMatch = metadataRegex.exec(trimmed)
    if (metaMatch) {
      const key = metaMatch[1].toLowerCase()
      const val = metaMatch[2].trim()
      if (key === 'ti' && val) parsedTitle = val
      if (key === 'ar' && val) parsedArtist = val
      if (key === 'offset' && !Number.isNaN(Number(val))) parsedOffsetMs = Number(val)
      continue
    }

    timeTagGlobalRegex.lastIndex = 0
    let match
    const timestamps = []
    let cleanText = trimmed

    while ((match = timeTagGlobalRegex.exec(trimmed)) !== null) {
      hasSynced = true
      const mins = parseInt(match[1], 10)
      const secs = parseInt(match[2], 10)
      const millisStr = match[3] || '0'
      let millis = 0
      if (millisStr.length === 1) millis = parseInt(millisStr, 10) * 100
      else if (millisStr.length === 2) millis = parseInt(millisStr, 10) * 10
      else millis = parseInt(millisStr.slice(0, 3), 10)

      const totalMs = Math.max(0, mins * 60 * 1000 + secs * 1000 + millis + parsedOffsetMs)
      timestamps.push(totalMs)
      cleanText = cleanText.replace(match[0], '').trim()
    }

    if (timestamps.length > 0) {
      timestamps.forEach((tMs) => {
        lines.push({
          timestampMs: tMs,
          rawCleanText: cleanText,
          text: cleanText.replace(/<[^>]+>/g, '').trim(),
          words: []
        })
      })
    }
  }

  lines.sort((a, b) => a.timestampMs - b.timestampMs)

  // Build word-level timings after sorting so fallback durations use actual gap to next line
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i]
    line.index = i
    const nextTimestamp = i < lines.length - 1 ? lines[i + 1].timestampMs : line.timestampMs + 3500
    const lineDuration = Math.min(6000, Math.max(1200, nextTimestamp - line.timestampMs))

    const wordRegex = /([^<]+)<(\d{1,3}):(\d{2})(?:\.(\d{1,3}))?>/g
    let wordMatch
    const words = []

    while ((wordMatch = wordRegex.exec(line.rawCleanText)) !== null) {
      const wText = wordMatch[1].trim()
      if (!wText) continue
      const wMins = parseInt(wordMatch[2], 10)
      const wSecs = parseInt(wordMatch[3], 10)
      const wMillisStr = wordMatch[4] || '0'
      let wMillis = 0
      if (wMillisStr.length === 1) wMillis = parseInt(wMillisStr, 10) * 100
      else if (wMillisStr.length === 2) wMillis = parseInt(wMillisStr, 10) * 10
      else wMillis = parseInt(wMillisStr.slice(0, 3), 10)

      const wStartMs = Math.max(0, wMins * 60 * 1000 + wSecs * 1000 + wMillis + parsedOffsetMs)
      words.push({ text: wText, startMs: wStartMs, endMs: wStartMs + 400 })
    }

    if (words.length === 0 && line.text) {
      const wordParts = line.text.split(/\s+/).filter(Boolean)
      const perWord = lineDuration / Math.max(wordParts.length, 1)
      wordParts.forEach((w, wIdx) => {
        const wStart = line.timestampMs + wIdx * perWord
        words.push({ text: w, startMs: wStart, endMs: wStart + perWord })
      })
    }

    line.words = words
    delete line.rawCleanText
  }

  return {
    trackTitle: parsedTitle,
    artistName: parsedArtist,
    syncType: hasSynced && lines.length > 0 ? 'SYNCED' : 'PLAIN',
    lines,
    plainText: rawText,
    offsetMs: parsedOffsetMs
  }
}

export function findActiveLineIndex(lines, positionMs) {
  if (!lines || lines.length === 0) return -1
  let activeIdx = -1
  for (let i = 0; i < lines.length; i++) {
    if (lines[i].timestampMs <= positionMs + 120) {
      activeIdx = i
    } else {
      break
    }
  }
  return activeIdx
}
