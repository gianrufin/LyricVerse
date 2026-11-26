import React, { useState, useEffect, useRef, useCallback } from 'react'
import { ImmersiveStage } from './components/ImmersiveStage'
import { SettingsDrawer } from './components/SettingsDrawer'
import { findActiveLineIndex } from './utils/lrcParser'
import { createMediaSessionListener } from './services/mediaSessionListener'
import { findBestLrcLibMatch, searchLrcLib } from './services/lrcLib'
import {
  startMicCapture,
  startTabAudioCapture,
  createAudioLevelMonitor,
  identifyWithAudD,
  isSpeechRecognitionSupported,
  createSpeechLyricWatcher,
  matchTranscriptToLines
} from './services/micListener'

const STORAGE_KEYS = {
  PREFS: 'lyricverse_web_preferences',
  AUDD_TOKEN: 'lyricverse_web_audd_token',
  SOURCE: 'lyricverse_web_audio_source',
  LISTEN_PREFS: 'lyricverse_web_listen_prefs'
}

const DEFAULT_PREFERENCES = {
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
}

function loadJson(key, fallback) {
  try {
    const raw = localStorage.getItem(key)
    return raw ? { ...fallback, ...JSON.parse(raw) } : fallback
  } catch {
    return fallback
  }
}

export default function App() {
  const [audioSource, setAudioSource] = useState(() => {
    try {
      return localStorage.getItem(STORAGE_KEYS.SOURCE) || 'SPOTIFY'
    } catch {
      return 'SPOTIFY'
    }
  })

  const [preferences, setPreferences] = useState(() => loadJson(STORAGE_KEYS.PREFS, DEFAULT_PREFERENCES))
  const [listenPrefs, setListenPrefs] = useState(() =>
    loadJson(STORAGE_KEYS.LISTEN_PREFS, { autoIdentify: true, speechFallback: true })
  )
  const [auddToken, setAuddToken] = useState(() => {
    try {
      return localStorage.getItem(STORAGE_KEYS.AUDD_TOKEN) || ''
    } catch {
      return ''
    }
  })

  const [customDocument, setCustomDocument] = useState(null)
  const [externalTrack, setExternalTrack] = useState({ title: '', artist: '' })
  const [isPlaying, setIsPlaying] = useState(false)
  const [positionMs, setPositionMs] = useState(0)
  const [syncOffsetMs, setSyncOffsetMs] = useState(0)

  const [isSettingsOpen, setIsSettingsOpen] = useState(false)
  const [settingsInitialTab, setSettingsInitialTab] = useState('SOURCES')

  // Local uploaded audio file state
  const [localAudioUrl, setLocalAudioUrl] = useState('')
  const [localAudioFileName, setLocalAudioFileName] = useState('')
  const [localAudioDurationMs, setLocalAudioDurationMs] = useState(0)
  const audioElRef = useRef(null)

  // Microphone / Tab Audio listening state
  const [captureState, setCaptureState] = useState('IDLE') // IDLE | LISTENING | SAMPLING | IDENTIFYING | ERROR
  const [captureMode, setCaptureMode] = useState(null) // MIC | TAB_AUDIO | null
  const [captureStatusText, setCaptureStatusText] = useState('')
  const [audioLevel, setAudioLevel] = useState(0)
  const [heardTranscript, setHeardTranscript] = useState('')
  const [speechSuggestions, setSpeechSuggestions] = useState([])

  const streamRef = useRef(null)
  const levelMonitorRef = useRef(null)
  const speechWatcherRef = useRef(null)
  const identifyBusyRef = useRef(false)
  const autoIdentifiedSessionRef = useRef(false)
  const lastSpeechSearchRef = useRef(0)

  const speechSupported = isSpeechRecognitionSupported()
  const activeDocument = customDocument
  const activeTitle = activeDocument?.trackTitle || externalTrack.title
  const activeArtist = activeDocument?.artistName || externalTrack.artist
  const activeColors = [0xe5a243, 0x7cd4d8, 0xf4efe6]

  const durationMs = localAudioUrl
    ? localAudioDurationMs || activeDocument?.durationMs || 180000
    : activeDocument
      ? Math.max(
          activeDocument.durationMs || 0,
          (activeDocument.lines[activeDocument.lines.length - 1]?.timestampMs || 180000) + 5000
        )
      : 180000

  const effectivePositionMs = Math.max(0, positionMs + syncOffsetMs)
  const activeLineIndex = findActiveLineIndex(activeDocument?.lines || [], effectivePositionMs)

  // Keep refs updated for async callbacks (speech watcher)
  const activeDocumentRef = useRef(activeDocument)
  const activeLineIndexRef = useRef(activeLineIndex)
  useEffect(() => {
    activeDocumentRef.current = activeDocument
  }, [activeDocument])
  useEffect(() => {
    activeLineIndexRef.current = activeLineIndex
  }, [activeLineIndex])

  // Persist settings
  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEYS.PREFS, JSON.stringify(preferences))
    } catch {}
  }, [preferences])

  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEYS.LISTEN_PREFS, JSON.stringify(listenPrefs))
    } catch {}
  }, [listenPrefs])

  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEYS.SOURCE, audioSource)
    } catch {}
  }, [audioSource])

  // Screen Wake Lock
  useEffect(() => {
    let wakeLockSentinel = null
    let released = false

    const acquire = async () => {
      if (!preferences.keepScreenOn || document.visibilityState !== 'visible') return
      if (!('wakeLock' in navigator)) return
      try {
        const sentinel = await navigator.wakeLock.request('screen')
        if (released) {
          sentinel.release().catch(() => {})
        } else {
          wakeLockSentinel = sentinel
        }
      } catch {}
    }

    acquire()

    const handleVisibility = () => {
      if (document.visibilityState === 'visible') acquire()
    }
    document.addEventListener('visibilitychange', handleVisibility)

    return () => {
      released = true
      document.removeEventListener('visibilitychange', handleVisibility)
      if (wakeLockSentinel) {
        wakeLockSentinel.release().catch(() => {})
      }
    }
  }, [preferences.keepScreenOn])

  // Media Session listener (works if browser exposes session metadata on same page / PWA context)
  useEffect(() => {
    const listener = createMediaSessionListener(
      (title, artist, playing, fetchedDoc) => {
        if (localAudioUrl) return
        setExternalTrack({ title, artist })
        setIsPlaying(playing)
        if (fetchedDoc) {
          setCustomDocument(fetchedDoc)
          setPositionMs(0)
        }
      },
      (pos, playing) => {
        if (localAudioUrl) return
        setPositionMs(pos)
        setIsPlaying(playing)
      }
    )

    listener.start()
    return () => listener.stop()
  }, [audioSource, localAudioUrl])

  // Local <audio> playback sync
  useEffect(() => {
    if (!localAudioUrl || !audioElRef.current) return
    let rafId = null

    const syncFrame = () => {
      const el = audioElRef.current
      if (el && !el.paused) {
        setPositionMs(el.currentTime * 1000)
        rafId = requestAnimationFrame(syncFrame)
      }
    }

    if (isPlaying) {
      rafId = requestAnimationFrame(syncFrame)
    }

    return () => {
      if (rafId) cancelAnimationFrame(rafId)
    }
  }, [localAudioUrl, isPlaying])

  // Timer loop for external synced lyrics (Spotify / YouTube Music / Mic)
  const lastTickRef = useRef(Date.now())
  useEffect(() => {
    if (localAudioUrl || !isPlaying || !activeDocument || activeDocument.syncType !== 'SYNCED') {
      lastTickRef.current = Date.now()
      return
    }

    lastTickRef.current = Date.now()
    const interval = setInterval(() => {
      const now = Date.now()
      const delta = now - lastTickRef.current
      lastTickRef.current = now

      setPositionMs((prev) => {
        const next = prev + delta
        if (next >= durationMs) {
          setIsPlaying(false)
          return durationMs
        }
        return next
      })
    }, 50)

    return () => clearInterval(interval)
  }, [localAudioUrl, isPlaying, activeDocument, durationMs])

  const stopCapture = useCallback(() => {
    if (speechWatcherRef.current) {
      speechWatcherRef.current.stop()
      speechWatcherRef.current = null
    }
    if (levelMonitorRef.current) {
      levelMonitorRef.current.stop()
      levelMonitorRef.current = null
    }
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((t) => t.stop())
      streamRef.current = null
    }
    identifyBusyRef.current = false
    autoIdentifiedSessionRef.current = false
    setAudioLevel(0)
    setCaptureMode(null)
    setCaptureState('IDLE')
    setCaptureStatusText('')
    setHeardTranscript('')
  }, [])

  // Cleanup capture + object URL on unmount
  useEffect(() => {
    return () => {
      stopCapture()
    }
  }, [stopCapture])

  useEffect(() => {
    return () => {
      if (localAudioUrl) URL.revokeObjectURL(localAudioUrl)
    }
  }, [localAudioUrl])

  const handleIdentifyNow = useCallback(async () => {
    if (!streamRef.current || identifyBusyRef.current) return
    if (!auddToken.trim()) {
      setCaptureStatusText('Add an AudD API token in Settings to identify songs acoustically.')
      setSettingsInitialTab('SOURCES')
      setIsSettingsOpen(true)
      return
    }

    identifyBusyRef.current = true
    try {
      const result = await identifyWithAudD(streamRef.current, auddToken, (stage) => {
        setCaptureState(stage)
        setCaptureStatusText(
          stage === 'SAMPLING' ? 'Sampling 5.5s of audio…' : 'Identifying song via AudD…'
        )
      })

      if (!streamRef.current) return // User stopped while identifying

      if (!result.matched) {
        setCaptureState('LISTENING')
        setCaptureStatusText(result.reason)
        return
      }

      setExternalTrack({ title: result.title, artist: result.artist })
      setCaptureState('LISTENING')
      setCaptureStatusText(`Matched: ${result.title} — ${result.artist}. Fetching lyrics…`)

      const lrcDoc = await findBestLrcLibMatch({
        title: result.title,
        artist: result.artist,
        album: result.album
      })

      if (lrcDoc) {
        setCustomDocument(lrcDoc)
        setSyncOffsetMs(0)
        if (result.estimatedPositionMs > 0) {
          setPositionMs(result.estimatedPositionMs)
        } else {
          setPositionMs(0)
        }
        setIsPlaying(lrcDoc.syncType === 'SYNCED')
        setSpeechSuggestions([])
        setCaptureStatusText(
          result.timecodeRaw
            ? `Synced "${result.title}" at ${result.timecodeRaw}`
            : `Loaded "${result.title}" (${lrcDoc.syncType})`
        )
      } else if (result.plainLyricsFallback) {
        setCustomDocument({
          trackTitle: result.title,
          artistName: result.artist,
          albumName: result.album,
          syncType: 'PLAIN',
          lines: [],
          plainText: result.plainLyricsFallback,
          providerId: 'audd',
          providerDisplayName: 'AudD Lyrics'
        })
        setIsPlaying(false)
        setCaptureStatusText(`Matched "${result.title}" (plain lyrics only)`)
      } else {
        setCaptureStatusText(`Matched "${result.title}" — ${result.artist}, but no lyrics were found on LRCLIB.`)
      }
    } catch (err) {
      if (streamRef.current) {
        setCaptureState('ERROR')
        setCaptureStatusText(err.message || 'Song identification failed.')
      }
    } finally {
      identifyBusyRef.current = false
    }
  }, [auddToken])

  // Auto-identify once per listening session when real audio energy is detected
  useEffect(() => {
    if (
      captureState === 'LISTENING' &&
      listenPrefs.autoIdentify &&
      auddToken.trim() &&
      !autoIdentifiedSessionRef.current &&
      !identifyBusyRef.current &&
      audioLevel >= 0.05
    ) {
      autoIdentifiedSessionRef.current = true
      handleIdentifyNow()
    }
  }, [captureState, listenPrefs.autoIdentify, auddToken, audioLevel, handleIdentifyNow])

  const startCaptureSession = async (mode) => {
    stopCapture()
    setSpeechSuggestions([])

    try {
      const { stream, captureMode: resolvedMode } =
        mode === 'TAB_AUDIO' ? await startTabAudioCapture() : await startMicCapture()

      streamRef.current = stream
      setCaptureMode(resolvedMode)
      setCaptureState('LISTENING')
      autoIdentifiedSessionRef.current = false

      if (resolvedMode === 'TAB_AUDIO') {
        setCaptureStatusText(
          auddToken.trim()
            ? 'Listening to browser tab audio — play music to identify.'
            : 'Tab audio connected. Add an AudD token in Settings to auto-identify.'
        )
      } else {
        setCaptureStatusText(
          auddToken.trim()
            ? 'Listening via microphone — play music to identify.'
            : speechSupported && listenPrefs.speechFallback
              ? 'Mic listening (Speech-to-Lyric mode). Load a song or sing/play clear vocals.'
              : 'Mic meter active. Add an AudD token in Settings for song ID.'
        )
      }

      // Stop gracefully if user ends tab share in browser chrome
      stream.getTracks().forEach((track) => {
        track.onended = () => {
          stopCapture()
        }
      })

      levelMonitorRef.current = createAudioLevelMonitor(stream, (rms) => {
        setAudioLevel(rms)
      })

      // Start browser Speech-to-Lyric watcher only for mic mode when no AudD token is set
      if (
        resolvedMode === 'MIC' &&
        !auddToken.trim() &&
        listenPrefs.speechFallback &&
        isSpeechRecognitionSupported()
      ) {
        const watcher = createSpeechLyricWatcher({
          onTranscript: async (text, isFinal) => {
            setHeardTranscript(text)
            const currentDoc = activeDocumentRef.current
            if (currentDoc?.lines?.length > 0) {
              const match = matchTranscriptToLines(text, currentDoc.lines, activeLineIndexRef.current)
              if (match) {
                setPositionMs(match.timestampMs)
                setIsPlaying(true)
                setCaptureStatusText(`Synced to heard line: "${currentDoc.lines[match.index].text}"`)
              }
            } else if (isFinal && text.split(/\s+/).length >= 3) {
              const now = Date.now()
              if (now - lastSpeechSearchRef.current > 8000) {
                lastSpeechSearchRef.current = now
                const matches = await searchLrcLib(text)
                if (matches.length > 0 && streamRef.current) {
                  setSpeechSuggestions(matches.slice(0, 2))
                }
              }
            }
          },
          onError: (errMsg) => {
            setCaptureStatusText(`Speech fallback notice: ${errMsg}`)
          }
        })
        if (watcher) {
          speechWatcherRef.current = watcher
          watcher.start()
        }
      }
    } catch (err) {
      setCaptureState('ERROR')
      setCaptureStatusText(err.message || 'Unable to start audio capture.')
    }
  }

  const handleUpdatePreference = (key, value) => {
    setPreferences((prev) => ({ ...prev, [key]: value }))
  }

  const handleUpdateListenPref = (key, value) => {
    setListenPrefs((prev) => ({ ...prev, [key]: value }))
  }

  const handleSaveAuddToken = (token) => {
    setAuddToken(token)
    try {
      if (token) localStorage.setItem(STORAGE_KEYS.AUDD_TOKEN, token)
      else localStorage.removeItem(STORAGE_KEYS.AUDD_TOKEN)
    } catch {}
  }

  const handleLoadDocument = (doc) => {
    setCustomDocument(doc)
    setExternalTrack({ title: doc.trackTitle, artist: doc.artistName })
    setSpeechSuggestions([])
    setSyncOffsetMs(0)
    if (!localAudioUrl) {
      setPositionMs(0)
      setIsPlaying(doc.syncType === 'SYNCED')
    }
    setIsSettingsOpen(false)
  }

  const handleLoadAudioFile = async (file) => {
    if (localAudioUrl) {
      URL.revokeObjectURL(localAudioUrl)
    }
    const url = URL.createObjectURL(file)
    setLocalAudioUrl(url)
    setLocalAudioFileName(file.name)
    setSyncOffsetMs(0)
    setPositionMs(0)
    setIsSettingsOpen(false)

    const baseName = file.name.replace(/\.[^/.]+$/, '').replace(/[_]+/g, ' ').trim()
    let parsedArtist = ''
    let parsedTitle = baseName
    if (baseName.includes(' - ')) {
      const parts = baseName.split(' - ')
      parsedArtist = parts[0].trim()
      parsedTitle = parts.slice(1).join(' - ').trim()
    }

    setExternalTrack({ title: parsedTitle, artist: parsedArtist })
    setIsPlaying(true)

    const matchedDoc = await findBestLrcLibMatch({
      title: parsedTitle,
      artist: parsedArtist
    })
    if (matchedDoc) {
      setCustomDocument(matchedDoc)
    }
  }

  const handleTogglePlayPause = () => {
    if (localAudioUrl && audioElRef.current) {
      if (audioElRef.current.paused) {
        audioElRef.current.play().catch(() => {})
        setIsPlaying(true)
      } else {
        audioElRef.current.pause()
        setIsPlaying(false)
      }
    } else {
      setIsPlaying((prev) => !prev)
    }
  }

  const handleSeekTo = (ms) => {
    setPositionMs(ms)
    if (audioElRef.current) {
      audioElRef.current.currentTime = ms / 1000
    }
  }

  const handleClearLocalAudio = () => {
    if (audioElRef.current) {
      audioElRef.current.pause()
    }
    if (localAudioUrl) {
      URL.revokeObjectURL(localAudioUrl)
    }
    setLocalAudioUrl('')
    setLocalAudioFileName('')
    setLocalAudioDurationMs(0)
    setIsPlaying(false)
    setPositionMs(0)
  }

  return (
    <div className="relative w-screen h-screen overflow-hidden bg-black font-jakarta">
      {/* Hidden Audio Element when playing uploaded local audio */}
      {localAudioUrl && (
        <audio
          ref={audioElRef}
          src={localAudioUrl}
          autoPlay
          onLoadedMetadata={(e) => {
            const dur = e.currentTarget.duration
            if (Number.isFinite(dur)) setLocalAudioDurationMs(dur * 1000)
          }}
          onPlay={() => setIsPlaying(true)}
          onPause={() => setIsPlaying(false)}
          onEnded={() => setIsPlaying(false)}
        />
      )}

      {/* Immersive Full-Window Lyrics Stage */}
      <ImmersiveStage
        document={activeDocument}
        activeLineIndex={activeLineIndex}
        positionMs={effectivePositionMs}
        durationMs={durationMs}
        isPlaying={isPlaying}
        preferences={preferences}
        trackTitle={activeTitle}
        artistName={activeArtist}
        albumLightColors={activeColors}
        onOpenSettings={() => {
          setSettingsInitialTab('THEME')
          setIsSettingsOpen(true)
        }}
        onOpenSearch={() => {
          setSettingsInitialTab('SOURCES')
          setIsSettingsOpen(true)
        }}
        audioSource={audioSource}
        onChangeAudioSource={(src) => setAudioSource(src)}
        captureState={captureState}
        captureMode={captureMode}
        captureStatusText={captureStatusText}
        audioLevel={audioLevel}
        hasAudDToken={Boolean(auddToken.trim())}
        speechSupported={speechSupported}
        heardTranscript={heardTranscript}
        speechSuggestions={speechSuggestions}
        onStartMic={() => startCaptureSession('MIC')}
        onStartTabAudio={() => startCaptureSession('TAB_AUDIO')}
        onStopCapture={stopCapture}
        onIdentifyNow={handleIdentifyNow}
        onSelectSuggestion={handleLoadDocument}
        hasLocalAudio={Boolean(localAudioUrl)}
        localAudioFileName={localAudioFileName}
        onTogglePlayPause={handleTogglePlayPause}
        onSeekTo={handleSeekTo}
        onClearLocalAudio={handleClearLocalAudio}
        syncOffsetMs={syncOffsetMs}
        onNudgeOffset={(delta) => setSyncOffsetMs((prev) => prev + delta)}
        onResetOffset={() => setSyncOffsetMs(0)}
      />

      {/* Collapsible Studio Settings Drawer */}
      <SettingsDrawer
        isOpen={isSettingsOpen}
        initialTab={settingsInitialTab}
        onClose={() => setIsSettingsOpen(false)}
        preferences={preferences}
        onUpdatePreference={handleUpdatePreference}
        onLoadDocument={handleLoadDocument}
        onLoadAudioFile={handleLoadAudioFile}
        captureState={captureState}
        captureMode={captureMode}
        audioLevel={audioLevel}
        auddToken={auddToken}
        onSaveAuddToken={handleSaveAuddToken}
        listenPrefs={listenPrefs}
        onUpdateListenPref={handleUpdateListenPref}
        speechSupported={speechSupported}
        onStartMic={() => startCaptureSession('MIC')}
        onStartTabAudio={() => startCaptureSession('TAB_AUDIO')}
        onStopCapture={stopCapture}
        onIdentifyNow={handleIdentifyNow}
        syncOffsetMs={syncOffsetMs}
        onNudgeOffset={(delta) => setSyncOffsetMs((prev) => prev + delta)}
        onResetOffset={() => setSyncOffsetMs(0)}
      />
    </div>
  )
}
