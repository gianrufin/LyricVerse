import React, { useState } from 'react'
import { X, Palette, Sparkles, Lock, Sliders, Search, Upload, Music } from 'lucide-react'
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
  { id: 'ALBUM_ART', label: 'Album Color' },
  { id: 'STAGE_WHITE', label: 'Pure White' },
  { id: 'WARM_AMBER', label: 'Stage Amber' },
  { id: 'WARM_IVORY', label: 'Warm Ivory' },
  { id: 'STAGE_CYAN', label: 'Ice Cyan' }
]

const TRANSITION_STYLES = [
  { id: 'APPLE_GLASS', label: 'Glass Dissolve' },
  { id: 'STAGE_SLIDE', label: 'Stage Glide' },
  { id: 'SILK_FADE', label: 'Silk Crossfade' },
  { id: 'SPOTLIGHT_POP', label: 'Spotlight Pop' },
  { id: 'TYPEWRITER_SNAP', label: 'Instant Snap' }
]

export function SettingsDrawer({
  isOpen,
  onClose,
  preferences,
  onUpdatePreference,
  onLoadDocument,
  onLoadAudioFile
}) {
  const [activeTab, setActiveTab] = useState('THEME')
  const [searchQuery, setSearchQuery] = useState('')
  const [searchResults, setSearchResults] = useState([])
  const [isSearching, setIsSearching] = useState(false)
  const [fontCategoryTab, setFontCategoryTab] = useState('STAGE')

  if (!isOpen) return null

  const handleSearch = async (e) => {
    e.preventDefault()
    if (!searchQuery.trim()) return
    setIsSearching(true)
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
        const doc = parseLrc(text, file.name.replace(/\.[^/.]+$/, ''), 'Uploaded File')
        onLoadDocument(doc)
      }
    }
    reader.readAsText(file)
  }

  const handleAudioUpload = (e) => {
    const file = e.target.files?.[0]
    if (!file) return
    onLoadAudioFile(file)
  }

  const filteredFonts = FONT_FAMILIES.filter(f => f.category === fontCategoryTab)

  return (
    <div className="fixed inset-0 z-50 flex justify-end bg-black/60 backdrop-blur-sm transition-opacity">
      <div className="w-full max-w-md h-full bg-surface border-l border-hairline flex flex-col shadow-2xl overflow-hidden animate-slide-left">
        {/* Drawer Header */}
        <div className="flex items-center justify-between px-6 py-5 border-b border-hairline">
          <div>
            <h2 className="text-lg font-bold text-ivory tracking-tight">Studio Settings</h2>
            <p className="text-xs text-stone-secondary">Audio player & LRCLIB search</p>
          </div>
          <button
            onClick={onClose}
            className="w-9 h-9 rounded-full bg-elevated hover:bg-highest flex items-center justify-center text-ivory/80 hover:text-ivory transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Tab Navigation */}
        <div className="flex items-center px-6 border-b border-hairline bg-obsidian">
          {[
            { id: 'THEME', label: 'Theme', icon: Palette },
            { id: 'TRANSITIONS', label: 'Transitions', icon: Sparkles },
            { id: 'SOURCES', label: 'Player & Search', icon: Music }
          ].map(tab => {
            const Icon = tab.icon
            const isActive = activeTab === tab.id
            return (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`flex-1 py-3.5 flex flex-col items-center gap-1 text-xs font-medium border-b-2 transition-colors ${
                  isActive ? 'border-amber text-amber font-semibold' : 'border-transparent text-stone-secondary hover:text-ivory'
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
          {activeTab === 'THEME' && (
            <div className="space-y-6">
              <div>
                <div className="flex items-center justify-between mb-3">
                  <span className="text-xs font-mono tracking-wider text-stone-secondary">FONT FAMILY</span>
                  <div className="flex bg-obsidian p-1 rounded-lg border border-hairline">
                    <button
                      onClick={() => setFontCategoryTab('STAGE')}
                      className={`px-3 py-1 text-xs rounded font-medium transition-colors ${fontCategoryTab === 'STAGE' ? 'bg-amber text-amber-ink' : 'text-stone-secondary hover:text-ivory'}`}
                    >
                      Stage Clean
                    </button>
                    <button
                      onClick={() => setFontCategoryTab('PREMIUM')}
                      className={`px-3 py-1 text-xs rounded font-medium transition-colors ${fontCategoryTab === 'PREMIUM' ? 'bg-amber text-amber-ink' : 'text-stone-secondary hover:text-ivory'}`}
                    >
                      Editorial
                    </button>
                  </div>
                </div>
                <div className="grid grid-cols-2 gap-2">
                  {filteredFonts.map(font => {
                    const selected = preferences.fontFamily === font.id
                    return (
                      <button
                        key={font.id}
                        onClick={() => onUpdatePreference('fontFamily', font.id)}
                        className={`p-3 rounded-xl text-left border transition-all ${
                          selected ? 'bg-elevated border-amber text-ivory font-bold' : 'bg-obsidian border-hairline text-stone-secondary hover:border-outline'
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
                <span className="text-xs font-mono tracking-wider text-stone-secondary block mb-3">LYRIC COLOR & GRADIENT</span>
                <div className="grid grid-cols-2 gap-2">
                  {COLOR_PRESETS.map(preset => {
                    const selected = preferences.colorPreset === preset.id
                    return (
                      <button
                        key={preset.id}
                        onClick={() => onUpdatePreference('colorPreset', preset.id)}
                        className={`p-3 rounded-xl text-left border flex items-center gap-2.5 transition-all ${
                          selected ? 'bg-elevated border-amber text-ivory font-bold' : 'bg-obsidian border-hairline text-stone-secondary hover:border-outline'
                        }`}
                      >
                        <span className="w-3 h-3 rounded-full bg-amber inline-block"></span>
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
                <span className="text-xs font-mono tracking-wider text-stone-secondary block mb-3">TEXT ALIGNMENT</span>
                <div className="grid grid-cols-3 gap-2">
                  {[
                    { id: 'START', label: 'Left' },
                    { id: 'CENTER', label: 'Center' },
                    { id: 'END', label: 'Right' }
                  ].map(align => (
                    <button
                      key={align.id}
                      onClick={() => onUpdatePreference('alignment', align.id)}
                      className={`py-2 rounded-xl text-xs font-medium border text-center transition-all ${
                        preferences.alignment === align.id ? 'bg-elevated border-amber text-ivory font-bold' : 'bg-obsidian border-hairline text-stone-secondary'
                      }`}
                    >
                      {align.label}
                    </button>
                  ))}
                </div>
              </div>
            </div>
          )}

          {activeTab === 'TRANSITIONS' && (
            <div className="space-y-6">
              <div>
                <span className="text-xs font-mono tracking-wider text-stone-secondary block mb-3">TRANSITION STYLE</span>
                <div className="space-y-2">
                  {TRANSITION_STYLES.map(style => {
                    const selected = preferences.transitionStyle === style.id
                    return (
                      <button
                        key={style.id}
                        onClick={() => onUpdatePreference('transitionStyle', style.id)}
                        className={`w-full p-3 rounded-xl text-left border flex items-center justify-between transition-all ${
                          selected ? 'bg-elevated border-amber text-ivory font-bold' : 'bg-obsidian border-hairline text-stone-secondary hover:border-outline'
                        }`}
                      >
                        <span className="text-sm">{style.label}</span>
                        {selected && <span className="w-2 h-2 rounded-full bg-amber"></span>}
                      </button>
                    )
                  })}
                </div>
              </div>

              <div className="h-px bg-hairline" />

              <div className="space-y-4">
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
                    <span className="text-xs text-stone-secondary">Frosted blur entry on upcoming lines</span>
                  </div>
                  <input
                    type="checkbox"
                    checked={preferences.glassBlurEnabled}
                    onChange={(e) => onUpdatePreference('glassBlurEnabled', e.target.checked)}
                    className="w-4 h-4 accent-amber rounded cursor-pointer"
                  />
                </label>
              </div>
            </div>
          )}

          {activeTab === 'SOURCES' && (
            <div className="space-y-6">
              <div>
                <span className="text-xs font-mono tracking-wider text-stone-secondary block mb-3">1. UPLOAD AUDIO FILE (100% PRECISE SYNC)</span>
                <label className="w-full p-4 rounded-xl bg-obsidian hover:bg-elevated border border-dashed border-amber/60 flex flex-col items-center justify-center cursor-pointer transition-colors">
                  <Music className="w-6 h-6 text-amber mb-2" />
                  <span className="text-xs font-bold text-ivory">Upload MP3 / Audio Song</span>
                  <span className="text-[11px] text-stone-secondary mt-1">Plays audio directly in app with automatic sync</span>
                  <input type="file" accept="audio/*" onChange={handleAudioUpload} className="hidden" />
                </label>
              </div>

              <div className="h-px bg-hairline" />

              <div>
                <span className="text-xs font-mono tracking-wider text-stone-secondary block mb-3">2. SEARCH LRCLIB FOR LYRICS</span>
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
                    className="px-4 py-2.5 rounded-xl bg-amber text-amber-ink font-bold text-sm flex items-center gap-2 hover:bg-amber/90 transition-colors"
                  >
                    <Search className="w-4 h-4" />
                  </button>
                </form>

                {isSearching && (
                  <div className="py-6 text-center text-xs text-stone-secondary">Searching LRCLIB database...</div>
                )}

                {searchResults.length > 0 && (
                  <div className="mt-3 space-y-2 max-h-56 overflow-y-auto">
                    {searchResults.map((res, rIdx) => (
                      <div key={rIdx} className="p-3 rounded-xl bg-obsidian border border-hairline flex items-center justify-between">
                        <div className="truncate pr-3">
                          <p className="text-xs font-bold text-ivory truncate">{res.trackTitle}</p>
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

              <div>
                <span className="text-xs font-mono tracking-wider text-stone-secondary block mb-3">3. UPLOAD .LRC FILE</span>
                <label className="w-full p-4 rounded-xl bg-obsidian hover:bg-elevated border border-dashed border-outline flex flex-col items-center justify-center cursor-pointer transition-colors">
                  <Upload className="w-6 h-6 text-amber mb-2" />
                  <span className="text-xs font-medium text-ivory">Click to upload .lrc file</span>
                  <input type="file" accept=".lrc,.txt" onChange={handleLrcUpload} className="hidden" />
                </label>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
