<p align="center"><img src="docs/logo.svg" width="96" alt="Vibe Visualizer logo"></p>

# Vibe Visualizer for Android

The native Android app of [Vibe Visualizer](https://github.com/BOZKOW/vibe-visualizer): a music visualizer with
word-by-word synced lyrics, animated the way [Spicy Lyrics](https://github.com/Spikerko/spicy-lyrics) animates them,
with romanization and translation, the cover, a live background made from it, Spotify Canvas and audio-reactive bars.

It follows what you play: your own audio files, Spotify (the Web API, and the Spotify app on the phone for exact
timing), and other music apps such as YouTube Music, SoundCloud, Apple Music or Deezer.

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
2. The setup guide asks for your Vibe Visualizer server's address (`NAME.your-tailnet.ts.net`), set up as in the
   [main repository](https://github.com/BOZKOW/vibe-visualizer#setup). Keep Tailscale connected on the phone; if the
   server can't be found, turn off Android's Private DNS (it hides Tailscale names). Without a server the app still
   plays your files and finds lyrics from the sources that don't need it.
3. Spotify logs in with the same Client ID and Redirect URI as the web app: Spotify comes back to your server's page,
   which hands the login to the app.

Updates install from inside the app: *Settings → Help & about → Update now*. Versions before 0.24 came from the
main repository's Releases page; 0.24 and later come from here.

## What it does

- **Lyrics**: Spicy Lyrics (through your server), Spotify's own, LRCLIB, AMLL, Unison, LRCMux, lrc.red, NetEase and,
  if you turn them on, KuGou, QQ Music and Kuwo, plus your own `.lrc` and `.ttml` files. The first source asks alone,
  then the rest at once; better timing wins. Romanization (Japanese, Chinese, Korean, Cyrillic…) and translation
  (Google, DeepL, LibreTranslate, any OpenAI-compatible AI, Gemini).
- **Following the music**: your files (with background playback and media controls), Spotify, the Spotify app on
  the phone and other music apps through their media sessions (with notification access; no notification is read).
- **The player**: Now Playing, full-screen lyrics and the visualizer, a landscape layout, picture-in-picture, a home
  screen widget, Ambient mode (an always-on display while music plays), Spotify Canvas, and two designs (Android and
  Glass).
- **The bars** hear everything the phone plays (Android's Visualizer; it needs the microphone permission, and nothing
  is recorded or sent), the microphone, or [Vibe Bridge](https://github.com/BOZKOW/vibe-visualizer#vibe-bridge-bars-from-the-spotify-app)
  through your server.
- The next song in the queue (Spotify's, the music app's or your files') gets its lyrics, Canvas and cover ready while
  the song before it plays; Canvas videos are kept on the phone.
- Lyrics timing per audio output, a heat warning and Low performance mode with Power saving, keyboard shortcuts, and
  settings that can be copied to the web app and back (*Settings → Help & about → Back up settings*).

## Building

Kotlin, Jetpack Compose (Material 3 Expressive), Media3, OkHttp and kuromoji; AGP 9.4, compileSdk 37, minSdk 31.

```
./gradlew testDebugUnitTest assembleDebug
```

`.github/workflows/android.yml` builds every push. A tag like `android-v0.24.0` publishes a release with
`RELEASE_NOTES.md` as its notes (also shown in the app's *What's new*), signed with the release key in the repository
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
audio output, the lookup order (the first source alone, then the rest at once, better timing winning), the back-off for
failing sources, the 20-second limit per source, the cleaning of file names, word timing in LRC lyrics and the
translations under their lines, the Unison, LRCMux, lrc.red, KuGou, QQ Music and Kuwo sources, the
translation services, session stickiness, Spicy Lyrics' Canvas service, Ambient mode, the widget and the Now Playing
layout. Its LRCMux source is itself adapted from Lyrica's lrcmux adapter (MIT).

The lyrics font option "Spicy Lyrics" uses [Inter](https://rsms.me/inter/) by Rasmus Andersson (SIL Open Font
License 1.1). Japanese romanization uses [kuromoji](https://github.com/atilika/kuromoji) (Apache 2.0).

## License

Vibe Visualizer for Android is free software, licensed under the [GNU Affero General Public License v3.0](LICENSE)
(AGPL-3.0), because it includes code ported from Spicy Lyrics and Spicy Lyrics Mobile. The app links to its source
code under *Settings → Help & about*.
