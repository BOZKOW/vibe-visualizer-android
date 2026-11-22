package io.github.bozkow.vibevisualizer.ui

import android.os.Build
import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.bozkow.vibevisualizer.BuildConfig
import io.github.bozkow.vibevisualizer.core.S
import io.github.bozkow.vibevisualizer.engine.Lyr
import io.github.bozkow.vibevisualizer.engine.ProblemReport

/** This prepares a report, never uploads/submits it. GitHub remains the explicit review/submission step. */
@Composable
fun ReportProblemSheet() {
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var include by rememberSaveable { mutableStateOf(true) }
    var preview by rememberSaveable { mutableStateOf(false) }
    val config = LocalConfiguration.current
    val details = ProblemReport.Details("${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}, ${BuildConfig.BUILD_TYPE}, debuggable=${BuildConfig.DEBUG})",
        "${Build.MANUFACTURER} ${Build.MODEL}", "${Build.VERSION.RELEASE} / API ${Build.VERSION.SDK_INT}",
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) "landscape" else "portrait",
        if (liquidOn()) "Liquid" else "Frosted", S.str("fpscap"), S.low, S.bgStyle,
        S.b("cvBlur"), S.str("cvBehind"), S.i("cvBlurAmt"))
    val report = ProblemReport.body(description, details.takeIf { include })
    val url = ProblemReport.draftUrl(title, report)
    val ready = title.isNotBlank() && description.isNotBlank()
    Text("Report a problem", fontSize = 22.sp, color = Color.White)
    Spacer(Modifier.height(8.dp))
    Hint("Describe what happened and how to reproduce it. You can review the report before opening GitHub.")
    OutlinedTextField(title, { title = it.take(120) }, Modifier.fillMaxWidth(), singleLine = true,
        label = { Text("Short title") })
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(description, { description = it.take(4000) }, Modifier.fillMaxWidth(), minLines = 3, maxLines = 7,
        label = { Text("What happened? Steps to reproduce") }, supportingText = { Text("${description.length}/4000") })
    Row(Modifier.fillMaxWidth().toggleable(include, role = Role.Switch) { include = it }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Include technical details", color = Color.White)
            Hint("App version, phone model and display settings. No keys, server addresses, song history or logs.")
        }
        VSwitch(include, null)
    }
    TextButton({ preview = !preview }) { Text(if (preview) "Hide report preview" else "Preview report") }
    if (preview) SelectionContainer { Text(report, fontSize = 12.sp, color = Color.White.copy(alpha = .75f)) }
    Hint("GitHub reports are public. Don’t include keys or private URLs. A GitHub account is needed to submit; you can also copy the report.")
    if (url == null) Hint("This report is too long for a link. Copy & open GitHub, then paste it into the issue description.")
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        VButton({
            if (url != null) openUrl(url)
            else { Lyr.copy(report); openUrl(ProblemReport.blankUrl(title)) }
        }, enabled = ready) { Text(if (url != null) "Review on GitHub" else "Copy & open GitHub") }
        VOutlinedButton({ Lyr.copy("# ${title.trim()}\n\n$report") }, enabled = description.isNotBlank()) { Text("Copy report") }
    }
    TextButton({ openUrl(ProblemReport.ISSUES) }) { Text("View existing reports") }
}
