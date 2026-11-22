package io.github.bozkow.vibevisualizer.lyrics

import io.github.bozkow.vibevisualizer.engine.ProblemReport
import java.net.URI
import java.net.URLDecoder
import org.junit.Assert.*
import org.junit.Test

class ProblemReportTest {
    private val details = ProblemReport.Details("0.61.1", "Samsung SM-G990B", "16 / API 36", "portrait", "Liquid", "60", false, "dynamic", true, "both", 50)
    @Test fun optionalDiagnosticsAreAnExplicitLimitedSet() {
        val report = ProblemReport.body(" Blur ignores the controls\nSteps: open player ", details)
        assertTrue(report.contains("- Canvas blur: true"))
        assertTrue(report.contains("Samsung SM-G990B"))
        assertFalse(report.contains("server="))
        val without = ProblemReport.body("My description", null)
        assertEquals("## Problem\n\nMy description\n", without)
        assertFalse(without.contains("Technical details"))
        assertFalse(without.contains("SM-G990B"))
    }
    @Test fun issueLinkEncodesUnicodeAndQueryCharactersWithoutPublishingAnything() {
        val title = "歌詞 & blur? #1"; val body = ProblemReport.body("Step 1\nA+B & C?", details)
        val uri = URI(ProblemReport.draftUrl(title, body)!!)
        assertEquals("github.com", uri.host)
        assertEquals("/BOZKOW/vibe-visualizer-android/issues/new", uri.path)
        val fields = uri.rawQuery.split('&').associate { val p = it.split('=', limit = 2); p[0] to URLDecoder.decode(p[1], "UTF-8") }
        assertEquals(title, fields["title"]); assertEquals(body, fields["body"])
        assertEquals(setOf("title", "body"), fields.keys)
    }
    @Test fun unknownImportedSettingValuesAreNotEchoedIntoDiagnostics() {
        val report = ProblemReport.body("Issue", details.copy(background = "https://private.example/?key=secret", controlsBackdrop = "private-url", fps = "private-token"))
        assertFalse(report.contains("private")); assertFalse(report.contains("secret"))
        assertTrue(report.contains("- Background: other"))
    }
    @Test fun oversizedUnicodeReportUsesCopyFallbackWithoutLosingText() {
        val text = "歌".repeat(4000)
        val body = ProblemReport.body(text, details)
        assertTrue(body.contains(text))
        assertNull(ProblemReport.draftUrl("Large report", body))
        assertTrue(ProblemReport.blankUrl("Large report").length < 7500)
    }
}
