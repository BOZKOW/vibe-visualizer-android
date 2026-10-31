package io.github.bozkow.vibevisualizer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.DeviceThermostat
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import io.github.bozkow.vibevisualizer.core.S
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Surface
import kotlinx.coroutines.delay
import androidx.compose.ui.unit.sp
import io.github.bozkow.vibevisualizer.engine.E

private class SnackLook(val s: E.Snack) : SnackbarVisuals {
    override val message = s.msg
    override val actionLabel = s.action?.label
    override val withDismissAction = false
    override val duration = if (s.long || s.action != null) SnackbarDuration.Long else SnackbarDuration.Short
}

/** One-off errors and warnings, one at a time: each stays a few seconds (ten with a button) unless its button is
 *  pressed. On the player they're in the top bar, where "Playing from …" goes; over Settings, a sheet or the setup
 *  guide they come down from the top here. */
@Composable
fun TopNotes(modifier: Modifier = Modifier) {
    val cur = E.notes.firstOrNull()   // it stays until OK (or its detail screen's OK)
    val last = remember { arrayOfNulls<E.Snack>(1) }   // still shown while it slides away
    if (cur != null) last[0] = cur
    val over = E.settingsPage != null || E.wizard || E.sheet != null
    AnimatedVisibility(cur != null && over && !E.inPip, modifier, enter = slideInVertically { -it } + fadeIn(), exit = slideOutVertically { -it } + fadeOut()) {
        val n = last[0] ?: return@AnimatedVisibility
        val err = n.kind == "err"
        // frosted glass: a dark pill that lets a little through (what's under it never shows through its words), lightly
        // tinted by its kind (red for errors, amber for waits and warnings), lit from the top, with a thin light edge; in
        // the middle, clear of the close button on either side, its words centred
        val tint = when (n.kind) { "err" -> Color(0xFFFF6B6B); "warn" -> Color(0xFFFFC857); else -> Color.White }
        Surface(Modifier.padding(horizontal = 60.dp).widthIn(max = 560.dp), shape = RoundedCornerShape(22.dp), color = Color(0xFF202027).copy(alpha = .9f),
            contentColor = Color.White, shadowElevation = 8.dp, border = BorderStroke(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = .38f), Color.White.copy(alpha = .08f))))) {
            // tapped: its detail screen; OK puts it away (they no longer go by themselves). The words in the middle of
            // the glass, OK under them
            Column(Modifier.background(Brush.verticalGradient(listOf(tint.copy(alpha = .2f), tint.copy(alpha = .06f))))
                .clickable { E.haptic(); E.noticeOpen = E.notices().firstOrNull { it.id == "note" + n.id } }
                .padding(horizontal = 16.dp).padding(top = 10.dp, bottom = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(n.msg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                TextButton(onClick = { E.haptic(); E.noteDone(n, false) }, Modifier.heightIn(min = 36.dp)) { Text("OK", fontWeight = FontWeight.Bold, color = if (err) Color(0xFFFFB4AB) else Color.White) }
            }
        }
    }
}

/** Good news (an update, "Copied"), one at a time, at the bottom: Android's snackbar. Swipe one away to dismiss it. */
@Composable
fun Snacks(modifier: Modifier = Modifier) {
    val host = remember { SnackbarHostState() }
    val cur = E.snacks.firstOrNull()
    LaunchedEffect(cur?.id) {
        val s = cur ?: return@LaunchedEffect
        val r = host.showSnackbar(SnackLook(s))
        E.snacks.remove(s)
        if (r == SnackbarResult.ActionPerformed) s.action?.fn?.invoke() else s.onDismiss?.invoke()
    }
    SnackbarHost(host, modifier) { data ->
        val st = rememberSwipeToDismissBoxState()
        SwipeToDismissBox(st, backgroundContent = {}, onDismiss = { data.dismiss() }) {
            Snackbar(
                Modifier.padding(horizontal = 12.dp).widthIn(max = 560.dp),
                action = data.visuals.actionLabel?.let { l -> { TextButton(onClick = { E.haptic(); data.performAction() }) { Text(l, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) } } },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = Color.White,
            ) { Text(data.visuals.message, fontSize = 14.sp) }
        }
    }
}

/** A question you asked for (allow installs, forget this device, reconnect Spotify): Android's dialog. */
@Composable
fun AskDialog() {
    val a = E.ask
    val pri = a?.actions?.firstOrNull { it.pri }
    val other = a?.actions?.firstOrNull { !it.pri }
    // the app's own dialog (Android's can't open out of the button that asked)
    PopDialog(a != null, onDismiss = { E.ask = null }, title = a?.title ?: "", text = a?.msg ?: "",
        confirm = pri?.let { it.label to { E.ask = null; it.fn?.invoke(); Unit } }, dismiss = other?.let { it.label to { E.ask = null; it.fn?.invoke(); Unit } })
}

