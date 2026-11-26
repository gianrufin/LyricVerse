/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        obsidian: '#0B0A09',
        surface: '#141210',
        elevated: '#1D1A17',
        highest: '#27231E',
        amber: {
          DEFAULT: '#E5A243',
          ink: '#161108',
          container: '#332613',
          onContainer: '#F6D9A6',
        },
        ivory: '#F4EFE6',
        sand: '#E0CFB0',
        cyan: '#7CD4D8',
        stone: {
          secondary: '#A9A296',
          muted: '#7D766B',
        },
        outline: '#3A342D',
        hairline: '#26221E',
      },
      fontFamily: {
        jakarta: ['"Plus Jakarta Sans"', 'sans-serif'],
        outfit: ['Outfit', 'sans-serif'],
        space: ['"Space Grotesk"', 'sans-serif'],
        sora: ['Sora', 'sans-serif'],
        dmsans: ['"DM Sans"', 'sans-serif'],
        mono: ['"JetBrains Mono"', 'monospace'],
        instrument: ['"Instrument Serif"', 'serif'],
        cormorant: ['"Cormorant Garamond"', 'serif'],
        playfair: ['"Playfair Display"', 'serif'],
        syne: ['Syne', 'sans-serif'],
        fraunces: ['Fraunces', 'serif'],
        cinzel: ['Cinzel', 'serif'],
      }
    },
  },
  plugins: [],
}
