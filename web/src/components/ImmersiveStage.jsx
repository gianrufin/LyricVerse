import React, { useState, useEffect } from 'react'
import {
  Disc,
  Tv,
  Search,
  Mic,
  MicOff,
  Monitor,
  Play,
  Pause,
  Minus,
  Plus,
  RotateCcw,
  Sparkles,
  Loader2,
  X,
  Sliders,
  AlertCircle,
  Music
} from 'lucide-react'

const FONT_CLASS_MAP = {
  PLUS_JAKARTA_SANS: 'font-jakarta',
  OUTFIT: 'font-outfit',
  SPACE_GROTESK: 'font-space',
  SORA: 'font-sora',
  DM_SANS: 'font-dmsans',
  JETBRAINS_MONO: 'font-mono',
  INSTRUMENT_SERIF: 'font-instrument',
  CORMORANT_GARAMOND: 'font-cormorant',
  PLAYFAIR_DISPLAY: 'font-playfair',
  SYNE: 'font-syne',
  FRAUNCES: 'font-fraunces',
  CINZEL: 'font-cinzel'
}

const SPEED_DURATION_MS = {
  SNAPPY: '220ms',
  BALANCED: '360ms',
  DREAMY: '560ms'
}

function formatTime(ms) {
  const totalSec = Math.max(0, Math.floor((ms || 0) / 1000))
  const mins = Math.floor(totalSec / 60)
  const secs = totalSec % 60
  return `${mins}:${secs.toString().padStart(2, '0')}`
}

