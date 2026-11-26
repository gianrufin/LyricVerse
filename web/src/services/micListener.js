/**
 * Real browser audio capture & recognition service for LyricVerse Web.
 * - Microphone capture via navigator.mediaDevices.getUserMedia
 * - Browser Tab Audio capture via navigator.mediaDevices.getDisplayMedia (Chrome/Edge)
 * - Real-time RMS audio metering via Web Audio API AnalyserNode
 * - Acoustic fingerprinting via AudD (https://api.audd.io/) returning track + live timecode
 * - Keyless Web Speech API phrase matching fallback for mic input
 */

export function parseTimecodeToMs(timecode) {
  if (!timecode || typeof timecode !== 'string') return 0
  const parts = timecode.trim().split(':').map((n) => parseFloat(n))
  if (parts.some((n) => Number.isNaN(n))) return 0
  if (parts.length === 2) {
    return Math.round((parts[0] * 60 + parts[1]) * 1000)
  }
  if (parts.length === 3) {
    return Math.round((parts[0] * 3600 + parts[1] * 60 + parts[2]) * 1000)
  }
  return 0
}

export async function startMicCapture() {
  if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
    throw new Error('Microphone access requires a modern browser over HTTPS or localhost.')
  }

  let stream
  try {
    stream = await navigator.mediaDevices.getUserMedia({
      audio: {
        echoCancellation: false,
        noiseSuppression: false,
        autoGainControl: false
      }
    })
  } catch {
    stream = await navigator.mediaDevices.getUserMedia({ audio: true })
  }

  return {
    stream,
    captureMode: 'MIC'
  }
}

export async function startTabAudioCapture() {
  if (!navigator.mediaDevices || !navigator.mediaDevices.getDisplayMedia) {
    throw new Error('Tab audio capture is not supported in this browser. Try Chrome or Edge on desktop.')
  }

  const displayStream = await navigator.mediaDevices.getDisplayMedia({
    video: true,
    audio: {
      echoCancellation: false,
      noiseSuppression: false,
      autoGainControl: false
    }
  })

  const audioTracks = displayStream.getAudioTracks()
  // Stop video tracks immediately if audio is present so we don't hold screen video frames
  if (audioTracks.length === 0) {
    displayStream.getTracks().forEach((t) => t.stop())
    throw new Error('No audio track was shared. Pick your Spotify/YouTube Music tab and check "Share tab audio".')
  }

  displayStream.getVideoTracks().forEach((t) => t.stop())
  const audioOnlyStream = new MediaStream(audioTracks)

  return {
    stream: audioOnlyStream,
    captureMode: 'TAB_AUDIO'
  }
}

export function createAudioLevelMonitor(stream, onLevel) {
  const AudioCtx = window.AudioContext || window.webkitAudioContext
  if (!AudioCtx) {
    return { stop: () => {} }
  }

  const audioCtx = new AudioCtx()
  if (audioCtx.state === 'suspended') {
    audioCtx.resume().catch(() => {})
  }

  const source = audioCtx.createMediaStreamSource(stream)
  const analyser = audioCtx.createAnalyser()
  analyser.fftSize = 1024
  analyser.smoothingTimeConstant = 0.75
  source.connect(analyser)

  const buffer = new Uint8Array(analyser.fftSize)
  let rafId = null
  let lastEmit = 0

  const tick = (now) => {
    if (now - lastEmit >= 75) {
      analyser.getByteTimeDomainData(buffer)
      let sumSquares = 0
      for (let i = 0; i < buffer.length; i++) {
        const normalized = (buffer[i] - 128) / 128
        sumSquares += normalized * normalized
      }
      const rms = Math.sqrt(sumSquares / buffer.length)
      const scaled = Math.min(1, rms * 3.8)
      onLevel(scaled)
      lastEmit = now
    }
    rafId = requestAnimationFrame(tick)
  }

  rafId = requestAnimationFrame(tick)

  return {
    stop() {
      if (rafId) cancelAnimationFrame(rafId)
      try {
        source.disconnect()
      } catch {}
      try {
        analyser.disconnect()
      } catch {}
      audioCtx.close().catch(() => {})
    }
  }
}

function selectRecorderMimeType() {
  if (typeof MediaRecorder === 'undefined') return ''
  const candidates = [
    'audio/webm;codecs=opus',
    'audio/webm',
    'audio/mp4',
    'audio/ogg;codecs=opus'
  ]
  for (const type of candidates) {
    if (MediaRecorder.isTypeSupported(type)) return type
  }
  return ''
}

export async function recordAudioSnippet(stream, durationMs = 5500) {
  if (typeof MediaRecorder === 'undefined') {
    throw new Error('MediaRecorder is unavailable in this browser.')
  }

  const mimeType = selectRecorderMimeType()
  const recorder = mimeType ? new MediaRecorder(stream, { mimeType }) : new MediaRecorder(stream)
  const chunks = []

  return new Promise((resolve, reject) => {
    let stopTimer = null

    recorder.ondataavailable = (event) => {
      if (event.data && event.data.size > 0) {
        chunks.push(event.data)
      }
    }

    recorder.onerror = (event) => {
      if (stopTimer) clearTimeout(stopTimer)
      reject(event.error || new Error('Failed while recording audio snippet.'))
    }

    recorder.onstop = () => {
      if (stopTimer) clearTimeout(stopTimer)
      const stoppedAt = Date.now()
      const blob = new Blob(chunks, { type: mimeType || 'audio/webm' })
      resolve({ blob, stoppedAt })
    }

    try {
      recorder.start(250)
      stopTimer = setTimeout(() => {
        if (recorder.state === 'recording') {
          recorder.stop()
        }
      }, durationMs)
    } catch (err) {
      reject(err)
    }
  })
}

