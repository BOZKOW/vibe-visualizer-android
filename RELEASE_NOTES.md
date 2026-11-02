Vibe Visualizer for Android 0.41.4 (beta). Android 12 or newer; it installs over 0.41.3 (Settings → Help & about → Update now).

### Smoother

- **The desync check no longer holds up the lyrics**: while it listens, every 5 seconds it worked out on the app's main thread whether the singing lines up, which could cause a small stutter each time. That now happens in the background; it answers the same way.
- **A little less work all the time**: the check of Spicy Lyrics' status no longer reads your lyrics sources 4 times a second, and laying out lyrics no longer rebuilds the same text patterns for every line.
