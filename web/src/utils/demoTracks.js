import { parseLrc } from './lrcParser'

const rawBlindingLights = `[00:03.50] Yeah
[00:08.20] I've been tryna call
[00:11.40] I've been on my own for long enough
[00:15.90] Maybe you can show me how to love, maybe
[00:21.00] I'm going through withdrawals
[00:24.20] You don't even have to do too much
[00:28.50] You can turn me on with just a touch, baby
[00:33.80] I look around and Sin City's cold and empty
[00:40.20] No one's around to judge me
[00:43.90] I can't see clearly when you're gone
[00:48.50] I said, ooh, I'm blinded by the lights
[00:55.20] No, I can't sleep until I feel your touch
[01:01.40] I said, ooh, I'm drowning in the night
[01:08.10] Oh, when I'm like this, you're the one I trust`

const rawStarboy = `[00:02.10] I'm tryna put you in the worst mood, ah
[00:07.40] P1 cleaner than your church shoes, ah
[00:12.10] Milli point two just to hurt you, ah
[00:16.80] All red Lamb' just to tease you, ah
[00:21.50] None of these toys on lease too, ah
[00:26.20] Made your whole year in a week too, yah
[00:31.00] Main bitch out your league too, ah
[00:35.80] Side bitch out of your league too, ah
[00:40.50] Look what you've done
[00:43.20] I'm a motherfucking starboy`

const rawAsItWas = `[00:03.10] Come on, Harry, we wanna say goodnight to you
[00:09.40] Hold me back
[00:12.80] As it was
[00:15.10] You know it's not the same as it was
[00:19.80] In this world, it's just us
[00:23.50] You know it's not the same as it was
[00:28.10] Answer the phone
[00:30.90] "Harry, you're no good alone
[00:34.20] Why are you sitting at home on the floor?"
[00:39.10] What kind of pills are you on?`

export const DEMO_TRACKS = [
  {
    trackTitle: "Blinding Lights",
    artistName: "The Weeknd",
    albumName: "After Hours",
    durationMs: 72000,
    albumLightColors: [0xFFE5A243, 0xFFE0CFB0, 0xFF7CD4D8],
    document: parseLrc(rawBlindingLights, "Blinding Lights", "The Weeknd")
  },
  {
    trackTitle: "Starboy",
    artistName: "The Weeknd ft. Daft Punk",
    albumName: "Starboy",
    durationMs: 65000,
    albumLightColors: [0xFFD63447, 0xFFF5B461, 0xFF2A1B3D],
    document: parseLrc(rawStarboy, "Starboy", "The Weeknd ft. Daft Punk")
  },
  {
    trackTitle: "As It Was",
    artistName: "Harry Styles",
    albumName: "Harry's House",
    durationMs: 68000,
    albumLightColors: [0xFFE06D65, 0xFF7CD4D8, 0xFFF4EFE6],
    document: parseLrc(rawAsItWas, "As It Was", "Harry Styles")
  }
]
