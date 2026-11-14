Vibe Visualizer for Android 0.52.0 (beta). Android 12 or newer; it installs over 0.51.2 (Settings → Help & about → Update now).

The big Spicy Lyrics 6.5.0 update, brought over:

- **Better romanization.** Japanese is read a whole line at a time, so a word sung over several syllables keeps its reading (一|緒 is "is|sho"), long vowels are written as they are (kyou, koohii), particles as they're said (wa, e, wo), and the romaji has gaps between words even where the lyrics run them together ("Kimi no namae wo yonde ita"). Korean is read by sound (사랑해 → saranghae, 같이 → gachi), Chinese pinyin is spaced character by character, songs in Ukrainian get Ukrainian spelling (Київ → Kyiv), and a romanized line starts with a capital. Ported from Spicy Lyrics 6.5.0 (romanization by TheX24).
- **Up next.** In a song's last 30 seconds, the song after it shows quietly in the notice bar at the top: its cover as the icon, its name and artist beside it. Not on repeat-one. The queue is read once more as those 30 seconds begin (one more request to Spotify a song). Settings → Lyrics → Up next.
- **Join split words has four choices:** Off, All words, Steadily sung and Held words. A dash or a slash between the parts of a word now ends a word there ("tea|ry-|eyed" is "teary-" then "eyed"). If you had it off, it stays off.
- **Spring and Bloom.** The lyrics styles have Spicy Lyrics 6.5.0's names: Spring (was Spicy Lyrics) and Bloom (was Spicy Lyrics Simple, now always the refreshed one, so its own switch is gone). Background vocal pop-out is on with Bloom. Spring gets Increased line spacing, on by default (Settings → Lyrics).
- **Long names slide in step.** A long song name and its artist set off together, the shorter one waits at its end, and they come back together, at one steady pace.
- **Smaller things.** Text size goes from 60 to 200%. A song's own timing also moves by 10 ms. Minimal lyrics mode is now called Fade away previous lines.

Fixes:

- In your own TTML files written without spaces, Japanese and Chinese syllables are no longer spaced apart (君 の 名 前 is 君の名前 again).
- On its side over a Canvas, the line at the top no longer has to slide: "PLAYING FROM" and "PLAYING ON" are left out when they wouldn't fit.
