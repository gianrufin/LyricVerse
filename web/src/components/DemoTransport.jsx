import React from 'react'
import { Play, Pause, SkipBack, SkipForward } from 'lucide-react'

export function DemoTransport({
  trackTitle,
  artistName,
  isPlaying,
  positionMs,
  durationMs,
  onTogglePlayPause,
  onSeek,
  onNext,
  onPrev
}) {
  const formatTime = (ms) => {
    const totalSecs = Math.floor(ms / 1000)
    const mins = Math.floor(totalSecs / 60)
    const secs = totalSecs % 60
    return `${mins}:${secs < 10 ? '0' : ''}${secs}`
  }

  return (
    <div className="absolute bottom-6 left-1/2 -translate-x-1/2 z-30 w-full max-w-xl px-6">
      <div className="bg-surface/90 hover:bg-surface border border-outline/80 rounded-2xl p-4 shadow-2xl glass-blur transition-all flex flex-col gap-3">
        {/* Track header */}
        <div className="flex items-center justify-between">
          <div className="truncate pr-4">
            <h4 className="text-sm font-bold text-ivory truncate">{trackTitle}</h4>
            <p className="text-xs text-stone-secondary truncate">{artistName}</p>
          </div>
          <span className="text-xs font-mono text-stone-secondary">
            {formatTime(positionMs)} / {formatTime(durationMs)}
          </span>
        </div>

        {/* Scrubber */}
        <input
          type="range"
          min="0"
          max={durationMs || 1000}
          value={positionMs}
          onChange={(e) => onSeek(parseInt(e.target.value, 10))}
          className="w-full h-1.5 bg-obsidian rounded-lg accent-amber cursor-pointer"
        />

        {/* Controls */}
        <div className="flex items-center justify-center gap-4 pt-1">
          <button
            onClick={onPrev}
            className="w-9 h-9 rounded-full bg-elevated hover:bg-highest flex items-center justify-center text-ivory/80 hover:text-ivory transition-colors"
          >
            <SkipBack className="w-4 h-4" />
          </button>
          <button
            onClick={onTogglePlayPause}
            className="w-12 h-12 rounded-full bg-amber text-amber-ink hover:bg-amber/90 flex items-center justify-center transition-transform hover:scale-105 shadow-lg"
          >
            {isPlaying ? <Pause className="w-5 h-5 fill-current" /> : <Play className="w-5 h-5 fill-current ml-0.5" />}
          </button>
          <button
            onClick={onNext}
            className="w-12 h-12 rounded-full bg-elevated hover:bg-highest flex items-center justify-center text-ivory/80 hover:text-ivory transition-colors"
          >
            <SkipForward className="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>
  )
}
