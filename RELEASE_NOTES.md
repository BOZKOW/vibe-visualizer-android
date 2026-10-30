Vibe Visualizer for Android 0.33.0 (beta). Android 12 or newer; it installs over 0.32.0 (Settings → Help & about → Update now).

### The Canvas on its side, as on the desktop

- **The video fills the left half of the screen**, fading softly into the background toward the middle (darker at the top and the foot), with the cover smaller over it and the song's name centred under it (with *Hide the cover while a Canvas plays*, the name sits at the foot of the video). The lyrics, the seek bar and the controls are on the right. Without a Canvas it stays the big cover on the left.

### Notices: words only

- **Every message now shows in the space at the top**, also the ones that used to pop up at the bottom ("Copied", "Lyrics file added", update news).
- **Only hard errors have a button** (no internet, the server or Spotify not answering, lyrics that couldn't load, a hot phone). Notices (good news in white, waits and warnings in yellow, a warm phone) are words alone, with nothing to press, and go by themselves. The space can't be tapped any more.

## From 0.32.0

#### With a server, or without one

- **The setup guide now asks**: *With my server* or *Without a server*. You can switch any time in *Settings → Server & connection* (your server's address is kept, just not used, while you run without it).
- **With your server** nothing changes: Spicy Lyrics, Spotify's own lyrics and Canvas, Vibe Bridge, your settings and error log kept there.
- **Without a server everything else runs on the phone**: your files, Spotify (it logs in straight back to the app: add `vibevisualizer://callback` to your Spotify developer app's Redirect URIs; the setup guide shows it with a copy button), other music apps, lyrics from LRCLIB, AMLL, Unison, LRCMux, lrc.red, KuGou, QQ Music, Kuwo and your own files, **NetEase now asked straight from the phone** (it used to go only through the server), Canvas from Spicy Lyrics' Canvas service, translation, the bars, the widget, picture-in-picture and Ambient mode. No "can't reach your server" errors.
- **Left out without a server**: Spicy Lyrics (its key may only be kept on a server, by its terms), Spotify's own lyrics (they need a Spotify cookie kept on the server) and Vibe Bridge (it relays through the server).

#### Fixed

- **The heat warning on a phone on its side**: its buttons ended up below the screen. It's smaller there now and scrolls, and over the setup guide or Settings it's darker behind, so it reads well.
