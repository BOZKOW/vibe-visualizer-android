Vibe Visualizer for Android 0.45.0 (beta). Android 12 or newer; it installs over 0.44.0 (Settings → Help & about → Update now).

### The Canvas on its side, as Spicy Lyrics does it

- **The video now takes the cover's place**, on a stage laid out with Spicy Lyrics' own measurements: sharp up to a set point, then one long, eased soft edge toward the lyrics, which run over it. It's translucent, so the blurred background carries through and the two read as one surface, and it fades in over the top. No cover is drawn over it on its side, whatever the setting.
- **The song, the seek bar and the controls are docked at its foot**, like a video player's, with the name as a caption.
- **A dim veil over the video** that deepens while the controls show, so they stay readable.
- **The picture is cropped for faces**: 40% from the top instead of the middle, which often cut them off.
- **The blurred background** follows Spicy Lyrics' colours, without the old dark edges.
- It's lighter too: the video isn't blurred in place any more, and janky frames with a fast video went from 22% to 13% on the test emulator.

### Frosted upright too

- **The seek bar, the buttons, the notifications and the Glass bars are frosted upright as well**, now that the lyrics run behind them. It costs nothing measurable: they show part of a picture that's already blurred.

### Ambient mode and variable refresh rate

- **Ambient mode now lets a variable-rate (LTPO) screen slow down**: with only the clock showing, the app draws nothing between its changes and asks for 10 Hz, so the screen can idle as low as it goes; with the lyrics or the bars moving it asks for 30 Hz. Any touch brings the full rate back. Debug info now shows the screen's current rate.

### Smoother lyrics behind the blur

- **The lyrics behind the blur no longer move in jumps**: their blurred copy moved 8 pixels at a time as they scrolled. It now moves by single pixels, for no extra work.
