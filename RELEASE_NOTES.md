Vibe Visualizer for Android 0.41.6 (beta). Android 12 or newer; it installs over 0.41.5 (Settings → Help & about → Update now).

### Scrolling between lines

- **In the small window and on its side, the scroll follows the gap between the lines**: it starts when a line's last word ends and arrives as the next line's first word starts. A short gap makes a quick scroll, a longer one a slower scroll. Lines with no gap between them, or that overlap, get the quickest.
- This pacing is for the Spicy Lyrics and Simple styles, when the lyrics know where their lines end. Focus keeps its own way of bringing lines in, and LRC lyrics and a break's dots keep their timing.

### Smoother scrolling

- **There, the scroll now glides**: it starts softly and keeps its speed when the next scroll begins before the last one is done, where it used to start over. This needs *Smooth scrolling* on, as it is by default.
- **A line too big for its space moves steadily with the singing**, in any layout, where it used to jump a row at a time.
- **All the lines move together**: in any layout, they no longer shift against each other by a pixel as they scroll.