/** The phone is getting warm (or hot), as Android says: a warning over the whole screen, the player blurred behind
 *  it, offering Low performance mode. Back is "Not now". */
@Composable
fun HeatWarning() {
    val w = io.github.bozkow.vibevisualizer.engine.Perf.warn.takeIf { !S.low } ?: 0
    val vis = androidx.compose.runtime.remember { androidx.compose.animation.core.MutableTransitionState(false) }; vis.targetState = w > 0
    val last = androidx.compose.runtime.remember { object { var w = 1; var c = "" } }
    if (w > 0 && last.w != w) { last.w = w }
    if (w > 0) last.c = io.github.bozkow.vibevisualizer.engine.Perf.cause   // what set it off (kept while it fades)
    if (w > 0) androidx.activity.compose.BackHandler { io.github.bozkow.vibevisualizer.engine.Perf.warnDone(false) }
    if (!vis.currentState && !vis.targetState) return
    val hot = last.w >= 2
    androidx.compose.animation.AnimatedVisibility(vis, enter = androidx.compose.animation.EnterTransition.None, exit = androidx.compose.animation.ExitTransition.None) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxSize().animateEnterExit(enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(260)), exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(220)))
                .background(Color.Black.copy(alpha = if (E.wizard || E.settingsPage != null) .86f else .62f)).clickable(androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, null) {})
            // on its side there's little height: smaller, and it scrolls, so the buttons can always be reached
            val short = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp < 520
            Column(Modifier.animateEnterExit(enter = popIn(androidx.compose.ui.graphics.TransformOrigin.Center, .6f), exit = popOut(androidx.compose.ui.graphics.TransformOrigin.Center, .8f))
                .statusBarsSpace().navBarsSpace().verticalScroll(rememberScrollState()).padding(if (short) 16.dp else 32.dp).widthIn(max = if (short) 560.dp else 420.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val tint = if (hot) Color(0xFFFF5449) else Color(0xFFFFD27A)
                Box(Modifier.size(if (short) 56.dp else 96.dp).clip(CircleShape).background(tint.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
                    Icon(androidx.compose.material.icons.Icons.Rounded.DeviceThermostat, null, Modifier.size(if (short) 32.dp else 52.dp), tint = tint)
                }
                Text(if (hot) "Your phone is hot" else "Your phone is getting warm", Modifier.padding(top = if (short) 10.dp else 24.dp), fontSize = if (short) 22.sp else 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, textAlign = TextAlign.Center)
                last.c.takeIf { it.isNotEmpty() }?.let { Text(it.replaceFirstChar { ch -> ch.uppercase() }, Modifier.padding(top = 6.dp), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = tint, textAlign = TextAlign.Center) }
                Text("Low performance mode keeps it cooler: the moving background and the live blurs rest, and the app draws fewer frames a second. You can turn it off again in Settings → Performance & battery.",
                    Modifier.padding(top = if (short) 8.dp else 14.dp), fontSize = if (short) 13.sp else 15.sp, lineHeight = if (short) 18.sp else 21.sp, color = Color.White.copy(alpha = .75f), textAlign = TextAlign.Center)
                VButton(onClick = { E.haptic(); io.github.bozkow.vibevisualizer.engine.Perf.warnDone(true) }, Modifier.padding(top = if (short) 14.dp else 28.dp).fillMaxWidth().height(if (short) 46.dp else 54.dp)) { Text("Turn on Low performance mode", fontSize = 16.sp) }
                TextButton(onClick = { io.github.bozkow.vibevisualizer.engine.Perf.warnDone(false) }, Modifier.padding(top = 6.dp)) { Text("Not now", fontSize = 15.sp, color = Color.White.copy(alpha = .8f)) }
            }
        }
    }
}

/** A notice tapped at the top: over the whole screen, as the heat warning, what it says, what to do about it (its
 *  steps), the button that fixes it (when there is one) and OK, which puts it away. Back closes it as it is. */
