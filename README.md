# LyricVerse — Immersive Karaoke Companion

LyricVerse is a professional, art-directed karaoke lyrics companion designed for both **Android** (passenger lock-screen AMOLED display and media tracking) and **Web** (full-window desktop stage with Spotify & YouTube Music media session synchronization).

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

The standalone web edition offers a full-window browser stage with custom styling, LRCLIB online lookup, and desktop media synchronization.

### Hosting on GitHub Pages
1. Push this repository to GitHub.
2. Go to your repository **Settings** > **Pages**.
3. Under **Build and deployment**, set **Source** to **GitHub Actions**.
4. The included workflow (`.github/workflows/deploy.yml`) will automatically build and deploy your web app to GitHub Pages on every push to `main`.

### Running Locally
```bash
cd web
npm install
npm run dev
```
Open `http://localhost:3000` in your browser.

---

## 🎧 3. Desktop Media Integration (Spotify Web & YouTube Music)

To prevent computer audio confusion when multiple browser tabs or apps play media, LyricVerse Web features a **Player Source Selector** badge in the stage header.

### How to Use Media Integration
1. Click the source badge in the top-left header (defaults to **Demo Mode**).
2. Switch the source to **Spotify Web** or **YouTube Music**.
3. When playing music in Spotify Web player or YouTube Music browser tabs, LyricVerse automatically queries `navigator.mediaSession` and active media elements to sync track titles, artists, and live timestamps in real time.
4. Use **Custom LRC** to drag-and-drop or upload your own `.lrc` lyrics files.

### Best Practices for Web Version
- **Second-Screen Setup**: Open LyricVerse Web on an external monitor, tablet, or secondary browser window in full-screen mode (`F11`).
- **Browser Permissions**: Ensure your browser allows media session activity and background tab timers.
