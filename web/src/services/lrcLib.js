import { parseLrc } from '../utils/lrcParser'

function mapLrcLibItem(item) {
  const rawLrc = item.syncedLyrics || item.plainLyrics || ''
  const doc = parseLrc(rawLrc, item.trackName || 'Unknown Track', item.artistName || 'Unknown Artist')
  return {
    id: item.id,
    trackTitle: item.trackName || doc.trackTitle || 'Unknown Track',
    artistName: item.artistName || doc.artistName || 'Unknown Artist',
    albumName: item.albumName || '',
    durationMs: (item.duration || 180) * 1000,
    syncType: item.syncedLyrics && doc.lines.length > 0 ? 'SYNCED' : 'PLAIN',
    lines: doc.lines,
    plainText: item.plainLyrics || rawLrc || '',
    providerId: 'lrclib_web',
    providerDisplayName: 'LRCLIB Web'
  }
}

export async function searchLrcLib(query) {
  if (!query || !query.trim()) return []
  try {
    const encoded = encodeURIComponent(query.trim())
    const res = await fetch(`https://lrclib.net/api/search?q=${encoded}`)
    if (!res.ok) return []
    const data = await res.json()
    if (!Array.isArray(data)) return []

    const mapped = data.map(mapLrcLibItem)
    // Prefer synced lyrics over plain-only results while keeping relative order
    return mapped.sort((a, b) => {
      if (a.syncType === b.syncType) return 0
      return a.syncType === 'SYNCED' ? -1 : 1
    })
  } catch (err) {
    console.error('LRCLIB search error:', err)
    return []
  }
}

export async function findBestLrcLibMatch({ title, artist, album = '', durationSeconds = null }) {
  if (!title || !title.trim()) return null

  try {
    const params = new URLSearchParams({
      track_name: title.trim(),
      artist_name: (artist || '').trim()
    })
    if (album && album.trim()) params.set('album_name', album.trim())
    if (durationSeconds && Number.isFinite(durationSeconds) && durationSeconds > 0) {
      params.set('duration', String(Math.round(durationSeconds)))
    }

    if (artist && artist.trim()) {
      const exactRes = await fetch(`https://lrclib.net/api/get?${params.toString()}`)
      if (exactRes.ok) {
        const exactItem = await exactRes.json()
        if (exactItem && (exactItem.syncedLyrics || exactItem.plainLyrics)) {
          return mapLrcLibItem(exactItem)
        }
      }
    }
  } catch (err) {
    console.warn('LRCLIB exact lookup fallback to search:', err)
  }

  const results = await searchLrcLib(`${title} ${artist || ''}`.trim())
  if (results.length === 0) return null

  if (durationSeconds && Number.isFinite(durationSeconds) && durationSeconds > 0) {
    const targetMs = durationSeconds * 1000
    const synced = results.filter((r) => r.syncType === 'SYNCED')
    const pool = synced.length > 0 ? synced : results
    return pool.slice().sort((a, b) => Math.abs(a.durationMs - targetMs) - Math.abs(b.durationMs - targetMs))[0]
  }

  return results[0]
}
