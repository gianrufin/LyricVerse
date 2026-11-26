# LyricVerse — Immersive Karaoke Companion

LyricVerse is an art-directed karaoke lyrics companion built for both **Android** (passenger lock-screen AMOLED display and system media tracking) and **Web** (full-window desktop stage hosted on GitHub Pages with microphone listening, tab audio capture, and LRCLIB synchronization).

---

## 📱 1. Android App & APK Download

LyricVerse features a token-based **Warm Obsidian, Ivory, and Burnished Amber** design system with hairline-divided settings, a sticky pure-black stage preview, and full lock-screen AMOLED protection.

### How to Download & Install the APK
1. Locate the pre-built universal debug APK in your project export:
   ```
   apk/LyricVerse-v1.0-universal-debug.apk
   ```
2. Transfer or download the APK file onto your Android device.
3. Tap the file to install (ensure "Install from unknown sources" is enabled if prompted).
4. **Required Setup for Passenger Lock-Screen Lyrics**:
   - Open LyricVerse and grant **Notification Listener Access** so it can detect songs playing in Spotify, Apple Music, or YouTube Music.
   - Enable **Auto-Show on Lock Screen** and grant **Display Over Apps** permission so lock-screen lyrics appear automatically when locked during active playback.
   - *Note on Android Auto*: Car head units are excluded for driver safety; lyrics continue tracking on your phone lock screen while audio routes through your car or Bluetooth.

---

## 🌐 2. Web App & GitHub Pages Hosting

The standalone web edition (`web/`) delivers a pure `#000000` full-window browser stage with custom typography, transition animations, Wake Lock support, LRCLIB search, local audio/LRC playback, and real-time audio listening.

### Hosting on GitHub Pages
1. Push this repository to GitHub.
2. Go to your repository **Settings** > **Pages**.
3. Under **Build and deployment**, set **Source** to **GitHub Actions**.
4. The included workflow (`.github/workflows/deploy.yml`) automatically builds and deploys `web/` to GitHub Pages on every push to `main`.

### Running Locally
```bash
cd web
npm install
npm run dev
```
Open `http://localhost:3000` in your browser.

---

## 🎙️ 3. Listening to Spotify Web or YouTube Music on Desktop

Desktop browsers isolate separate tabs (like `open.spotify.com` or `music.youtube.com`) for security, so a static web page cannot read another tab's metadata directly. LyricVerse Web bridges this using two real browser audio-capture options:

1. **Microphone Listening (`MIC LISTEN`)**:
   - Captures audio playing from your speakers or room via `getUserMedia` with a real-time Web Audio input meter.
   - **With an AudD Token** *(recommended for Spotify/YouTube Music)*: Paste your personal `api_token` from [audd.io](https://audd.io/) into **Settings → Listen & Search** (stored locally in your browser). LyricVerse samples a ~5.5-second audio clip, identifies the exact song and live `timecode`, fetches synced lyrics from LRCLIB, and jumps straight to the matching lyric line.
   - **Without an API Key (Chrome/Edge Fallback)**: Uses the browser's `SpeechRecognition` API to listen for audible vocal phrases—syncing to matching lines in a loaded song or suggesting LRCLIB matches when clear lyrics are heard.
2. **Browser Tab Audio Capture (`TAB AUDIO`)**:
   - On desktop Chrome or Edge, click **Tab Audio**, pick your Spotify Web or YouTube Music tab, and enable **Share tab audio**. This captures crystal-clear digital tab audio with zero room noise for AudD identification.
3. **Manual Fine-Tuning & Local Playback**:
   - Use the `-0.5s` / `+0.5s` sync calibration controls at the bottom of the stage to nudge lyric timing cleanly.
   - Or upload an MP3/audio file or `.lrc` file in **Settings → Listen & Search** for frame-locked local playback.

### Best Practices for Web Version
- **Second-Screen Stage**: Open LyricVerse Web on a secondary monitor or tablet in full-screen mode (`F11`) with **Keep Screen Awake** enabled.
- **Tab Audio vs. Mic**: Use **Tab Audio** when playing Spotify Web or YouTube Music in the same desktop browser, and **Mic Listen** when audio is playing from external speakers, a phone, or the Spotify desktop app.
