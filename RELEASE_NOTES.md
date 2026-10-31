Vibe Visualizer for Android 0.38.0 (beta). Android 12 or newer; it installs over 0.37.1 (Settings → Help & about → Update now).

### One screen

- **The cover, the lyrics and the bars are on one screen**: no separate Now Playing (cover only), Lyrics and Visualizer screens any more. The lyrics fill it, the bars move under them, and the cover sits small beside the song's name; a song without lyrics shows the big cover, as before. The bars can be turned off (Settings → Visualizer → The bars, or the bars button).
- **The buttons under the controls**: the Lyrics and Visualizer buttons are gone (a button you had set to them turns the bars on and off, or opens Lyrics timing); the default row is where it plays, the bars, Lyrics timing and Resync, then Settings. The home screen shortcuts to Lyrics and the Visualizer are gone too.

### On its side

- **The cover, the song and the seek bar on the left, the lyrics with the whole height on the right**, so duets and background vocals have room; with a Canvas, the song's name and the seek bar are over the Canvas too, and the credits stay with the lyrics. With the bars on, they're under the seek bar.
- **The controls in two rows, each its full size**: side by side in one row they were too small to hit. They hide after a few seconds and the lyrics get the room back.

### Lyrics

- **Two lines sung at the same time stay in view together** (a duet, or lines whose times overlap, as some Spicy Lyrics syncs have): the pair sits around the middle, and when they're too tall for that, the newer one stays whole with as much of the earlier one as fits.

## From 0.37.1

#### Fixed

- **No OK under an error in its place** (0.37.0 put one there): at the top, notices and errors alike are words alone; OK is on the screen tapping one opens.

## From 0.37.0

#### Notices you can open

- **Tap a notice at the top for its own screen**, as the heat warning has: what's wrong, what to do about it, step by step (no internet, the server or Spotify not answering, Spotify asking to wait, a warm phone, lyrics that couldn't load, an update downloading…), the button that fixes it when there is one, and OK. Tapping it doesn't bring the controls back.
- **Notices no longer go by themselves**: they stay until you put them away with OK on their screen. In their place they're words alone, errors too; what fixed an error before is on its screen. Something that's still wrong after OK (no internet, a warm phone) comes back the next time it happens.
- **The words sit in the middle of the glass**, at the top of the player and over Settings.

#### Also

- **Loading lyrics looks the same everywhere**: under the big cover it showed "Looking for lyrics…" as a plain line; now it's the same as where the lyrics go, a small "Loading lyrics" and a faint line the light runs along (or the same spinner, with the loading skeleton off).
- **"Playing from" and "Playing on" are in the middle of their sides** again.

## From 0.36.3

#### Fixed

- **Updates found the wrong newest version**: the app took the release published last for the newest, and 0.36.1's finished building after 0.36.2's, so 0.36.2 was never offered. The newest is now the highest version, whenever it was published.

## From 0.36.2

#### Changed

- **No outline on the translate and romanize buttons**: off, they're just the frosted fill; on, filled white.
- **No circle behind shuffle and repeat**: on, they're in the cover's colour with the dot under them.

## From 0.36.1

#### Changed

- **Lyrics timing is right in Settings** (Settings → Lyrics timing): all songs, this output, this music app and this song, and Resync, on the page itself instead of behind a button that opened a separate window. The timing button under the controls still opens it as a quick pop-up.
- **More frosted glass in the Glass design**: the pop-up sheets (Lyrics timing, where it plays, what the bars hear…) and the questions the app asks, the sliders' tracks, and the options to pick from in the sheets and the setup guide (the picked one tinted with the cover's colour).

## From 0.36.0

From Spicy Lyrics Mobile 0.17.0:

#### New

