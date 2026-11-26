import { parseLrc } from '../utils/lrcParser'

export async function searchLrcLib(query) {
  if (!query || !query.trim()) return []
  try {
    const encoded = encodeURIComponent(query.trim())
    const res = await fetch(`https://lrclib.net/api/search?q=${encoded}`)
    if (!res.ok) return []
    const data = await res.json()
    if (!Array.isArray(data)) return []

    return data.map(item => {
      const rawLrc = item.syncedLyrics || item.plainLyrics || ''
      const doc = parseLrc(rawLrc, item.trackName || 'Unknown', item.artistName || 'Unknown')
      return {
        trackTitle: item.trackName || 'Unknown Track',
        artistName: item.artistName || 'Unknown Artist',
        albumName: item.albumName || '',
        durationMs: (item.duration || 180) * 1000,
        syncType: item.syncedLyrics ? 'SYNCED' : 'PLAIN',
        lines: doc.lines,
        plainText: item.plainLyrics || '',
        providerId: 'lrclib_web',
        providerDisplayName: 'LRCLIB Web'
      }
    })
  } catch (err) {
    console.error('LRCLIB search error:', err)
    return []
  }
}
