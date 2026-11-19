package io.github.bozkow.vibevisualizer.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.bozkow.vibevisualizer.engine.E
import kotlin.math.roundToInt

internal val LocalPlayerNotices = staticCompositionLocalOf<PlayerNoticeHost?> { null }
internal val LocalNoticeBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/** Notice placeholders stay in their original layouts; their visible contents sit above one recorded player body.
 * The body includes the cover, song text, lyrics and background, but never the notice sampling it. */
internal class PlayerNoticeHost(val backdrop: Backdrop) {
    data class Slot(val order: Int, val home: Boolean, val row: Boolean, val upNext: Boolean, val at: Offset, val width: Int, val height: Int = 0)
    val slots = mutableStateMapOf<Any, Slot>()
    private var serial = 0
    fun order() = ++serial
    val active get() = slots.entries.maxByOrNull { it.value.order }
    fun hasNotices(): Boolean = active?.value?.let { s -> E.notices(s.home).any { s.upNext || it.id != "upnext" } } == true
}

@Composable
internal fun PlayerNoticeSlot(host: PlayerNoticeHost, home: Boolean, row: Boolean, upNext: Boolean) {
    val owner = remember(host) { Any() }
    val order = remember(owner) { host.order() }
    val old = host.slots[owner]
    val has = E.notices(home).any { upNext || it.id != "upnext" }
    val h = if (!has) 0.dp else old?.height?.takeIf { it > 0 }?.let { with(LocalDensity.current) { it.toDp() } } ?: 60.dp
    DisposableEffect(owner) { onDispose { host.slots.remove(owner) } }
    Box(Modifier.fillMaxWidth().height(h).onGloballyPositioned { c ->
        val entry = PlayerNoticeHost.Slot(order, home, row, upNext, c.positionInRoot(), c.size.width, host.slots[owner]?.height ?: 0)
        if (host.slots[owner] != entry) host.slots[owner] = entry
    })
}

@Composable
internal fun PlayerNoticeOverlay(host: PlayerNoticeHost) {
    val entry = host.active ?: return
    val (owner, slot) = entry
    val density = LocalDensity.current
    if (slot.width <= 0) return
    androidx.compose.runtime.CompositionLocalProvider(LocalNoticeBackdrop provides host.backdrop) {
        Box(Modifier.offset { IntOffset(slot.at.x.roundToInt(), slot.at.y.roundToInt()) }
            .width(with(density) { slot.width.toDp() }).onSizeChanged { size ->
                host.slots[owner]?.let { if (it.height != size.height) host.slots[owner] = it.copy(height = size.height) }
            }) {
            PlayerNoticeContent(slot.home, slot.row, slot.upNext)
        }
    }
}
