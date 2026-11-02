Vibe Visualizer for Android 0.42.1 (beta). Android 12 or newer; it installs over 0.42.0 (Settings → Help & about → Update now).

### Lyrics

- **Split words stay on one row**: a word sung in syllables, which is common in Spicy Lyrics' files, could break in the middle when its parts didn't line up well enough to be joined. It now moves to the next row whole. Rows can still end after a dash, and between letters in scripts written without spaces.
- **The scroll starts later on its side**, and for lines too long for the screen: it waits until the last word has been sung, and starts at most .8 s before the next first word. It used to set off while that word was still being sung.
- **No more sharp edges on the blur on its side**: the blurred lyrics were cut off in a straight line at both sides of their column. They now fade out by themselves.
