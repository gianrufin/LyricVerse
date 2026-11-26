export function parseLrc(rawText, trackTitle = "Unknown Track", artistName = "Unknown Artist") {
  if (!rawText || typeof rawText !== 'string') {
    return {
      trackTitle,
      artistName,
      syncType: 'NONE',
      lines: [],
      plainText: rawText || ''
    }
  }

  const lines = []
  const rawLines = rawText.split(/\r?\n/)
  let hasSynced = false

  // Regex to match [mm:ss.xx] or [mm:ss.xxx] or [mm:ss]
  const timeTagRegex = /\[(\d{2}):(\d{2})(?:\.(\d{2,3}))?\]/g

  for (let i = 0; i < rawLines.length; i++) {
    const line = rawLines.item ? rawLines.item(i) : rawLines[i]
    const trimmed = line.trim()
    if (!trimmed) continue

    // Find all time tags in this line
    let match
    const timestamps = []
    let cleanText = trimmed

    while ((match = timeTagRegex.exec(trimmed)) !== null) {
      hasSynced = true
      const mins = parseInt(match[1], 10)
      const secs = parseInt(match[2], 10)
      const millisStr = match[3] || '0'
      const millis = millisStr.length === 2 ? parseInt(millisStr, 10) * 10 : parseInt(millisStr, 10)
      const totalMs = (mins * 60 * 1000) + (secs * 1000) + millis
      timestamps.push(totalMs)
      cleanText = cleanText.replace(match[0], '').trim()
    }

    if (timestamps.length > 0) {
      // Parse word-level timing if present: word <mm:ss.xx>
      const words = []
      const wordRegex = /([^<]+)<(\d{2}):(\d{2})(?:\.(\d{2,3}))?>/g
      let wordMatch
      let hasWords = false

      while ((wordMatch = wordRegex.exec(cleanText)) !== null) {
        hasWords = true
        const wText = wordMatch[1].trim()
        const wMins = parseInt(wordMatch[2], 10)
        const wSecs = parseInt(wordMatch[3], 10)
        const wMillisStr = wordMatch[4] || '0'
        const wMillis = wMillisStr.length === 2 ? parseInt(wMillisStr, 10) * 10 : parseInt(wMillisStr, 10)
        const wStartMs = (wMins * 60 * 1000) + (wSecs * 1000) + wMillis
        words.push({ text: wText, startMs: wStartMs, endMs: wStartMs + 400 })
      }

      if (!hasWords && cleanText) {
        // Fallback: split words evenly across line duration
        const wordParts = cleanText.split(/\s+/)
        const approxDuration = 3000
        const perWord = approxDuration / Math.max(wordParts.length, 1)
        wordParts.forEach((w, wIdx) => {
          const wStart = timestamps[0] + (wIdx * perWord)
          words.push({ text: w, startMs: wStart, endMs: wStart + perWord })
        })
      }

      timestamps.forEach(tMs => {
        lines.push({
          timestampMs: tMs,
          text: cleanText.replace(/<[^>]+>/g, '').trim(),
          words
        })
      })
    }
  }

  lines.sort((a, b) => a.timestampMs - b.timestampMs)
  // Assign index
  lines.forEach((l, idx) => { l.index = idx })

  return {
    trackTitle,
    artistName,
    syncType: hasSynced && lines.length > 0 ? 'SYNCED' : 'PLAIN',
    lines,
    plainText: rawText
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
