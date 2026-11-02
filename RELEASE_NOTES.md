Vibe Visualizer for Android 0.43.0 (beta). Android 12 or newer; it installs over 0.42.1 (Settings → Help & about → Update now).

### Colours from the Canvas

- **The visualizer and the app's accent colour now come from the Canvas while one plays**, not only from the cover (with the Cover colors theme). The app takes a look at the video a few times in its first seconds; when there's no Canvas, or it's turned off, the colours go back to the cover's.

### Faster Settings

- **Settings scroll more smoothly with a song playing**: while they're open, the player behind them no longer runs its own blurs (the video's, each lyric line's and the lyrics' ends), which can't be seen under Settings' blur anyway, and it moves on at half the rate. They come back as Settings close. On the test emulator with a Canvas playing, janky frames went from about 35% to about 7% upright, and from 24% to 3% on its side.

### Softer blur

- **The blur now melts into the Canvas and the lyrics**: where it's behind the top line, what's wrong and the controls, its edges are much longer and eased, so they no longer show as a band. The Canvas's part applies with Blur chosen under Settings → Canvas → Behind the controls.
