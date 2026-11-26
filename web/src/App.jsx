import React, { useState, useEffect, useRef } from 'react'
import { ImmersiveStage } from './components/ImmersiveStage'
import { SettingsDrawer } from './components/SettingsDrawer'
import { DemoTransport } from './components/DemoTransport'
import { DEMO_TRACKS } from './utils/demoTracks'
import { findActiveLineIndex } from './utils/lrcParser'

export default function App() {
  const [currentTrackIndex, setCurrentTrackIndex] = useState(0)
  const [customDocument, setCustomDocument] = useState(null)
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

  const currentTrack = DEMO_TRACKS[currentTrackIndex]
  const activeDocument = customDocument || currentTrack.document
  const activeTitle = customDocument ? customDocument.trackTitle : currentTrack.trackTitle
  const activeArtist = customDocument ? customDocument.artistName : currentTrack.artistName
  const activeColors = customDocument ? [0xE5A243, 0x7CD4D8] : currentTrack.albumLightColors
  const durationMs = customDocument ? (customDocument.lines[customDocument.lines.length - 1]?.timestampMs || 120000) + 5000 : currentTrack.durationMs

  const activeLineIndex = findActiveLineIndex(activeDocument.lines, positionMs)

  // Playback timer loop
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
          return 0 // loop
        }
        return next
      })
    }, 50)

    return () => clearInterval(interval)
  }, [isPlaying, durationMs])

  const handleUpdatePreference = (key, value) => {
    setPreferences(prev => ({ ...prev, [key]: value }))
  }

  const handleStartDemoTrack = (index) => {
    setCurrentTrackIndex(index)
    setCustomDocument(null)
    setPositionMs(0)
    setIsPlaying(true)
    setIsSettingsOpen(false)
  }

  const handleLoadDocument = (doc) => {
    setCustomDocument(doc)
    setPositionMs(0)
    setIsPlaying(true)
    setIsSettingsOpen(false)
  }

  const handleNextTrack = () => {
    if (customDocument) {
      setCustomDocument(null)
    } else {
      setCurrentTrackIndex((currentTrackIndex + 1) % DEMO_TRACKS.length)
    }
    setPositionMs(0)
    setIsPlaying(true)
  }

  const handlePrevTrack = () => {
    if (customDocument) {
      setCustomDocument(null)
    } else {
      setCurrentTrackIndex((currentTrackIndex - 1 + DEMO_TRACKS.length) % DEMO_TRACKS.length)
    }
    setPositionMs(0)
    setIsPlaying(true)
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
      />

      {/* Floating Demo Transport Controls */}
      <DemoTransport
        trackTitle={activeTitle}
        artistName={activeArtist}
        isPlaying={isPlaying}
        positionMs={positionMs}
        durationMs={durationMs}
        onTogglePlayPause={() => setIsPlaying(!isPlaying)}
        onSeek={(pos) => setPositionMs(pos)}
        onNext={handleNextTrack}
        onPrev={handlePrevTrack}
      />

      {/* Collapsible Studio Settings Drawer */}
      <SettingsDrawer
        isOpen={isSettingsOpen}
        onClose={() => setIsSettingsOpen(false)}
        preferences={preferences}
        onUpdatePreference={handleUpdatePreference}
        onLoadDocument={handleLoadDocument}
        demoTracks={DEMO_TRACKS}
        onStartDemoTrack={handleStartDemoTrack}
      />
    </div>
  )
}
