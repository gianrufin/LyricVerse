import React, { useState, useEffect } from 'react'
import { Disc, Tv, Search, FolderOpen } from 'lucide-react'

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
  audioSource,
  onChangeAudioSource,
  onOpenSearch
}) {
  const [showingIntro, setShowingIntro] = useState(false)
  const [isSourceDropdownOpen, setIsSourceDropdownOpen] = useState(false)

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

  const sourceLabels = {
    SPOTIFY: { label: 'Spotify / YouTube Music', icon: Disc },
    CUSTOM: { label: 'Custom LRC', icon: FolderOpen }
  }

  const CurrentSourceIcon = sourceLabels[audioSource]?.icon || Disc

  return (
    <div className="relative w-full h-full bg-black flex flex-col items-center justify-center overflow-hidden select-none px-6 py-12">
      {/* Top Header Bar */}
      <div className="absolute top-6 left-6 right-6 z-30 flex items-center justify-between pointer-events-none">
        <div className="flex items-center gap-3 pointer-events-auto">
          {/* Audio Source Badge */}
          <div className="relative">
            <button
              onClick={() => setIsSourceDropdownOpen(!isSourceDropdownOpen)}
              className="px-4 py-2.5 rounded-full bg-surface/90 hover:bg-elevated border border-outline/80 text-ivory text-xs font-mono tracking-wider transition-all flex items-center gap-2.5 shadow-2xl glass-blur"
            >
              <CurrentSourceIcon className="w-4 h-4 text-amber" />
              <span>{sourceLabels[audioSource]?.label}</span>
              <span className="w-1.5 h-1.5 rounded-full bg-amber animate-pulse"></span>
            </button>

            {isSourceDropdownOpen && (
              <div className="absolute top-full left-0 mt-2 w-56 bg-surface border border-outline rounded-2xl shadow-2xl overflow-hidden z-40 glass-blur">
                {[
                  { id: 'SPOTIFY', label: 'Spotify / YouTube Music', icon: Disc },
                  { id: 'CUSTOM', label: 'Custom Upload / File', icon: FolderOpen }
                ].map(src => {
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
                        isSelected ? 'bg-elevated text-amber font-bold' : 'text-stone-secondary hover:text-ivory hover:bg-obsidian'
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
            <h2 className="text-xl font-semibold text-ivory">Ready for {trackTitle || 'Spotify / YouTube Music'}</h2>
            <p className="text-sm text-stone-secondary max-w-md">
              Due to browser security isolation between tabs, use <strong className="text-ivory">Search LRCLIB</strong> above to instantly load synced lyrics for the song playing in your Spotify or YouTube Music app.
            </p>
            <button
              onClick={onOpenSearch}
              className="mt-4 px-6 py-3 rounded-xl bg-amber text-amber-ink font-bold text-sm hover:bg-amber/90 transition-colors shadow-lg"
            >
              Search Song Lyrics
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

      <div className="absolute bottom-6 left-6 text-xs font-mono text-stone-secondary/50 tracking-wider flex items-center gap-3">
        <span>LYRICVERSE WEB</span>
        <span>·</span>
        <span>AMOLED #000000 STAGE</span>
      </div>
    </div>
  )
}
