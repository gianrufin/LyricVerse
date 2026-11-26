import React, { useState, useEffect, useRef } from 'react'
import { ImmersiveStage } from './components/ImmersiveStage'
import { SettingsDrawer } from './components/SettingsDrawer'
import { DemoTransport } from './components/DemoTransport'
import { DEMO_TRACKS } from './utils/demoTracks'
import { findActiveLineIndex } from './utils/lrcParser'
import { createMediaSessionListener } from './services/mediaSessionListener'

export default function App() {
  const [audioSource, setAudioSource] = useState('DEMO') // DEMO | SPOTIFY | YOUTUBE | CUSTOM
  const [currentTrackIndex, setCurrentTrackIndex] = useState(0)
  const [customDocument, setCustomDocument] = useState(null)
  const [externalTrack, setExternalTrack] = useState({ title: '', artist: '' })
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
  const activeDocument = audioSource === 'CUSTOM' && customDocument ? customDocument : currentTrack.document
  const activeTitle = audioSource === 'SPOTIFY' || audioSource === 'YOUTUBE' ? (externalTrack.title || 'Waiting for browser audio...') : (customDocument ? customDocument.trackTitle : currentTrack.trackTitle)
  const activeArtist = audioSource === 'SPOTIFY' || audioSource === 'YOUTUBE' ? (externalTrack.artist || audioSource) : (customDocument ? customDocument.artistName : currentTrack.artistName)
  const activeColors = customDocument ? [0xE5A243, 0x7CD4D8] : currentTrack.albumLightColors
  const durationMs = customDocument ? (customDocument.lines[customDocument.lines.length - 1]?.timestampMs || 120000) + 5000 : currentTrack.durationMs

  const activeLineIndex = findActiveLineIndex(activeDocument.lines, positionMs)

  // Media Session listener for Spotify Web or YouTube Music browser tabs
  useEffect(() => {
    if (audioSource !== 'SPOTIFY' && audioSource !== 'YOUTUBE') return

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
  }, [audioSource])

  // Demo playback timer loop
  const lastTickRef = useRef(Date.now())
  useEffect(() => {
    if ((audioSource === 'SPOTIFY' || audioSource === 'YOUTUBE') || !isPlaying) {
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
  }, [isPlaying, durationMs, audioSource])

  const handleUpdatePreference = (key, value) => {
    setPreferences(prev => ({ ...prev, [key]: value }))
  }

  const handleStartDemoTrack = (index) => {
    setAudioSource('DEMO')
    setCurrentTrackIndex(index)
    setCustomDocument(null)
    setPositionMs(0)
    setIsPlaying(true)
    setIsSettingsOpen(false)
  }

  const handleLoadDocument = (doc) => {
    setAudioSource('CUSTOM')
    setCustomDocument(doc)
    setPositionMs(0)
    setIsPlaying(true)
    setIsSettingsOpen(false)
  }

  const handleNextTrack = () => {
    if (audioSource === 'CUSTOM' || audioSource === 'SPOTIFY' || audioSource === 'YOUTUBE') {
      // no-op or rotate demo
    } else {
      setCurrentTrackIndex((currentTrackIndex + 1) % DEMO_TRACKS.length)
      setPositionMs(0)
      setIsPlaying(true)
    }
  }

  const handlePrevTrack = () => {
    if (audioSource === 'CUSTOM' || audioSource === 'SPOTIFY' || audioSource === 'YOUTUBE') {
      // no-op
    } else {
      setCurrentTrackIndex((currentTrackIndex - 1 + DEMO_TRACKS.length) % DEMO_TRACKS.length)
      setPositionMs(0)
      setIsPlaying(true)
    }
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
      />

      {/* Floating Demo Transport Controls (visible in DEMO mode) */}
      {audioSource === 'DEMO' && (
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
      )}

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
