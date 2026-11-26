import React, { useState, useEffect } from 'react'

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
  onOpenSettings
}) {
  const [showingIntro, setShowingIntro] = useState(false)
  const [introKey, setIntroKey] = useState(0)

  const fontClass = FONT_CLASS_MAP[preferences.fontFamily] || 'font-jakarta'
  const textAlign = preferences.alignment === 'START' ? 'text-left items-start' : preferences.alignment === 'END' ? 'text-right items-end' : 'text-center items-center'

  // Trigger brief song intro on new track
  useEffect(() => {
    if (preferences.showTrackIntroOnStart && trackTitle) {
      setShowingIntro(true)
      const timer = setTimeout(() => setShowingIntro(false), 2400)
      return () => clearTimeout(timer)
    } else {
      setShowingIntro(false)
    }
  }, [trackTitle, introKey, preferences.showTrackIntroOnStart])

  const lines = document?.lines || []
  const hasLyrics = document && ((document.syncType === 'SYNCED' && lines.length > 0) || (document.syncType === 'PLAIN' && document.plainText.trim()))

  const activeLine = lines[activeLineIndex]
  const nextLine = preferences.nextLineVisible && lines[activeLineIndex + 1] ? lines[activeLineIndex + 1].text : null

  // Color resolver
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

  return (
    <div className="relative w-full h-full bg-black flex flex-col items-center justify-center overflow-hidden select-none px-6 py-12">
      {/* Settings Drawer Toggle Button in top corner */}
      <button
        onClick={onOpenSettings}
        className="absolute top-6 right-6 z-30 px-4 py-2.5 rounded-full bg-surface/80 hover:bg-elevated border border-outline/60 text-ivory/80 hover:text-ivory text-xs font-mono tracking-wider transition-all flex items-center gap-2 shadow-2xl glass-blur"
      >
        <span className="w-2 h-2 rounded-full bg-amber animate-pulse"></span>
        STUDIO SETTINGS
      </button>

      {/* Immersive Center Content */}
      <div className={`w-full max-w-4xl flex flex-col ${textAlign} z-10 transition-all duration-300`}>
        {!hasLyrics ? (
          <div className="flex flex-col items-center justify-center text-center space-y-3 py-20">
            <div className="w-12 h-12 rounded-full bg-surface border border-outline flex items-center justify-center text-amber text-lg font-bold">♪</div>
            <h2 className="text-xl font-semibold text-ivory">No lyrics available</h2>
            <p className="text-sm text-stone-secondary max-w-sm">
              Play a demo track, search LRCLIB online, or upload an .lrc file in Studio Settings.
            </p>
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
            {/* Active Line */}
            <div className="w-full transition-all duration-300 transform">
              {preferences.wordByWordEnabled && activeLine?.words?.length > 0 ? (
                <div className={`flex flex-wrap ${preferences.alignment === 'START' ? 'justify-start' : preferences.alignment === 'END' ? 'justify-end' : 'justify-center'} gap-x-3 gap-y-2`}>
                  {activeLine.words.map((word, wIdx) => {
                    const isReached = positionMs >= word.startMs
                    const isCurrent = positionMs >= word.startMs && positionMs <= word.endMs
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

            {/* Next Line Preview */}
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

      {/* Footer subtle branding & status */}
      <div className="absolute bottom-6 left-6 text-xs font-mono text-stone-secondary/50 tracking-wider flex items-center gap-3">
        <span>LYRICVERSE WEB</span>
        <span>·</span>
        <span>AMOLED #000000 STAGE</span>
      </div>
    </div>
  )
}
