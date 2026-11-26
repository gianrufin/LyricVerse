import { searchLrcLib } from './lrcLib'
import { parseLrc } from '../utils/lrcParser'

export function createMediaSessionListener(onUpdateTrack, onUpdatePosition) {
  let timer = null
  let lastTitle = ''
  let lastArtist = ''
  let lastFetchedTitle = ''

  const pollMedia = async () => {
    try {
      // 1. Check navigator.mediaSession if supported
      let title = ''
      let artist = ''
      let position = 0
      let isPlaying = true

      if ('mediaSession' in navigator && navigator.mediaSession.metadata) {
        title = navigator.mediaSession.metadata.title || ''
        artist = navigator.mediaSession.metadata.artist || ''
      }

      // 2. Fallback: inspect any active audio/video element on the page
      const mediaEl = document.querySelector('video, audio')
      if (mediaEl) {
        position = (mediaEl.currentTime || 0) * 1000
        isPlaying = !mediaEl.paused
      }

      if (title && (title !== lastTitle || artist !== lastArtist)) {
        lastTitle = title
        lastArtist = artist
        onUpdateTrack(title, artist, isPlaying)

        // Automatically fetch lyrics from LRCLIB if title changed
        if (title !== lastFetchedTitle) {
          lastFetchedTitle = title
          const query = `${title} ${artist}`.trim()
          const results = await searchLrcLib(query)
          if (results.length > 0) {
            onUpdateTrack(title, artist, isPlaying, results[0])
          }
        }
      }

      if (mediaEl) {
        onUpdatePosition(position, isPlaying)
      }
    } catch (err) {
      console.error('MediaSession polling error:', err)
    }
  }

  return {
    start() {
      if (timer) clearInterval(timer)
      timer = setInterval(pollMedia, 800)
    },
    stop() {
      if (timer) clearInterval(timer)
      timer = null
    }
  }
}
