Vibe Visualizer for Android 0.30.1 (beta). Android 12 or newer; it installs over 0.30.0 (Settings → Help & about → Update now).

### Fixed

- **A crash while a Canvas played** (found by the new crash detection): the blur behind the top line was worked out before the video had a size, and that could close the app.
- **Several errors keep taking turns for as long as there's more than one**; a change in the list no longer starts them over, and a new one still shows first.
- **"You have the newest version"**: *Check for updates* now says so when there's nothing newer (and Help & about shows it too).

## From 0.30.0

#### On its side, laid out again

- **The song's name sits next to the cover**, so the cover can be as tall as the side allows instead of a small square above the name. With a Canvas and no cover, the name has the whole width.
- **The lyrics no longer disappear in a long intro or break.** 0.29.0 let them rest while the cover covers them upright, but on its side nothing covers them, so they vanished. They rest now only while the cover really is over them.
- **An error is one tidy line**: its title whole, then as much of the detail as fits, then its button (it was cut into pieces before). The top line and the error take less height.

#### The visualizer hides when the music plays elsewhere (fixed)

- **It now goes by where Spotify says it plays**: a computer, a speaker or a TV, or another phone, hides the visualizer, also when Vibe Bridge runs on that computer. Only with the microphone picked (the music may play in the room) do the bars stay.
