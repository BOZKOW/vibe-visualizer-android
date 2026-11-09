Vibe Visualizer for Android 0.49.2 (beta). Android 12 or newer; it installs over 0.49.1 (Settings → Help & about → Update now).

- **Fixed a crash.** When a lookup on the internet answered at the very moment the app gave up waiting for it (a slow connection, or a song change), the app closed that answer on its main thread, and Android stops an app for network work there. Answers are now always read and closed in the background.
