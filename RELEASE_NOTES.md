Vibe Visualizer for Android 0.25.0 (beta). Android 12 or newer; it installs over 0.24.1 (Settings → Help & about → Update now).

### One layout for every screen

Now Playing, Lyrics and the Visualizer are laid out the same way when the phone is upright:

- **At the top**: only where the music plays from and on what device ("PLAYING FROM SPOTIFY · on Galaxy S25"). When something is wrong (no internet, the server can't be reached, Spotify isn't answering or has paused requests), that space shows it instead, in red or yellow, with the one button that fixes it. It stays there even while the controls are hidden, and goes back to the device once it's solved.
- **At the bottom**: the song's name, the seek bar and where the lyrics are from ("Provided by …", with who made and uploaded community lyrics, and the songwriters when the source names them). These stay on screen when the controls slide away.
- The translate and romanize buttons of the Lyrics screen sit at the right of the top bar.

The phone on its side keeps its own layout.

### Your own files run smoothly

- **The lyrics and the visualizer ran at about 4 frames a second** while your own files played: the player reports where it is only a few times a second, and the lyrics followed those jumps. Now the time between them is filled in, so they move every frame again.

### More colour

- **The background is far more vivid**: the colours from the cover (and the blurred Canvas) are turned up a lot, and a little brighter. *Settings → Look → Saturation* and *Brightness* still tune them from there.

From 0.24.0: the app has its own repository.
