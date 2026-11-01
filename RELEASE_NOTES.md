Vibe Visualizer for Android 0.39.2 (beta). Android 12 or newer; it installs over 0.39.1 (Settings → Help & about → Update now).

### Fixed

- **The dots start when a line ends, for every lyrics source and style**: they only came for a pause of over 5 seconds, so at a pause of 3 to 5 seconds nothing started, and with the Focus style the finished line stayed fully lit until the next one. They now come from 3 seconds, as in Spicy Lyrics.
- **A pause only starts once nobody's singing**: with overlapping lines or background vocals still going, the dots could come in over them.
- **"No internet" while the phone is online**: when the app was started in the background (by the song following or the widget), it could read as offline, and with errors now staying until solved the message never went. It only shows when there's really no connection.
