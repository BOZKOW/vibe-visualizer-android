Vibe Visualizer for Android 0.34.0 (beta). Android 12 or newer; it installs over 0.33.3 (Settings → Help & about → Update now).

### Settings that need another one are greyed out

- **A setting that only works together with another one is greyed out and can't be changed until it can**, with a short line saying why: the Ambient mode options until Ambient mode is on, the Canvas options until Canvas is on (and the Canvas blur strength until a blur is picked, the smaller cover while the cover is hidden), "Hide them after" when the controls are always visible, the motion speed outside the Dynamic background, glow, motion blur and the frame rate limit in Low performance mode, the early scroll time, the credits' place, Refreshed simple lyrics mode and the picture-in-picture size until what they belong to is on, Vibe Bridge without a server and the Like button without Spotify.

### More animation

- The translate and romanize buttons glide between on and off with a little pop, the Ambient mode clock rolls each new minute in, the dots under several notices stretch toward the one showing, a new choice in Settings slides in, release notes open and close smoothly, and greyed settings fade.

### Also

- **Notices stay much longer** at the top (15 seconds, 20 for the longer ones), so there's time to read them while they take turns.
- **Release notes show their bold as bold** in Help & about (they showed the asterisks).
- **The update download no longer fills the app's web cache**: the 19 MB download went through the 30 MB cache meant for small answers (the lyrics sources' and GitHub's), pushing them out.

## From 0.33.1–0.33.3

#### Fixed

- **Update downloads timing out**: the update used the same 35-second limit as the quick lyrics and Spotify lookups, so on a slower connection the 19 MB download was cut off part way. It now has no limit on how long it takes; it only gives up when nothing arrives for a whole minute.
- **Translation failing with "HTTP 500"**: Google's free translation answers with a server error for some songs (a longer stretch of lyrics in Latin letters, translated into Czech and a few other languages, when its romanization is asked for too). The app now asks again in another way when that happens, and the translation comes through.
- **Ambient mode's clock and date follow a change of the phone's language** while Ambient mode is on (they kept the old one until it was opened again).
- **Numbers on screen** (the lyrics timing messages, wait times, the motion speed in Settings) are written the way your phone's language writes them.
- **The widget's smallest text** is a little bigger (11 sp), Android's smallest readable size.
- Behind the scenes: old code for Android versions this app doesn't run on is gone, and an unused icon removed. Nothing else looks or works differently.

## From 0.33.0

#### The Canvas on its side, as on the desktop

- **The video fills the left half of the screen**, fading softly into the background toward the middle (darker at the top and the foot), with the cover smaller over it and the song's name centred under it (with *Hide the cover while a Canvas plays*, the name sits at the foot of the video). The lyrics, the seek bar and the controls are on the right. Without a Canvas it stays the big cover on the left.

#### Notices: words only

- **Every message now shows in the space at the top**, also the ones that used to pop up at the bottom ("Copied", "Lyrics file added", update news).
- **Only hard errors have a button** (no internet, the server or Spotify not answering, lyrics that couldn't load, a hot phone). Notices (good news in white, waits and warnings in yellow, a warm phone) are words alone, with nothing to press, and go by themselves. The space can't be tapped any more.
