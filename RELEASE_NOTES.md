Vibe Visualizer for Android 0.31.0 (beta). Android 12 or newer; it installs over 0.30.2 (Settings → Help & about → Update now).

### On its side, laid out again

- **The cover as tall as the screen on the left**, and everything else on the right: where it plays (and what's wrong) at the top, the lyrics (or the bars, for the Visualizer or a song without lyrics), then the song, the seek bar, where the lyrics are from, and the controls in one row (they slide away as usual, giving the lyrics more room).
- **Canvas-friendly**: while a Canvas plays, the video itself takes the cover's place, whole and as tall as the screen (9:16, nothing cropped), instead of being stretched across most of the screen behind everything. The blurred Canvas stays behind it all.

## From 0.30.2

Checked on other screen shapes this time (a small phone, the largest display and font sizes, a tablet both ways, a camera hole in a corner), which found these:

#### Fixed

- **A camera hole in a corner no longer pushes everything to the side**: on phones with the hole near a top corner, the player and Settings were shifted right by about 64 dp when held upright. The room Android keeps at the top already clears such a hole.
- **The top line with a corner camera**: both texts now share the side with room (or "Playing from" takes all of it when there's no device to name), instead of one squeezed next to the hole.
- **The round buttons always fit**: at the largest display size (or on a narrow phone) the five round buttons touched each other; they now size themselves to fit with room between.
- **Tablets and open foldables held upright** get the phone's layout (a big cover, the song, the controls), no wider than 600 dp and centred, instead of two narrow columns. Held wide, they keep the side-by-side layout.


#### Fixed

- **A crash while a Canvas played** (found by the new crash detection): the blur behind the top line was worked out before the video had a size, and that could close the app.
- **Several errors keep taking turns for as long as there's more than one**; a change in the list no longer starts them over, and a new one still shows first.
- **"You have the newest version"**: *Check for updates* now says so when there's nothing newer (and Help & about shows it too).
