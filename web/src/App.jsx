import React, { useState, useEffect, useRef } from 'react'
import { ImmersiveStage } from './components/ImmersiveStage'
import { SettingsDrawer } from './components/SettingsDrawer'
import { findActiveLineIndex } from './utils/lrcParser'
import { createMediaSessionListener } from './services/mediaSessionListener'

export default function App() {
  const [audioSource, setAudioSource] = useState('SPOTIFY') // SPOTIFY | CUSTOM
  const [customDocument, setCustomDocument] = useState(null)
  const [externalTrack, setExternalTrack] = useState({ title: 'Spotify / YouTube Music', artist: 'Ready for lyrics' })
  const [isPlaying, setIsPlaying] = useState(true)
  const [positionMs, setPositionMs] = useState(0)
  const [isSettingsOpen, setIsSettingsOpen] = useState(false)

  const [preferences, setPreferences] = useState({
    fontFamily: 'PLUS_JAKARTA_SANS',
    fontSizeSp: 32,
    colorPreset: 'ALBUM_ART',
    alignment: 'CENTER',
    nextLineVisible: true,
    wordByWordEnabled: true,
    glassBlurEnabled: true,
    transitionStyle: 'APPLE_GLASS',
    transitionSpeed: 'BALANCED',
    showTrackIntroOnStart: true,
    keepScreenOn: true
  })

  const activeDocument = customDocument
  const activeTitle = externalTrack.title
  const activeArtist = externalTrack.artist
  const activeColors = [0xE5A243, 0x7CD4D8, 0xF4EFE6]
  const durationMs = customDocument ? (customDocument.lines[customDocument.lines.length - 1]?.timestampMs || 180000) + 5000 : 180000

  const activeLineIndex = findActiveLineIndex(activeDocument?.lines || [], positionMs)

  // Media Session listener for Spotify Web or YouTube Music browser tabs / page media
  useEffect(() => {
    const listener = createMediaSessionListener(
      (title, artist, playing, fetchedDoc) => {
        setExternalTrack({ title, artist })
        setIsPlaying(playing)
        if (fetchedDoc) {
          setCustomDocument(fetchedDoc)
        }
      },
      (pos, playing) => {
        setPositionMs(pos)
        setIsPlaying(playing)
      }
    )

    listener.start()
    return () => listener.stop()
  }, [])

  // Playback timer loop when syncing with active media elements
  const lastTickRef = useRef(Date.now())
  useEffect(() => {
    if (!isPlaying) {
      lastTickRef.current = Date.now()
      return
    }

    const interval = setInterval(() => {
      const now = Date.now()
      const delta = now - lastTickRef.current
      lastTickRef.current = now

      setPositionMs(prev => {
        const next = prev + delta
        if (next >= durationMs) {
          return 0
        }
        return next
      })
    }, 50)

    return () => clearInterval(interval)
  }, [isPlaying, durationMs])

  const handleUpdatePreference = (key, value) => {
    setPreferences(prev => ({ ...prev, [key]: value }))
  }

  const handleLoadDocument = (doc) => {
    setCustomDocument(doc)
    setPositionMs(0)
    setIsPlaying(true)
    setIsSettingsOpen(false)
  }

  return (
    <div className="relative w-screen h-screen overflow-hidden bg-black font-jakarta">
      {/* Immersive Full-Window Lyrics Stage */}
      <ImmersiveStage
        document={activeDocument}
        activeLineIndex={activeLineIndex}
        positionMs={positionMs}
        isPlaying={isPlaying}
        preferences={preferences}
        trackTitle={activeTitle}
        artistName={activeArtist}
        albumLightColors={activeColors}
        onOpenSettings={() => setIsSettingsOpen(true)}
        audioSource={audioSource}
        onChangeAudioSource={(src) => setAudioSource(src)}
        onOpenSearch={() => setIsSettingsOpen(true)}
      />

      {/* Collapsible Studio Settings Drawer */}
      <SettingsDrawer
        isOpen={isSettingsOpen}
        onClose={() => setIsSettingsOpen(false)}
        preferences={preferences}
        onUpdatePreference={handleUpdatePreference}
        onLoadDocument={handleLoadDocument}
      />
    </div>
  )
}
