Vibe Visualizer for Android 0.32.0 (beta). Android 12 or newer; it installs over 0.31.0 (Settings → Help & about → Update now).

### With a server, or without one

- **The setup guide now asks**: *With my server* or *Without a server*. You can switch any time in *Settings → Server & connection* (your server's address is kept, just not used, while you run without it).
- **With your server** nothing changes: Spicy Lyrics, Spotify's own lyrics and Canvas, Vibe Bridge, your settings and error log kept there.
- **Without a server everything else runs on the phone**: your files, Spotify (it logs in straight back to the app: add `vibevisualizer://callback` to your Spotify developer app's Redirect URIs; the setup guide shows it with a copy button), other music apps, lyrics from LRCLIB, AMLL, Unison, LRCMux, lrc.red, KuGou, QQ Music, Kuwo and your own files, **NetEase now asked straight from the phone** (it used to go only through the server), Canvas from Spicy Lyrics' Canvas service, translation, the bars, the widget, picture-in-picture and Ambient mode. No "can't reach your server" errors.
- **Left out without a server**: Spicy Lyrics (its key may only be kept on a server, by its terms), Spotify's own lyrics (they need a Spotify cookie kept on the server) and Vibe Bridge (it relays through the server).

### Fixed

- **The heat warning on a phone on its side**: its buttons ended up below the screen. It's smaller there now and scrolls, and over the setup guide or Settings it's darker behind, so it reads well.

## From 0.31.0

#### On its side, laid out again

- **The cover as tall as the screen on the left**, and everything else on the right: where it plays (and what's wrong) at the top, the lyrics (or the bars, for the Visualizer or a song without lyrics), then the song, the seek bar, where the lyrics are from, and the controls in one row (they slide away as usual, giving the lyrics more room).
- **Canvas-friendly**: while a Canvas plays, the video itself takes the cover's place, whole and as tall as the screen (9:16, nothing cropped), instead of being stretched across most of the screen behind everything. The blurred Canvas stays behind it all.