@Composable
fun NoticeDetail() {
    val p = E.noticeOpen
    val vis = androidx.compose.runtime.remember { androidx.compose.animation.core.MutableTransitionState(false) }; vis.targetState = p != null
    val last = androidx.compose.runtime.remember { arrayOfNulls<E.Prob>(1) }; if (p != null) last[0] = p
    if (p != null) androidx.activity.compose.BackHandler { E.noticeOpen = null }
    if (!vis.currentState && !vis.targetState) return
    val q = last[0] ?: return
    val close = { E.noticeOpen = null }
    androidx.compose.animation.AnimatedVisibility(vis, enter = androidx.compose.animation.EnterTransition.None, exit = androidx.compose.animation.ExitTransition.None) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxSize().animateEnterExit(enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(260)), exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(220)))
                .background(Color.Black.copy(alpha = if (E.wizard || E.settingsPage != null) .86f else .62f)).clickable(androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, null) { close() })
            val short = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp < 520
            Column(Modifier.animateEnterExit(enter = popIn(androidx.compose.ui.graphics.TransformOrigin.Center, .6f), exit = popOut(androidx.compose.ui.graphics.TransformOrigin.Center, .8f))
                .statusBarsSpace().navBarsSpace().verticalScroll(rememberScrollState()).padding(if (short) 16.dp else 32.dp).widthIn(max = if (short) 560.dp else 420.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val tint = when (q.k) { "err" -> Color(0xFFFF5449); "ok" -> Color.White; else -> Color(0xFFFFD27A) }
                val icon = when (q.id) {
                    "offline" -> androidx.compose.material.icons.Icons.Rounded.WifiOff
                    "server" -> androidx.compose.material.icons.Icons.Rounded.Dns
                    "spotify" -> VIcons.spotify
                    "wait" -> androidx.compose.material.icons.Icons.Rounded.HourglassTop
                    "heat" -> androidx.compose.material.icons.Icons.Rounded.DeviceThermostat
                    "lyrics" -> VIcons.lyrics
                    "update" -> androidx.compose.material.icons.Icons.Rounded.Download
                    else -> if (q.k == "err") androidx.compose.material.icons.Icons.Rounded.ErrorOutline else if (q.k == "ok") androidx.compose.material.icons.Icons.Rounded.CheckCircleOutline else androidx.compose.material.icons.Icons.Rounded.Info
                }
                Box(Modifier.size(if (short) 56.dp else 96.dp).clip(CircleShape).background(tint.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
                    Icon(icon, null, Modifier.size(if (short) 32.dp else 52.dp), tint = tint)
                }
                Text(q.t, Modifier.padding(top = if (short) 10.dp else 24.dp), fontSize = if (short) 22.sp else 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, textAlign = TextAlign.Center)
                if (q.s.isNotEmpty()) Text(q.s, Modifier.padding(top = 6.dp), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = tint, textAlign = TextAlign.Center)
                if (q.steps.isNotEmpty()) Column(Modifier.padding(top = if (short) 10.dp else 18.dp).fillMaxWidth().glassCard().padding(16.dp)) {
                    Text("What to do", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = .6f), letterSpacing = 1.sp)
                    q.steps.forEachIndexed { i, st ->
                        Row(Modifier.padding(top = 8.dp)) {
                            Text("${i + 1}", Modifier.width(22.dp), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = tint)
                            Text(st, fontSize = 15.sp, lineHeight = 21.sp, color = Color.White.copy(alpha = .85f))
                        }
                    }
                }
                q.act?.let { a -> VButton(onClick = { E.haptic(); close(); a.fn?.invoke() }, Modifier.padding(top = if (short) 14.dp else 24.dp).fillMaxWidth().height(if (short) 46.dp else 54.dp)) { Text(a.label, fontSize = 16.sp) } }
                VTonalButton(onClick = { E.haptic(); close(); q.ok?.invoke() }, Modifier.padding(top = if (q.act != null) 8.dp else if (short) 14.dp else 24.dp).fillMaxWidth().height(if (short) 44.dp else 50.dp)) { Text("OK", fontSize = 16.sp) }
            }
        }
    }
}

/** A card of frosted glass (the steps on a notice's detail screen). */
@Composable
private fun Modifier.glassCard(): Modifier = this.glass(RoundedCornerShape(20.dp))

/** The heat warning in the picture-in-picture window: taps there reach only the window's own buttons, which offer
 *  Low performance mode, play / pause and "Not now" while it shows. */
@Composable
fun PipHeat() {
    val w = io.github.bozkow.vibevisualizer.engine.Perf.warn.takeIf { !S.low } ?: 0
    androidx.compose.animation.AnimatedVisibility(w > 0, enter = androidx.compose.animation.fadeIn(), exit = androidx.compose.animation.fadeOut()) {
        val tint = if (w >= 2) Color(0xFFFF5449) else Color(0xFFFFD27A)
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .72f)).padding(10.dp), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(androidx.compose.material.icons.Icons.Rounded.DeviceThermostat, null, Modifier.size(30.dp), tint = tint)
                Column(Modifier.padding(start = 8.dp)) {
                    Text(if (w >= 2) "Your phone is hot" else "Your phone is getting warm", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 2)
                    Text("Low performance mode suggested · tap the window for its buttons", fontSize = 11.sp, color = Color.White.copy(alpha = .75f), maxLines = 2)
                }
            }
        }
    }
}
