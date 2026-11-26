import React, { useState, useEffect } from 'react'
import { Search, FolderOpen, Play, Pause, Music } from 'lucide-react'

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

export function ImmersiveStage({
  document,
  activeLineIndex,
  positionMs,
  isPlaying,
  preferences,
  trackTitle,
  artistName,
  albumLightColors,
  onOpenSettings,
  onOpenSearch,
  audioFileName,
  onTogglePlayPause,
  onSeek
}) {
  const [showingIntro, setShowingIntro] = useState(false)

  const fontClass = FONT_CLASS_MAP[preferences.fontFamily] || 'font-jakarta'
  const textAlign = preferences.alignment === 'START' ? 'text-left items-start' : preferences.alignment === 'END' ? 'text-right items-end' : 'text-center items-center'

  useEffect(() => {
    if (preferences.showTrackIntroOnStart && trackTitle) {
      setShowingIntro(true)
      const timer = setTimeout(() => setShowingIntro(false), 2400)
      return () => clearTimeout(timer)
    } else {
      setShowingIntro(false)
    }
  }, [trackTitle, preferences.showTrackIntroOnStart])

  const lines = document?.lines || []
  const hasLyrics = document && ((document.syncType === 'SYNCED' && lines.length > 0) || (document.syncType === 'PLAIN' && document.plainText.trim()))

  const activeLine = lines[activeLineIndex]
  const nextLine = preferences.nextLineVisible && lines[activeLineIndex + 1] ? lines[activeLineIndex + 1].text : null

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
    const c1 = '#' + (albumLightColors[0] & 0x00FFFFFF).toString(16).padStart(6, '0')
    const c2 = '#' + (albumLightColors[albumLightColors.length - 1] & 0x00FFFFFF).toString(16).padStart(6, '0')
    activeGradientStyle = {
      backgroundImage: `linear-gradient(135deg, ${c1}, ${c2})`,
      WebkitBackgroundClip: 'text',
      WebkitTextFillColor: 'transparent'
    }
  }

  const formatTime = (ms) => {
    const totalSecs = Math.floor(ms / 1000)
    const mins = Math.floor(totalSecs / 60)
    const secs = totalSecs % 60
    return `${mins}:${secs < 10 ? '0' : ''}${secs}`
  }

  return (
    <div className="relative w-full h-full bg-black flex flex-col items-center justify-center overflow-hidden select-none px-6 py-12">
      {/* Top Header Bar */}
      <div className="absolute top-6 left-6 right-6 z-30 flex items-center justify-between pointer-events-none">
        <div className="flex items-center gap-3 pointer-events-auto">
          {/* Audio File Badge */}
          <div className="px-4 py-2.5 rounded-full bg-surface/90 border border-outline/80 text-ivory text-xs font-mono tracking-wider flex items-center gap-2.5 shadow-2xl glass-blur">
            <Music className="w-4 h-4 text-amber" />
            <span className="truncate max-w-[200px]">{audioFileName || 'No Audio Loaded'}</span>
            <span className="w-1.5 h-1.5 rounded-full bg-amber animate-pulse"></span>
          </div>

          {/* Quick Search Button */}
          <button
            onClick={onOpenSearch}
            className="px-4 py-2.5 rounded-full bg-surface/90 hover:bg-elevated border border-outline/80 text-ivory text-xs font-mono tracking-wider transition-all flex items-center gap-2 shadow-2xl glass-blur"
          >
            <Search className="w-4 h-4 text-amber" />
            <span>SEARCH LRCLIB</span>
          </button>
        </div>

        {/* Studio Settings Drawer Toggle */}
        <button
          onClick={onOpenSettings}
          className="pointer-events-auto px-4 py-2.5 rounded-full bg-surface/90 hover:bg-elevated border border-outline/80 text-ivory/90 hover:text-ivory text-xs font-mono tracking-wider transition-all flex items-center gap-2 shadow-2xl glass-blur"
        >
          STUDIO SETTINGS
        </button>
      </div>

      {/* Immersive Center Content */}
      <div className={`w-full max-w-4xl flex flex-col ${textAlign} z-10 transition-all duration-300`}>
        {!hasLyrics ? (
          <div className="flex flex-col items-center justify-center text-center space-y-3 py-20">
            <div className="w-12 h-12 rounded-full bg-surface border border-outline flex items-center justify-center text-amber text-lg font-bold">♪</div>
            <h2 className="text-xl font-semibold text-ivory">Ready for {trackTitle || 'Audio & Lyrics'}</h2>
            <p className="text-sm text-stone-secondary max-w-md">
              Upload your song audio file and search LRCLIB or upload your `.lrc` lyrics file in Studio Settings for 100% precise synchronization.
            </p>
            <button
              onClick={onOpenSearch}
              className="mt-4 px-6 py-3 rounded-xl bg-amber text-amber-ink font-bold text-sm hover:bg-amber/90 transition-colors shadow-lg flex items-center gap-2"
            >
              <Search className="w-4 h-4" />
              Open Player & Lyrics Studio
            </button>
          </div>
        ) : showingIntro ? (
          <div className="flex flex-col items-center text-center space-y-3 animate-fade-in">
            <h1
              style={activeGradientStyle || { color: activeColor }}
              className={`${fontClass} font-bold text-4xl md:text-6xl tracking-tight leading-tight`}
            >
              {trackTitle}
            </h1>
            {artistName && (
              <p className="text-stone-secondary text-lg font-medium tracking-wide">
                {artistName}
              </p>
            )}
          </div>
        ) : document.syncType === 'PLAIN' ? (
          <div className="w-full max-h-[70vh] overflow-y-auto px-4 py-6 space-y-4 text-center">
            <pre className={`${fontClass} text-xl md:text-2xl text-ivory/90 leading-relaxed whitespace-pre-wrap font-sans`}>
              {document.plainText}
            </pre>
          </div>
        ) : (
          <div className="flex flex-col items-center text-center space-y-8 w-full">
            <div className="w-full transition-all duration-300 transform">
              {preferences.wordByWordEnabled && activeLine?.words?.length > 0 ? (
                <div className={`flex flex-wrap ${preferences.alignment === 'START' ? 'justify-start' : preferences.alignment === 'END' ? 'justify-end' : 'justify-center'} gap-x-3 gap-y-2`}>
                  {activeLine.words.map((word, wIdx) => {
                    const isReached = positionMs >= word.startMs
                    const opacity = isReached ? 'opacity-100 scale-105' : 'opacity-35 scale-100'
                    const blurClass = preferences.glassBlurEnabled && !isReached ? 'blur-[2px]' : 'blur-0'

                    return (
                      <span
                        key={wIdx}
                        style={activeGradientStyle && isReached ? activeGradientStyle : { color: isReached ? activeColor : '#A9A296' }}
                        className={`${fontClass} font-bold text-3xl md:text-5xl lg:text-6xl transition-all duration-200 transform ${opacity} ${blurClass}`}
                      >
                        {word.text}
                      </span>
                    )
                  })}
                </div>
              ) : (
                <h2
                  style={activeGradientStyle || { color: activeColor }}
                  className={`${fontClass} font-bold text-3xl md:text-5xl lg:text-6xl tracking-tight leading-tight transition-all duration-300 ${preferences.glassBlurEnabled ? 'glass-blur' : ''}`}
                >
                  {activeLine ? activeLine.text : '♪'}
                </h2>
              )}
            </div>

            {nextLine && (
              <div className="transition-all duration-300 pt-2">
                <p className={`${fontClass} text-lg md:text-2xl text-stone-secondary/60 font-medium tracking-wide`}>
                  {nextLine}
                </p>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Floating Audio Transport Bar when audio is loaded */}
      {audioFileName && (
        <div className="absolute bottom-6 left-1/2 -translate-x-1/2 z-30 w-full max-w-xl px-6">
          <div className="bg-surface/90 border border-outline/80 rounded-2xl p-4 shadow-2xl glass-blur flex flex-col gap-2">
            <div className="flex items-center justify-between text-xs font-mono text-stone-secondary">
              <span className="truncate text-ivory font-bold">{audioFileName}</span>
              <span>{formatTime(positionMs)}</span>
            </div>
            <input
              type="range"
              min="0"
              max="100"
              value="0"
              onChange={(e) => onSeek(parseFloat(e.target.value))}
              className="w-full h-1.5 bg-obsidian rounded-lg accent-amber cursor-pointer"
            />
            <div className="flex justify-center pt-1">
              <button
                onClick={onTogglePlayPause}
                className="w-11 h-11 rounded-full bg-amber text-amber-ink hover:bg-amber/90 flex items-center justify-center shadow-lg transition-transform hover:scale-105"
              >
                {isPlaying ? <Pause className="w-5 h-5 fill-current" /> : <Play className="w-5 h-5 fill-current ml-0.5" />}
              </button>
            </div>
          </div>
        </div>
      )}

      <div className="absolute bottom-6 left-6 text-xs font-mono text-stone-secondary/50 tracking-wider flex items-center gap-3">
        <span>LYRICVERSE WEB</span>
        <span>·</span>
        <span>AMOLED #000000 STAGE</span>
      </div>
    </div>
  )
}
