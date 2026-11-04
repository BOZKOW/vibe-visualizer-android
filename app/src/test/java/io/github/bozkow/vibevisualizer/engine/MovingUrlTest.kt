package io.github.bozkow.vibevisualizer.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class MovingUrlTest {
    @Test fun discordPictureThatCanMoveIsAskedForAsGif() {
        assertEquals("https://cdn.discordapp.com/avatars/1/a_abc.gif?size=256", movingUrl("https://cdn.discordapp.com/avatars/1/a_abc.webp?size=256"))
        assertEquals("https://cdn.discordapp.com/avatars/1/a_abc.gif", movingUrl("https://cdn.discordapp.com/avatars/1/a_abc.png"))
    }

    @Test fun everythingElseStaysAsItIs() {
        for (u in listOf(
            "https://cdn.discordapp.com/avatars/1/a_abc.gif?size=256",   // already the GIF
            "https://cdn.discordapp.com/guilds/1/users/2/avatars/a_abc.gif?size=256",
            "https://cdn.discordapp.com/avatars/1/abc.png?size=128",     // a still one on Discord
            "https://example.com/a_abc.png",                             // not Discord
        )) assertEquals(u, movingUrl(u))
    }
}
