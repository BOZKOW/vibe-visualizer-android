package io.github.bozkow.vibevisualizer.lyrics

import org.junit.Assert.*
import org.junit.Test

class InterludeAlignmentTest {
    @Test fun introAndGapFollowTheUpcomingSinger() {
        val lines = interludes(listOf(Line(4.0, 5.0, "Other singer", opp = true), Line(10.0, 11.0, "Lead singer")))
        val dots = lines.filter { it.dots }
        assertEquals(2, dots.size)
        assertTrue(dots[0].opp)
        assertFalse(dots[1].opp)
        assertEquals(4.0, dots[0].end!!, 0.0)
        assertEquals(10.0, dots[1].end!!, 0.0)
    }
    @Test fun suppliedBreaksAlsoFollowTheNextRealLine() {
        val lines = interludes(listOf(Line(0.0, 1.0, "Lead"), Line(1.0, 6.0, "", dots = true), Line(6.0, 7.0, "Other", opp = true)))
        val dots = lines.filter { it.dots }
        assertEquals(1, dots.size)
        assertTrue(dots.single().opp)
        assertEquals(1.0, dots.single().t!!, 0.0)
    }
}
