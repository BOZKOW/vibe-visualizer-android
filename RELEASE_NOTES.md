Vibe Visualizer for Android 0.24.1 (beta). Android 12 or newer; it installs over 0.24.0 (Settings → Help & about → Update now).

### Faster

- **The lyrics draw less**: each line's blur was handed over as a new effect every frame, so every blurred line was blurred again 60 or more times a second even when it hadn't changed. Now an unchanged line keeps its finished picture
- **The seek bar repaints alone**: while music plays its glide rebuilt the whole seek row (and its times) every frame; now only the bar is redrawn, and the times move on a few times a second as before

Nothing looks or works differently.

### More animation

- Settings' cards slide in one after another when a page opens, rows squish a little when you press them, and search results glide into place as you type
- The "Playing from …" line and the one under it cross-fade when they change, and a new song's name rises in at the top of the Lyrics screen, as it does on Now Playing

All of them follow *Settings → Performance → Animations* (and Android's Remove animations).

From 0.24.0: the app has its own repository.