- **A delay for each music app** (Lyrics timing): for a player that runs early or late on every song, kept for that app (Spotify, YouTube Music, Poweramp…, or this app's own player) and added to yours, the output's and the song's.
- **Canvas behind the lyrics** (Settings → Canvas): how much of the video shows behind the lyrics, dimmed so they stay readable, from none (only its soft glow) to 80 % (it was always 25 %).
- **Current line at the top** for the small window (Settings → Small window & widget), so the room above it goes to the lines coming next.
- **Poweramp can open Vibe Visualizer** as its 3rd party lyrics app (its lyrics button), straight on the Lyrics screen.
- **The release notes mark the version you have** (Help & about), open, with "Yours" beside it.

#### Fixed

- **The widget's lyrics too small or squeezed on Samsung phones** with the display size (Screen zoom) changed from the default: One UI's home screen gives the widget's size for the default display size, which is now corrected for.

## From 0.35.4

#### Frosted glass everywhere

- **In the Glass design, everything is the same frosted glass as the seek bar's track and the notices**: clear white over what's under it with a light edge, never blurred live (what's under it is blurred already). The cards in Settings, the search field, every button (the main ones tinted with the cover's colour, so they're still the ones to press), the switches while they're off (on, they keep the cover's colour, so on and off are told apart at a glance) and the menus' edges.
- **The round buttons on the player** (under the controls, and translate and romanize) are frosted glass while they're off, in both designs; on, they're filled white as before.

## From 0.35.3

#### Notices on frosted glass

- **What's wrong (or worth knowing) under the top line now sits on frosted glass**, the same soft white pill with a light edge as the seek bar's track, growing and shrinking to each notice. It isn't blurred live: what's under it is the blurred background already.

## From 0.35.2

#### The line at the top, tidied

- **Where it plays from and where it plays now fit either side of the camera**: "PLAYING FROM SPOTIFY" and "PLAYING ON …" were too long for the room beside the camera, so both crawled along and mostly showed their first words ("PLAYING FROM SPO", "PLAYING ON LIVING"). Now it's an icon and the name: Spotify's logo and SPOTIFY (or the music app, or THIS PHONE) at the left, and the device's icon (a speaker, a phone, a computer, a TV) with its name at the right. Only a very long device name still scrolls.
- **They sit at the edges**, as a status bar's clock and icons do, in line with the rest of the screen (each floated in the middle of its side). On its side the app keeps the one line in words: "PLAYING FROM SPOTIFY ON …".

## From 0.35.1

#### Fixed

- **Haptic feedback works**: the tick it asked Android for plays nothing on some phones (Samsung's), so the setting seemed to do nothing. It's now a click played on the vibrator itself, as strong as your phone's touch vibration (and still there when that's off, since you asked for it here). Turning the setting on ticks at once, and each tap ticks once (the play button and the screen buttons ticked twice).
- **Buttons that are on are easy to tell**: a button under the controls that's on (the screen you're on, Translate, Romanize, Canvas, Like) is filled white with a dark icon, as the translate and romanize buttons by the song; shuffle and repeat sit in a soft circle of their colour, besides the dot.

#### Next shows the next song at once

- **Pressing Next in the app shows the next song straight away**: its name, cover, lyrics and Canvas, got ready while the song before played, without waiting for Spotify to say a new song started (its time counts from the press until Spotify does). Pressing Next again before Spotify answers goes on to the one after. If Spotify still plays the old song 8 seconds later, the app goes back to it.

## From 0.35.0

#### The lyrics no longer stutter while a Canvas plays

- **The blurred Canvas behind everything is drawn by the graphics chip itself.** Until now, 12 times a second a small frame of the video was copied back from it to make the blur, and every copy held up the next frame of the lyrics, the more so the faster the video moved.

#### Spotify's crossfade, gapless playback, Automix and the DJ

- **The lyrics stay in step across song changes** when songs flow into each other. Spotify can say where the new song is a few seconds before it names it, or not say it at all, and the lyrics either started at 0:00 and waited, or jumped to the old song's end, until you pressed Resync. Now the app keeps the new song's early report, and when Spotify says nothing, it checks the position with Spotify's Web API and moves the lyrics if they're more than 1.5 s off.
- **An ad or the DJ talking between songs** no longer shows "Nothing playing" for a moment.

#### Pick the buttons under the controls

- **Settings → Player screen → Buttons under the controls**: four buttons of your choice: Lyrics, where it plays, what the bars hear, Visualizer, Resync, Lyrics timing, Translate, Romanize, Canvas on or off, Like, or nothing. The defaults are as before, and Settings always stays the last one (it's the way into them).
- **Shuffle and repeat have a small dot under them** while they're on.

#### Skipping songs on Spotify

- **The next five songs in your Spotify queue are got ready** (their lyrics, Canvas and cover), not just the next one, still with one request to Spotify a song. Skipping a few songs shows their lyrics at once, without the short wait for songs skipped through quickly.

#### Also

- **Translate and romanize leave hidden controls hidden**: tapped while the controls are away, they only switch, like the seek bar.
- **The credits no longer show twice** at the end of a song (under the lyrics and at the foot) while a Canvas plays with the cover hidden, or on its side.
- **Focus mode's settings**: Cover in long breaks now goes with Focus mode (greyed without it), and what Focus mode does its own way is greyed while it's on: Blur inactive lines, Minimal lyrics mode, Early scroll and Keep overlapping lines. Background vocal pop-out is greyed until Simple and Refreshed simple lyrics mode are on.
- **Settings tidied**: Ambient mode has its own page, and the lyrics experiments sit in Lyrics beside what they go with (Focus mode with the cover in long breaks, Refined credits with the credits, the simple modes' and Minimal mode's extras under them).
- **Notices over Settings are frosted glass in the middle**, their words centred, clear of the close button.
- **The heat warning starts when the battery reaches 40 °C** (45 °C: hot), and when Android itself says the phone is warm it says so ("Android says it's warm inside", "Android expects it to slow itself down within seconds"), with the battery's temperature.

## From 0.34.0

#### Settings that need another one are greyed out

- **A setting that only works together with another one is greyed out and can't be changed until it can**, with a short line saying why: the Ambient mode options until Ambient mode is on, the Canvas options until Canvas is on (and the Canvas blur strength until a blur is picked, the smaller cover while the cover is hidden), "Hide them after" when the controls are always visible, the motion speed outside the Dynamic background, glow, motion blur and the frame rate limit in Low performance mode, the early scroll time, the credits' place, Refreshed simple lyrics mode and the picture-in-picture size until what they belong to is on, Vibe Bridge without a server and the Like button without Spotify.

#### More animation

- The translate and romanize buttons glide between on and off with a little pop, the Ambient mode clock rolls each new minute in, the dots under several notices stretch toward the one showing, a new choice in Settings slides in, release notes open and close smoothly, and greyed settings fade.

#### Also

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