export function ImmersiveStage({
  document,
  activeLineIndex,
  positionMs,
  durationMs,
  isPlaying,
  preferences,
  trackTitle,
  artistName,
  albumLightColors,
  onOpenSettings,
  onOpenSearch,
  audioSource,
  onChangeAudioSource,
  // Mic / Tab Audio listening props
  captureState,
  captureMode,
  captureStatusText,
  audioLevel,
  hasAudDToken,
  speechSupported,
  heardTranscript,
  speechSuggestions,
  onStartMic,
  onStartTabAudio,
  onStopCapture,
  onIdentifyNow,
  onSelectSuggestion,
  // Playback & Sync calibration props
  hasLocalAudio,
  localAudioFileName,
  onTogglePlayPause,
  onSeekTo,
  onClearLocalAudio,
  syncOffsetMs,
  onNudgeOffset,
  onResetOffset
}) {
  const [showingIntro, setShowingIntro] = useState(false)
  const [isSourceDropdownOpen, setIsSourceDropdownOpen] = useState(false)

  const fontClass = FONT_CLASS_MAP[preferences.fontFamily] || 'font-jakarta'
  const textAlign =
    preferences.alignment === 'START'
      ? 'text-left items-start'
      : preferences.alignment === 'END'
        ? 'text-right items-end'
        : 'text-center items-center'

  useEffect(() => {
    if (preferences.showTrackIntroOnStart && trackTitle && document) {
      setShowingIntro(true)
      const timer = setTimeout(() => setShowingIntro(false), 2200)
      return () => clearTimeout(timer)
    } else {
      setShowingIntro(false)
    }
  }, [trackTitle, document, preferences.showTrackIntroOnStart])

  const lines = document?.lines || []
  const hasLyrics =
    document &&
    ((document.syncType === 'SYNCED' && lines.length > 0) ||
      (document.syncType === 'PLAIN' && document.plainText?.trim()))

  const activeLine = lines[activeLineIndex]
  const nextLine =
    preferences.nextLineVisible && lines[activeLineIndex + 1] ? lines[activeLineIndex + 1].text : null

  let activeColor = '#F4EFE6'
  let activeGradientStyle = null

  if (preferences.colorPreset === 'STAGE_WHITE') {
    activeColor = '#FFFFFF'
  } else if (preferences.colorPreset === 'WARM_AMBER') {
    activeColor = '#E5A243'
  } else if (preferences.colorPreset === 'WARM_IVORY') {
    activeColor = '#F4EFE6'
  } else if (preferences.colorPreset === 'STAGE_CYAN') {
    activeColor = '#7CD4D8'
  } else if (preferences.colorPreset === 'ALBUM_ART' && albumLightColors?.length >= 2) {
    const c1 = '#' + (albumLightColors[0] & 0x00ffffff).toString(16).padStart(6, '0')
    const c2 = '#' + (albumLightColors[albumLightColors.length - 1] & 0x00ffffff).toString(16).padStart(6, '0')
    activeGradientStyle = {
      backgroundImage: `linear-gradient(135deg, ${c1}, ${c2})`,
      WebkitBackgroundClip: 'text',
      WebkitTextFillColor: 'transparent'
    }
  }

  const fontSizeSp = Number(preferences.fontSizeSp) || 32
  const responsiveLyricFontSize = `clamp(${Math.round(fontSizeSp * 0.78)}px, ${Math.round(fontSizeSp * 0.14)}vw + ${Math.round(fontSizeSp * 0.48)}px, ${Math.round(fontSizeSp * 1.75)}px)`
  const nextLineFontSize = `clamp(${Math.round(fontSizeSp * 0.48)}px, ${Math.round(fontSizeSp * 0.06)}vw + ${Math.round(fontSizeSp * 0.34)}px, ${Math.round(fontSizeSp * 0.82)}px)`

  const transitionClass = `lyric-enter-${preferences.transitionStyle || 'APPLE_GLASS'}`
  const transitionDuration = SPEED_DURATION_MS[preferences.transitionSpeed] || '360ms'

  const sourceLabels = {
    SPOTIFY: { label: 'Spotify Web', icon: Disc },
    YOUTUBE: { label: 'YouTube Music', icon: Tv },
    MIC: { label: 'Mic / Room Audio', icon: Mic }
  }

  const CurrentSourceIcon = sourceLabels[audioSource]?.icon || Disc
  const isListening = captureState === 'LISTENING' || captureState === 'SAMPLING' || captureState === 'IDENTIFYING'
  const isBusyIdentifying = captureState === 'SAMPLING' || captureState === 'IDENTIFYING'

  return (
    <div className="relative w-full h-full bg-black flex flex-col items-center justify-center overflow-hidden select-none px-4 sm:px-8 py-16">
      {/* Top Header Bar */}
      <div className="absolute top-4 sm:top-6 left-4 sm:left-6 right-4 sm:right-6 z-30 flex flex-wrap items-center justify-between gap-2 pointer-events-none">
        <div className="flex flex-wrap items-center gap-2 sm:gap-2.5 pointer-events-auto">
          {/* Audio Source Selector Badge */}
          <div className="relative">
            <button
              onClick={() => setIsSourceDropdownOpen(!isSourceDropdownOpen)}
              className="px-3.5 py-2.5 rounded-full bg-surface/90 hover:bg-elevated border border-outline/80 text-ivory text-xs font-mono tracking-wider transition-all flex items-center gap-2 shadow-2xl glass-blur"
            >
              <CurrentSourceIcon className="w-4 h-4 text-amber" />
              <span className="hidden sm:inline">{sourceLabels[audioSource]?.label}</span>
              <span className="sm:hidden">{audioSource}</span>
              <span className={`w-1.5 h-1.5 rounded-full ${isListening ? 'bg-cyan animate-ping' : 'bg-amber animate-pulse'}`} />
            </button>

            {isSourceDropdownOpen && (
              <div className="absolute top-full left-0 mt-2 w-56 bg-surface border border-outline rounded-2xl shadow-2xl overflow-hidden z-40 glass-blur">
                {[
                  { id: 'SPOTIFY', label: 'Spotify Web', icon: Disc },
                  { id: 'YOUTUBE', label: 'YouTube Music', icon: Tv },
                  { id: 'MIC', label: 'Mic / Room Audio', icon: Mic }
                ].map((src) => {
                  const Icon = src.icon
                  const isSelected = audioSource === src.id
                  return (
                    <button
                      key={src.id}
                      onClick={() => {
                        onChangeAudioSource(src.id)
                        setIsSourceDropdownOpen(false)
                      }}
                      className={`w-full px-4 py-3 text-left text-xs font-medium flex items-center gap-3 transition-colors ${
                        isSelected
                          ? 'bg-elevated text-amber font-bold'
                          : 'text-stone-secondary hover:text-ivory hover:bg-obsidian'
                      }`}
                    >
                      <Icon className="w-4 h-4" />
                      {src.label}
                    </button>
                  )
                })}
              </div>
            )}
          </div>

          {/* Primary Mic Listen Pill */}
          {!isListening ? (
            <button
              onClick={onStartMic}
              className="px-3.5 py-2.5 rounded-full bg-amber text-amber-ink hover:bg-amber/90 text-xs font-bold tracking-wide transition-all flex items-center gap-2 shadow-lg"
            >
              <Mic className="w-4 h-4" />
              <span>MIC LISTEN</span>
            </button>
          ) : (
            <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-elevated/95 border border-amber/60 shadow-2xl glass-blur">
              {/* Live RMS Audio Level Bars */}
              <div className="flex items-end gap-0.5 h-4 px-1" aria-label="Audio input level">
                {[0.25, 0.5, 0.75, 1].map((threshold, idx) => {
                  const active = audioLevel >= threshold * 0.55
                  return (
                    <span
                      key={idx}
                      style={{ height: active ? `${Math.max(35, Math.min(100, audioLevel * 120))}%` : '25%' }}
                      className={`w-1 rounded-full transition-all duration-100 ${
                        active ? 'bg-amber' : 'bg-outline'
                      }`}
                    />
                  )
                })}
              </div>

              <span className="text-[11px] font-mono text-ivory/90 px-1">
                {captureMode === 'TAB_AUDIO' ? 'TAB' : 'MIC'}
              </span>

              {hasAudDToken && (
                <button
                  onClick={onIdentifyNow}
                  disabled={isBusyIdentifying}
                  className="px-2.5 py-1 rounded-full bg-amber text-amber-ink text-[11px] font-bold hover:bg-amber/90 disabled:opacity-60 flex items-center gap-1 transition-colors"
                >
                  {isBusyIdentifying ? (
                    <Loader2 className="w-3 h-3 animate-spin" />
                  ) : (
                    <Sparkles className="w-3 h-3" />
                  )}
                  <span>{captureState === 'SAMPLING' ? 'Listening' : captureState === 'IDENTIFYING' ? 'Matching' : 'Identify'}</span>
                </button>
              )}

              <button
                onClick={onStopCapture}
                title="Stop listening"
                aria-label="Stop listening"
                className="p-1 rounded-full text-stone-secondary hover:text-ivory hover:bg-highest transition-colors"
              >
                <MicOff className="w-3.5 h-3.5" />
              </button>
            </div>
          )}

          {/* Direct Browser Tab Audio Capture (Desktop Chrome/Edge) */}
          {!isListening && (
            <button
              onClick={onStartTabAudio}
              title="Capture audio directly from a Spotify or YouTube Music tab (Chrome/Edge)"
              className="hidden md:flex px-3.5 py-2.5 rounded-full bg-surface/90 hover:bg-elevated border border-outline/80 text-ivory text-xs font-mono tracking-wider transition-all items-center gap-2 shadow-2xl glass-blur"
            >
              <Monitor className="w-4 h-4 text-cyan" />
              <span>TAB AUDIO</span>
            </button>
          )}

          {/* Quick LRCLIB Search */}
          <button
            onClick={onOpenSearch}
            className="px-3.5 py-2.5 rounded-full bg-surface/90 hover:bg-elevated border border-outline/80 text-ivory text-xs font-mono tracking-wider transition-all flex items-center gap-2 shadow-2xl glass-blur"
          >
            <Search className="w-4 h-4 text-amber" />
            <span className="hidden sm:inline">SEARCH</span>
          </button>
        </div>

        {/* Studio Settings Toggle */}
        <button
          onClick={onOpenSettings}
          className="pointer-events-auto px-3.5 py-2.5 rounded-full bg-surface/90 hover:bg-elevated border border-outline/80 text-ivory/90 hover:text-ivory text-xs font-mono tracking-wider transition-all flex items-center gap-2 shadow-2xl glass-blur"
        >
          <Sliders className="w-4 h-4 text-amber" />
          <span className="hidden sm:inline">SETTINGS</span>
        </button>
      </div>

      {/* Unobtrusive Live Status / Recognition Banner */}
      {(captureStatusText || heardTranscript || captureState === 'ERROR') && (
        <div className="absolute top-20 left-1/2 -translate-x-1/2 z-20 max-w-xl w-[calc(100%-2rem)] flex flex-col items-center gap-2 pointer-events-auto">
          {captureStatusText && (
            <div
              className={`px-4 py-2 rounded-full border text-xs font-medium flex items-center gap-2 shadow-xl glass-blur ${
                captureState === 'ERROR'
                  ? 'bg-surface/95 border-red-500/60 text-red-300'
                  : 'bg-surface/90 border-hairline text-stone-secondary'
              }`}
            >
              {captureState === 'ERROR' && <AlertCircle className="w-3.5 h-3.5 text-red-400 shrink-0" />}
              {isBusyIdentifying && <Loader2 className="w-3.5 h-3.5 text-amber animate-spin shrink-0" />}
              <span className="truncate">{captureStatusText}</span>
            </div>
          )}

          {heardTranscript && !hasAudDToken && isListening && (
            <div className="px-3.5 py-1.5 rounded-full bg-obsidian/90 border border-outline/60 text-[11px] font-mono text-amber/90 truncate max-w-full">
              Heard: &ldquo;{heardTranscript}&rdquo;
            </div>
          )}

          {speechSuggestions?.length > 0 && (
            <div className="flex flex-wrap items-center justify-center gap-2">
              {speechSuggestions.slice(0, 2).map((item, idx) => (
                <button
                  key={item.id || idx}
                  onClick={() => onSelectSuggestion(item)}
                  className="px-3 py-1.5 rounded-full bg-elevated/95 hover:bg-highest border border-amber/50 text-xs text-ivory flex items-center gap-1.5 shadow-lg transition-colors"
                >
                  <Music className="w-3 h-3 text-amber" />
                  <span className="truncate max-w-[200px]">
                    Load {item.trackTitle} — {item.artistName}
                  </span>
                </button>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Immersive Center Content */}
      <div className={`w-full max-w-4xl flex flex-col ${textAlign} z-10 transition-all duration-300`}>
        {!hasLyrics ? (
          <div className="flex flex-col items-center justify-center text-center space-y-4 py-12 max-w-lg mx-auto">
            <div className="w-14 h-14 rounded-full bg-surface border border-outline flex items-center justify-center text-amber shadow-xl">
              {isListening ? <Mic className="w-6 h-6 animate-pulse" /> : <CurrentSourceIcon className="w-6 h-6" />}
            </div>

            <div className="space-y-2">
              <h2 className="text-xl sm:text-2xl font-semibold text-ivory tracking-tight">
                {isListening
                  ? captureMode === 'TAB_AUDIO'
                    ? 'Listening to Browser Tab Audio'
                    : 'Listening via Microphone'
                  : `Ready for ${sourceLabels[audioSource]?.label}`}
              </h2>
              <p className="text-sm text-stone-secondary leading-relaxed">
                {isListening
                  ? hasAudDToken
                    ? 'Play your song on Spotify or YouTube Music. Tap Identify to fingerprint the track and jump straight to the synced timestamp.'
                    : speechSupported
                      ? 'Speak or play clear vocals near your mic, or tap Search to load synced lyrics first so your mic can jump to matching lines.'
                      : 'Microphone meter active. Add a free AudD token in Settings for automatic acoustic song ID, or Search LRCLIB to load lyrics.'
                  : 'Browsers isolate Spotify and YouTube Music tabs for security. Start Mic Listen (or Tab Audio on desktop) to hear what is playing, or Search LRCLIB to load synced lyrics.'}
              </p>
            </div>

            <div className="flex flex-wrap items-center justify-center gap-3 pt-2">
              {!isListening ? (
                <>
                  <button
                    onClick={onStartMic}
                    className="px-5 py-3 rounded-xl bg-amber text-amber-ink font-bold text-sm hover:bg-amber/90 transition-colors shadow-lg flex items-center gap-2"
                  >
                    <Mic className="w-4 h-4" />
                    Start Mic Listening
                  </button>
                  <button
                    onClick={onStartTabAudio}
                    className="px-5 py-3 rounded-xl bg-surface border border-outline text-ivory font-semibold text-sm hover:bg-elevated transition-colors flex items-center gap-2"
                  >
                    <Monitor className="w-4 h-4 text-cyan" />
                    Capture Tab Audio
                  </button>
                </>
              ) : (
                hasAudDToken && (
                  <button
                    onClick={onIdentifyNow}
                    disabled={isBusyIdentifying}
                    className="px-5 py-3 rounded-xl bg-amber text-amber-ink font-bold text-sm hover:bg-amber/90 disabled:opacity-60 transition-colors shadow-lg flex items-center gap-2"
                  >
                    {isBusyIdentifying ? <Loader2 className="w-4 h-4 animate-spin" /> : <Sparkles className="w-4 h-4" />}
                    {isBusyIdentifying ? 'Identifying Song…' : 'Identify Song Now'}
                  </button>
                )
              )}

              <button
                onClick={onOpenSearch}
                className="px-5 py-3 rounded-xl bg-surface border border-outline text-ivory font-semibold text-sm hover:bg-elevated transition-colors flex items-center gap-2"
              >
                <Search className="w-4 h-4 text-amber" />
                Search Lyrics
              </button>
            </div>
          </div>
        ) : showingIntro ? (
          <div className="flex flex-col items-center text-center space-y-3 animate-fade-in">
            <h1
              style={{
                ...(activeGradientStyle || { color: activeColor }),
                fontSize: responsiveLyricFontSize
              }}
              className={`${fontClass} font-bold tracking-tight leading-tight`}
            >
              {trackTitle}
            </h1>
            {artistName && (
              <p className="text-stone-secondary text-lg font-medium tracking-wide">{artistName}</p>
            )}
          </div>
        ) : document.syncType === 'PLAIN' ? (
          <div className="w-full max-h-[65vh] overflow-y-auto px-4 py-6 space-y-4">
            <div className="mb-4 text-center">
              <p className="text-xs font-mono uppercase tracking-widest text-amber">
                {trackTitle} {artistName ? `— ${artistName}` : ''}
              </p>
              <p className="text-[11px] text-stone-muted mt-1">Unsynced plain lyrics</p>
            </div>
            <pre
              style={{ fontSize: `clamp(16px, ${Math.round(fontSizeSp * 0.65)}px, 28px)` }}
              className={`${fontClass} text-ivory/90 leading-relaxed whitespace-pre-wrap text-center`}
            >
              {document.plainText}
            </pre>
          </div>
        ) : (
          <div className={`flex flex-col ${textAlign} space-y-6 w-full`}>
            <div
              key={activeLineIndex}
              style={{ '--lyric-duration': transitionDuration }}
              className={`w-full ${transitionClass}`}
            >
              {preferences.wordByWordEnabled && activeLine?.words?.length > 0 ? (
                <div
                  className={`flex flex-wrap ${
                    preferences.alignment === 'START'
                      ? 'justify-start'
                      : preferences.alignment === 'END'
                        ? 'justify-end'
                        : 'justify-center'
                  } gap-x-3 gap-y-2`}
                >
                  {activeLine.words.map((word, wIdx) => {
                    const isReached = positionMs >= word.startMs
                    const opacity = isReached ? 'opacity-100 scale-105' : 'opacity-35 scale-100'
                    const blurClass = preferences.glassBlurEnabled && !isReached ? 'blur-[2px]' : 'blur-0'

                    return (
                      <span
                        key={wIdx}
                        style={{
                          ...(activeGradientStyle && isReached
                            ? activeGradientStyle
                            : { color: isReached ? activeColor : '#A9A296' }),
                          fontSize: responsiveLyricFontSize
                        }}
                        className={`${fontClass} font-bold tracking-tight leading-tight transition-all duration-200 transform ${opacity} ${blurClass}`}
                      >
                        {word.text}
                      </span>
                    )
                  })}
                </div>
              ) : (
                <h2
                  style={{
                    ...(activeGradientStyle || { color: activeColor }),
                    fontSize: responsiveLyricFontSize
                  }}
                  className={`${fontClass} font-bold tracking-tight leading-tight transition-all duration-300`}
                >
                  {activeLine ? activeLine.text : '♪'}
                </h2>
              )}
            </div>

            {nextLine && (
              <div className="transition-all duration-300 pt-2">
                <p
                  style={{ fontSize: nextLineFontSize }}
                  className={`${fontClass} text-stone-secondary/60 font-medium tracking-wide`}
                >
                  {nextLine}
                </p>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Bottom Stage Footer / Live Sync & Local Audio Controls */}
      <div className="absolute bottom-4 sm:bottom-6 left-4 sm:left-6 right-4 sm:right-6 z-20 flex flex-wrap items-center justify-between gap-3">
        <div className="text-xs font-mono text-stone-secondary/60 tracking-wider flex items-center gap-2.5 truncate max-w-xs sm:max-w-md">
          <span className="text-amber/80">LYRICVERSE</span>
          {hasLyrics && (
            <>
              <span>·</span>
              <span className="truncate text-ivory/80">
                {trackTitle}
                {artistName ? ` — ${artistName}` : ''}
              </span>
            </>
          )}
        </div>

        {/* Local Audio Player Bar */}
        {hasLocalAudio ? (
          <div className="flex items-center gap-3 px-4 py-2 rounded-full bg-surface/95 border border-outline/80 shadow-2xl glass-blur max-w-full">
            <button
              onClick={onTogglePlayPause}
              aria-label={isPlaying ? 'Pause audio' : 'Play audio'}
              className="w-8 h-8 rounded-full bg-amber text-amber-ink flex items-center justify-center hover:bg-amber/90 transition-colors shrink-0"
            >
              {isPlaying ? <Pause className="w-4 h-4" /> : <Play className="w-4 h-4 ml-0.5" />}
            </button>

            <div className="flex items-center gap-2 min-w-[140px] sm:min-w-[220px]">
              <span className="text-[11px] font-mono text-stone-secondary tabular-nums">
                {formatTime(positionMs)}
              </span>
              <input
                type="range"
                min={0}
                max={Math.max(1000, durationMs || 1000)}
                value={Math.min(positionMs, durationMs || positionMs)}
                onChange={(e) => onSeekTo(Number(e.target.value))}
                className="flex-1 accent-amber cursor-pointer h-1"
              />
              <span className="text-[11px] font-mono text-stone-muted tabular-nums">
                {formatTime(durationMs)}
              </span>
            </div>

            <span className="hidden md:inline text-[11px] text-stone-secondary truncate max-w-[140px]">
              {localAudioFileName}
            </span>

            <button
              onClick={onClearLocalAudio}
              title="Unload local audio file"
              aria-label="Unload local audio file"
              className="p-1 text-stone-secondary hover:text-ivory transition-colors"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        ) : (
          hasLyrics &&
          document?.syncType === 'SYNCED' && (
            <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-surface/90 border border-outline/80 shadow-2xl glass-blur">
              <button
                onClick={onTogglePlayPause}
                aria-label={isPlaying ? 'Pause lyric timer' : 'Resume lyric timer'}
                className="w-7 h-7 rounded-full bg-amber text-amber-ink flex items-center justify-center hover:bg-amber/90 transition-colors"
              >
                {isPlaying ? <Pause className="w-3.5 h-3.5" /> : <Play className="w-3.5 h-3.5 ml-0.5" />}
              </button>

              <span className="text-[11px] font-mono text-ivory/90 px-1.5 tabular-nums">
                {formatTime(positionMs)}
              </span>

              <div className="h-4 w-px bg-hairline mx-0.5" />

              <button
                onClick={() => onNudgeOffset(-500)}
                title="Delay lyrics -0.5s"
                className="px-2 py-1 rounded-full text-[11px] font-mono text-stone-secondary hover:text-ivory hover:bg-elevated flex items-center gap-0.5 transition-colors"
              >
                <Minus className="w-3 h-3" />
                <span>0.5s</span>
              </button>

              <button
                onClick={() => onNudgeOffset(500)}
                title="Advance lyrics +0.5s"
                className="px-2 py-1 rounded-full text-[11px] font-mono text-stone-secondary hover:text-ivory hover:bg-elevated flex items-center gap-0.5 transition-colors"
              >
                <Plus className="w-3 h-3" />
                <span>0.5s</span>
              </button>

              {syncOffsetMs !== 0 && (
                <button
                  onClick={onResetOffset}
                  title="Reset sync offset"
                  className="px-2 py-1 rounded-full text-[11px] font-mono text-amber hover:bg-elevated flex items-center gap-1 transition-colors"
                >
                  <RotateCcw className="w-3 h-3" />
                  <span>{syncOffsetMs > 0 ? `+${(syncOffsetMs / 1000).toFixed(1)}s` : `${(syncOffsetMs / 1000).toFixed(1)}s`}</span>
                </button>
              )}
            </div>
          )
        )}
      </div>
    </div>
  )
}
