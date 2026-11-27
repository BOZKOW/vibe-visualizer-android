Vibe Visualizer for Android 0.63.2 (beta). Android 12 or newer; it installs over 0.63.1.

- **Catalog BPM for the dancer:** replaced live-audio tempo estimation with ReccoBeats lookups. Exact Spotify ID or ISRC first, otherwise verified title, artist, recording version and duration. No API key or setup.
- **No guessed speeds:** unmatched or ambiguous recordings stay still. Removed the background audio-tempo analysis used only by this experiment.
- **Lightweight lookups:** cached results for seven days, paced requests and rate-limit backoff. Only track identifiers or names are sent; audio and local files are never uploaded.
- **Landscape lyrics:** restored their original right margin; the dancer no longer narrows or shifts the lyrics column.
