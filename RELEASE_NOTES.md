Vibe Visualizer for Android 0.45.4 (beta). Android 12 or newer; it installs over 0.45.3 (Settings → Help & about → Update now).

### Notifications

- **Swipe between notifications**: when there are two or more at the top, swipe the pill left or right to go through them. After a swipe they stop changing by themselves until a new one arrives. Tapping one still opens its details.
- **A notification no longer moves the lyrics**: on the lyrics screen it now floats over them, so the lyrics and controls stay exactly where they are. The big cover screen still makes room for it, as before.
- In Low performance mode the floating notification has a dark backing, so its words stay readable over the lyrics.

### Screen refresh rate

- **Should stop dropping to 1 Hz in lyrics**: while lyrics are showing, the app now asks Android not to slow the screen down to save power. Other screens can still save power, and the Ambient clock still runs at its low rate. The emulator only has a 60 Hz screen, so this can't be confirmed until it's tried on a real phone. If it still happens, the "screen" line in Settings → Help & about → Copy debug info shows the rate.
