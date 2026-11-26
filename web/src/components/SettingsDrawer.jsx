import React, { useState } from 'react'
import {
  X,
  Palette,
  Sparkles,
  Search,
  Upload,
  Music,
  Mic,
  MicOff,
  Monitor,
  Key,
  Check,
  Loader2,
  RotateCcw
} from 'lucide-react'
import { searchLrcLib } from '../services/lrcLib'
import { parseLrc } from '../utils/lrcParser'

const FONT_FAMILIES = [
  { id: 'PLUS_JAKARTA_SANS', label: 'Jakarta', category: 'STAGE' },
  { id: 'OUTFIT', label: 'Outfit', category: 'STAGE' },
  { id: 'SPACE_GROTESK', label: 'Space Grotesk', category: 'STAGE' },
  { id: 'SORA', label: 'Sora', category: 'STAGE' },
  { id: 'DM_SANS', label: 'DM Sans', category: 'STAGE' },
  { id: 'JETBRAINS_MONO', label: 'JetBrains Mono', category: 'STAGE' },
  { id: 'INSTRUMENT_SERIF', label: 'Instrument', category: 'PREMIUM' },
  { id: 'CORMORANT_GARAMOND', label: 'Cormorant', category: 'PREMIUM' },
  { id: 'PLAYFAIR_DISPLAY', label: 'Playfair', category: 'PREMIUM' },
  { id: 'SYNE', label: 'Syne', category: 'PREMIUM' },
  { id: 'FRAUNCES', label: 'Fraunces', category: 'PREMIUM' },
  { id: 'CINZEL', label: 'Cinzel', category: 'PREMIUM' }
]

const COLOR_PRESETS = [
  { id: 'ALBUM_ART', label: 'Album Color', swatch: 'linear-gradient(135deg, #E5A243, #7CD4D8)' },
  { id: 'STAGE_WHITE', label: 'Pure White', swatch: '#FFFFFF' },
  { id: 'WARM_AMBER', label: 'Stage Amber', swatch: '#E5A243' },
  { id: 'WARM_IVORY', label: 'Warm Ivory', swatch: '#F4EFE6' },
  { id: 'STAGE_CYAN', label: 'Ice Cyan', swatch: '#7CD4D8' }
]

const TRANSITION_STYLES = [
  { id: 'APPLE_GLASS', label: 'Glass Dissolve' },
  { id: 'STAGE_SLIDE', label: 'Stage Glide' },
  { id: 'SILK_FADE', label: 'Silk Crossfade' },
  { id: 'SPOTLIGHT_POP', label: 'Spotlight Pop' },
  { id: 'TYPEWRITER_SNAP', label: 'Instant Snap' }
]

const TRANSITION_SPEEDS = [
  { id: 'SNAPPY', label: 'Snappy' },
  { id: 'BALANCED', label: 'Balanced' },
  { id: 'DREAMY', label: 'Dreamy' }
]

