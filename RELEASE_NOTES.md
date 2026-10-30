Vibe Visualizer for Android 0.29.0 (beta). Android 12 or newer; it installs over 0.28.1 (Settings → Help & about → Update now).

### On its side

- **One layout with and without lyrics**: the cover (or the Canvas), the song's name and the seek bar on the left; the lyrics on the right, or the visualizer when a song has none (with the "no lyrics" note above it). With a Canvas and no lyrics the cover stays in front of it, unless *Hide the cover while a Canvas plays* is on.
- **"PLAYING FROM SPOTIFY ON PETR'S S25" in one line** at the top of the left side.
- **Errors take one short line there** ("Couldn't load lyrics · … · Try again"), so the cover keeps its size.

### Fewer false alarms

- **"Couldn't load lyrics" only when no source could answer.** One busy source (QQ Music's "the search is busy") while the others answered no longer reads as an error: it's "No lyrics for this song", saying which source didn't answer, and the song is looked up again next time.

### Lighter

- **Paused and still, the app draws 30 frames a second** instead of the screen's 120 (after 2 seconds with nothing moving and no touch; a touch brings the full rate back at once).
- **The lyrics rest during long breaks** while the cover is up in their place: nothing is worked out or drawn for them until it goes.

## From 0.28.1 and 0.28.0

#### Errors take turns

- **Several errors or notices at once now take turns** in the space under the top line, one at a time (4.5 seconds each) instead of only the most important one showing. A new one shows first, a row of dots says how many there are, and a tap on the space moves on to the next.



#### The visualizer steps aside when the music plays elsewhere

- **When Spotify plays on another device** (a computer, a speaker, another phone), or a music app casts to one, the bars fade away: they only hear what this phone plays, so they'd just sit there. The bars under the cover or the lyrics take their room with them; on the Visualizer screen a short note says where it plays, with a button to what the bars hear. With the microphone, or Vibe Bridge on that computer, the bars can hear it, so they stay.

#### Crash detection

- **If the app ever closes because of a crash**, what went wrong is written down on the phone, and at the next start "Vibe Visualizer closed unexpectedly last time" shows at the top with a Copy details button. It also goes to the error log on your server, like the app's other errors.

#### The top line

- **"PLAYING ON …" looks like "PLAYING FROM …"**: the device is now in the same small capitals, at the other side of the camera.
- **On its side**, "Playing from", the device and the error space sit at the top of the cover / Canvas side, so they never push the lyrics down.
- **More room beside the camera on its side**: everything keeps 16 dp from the camera hole (was 8 dp).
