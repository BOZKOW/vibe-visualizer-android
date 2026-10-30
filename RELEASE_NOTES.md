Vibe Visualizer for Android 0.28.0 (beta). Android 12 or newer; it installs over 0.27.1 (Settings → Help & about → Update now).

### The visualizer steps aside when the music plays elsewhere

- **When Spotify plays on another device** (a computer, a speaker, another phone), or a music app casts to one, the bars fade away: they only hear what this phone plays, so they'd just sit there. The bars under the cover or the lyrics take their room with them; on the Visualizer screen a short note says where it plays, with a button to what the bars hear. With the microphone, or Vibe Bridge on that computer, the bars can hear it, so they stay.

### Crash detection

- **If the app ever closes because of a crash**, what went wrong is written down on the phone, and at the next start "Vibe Visualizer closed unexpectedly last time" shows at the top with a Copy details button. It also goes to the error log on your server, like the app's other errors.

### The top line

- **"PLAYING ON …" looks like "PLAYING FROM …"**: the device is now in the same small capitals, at the other side of the camera.
- **On its side**, "Playing from", the device and the error space sit at the top of the cover / Canvas side, so they never push the lyrics down.
- **More room beside the camera on its side**: everything keeps 16 dp from the camera hole (was 8 dp).

## From 0.27.0

#### The next song is ready before it starts

- **Lyrics, Canvas and cover are looked up ahead.** A few seconds into a song, the app finds the next one in the queue (your Spotify queue, the music app's queue when the app shares it, or your own files' list) and looks up its lyrics, Canvas and cover, so they show the moment it starts. With the Spotify app on this phone they even start before Spotify's Web API names the new song. It costs one more request to Spotify a song; turn it off in *Settings → Lyrics → Get the next song ready*.
- **Canvas videos are kept on the phone** (200 MB at most, the longest unplayed go first), so a song you play again starts its Canvas at once, without downloading it again.
- **Lyrics come in sooner**: the first source gets 0.7 s alone (was 1 s) before the rest are asked, an answer that's already in shows after 1.5 s (was 2 s), and a source that hasn't answered in 20 seconds is given up on, so a slow one never holds a lookup up (from Spicy Lyrics Mobile 0.16.1).
- **Skipping through songs asks for nothing until you stop**: the lyrics, the Canvas, the "liked" check and the Spotify catch-up after a skip in the Spotify app wait until a song stays on, so quick skipping doesn't run into Spotify's or the lyrics sources' limits.

#### The top line

- **Always on screen**, also when the controls slide away, each text in the middle of its side; a name too long for its side slides along like a long song name.
- **The blur behind it fits it**: just behind the top line, or down past an error while there's one under it. It's only there while a Canvas video plays (no blur work otherwise).

#### More

- **A thin outline around the cover**, so it stands out from the background (*Settings → Player screen → Cover outline*).
- **"Provided by" no longer shows twice**: on the Lyrics screen the line at the bottom steps aside while the Refined credits under the last line are on screen, and picture-in-picture keeps just its own small line.

#### From Spicy Lyrics Mobile 0.16

- **Word timing in LRC lyrics** (LRCLIB and others sometimes have it as `<00:12.34>` stamps): they now play word by word instead of line by line.
- **An LRC line with its translation under it** on the same time shows the translation as a translation, not as a second line.
- **TTML files** starting with a byte-order mark or spaces, or using `&nbsp;`, now load, and a word with no end time (or one before its start) keeps a length.
- **A player that reports playing at speed 0** no longer freezes the lyrics.

From 0.24.0: the app has its own repository.
