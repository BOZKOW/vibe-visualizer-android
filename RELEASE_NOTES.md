Vibe Visualizer for Android 0.44.0 (beta). Android 12 or newer; it installs over 0.43.1 (Settings → Help & about → Update now).

### Smoother with a Canvas

- **Much less stutter while a Canvas plays**: each lyric line's blur was worked out twice a frame, and the Canvas's blurred copy again for every frosted button, for every frame of the video. Each is now worked out once. On the test emulator, janky frames went from about 41% to 10% upright and 13% to 5% on its side with a slow video, and from about 44% to 21% and 34% to 22% with a fast one.
- **The video can no longer change the screen's refresh rate**, which would have slowed the whole app to the video's pace.

### Song changes

- **No more default colours in between**: the cover and background used to go from the old song to the default colours and only then to the new song, whenever the new cover took a moment. The old ones now stay until the new one is ready, and the default colours only show for a song that really has no cover.
- **The background is made off the main thread**, so a song change holds up less.

### Spotify's DJ

- **The cover and its colours stay while the DJ talks**, instead of going to the default colours and back each time.
- **The lyrics no longer get stuck on loading** when the DJ's name for what's playing changes while it talks.
- **A moment with no song named no longer reloads everything**, and something Spotify sends with no name isn't treated as a song any more.
- **Far fewer requests to Spotify while the DJ talks**: they could run into its limits.

### Glass bars

- **New option: Settings → Visualizer → Glass bars.** The bars take the glass look of the seek bar and the buttons, tinted with the song's colours: see-through upright, frosted on its side.