export function SettingsDrawer({
  isOpen,
  initialTab = 'SOURCES',
  onClose,
  preferences,
  onUpdatePreference,
  onLoadDocument,
  onLoadAudioFile,
  // Mic / Recognition props
  captureState,
  captureMode,
  audioLevel,
  auddToken,
  onSaveAuddToken,
  listenPrefs,
  onUpdateListenPref,
  speechSupported,
  onStartMic,
  onStartTabAudio,
  onStopCapture,
  onIdentifyNow,
  syncOffsetMs,
  onNudgeOffset,
  onResetOffset
}) {
  const [activeTab, setActiveTab] = useState(initialTab)
  const [searchQuery, setSearchQuery] = useState('')
  const [searchResults, setSearchResults] = useState([])
  const [hasSearched, setHasSearched] = useState(false)
  const [isSearching, setIsSearching] = useState(false)
  const [fontCategoryTab, setFontCategoryTab] = useState('STAGE')
  const [tokenInput, setTokenInput] = useState(auddToken || '')
  const [tokenSavedNotice, setTokenSavedNotice] = useState(false)

  React.useEffect(() => {
    if (isOpen && initialTab) {
      setActiveTab(initialTab)
    }
  }, [isOpen, initialTab])

  React.useEffect(() => {
    setTokenInput(auddToken || '')
  }, [auddToken])

  if (!isOpen) return null

  const isListening = captureState === 'LISTENING' || captureState === 'SAMPLING' || captureState === 'IDENTIFYING'
  const isBusyIdentifying = captureState === 'SAMPLING' || captureState === 'IDENTIFYING'

  const handleSearch = async (e) => {
    e.preventDefault()
    if (!searchQuery.trim()) return
    setIsSearching(true)
    setHasSearched(true)
    const results = await searchLrcLib(searchQuery)
    setSearchResults(results)
    setIsSearching(false)
  }

  const handleLrcUpload = (e) => {
    const file = e.target.files?.[0]
    if (!file) return
    const reader = new FileReader()
    reader.onload = (event) => {
      const text = event.target?.result
      if (typeof text === 'string') {
        const doc = parseLrc(text, file.name.replace(/\.[^/.]+$/, ''), 'Uploaded LRC')
        onLoadDocument(doc)
      }
    }
    reader.readAsText(file)
  }

  const handleAudioUpload = (e) => {
    const file = e.target.files?.[0]
    if (!file) return
    onLoadAudioFile?.(file)
  }

  const handleSaveToken = (e) => {
    e.preventDefault()
    onSaveAuddToken(tokenInput.trim())
    setTokenSavedNotice(true)
    setTimeout(() => setTokenSavedNotice(false), 2000)
  }

  const filteredFonts = FONT_FAMILIES.filter((f) => f.category === fontCategoryTab)

  return (
    <div className="fixed inset-0 z-50 flex justify-end bg-black/65 backdrop-blur-sm transition-opacity">
      <div className="w-full max-w-md h-full bg-surface border-l border-hairline flex flex-col shadow-2xl overflow-hidden animate-slide-left">
        {/* Drawer Header */}
        <div className="flex items-center justify-between px-6 py-5 border-b border-hairline">
          <div>
            <h2 className="text-lg font-bold text-ivory tracking-tight">Studio Settings</h2>
            <p className="text-xs text-stone-secondary">Mic listening, LRCLIB search &amp; stage design</p>
          </div>
          <button
            onClick={onClose}
            aria-label="Close Studio Settings"
            className="w-9 h-9 rounded-full bg-elevated hover:bg-highest flex items-center justify-center text-ivory/80 hover:text-ivory transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Tab Navigation */}
        <div className="flex items-center px-6 border-b border-hairline bg-obsidian">
          {[
            { id: 'SOURCES', label: 'Listen & Search', icon: Mic },
            { id: 'THEME', label: 'Theme', icon: Palette },
            { id: 'TRANSITIONS', label: 'Transitions', icon: Sparkles }
          ].map((tab) => {
            const Icon = tab.icon
            const isActive = activeTab === tab.id
            return (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`flex-1 py-3.5 flex flex-col items-center gap-1 text-xs font-medium border-b-2 transition-colors ${
                  isActive
                    ? 'border-amber text-amber font-semibold'
                    : 'border-transparent text-stone-secondary hover:text-ivory'
                }`}
              >
                <Icon className="w-4 h-4" />
                {tab.label}
              </button>
            )
          })}
        </div>

        {/* Drawer Scrollable Body */}
        <div className="flex-1 overflow-y-auto px-6 py-6 space-y-6">
          {activeTab === 'SOURCES' && (
            <div className="space-y-6">
              {/* 1. Microphone & Tab Audio Listening */}
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-mono tracking-wider text-stone-secondary">
                    1. MICROPHONE &amp; TAB AUDIO LISTENING
                  </span>
                  {isListening && (
                    <span className="text-[11px] font-mono text-amber">
                      {captureMode === 'TAB_AUDIO' ? 'TAB ACTIVE' : 'MIC ACTIVE'}
                    </span>
                  )}
                </div>

                <div className="grid grid-cols-2 gap-2">
                  {!isListening ? (
                    <>
                      <button
                        onClick={onStartMic}
                        className="p-3 rounded-xl bg-amber text-amber-ink font-bold text-xs flex items-center justify-center gap-2 hover:bg-amber/90 transition-colors"
                      >
                        <Mic className="w-4 h-4" />
                        <span>Start Mic Listen</span>
                      </button>
                      <button
                        onClick={onStartTabAudio}
                        className="p-3 rounded-xl bg-obsidian hover:bg-elevated border border-outline text-ivory font-semibold text-xs flex items-center justify-center gap-2 transition-colors"
                      >
                        <Monitor className="w-4 h-4 text-cyan" />
                        <span>Capture Tab Audio</span>
                      </button>
                    </>
                  ) : (
                    <>
                      <button
                        onClick={onIdentifyNow}
                        disabled={isBusyIdentifying || !auddToken?.trim()}
                        className="p-3 rounded-xl bg-amber text-amber-ink font-bold text-xs flex items-center justify-center gap-2 hover:bg-amber/90 disabled:opacity-50 transition-colors"
                      >
                        {isBusyIdentifying ? (
                          <Loader2 className="w-4 h-4 animate-spin" />
                        ) : (
                          <Sparkles className="w-4 h-4" />
                        )}
                        <span>{isBusyIdentifying ? 'Identifying…' : 'Identify Song'}</span>
                      </button>
                      <button
                        onClick={onStopCapture}
                        className="p-3 rounded-xl bg-obsidian hover:bg-elevated border border-outline text-ivory font-semibold text-xs flex items-center justify-center gap-2 transition-colors"
                      >
                        <MicOff className="w-4 h-4 text-red-400" />
                        <span>Stop Listening</span>
                      </button>
                    </>
                  )}
                </div>

                {/* Live Audio Meter */}
                {isListening && (
                  <div className="p-3 rounded-xl bg-obsidian border border-hairline space-y-1.5">
                    <div className="flex items-center justify-between text-[11px] font-mono text-stone-secondary">
                      <span>INPUT LEVEL</span>
                      <span>{Math.round(audioLevel * 100)}%</span>
                    </div>
                    <div className="w-full h-1.5 rounded-full bg-elevated overflow-hidden">
                      <div
                        style={{ width: `${Math.min(100, Math.round(audioLevel * 100))}%` }}
                        className="h-full bg-amber transition-all duration-75"
                      />
                    </div>
                  </div>
                )}

                {/* AudD Acoustic Recognition Token */}
                <form onSubmit={handleSaveToken} className="p-3.5 rounded-xl bg-obsidian border border-hairline space-y-2.5">
                  <div className="flex items-center justify-between">
                    <label className="text-xs font-semibold text-ivory flex items-center gap-1.5">
                      <Key className="w-3.5 h-3.5 text-amber" />
                      AudD Acoustic Recognition Token
                    </label>
                    {tokenSavedNotice && (
                      <span className="text-[11px] font-mono text-amber flex items-center gap-1">
                        <Check className="w-3 h-3" /> Saved
                      </span>
                    )}
                  </div>
                  <p className="text-[11px] text-stone-secondary leading-relaxed">
                    Identifies Spotify or YouTube Music from mic or tab audio and jumps to the exact song timestamp via{' '}
                    <span className="text-ivory">audd.io</span>. Stored only in your browser.
                  </p>
                  <div className="flex gap-2">
                    <input
                      type="password"
                      value={tokenInput}
                      onChange={(e) => setTokenInput(e.target.value)}
                      placeholder="Paste AudD api_token..."
                      className="flex-1 bg-surface border border-hairline rounded-lg px-3 py-2 text-xs text-ivory focus:outline-none focus:border-amber"
                    />
                    <button
                      type="submit"
                      className="px-3 py-2 rounded-lg bg-elevated hover:bg-highest border border-outline text-xs font-semibold text-amber transition-colors"
                    >
                      Save
                    </button>
                  </div>
                </form>

                {/* Listening Behavior Toggles */}
                <div className="space-y-2">
                  <label className="flex items-center justify-between p-3 rounded-xl bg-obsidian border border-hairline cursor-pointer">
                    <div>
                      <span className="text-xs font-medium text-ivory block">Auto-Identify on Audio</span>
                      <span className="text-[11px] text-stone-secondary">
                        Automatically sample when music is heard (requires AudD token)
                      </span>
                    </div>
                    <input
                      type="checkbox"
                      checked={listenPrefs.autoIdentify}
                      onChange={(e) => onUpdateListenPref('autoIdentify', e.target.checked)}
                      className="w-4 h-4 accent-amber rounded cursor-pointer"
                    />
                  </label>

                  <label className="flex items-center justify-between p-3 rounded-xl bg-obsidian border border-hairline cursor-pointer">
                    <div>
                      <span className="text-xs font-medium text-ivory block">
                        Browser Speech-to-Lyric Fallback
                      </span>
                      <span className="text-[11px] text-stone-secondary">
                        {speechSupported
                          ? 'Matches audible vocals against loaded lyrics when no token is set'
                          : 'SpeechRecognition is unavailable in this browser (use Chrome/Edge)'}
                      </span>
                    </div>
                    <input
                      type="checkbox"
                      disabled={!speechSupported}
                      checked={listenPrefs.speechFallback && speechSupported}
                      onChange={(e) => onUpdateListenPref('speechFallback', e.target.checked)}
                      className="w-4 h-4 accent-amber rounded cursor-pointer disabled:opacity-40"
                    />
                  </label>
                </div>

                {/* Live Sync Calibration */}
                <div className="p-3 rounded-xl bg-obsidian border border-hairline flex items-center justify-between">
                  <div>
                    <span className="text-xs font-medium text-ivory block">Lyric Sync Offset</span>
                    <span className="text-[11px] font-mono text-amber">
                      {syncOffsetMs === 0
                        ? '0.0s (Live)'
                        : syncOffsetMs > 0
                          ? `+${(syncOffsetMs / 1000).toFixed(1)}s ahead`
                          : `${(syncOffsetMs / 1000).toFixed(1)}s behind`}
                    </span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <button
                      onClick={() => onNudgeOffset(-500)}
                      className="px-2.5 py-1.5 rounded-lg bg-elevated hover:bg-highest border border-outline text-xs font-mono text-ivory"
                    >
                      -0.5s
                    </button>
                    <button
                      onClick={() => onNudgeOffset(500)}
                      className="px-2.5 py-1.5 rounded-lg bg-elevated hover:bg-highest border border-outline text-xs font-mono text-ivory"
                    >
                      +0.5s
                    </button>
                    <button
                      onClick={onResetOffset}
                      title="Reset sync offset"
                      className="p-1.5 rounded-lg bg-elevated hover:bg-highest border border-outline text-stone-secondary hover:text-ivory"
                    >
                      <RotateCcw className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
              </div>

              <div className="h-px bg-hairline" />

              {/* 2. Search LRCLIB */}
              <div>
                <span className="text-xs font-mono tracking-wider text-stone-secondary block mb-3">
                  2. SEARCH LRCLIB FOR LYRICS
                </span>
                <form onSubmit={handleSearch} className="flex gap-2">
                  <input
                    type="text"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    placeholder="Enter song title or artist..."
                    className="flex-1 bg-obsidian border border-hairline rounded-xl px-4 py-2.5 text-sm text-ivory focus:outline-none focus:border-amber"
                  />
                  <button
                    type="submit"
                    aria-label="Search LRCLIB"
                    className="px-4 py-2.5 rounded-xl bg-amber text-amber-ink font-bold text-sm flex items-center gap-2 hover:bg-amber/90 transition-colors"
                  >
                    <Search className="w-4 h-4" />
                  </button>
                </form>

                {isSearching && (
                  <div className="py-5 text-center text-xs text-stone-secondary">Searching LRCLIB database...</div>
                )}

                {!isSearching && hasSearched && searchResults.length === 0 && (
                  <div className="py-4 text-center text-xs text-stone-secondary">
                    No matching lyrics found on LRCLIB.
                  </div>
                )}

                {searchResults.length > 0 && (
                  <div className="mt-3 space-y-2 max-h-56 overflow-y-auto">
                    {searchResults.map((res, rIdx) => (
                      <div
                        key={res.id || rIdx}
                        className="p-3 rounded-xl bg-obsidian border border-hairline flex items-center justify-between gap-2"
                      >
                        <div className="truncate pr-2">
                          <div className="flex items-center gap-1.5">
                            <p className="text-xs font-bold text-ivory truncate">{res.trackTitle}</p>
                            <span
                              className={`px-1.5 py-0.5 rounded text-[9px] font-mono ${
                                res.syncType === 'SYNCED'
                                  ? 'bg-amber/20 text-amber'
                                  : 'bg-elevated text-stone-secondary'
                              }`}
                            >
                              {res.syncType}
                            </span>
                          </div>
                          <p className="text-[11px] text-stone-secondary truncate">{res.artistName}</p>
                        </div>
                        <button
                          onClick={() => onLoadDocument(res)}
                          className="px-3 py-1.5 rounded-lg bg-elevated hover:bg-highest text-xs font-medium text-amber border border-outline whitespace-nowrap"
                        >
                          Load Stage
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              <div className="h-px bg-hairline" />

              {/* 3. Local Audio & LRC Files */}
              <div className="space-y-3">
                <span className="text-xs font-mono tracking-wider text-stone-secondary block">
                  3. LOCAL AUDIO OR .LRC FILE
                </span>

                <label className="w-full p-4 rounded-xl bg-obsidian hover:bg-elevated border border-dashed border-amber/60 flex flex-col items-center justify-center cursor-pointer transition-colors">
                  <Music className="w-5 h-5 text-amber mb-1.5" />
                  <span className="text-xs font-bold text-ivory">Upload MP3 / Audio File</span>
                  <span className="text-[11px] text-stone-secondary mt-0.5 text-center">
                    Plays in-app and auto-fetches synced lyrics from LRCLIB
                  </span>
                  <input type="file" accept="audio/*" onChange={handleAudioUpload} className="hidden" />
                </label>

                <label className="w-full p-4 rounded-xl bg-obsidian hover:bg-elevated border border-dashed border-outline flex flex-col items-center justify-center cursor-pointer transition-colors">
                  <Upload className="w-5 h-5 text-amber mb-1.5" />
                  <span className="text-xs font-medium text-ivory">Upload .lrc File</span>
                  <input type="file" accept=".lrc,.txt" onChange={handleLrcUpload} className="hidden" />
                </label>
              </div>
            </div>
          )}

          {activeTab === 'THEME' && (
            <div className="space-y-6">
              <div>
                <div className="flex items-center justify-between mb-3">
                  <span className="text-xs font-mono tracking-wider text-stone-secondary">FONT FAMILY</span>
                  <div className="flex bg-obsidian p-1 rounded-lg border border-hairline">
                    <button
                      onClick={() => setFontCategoryTab('STAGE')}
                      className={`px-3 py-1 text-xs rounded font-medium transition-colors ${
                        fontCategoryTab === 'STAGE' ? 'bg-amber text-amber-ink' : 'text-stone-secondary hover:text-ivory'
                      }`}
                    >
                      Stage Clean
                    </button>
                    <button
                      onClick={() => setFontCategoryTab('PREMIUM')}
                      className={`px-3 py-1 text-xs rounded font-medium transition-colors ${
                        fontCategoryTab === 'PREMIUM' ? 'bg-amber text-amber-ink' : 'text-stone-secondary hover:text-ivory'
                      }`}
                    >
                      Editorial
                    </button>
                  </div>
                </div>
                <div className="grid grid-cols-2 gap-2">
                  {filteredFonts.map((font) => {
                    const selected = preferences.fontFamily === font.id
                    return (
                      <button
                        key={font.id}
                        onClick={() => onUpdatePreference('fontFamily', font.id)}
                        className={`p-3 rounded-xl text-left border transition-all ${
                          selected
                            ? 'bg-elevated border-amber text-ivory font-bold'
                            : 'bg-obsidian border-hairline text-stone-secondary hover:border-outline'
                        }`}
                      >
                        <span className="text-sm block">{font.label}</span>
                      </button>
                    )
                  })}
                </div>
              </div>

              <div className="h-px bg-hairline" />

              <div>
                <span className="text-xs font-mono tracking-wider text-stone-secondary block mb-3">
                  LYRIC COLOR &amp; GRADIENT
                </span>
                <div className="grid grid-cols-2 gap-2">
                  {COLOR_PRESETS.map((preset) => {
                    const selected = preferences.colorPreset === preset.id
                    return (
                      <button
                        key={preset.id}
                        onClick={() => onUpdatePreference('colorPreset', preset.id)}
                        className={`p-3 rounded-xl text-left border flex items-center gap-2.5 transition-all ${
                          selected
                            ? 'bg-elevated border-amber text-ivory font-bold'
                            : 'bg-obsidian border-hairline text-stone-secondary hover:border-outline'
                        }`}
                      >
                        <span
                          style={{ background: preset.swatch }}
                          className="w-3.5 h-3.5 rounded-full inline-block shrink-0 border border-white/20"
                        />
                        <span className="text-xs">{preset.label}</span>
                      </button>
                    )
                  })}
                </div>
              </div>

              <div className="h-px bg-hairline" />

              <div>
                <div className="flex items-center justify-between mb-2">
                  <span className="text-xs font-mono tracking-wider text-stone-secondary">TYPE SCALE</span>
                  <span className="text-xs font-mono text-amber">{preferences.fontSizeSp} sp</span>
                </div>
                <input
                  type="range"
                  min="20"
                  max="42"
                  value={preferences.fontSizeSp}
                  onChange={(e) => onUpdatePreference('fontSizeSp', parseFloat(e.target.value))}
                  className="w-full accent-amber cursor-pointer"
                />
              </div>

              <div className="h-px bg-hairline" />

              <div>
                <span className="text-xs font-mono tracking-wider text-stone-secondary block mb-3">
                  TEXT ALIGNMENT
                </span>
                <div className="grid grid-cols-3 gap-2">
                  {[
                    { id: 'START', label: 'Left' },
                    { id: 'CENTER', label: 'Center' },
                    { id: 'END', label: 'Right' }
                  ].map((align) => (
                    <button
                      key={align.id}
                      onClick={() => onUpdatePreference('alignment', align.id)}
                      className={`py-2 rounded-xl text-xs font-medium border text-center transition-all ${
                        preferences.alignment === align.id
                          ? 'bg-elevated border-amber text-ivory font-bold'
                          : 'bg-obsidian border-hairline text-stone-secondary'
                      }`}
                    >
                      {align.label}
                    </button>
                  ))}
                </div>
              </div>

              <div className="h-px bg-hairline" />

              <div className="space-y-3">
                <label className="flex items-center justify-between p-3 rounded-xl bg-obsidian border border-hairline cursor-pointer">
                  <div>
                    <span className="text-sm font-medium text-ivory block">Next Line Preview</span>
                    <span className="text-xs text-stone-secondary">Show upcoming lyric below active line</span>
                  </div>
                  <input
                    type="checkbox"
                    checked={preferences.nextLineVisible}
                    onChange={(e) => onUpdatePreference('nextLineVisible', e.target.checked)}
                    className="w-4 h-4 accent-amber rounded cursor-pointer"
                  />
                </label>

                <label className="flex items-center justify-between p-3 rounded-xl bg-obsidian border border-hairline cursor-pointer">
                  <div>
                    <span className="text-sm font-medium text-ivory block">Keep Screen Awake</span>
                    <span className="text-xs text-stone-secondary">Use Screen Wake Lock during stage mode</span>
                  </div>
                  <input
                    type="checkbox"
                    checked={preferences.keepScreenOn}
                    onChange={(e) => onUpdatePreference('keepScreenOn', e.target.checked)}
                    className="w-4 h-4 accent-amber rounded cursor-pointer"
                  />
                </label>
              </div>
            </div>
          )}

          {activeTab === 'TRANSITIONS' && (
            <div className="space-y-6">
              <div>
                <span className="text-xs font-mono tracking-wider text-stone-secondary block mb-3">
                  TRANSITION STYLE
                </span>
                <div className="space-y-2">
                  {TRANSITION_STYLES.map((style) => {
                    const selected = preferences.transitionStyle === style.id
                    return (
                      <button
                        key={style.id}
                        onClick={() => onUpdatePreference('transitionStyle', style.id)}
                        className={`w-full p-3 rounded-xl text-left border flex items-center justify-between transition-all ${
                          selected
                            ? 'bg-elevated border-amber text-ivory font-bold'
                            : 'bg-obsidian border-hairline text-stone-secondary hover:border-outline'
                        }`}
                      >
                        <span className="text-sm">{style.label}</span>
                        {selected && <span className="w-2 h-2 rounded-full bg-amber" />}
                      </button>
                    )
                  })}
                </div>
              </div>

              <div className="h-px bg-hairline" />

              <div>
                <span className="text-xs font-mono tracking-wider text-stone-secondary block mb-3">
                  TRANSITION PACING
                </span>
                <div className="grid grid-cols-3 gap-2">
                  {TRANSITION_SPEEDS.map((spd) => (
                    <button
                      key={spd.id}
                      onClick={() => onUpdatePreference('transitionSpeed', spd.id)}
                      className={`py-2 rounded-xl text-xs font-medium border text-center transition-all ${
                        preferences.transitionSpeed === spd.id
                          ? 'bg-elevated border-amber text-ivory font-bold'
                          : 'bg-obsidian border-hairline text-stone-secondary'
                      }`}
                    >
                      {spd.label}
                    </button>
                  ))}
                </div>
              </div>

              <div className="h-px bg-hairline" />

              <div className="space-y-3">
                <label className="flex items-center justify-between p-3 rounded-xl bg-obsidian border border-hairline cursor-pointer">
                  <div>
                    <span className="text-sm font-medium text-ivory block">Word-by-Word Reveal</span>
                    <span className="text-xs text-stone-secondary">Paces each word smoothly with timing</span>
                  </div>
                  <input
                    type="checkbox"
                    checked={preferences.wordByWordEnabled}
                    onChange={(e) => onUpdatePreference('wordByWordEnabled', e.target.checked)}
                    className="w-4 h-4 accent-amber rounded cursor-pointer"
                  />
                </label>

                <label className="flex items-center justify-between p-3 rounded-xl bg-obsidian border border-hairline cursor-pointer">
                  <div>
                    <span className="text-sm font-medium text-ivory block">Apple-Style Glass Blur</span>
                    <span className="text-xs text-stone-secondary">Frosted blur entry on upcoming words</span>
                  </div>
                  <input
                    type="checkbox"
                    checked={preferences.glassBlurEnabled}
                    onChange={(e) => onUpdatePreference('glassBlurEnabled', e.target.checked)}
                    className="w-4 h-4 accent-amber rounded cursor-pointer"
                  />
                </label>

                <label className="flex items-center justify-between p-3 rounded-xl bg-obsidian border border-hairline cursor-pointer">
                  <div>
                    <span className="text-sm font-medium text-ivory block">Track Intro Card</span>
                    <span className="text-xs text-stone-secondary">Briefly show title &amp; artist when song loads</span>
                  </div>
                  <input
                    type="checkbox"
                    checked={preferences.showTrackIntroOnStart}
                    onChange={(e) => onUpdatePreference('showTrackIntroOnStart', e.target.checked)}
                    className="w-4 h-4 accent-amber rounded cursor-pointer"
                  />
                </label>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
