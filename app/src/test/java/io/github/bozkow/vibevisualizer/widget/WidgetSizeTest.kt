package io.github.bozkow.vibevisualizer.widget

import org.junit.Assert.assertEquals
import org.junit.Test

/** Mobile's WidgetFitTest cases for launcherDp (Spicy Lyrics Mobile 0.19.0). */
class WidgetSizeTest {
    @Test fun `a smaller display size is taken back to the app's dp`() {
        // One UI reports a widget a third too big when the display is made smaller
        assertEquals(285 to 143, launcherDp(380, 190, 315, 420))
    }

    @Test fun `the default density or none at all is left alone`() {
        assertEquals(380 to 190, launcherDp(380, 190, 420, 420))
        assertEquals(380 to 190, launcherDp(380, 190, 420, 0))
        assertEquals(380 to 190, launcherDp(380, 190, 0, 420))
    }

    @Test fun `a larger display size stays the size it is`() {
        // An S25 Edge at its largest display size (Mobile's issue #44): scaled up, it was wider than the screen
        // and the lyrics came out far too small
        assertEquals(401 to 213, launcherDp(401, 213, 600, 450))
    }
}
