<p align="center"><img src="docs/logo.svg" width="96" alt="Vibe Visualizer logo"></p>

# Vibe Visualizer for Android

The native Android app of [Vibe Visualizer](https://github.com/BOZKOW/vibe-visualizer): a music visualizer with
word-by-word synced lyrics, animated the way [Spicy Lyrics](https://github.com/Spikerko/spicy-lyrics) animates them,
with romanization and translation, the cover, a live background made from it, Spotify Canvas and audio-reactive bars.

It follows whatever plays on the phone, straight from Android: the Spotify app, other music apps such as YouTube
Music, SoundCloud, Apple Music or Deezer, and your own audio files. No Spotify account or login is needed.

> **Unofficial.** Vibe Visualizer is not made by, endorsed by or affiliated with Spicy Lyrics. Its lyrics animation is
> ported from the Spicy Lyrics source code, and it uses the [Spicy Lyrics API](https://developers.spicylyrics.org)
> through your own server (see [Credits](#credits) and [License](#license)).

The web app, the server helper, the macOS app and Vibe Bridge live in the main repository,
[BOZKOW/vibe-visualizer](https://github.com/BOZKOW/vibe-visualizer).

## Install

Android 12 or newer. The app is a beta.

1. Download `vibe-visualizer-android.apk` from the newest release on the [Releases page](../../releases) on the phone
   and open it (allow installing from your browser when Android asks). Google Play Protect may offer to check the app
   first, as it does with any app from outside the Play Store: that's fine to allow.
2. Give Vibe Visualizer **notification access** when it asks. That's how it sees what the Spotify app and other music
   apps are playing and controls them; it never reads your notifications.
3. The setup guide asks whether to use a server:
   - **With your Vibe Visualizer server** (set up as in the [main repository](https://github.com/BOZKOW/vibe-visualizer#setup)):
     enter its address (`NAME.your-tailnet.ts.net`) and keep Tailscale connected on the phone; if the server can't be
     found, turn off Android's Private DNS (it hides Tailscale names). The server is only asked for what needs it:
     Spicy Lyrics, Spotify's own lyrics, your Canvas source and Vibe Bridge.
   - **Without a server**: everything else runs on the phone (lyrics from LRCLIB, NetEase, AMLL, Unison, LRCMux,
     lrc.red and the Chinese catalogues, Canvas from Spicy Lyrics' Canvas service, the bars, the widget…).

   You can switch later in *Settings → Server & connection*.

Updates install from inside the app: *Settings → Help & about → Update now*. After an update, a card shows what's new.

## What it does

- **Lyrics**: Spicy Lyrics (through your server), Spotify's own, LRCLIB, AMLL, Unison, LRCMux, lrc.red, NetEase and,
  if you turn them on, KuGou, QQ Music and Kuwo, plus your own `.lrc` and `.ttml` files. Your order decides which
  source wins. Duets and background vocals get their own place, and a line too tall for the screen scrolls with the
  singing. Lyrics without timing scroll with the song and hold still through quiet parts.
- **Romanization and translation**: Japanese read a whole line at a time, Korean by sound, Chinese by word, Russian
  and Ukrainian, Greek; under the line or in its place. Background vocals get their own. Translation with Google,
  DeepL, LibreTranslate, any OpenAI-compatible AI or Gemini.
- **Three lyrics styles** (*Settings → Lyrics*): Spring, Bloom and Focus. In Focus, a line flies up and away as soon
  as it's sung and the next one comes in from below; in long breaks the cover grows into the lyrics' place.
- **Following the music**: the Spotify app and other music apps through Android (the song, where it is, its cover and
  its own controls, shuffle, repeat and the heart included), and your own files with background playback. The lyrics
  stay in step through Spotify's crossfade, gapless playback, Automix and the DJ.
- **The player**: the song at the top, the lyrics, the bars and the controls on one screen; held sideways, the lyrics
  get the right side. Picture-in-picture, a home screen widget, Ambient mode (an always-on display while music plays),
  Spotify Canvas, Apple Music's animated covers (free, no account or key), Up next in a song's last 30 seconds, and
  frosted glass throughout (Liquid glass as an experiment).
- **Buttons you pick**: choose what goes under the controls, including one tap to add a lyrics file or play songs from
  your phone.
- **The bars** hear everything the phone plays (Android's Visualizer; it needs the microphone permission, and nothing
  is recorded or sent), the microphone, or [Vibe Bridge](https://github.com/BOZKOW/vibe-visualizer#vibe-bridge-bars-from-the-spotify-app)
  through your server.
- **Ready before they play**: the next songs in the music app's queue (or your files') get their lyrics, Canvas and
  cover while the song before plays.
- **Only errors interrupt**: something that's broken shows at the top of the player; tap it to see what it means and
  what to do.
- **And also**: lyrics timing per audio output, a desync check that listens to the music, Low performance mode, Motion
  sickness mode, and settings that can be copied to the web app and back (*Settings → Help & about → Back up
  settings*).

## Building

Kotlin, Jetpack Compose (Material 3 Expressive), Media3, OkHttp and kuromoji; AGP 9.4, compileSdk 37, minSdk 31.

```
./gradlew testDebugUnitTest assembleDebug
```

`.github/workflows/android.yml` builds every push. A tag like `android-v0.24.0` publishes a release with
`RELEASE_NOTES.md` as its notes (it also comes with the app, for the card shown after an update), signed with the release key in the repository
secrets `ANDROID_KEYSTORE_B64` and `ANDROID_KEYSTORE_PASS`; without them the build is signed with a debug key. The
build number comes from the version (0.24.0 is 124000), so every release counts up.

## Credits

Made by [BOZKOW](https://github.com/BOZKOW), built together with [Claude Code](https://claude.com/claude-code)
(Anthropic's Claude).

The lyrics animation (word and letter springs, fill gradient, line dimming and blur), the instrumental-break dots, the
credits layout and the way romanization is done are ported from [Spicy Lyrics](https://github.com/Spikerko/spicy-lyrics)
by Spikerko and the Spicy Lyrics contributors (GNU AGPL-3.0). Its spring model is itself a port of
[spr](https://github.com/Fraktality/spr) by Fraktality (MIT).

From [Spicy Lyrics Mobile](https://github.com/spicylyrics/mobile) by TheX24 (GNU AGPL-3.0): background vocals in
brackets and a song's own lyrics delay, the lyric clock that reads music apps' position reports, the timing kept per
audio output, the lookup order (the first source alone, then the rest at once, better timing winning), the back-off
for failing sources, the 20-second limit per source, the song-change handling at the end of a track, the cleaning of
file names, word timing in LRC lyrics and the translations under their lines, the delay per music app, the Canvas
behind the lyrics, the small window's line at the top, the One UI widget size fix, Apple Music's animated covers, your
own background color, Keep syllable pace, the language of the lyrics' own translations, the Unison, LRCMux, lrc.red,
KuGou, QQ Music and Kuwo sources, the translation services, session stickiness, Spicy Lyrics' Canvas service, Ambient
mode, the widget, the Now Playing layout, its glass (0.19), its transport and timeline, reading the Spotify song ID and
the player's own buttons from Android, Chinese read by word, and keeping lyrics longer. Its LRCMux source is itself
adapted from Lyrica's lrcmux adapter (MIT).

Japanese, Korean and Chinese romanization by line, Join split words, Up next and the name scrolling follow Spicy Lyrics
6.5 (romanization by TheX24 for Spikerko/spicy-lyrics).

The lyrics font option "Spicy Lyrics" uses [Inter](https://rsms.me/inter/) by Rasmus Andersson (SIL Open Font
License 1.1). Japanese romanization uses [kuromoji](https://github.com/atilika/kuromoji) (Apache 2.0); Chinese uses
the dictionaries of [pinyin](https://github.com/hotoo/pinyin) 4.0.0 by 闲耘 (MIT).

## License

Vibe Visualizer for Android is free software, licensed under the [GNU Affero General Public License v3.0](LICENSE)
(AGPL-3.0), because it includes code ported from Spicy Lyrics and Spicy Lyrics Mobile. The app links to its source
code under *Settings → Help & about*.
