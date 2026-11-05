package com.rentease.app.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

/** How often an open conversation checks for new messages. */
const val LIVE_REFRESH_MS = 4_000L

/** How often a conversation LIST checks for new conversations or unread counts. */
const val LIST_REFRESH_MS = 15_000L

/**
 * Calls [onTick] every [intervalMs] while the screen is visible (STARTED) and stops when the app is
 * in the background, so an open chat shows new messages without a manual refresh and without
 * draining the battery in the background. Polling keeps the app free of a websocket dependency.
 */
@Composable
fun LiveRefresh(intervalMs: Long = LIVE_REFRESH_MS, onTick: () -> Unit) {
    val latest = rememberUpdatedState(onTick)
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(owner, intervalMs) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                delay(intervalMs)
                latest.value()
            }
        }
    }
}
