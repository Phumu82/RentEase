package com.rentease.app.presentation.common

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

/** One-shot: opens [url] (e.g. a signed document URL) in the viewer app, then calls [onOpened]. */
@Composable
fun OpenUrlEffect(url: String?, onOpened: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(url) {
        if (url != null) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            onOpened()
        }
    }
}
