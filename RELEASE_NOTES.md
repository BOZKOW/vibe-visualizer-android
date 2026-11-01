Vibe Visualizer for Android 0.40.0 (beta). Android 12 or newer; it installs over 0.39.2 (Settings → Help & about → Update now).

From Spicy Lyrics Mobile 0.18.0:

### New

- **Your own background color** (Settings → Look → Background → Color): the cover's color as before, a dark preset, or any hex code. Pure black for OLED screens is one tap.
- **Keep syllable pace** (Settings → Lyrics, with Join split words on): a joined held word still reads as one word, but each syllable's letters fill at that syllable's own speed.
- **The release year beside the artist**, wherever the artist shows, when the year is known. Settings → Player screen → Release year turns it off.

### Changed

- **Clear stored lyrics** also clears translations and what's held in memory, and looks up the song that's playing again.
- **Poweramp no longer lists Vibe Visualizer as its lyrics app**: that link only works for apps that also supply Poweramp's lyrics, which this one doesn't. The app still follows Poweramp as before.

### Fixed

- **Translations that come with the lyrics are used when they don't say their language** (as the Spicy Lyrics API's don't): the phone tells which language they're in, on the phone itself. They were taken for English, so with another language they were never used and got a machine translation instead.
- **Offline, a failed translation no longer adds its own notice** beside "No internet".
