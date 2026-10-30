Vibe Visualizer for Android 0.27.0 (beta). Android 12 or newer; it installs over 0.26.0 (Settings → Help & about → Update now).

### The next song is ready before it starts

- **Lyrics, Canvas and cover are looked up ahead.** A few seconds into a song, the app finds the next one in the queue (your Spotify queue, the music app's queue when the app shares it, or your own files' list) and looks up its lyrics, Canvas and cover, so they show the moment it starts. With the Spotify app on this phone they even start before Spotify's Web API names the new song. It costs one more request to Spotify a song; turn it off in *Settings → Lyrics → Get the next song ready*.
- **Canvas videos are kept on the phone** (200 MB at most, the longest unplayed go first), so a song you play again starts its Canvas at once, without downloading it again.
- **Lyrics come in sooner**: the first source gets 0.7 s alone (was 1 s) before the rest are asked, an answer that's already in shows after 1.5 s (was 2 s), and a source that hasn't answered in 20 seconds is given up on, so a slow one never holds a lookup up (from Spicy Lyrics Mobile 0.16.1).
- **Skipping through songs asks for nothing until you stop**: the lyrics, the Canvas, the "liked" check and the Spotify catch-up after a skip in the Spotify app wait until a song stays on, so quick skipping doesn't run into Spotify's or the lyrics sources' limits.

### The top line

- **Always on screen**, also when the controls slide away, each text in the middle of its side; a name too long for its side slides along like a long song name.
- **The blur behind it fits it**: just behind the top line, or down past an error while there's one under it. It's only there while a Canvas video plays (no blur work otherwise).

### More

- **A thin outline around the cover**, so it stands out from the background (*Settings → Player screen → Cover outline*).
- **"Provided by" no longer shows twice**: on the Lyrics screen the line at the bottom steps aside while the Refined credits under the last line are on screen, and picture-in-picture keeps just its own small line.

### From Spicy Lyrics Mobile 0.16

- **Word timing in LRC lyrics** (LRCLIB and others sometimes have it as `<00:12.34>` stamps): they now play word by word instead of line by line.
- **An LRC line with its translation under it** on the same time shows the translation as a translation, not as a second line.
- **TTML files** starting with a byte-order mark or spaces, or using `&nbsp;`, now load, and a word with no end time (or one before its start) keeps a length.
- **A player that reports playing at speed 0** no longer freezes the lyrics.

From 0.24.0: the app has its own repository.
