Vibe Visualizer for Android 0.30.0 (beta). Android 12 or newer; it installs over 0.29.0 (Settings → Help & about → Update now).

### On its side, laid out again

- **The song's name sits next to the cover**, so the cover can be as tall as the side allows instead of a small square above the name. With a Canvas and no cover, the name has the whole width.
- **The lyrics no longer disappear in a long intro or break.** 0.29.0 let them rest while the cover covers them upright, but on its side nothing covers them, so they vanished. They rest now only while the cover really is over them.
- **An error is one tidy line**: its title whole, then as much of the detail as fits, then its button (it was cut into pieces before). The top line and the error take less height.

### The visualizer hides when the music plays elsewhere (fixed)

- **It now goes by where Spotify says it plays**: a computer, a speaker or a TV, or another phone, hides the visualizer, also when Vibe Bridge runs on that computer. Only with the microphone picked (the music may play in the room) do the bars stay.

## From 0.29.0

#### On its side

- **One layout with and without lyrics**: the cover (or the Canvas), the song's name and the seek bar on the left; the lyrics on the right, or the visualizer when a song has none (with the "no lyrics" note above it). With a Canvas and no lyrics the cover stays in front of it, unless *Hide the cover while a Canvas plays* is on.
- **"PLAYING FROM SPOTIFY ON <DEVICE>" in one line** at the top of the left side.
- **Errors take one short line there** ("Couldn't load lyrics · … · Try again"), so the cover keeps its size.

#### Fewer false alarms

- **"Couldn't load lyrics" only when no source could answer.** One busy source (QQ Music's "the search is busy") while the others answered no longer reads as an error: it's "No lyrics for this song", saying which source didn't answer, and the song is looked up again next time.

#### Lighter

- **Paused and still, the app draws 30 frames a second** instead of the screen's 120 (after 2 seconds with nothing moving and no touch; a touch brings the full rate back at once).
- **The lyrics rest during long breaks** while the cover is up in their place: nothing is worked out or drawn for them until it goes.