export async function identifyWithAudD(stream, apiToken, onStageChange) {
  const token = (apiToken || '').trim()
  if (!token) {
    throw new Error('AudD API token is required for acoustic song identification.')
  }

  onStageChange?.('SAMPLING')
  const { blob, stoppedAt } = await recordAudioSnippet(stream, 5500)
  if (!blob || blob.size < 1024) {
    throw new Error('Captured audio clip was empty. Make sure audio is playing.')
  }

  onStageChange?.('IDENTIFYING')
  const formData = new FormData()
  formData.append('api_token', token)
  formData.append('file', blob, 'lyricverse_clip.webm')
  formData.append('return', 'lyrics')

  const response = await fetch('https://api.audd.io/', {
    method: 'POST',
    body: formData
  })

  if (!response.ok) {
    throw new Error(`Recognition request failed (HTTP ${response.status}).`)
  }

  const data = await response.json()
  if (data.status === 'error') {
    throw new Error(data.error?.error_message || 'AudD rejected the recognition request.')
  }

  if (!data.result) {
    return {
      matched: false,
      reason: 'No match found in that audio clip. Try a louder part of the song or Tab Audio capture.'
    }
  }

  const timecodeMs = parseTimecodeToMs(data.result.timecode)
  const networkLatencyMs = Math.max(0, Date.now() - stoppedAt)
  const estimatedPositionMs = timecodeMs > 0 ? timecodeMs + networkLatencyMs : 0

  return {
    matched: true,
    title: data.result.title || 'Unknown Track',
    artist: data.result.artist || 'Unknown Artist',
    album: data.result.album || '',
    timecodeRaw: data.result.timecode || '',
    timecodeMs,
    estimatedPositionMs,
    plainLyricsFallback: data.result.lyrics?.lyrics || ''
  }
}

export function isSpeechRecognitionSupported() {
  return typeof window !== 'undefined' && Boolean(window.SpeechRecognition || window.webkitSpeechRecognition)
}

export function createSpeechLyricWatcher({ onTranscript, onError, onEnd }) {
  const SpeechRec = typeof window !== 'undefined' ? (window.SpeechRecognition || window.webkitSpeechRecognition) : null
  if (!SpeechRec) return null

  let recognition = null
  let shouldKeepRunning = false

  const start = () => {
    shouldKeepRunning = true
    if (recognition) {
      try { recognition.abort() } catch {}
    }

    recognition = new SpeechRec()
    recognition.continuous = true
    recognition.interimResults = true
    recognition.lang = navigator.language || 'en-US'

    recognition.onresult = (event) => {
      for (let i = event.resultIndex; i < event.results.length; i++) {
        const result = event.results[i]
        const text = result?.[0]?.transcript?.trim()
        if (text) {
          onTranscript?.(text, result.isFinal)
        }
      }
    }

    recognition.onerror = (event) => {
      if (event.error === 'no-speech' || event.error === 'aborted') return
      onError?.(event.error || 'Speech recognition error')
    }

    recognition.onend = () => {
      if (shouldKeepRunning) {
        try {
          recognition.start()
        } catch {
          shouldKeepRunning = false
          onEnd?.()
        }
      } else {
        onEnd?.()
      }
    }

    try {
      recognition.start()
    } catch (err) {
      shouldKeepRunning = false
      onError?.(err.message || 'Unable to start speech recognition')
    }
  }

  const stop = () => {
    shouldKeepRunning = false
    if (recognition) {
      try { recognition.stop() } catch {}
      recognition = null
    }
  }

  return { start, stop }
}

const STOP_WORDS = new Set(['i', 'a', 'the', 'to', 'and', 'of', 'in', 'is', 'it', 'you', 'me', 'my', 'on', 'for', 'we'])

function normalizeTokens(text) {
  return (text || '')
    .toLowerCase()
    .replace(/[^a-z0-9\s']/g, ' ')
    .split(/\s+/)
    .filter((w) => w.length > 1 && !STOP_WORDS.has(w))
}

export function matchTranscriptToLines(transcript, lines, currentIndex = -1) {
  if (!transcript || !lines || lines.length === 0) return null
  const heardTokens = normalizeTokens(transcript)
  if (heardTokens.length < 2) return null

  const heardSet = new Set(heardTokens)
  let best = null

  for (let i = 0; i < lines.length; i++) {
    const lineTokens = normalizeTokens(lines[i].text)
    if (lineTokens.length === 0) continue

    let overlap = 0
    for (const token of lineTokens) {
      if (heardSet.has(token)) overlap++
    }
    if (overlap < 2 && !(lineTokens.length === 1 && overlap === 1)) continue

    const ratio = overlap / Math.max(lineTokens.length, heardTokens.length)
    // Slight preference for lines near or slightly ahead of the current line
    const proximityBonus = currentIndex >= 0 && i >= currentIndex && i <= currentIndex + 8 ? 0.12 : 0
    const score = ratio + proximityBonus

    if (score >= 0.45 && (!best || score > best.score)) {
      best = {
        index: i,
        timestampMs: lines[i].timestampMs,
        score
      }
    }
  }

  return best
}
