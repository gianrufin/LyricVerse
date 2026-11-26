import React, { useState, useEffect, useRef } from 'react'
import { ImmersiveStage } from './components/ImmersiveStage'
import { SettingsDrawer } from './components/SettingsDrawer'
import { findActiveLineIndex } from './utils/lrcParser'

export default function App() {
  const [customDocument, setCustomDocument] = useState(null)
  const [audioFile, setAudioFile] = useState(null)
  const [audioFileName, setAudioFileName] = useState('')
  const [isPlaying, setIsPlaying] = useState(false)
  const [positionMs, setPositionMs] = useState(0)
  const [isSettingsOpen, setIsSettingsOpen] = useState(false)

  const audioRef = useRef(null)

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
  const activeTitle = customDocument ? customDocument.trackTitle : (audioFileName || 'Audio & Lyrics Stage')
  const activeArtist = customDocument ? customDocument.artistName : 'Upload Song & LRCLIB Lyrics'
  const activeColors = [0xE5A243, 0x7CD4D8, 0xF4EFE6]

  const activeLineIndex = findActiveLineIndex(activeDocument?.lines || [], positionMs)

  const handleLoadAudioFile = (file) => {
    if (audioRef.current) {
      audioRef.current.pause()
    }
    const url = URL.createObjectURL(file)
    const audio = new Audio(url)
    audioRef.current = audio
    setAudioFile(file)
    setAudioFileName(file.name)

    audio.addEventListener('timeupdate', () => {
      setPositionMs(audio.currentTime * 1000)
    })
    audio.addEventListener('play', () => setIsPlaying(true))
    audio.addEventListener('pause', () => setIsPlaying(false))
    audio.addEventListener('ended', () => setIsPlaying(false))

    audio.play().then(() => setIsPlaying(true)).catch(err => console.error('Audio play error:', err))
    setIsSettingsOpen(false)
  }

  const handleTogglePlayPause = () => {
    if (!audioRef.current) return
    if (isPlaying) {
      audioRef.current.pause()
    } else {
      audioRef.current.play()
    }
  }

  const handleSeek = (progressPct) => {
    if (!audioRef.current || !audioRef.current.duration) return
    const newTime = (progressPct / 100) * audioRef.current.duration
    audioRef.current.currentTime = newTime
    setPositionMs(newTime * 1000)
  }

  const handleUpdatePreference = (key, value) => {
    setPreferences(prev => ({ ...prev, [key]: value }))
  }

  const handleLoadDocument = (doc) => {
    setCustomDocument(doc)
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
        onOpenSearch={() => setIsSettingsOpen(true)}
        audioFileName={audioFileName}
        onTogglePlayPause={handleTogglePlayPause}
        onSeek={handleSeek}
      />

      {/* Collapsible Studio Settings Drawer */}
      <SettingsDrawer
        isOpen={isSettingsOpen}
        onClose={() => setIsSettingsOpen(false)}
        preferences={preferences}
        onUpdatePreference={handleUpdatePreference}
        onLoadDocument={handleLoadDocument}
        onLoadAudioFile={handleLoadAudioFile}
      />
    </div>
  )
}
